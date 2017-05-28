package org.hillclimbing;

public final class HillClimbingOptions {

  public static final HillClimbingOptions DEFAULT_OPTIONS = builder().build();

  public static final class Builder {
    private int cpuUtilizationThreshold = 95;
    private int minThreadsCount = Runtime.getRuntime().availableProcessors();
    private int maxThreadsCount = 200;
    private int wavePeriod = 4;
    private int samplesToMeasure = wavePeriod * 8;
    private double targetThroughputRatio = 15 / 100.0;
    private double targetSignalToNoiseRatio = 300 / 100.0;
    private int maxChangePerSecond = 4;
    private int maxChangePerSample = 20;
    private int maxThreadWaveMagnitude = 20;
    private int sampleIntervalLow = 10;
    private int sampleIntervalHigh = 200;
    private double threadMagnitudeMultiplier = 100 / 100.0;
    private double throughputErrorSmoothingFactor = 1 / 100.0;
    private double gainExponent = 200 / 100.0;
    private double maxSampleError = 15 / 100.0;

    private Builder() {
    }

    public Builder setCpuUtilizationThreshold(int cpuUtilizationThreshold) {
      this.cpuUtilizationThreshold = cpuUtilizationThreshold;
      return this;
    }

    public Builder setMinThreadsCount(int minThreadsCount) {
      this.minThreadsCount = minThreadsCount;
      return this;
    }

    public Builder setMaxThreadsCount(int maxThreadsCount) {
      this.maxThreadsCount = maxThreadsCount;
      return this;
    }

    public Builder setWavePeriod(int wavePeriod) {
      this.wavePeriod = wavePeriod;
      return this;
    }

    public Builder setSamplesToMeasure(int samplesToMeasure) {
      this.samplesToMeasure = samplesToMeasure;
      return this;
    }

    public Builder setTargetThroughputRatio(double targetThroughputRatio) {
      this.targetThroughputRatio = targetThroughputRatio;
      return this;
    }

    public Builder setTargetSignalToNoiseRatio(double targetSignalToNoiseRatio) {
      this.targetSignalToNoiseRatio = targetSignalToNoiseRatio;
      return this;
    }

    public Builder setMaxChangePerSecond(int maxChangePerSecond) {
      this.maxChangePerSecond = maxChangePerSecond;
      return this;
    }

    public Builder setMaxChangePerSample(int maxChangePerSample) {
      this.maxChangePerSample = maxChangePerSample;
      return this;
    }

    public Builder setMaxThreadWaveMagnitude(int maxThreadWaveMagnitude) {
      this.maxThreadWaveMagnitude = maxThreadWaveMagnitude;
      return this;
    }

    public Builder setSampleIntervalLow(int sampleIntervalLow) {
      this.sampleIntervalLow = sampleIntervalLow;
      return this;
    }

    public Builder setSampleIntervalHigh(int sampleIntervalHigh) {
      this.sampleIntervalHigh = sampleIntervalHigh;
      return this;
    }

    public Builder setThreadMagnitudeMultiplier(double threadMagnitudeMultiplier) {
      this.threadMagnitudeMultiplier = threadMagnitudeMultiplier;
      return this;
    }

    public Builder setThroughputErrorSmoothingFactor(double throughputErrorSmoothingFactor) {
      this.throughputErrorSmoothingFactor = throughputErrorSmoothingFactor;
      return this;
    }

    public Builder setGainExponent(double gainExponent) {
      this.gainExponent = gainExponent;
      return this;
    }

    public Builder setMaxSampleError(double maxSampleError) {
      this.maxSampleError = maxSampleError;
      return this;
    }

    public HillClimbingOptions build() {
      return new HillClimbingOptions(cpuUtilizationThreshold, minThreadsCount, maxThreadsCount, wavePeriod, samplesToMeasure,
        targetThroughputRatio, targetSignalToNoiseRatio, maxChangePerSecond, maxChangePerSample, maxThreadWaveMagnitude,
        sampleIntervalLow, sampleIntervalHigh, threadMagnitudeMultiplier, throughputErrorSmoothingFactor, gainExponent,
        maxSampleError);
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
