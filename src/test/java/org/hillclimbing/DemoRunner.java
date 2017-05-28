package org.hillclimbing;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Demo program, which (manually) emulates throughput changes
 * on hill-climbing updates.
 * Adapted from https://github.com/mattwarren/HillClimbingClrThreadPool
 */
class DemoRunner {

  private final Random random;
  private final boolean randomWorkloadJumps;
  private final boolean printDecisions;

  DemoRunner(Random random, boolean randomWorkloadJumps, boolean printDecisions) {
    this.random = random;
    this.randomWorkloadJumps = randomWorkloadJumps;
    this.printDecisions = printDecisions;
  }

  /**
   * @return list of triples (time,throughput,threads) in csv format
   */
  List<String> run() {
    print("Running Hill Climbing algorithm");
    List<String> result = new ArrayList<>();

    HillClimbing hc = new HillClimbing(random);
    int newMax;
    long totalCompletions = 0;
    int timer = 0;
    int lastSampleTimer = 0;
    int currentThreadCount = 2;

    hc.forceChange(currentThreadCount, HillClimbing.HillClimbingStateTransition.Initializing);
    for (int mode = 1; mode <= 5; mode++) {
      int currentWorkLoad = getCurrentWorkLoad(mode);
      boolean reportedMsgInWorkload = false;
      int workLoadForSection = currentWorkLoad * 500;
      while (workLoadForSection > 0) {
        if (randomWorkloadJumps) {
          currentWorkLoad = disturbWorkload(currentWorkLoad);
        }

        timer += 1; // Each iteration of the loop is 1 second
        totalCompletions += currentThreadCount;
        workLoadForSection -= currentThreadCount;
        double randomNoise = random.nextDouble() / 100.0 * 5; // [0..1) -> [0..0.05)
        result.add(String.format("%d,%f,%d", timer,
          Math.min(currentWorkLoad, currentThreadCount) * (0.95 + randomNoise),
          currentThreadCount));

        // Calling hill-climbing update should only happen when we need more threads, not all the time
        if (currentThreadCount != currentWorkLoad) {
          // We naively assume that each work items takes 1 second (which is also our loop/timer length)
          // So in every loop we complete 'currentThreadCount' pieces of work
          int numCompletions = currentThreadCount;

          // In win32threadpool.cpp it does the following check before calling Update(..)
          // if (elapsed * 1000.0 >= (ThreadAdjustmentInterval / 2))
          // Also 'ThreadAdjustmentInterval' is initially set to low hill-climbing sample interval
          double sampleDuration = (double) (timer - lastSampleTimer);
          if (sampleDuration * 1000.0 >= (hc.pNewSampleInterval / 2)) {

            newMax = hc.update(currentThreadCount, sampleDuration, numCompletions);

            print("Mode=%d, Num Completions=%d (%d), New Max=%d (Old=%d), threadAdjustmentInterval=%d",
              mode, numCompletions, totalCompletions, newMax, currentThreadCount, hc.pNewSampleInterval);

            if (newMax > currentThreadCount) {
              // Never go beyond what we actually need (plus 1)
              int newThreadCount = Math.min(newMax, currentWorkLoad + 1); // + 1
              if (newThreadCount != 0 && newThreadCount > currentThreadCount) {
                // We only ever increase by 1 at a time
                print("Increasing thread count: %d -> %d (CurrentWorkLoad=%d, " +
                    "Hill-Climbing New Max=%d)",
                  currentThreadCount, currentThreadCount + 1, currentWorkLoad, newMax);
                currentThreadCount += 1;
              }
              else {
                print("Should have increased thread count, but didn't, newMax=%d, currentThreadCount=%d, " +
                    "currentWorkLoad=%d",
                  newMax, currentThreadCount, currentWorkLoad);
              }
            }
            else if (newMax < (currentThreadCount - 1) && newMax != 0) {
              print("Decreasing thread count, from %d -> %d (CurrentWorkLoad=%d, Hill-Climbing New Max=%d)",
                currentThreadCount, currentThreadCount - 1, currentWorkLoad, newMax);
              currentThreadCount -= 1;
            }

            lastSampleTimer = timer;
          }
          else {
            print("Sample duration is too small, current=%f, needed=%f (threadAdjustmentInterval=%d)",
              sampleDuration, (hc.pNewSampleInterval / 2) / 1000.0, hc.pNewSampleInterval);
          }
        }
        else {
          if (!reportedMsgInWorkload) {
            print("Enough threads to carry out current workload, currentThreadCount=%d, currentWorkLoad=%d",
              currentThreadCount, currentWorkLoad);
          }

          reportedMsgInWorkload = true;
        }
      }
    }

    return result;
  }

  private int disturbWorkload(int currentWorkLoad) {
    int randomValue = random.nextInt(21);
    if (randomValue >= 19) { // 10% chance of random walk
      int randomChange = random.nextInt(5) - 2; // [-2; 2]
      if (randomChange != 0 && currentWorkLoad + randomChange > 0) {
        print("Changing workload from %d -> %d", currentWorkLoad, currentWorkLoad + randomChange);
        currentWorkLoad += randomChange;
      }
    }
    return currentWorkLoad;
  }

  private void print(String pattern, Object... args) {
    if (printDecisions) {
      System.out.println(String.format(pattern, args));
    }
  }

  private static int getCurrentWorkLoad(int mode) {
    switch (mode) {
      case 1:
      case 5:
        return 3;
      case 2:
      case 4:
        return 7;
      case 3:
        return 10;
      default:
        return 1;
    }
  }
}
