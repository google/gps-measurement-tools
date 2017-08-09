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
 * Calculate the troposheric delay based on the ENGOS Tropospheric model.
 *
 * <p>The tropospheric delay is modeled as a combined effect of the delay experienced due to
 * hyrostatic (dry) and wet components of the troposphere. Both delays experienced at zenith are
 * scaled with a mapping function to get the delay at any specific elevation.
 *
 * <p>The tropospheric model algorithm of EGNOS model by Penna, N., A. Dodson and W. Chen (2001)
 * (http://espace.library.curtin.edu.au/cgi-bin/espace.pdf?file=/2008/11/13/file_1/18917) is used
 * for calculating the zenith delays. In this model, the weather parameters are extracted using
 * interpolation from lookup table derived from the US Standard Atmospheric Supplements, 1966.
 *
 * <p>A close form mapping function is built using Guo and Langley, 2003
 * (http://gauss2.gge.unb.ca/papers.pdf/iongpsgnss2003.guo.pdf) which is able to calculate accurate
 * mapping down to 2 degree elevations.
 *
 * <p>Sources:
 * <p>http://espace.library.curtin.edu.au/cgi-bin/espace.pdf?file=/2008/11/13/file_1/18917
 * <p>- http://www.academia.edu/3512180/Assessment_of_UNB3M_neutral
 * _atmosphere_model_and_EGNOS_model_for_near-equatorial-tropospheric_delay_correction
 * <p>- http://gauss.gge.unb.ca/papers.pdf/ion52am.collins.pdf
 * <p>- http://www.navipedia.net/index.php/Tropospheric_Delay#cite_ref-3
 * <p>Hydrostatic and non-hydrostatic mapping functions are obtained from:
 * http://gauss2.gge.unb.ca/papers.pdf/iongpsgnss2003.guo.pdf
 *
 */
public class TroposphericModelEgnos {
  // parameters of the EGNOS models
  private static final int INDEX_15_DEGREES = 0;
  private static final int INDEX_75_DEGREES = 4;
  private static final int LATITUDE_15_DEGREES = 15;
  private static final int LATITUDE_75_DEGREES = 75;
  // Lookup Average parameters
  // Troposphere average presssure mbar
  private static final double[] latDegreeToPressureMbarAvgMap =
    {1013.25,  1017.25, 1015.75, 1011.75, 1013.0};
  // Troposphere average temperature Kelvin
  private static final double[] latDegreeToTempKelvinAvgMap =
    {299.65, 294.15, 283.15, 272.15, 263.65};
  // Troposphere average wator vapor pressure
  private static final double[] latDegreeToWVPressureMbarAvgMap = {26.31, 21.79, 11.66, 6.78, 4.11};
  // Troposphere average temperature lapse rate K/m
  private static final double[] latDegreeToBetaAvgMapKPM =
    {6.30e-3, 6.05e-3, 5.58e-3, 5.39e-3, 4.53e-3};
  // Troposphere average water vapor lapse rate (dimensionless)
  private static final double[] latDegreeToLampdaAvgMap = {2.77, 3.15, 2.57, 1.81, 1.55};

  // Lookup Amplitude parameters
  // Troposphere amplitude presssure mbar
  private static final double[] latDegreeToPressureMbarAmpMap = {0.0, -3.75, -2.25, -1.75, -0.5};
  // Troposphere amplitude temperature Kelvin
  private static final double[] latDegreeToTempKelvinAmpMap = {0.0, 7.0, 11.0, 15.0, 14.5};
  // Troposphere amplitude wator vapor pressure
  private static final double[] latDegreeToWVPressureMbarAmpMap = {0.0, 8.85, 7.24, 5.36, 3.39};
  // Troposphere amplitude temperature lapse rate K/m
  private static final double[] latDegreeToBetaAmpMapKPM =
    {0.0, 0.25e-3, 0.32e-3, 0.81e-3, 0.62e-3};
  // Troposphere amplitude water vapor lapse rate (dimensionless)
  private static final double[] latDegreeToLampdaAmpMap = {0.0, 0.33, 0.46, 0.74, 0.30};
  // Zenith delay dry constant K/mbar
  private static final double K1 = 77.604;
  // Zenith delay wet constant K^2/mbar
  private static final double K2 = 382000.0;
  // gas constant for dry air J/kg/K
  private static final double RD = 287.054;
  // Acceleration of gravity at the atmospheric column centroid m/s^-2
  private static final double GM = 9.784;
  // Gravity m/s^2
  private static final double GRAVITY_MPS2 = 9.80665;

  private static final double MINIMUM_INTERPOLATION_THRESHOLD = 1e-25;
  private static final double B_HYDROSTATIC = 0.0035716;
  private static final double C_HYDROSTATIC = 0.082456;
  private static final double B_NON_HYDROSTATIC = 0.0018576;
  private static final double C_NON_HYDROSTATIC = 0.062741;
  private static final double SOUTHERN_HEMISPHERE_DMIN = 211.0;
  private static final double NORTHERN_HEMISPHERE_DMIN = 28.0;
  // Days recalling that every fourth year is a leap year and has an extra day - February 29th
  private static final double DAYS_PER_YEAR = 365.25;

  /**
   * Computes the tropospheric correction in meters given the satellite elevation in radians, the
   * user latitude in radians, the user Orthometric height above sea level in meters and the day of
   * the year.
   *
   * <p>Dry and wet delay zenith delay components are calculated and then scaled with the mapping
   * function at the given satellite elevation.
   *
   */
  public static double calculateTropoCorrectionMeters(double satElevationRadians,
      double userLatitudeRadian, double heightMetersAboveSeaLevel, int dayOfYear1To366) {
    DryAndWetMappingValues dryAndWetMappingValues =
        computeDryAndWetMappingValuesUsingUNBabcMappingFunction(satElevationRadians,
            userLatitudeRadian, heightMetersAboveSeaLevel);
    DryAndWetZenithDelays dryAndWetZenithDelays = calculateZenithDryAndWetDelaysSec
        (userLatitudeRadian, heightMetersAboveSeaLevel, dayOfYear1To366);

    double drydelaySeconds =
        dryAndWetZenithDelays.dryZenithDelaySec * dryAndWetMappingValues.dryMappingValue;
    double wetdelaySeconds =
        dryAndWetZenithDelays.wetZenithDelaySec * dryAndWetMappingValues.wetMappingValue;
    return drydelaySeconds + wetdelaySeconds;
  }

  /**
   * Computes the dry and wet mapping values based on the University of Brunswick UNBabc model. The
   * mapping function inputs are satellite elevation in radians, user latitude in radians and user
   * orthometric height above sea level in meters. The function returns
   * {@code DryAndWetMappingValues} containing dry and wet mapping values.
   *
   * <p>From the many dry and wet mapping functions of components of the troposphere, the method
   * from the University of Brunswick in Canada was selected due to its reasonable computation time
   * and accuracy with satellites as low as 2 degrees elevation.
   * <p>Source: http://gauss2.gge.unb.ca/papers.pdf/iongpsgnss2003.guo.pdf
   */
  private static DryAndWetMappingValues computeDryAndWetMappingValuesUsingUNBabcMappingFunction(
      double satElevationRadians, double userLatitudeRadians, double heightMetersAboveSeaLevel) {

    if (satElevationRadians > Math.PI / 2.0) {
      satElevationRadians = Math.PI / 2.0;
    } else if (satElevationRadians < 2.0 * Math.PI / 180.0) {
      satElevationRadians = Math.toRadians(2.0);
    }

    // dry components mapping parameters
    double aHidrostatic = (1.18972 - 0.026855 * heightMetersAboveSeaLevel / 1000.0 + 0.10664
        * Math.cos(userLatitudeRadians)) / 1000.0;


    double numeratorDry = 1.0 + (aHidrostatic / (1.0 + (B_HYDROSTATIC / (1.0 + C_HYDROSTATIC))));
    double denominatorDry = Math.sin(satElevationRadians) + (aHidrostatic / (
        Math.sin(satElevationRadians)
        + (B_HYDROSTATIC / (Math.sin(satElevationRadians) + C_HYDROSTATIC))));

    double drymap = numeratorDry / denominatorDry;

    // wet components mapping parameters
    double aNonHydrostatic = (0.61120 - 0.035348 * heightMetersAboveSeaLevel / 1000.0 - 0.01526
        * Math.cos(userLatitudeRadians)) / 1000.0;


    double numeratorWet =
        1.0 + (aNonHydrostatic / (1.0 + (B_NON_HYDROSTATIC / (1.0 + C_NON_HYDROSTATIC))));
    double denominatorWet = Math.sin(satElevationRadians) + (aNonHydrostatic / (
        Math.sin(satElevationRadians)
        + (B_NON_HYDROSTATIC / (Math.sin(satElevationRadians) + C_NON_HYDROSTATIC))));

    double wetmap = numeratorWet / denominatorWet;
    return new DryAndWetMappingValues(drymap, wetmap);
  }

  /**
   * Computes the combined effect of the delay at zenith experienced due to hyrostatic (dry) and wet
   * components of the troposphere. The function inputs are the user latitude in radians, user
   * orthometric height above sea level in meters and the day of the year (1-366). The function
   * returns a {@code DryAndWetZenithDelays} containing dry and wet delays at zenith.
   *
   * <p>EGNOS Tropospheric model by Penna et al. (2001) is used in this case.
   * (http://espace.library.curtin.edu.au/cgi-bin/espace.pdf?file=/2008/11/13/file_1/18917)
   *
   */
  private static DryAndWetZenithDelays calculateZenithDryAndWetDelaysSec(double userLatitudeRadians,
      double heightMetersAboveSeaLevel, int dayOfyear1To366) {
    // interpolated meteorological values
    double pressureMbar;
    double tempKelvin;
    double waterVaporPressureMbar;
    // temperature lapse rate, [K/m]
    double beta;
    // water vapor lapse rate, dimensionless
    double lambda;

    double absLatitudeDeg = Math.toDegrees(Math.abs(userLatitudeRadians));
    // day of year min constant
    double dmin;
    if (userLatitudeRadians < 0) {
      dmin = SOUTHERN_HEMISPHERE_DMIN;
    } else {
      dmin = NORTHERN_HEMISPHERE_DMIN;

    }
    double amplitudeScalefactor = Math.cos((2 * Math.PI * (dayOfyear1To366 - dmin))
        / DAYS_PER_YEAR);

    if (absLatitudeDeg <= LATITUDE_15_DEGREES) {
      pressureMbar = latDegreeToPressureMbarAvgMap[INDEX_15_DEGREES]
          - latDegreeToPressureMbarAmpMap[INDEX_15_DEGREES] * amplitudeScalefactor;
      tempKelvin = latDegreeToTempKelvinAvgMap[INDEX_15_DEGREES]
          - latDegreeToTempKelvinAmpMap[INDEX_15_DEGREES] * amplitudeScalefactor;
      waterVaporPressureMbar = latDegreeToWVPressureMbarAvgMap[INDEX_15_DEGREES]
          - latDegreeToWVPressureMbarAmpMap[INDEX_15_DEGREES] * amplitudeScalefactor;
      beta = latDegreeToBetaAvgMapKPM[INDEX_15_DEGREES] - latDegreeToBetaAmpMapKPM[INDEX_15_DEGREES]
          * amplitudeScalefactor;
      lambda = latDegreeToLampdaAmpMap[INDEX_15_DEGREES] - latDegreeToLampdaAmpMap[INDEX_15_DEGREES]
          * amplitudeScalefactor;
    } else if (absLatitudeDeg > LATITUDE_15_DEGREES && absLatitudeDeg < LATITUDE_75_DEGREES) {
      int key = (int) (absLatitudeDeg / LATITUDE_15_DEGREES);

      double averagePressureMbar = interpolate(key * LATITUDE_15_DEGREES,
          latDegreeToPressureMbarAvgMap[key - 1], (key + 1) * LATITUDE_15_DEGREES,
          latDegreeToPressureMbarAvgMap[key], absLatitudeDeg);
      double amplitudePressureMbar = interpolate(key * LATITUDE_15_DEGREES,
          latDegreeToPressureMbarAmpMap[key - 1], (key + 1) * LATITUDE_15_DEGREES,
          latDegreeToPressureMbarAmpMap[key], absLatitudeDeg);
      pressureMbar = averagePressureMbar - amplitudePressureMbar * amplitudeScalefactor;

      double averageTempKelvin = interpolate(key * LATITUDE_15_DEGREES,
          latDegreeToTempKelvinAvgMap[key - 1], (key + 1) * LATITUDE_15_DEGREES,
          latDegreeToTempKelvinAvgMap[key], absLatitudeDeg);
      double amplitudeTempKelvin = interpolate(key * LATITUDE_15_DEGREES,
          latDegreeToTempKelvinAmpMap[key - 1], (key + 1) * LATITUDE_15_DEGREES,
          latDegreeToTempKelvinAmpMap[key], absLatitudeDeg);
      tempKelvin = averageTempKelvin - amplitudeTempKelvin * amplitudeScalefactor;

      double averageWaterVaporPressureMbar = interpolate(key * LATITUDE_15_DEGREES,
          latDegreeToWVPressureMbarAvgMap[key - 1], (key + 1) * LATITUDE_15_DEGREES,
          latDegreeToWVPressureMbarAvgMap[key], absLatitudeDeg);
      double amplitudeWaterVaporPressureMbar = interpolate(key * LATITUDE_15_DEGREES,
          latDegreeToWVPressureMbarAmpMap[key - 1], (key + 1) * LATITUDE_15_DEGREES,
          latDegreeToWVPressureMbarAmpMap[key], absLatitudeDeg);
      waterVaporPressureMbar =
          averageWaterVaporPressureMbar - amplitudeWaterVaporPressureMbar * amplitudeScalefactor;

      double averageBeta = interpolate(key * LATITUDE_15_DEGREES, latDegreeToBetaAvgMapKPM[key - 1],
          (key + 1) * LATITUDE_15_DEGREES, latDegreeToBetaAvgMapKPM[key], absLatitudeDeg);
      double amplitudeBeta = interpolate(key * LATITUDE_15_DEGREES,
          latDegreeToBetaAmpMapKPM[key - 1], (key + 1) * LATITUDE_15_DEGREES,
          latDegreeToBetaAmpMapKPM[key], absLatitudeDeg);
      beta = averageBeta - amplitudeBeta * amplitudeScalefactor;

      double averageLambda = interpolate(key * LATITUDE_15_DEGREES,
          latDegreeToLampdaAvgMap[key - 1], (key + 1) * LATITUDE_15_DEGREES,
          latDegreeToLampdaAvgMap[key], absLatitudeDeg);
      double amplitudeLambda = interpolate(key * LATITUDE_15_DEGREES,
          latDegreeToLampdaAmpMap[key - 1], (key + 1) * LATITUDE_15_DEGREES,
          latDegreeToLampdaAmpMap[key], absLatitudeDeg);
      lambda = averageLambda - amplitudeLambda * amplitudeScalefactor;
    } else {
      pressureMbar = latDegreeToPressureMbarAvgMap[INDEX_75_DEGREES]
          - latDegreeToPressureMbarAmpMap[INDEX_75_DEGREES] * amplitudeScalefactor;
      tempKelvin = latDegreeToTempKelvinAvgMap[INDEX_75_DEGREES]
          - latDegreeToTempKelvinAmpMap[INDEX_75_DEGREES] * amplitudeScalefactor;
      waterVaporPressureMbar = latDegreeToWVPressureMbarAvgMap[INDEX_75_DEGREES]
          - latDegreeToWVPressureMbarAmpMap[INDEX_75_DEGREES] * amplitudeScalefactor;
      beta = latDegreeToBetaAvgMapKPM[INDEX_75_DEGREES] - latDegreeToBetaAmpMapKPM[INDEX_75_DEGREES]
          * amplitudeScalefactor;
      lambda = latDegreeToLampdaAmpMap[INDEX_75_DEGREES] - latDegreeToLampdaAmpMap[INDEX_75_DEGREES]
          * amplitudeScalefactor;
    }

    double zenithDryDelayAtSeaLevelSeconds = (1.0e-6 * K1 * RD * pressureMbar) / GM;
    double zenithWetDelayAtSeaLevelSeconds = (((1.0e-6 * K2 * RD)
        / (GM * (lambda + 1.0) - beta * RD)) * (waterVaporPressureMbar / tempKelvin));
    double commonBase = 1.0 - ((beta * heightMetersAboveSeaLevel) / tempKelvin);

    double powerDry = (GRAVITY_MPS2 / (RD * beta));
    double powerWet = (((lambda + 1.0) * GRAVITY_MPS2) / (RD * beta)) - 1.0;
    double zenithDryDelaySeconds = zenithDryDelayAtSeaLevelSeconds * Math.pow(commonBase, powerDry);
    double zenithWetDelaySeconds = zenithWetDelayAtSeaLevelSeconds * Math.pow(commonBase, powerWet);
    return new DryAndWetZenithDelays(zenithDryDelaySeconds, zenithWetDelaySeconds);
  }

  /**
   * Interpolates linearly given two points (point1X, point1Y) and (point2X, point2Y). Given the
   * desired value of x (xInterpolated), an interpolated value of y shall be computed and returned.
   */
  private static double interpolate(double point1X, double point1Y, double point2X, double point2Y,
      double xOutput) {
    // Check that xOutput is between the two interpolation points.
    if ((point1X < point2X && (xOutput < point1X || xOutput > point2X))
        || (point2X < point1X && (xOutput < point2X || xOutput > point1X))) {
      throw new IllegalArgumentException("Interpolated value is outside the interpolated region");
    }
    double deltaX = point2X - point1X;
    double yOutput;

    if (Math.abs(deltaX) > MINIMUM_INTERPOLATION_THRESHOLD) {
      yOutput = point1Y + (xOutput - point1X) / deltaX * (point2Y - point1Y);
    } else {
      yOutput = point1Y;
    }
    return yOutput;
  }

  /**
   *
   * A class containing dry and wet mapping values
   */
  private static class DryAndWetMappingValues {
    public double dryMappingValue;
    public double wetMappingValue;

    public DryAndWetMappingValues(double dryMappingValue, double wetMappingValue) {
      this.dryMappingValue = dryMappingValue;
      this.wetMappingValue = wetMappingValue;
    }
  }

  /**
   *
   * A class containing dry and wet delays in seconds experienced at zenith
   */
  private static class DryAndWetZenithDelays {
    public double dryZenithDelaySec;
    public double wetZenithDelaySec;

    public DryAndWetZenithDelays(double dryZenithDelay, double wetZenithDelay) {
      this.dryZenithDelaySec = dryZenithDelay;
      this.wetZenithDelaySec = wetZenithDelay;
    }
  }
}
