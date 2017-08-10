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
 * Converts ECEF (Earth Centered Earth Fixed) Cartesian coordinates to LLA (latitude, longitude,
 * and altitude).
 *
 * <p> Source: reference from Mathworks: https://microem.ru/files/2012/08/GPS.G1-X-00006.pdf
 * http://www.mathworks.com/help/aeroblks/ecefpositiontolla.html
 */

public class Ecef2LlaConverter {
  // WGS84 Ellipsoid Parameters
  private static final double EARTH_SEMI_MAJOR_AXIS_METERS = 6378137.0;
  private static final double ECCENTRICITY = 8.1819190842622e-2;
  private static final double INVERSE_FLATENNING = 298.257223563;
  private static final double MIN_MAGNITUDE_METERS = 1.0e-22;
  private static final double MAX_ITERATIONS = 15;
  private static final double RESIDUAL_TOLERANCE = 1.0e-6;
  private static final double SEMI_MINOR_AXIS_METERS =
      Math.sqrt(Math.pow(EARTH_SEMI_MAJOR_AXIS_METERS, 2) * (1 - Math.pow(ECCENTRICITY, 2)));
  private static final double SECOND_ECCENTRICITY = Math.sqrt(
      (Math.pow(EARTH_SEMI_MAJOR_AXIS_METERS, 2) - Math.pow(SEMI_MINOR_AXIS_METERS, 2))
      / Math.pow(SEMI_MINOR_AXIS_METERS, 2));
  private static final double ECEF_NEAR_POLE_THRESHOLD_METERS = 1.0;

  /**
  * Converts ECEF (Earth Centered Earth Fixed) Cartesian coordinates to LLA (latitude,
  * longitude, and altitude) using the close form approach
  *
  * <p>Inputs are cartesian coordinates x,y,z
  *
  * <p>Output is GeodeticLlaValues class containing geodetic latitude (radians), geodetic longitude
  * (radians), height above WGS84 ellipsoid (m)}
  */
  public static GeodeticLlaValues convertECEFToLLACloseForm(double ecefXMeters, double ecefYMeters,
      double ecefZMeters) {

    // Auxiliary parameters
    double pMeters = Math.sqrt(Math.pow(ecefXMeters, 2) + Math.pow(ecefYMeters, 2));
    double thetaRadians =
        Math.atan2(EARTH_SEMI_MAJOR_AXIS_METERS * ecefZMeters, SEMI_MINOR_AXIS_METERS * pMeters);

    double lngRadians = Math.atan2(ecefYMeters, ecefXMeters);
    // limit longitude to range of 0 to 2Pi
    lngRadians = lngRadians % (2 * Math.PI);

    final double sinTheta = Math.sin(thetaRadians);
    final double cosTheta = Math.cos(thetaRadians);
    final double tempY = ecefZMeters
        + Math.pow(SECOND_ECCENTRICITY, 2) * SEMI_MINOR_AXIS_METERS * Math.pow(sinTheta, 3);
    final double tempX = pMeters
        - Math.pow(ECCENTRICITY, 2) * EARTH_SEMI_MAJOR_AXIS_METERS * (Math.pow(cosTheta, 3));
    double latRadians = Math.atan2(tempY, tempX);
    // Radius of curvature in the vertical prime
    double curvatureRadius = EARTH_SEMI_MAJOR_AXIS_METERS
        / Math.sqrt(1 - Math.pow(ECCENTRICITY, 2) * (Math.pow(Math.sin(latRadians), 2)));
    double altMeters = (pMeters / Math.cos(latRadians)) - curvatureRadius;

    // Correct for numerical instability in altitude near poles
    boolean polesCheck = Math.abs(ecefXMeters) < ECEF_NEAR_POLE_THRESHOLD_METERS
        && Math.abs(ecefYMeters) < ECEF_NEAR_POLE_THRESHOLD_METERS;
    if (polesCheck) {
      altMeters = Math.abs(ecefZMeters) - SEMI_MINOR_AXIS_METERS;
    }

    return  new GeodeticLlaValues(latRadians, lngRadians, altMeters);
  }

