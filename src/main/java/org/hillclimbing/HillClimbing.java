/*
 * Licensed to the .NET Foundation under one or more agreements.
 * The .NET Foundation licenses this file to you under the MIT license.
 * See the LICENSE file in the project root for more information.
 */
package org.hillclimbing;

import java.util.Random;

import static org.hillclimbing.Complex.real;

// TODO: rename, extract opts, fix behaviour, provide cpu-utilization, provide adequate api, revisit overflows
final class HillClimbing {

  private static final double PI = 3.141592653589793;

  private final HillClimbingOptions options;

  private final double[] samples;
  private final double[] threadCounts;
  private final Random randomIntervalGenerator;

  // Emulate out/int* parameter
  public int pNewSampleInterval;
  private double currentControlSetting;
  private long totalSamples;
  private int lastThreadCount;
  // seconds in double
  private double elapsedSinceLastChange;
  private double completionsSinceLastChange;
  private double averageThroughputNoise;
  private int currentSampleInterval;
  private int accumulatedCompletionCount;
  private double accumulatedSampleDuration;

  HillClimbing(HillClimbingOptions options, Random random) {
    this.options = options;
    currentControlSetting = 0;
    totalSamples = 0;
    lastThreadCount = 0;
    averageThroughputNoise = 0;
    elapsedSinceLastChange = 0;
    completionsSinceLastChange = 0;
    accumulatedCompletionCount = 0;
    accumulatedSampleDuration = 0;
    samples = new double[options.samplesToMeasure];
    threadCounts = new double[options.samplesToMeasure];
    randomIntervalGenerator = random;
    currentSampleInterval = options.sampleIntervalLow + randomIntervalGenerator.nextInt(options.sampleIntervalHigh + 1);
  }


  void forceChange(int newThreadCount, StateTransition transition) {
    if (newThreadCount != lastThreadCount) {
      currentControlSetting += (newThreadCount - lastThreadCount);
      changeThreadsCount(newThreadCount, transition);
    }
  }

  void changeThreadsCount(int newThreadCount, StateTransition transition) {
    lastThreadCount = newThreadCount;
    currentSampleInterval = options.sampleIntervalLow + randomIntervalGenerator.nextInt(options.sampleIntervalHigh + 1);
    // TODO: report here
    // double throughput = (elapsedSinceLastChange > 0) ? (completionsSinceLastChange / elapsedSinceLastChange) : 0;
    //  m_options.FireEtwThreadPoolWorkerThreadAdjustmentAdjustment(newThreadCount, throughput, transition);
    elapsedSinceLastChange = 0;
    completionsSinceLastChange = 0;
  }

