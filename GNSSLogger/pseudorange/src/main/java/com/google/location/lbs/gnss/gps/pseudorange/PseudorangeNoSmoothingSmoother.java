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

import java.util.Collections;
import java.util.List;

/**
 * An implementation of {@link PseudorangeSmoother} that performs no smoothing.
 *
 * <p> A new list of {@link GpsMeasurementWithRangeAndUncertainty} instances is filled with a copy
 * of the input list.
 */
class PseudorangeNoSmoothingSmoother implements PseudorangeSmoother {

  @Override
  public List<GpsMeasurementWithRangeAndUncertainty> updatePseudorangeSmoothingResult(
      List<GpsMeasurementWithRangeAndUncertainty> usefulSatellitesToGPSReceiverMeasurements) {
    return Collections.unmodifiableList(usefulSatellitesToGPSReceiverMeasurements);
  }

}
