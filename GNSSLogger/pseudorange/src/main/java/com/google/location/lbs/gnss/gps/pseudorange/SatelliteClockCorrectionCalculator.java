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

import android.location.cts.nano.Ephemeris.GpsEphemerisProto;
/**
 * Calculates the GPS satellite clock correction based on parameters observed from the navigation
 * message
 * <p>Source: Page 88 - 90 of the ICD-GPS 200
 */
public class SatelliteClockCorrectionCalculator {
  private static final double SPEED_OF_LIGHT_MPS = 299792458.0;
  private static final double EARTH_UNIVERSAL_GRAVITATIONAL_CONSTANT_M3_SM2 = 3.986005e14;
  private static final double RELATIVISTIC_CONSTANT_F = -4.442807633e-10;
  private static final int SECONDS_IN_WEEK = 604800;
  private static final double ACCURACY_TOLERANCE = 1.0e-11;
  private static final int MAX_ITERATIONS = 100;

  /**
   * Computes the GPS satellite clock correction term in meters iteratively following page 88 - 90
   * and 98 - 100 of the ICD GPS 200. The method returns a pair of satellite clock correction in
   * meters and Kepler Eccentric Anomaly in Radians.
   *
   * @param ephemerisProto parameters of the navigation message
   * @param receiverGpsTowAtTimeOfTransmission Reciever estimate of GPS time of week when signal was
   *        transmitted (seconds)
   * @param receiverGpsWeekAtTimeOfTrasnmission Receiver estimate of GPS week when signal was
   *        transmitted (0-1024+)
   * @throws Exception
   */

  public static SatClockCorrection calculateSatClockCorrAndEccAnomAndTkIteratively(
          GpsEphemerisProto ephemerisProto, double receiverGpsTowAtTimeOfTransmission,
          double receiverGpsWeekAtTimeOfTrasnmission) throws Exception {
    // Units are not added in the variable names to have the same name as the ICD-GPS200
    // Mean anomaly (radians)
    double meanAnomalyRad;
    // Kepler's Equation for Eccentric Anomaly iteratively (Radians)
    double eccentricAnomalyRad;
    // Semi-major axis of orbit (meters)
    double a = ephemerisProto.rootOfA * ephemerisProto.rootOfA;
    // Computed mean motion (radians/seconds)
    double n0 = Math.sqrt(EARTH_UNIVERSAL_GRAVITATIONAL_CONSTANT_M3_SM2 / (a * a * a));
    // Corrected mean motion (radians/seconds)
    double n = n0 + ephemerisProto.deltaN;
    // In the following, Receiver GPS week and ephemeris GPS week are used to correct for week
    // rollover when calculating the time from clock reference epoch (tcSec)
    double timeOfTransmissionIncludingRxWeekSec =
            receiverGpsWeekAtTimeOfTrasnmission * SECONDS_IN_WEEK + receiverGpsTowAtTimeOfTransmission;
    // time from clock reference epoch (seconds) page 88 ICD-GPS200
    double tcSec = timeOfTransmissionIncludingRxWeekSec
            - (ephemerisProto.week * SECONDS_IN_WEEK + ephemerisProto.toc);
    // Correction for week rollover
    tcSec = fixWeekRollover(tcSec);
    double oldEcentricAnomalyRad = 0.0;
    double newSatClockCorrectionSeconds = 0.0;
    double relativisticCorrection = 0.0;
    double changeInSatClockCorrection = 0.0;
    // Initial satellite clock correction (unknown relativistic correction). Iterate to correct
    // with the relativistic effect and obtain a stable
    final double initSatClockCorrectionSeconds = ephemerisProto.af0
            + ephemerisProto.af1 * tcSec
            + ephemerisProto.af2 * tcSec * tcSec - ephemerisProto.tgd;
    double satClockCorrectionSeconds = initSatClockCorrectionSeconds;
    double tkSec;
    int satClockCorrectionsCounter = 0;
    do {
      int eccentricAnomalyCounter = 0;
      // time from ephemeris reference epoch (seconds) page 98 ICD-GPS200
      tkSec = timeOfTransmissionIncludingRxWeekSec - (
              ephemerisProto.week * SECONDS_IN_WEEK + ephemerisProto.toe
                      + satClockCorrectionSeconds);
      // Correction for week rollover
      tkSec = fixWeekRollover(tkSec);
      // Mean anomaly (radians)
      meanAnomalyRad = ephemerisProto.m0 + n * tkSec;
      // eccentric anomaly (radians)
      eccentricAnomalyRad = meanAnomalyRad;
      // Iteratively solve for Kepler's eccentric anomaly according to ICD-GPS200 page 99
      do {
        oldEcentricAnomalyRad = eccentricAnomalyRad;
        eccentricAnomalyRad =
                meanAnomalyRad + ephemerisProto.e * Math.sin(eccentricAnomalyRad);
        eccentricAnomalyCounter++;
        if (eccentricAnomalyCounter > MAX_ITERATIONS) {
          throw new Exception("Kepler Eccentric Anomaly calculation did not converge in "
                  + MAX_ITERATIONS + " iterations");
        }
      } while (Math.abs(oldEcentricAnomalyRad - eccentricAnomalyRad) > ACCURACY_TOLERANCE);
      // relativistic correction term (seconds)
      relativisticCorrection = RELATIVISTIC_CONSTANT_F * ephemerisProto.e
              * ephemerisProto.rootOfA * Math.sin(eccentricAnomalyRad);
      // satellite clock correction including relativistic effect
      newSatClockCorrectionSeconds = initSatClockCorrectionSeconds + relativisticCorrection;
      changeInSatClockCorrection =
              Math.abs(satClockCorrectionSeconds - newSatClockCorrectionSeconds);
      satClockCorrectionSeconds = newSatClockCorrectionSeconds;
      satClockCorrectionsCounter++;
      if (satClockCorrectionsCounter > MAX_ITERATIONS) {
        throw new Exception("Satellite Clock Correction calculation did not converge in "
                + MAX_ITERATIONS + " iterations");
      }
    } while (changeInSatClockCorrection > ACCURACY_TOLERANCE);
    tkSec = timeOfTransmissionIncludingRxWeekSec - (
            ephemerisProto.week * SECONDS_IN_WEEK + ephemerisProto.toe
                    + satClockCorrectionSeconds);
    // return satellite clock correction (meters) and Kepler Eccentric Anomaly in Radians
    return new SatClockCorrection(satClockCorrectionSeconds * SPEED_OF_LIGHT_MPS,
            eccentricAnomalyRad, tkSec);
  }

