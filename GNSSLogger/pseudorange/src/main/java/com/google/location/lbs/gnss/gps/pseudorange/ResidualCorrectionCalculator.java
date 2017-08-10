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

import com.google.common.base.Preconditions;
import com.google.location.lbs.gnss.gps.pseudorange.EcefToTopocentricConverter.TopocentricAEDValues;
import com.google.location.lbs.gnss.gps.pseudorange.UserPositionVelocityWeightedLeastSquare.
    SatellitesPositionPseudorangesResidualAndCovarianceMatrix;
import java.util.Arrays;
import java.util.Comparator;

/**
 * A tool with the methods to perform the pseudorange residual analysis.
 *
 * <p>The tool allows correcting the pseudorange residuals computed in WLS by removing the user
 * clock error. The user clock bias is computed using the highest elevation satellites as those are
 * assumed not to suffer from multipath. The reported residuals are provided at the input ground
 * truth position by applying an adjustment using the distance of WLS to satellites vs ground-truth
 * to satellites.
 */

public class ResidualCorrectionCalculator {

  /**
   * The threshold for the residual of user clock bias per satellite with respect to the best user
   * clock bias.
   */
  private static final double BEST_USER_CLOCK_BIAS_RESIDUAL_THRESHOLD_METERS = 10;

  /* The number of satellites we pick for calculating the best user clock bias */
  private static final int MIN_SATS_FOR_BIAS_COMPUTATION = 4;

  /**
   * Corrects the pseudorange residual by the best user clock bias estimation computed from the top
   * elevation satellites.
   *
   * @param satellitesPositionPseudorangesResidual satellite position and pseudorange residual info
   *     passed in from WLS
   * @param positionVelocitySolutionECEF position velocity solution passed in from WLS
   * @param groundTruthInputECEFMeters the reference position in ECEF meters
   * @return an array contains the corrected pseusorange residual in meters for each satellite
   */
  public static double[] calculateCorrectedResiduals(
      SatellitesPositionPseudorangesResidualAndCovarianceMatrix
          satellitesPositionPseudorangesResidual,
      double[] positionVelocitySolutionECEF,
      double[] groundTruthInputECEFMeters) {


    double[] residuals = satellitesPositionPseudorangesResidual.pseudorangeResidualsMeters.clone();
    int[] satellitePrn = satellitesPositionPseudorangesResidual.satellitePRNs.clone();
    double[] satelliteElevationDegree = new double[residuals.length];
    SatelliteElevationAndResiduals[] satelliteResidualsListAndElevation =
        new SatelliteElevationAndResiduals[residuals.length];

    // Check the alignment between inputs
    Preconditions.checkArgument(residuals.length == satellitePrn.length);

    // Apply residual corrections per satellite
    for (int i = 0; i < residuals.length; i++) {
      // Calculate the delta of user-satellite distance between ground truth and WLS solution
      // and use the delta to adjust the residuals computed from the WLS. With this adjustments all
      // residuals will be as if they are computed with respect to the ground truth rather than
      // the WLS.
      double[] satellitePos = satellitesPositionPseudorangesResidual.satellitesPositionsMeters[i];
      double wlsUserSatelliteDistance =
          GpsMathOperations.vectorNorm(
              GpsMathOperations.subtractTwoVectors(
                  Arrays.copyOf(positionVelocitySolutionECEF, 3),
                  satellitePos));
      double groundTruthSatelliteDistance =
          GpsMathOperations.vectorNorm(
              GpsMathOperations.subtractTwoVectors(groundTruthInputECEFMeters, satellitePos));

      // Compute the adjustment for satellite i
      double groundTruthAdjustment = wlsUserSatelliteDistance - groundTruthSatelliteDistance;

      // Correct the input residual with the adjustment to ground truth
      residuals[i] = residuals[i] - groundTruthAdjustment;

      // Calculate the elevation in degrees of satellites
      TopocentricAEDValues topocentricAedValues =
          EcefToTopocentricConverter.calculateElAzDistBetween2Points(
              groundTruthInputECEFMeters, satellitesPositionPseudorangesResidual.
                  satellitesPositionsMeters[i]
          );

      satelliteElevationDegree[i] = Math.toDegrees(topocentricAedValues.elevationRadians);

      // Store the computed satellite elevations and residuals into a SatelliteElevationAndResiduals
      // list with clock correction removed.
      satelliteResidualsListAndElevation[i] =
          new SatelliteElevationAndResiduals(
              satelliteElevationDegree[i], residuals[i]
              + positionVelocitySolutionECEF[3], satellitePrn[i]);
    }

    double bestUserClockBiasMeters = calculateBestUserClockBias(satelliteResidualsListAndElevation);

    // Use the best clock bias to correct the residuals to ensure that the receiver clock errors are
    // removed from the reported residuals in the analysis
    double[] correctedResidualsMeters =
        GpsMathOperations.createAndFillArray(
            GpsNavigationMessageStore.MAX_NUMBER_OF_SATELLITES, Double.NaN
        );

    for (SatelliteElevationAndResiduals element :
        satelliteResidualsListAndElevation) {
      correctedResidualsMeters[element.svID - 1] = element.residual - bestUserClockBiasMeters;
    }

    return correctedResidualsMeters;
  }