  int update(int currentThreadCount, double sampleDuration, int numCompletions) {

    // If someone changed the thread count without telling us, update our records accordingly.
    if (currentThreadCount != lastThreadCount)
      forceChange(currentThreadCount, StateTransition.INITIALIZING);

    // Update the cumulative stats for this thread count
    elapsedSinceLastChange += sampleDuration;
    completionsSinceLastChange += numCompletions;

    // Add in any data we've already collected about this sample
    sampleDuration += accumulatedSampleDuration;
    numCompletions += accumulatedCompletionCount;
    // We need to make sure we're collecting reasonably accurate data.  Since we're just counting the end
    // of each work item, we are goinng to be missing some data about what really happened during the
    // sample interval.  The count produced by each thread includes an initial work item that may have
    // started well before the start of the interval, and each thread may have been running some new
    // work item for some time before the end of the interval, which did not yet get counted.  So
    // our count is going to be off by +/- threadCount workitems.
    //
    // The exception is that the thread that reported to us last time definitely wasn't running any work
    // at that time, and the thread that's reporting now definitely isn't running a work item now.  So
    // we really only need to consider threadCount-1 threads.
    //
    // Thus the percent error in our count is +/- (threadCount-1)/numCompletions.
    //
    // We cannot rely on the frequency-domain analysis we'll be doing later to filter out this error, because
    // of the way it accumulates over time.  If this sample is off by, say, 33% in the negative direction,
    // then the next one likely will be too.  The one after that will include the sum of the completions
    // we missed in the previous samples, and so will be 33% positive.  So every three samples we'll have
    // two "low" samples and one "high" sample.  This will appear as periodic variation right in the frequency
    // range we're targeting, which will not be filtered by the frequency-domain translation.
    if (totalSamples > 0 && ((currentThreadCount - 1.0) / numCompletions) >= options.maxSampleError) {
      // not accurate enough yet.  Let's accumulate the data so far, and tell the ThreadPool
      // to collect a little more.
      accumulatedSampleDuration = sampleDuration;
      accumulatedCompletionCount = numCompletions;
      pNewSampleInterval = 10;
      return currentThreadCount;
    }

    // We've got enouugh data for our sample; reset our accumulators for next time.
    accumulatedSampleDuration = 0;
    accumulatedCompletionCount = 0;

    // Add the current thread count and throughput sample to our history
    double throughput = (double) numCompletions / sampleDuration;

    int sampleIndex = (int) (totalSamples % options.samplesToMeasure);
    samples[sampleIndex] = throughput;
    threadCounts[sampleIndex] = currentThreadCount;
    totalSamples++;

    // Set up defaults for our metrics
    Complex threadWaveComponent;
    Complex throughputWaveComponent;
    Complex ratio = Complex.zero();
    double throughputErrorEstimate;
    double confidence = 0.0;

    StateTransition transition = StateTransition.WARMUP;

    // How many samples will we use?  It must be at least the three wave periods we're looking for, and it must also be a whole
    // multiple of the primary wave's period; otherwise the frequency we're looking for will fall between two  frequency bands
    // in the Fourier analysis, and we won't be able to measure it accurately.
    int sampleCount = (int) (Math.min(totalSamples - 1, options.samplesToMeasure) / options.wavePeriod) * options.wavePeriod;

    if (sampleCount > options.wavePeriod) {
      // Average the throughput and thread count samples, so we can scale the wave magnitudes later.
      double sampleSum = 0;
      double threadSum = 0;
      for (int i = 0; i < sampleCount; i++) {
        sampleSum += samples[(int) ((totalSamples - sampleCount + i) % options.samplesToMeasure)];
        threadSum += threadCounts[(int) ((totalSamples - sampleCount + i) % options.samplesToMeasure)];
      }
      double averageThroughput = sampleSum / sampleCount;
      double averageThreadCount = threadSum / sampleCount;

      if (averageThroughput > 0 && averageThreadCount > 0) {
        // Calculate the periods of the adjacent frequency bands we'll be using to measure noise levels.
        // We want the two adjacent Fourier frequency bands.
        double adjacentPeriod1 = sampleCount / (((double) sampleCount / (double) options.wavePeriod) + 1);
        double adjacentPeriod2 = sampleCount / (((double) sampleCount / (double) options.wavePeriod) - 1);

        // Get the the three different frequency components of the throughput (scaled by average
        // throughput).  Our "error" estimate (the amount of noise that might be present in the
        // frequency band we're really interested in) is the average of the adjacent bands.
        throughputWaveComponent = computeWaveComponent(samples, sampleCount, options.wavePeriod).divideBy(averageThroughput);
        throughputErrorEstimate = computeWaveComponent(samples, sampleCount, adjacentPeriod1).divideBy(averageThroughput).abs();

        if (adjacentPeriod2 <= sampleCount) {
          throughputErrorEstimate = Math.max(throughputErrorEstimate,
            computeWaveComponent(samples, sampleCount, adjacentPeriod2).divideBy(averageThroughput).abs());
        }

        // Do the same for the thread counts, so we have something to compare to.  We don't measure thread count
        // noise, because there is none; these are exact measurements.
        threadWaveComponent = computeWaveComponent(threadCounts, sampleCount, options.wavePeriod).divideBy(averageThreadCount);

        // Update our moving average of the throughput noise.  We'll use this later as feedback to
        // determine the new size of the thread wave.
        if (averageThroughputNoise == 0) {
          averageThroughputNoise = throughputErrorEstimate;
        }
        else {
          averageThroughputNoise = (options.throughputErrorSmoothingFactor * throughputErrorEstimate)
            + ((1.0 - options.throughputErrorSmoothingFactor) * averageThroughputNoise);
        }

        if (threadWaveComponent.abs() > 0) {
          // Adjust the throughput wave so it's centered around the target wave, and then calculate the adjusted throughput/thread ratio.
          ratio = throughputWaveComponent
            .minus(threadWaveComponent.multiplyBy(real(options.targetThroughputRatio)))
            .divideBy(threadWaveComponent);
          transition = StateTransition.CLIMBING_MOVE;
        }
        else {
          ratio = Complex.zero();
          transition = StateTransition.STABILIZING;
        }

        // Calculate how confident we are in the ratio.  More noise == less confident.  This has
        // the effect of slowing down movements that might be affected by random noise.
        double noiseForConfidence = Math.max(averageThroughputNoise, throughputErrorEstimate);
        if (noiseForConfidence > 0) {
          confidence = (threadWaveComponent.abs() / noiseForConfidence) / options.targetSignalToNoiseRatio;
        }
        else {
          confidence = 1.0; //there is no noise!
        }
      }
    }

    // We use just the real part of the complex ratio we just calculated.  If the throughput signal
    // is exactly in phase with the thread signal, this will be the same as taking the magnitude of
    // the complex move and moving that far up.  If they're 180 degrees out of phase, we'll move
    // backward (because this indicates that our changes are having the opposite of the intended effect).
    // If they're 90 degrees out of phase, we won't move at all, because we can't tell whether we're
    // having a negative or positive effect on throughput.
    double move = Math.min(1.0, Math.max(-1.0, ratio.real));

    // Apply our confidence multiplier.
    move *= Math.min(1.0, Math.max(0.0, confidence));

    // Now apply non-linear gain, such that values around zero are attenuated, while higher values
    // are enhanced.  This allows us to move quickly if we're far away from the target, but more slowly
    // if we're getting close, giving us rapid ramp-up without wild oscillations around the target.
    double gain = options.maxChangePerSecond * sampleDuration;
    move = Math.pow(Math.abs(move), options.gainExponent) * (move >= 0.0 ? 1 : -1) * gain;
    move = Math.min(move, options.maxChangePerSample);

    // If the result was positive, and CPU is > 95%, refuse the move.
    if (move > 0.0 && currentCpuUtilization() > options.cpuUtilizationThreshold) {
      move = 0.0;
    }

    // Apply the move to our control setting
    currentControlSetting += move;

    // Calculate the new thread wave magnitude, which is based on the moving average we've been keeping of
    // the throughput error.  This average starts at zero, so we'll start with a nice safe little wave at first.
    int newThreadWaveMagnitude = (int) (0.5 +
      (currentControlSetting * averageThroughputNoise * options.targetSignalToNoiseRatio * options.threadMagnitudeMultiplier * 2.0));
    newThreadWaveMagnitude = Math.min(newThreadWaveMagnitude, options.maxThreadWaveMagnitude);
    newThreadWaveMagnitude = Math.max(newThreadWaveMagnitude, 1);

    // Make sure our control setting is within the ThreadPool's limits
    currentControlSetting = Math.min(options.maxThreadsCount - newThreadWaveMagnitude, currentControlSetting);
    currentControlSetting = Math.max(options.minThreadsCount, currentControlSetting);

    // Calculate the new thread count (control setting + square wave)
    int newThreadCount = (int) (currentControlSetting + newThreadWaveMagnitude * ((totalSamples / (options.wavePeriod / 2)) % 2));

    // Make sure the new thread count doesn't exceed the ThreadPool's limits
    newThreadCount = Math.min(options.maxThreadsCount, newThreadCount);
    newThreadCount = Math.max(options.minThreadsCount, newThreadCount);

    // If all of this caused an actual change in thread count, log that as well.
    if (newThreadCount != currentThreadCount)
      changeThreadsCount(newThreadCount, transition);

    // Return the new thread count and sample interval.  This is randomized to prevent correlations with other periodic
    // changes in throughput.  Among other things, this prevents us from getting confused by Hill Climbing instances
    // running in other processes.
    // If we're at minThreads, and we seem to be hurting performance by going higher, we can't go any lower to fix this.  So
    // we'll simply stay at minThreads much longer, and only occasionally try a higher value.
    if (ratio.real < 0.0 && newThreadCount == options.minThreadsCount) {
      pNewSampleInterval = (int) (0.5 + currentSampleInterval * (10.0 * Math.max(-ratio.real, 1.0)));
    }
    else {
      pNewSampleInterval = currentSampleInterval;
    }

    return newThreadCount;
  }

  private int currentCpuUtilization() {
    return 0;
  }

  private Complex computeWaveComponent(double[] samples, int sampleCount, double period) {
    assert sampleCount >= period; //can't measure a wave that doesn't fit
    assert period >= 2; //can't measure above the Nyquist frequency

    // Calculate the sinusoid with the given period.
    // We're using the Goertzel algorithm for this.  See http://en.wikipedia.org/wiki/Goertzel_algorithm.
    double w = 2.0 * PI / period;
    double cosine = Math.cos(w);
    double sine = Math.sin(w);
    double coefficient = 2.0 * cosine;
    double q0;
    double q1 = 0.0;
    double q2 = 0.0;

    for (int i = 0; i < sampleCount; i++) {
      double sample = samples[(int) ((totalSamples - sampleCount + i) % options.samplesToMeasure)];

      q0 = coefficient * q1 - q2 + sample;
      q2 = q1;
      q1 = q0;
    }

    return new Complex(q1 - q2 * cosine, q2 * sine).divideBy((double) sampleCount);
  }

  enum StateTransition {
    WARMUP,
    INITIALIZING,
    CLIMBING_MOVE,
    STABILIZING,
  }
}
