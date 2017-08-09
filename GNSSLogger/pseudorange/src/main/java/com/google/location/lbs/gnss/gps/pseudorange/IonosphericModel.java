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

import com.google.location.lbs.gnss.gps.pseudorange.Ecef2LlaConverter.GeodeticLlaValues;
import com.google.location.lbs.gnss.gps.pseudorange.EcefToTopocentricConverter.TopocentricAEDValues;

/**
 * Calculates the Ionospheric correction of the pseudorange given the {@code userPosition},
 * {@code satellitePosition}, {@code gpsTimeSeconds} and the ionospheric parameters sent by the
 * satellite {@code alpha} and {@code beta}
 *
 * <p>Source: http://www.navipedia.net/index.php/Klobuchar_Ionospheric_Model and
 * http://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=4104345 and
 * http://www.ion.org/museum/files/ACF2A4.pdf
 */
public class IonosphericModel {
  /** Center frequency of the L1 band in Hz. */
  public static final double L1_FREQ_HZ = 10.23 * 1e6 * 154;
  /** Center frequency of the L2 band in Hz. */
  public static final double L2_FREQ_HZ = 10.23 * 1e6 * 120;
  /** Center frequency of the L5 band in Hz. */
  public static final double L5_FREQ_HZ = 10.23 * 1e6 * 115;
      
  private static final double SECONDS_PER_DAY = 86400.0;
  private static final double PERIOD_OF_DELAY_TRHESHOLD_SECONDS = 72000.0;
  private static final double IPP_LATITUDE_THRESHOLD_SEMI_CIRCLE = 0.416;
  private static final double DC_TERM = 5.0e-9;
  private static final double NORTH_GEOMAGNETIC_POLE_LONGITUDE_RADIANS = 5.08;
  private static final double GEOMETRIC_LATITUDE_CONSTANT = 0.064;
  private static final int DELAY_PHASE_TIME_CONSTANT_SECONDS = 50400;
  private static final int IONO_0_IDX = 0;
  private static final int IONO_1_IDX = 1;
  private static final int IONO_2_IDX = 2;
  private static final int IONO_3_IDX = 3;

  /**
   * Calculates the Ionospheric correction of the pseudorane in seconds using the Klobuchar
   * Ionospheric model.
   */
  public static double ionoKloboucharCorrectionSeconds(
      double[] userPositionECEFMeters,
      double[] satellitePositionECEFMeters,
      double gpsTOWSeconds,
      double[] alpha,
      double[] beta,
      double frequencyHz) {

    TopocentricAEDValues elevationAndAzimuthRadians = EcefToTopocentricConverter
        .calculateElAzDistBetween2Points(userPositionECEFMeters, satellitePositionECEFMeters);
    double elevationSemiCircle = elevationAndAzimuthRadians.elevationRadians / Math.PI;
    double azimuthSemiCircle = elevationAndAzimuthRadians.azimuthRadians / Math.PI;
    GeodeticLlaValues latLngAlt = Ecef2LlaConverter.convertECEFToLLACloseForm(
        userPositionECEFMeters[0], userPositionECEFMeters[1], userPositionECEFMeters[2]);
    double latitudeUSemiCircle = latLngAlt.latitudeRadians / Math.PI;
    double longitudeUSemiCircle = latLngAlt.longitudeRadians / Math.PI;

    // earth's centered angle (semi-circles)
    double earthCentredAngleSemiCirle = 0.0137 / (elevationSemiCircle + 0.11) - 0.022;

    // latitude of the Ionospheric Pierce Point (IPP) (semi-circles)
    double latitudeISemiCircle =
        latitudeUSemiCircle + earthCentredAngleSemiCirle * Math.cos(azimuthSemiCircle * Math.PI);

    if (latitudeISemiCircle > IPP_LATITUDE_THRESHOLD_SEMI_CIRCLE) {
      latitudeISemiCircle = IPP_LATITUDE_THRESHOLD_SEMI_CIRCLE;
    } else if (latitudeISemiCircle < -IPP_LATITUDE_THRESHOLD_SEMI_CIRCLE) {
      latitudeISemiCircle = -IPP_LATITUDE_THRESHOLD_SEMI_CIRCLE;
    }

    // geodetic longitude of the Ionospheric Pierce Point (IPP) (semi-circles)
    double longitudeISemiCircle = longitudeUSemiCircle + earthCentredAngleSemiCirle
        * Math.sin(azimuthSemiCircle * Math.PI) / Math.cos(latitudeISemiCircle * Math.PI);

    // geomagnetic latitude of the Ionospheric Pierce Point (IPP) (semi-circles)
    double geomLatIPPSemiCircle = latitudeISemiCircle + GEOMETRIC_LATITUDE_CONSTANT
        * Math.cos(longitudeISemiCircle * Math.PI - NORTH_GEOMAGNETIC_POLE_LONGITUDE_RADIANS);

    // local time (sec) at the Ionospheric Pierce Point (IPP)
    double localTimeSeconds = SECONDS_PER_DAY / 2.0 * longitudeISemiCircle + gpsTOWSeconds;
    localTimeSeconds %= SECONDS_PER_DAY;
    if (localTimeSeconds < 0) {
      localTimeSeconds += SECONDS_PER_DAY;
    }

    // amplitude of the ionospheric delay (seconds)
    double amplitudeOfDelaySeconds = alpha[IONO_0_IDX] + alpha[IONO_1_IDX] * geomLatIPPSemiCircle
        + alpha[IONO_2_IDX] * geomLatIPPSemiCircle * geomLatIPPSemiCircle + alpha[IONO_3_IDX]
        * geomLatIPPSemiCircle * geomLatIPPSemiCircle * geomLatIPPSemiCircle;
    if (amplitudeOfDelaySeconds < 0) {
      amplitudeOfDelaySeconds = 0;
    }

    // period of ionospheric delay
    double periodOfDelaySeconds = beta[IONO_0_IDX] + beta[IONO_1_IDX] * geomLatIPPSemiCircle
        + beta[IONO_2_IDX] * geomLatIPPSemiCircle * geomLatIPPSemiCircle + beta[IONO_3_IDX]
        * geomLatIPPSemiCircle * geomLatIPPSemiCircle * geomLatIPPSemiCircle;
    if (periodOfDelaySeconds < PERIOD_OF_DELAY_TRHESHOLD_SECONDS) {
      periodOfDelaySeconds = PERIOD_OF_DELAY_TRHESHOLD_SECONDS;
    }

    // phase of ionospheric delay
    double phaseOfDelayRadians =
        2 * Math.PI * (localTimeSeconds - DELAY_PHASE_TIME_CONSTANT_SECONDS) / periodOfDelaySeconds;

    // slant factor
    double slantFactor = 1.0 + 16.0 * Math.pow(0.53 - elevationSemiCircle, 3);

    // ionospheric time delay (seconds)
    double ionoDelaySeconds;

    if (Math.abs(phaseOfDelayRadians) >= Math.PI / 2.0) {
      ionoDelaySeconds = DC_TERM * slantFactor;
    } else {
      ionoDelaySeconds = (DC_TERM
          + (1 - Math.pow(phaseOfDelayRadians, 2) / 2.0 + Math.pow(phaseOfDelayRadians, 4) / 24.0)
          * amplitudeOfDelaySeconds) * slantFactor;
    }
    
    // apply factor for frequency bands other than L1 
    ionoDelaySeconds *= (L1_FREQ_HZ * L1_FREQ_HZ) / (frequencyHz * frequencyHz);

    return ionoDelaySeconds;
  }
}
