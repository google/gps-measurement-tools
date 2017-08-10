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

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * Converts ECEF (Earth Centered Earth Fixed) Cartesian coordinates to local ENU (East, North,
 * and Up).
 *
 * <p> Source: reference from Navipedia:
 * http://www.navipedia.net/index.php/Transformations_between_ECEF_and_ENU_coordinates
 */

public class Ecef2EnuConverter {

  /**
   * Converts a vector represented by coordinates ecefX, ecefY, ecefZ in an
   * Earth-Centered Earth-Fixed (ECEF) Cartesian system into a vector in a
   * local east-north-up (ENU) Cartesian system.
   *
   * <p> For example it can be used to rotate a speed vector or position offset vector to ENU.
   *
   * @param ecefX X coordinates in ECEF
   * @param ecefY Y coordinates in ECEF
   * @param ecefZ Z coordinates in ECEF
   * @param refLat Latitude in Radians of the Reference Position
   * @param refLng Longitude in Radians of the Reference Position
   * @return the converted values in {@code EnuValues}
   */
  public static EnuValues convertEcefToEnu(double ecefX, double ecefY, double ecefZ,
      double refLat, double refLng){

    RealMatrix rotationMatrix = getRotationMatrix(refLat, refLng);
    RealMatrix ecefCoordinates = new Array2DRowRealMatrix(new double[]{ecefX, ecefY, ecefZ});

    RealMatrix enuResult = rotationMatrix.multiply(ecefCoordinates);
    return new EnuValues(enuResult.getEntry(0, 0),
        enuResult.getEntry(1, 0), enuResult.getEntry(2 , 0));
  }

  /**
   * Computes a rotation matrix for converting a vector in Earth-Centered Earth-Fixed (ECEF)
   * Cartesian system into a vector in local east-north-up (ENU) Cartesian system with respect to
   * a reference location. The matrix has the following content:
   *
   * - sinLng                     cosLng            0
   * - sinLat * cosLng      - sinLat * sinLng      cosLat
   *   cosLat * cosLng        cosLat * sinLng      sinLat
   *
   * <p> Reference: Pratap Misra and Per Enge
   * "Global Positioning System: Signals, Measurements, and Performance" Page 137.
   *
   * @param refLat Latitude of reference location
   * @param refLng Longitude of reference location
   * @return the Ecef to Enu rotation matrix
   */
  public static RealMatrix getRotationMatrix(double refLat, double refLng){
    RealMatrix rotationMatrix = new Array2DRowRealMatrix(3, 3);

    // Fill in the rotation Matrix
    rotationMatrix.setEntry(0, 0, -1 * Math.sin(refLng));
    rotationMatrix.setEntry(1, 0, -1 * Math.cos(refLng) * Math.sin(refLat));
    rotationMatrix.setEntry(2, 0, Math.cos(refLng) * Math.cos(refLat));
    rotationMatrix.setEntry(0, 1, Math.cos(refLng));
    rotationMatrix.setEntry(1, 1, -1 * Math.sin(refLat) * Math.sin(refLng));
    rotationMatrix.setEntry(2, 1, Math.cos(refLat) * Math.sin(refLng));
    rotationMatrix.setEntry(0, 2, 0);
    rotationMatrix.setEntry(1, 2, Math.cos(refLat));
    rotationMatrix.setEntry(2, 2, Math.sin(refLat));
    return rotationMatrix;
  }

  /**
   * A container for values in ENU (East, North, Up) coordination system.
   */
  public static class EnuValues {

    /**
     * East Coordinates in local ENU
     */
    public final double enuEast;

    /**
     * North Coordinates in local ENU
     */
    public final double enuNorth;

    /**
     * Up Coordinates in local ENU
     */
    public final double enuUP;

    /**
     * Constructor
     */
    public EnuValues(double enuEast, double enuNorth, double enuUP){
      this.enuEast = enuEast;
      this.enuNorth = enuNorth;
      this.enuUP = enuUP;
    }
   }

}
