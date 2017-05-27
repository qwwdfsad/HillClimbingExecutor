package org.hillclimbing;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.String.format;

/**
 * Port of demo from https://github.com/mattwarren/HillClimbingClrThreadPool
 */
public class Demo {

  public static void main(String[] args) throws Exception {
    try {
      runTest(true, "results-random.csv");
      runTest(false, "results-smooth.csv");
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  // TODO revisit it properly after fixing behaviour
  private static void runTest(boolean randomWorkloadJumps, String fileName) throws Exception {
    System.out.println(format("Running Hill Climbing algorithm (%s)", fileName));

    HillClimbing hc = new HillClimbing();

    int newMax;
    long totalCompletions = 0;
    int timer = 0;
    int lastSampleTimer = 0;

    int currentThreadCount = 2;
    hc.forceChange(currentThreadCount, HillClimbing.HillClimbingStateTransition.Initializing);
    Random randomGenerator = ThreadLocalRandom.current();

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
      writer.write("Time,Throughput,Threads\n");
      for (int mode = 1; mode <= 5; mode++) {
        int currentWorkLoad;
        switch (mode) {
          case 1:
          case 5:
            currentWorkLoad = 3;
            break;
          case 2:
          case 4:
            currentWorkLoad = 7;
            break;
          case 3:
            currentWorkLoad = 10;
            break;
          default:
            currentWorkLoad = 1;
            break;
        }

        boolean reportedMsgInWorkload = false;
        int workLoadForSection = currentWorkLoad * 500;
        while (workLoadForSection > 0) {
          if (randomWorkloadJumps) {
            int randomValue = randomGenerator.nextInt(21); // 0 to 20
            if (randomValue >= 19) {
              int randomChange = -2 + randomGenerator.nextInt(5); // i.e. -2, -1, 0, 1, 2 (not 3)
              if (randomChange != 0) {
                System.out.println(format("Changing workload from %d -> %d", currentWorkLoad, currentWorkLoad + randomChange));
                currentWorkLoad += randomChange;
              }
            }
          }

          timer += 1; //tick-tock, each iteration of the loop is 1 second
          totalCompletions += currentThreadCount;
          workLoadForSection -= currentThreadCount;
          double randomNoise = randomGenerator.nextDouble() / 100.0 * 5; // [0..1) -> [0..0.01) -> [0..0.05)
          writer.write(String.format("%d,%f,%d\n", timer,
            Math.min(currentWorkLoad, currentThreadCount) * (0.95 + randomNoise),
            currentThreadCount));


          // Calling HillClimbingInstance.Update(..) should ONLY happen when we need more threads, not all the time!!
          if (currentThreadCount != currentWorkLoad) {
            // We naively assume that each work items takes 1 second (which is also our loop/timer length)
            // So in every loop we complete 'currentThreadCount' pieces of work
            int numCompletions = currentThreadCount;

            // In win32threadpool.cpp it does the following check before calling Update(..)
            // if (elapsed*1000.0 >= (ThreadAdjustmentInterval/2)) //
            // Also 'ThreadAdjustmentInterval' is initially set like so ('INTERNAL_HillClimbing_SampleIntervalLow' = 10):
            // ThreadAdjustmentInterval = CLRConfig::GetConfigValue(CLRConfig::INTERNAL_HillClimbing_SampleIntervalLow);
            double sampleDuration = (double) (timer - lastSampleTimer);
            if (sampleDuration * 1000.0 >= (hc.pNewSampleInterval / 2)) {

              newMax = hc.update(currentThreadCount, sampleDuration, numCompletions);

              System.out.println(format("Mode = %d, Num Completions = %d (%d), New Max = %d (Old = %d), threadAdjustmentInterval = %d",
                mode, numCompletions, totalCompletions, newMax, currentThreadCount, hc.pNewSampleInterval));

              if (newMax > currentThreadCount) {
                // Never go beyond what we actually need (plus 1)
                int newThreadCount = Math.min(newMax, currentWorkLoad + 1); // + 1
                if (newThreadCount != 0 && newThreadCount > currentThreadCount) {
                  // We only ever increase by 1 at a time!
                  System.out.println(format("Increasing thread count, from %d -> %d (CurrentWorkLoad = %d, " +
                      "Hill-Climbing New Max = %d)",
                    currentThreadCount, currentThreadCount + 1, currentWorkLoad, newMax));
                  currentThreadCount += 1;
                }
                else {
                  System.out.println(format("Should have increased thread count, but didn't, newMax = %d, currentThreadCount = %d, " +
                      "currentWorkLoad = %d",
                    newMax, currentThreadCount, currentWorkLoad));
                }
              }
              else if (newMax < (currentThreadCount - 1) && newMax != 0) {
                System.out.println(format("Decreasing thread count, from %d -> %d (CurrentWorkLoad = %d, Hill-Climbing New Max = %d)",
                  currentThreadCount, currentThreadCount - 1, currentWorkLoad, newMax));
                currentThreadCount -= 1;
              }

              lastSampleTimer = timer;
            }
            else {
              System.out.println(format("Sample Duration is too small, current = %f, needed = %f (threadAdjustmentInterval = %d)",
                sampleDuration, (hc.pNewSampleInterval / 2) / 1000.0, hc.pNewSampleInterval));
            }
          }
          else {
            if (!reportedMsgInWorkload) {
              System.out.println(format("Enough threads to carry out current workload, currentThreadCount = %d, currentWorkLoad= %d",
                currentThreadCount, currentWorkLoad));
            }

            reportedMsgInWorkload = true;
          }
        }
      }
    }
  }
}
