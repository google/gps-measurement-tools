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

import java.util.List;

/**
 * Interface for smoothing a list of {@link GpsMeasurementWithRangeAndUncertainty} instances
 * received at a point of time.
 */
interface PseudorangeSmoother {

  /**
   * Takes an input list of {@link GpsMeasurementWithRangeAndUncertainty} instances and returns a
   * new list that contains smoothed pseudorange measurements.
   *
   * <p>The input list is of size {@link GpsNavigationMessageStore#MAX_NUMBER_OF_SATELLITES} with
   * not visible GPS satellites having null entries, and the returned new list is of the same size.
   *
   * <p>The method does not modify the input list.
   */
  List<GpsMeasurementWithRangeAndUncertainty> updatePseudorangeSmoothingResult(
          List<GpsMeasurementWithRangeAndUncertainty> usefulSatellitesToGPSReceiverMeasurements);
}
