/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.location.lbs.gnss.gps.pseudorange;

/**
 * A container for the received GPS measurements for a single satellite.
 *
 * <p>The GPS receiver measurements includes: satellite PRN, accumulated delta range in meters,
 * accumulated delta range state (boolean), pseudorange rate in meters per second, received signal
 * to noise ratio dB, accumulated delta range uncertainty in meters, pseudorange rate uncertainty in
 * meters per second.
 */
class GpsMeasurement {
  /** Time since GPS week start (Nano seconds) */
  public final long arrivalTimeSinceGpsWeekNs;

  /** Accumulated delta range (meters) */
  public final double accumulatedDeltaRangeMeters;

  /** Accumulated delta range state */
  public final boolean validAccumulatedDeltaRangeMeters; 

  /** Pseudorange rate measurement (meters per second) */
  public final double pseudorangeRateMps;  

  /** Signal to noise ratio (dB) */
  public final double signalToNoiseRatioDb;  

  /** Accumulated Delta Range Uncertainty (meters) */
  public final double accumulatedDeltaRangeUncertaintyMeters;

  /** Pseudorange rate uncertainty (meter per seconds) */
  public final double pseudorangeRateUncertaintyMps;
  
  public GpsMeasurement(long arrivalTimeSinceGpsWeekNs, double accumulatedDeltaRangeMeters,
      boolean validAccumulatedDeltaRangeMeters, double pseudorangeRateMps,
      double signalToNoiseRatioDb, double accumulatedDeltaRangeUncertaintyMeters,
      double pseudorangeRateUncertaintyMps) {
    this.arrivalTimeSinceGpsWeekNs = arrivalTimeSinceGpsWeekNs;
    this.accumulatedDeltaRangeMeters = accumulatedDeltaRangeMeters;
    this.validAccumulatedDeltaRangeMeters = validAccumulatedDeltaRangeMeters;
    this.pseudorangeRateMps = pseudorangeRateMps;
    this.signalToNoiseRatioDb = signalToNoiseRatioDb;
    this.accumulatedDeltaRangeUncertaintyMeters = accumulatedDeltaRangeUncertaintyMeters;
    this.pseudorangeRateUncertaintyMps = pseudorangeRateUncertaintyMps;
  }  

  protected GpsMeasurement(GpsMeasurement another) {
    this(another.arrivalTimeSinceGpsWeekNs, another.accumulatedDeltaRangeMeters,
        another.validAccumulatedDeltaRangeMeters, another.pseudorangeRateMps,
        another.signalToNoiseRatioDb, another.accumulatedDeltaRangeUncertaintyMeters,
        another.pseudorangeRateUncertaintyMps);
  } 
}
