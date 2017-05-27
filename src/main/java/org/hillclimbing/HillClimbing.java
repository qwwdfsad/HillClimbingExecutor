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
  private static final int CPU_UTILIZATION_THRESHOLD = 95;
  private static final int MIN_THREADS_COUNT = 2;
  private static final int MAX_THREADS_COUNT = 200;


  private final int m_wavePeriod;
  private final int m_samplesToMeasure;
  private final double m_targetThroughputRatio;
  private final double m_targetSignalToNoiseRatio;
  private final int m_maxChangePerSecond;
  private final int m_maxChangePerSample;
  private final int m_maxThreadWaveMagnitude;
  // Both are uint actually
  private final int m_sampleIntervalLow;
  private final int m_sampleIntervalHigh;
  private final double m_threadMagnitudeMultiplier;
  private final double m_throughputErrorSmoothingFactor;
  private final double m_gainExponent;
  private final double m_maxSampleError;
  private final double[] m_samples;
  private final double[] m_threadCounts;
  private final Random m_randomIntervalGenerator;
  private double m_currentControlSetting;
  private long m_totalSamples;  // longlong actually
  private int m_lastThreadCount;
  // seconds in double
  private double m_elapsedSinceLastChange;
  private double m_completionsSinceLastChange;
  private double m_averageThroughputNoise;
  private int m_currentSampleInterval; // uint
  private int m_accumulatedCompletionCount;
  private double m_accumulatedSampleDuration; // again seconds in double

  // Emulate out/int* parameter
  public int pNewSampleInterval;

  HillClimbing() {
    m_wavePeriod = 4;
    m_maxThreadWaveMagnitude = 20;
    m_threadMagnitudeMultiplier = 100 / 100.0;
    m_samplesToMeasure = m_wavePeriod * 8;
    m_targetThroughputRatio = 15 / 100.0;
    m_targetSignalToNoiseRatio = 300 / 100.0;
    m_maxChangePerSecond = 4;
    m_maxChangePerSample = 20;
    m_sampleIntervalLow = 10;
    m_sampleIntervalHigh = 200;
    m_throughputErrorSmoothingFactor = 1 / 100.0;
    m_gainExponent = 200 / 100.0;
    m_maxSampleError = 15 / 100.0;
    m_currentControlSetting = 0;
    m_totalSamples = 0;
    m_lastThreadCount = 0;
    m_averageThroughputNoise = 0;
    m_elapsedSinceLastChange = 0;
    m_completionsSinceLastChange = 0;
    m_accumulatedCompletionCount = 0;
    m_accumulatedSampleDuration = 0;

    m_samples = new double[m_samplesToMeasure];
    m_threadCounts = new double[m_samplesToMeasure];

    // seed our random number generator with the CLR instance ID and the process ID, to avoid correlations with other CLR ThreadPool
    // instances.
//    m_randomIntervalGenerator.Init(((int) 1 << 16) ^ (int) GetCurrentProcessId());
    m_randomIntervalGenerator = new Random();
    m_currentSampleInterval = m_sampleIntervalLow + m_randomIntervalGenerator.nextInt(m_sampleIntervalHigh + 1);
  }


  void forceChange(int newThreadCount, HillClimbingStateTransition transition) {
    if (newThreadCount != m_lastThreadCount) {
      m_currentControlSetting += (newThreadCount - m_lastThreadCount);
      changeThreadsCount(newThreadCount, transition);
    }
  }

  void changeThreadsCount(int newThreadCount, HillClimbingStateTransition transition) {
    m_lastThreadCount = newThreadCount;
    m_currentSampleInterval = m_sampleIntervalLow + m_randomIntervalGenerator.nextInt(m_sampleIntervalHigh + 1);
//    double throughput = (m_elapsedSinceLastChange > 0) ? (m_completionsSinceLastChange / m_elapsedSinceLastChange) : 0;
//    m_options.FireEtwThreadPoolWorkerThreadAdjustmentAdjustment(newThreadCount, throughput, transition);
    m_elapsedSinceLastChange = 0;
    m_completionsSinceLastChange = 0;
  }

  int update(int currentThreadCount, double sampleDuration, int numCompletions) {

    // If someone changed the thread count without telling us, update our records accordingly.
    if (currentThreadCount != m_lastThreadCount)
      forceChange(currentThreadCount, HillClimbingStateTransition.Initializing);

    // Update the cumulative stats for this thread count
    m_elapsedSinceLastChange += sampleDuration;
    m_completionsSinceLastChange += numCompletions;

    // Add in any data we've already collected about this sample
    sampleDuration += m_accumulatedSampleDuration;
    numCompletions += m_accumulatedCompletionCount;
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
    if (m_totalSamples > 0 && ((currentThreadCount - 1.0) / numCompletions) >= m_maxSampleError) {
      // not accurate enough yet.  Let's accumulate the data so far, and tell the ThreadPool
      // to collect a little more.
      m_accumulatedSampleDuration = sampleDuration;
      m_accumulatedCompletionCount = numCompletions;
      pNewSampleInterval = 10;
      return currentThreadCount;
    }

    // We've got enouugh data for our sample; reset our accumulators for next time.
    m_accumulatedSampleDuration = 0;
    m_accumulatedCompletionCount = 0;

    // Add the current thread count and throughput sample to our history
    double throughput = (double) numCompletions / sampleDuration;

    int sampleIndex = (int) (m_totalSamples % m_samplesToMeasure);
    m_samples[sampleIndex] = throughput;
    m_threadCounts[sampleIndex] = currentThreadCount;
    m_totalSamples++;

    // Set up defaults for our metrics
    Complex threadWaveComponent;
    Complex throughputWaveComponent;
    Complex ratio = Complex.zero();
    double throughputErrorEstimate;
    double confidence = 0.0;

    HillClimbingStateTransition transition = HillClimbingStateTransition.Warmup;

    // How many samples will we use?  It must be at least the three wave periods we're looking for, and it must also be a whole
    // multiple of the primary wave's period; otherwise the frequency we're looking for will fall between two  frequency bands
    // in the Fourier analysis, and we won't be able to measure it accurately.
    int sampleCount = (int) (Math.min(m_totalSamples - 1, m_samplesToMeasure) / m_wavePeriod) * m_wavePeriod;

    if (sampleCount > m_wavePeriod) {
      // Average the throughput and thread count samples, so we can scale the wave magnitudes later.
      double sampleSum = 0;
      double threadSum = 0;
      for (int i = 0; i < sampleCount; i++) {
        sampleSum += m_samples[(int) ((m_totalSamples - sampleCount + i) % m_samplesToMeasure)];
        threadSum += m_threadCounts[(int) ((m_totalSamples - sampleCount + i) % m_samplesToMeasure)];
      }
      double averageThroughput = sampleSum / sampleCount;
      double averageThreadCount = threadSum / sampleCount;

      if (averageThroughput > 0 && averageThreadCount > 0) {
        // Calculate the periods of the adjacent frequency bands we'll be using to measure noise levels.
        // We want the two adjacent Fourier frequency bands.
        double adjacentPeriod1 = sampleCount / (((double) sampleCount / (double) m_wavePeriod) + 1);
        double adjacentPeriod2 = sampleCount / (((double) sampleCount / (double) m_wavePeriod) - 1);

        // Get the the three different frequency components of the throughput (scaled by average
        // throughput).  Our "error" estimate (the amount of noise that might be present in the
        // frequency band we're really interested in) is the average of the adjacent bands.
        throughputWaveComponent = computeWaveComponent(m_samples, sampleCount, m_wavePeriod).divideBy(averageThroughput);
        throughputErrorEstimate = computeWaveComponent(m_samples, sampleCount, adjacentPeriod1).divideBy(averageThroughput).abs();

        if (adjacentPeriod2 <= sampleCount) {
          throughputErrorEstimate = Math.max(throughputErrorEstimate,
            computeWaveComponent(m_samples, sampleCount, adjacentPeriod2).divideBy(averageThroughput).abs());
        }

        // Do the same for the thread counts, so we have something to compare to.  We don't measure thread count
        // noise, because there is none; these are exact measurements.
        threadWaveComponent = computeWaveComponent(m_threadCounts, sampleCount, m_wavePeriod).divideBy(averageThreadCount);

        //
        // Update our moving average of the throughput noise.  We'll use this later as feedback to
        // determine the new size of the thread wave.
        //
        if (m_averageThroughputNoise == 0) {
          m_averageThroughputNoise = throughputErrorEstimate;
        }
        else {
          m_averageThroughputNoise = (m_throughputErrorSmoothingFactor * throughputErrorEstimate)
            + ((1.0 - m_throughputErrorSmoothingFactor) * m_averageThroughputNoise);
        }

        if (threadWaveComponent.abs() > 0) {
          // Adjust the throughput wave so it's centered around the target wave, and then calculate the adjusted throughput/thread ratio.
          ratio = throughputWaveComponent
            .minus(threadWaveComponent.multiplyBy(real(m_targetThroughputRatio)))
            .divideBy(threadWaveComponent);
          transition = HillClimbingStateTransition.ClimbingMove;
        }
        else {
          ratio = Complex.zero();
          transition = HillClimbingStateTransition.Stabilizing;
        }

        //
        // Calculate how confident we are in the ratio.  More noise == less confident.  This has
        // the effect of slowing down movements that might be affected by random noise.
        //
        double noiseForConfidence = Math.max(m_averageThroughputNoise, throughputErrorEstimate);
        if (noiseForConfidence > 0) {
          confidence = (threadWaveComponent.abs() / noiseForConfidence) / m_targetSignalToNoiseRatio;
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
    double gain = m_maxChangePerSecond * sampleDuration;
    move = Math.pow(Math.abs(move), m_gainExponent) * (move >= 0.0 ? 1 : -1) * gain;
    move = Math.min(move, m_maxChangePerSample);

    // If the result was positive, and CPU is > 95%, refuse the move.
    if (move > 0.0 && currentCpuUtilization() > CPU_UTILIZATION_THRESHOLD) {
      move = 0.0;
    }

    // Apply the move to our control setting
    m_currentControlSetting += move;

    // Calculate the new thread wave magnitude, which is based on the moving average we've been keeping of
    // the throughput error.  This average starts at zero, so we'll start with a nice safe little wave at first.
    int newThreadWaveMagnitude = (int) (0.5 +
      (m_currentControlSetting * m_averageThroughputNoise * m_targetSignalToNoiseRatio * m_threadMagnitudeMultiplier * 2.0));
    newThreadWaveMagnitude = Math.min(newThreadWaveMagnitude, m_maxThreadWaveMagnitude);
    newThreadWaveMagnitude = Math.max(newThreadWaveMagnitude, 1);

    // Make sure our control setting is within the ThreadPool's limits
    m_currentControlSetting = Math.min(MAX_THREADS_COUNT - newThreadWaveMagnitude, m_currentControlSetting);
    m_currentControlSetting = Math.max(MIN_THREADS_COUNT, m_currentControlSetting);

    // Calculate the new thread count (control setting + square wave)
    int newThreadCount = (int) (m_currentControlSetting + newThreadWaveMagnitude * ((m_totalSamples / (m_wavePeriod / 2)) % 2));

    // Make sure the new thread count doesn't exceed the ThreadPool's limits
    newThreadCount = Math.min(MAX_THREADS_COUNT, newThreadCount);
    newThreadCount = Math.max(MIN_THREADS_COUNT, newThreadCount);

    // If all of this caused an actual change in thread count, log that as well.
    if (newThreadCount != currentThreadCount)
      changeThreadsCount(newThreadCount, transition);

    // Return the new thread count and sample interval.  This is randomized to prevent correlations with other periodic
    // changes in throughput.  Among other things, this prevents us from getting confused by Hill Climbing instances
    // running in other processes.
    // If we're at minThreads, and we seem to be hurting performance by going higher, we can't go any lower to fix this.  So
    // we'll simply stay at minThreads much longer, and only occasionally try a higher value.
    if (ratio.real < 0.0 && newThreadCount == MIN_THREADS_COUNT) {
      pNewSampleInterval = (int) (0.5 + m_currentSampleInterval * (10.0 * Math.max(-ratio.real, 1.0)));
    }
    else {
      pNewSampleInterval = m_currentSampleInterval;
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
      double sample = samples[(int) ((m_totalSamples - sampleCount + i) % m_samplesToMeasure)];

      q0 = coefficient * q1 - q2 + sample;
      q2 = q1;
      q1 = q0;
    }

    return new Complex(q1 - q2 * cosine, q2 * sine).divideBy((double) sampleCount);
  }

  // TODO revisit unused state transitions
  enum HillClimbingStateTransition {
    Warmup,
    Initializing,
    RandomMove,
    ClimbingMove,
    ChangePoint,
    Stabilizing,
    Starvation, //used by ThreadpoolMgr
    ThreadTimedOut, //used by ThreadpoolMgr
    Undefined,
  }
}