  /**
   * Calculates Satellite Clock Error Rate in (meters/second) by subtracting the Satellite
   * Clock Error Values at t+0.5s and t-0.5s.
   *
   * <p>This approximation is more accurate than differentiating because both the orbital
   * and relativity terms have non-linearities that are not easily differentiable.
   */
  public static double calculateSatClockCorrErrorRate(
      GpsEphemerisProto ephemerisProto, double receiverGpsTowAtTimeOfTransmissionSeconds,
      double receiverGpsWeekAtTimeOfTrasnmission) throws Exception {
    SatClockCorrection satClockCorrectionPlus = calculateSatClockCorrAndEccAnomAndTkIteratively(
        ephemerisProto, receiverGpsTowAtTimeOfTransmissionSeconds + 0.5,
        receiverGpsWeekAtTimeOfTrasnmission);
    SatClockCorrection satClockCorrectionMinus = calculateSatClockCorrAndEccAnomAndTkIteratively(
        ephemerisProto, receiverGpsTowAtTimeOfTransmissionSeconds - 0.5,
        receiverGpsWeekAtTimeOfTrasnmission);
    double satelliteClockErrorRate = satClockCorrectionPlus.satelliteClockCorrectionMeters
        - satClockCorrectionMinus.satelliteClockCorrectionMeters;
    return satelliteClockErrorRate;
  }

  /**
   * Method to check for week rollover according to ICD-GPS 200 page 98.
   *
   * <p>Result should be between -302400 and 302400 if the ephemeris is within one week of
   * transmission, otherwise it is adjusted to the correct range
   */
  private static double fixWeekRollover(double time) {
    double correctedTime = time;
    if (time > SECONDS_IN_WEEK / 2.0) {
      correctedTime = time - SECONDS_IN_WEEK;
    }
    if (time < -SECONDS_IN_WEEK / 2.0) {
      correctedTime = time + SECONDS_IN_WEEK;
    }
    return correctedTime;
  }

  /**
   *
   * Class containing the satellite clock correction parameters: The satellite clock correction in
   * meters, Kepler Eccentric Anomaly in Radians and the time from the reference epoch in seconds.
   */
  public static class SatClockCorrection {
    /**
     *  Satellite clock correction in meters
     */
    public final double satelliteClockCorrectionMeters;
    /**
     * Kepler Eccentric Anomaly in Radians
     */
    public final double eccentricAnomalyRadians;
    /**
     *  Time from the reference epoch in Seconds
     */
    public final double timeFromRefEpochSec;

    /**
     * Constructor
     */
    public SatClockCorrection(double satelliteClockCorrectionMeters, double eccentricAnomalyRadians,
        double timeFromRefEpochSec) {
      this.satelliteClockCorrectionMeters = satelliteClockCorrectionMeters;
      this.eccentricAnomalyRadians = eccentricAnomalyRadians;
      this.timeFromRefEpochSec = timeFromRefEpochSec;
    }
  }
}