  /**
   * Computes the user clock bias by iteratively averaging the clock bias of top elevation
   * satellites.
   *
   * @param satelliteResidualsAndElevationList a list of satellite elevation and
   *        pseudorange residuals
   * @return the corrected best user clock bias
   */
  private static double calculateBestUserClockBias(
      SatelliteElevationAndResiduals[] satelliteResidualsAndElevationList) {

    // Sort the satellites by descending order of their elevations
    Arrays.sort(
        satelliteResidualsAndElevationList,
        new Comparator<SatelliteElevationAndResiduals>() {
          @Override
          public int compare(
              SatelliteElevationAndResiduals o1, SatelliteElevationAndResiduals o2) {
            return Double.compare(o2.elevationDegree, o1.elevationDegree);
          }
        });

    // Pick up the top elevation satellites
    double[] topElevationSatsResiduals = GpsMathOperations.createAndFillArray(
        MIN_SATS_FOR_BIAS_COMPUTATION, Double.NaN
    );
    int numOfUsefulSatsToComputeBias = 0;
    for (int i = 0; i < satelliteResidualsAndElevationList.length
        && i < topElevationSatsResiduals.length; i++) {
      topElevationSatsResiduals[i] = satelliteResidualsAndElevationList[i].residual;
      numOfUsefulSatsToComputeBias++;
    }

    double meanResidual;
    double[] deltaResidualFromMean;
    int maxDeltaIndex = -1;

    // Iteratively remove the satellites with highest residuals with respect to the mean of the
    // residuals until the highest residual in the list is below threshold.
    do {
      if (maxDeltaIndex >= 0) {
        topElevationSatsResiduals[maxDeltaIndex] = Double.NaN;
        numOfUsefulSatsToComputeBias--;
      }
      meanResidual = GpsMathOperations.meanOfVector(topElevationSatsResiduals);
      deltaResidualFromMean
          = GpsMathOperations.subtractByScalar(topElevationSatsResiduals, meanResidual);
      maxDeltaIndex = GpsMathOperations.maxIndexOfVector(deltaResidualFromMean);
    } while (deltaResidualFromMean[maxDeltaIndex] > BEST_USER_CLOCK_BIAS_RESIDUAL_THRESHOLD_METERS
        && numOfUsefulSatsToComputeBias > 2);

    return meanResidual;
  }

  /** A container for satellite residual and elevationDegree information */
  private static class SatelliteElevationAndResiduals {
    /** Satellite pseudorange or pseudorange rate residual with clock correction removed */
    final double residual;

    /** Satellite elevation in degrees with respect to the user */
    final double elevationDegree;

    /** Satellite ID */
    final int svID;

    SatelliteElevationAndResiduals(
        double elevationDegree, double residual, int svID) {
      this.residual = residual;
      this.svID = svID;
      this.elevationDegree = elevationDegree;
    }
  }

}
