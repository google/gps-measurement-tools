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
 * <p>The container extends {@link GpsMeasurement} to additionally include
 * {@link #pseudorangeMeters} and {@link #pseudorangeUncertaintyMeters}.
 */
class GpsMeasurementWithRangeAndUncertainty extends GpsMeasurement {

  /** Pseudorange measurement (meters) */
  public final double pseudorangeMeters;

  /** Pseudorange uncertainty (meters) */
  public final double pseudorangeUncertaintyMeters;
  
  public GpsMeasurementWithRangeAndUncertainty(GpsMeasurement another, double pseudorangeMeters,
      double pseudorangeUncertaintyMeters) {
    super(another);
    this.pseudorangeMeters = pseudorangeMeters;
    this.pseudorangeUncertaintyMeters = pseudorangeUncertaintyMeters;
  } 

}