   /**
   * Converts ECEF (Earth Centered Earth Fixed) Cartesian coordinates to LLA (latitude,
   * longitude, and altitude) using iteration approach
   *
   * <p>Inputs are cartesian coordinates x,y,z.
   *
   * <p>Outputs is GeodeticLlaValues containing geodetic latitude (radians), geodetic longitude
   * (radians), height above WGS84 ellipsoid (m)}
   */
  public static GeodeticLlaValues convertECEFToLLAByIterations(double ecefXMeters,
      double ecefYMeters, double ecefZMeters) {

    double xyLengthMeters = Math.sqrt(Math.pow(ecefXMeters, 2) + Math.pow(ecefYMeters, 2));
    double xyzLengthMeters = Math.sqrt(Math.pow(xyLengthMeters, 2) + Math.pow(ecefZMeters, 2));

    double lngRad;
    if (xyLengthMeters > MIN_MAGNITUDE_METERS) {
      lngRad = Math.atan2(ecefYMeters, ecefXMeters);
    } else {
      lngRad = 0;
    }

    double sinPhi;
    if (xyzLengthMeters > MIN_MAGNITUDE_METERS) {
      sinPhi = ecefZMeters / xyzLengthMeters;
    } else {
      sinPhi = 0;
    }
    // initial latitude (iterate next to improve accuracy)
    double latRad = Math.asin(sinPhi);
    double altMeters;
    if (xyzLengthMeters > MIN_MAGNITUDE_METERS) {
      double ni;
      double pResidual;
      double ecefZMetersResidual;
      // initial height (iterate next to improve accuracy)
      altMeters = xyzLengthMeters - EARTH_SEMI_MAJOR_AXIS_METERS
          * (1 - sinPhi * sinPhi / INVERSE_FLATENNING);

      for (int i = 1; i <= MAX_ITERATIONS; i++) {
        sinPhi = Math.sin(latRad);

        // calculate radius of curvature in prime vertical direction
        ni = EARTH_SEMI_MAJOR_AXIS_METERS / Math.sqrt(1 - (2 - 1 / INVERSE_FLATENNING)
            / INVERSE_FLATENNING * Math.sin(latRad) * Math.sin(latRad));

        // calculate residuals in p and ecefZMeters
        pResidual = xyLengthMeters - (ni + altMeters) * Math.cos(latRad);
        ecefZMetersResidual = ecefZMeters
            - (ni * (1 - (2 - 1 / INVERSE_FLATENNING) / INVERSE_FLATENNING) + altMeters)
            * Math.sin(latRad);

        // update height and latitude
        altMeters += Math.sin(latRad) * ecefZMetersResidual + Math.cos(latRad) * pResidual;
        latRad += (Math.cos(latRad) * ecefZMetersResidual - Math.sin(latRad) * pResidual)
            / (ni + altMeters);

        if (Math.sqrt((pResidual * pResidual + ecefZMetersResidual * ecefZMetersResidual))
            < RESIDUAL_TOLERANCE) {
          break;
        }

        if (i == MAX_ITERATIONS) {
          System.err.println(
              "Geodetic coordinate calculation did not converge in " + i + " iterations");
        }
      }
    } else {
      altMeters = 0;
    }
    return new GeodeticLlaValues(latRad, lngRad, altMeters);
  }

  /**
   *
   * Class containing geodetic coordinates: latitude in radians, geodetic longitude in radians
   *  and altitude in meters
   */
  public static class GeodeticLlaValues {

    public final double latitudeRadians;
    public final double longitudeRadians;
    public final double altitudeMeters;

    public GeodeticLlaValues(double latitudeRadians,
        double longitudeRadians, double altitudeMeters) {
      this.latitudeRadians = latitudeRadians;
      this.longitudeRadians = longitudeRadians;
      this.altitudeMeters = altitudeMeters;
    }
  }

}
