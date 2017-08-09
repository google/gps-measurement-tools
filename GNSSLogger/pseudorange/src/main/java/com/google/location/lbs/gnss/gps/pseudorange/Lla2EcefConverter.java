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

/**
 * A tool to convert geodetic latitude, longitude and altitude above planetary ellipsoid to
 * Earth-centered Earth-fixed (ECEF) Cartesian coordinates
 *
 * <p>Source: https://www.mathworks.com/help/aeroblks/llatoecefposition.html
 */
public class Lla2EcefConverter {
  private static final double ECCENTRICITY = 8.1819190842622e-2;
  private static final double EARTH_SEMI_MAJOR_AXIS_METERS = 6378137.0;

  /**
   * Converts LLA (latitude,longitude, and altitude) coordinates to  ECEF
   * (Earth-Centered Earth-Fixed) Cartesian coordinates
   *
   * <p>Inputs is GeodeticLlaValues class {@link GeodeticLlaValues} containing geodetic latitude
   * (radians), geodetic longitude (radians), height above WGS84 ellipsoid (m)
   *
   * <p>Output is cartesian coordinates x,y,z in meters
   */
  public static double[] convertFromLlaToEcefMeters(GeodeticLlaValues llaValues) {
    double cosLatitude = Math.cos(llaValues.latitudeRadians);
    double cosLongitude = Math.cos(llaValues.longitudeRadians);
    double sinLatitude = Math.sin(llaValues.latitudeRadians);
    double sinLongitude = Math.sin(llaValues.longitudeRadians);

    double r0 =
        EARTH_SEMI_MAJOR_AXIS_METERS
            / Math.sqrt(1.0 - Math.pow(ECCENTRICITY, 2) * sinLatitude * sinLatitude);

    double[] positionEcefMeters = new double[3];
    positionEcefMeters[0] = (llaValues.altitudeMeters + r0) * cosLatitude * cosLongitude;
    positionEcefMeters[1] = (llaValues.altitudeMeters + r0) * cosLatitude * sinLongitude;
    positionEcefMeters[2] =
        (llaValues.altitudeMeters + r0 * (1.0 - Math.pow(ECCENTRICITY, 2))) * sinLatitude;
    return positionEcefMeters;
  }
}
