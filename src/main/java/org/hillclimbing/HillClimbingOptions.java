package org.hillclimbing;

public final class HillClimbingOptions {

  public static final HillClimbingOptions DEFAULT_OPTIONS = builder().build();

  public static final class Builder {
    private int myCpuUtilizationThreshold = 95;
    private int myMinThreadsCount = Runtime.getRuntime().availableProcessors();
    private int myMaxThreadsCount = 200;
    private int myWavePeriod = 4;
    private int mySamplesToMeasure = myWavePeriod * 8;
    private double myTargetThroughputRatio = 15 / 100.0;
    private double myTargetSignalToNoiseRatio = 300 / 100.0;
    private int myMaxChangePerSecond = 4;
    private int myMaxChangePerSample = 20;
    private int myMaxThreadWaveMagnitude = 20;
    private int mySampleIntervalLow = 10;
    private int mySampleIntervalHigh = 200;
    private double myThreadMagnitudeMultiplier = 100 / 100.0;
    private double myThroughputErrorSmoothingFactor = 1 / 100.0;
    private double myGainExponent = 200 / 100.0;
    private double myMaxSampleError = 15 / 100.0;

    private Builder() {
    }

    public Builder setCpuUtilizationThreshold(int cpuUtilizationThreshold) {
      myCpuUtilizationThreshold = cpuUtilizationThreshold;
      return this;
    }

    public Builder setMinThreadsCount(int minThreadsCount) {
      myMinThreadsCount = minThreadsCount;
      return this;
    }

    public Builder setMaxThreadsCount(int maxThreadsCount) {
      myMaxThreadsCount = maxThreadsCount;
      return this;
    }

    public Builder setWavePeriod(int wavePeriod) {
      myWavePeriod = wavePeriod;
      return this;
    }

    public Builder setSamplesToMeasure(int samplesToMeasure) {
      mySamplesToMeasure = samplesToMeasure;
      return this;
    }

    public Builder setTargetThroughputRatio(double targetThroughputRatio) {
      myTargetThroughputRatio = targetThroughputRatio;
      return this;
    }

    public Builder setTargetSignalToNoiseRatio(double targetSignalToNoiseRatio) {
      myTargetSignalToNoiseRatio = targetSignalToNoiseRatio;
      return this;
    }

    public Builder setMaxChangePerSecond(int maxChangePerSecond) {
      myMaxChangePerSecond = maxChangePerSecond;
      return this;
    }

    public Builder setMaxChangePerSample(int maxChangePerSample) {
      myMaxChangePerSample = maxChangePerSample;
      return this;
    }

    public Builder setMaxThreadWaveMagnitude(int maxThreadWaveMagnitude) {
      myMaxThreadWaveMagnitude = maxThreadWaveMagnitude;
      return this;
    }

    public Builder setSampleIntervalLow(int sampleIntervalLow) {
      mySampleIntervalLow = sampleIntervalLow;
      return this;
    }

    public Builder setSampleIntervalHigh(int sampleIntervalHigh) {
      mySampleIntervalHigh = sampleIntervalHigh;
      return this;
    }

    public Builder setThreadMagnitudeMultiplier(double threadMagnitudeMultiplier) {
      myThreadMagnitudeMultiplier = threadMagnitudeMultiplier;
      return this;
    }

    public Builder setThroughputErrorSmoothingFactor(double throughputErrorSmoothingFactor) {
      myThroughputErrorSmoothingFactor = throughputErrorSmoothingFactor;
      return this;
    }

    public Builder setGainExponent(double gainExponent) {
      myGainExponent = gainExponent;
      return this;
    }

    public Builder setMaxSampleError(double maxSampleError) {
      myMaxSampleError = maxSampleError;
      return this;
    }

    public HillClimbingOptions build() {
      return new HillClimbingOptions(myCpuUtilizationThreshold, myMinThreadsCount, myMaxThreadsCount, myWavePeriod, mySamplesToMeasure,
        myTargetThroughputRatio, myTargetSignalToNoiseRatio, myMaxChangePerSecond, myMaxChangePerSample, myMaxThreadWaveMagnitude,
        mySampleIntervalLow, mySampleIntervalHigh, myThreadMagnitudeMultiplier, myThroughputErrorSmoothingFactor, myGainExponent,
        myMaxSampleError);
    }
  }


  public final int cpuUtilizationThreshold;
  public final int minThreadsCount;
  public final int maxThreadsCount;
  public final int wavePeriod;
  public final int samplesToMeasure;
  public final double targetThroughputRatio;
  public final double targetSignalToNoiseRatio;
  public final int maxChangePerSecond;
  public final int maxChangePerSample;
  public final int maxThreadWaveMagnitude;
  public final int sampleIntervalLow;
  public final int sampleIntervalHigh;
  public final double threadMagnitudeMultiplier;
  public final double throughputErrorSmoothingFactor;
  public final double gainExponent;
  public final double maxSampleError;


  public HillClimbingOptions(int cpuUtilizationThreshold, int minThreadsCount, int maxThreadsCount, int wavePeriod,
                             int samplesToMeasure, double targetThroughputRatio, double targetSignalToNoiseRatio,
                             int maxChangePerSecond, int maxChangePerSample, int maxThreadWaveMagnitude,
                             int sampleIntervalLow, int sampleIntervalHigh, double threadMagnitudeMultiplier,
                             double throughputErrorSmoothingFactor, double gainExponent, double maxSampleError) {
    this.cpuUtilizationThreshold = cpuUtilizationThreshold;
    this.minThreadsCount = minThreadsCount;
    this.maxThreadsCount = maxThreadsCount;
    this.wavePeriod = wavePeriod;
    this.samplesToMeasure = samplesToMeasure;
    this.targetThroughputRatio = targetThroughputRatio;
    this.targetSignalToNoiseRatio = targetSignalToNoiseRatio;
    this.maxChangePerSecond = maxChangePerSecond;
    this.maxChangePerSample = maxChangePerSample;
    this.maxThreadWaveMagnitude = maxThreadWaveMagnitude;
    this.sampleIntervalLow = sampleIntervalLow;
    this.sampleIntervalHigh = sampleIntervalHigh;
    this.threadMagnitudeMultiplier = threadMagnitudeMultiplier;
    this.throughputErrorSmoothingFactor = throughputErrorSmoothingFactor;
    this.gainExponent = gainExponent;
    this.maxSampleError = maxSampleError;
  }

  public static Builder builder() {
    return new Builder();
  }
}
