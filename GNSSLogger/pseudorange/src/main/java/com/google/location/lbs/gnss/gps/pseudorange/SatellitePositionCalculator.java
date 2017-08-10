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

import com.google.location.lbs.gnss.gps.pseudorange.SatelliteClockCorrectionCalculator.SatClockCorrection;
import android.location.cts.nano.Ephemeris.GpsEphemerisProto;

/**
 * Class to calculate GPS satellite positions from the ephemeris data
 */
public class SatellitePositionCalculator {
  private static final double SPEED_OF_LIGHT_MPS = 299792458.0;
  private static final double UNIVERSAL_GRAVITATIONAL_PARAMETER_M3_SM2 = 3.986005e14;
  private static final int NUMBER_OF_ITERATIONS_FOR_SAT_POS_CALCULATION = 5;
  private static final double EARTH_ROTATION_RATE_RAD_PER_SEC = 7.2921151467e-5;

  /**
   *
   * Calculates GPS satellite position and velocity from ephemeris including the Sagnac effect
   * starting from unknown user to satellite distance and speed. So we start from an initial guess
   * of the user to satellite range and range rate and iterate to include the Sagnac effect. Few
   * iterations are enough to achieve a satellite position with millimeter accuracy.
   * A {@code PositionAndVelocity} class is returned containing satellite position in meters
   * (x, y and z) and velocity in meters per second (x, y, z)
   *
   * <p>Satelite position and velocity equations are obtained from:
   * http://www.gps.gov/technical/icwg/ICD-GPS-200C.pdf) pages 94 - 101 and
   * http://fenrir.naruoka.org/download/autopilot/note/080205_gps/gps_velocity.pdf
   *
   * @param ephemerisProto parameters of the navigation message
   * @param receiverGpsTowAtTimeOfTransmissionCorrectedSec Receiver estimate of GPS time of week
   *        when signal was transmitted corrected with the satellite clock drift (seconds)
   * @param receiverGpsWeekAtTimeOfTransmission Receiver estimate of GPS week when signal was
   *        transmitted (0-1024+)
   * @param userPosXMeters Last known user x-position (if known) [meters]
   * @param userPosYMeters Last known user y-position (if known) [meters]
   * @param userPosZMeters Last known user z-position (if known) [meters]
   * @throws Exception
   */
  public static PositionAndVelocity calculateSatellitePositionAndVelocityFromEphemeris
  (GpsEphemerisProto ephemerisProto, double receiverGpsTowAtTimeOfTransmissionCorrectedSec,
      int receiverGpsWeekAtTimeOfTransmission,
      double userPosXMeters,
      double userPosYMeters,
      double userPosZMeters) throws Exception {

    // lets start with a first user to sat distance guess of 70 ms and zero velocity
    RangeAndRangeRate userSatRangeAndRate = new RangeAndRangeRate
        (0.070 * SPEED_OF_LIGHT_MPS, 0.0 /* range rate*/);

    // To apply sagnac effect correction, We are starting from an approximate guess of the user to
    // satellite range, iterate 3 times and that should be enough to reach millimeter accuracy
    PositionAndVelocity satPosAndVel = new PositionAndVelocity(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    PositionAndVelocity userPosAndVel =
        new PositionAndVelocity(userPosXMeters, userPosYMeters, userPosZMeters,
            0.0 /* user velocity x*/, 0.0 /* user velocity y*/, 0.0 /* user velocity z */);
    for (int i = 0; i < NUMBER_OF_ITERATIONS_FOR_SAT_POS_CALCULATION; i++) {
      calculateSatellitePositionAndVelocity(ephemerisProto,
          receiverGpsTowAtTimeOfTransmissionCorrectedSec, receiverGpsWeekAtTimeOfTransmission,
          userSatRangeAndRate, satPosAndVel);
      computeUserToSatelliteRangeAndRangeRate(userPosAndVel, satPosAndVel, userSatRangeAndRate);
    }
    return satPosAndVel;
  }

  /**
   * Calculates GPS satellite position and velocity from ephemeris based on the ICD-GPS-200.
   * Satellite position in meters (x, y and z) and velocity in meters per second (x, y, z) are set
   * in the passed {@code PositionAndVelocity} instance.
   *
   * <p>Sources: http://www.gps.gov/technical/icwg/ICD-GPS-200C.pdf) pages 94 - 101 and
   * http://fenrir.naruoka.org/download/autopilot/note/080205_gps/gps_velocity.pdf
   *
   * @param ephemerisProto parameters of the navigation message
   * @param receiverGpsTowAtTimeOfTransmissionCorrected Receiver estimate of GPS time of week when
   *        signal was transmitted corrected with the satellite clock drift (seconds)
   * @param receiverGpsWeekAtTimeOfTransmission Receiver estimate of GPS week when signal was
   *        transmitted (0-1024+)
   * @param userSatRangeAndRate user to satellite range and range rate
   * @param satPosAndVel Satellite position and velocity instance in which the method results will
   * be set
   * @throws Exception
   */
  public static void calculateSatellitePositionAndVelocity(GpsEphemerisProto ephemerisProto,
      double receiverGpsTowAtTimeOfTransmissionCorrected, int receiverGpsWeekAtTimeOfTransmission,
      RangeAndRangeRate userSatRangeAndRate, PositionAndVelocity satPosAndVel) throws Exception {

    // Calculate satellite clock correction (meters), Kepler Eccentric anomaly (radians) and time
    // from ephemeris refrence epoch (tkSec) iteratively
    SatClockCorrection satClockCorrectionValues =
        SatelliteClockCorrectionCalculator.calculateSatClockCorrAndEccAnomAndTkIteratively(
            ephemerisProto, receiverGpsTowAtTimeOfTransmissionCorrected,
            receiverGpsWeekAtTimeOfTransmission);

    double eccentricAnomalyRadians = satClockCorrectionValues.eccentricAnomalyRadians;
    double tkSec = satClockCorrectionValues.timeFromRefEpochSec;

    // True_anomaly (angle from perigee)
    double trueAnomalyRadians = Math.atan2(
            Math.sqrt(1.0 - ephemerisProto.e * ephemerisProto.e)
                    * Math.sin(eccentricAnomalyRadians),
            Math.cos(eccentricAnomalyRadians) - ephemerisProto.e);

    // Argument of latitude of the satellite
    double argumentOfLatitudeRadians = trueAnomalyRadians + ephemerisProto.omega;

    // Radius of satellite orbit
    double radiusOfSatelliteOrbitMeters = ephemerisProto.rootOfA * ephemerisProto.rootOfA
            * (1.0 - ephemerisProto.e * Math.cos(eccentricAnomalyRadians));

    // Radius correction due to second harmonic perturbations of the orbit
    double radiusCorrectionMeters = ephemerisProto.crc
            * Math.cos(2.0 * argumentOfLatitudeRadians) + ephemerisProto.crs
            * Math.sin(2.0 * argumentOfLatitudeRadians);
    // Argument of latitude correction due to second harmonic perturbations of the orbit
    double argumentOfLatitudeCorrectionRadians = ephemerisProto.cuc
            * Math.cos(2.0 * argumentOfLatitudeRadians) + ephemerisProto.cus
            * Math.sin(2.0 * argumentOfLatitudeRadians);
    // Correction to inclination due to second harmonic perturbations of the orbit
    double inclinationCorrectionRadians = ephemerisProto.cic
            * Math.cos(2.0 * argumentOfLatitudeRadians) + ephemerisProto.cis
            * Math.sin(2.0 * argumentOfLatitudeRadians);

    // Corrected radius of satellite orbit
    radiusOfSatelliteOrbitMeters += radiusCorrectionMeters;
    // Corrected argument of latitude
    argumentOfLatitudeRadians += argumentOfLatitudeCorrectionRadians;
    // Corrected inclination
    double inclinationRadians =
            ephemerisProto.i0 + inclinationCorrectionRadians + ephemerisProto.iDot * tkSec;

    // Position in orbital plane
    double xPositionMeters = radiusOfSatelliteOrbitMeters * Math.cos(argumentOfLatitudeRadians);
    double yPositionMeters = radiusOfSatelliteOrbitMeters * Math.sin(argumentOfLatitudeRadians);

    // Corrected longitude of the ascending node (signal propagation time is included to compensate
    // for the Sagnac effect)
    double omegaKRadians = ephemerisProto.omega0
            + (ephemerisProto.omegaDot - EARTH_ROTATION_RATE_RAD_PER_SEC) * tkSec
            - EARTH_ROTATION_RATE_RAD_PER_SEC
            * (ephemerisProto.toe + userSatRangeAndRate.rangeMeters / SPEED_OF_LIGHT_MPS);

    // compute the resulting satellite position
    double satPosXMeters = xPositionMeters * Math.cos(omegaKRadians) - yPositionMeters
            * Math.cos(inclinationRadians) * Math.sin(omegaKRadians);
    double satPosYMeters = xPositionMeters * Math.sin(omegaKRadians) + yPositionMeters
            * Math.cos(inclinationRadians) * Math.cos(omegaKRadians);
    double satPosZMeters = yPositionMeters * Math.sin(inclinationRadians);

    // Satellite Velocity Computation using the broadcast ephemeris
    // http://fenrir.naruoka.org/download/autopilot/note/080205_gps/gps_velocity.pdf
    // Units are not added in some of the variable names to have the same name as the ICD-GPS200
    // Semi-major axis of orbit (meters)
    double a = ephemerisProto.rootOfA * ephemerisProto.rootOfA;
    // Computed mean motion (radians/seconds)
    double n0 = Math.sqrt(UNIVERSAL_GRAVITATIONAL_PARAMETER_M3_SM2 / (a * a * a));
    // Corrected mean motion (radians/seconds)
    double n = n0 + ephemerisProto.deltaN;
    // Derivative of mean anomaly (radians/seconds)
    double meanAnomalyDotRadPerSec = n;
    // Derivative of eccentric anomaly (radians/seconds)
    double eccentricAnomalyDotRadPerSec =
            meanAnomalyDotRadPerSec / (1.0 - ephemerisProto.e * Math.cos(eccentricAnomalyRadians));
    // Derivative of true anomaly (radians/seconds)
    double trueAnomalydotRadPerSec = Math.sin(eccentricAnomalyRadians)
            * eccentricAnomalyDotRadPerSec
            * (1.0 + ephemerisProto.e * Math.cos(trueAnomalyRadians)) / (
            Math.sin(trueAnomalyRadians)
                    * (1.0 - ephemerisProto.e * Math.cos(eccentricAnomalyRadians)));
    // Derivative of argument of latitude (radians/seconds)
    double argumentOfLatitudeDotRadPerSec = trueAnomalydotRadPerSec + 2.0 * (ephemerisProto.cus
            * Math.cos(2.0 * argumentOfLatitudeRadians) - ephemerisProto.cuc
            * Math.sin(2.0 * argumentOfLatitudeRadians)) * trueAnomalydotRadPerSec;
    // Derivative of radius of satellite orbit (m/s)
    double radiusOfSatelliteOrbitDotMPerSec = a * ephemerisProto.e
            * Math.sin(eccentricAnomalyRadians) * n
            / (1.0 - ephemerisProto.e * Math.cos(eccentricAnomalyRadians)) + 2.0 * (
            ephemerisProto.crs * Math.cos(2.0 * argumentOfLatitudeRadians)
                    - ephemerisProto.crc * Math.sin(2.0 * argumentOfLatitudeRadians))
            * trueAnomalydotRadPerSec;
    // Derivative of the inclination (radians/seconds)
    double inclinationDotRadPerSec = ephemerisProto.iDot + (ephemerisProto.cis
            * Math.cos(2.0 * argumentOfLatitudeRadians) - ephemerisProto.cic
            * Math.sin(2.0 * argumentOfLatitudeRadians)) * 2.0 * trueAnomalydotRadPerSec;

    double xVelocityMPS = radiusOfSatelliteOrbitDotMPerSec * Math.cos(argumentOfLatitudeRadians)
            - yPositionMeters * argumentOfLatitudeDotRadPerSec;
    double yVelocityMPS = radiusOfSatelliteOrbitDotMPerSec * Math.sin(argumentOfLatitudeRadians)
            + xPositionMeters * argumentOfLatitudeDotRadPerSec;

    // Corrected rate of right ascension including compensation for the Sagnac effect
    double omegaDotRadPerSec = ephemerisProto.omegaDot - EARTH_ROTATION_RATE_RAD_PER_SEC
            * (1.0 + userSatRangeAndRate.rangeRateMetersPerSec / SPEED_OF_LIGHT_MPS);
    // compute the resulting satellite velocity
    double satVelXMPS =
            (xVelocityMPS - yPositionMeters * Math.cos(inclinationRadians) * omegaDotRadPerSec)
                    * Math.cos(omegaKRadians) - (xPositionMeters * omegaDotRadPerSec + yVelocityMPS
                    * Math.cos(inclinationRadians) - yPositionMeters * Math.sin(inclinationRadians)
                    * inclinationDotRadPerSec) * Math.sin(omegaKRadians);
    double satVelYMPS =
            (xVelocityMPS - yPositionMeters * Math.cos(inclinationRadians) * omegaDotRadPerSec)
                    * Math.sin(omegaKRadians) + (xPositionMeters * omegaDotRadPerSec + yVelocityMPS
                    * Math.cos(inclinationRadians) - yPositionMeters * Math.sin(inclinationRadians)
                    * inclinationDotRadPerSec) * Math.cos(omegaKRadians);
    double satVelZMPS = yVelocityMPS * Math.sin(inclinationRadians) + yPositionMeters
            * Math.cos(inclinationRadians) * inclinationDotRadPerSec;

    satPosAndVel.positionXMeters = satPosXMeters;
    satPosAndVel.positionYMeters = satPosYMeters;
    satPosAndVel.positionZMeters = satPosZMeters;
    satPosAndVel.velocityXMetersPerSec = satVelXMPS;
    satPosAndVel.velocityYMetersPerSec = satVelYMPS;
    satPosAndVel.velocityZMetersPerSec = satVelZMPS;
  }

  /**
   * Computes and sets the passed {@code RangeAndRangeRate} instance containing user to satellite
   * range (meters) and range rate (m/s) given the user position (ECEF meters), user velocity (m/s),
   * satellite position (ECEF meters) and satellite velocity (m/s).
   */
  private static void computeUserToSatelliteRangeAndRangeRate(PositionAndVelocity userPosAndVel,
      PositionAndVelocity satPosAndVel, RangeAndRangeRate rangeAndRangeRate) {
    double dXMeters = satPosAndVel.positionXMeters - userPosAndVel.positionXMeters;
    double dYMeters = satPosAndVel.positionYMeters - userPosAndVel.positionYMeters;
    double dZMeters = satPosAndVel.positionZMeters - userPosAndVel.positionZMeters;
    // range in meters
    double rangeMeters = Math.sqrt(dXMeters * dXMeters + dYMeters * dYMeters + dZMeters * dZMeters);
    // range rate in meters / second
    double rangeRateMetersPerSec =
        ((userPosAndVel.velocityXMetersPerSec - satPosAndVel.velocityXMetersPerSec) * dXMeters
        + (userPosAndVel.velocityYMetersPerSec - satPosAndVel.velocityYMetersPerSec) * dYMeters
        + (userPosAndVel.velocityZMetersPerSec - satPosAndVel.velocityZMetersPerSec) * dZMeters)
        / rangeMeters;
    rangeAndRangeRate.rangeMeters = rangeMeters;
    rangeAndRangeRate.rangeRateMetersPerSec = rangeRateMetersPerSec;
  }

  /**
   *
   * A class containing position values (x, y, z) in meters and velocity values (x, y, z) in meters
   * per seconds
   */
  public static class PositionAndVelocity {
    /**
     * x - position in meters
     */
    public double positionXMeters;
    /**
     * y - position in meters
     */
    public double positionYMeters;
    /**
     * z - position in meters
     */
    public double positionZMeters;
    /**
     * x - velocity in meters
     */
    public double velocityXMetersPerSec;
    /**
     * y - velocity in meters
     */
    public double velocityYMetersPerSec;
    /**
     * z - velocity in meters
     */
    public double velocityZMetersPerSec;

    /**
     * Constructor
     */
    public PositionAndVelocity(double positionXMeters,
        double positionYMeters,
        double positionZMeters,
        double velocityXMetersPerSec,
        double velocityYMetersPerSec,
        double velocityZMetersPerSec) {
      this.positionXMeters = positionXMeters;
      this.positionYMeters = positionYMeters;
      this.positionZMeters = positionZMeters;
      this.velocityXMetersPerSec = velocityXMetersPerSec;
      this.velocityYMetersPerSec = velocityYMetersPerSec;
      this.velocityZMetersPerSec = velocityZMetersPerSec;
    }
  }

  /**
   *
   * A class containing range of satellite to user in meters and range rate in meters per seconds
   */
  public static class RangeAndRangeRate {
    /**
     * Range in meters
     */
    public double rangeMeters;
    /**
     * Range rate in meters per seconds
     */
    public double rangeRateMetersPerSec;

    /**
     * Constructor
     */
    public RangeAndRangeRate(double rangeMeters, double rangeRateMetersPerSec) {
      this.rangeMeters = rangeMeters;
      this.rangeRateMetersPerSec = rangeRateMetersPerSec;
    }
  }
}
