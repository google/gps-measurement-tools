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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.location.lbs.gnss.gps.pseudorange.Ecef2LlaConverter.GeodeticLlaValues;
import com.google.location.lbs.gnss.gps.pseudorange.EcefToTopocentricConverter.TopocentricAEDValues;
import com.google.location.lbs.gnss.gps.pseudorange.SatellitePositionCalculator.PositionAndVelocity;
import android.location.cts.nano.Ephemeris.GpsEphemerisProto;
import android.location.cts.nano.Ephemeris.GpsNavMessageProto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * Computes an iterative least square receiver position solution given the pseudorange (meters) and
 * accumulated delta range (meters) measurements, receiver time of week, week number and the
 * navigation message.
 */
class UserPositionVelocityWeightedLeastSquare {
  private static final double SPEED_OF_LIGHT_MPS = 299792458.0;
  private static final int SECONDS_IN_WEEK = 604800;
  private static final double LEAST_SQUARE_TOLERANCE_METERS = 4.0e-8;
  /** Position correction threshold below which atmospheric correction will be applied */
  private static final double ATMPOSPHERIC_CORRECTIONS_THRESHOLD_METERS = 1000.0;
  private static final int MINIMUM_NUMER_OF_SATELLITES = 4;
  private static final double RESIDUAL_TO_REPEAT_LEAST_SQUARE_METERS = 20.0;
  private static final int MAXIMUM_NUMBER_OF_LEAST_SQUARE_ITERATIONS = 100;
  /** GPS C/A code chip width Tc = 1 microseconds */
  private static final double GPS_CHIP_WIDTH_T_C_SEC = 1.0e-6;
  /** Narrow correlator with spacing d = 0.1 chip */
  private static final double GPS_CORRELATOR_SPACING_IN_CHIPS = 0.1;
  /** Average time of DLL correlator T of 20 milliseconds */
  private static final double GPS_DLL_AVERAGING_TIME_SEC = 20.0e-3;
  /** Average signal travel time from GPS satellite and earth */
  private static final double AVERAGE_TRAVEL_TIME_SECONDS = 70.0e-3;
  private static final double SECONDS_PER_NANO = 1.0e-9;
  private static final double DOUBLE_ROUND_OFF_TOLERANCE = 0.0000000001;

  private final PseudorangeSmoother pseudorangeSmoother;
  private double geoidHeightMeters;
  private ElevationApiHelper elevationApiHelper;
  private boolean calculateGeoidMeters = true;
  private RealMatrix geometryMatrix;
  private double[] truthLocationForCorrectedResidualComputationEcef = null;

  /** Constructor */
  public UserPositionVelocityWeightedLeastSquare(PseudorangeSmoother pseudorangeSmoother) {
    this.pseudorangeSmoother = pseudorangeSmoother;
  }

  /** Constructor with Google Elevation API Key */
  public UserPositionVelocityWeightedLeastSquare(PseudorangeSmoother pseudorangeSmoother,
      String elevationApiKey){
    this.pseudorangeSmoother = pseudorangeSmoother;
    this.elevationApiHelper = new ElevationApiHelper(elevationApiKey);
  }

  /**
   * Sets the reference ground truth for pseudornage residual correction calculation. If no ground
   * truth is set, no corrected pesudorange residual will be calculated.
   */
  public void setTruthLocationForCorrectedResidualComputationEcef
  (double[] groundTruthForResidualCorrectionEcef) {
    this.truthLocationForCorrectedResidualComputationEcef = groundTruthForResidualCorrectionEcef;
  }

  /**
   * Least square solution to calculate the user position given the navigation message, pseudorange
   * and accumulated delta range measurements. Also calculates user velocity non-iteratively from
   * Least square position solution.
   *
   * <p>The method fills the user position and velocity in ECEF coordinates and receiver clock
   * offset in meters and clock offset rate in meters per second.
   *
   * <p>One can choose between no smoothing, using the carrier phase measurements (accumulated delta
   * range) or the doppler measurements (pseudorange rate) for smoothing the pseudorange. The
   * smoothing is applied only if time has changed below a specific threshold since last invocation.
   *
   * <p>Source for least squares:
   *
   * <ul>
   *   <li>http://www.u-blox.com/images/downloads/Product_Docs/GPS_Compendium%28GPS-X-02007%29.pdf
   *       page 81 - 85
   *   <li>Parkinson, B.W., Spilker Jr., J.J.: ‘Global positioning system: theory and applications’
   *       page 412 - 414
   * </ul>
   *
   * <p>Sources for smoothing pseudorange with carrier phase measurements:
   *
   * <ul>
   *   <li>Satellite Communications and Navigation Systems book, page 424,
   *   <li>Principles of GNSS, Inertial, and Multisensor Integrated Navigation Systems, page 388,
   *       389.
   * </ul>
   *
   * <p>The function does not modify the smoothed measurement list {@code
   * immutableSmoothedSatellitesToReceiverMeasurements}
   *
   * @param navMessageProto parameters of the navigation message
   * @param usefulSatellitesToReceiverMeasurements Map of useful satellite PRN to {@link
   *     GpsMeasurementWithRangeAndUncertainty} containing receiver measurements for computing the
   *     position solution.
   * @param receiverGPSTowAtReceptionSeconds Receiver estimate of GPS time of week (seconds)
   * @param receiverGPSWeek Receiver estimate of GPS week (0-1024+)
   * @param dayOfYear1To366 The day of the year between 1 and 366
   * @param positionVelocitySolutionECEF Solution array of the following format:
   *        [0-2] xyz solution of user.
   *        [3] clock bias of user.
   *        [4-6] velocity of user.
   *        [7] clock bias rate of user.
   * @param positionVelocityUncertaintyEnu Uncertainty of calculated position and velocity solution
   *     in meters and mps local ENU system. Array has the following format:
   *     [0-2] Enu uncertainty of position solution in meters
   *     [3-5] Enu uncertainty of velocity solution in meters per second.
   * @param pseudorangeResidualMeters The pseudorange residual corrected by subtracting expected
   *     psudorange calculated with the use clock bias of the highest elevation satellites.
   */
  public void calculateUserPositionVelocityLeastSquare(
      GpsNavMessageProto navMessageProto,
      List<GpsMeasurementWithRangeAndUncertainty> usefulSatellitesToReceiverMeasurements,
      double receiverGPSTowAtReceptionSeconds,
      int receiverGPSWeek,
      int dayOfYear1To366,
      double[] positionVelocitySolutionECEF,
      double[] positionVelocityUncertaintyEnu,
      double[] pseudorangeResidualMeters)
      throws Exception {

    // Use PseudorangeSmoother to smooth the pseudorange according to: Satellite Communications and
    // Navigation Systems book, page 424 and Principles of GNSS, Inertial, and Multisensor
    // Integrated Navigation Systems, page 388, 389.
    double[] deltaPositionMeters;
    List<GpsMeasurementWithRangeAndUncertainty> immutableSmoothedSatellitesToReceiverMeasurements =
        pseudorangeSmoother.updatePseudorangeSmoothingResult(
            Collections.unmodifiableList(usefulSatellitesToReceiverMeasurements));
    List<GpsMeasurementWithRangeAndUncertainty> mutableSmoothedSatellitesToReceiverMeasurements =
        Lists.newArrayList(immutableSmoothedSatellitesToReceiverMeasurements);
    int numberOfUsefulSatellites =
        getNumberOfUsefulSatellites(mutableSmoothedSatellitesToReceiverMeasurements);
    // Least square position solution is supported only if 4 or more satellites visible
    Preconditions.checkArgument(numberOfUsefulSatellites >= MINIMUM_NUMER_OF_SATELLITES,
        "At least 4 satellites have to be visible... Only 3D mode is supported...");
    boolean repeatLeastSquare = false;
    SatellitesPositionPseudorangesResidualAndCovarianceMatrix satPosPseudorangeResidualAndWeight;

    boolean isFirstWLS = true;

    do {
      // Calculate satellites' positions, measurement residuals per visible satellite and
      // weight matrix for the iterative least square
      boolean doAtmosphericCorrections = false;
      satPosPseudorangeResidualAndWeight =
          calculateSatPosAndPseudorangeResidual(
              navMessageProto,
              mutableSmoothedSatellitesToReceiverMeasurements,
              receiverGPSTowAtReceptionSeconds,
              receiverGPSWeek,
              dayOfYear1To366,
              positionVelocitySolutionECEF,
              doAtmosphericCorrections);

      // Calculate the geometry matrix according to "Global Positioning System: Theory and
      // Applications", Parkinson and Spilker page 413
      RealMatrix covarianceMatrixM2 =
          new Array2DRowRealMatrix(satPosPseudorangeResidualAndWeight.covarianceMatrixMetersSquare);
      geometryMatrix = new Array2DRowRealMatrix(calculateGeometryMatrix(
          satPosPseudorangeResidualAndWeight.satellitesPositionsMeters,
          positionVelocitySolutionECEF));
      RealMatrix weightedGeometryMatrix;
      RealMatrix weightMatrixMetersMinus2 = null;
      // Apply weighted least square only if the covariance matrix is not singular (has a non-zero
      // determinant), otherwise apply ordinary least square. The reason is to ignore reported
      // signal to noise ratios by the receiver that can lead to such singularities
      LUDecomposition ludCovMatrixM2 = new LUDecomposition(covarianceMatrixM2);
      double det = ludCovMatrixM2.getDeterminant();

      if (det <= DOUBLE_ROUND_OFF_TOLERANCE) {
        // Do not weight the geometry matrix if covariance matrix is singular.
        weightedGeometryMatrix = geometryMatrix;
      } else {
        weightMatrixMetersMinus2 = ludCovMatrixM2.getSolver().getInverse();
        RealMatrix hMatrix =
            calculateHMatrix(weightMatrixMetersMinus2, geometryMatrix);
        weightedGeometryMatrix = hMatrix.multiply(geometryMatrix.transpose())
            .multiply(weightMatrixMetersMinus2);
      }

      // Equation 9 page 413 from "Global Positioning System: Theory and Applicaitons", Parkinson
      // and Spilker
      deltaPositionMeters =
          GpsMathOperations.matrixByColVectMultiplication(weightedGeometryMatrix.getData(),
          satPosPseudorangeResidualAndWeight.pseudorangeResidualsMeters);

      // Apply corrections to the position estimate
      positionVelocitySolutionECEF[0] += deltaPositionMeters[0];
      positionVelocitySolutionECEF[1] += deltaPositionMeters[1];
      positionVelocitySolutionECEF[2] += deltaPositionMeters[2];
      positionVelocitySolutionECEF[3] += deltaPositionMeters[3];
      // Iterate applying corrections to the position solution until correction is below threshold
      satPosPseudorangeResidualAndWeight =
          applyWeightedLeastSquare(
              navMessageProto,
              mutableSmoothedSatellitesToReceiverMeasurements,
              receiverGPSTowAtReceptionSeconds,
              receiverGPSWeek,
              dayOfYear1To366,
              positionVelocitySolutionECEF,
              deltaPositionMeters,
              doAtmosphericCorrections,
              satPosPseudorangeResidualAndWeight,
              weightMatrixMetersMinus2);

      // We use the first WLS iteration results and correct them based on the ground truth position
      // and using a clock error computed from high elevation satellites. The first iteration is
      // used before satellite with high residuals being removed.
      if (isFirstWLS && truthLocationForCorrectedResidualComputationEcef != null) {
        // Snapshot the information needed before high residual satellites are removed
        System.arraycopy(
            ResidualCorrectionCalculator.calculateCorrectedResiduals(
                satPosPseudorangeResidualAndWeight,
                positionVelocitySolutionECEF.clone(),
                truthLocationForCorrectedResidualComputationEcef),
            0 /*source starting pos*/,
            pseudorangeResidualMeters,
            0 /*destination starting pos*/,
            GpsNavigationMessageStore.MAX_NUMBER_OF_SATELLITES /*length of elements*/);
        isFirstWLS = false;
      }
      repeatLeastSquare = false;
      int satsWithResidualBelowThreshold =
          satPosPseudorangeResidualAndWeight.pseudorangeResidualsMeters.length;
      // remove satellites that have residuals above RESIDUAL_TO_REPEAT_LEAST_SQUARE_METERS as they
      // worsen the position solution accuracy. If any satellite is removed, repeat the least square
      repeatLeastSquare =
          removeHighResidualSats(
              mutableSmoothedSatellitesToReceiverMeasurements,
              repeatLeastSquare,
              satPosPseudorangeResidualAndWeight,
              satsWithResidualBelowThreshold);

    } while (repeatLeastSquare);
    calculateGeoidMeters = false;

    // The computed ECEF position will be used next to compute the user velocity.
    // we calculate and fill in the user velocity solutions based on following equation:
    // Weight Matrix * GeometryMatrix * User Velocity Vector
    // = Weight Matrix * deltaPseudoRangeRateWeightedMps
    // Reference: Pratap Misra and Per Enge
    // "Global Positioning System: Signals, Measurements, and Performance" Page 218.

    // Get the number of satellite used in Geometry Matrix
    numberOfUsefulSatellites = geometryMatrix.getRowDimension();

    RealMatrix rangeRateMps = new Array2DRowRealMatrix(numberOfUsefulSatellites, 1);
    RealMatrix deltaPseudoRangeRateMps =
        new Array2DRowRealMatrix(numberOfUsefulSatellites, 1);
    RealMatrix pseudorangeRateWeight
        = new Array2DRowRealMatrix(numberOfUsefulSatellites, numberOfUsefulSatellites);

    // Correct the receiver time of week with the estimated receiver clock bias
    receiverGPSTowAtReceptionSeconds =
        receiverGPSTowAtReceptionSeconds - positionVelocitySolutionECEF[3] / SPEED_OF_LIGHT_MPS;

    int measurementCount = 0;

    // Calculate range rates
    for (int i = 0; i < GpsNavigationMessageStore.MAX_NUMBER_OF_SATELLITES; i++) {
      if (mutableSmoothedSatellitesToReceiverMeasurements.get(i) != null) {
        GpsEphemerisProto ephemeridesProto = getEphemerisForSatellite(navMessageProto, i + 1);

        double pseudorangeMeasurementMeters =
            mutableSmoothedSatellitesToReceiverMeasurements.get(i).pseudorangeMeters;
        GpsTimeOfWeekAndWeekNumber correctedTowAndWeek =
            calculateCorrectedTransmitTowAndWeek(ephemeridesProto, receiverGPSTowAtReceptionSeconds,
                receiverGPSWeek, pseudorangeMeasurementMeters);

        // Calculate satellite velocity
        PositionAndVelocity satPosECEFMetersVelocityMPS = SatellitePositionCalculator
            .calculateSatellitePositionAndVelocityFromEphemeris(
                ephemeridesProto,
                correctedTowAndWeek.gpsTimeOfWeekSeconds,
                correctedTowAndWeek.weekNumber,
                positionVelocitySolutionECEF[0],
                positionVelocitySolutionECEF[1],
                positionVelocitySolutionECEF[2]);

        // Calculate satellite clock error rate
        double satelliteClockErrorRateMps = SatelliteClockCorrectionCalculator.
            calculateSatClockCorrErrorRate(
                ephemeridesProto,
                correctedTowAndWeek.gpsTimeOfWeekSeconds,
                correctedTowAndWeek.weekNumber);

        // Fill in range rates. range rate = satellite velocity (dot product) line-of-sight vector
        rangeRateMps.setEntry(measurementCount, 0,  -1 * (
            satPosECEFMetersVelocityMPS.velocityXMetersPerSec
                * geometryMatrix.getEntry(measurementCount, 0)
                + satPosECEFMetersVelocityMPS.velocityYMetersPerSec
                * geometryMatrix.getEntry(measurementCount, 1)
                + satPosECEFMetersVelocityMPS.velocityZMetersPerSec
                * geometryMatrix.getEntry(measurementCount, 2)));

        deltaPseudoRangeRateMps.setEntry(measurementCount, 0,
            mutableSmoothedSatellitesToReceiverMeasurements.get(i).pseudorangeRateMps
                - rangeRateMps.getEntry(measurementCount, 0) + satelliteClockErrorRateMps
                - positionVelocitySolutionECEF[7]);

        // Calculate the velocity weight matrix by using 1 / square(Pseudorangerate Uncertainty)
        // along the diagonal
        pseudorangeRateWeight.setEntry(measurementCount, measurementCount,
            1 / (mutableSmoothedSatellitesToReceiverMeasurements
                .get(i).pseudorangeRateUncertaintyMps
                * mutableSmoothedSatellitesToReceiverMeasurements
                .get(i).pseudorangeRateUncertaintyMps));
        measurementCount++;
      }
    }

    RealMatrix weightedGeoMatrix = pseudorangeRateWeight.multiply(geometryMatrix);
    RealMatrix deltaPseudoRangeRateWeightedMps =
        pseudorangeRateWeight.multiply(deltaPseudoRangeRateMps);
    QRDecomposition qrdWeightedGeoMatrix = new QRDecomposition(weightedGeoMatrix);
    RealMatrix velocityMps
        = qrdWeightedGeoMatrix.getSolver().solve(deltaPseudoRangeRateWeightedMps);
    positionVelocitySolutionECEF[4] = velocityMps.getEntry(0, 0);
    positionVelocitySolutionECEF[5] = velocityMps.getEntry(1, 0);
    positionVelocitySolutionECEF[6] = velocityMps.getEntry(2, 0);
    positionVelocitySolutionECEF[7] = velocityMps.getEntry(3, 0);

    RealMatrix pseudorangeWeight
        = new LUDecomposition(
            new Array2DRowRealMatrix(satPosPseudorangeResidualAndWeight.covarianceMatrixMetersSquare
            )
    ).getSolver().getInverse();

    // Calculate and store the uncertainties of position and velocity in local ENU system in meters
    // and meters per second.
    double[] pvUncertainty =
        calculatePositionVelocityUncertaintyEnu(pseudorangeRateWeight, pseudorangeWeight,
            positionVelocitySolutionECEF);
    System.arraycopy(pvUncertainty,
        0 /*source starting pos*/,
        positionVelocityUncertaintyEnu,
        0 /*destination starting pos*/,
        6 /*length of elements*/);
  }

  /**
   * Calculates the position uncertainty in meters and the velocity uncertainty
   * in meters per second solution in local ENU system.
   *
   * <p> Reference: Global Positioning System: Signals, Measurements, and Performance
   * by Pratap Misra, Per Enge, Page 206 - 209.
   *
   * @param velocityWeightMatrix the velocity weight matrix
   * @param positionWeightMatrix the position weight matrix
   * @param positionVelocitySolution the position and velocity solution in ECEF
   * @return an array containing the position and velocity uncertainties in ENU coordinate system.
   *         [0-2] Enu uncertainty of position solution in meters.
   *         [3-5] Enu uncertainty of velocity solution in meters per second.
   */
  public double[] calculatePositionVelocityUncertaintyEnu(
      RealMatrix velocityWeightMatrix, RealMatrix positionWeightMatrix,
      double[] positionVelocitySolution){

    if (geometryMatrix == null){
      return null;
    }

    RealMatrix velocityH = calculateHMatrix(velocityWeightMatrix, geometryMatrix);
    RealMatrix positionH = calculateHMatrix(positionWeightMatrix, geometryMatrix);

    // Calculate the rotation Matrix to convert to local ENU system.
    RealMatrix rotationMatrix = new Array2DRowRealMatrix(4, 4);
    GeodeticLlaValues llaValues = Ecef2LlaConverter.convertECEFToLLACloseForm
        (positionVelocitySolution[0], positionVelocitySolution[1], positionVelocitySolution[2]);
    rotationMatrix.setSubMatrix(
        Ecef2EnuConverter.getRotationMatrix(llaValues.longitudeRadians,
            llaValues.latitudeRadians).getData(), 0, 0);
    rotationMatrix.setEntry(3, 3, 1);

    // Convert to local ENU by pre-multiply rotation matrix and multiply rotation matrix transposed
    velocityH = rotationMatrix.multiply(velocityH).multiply(rotationMatrix.transpose());
    positionH = rotationMatrix.multiply(positionH).multiply(rotationMatrix.transpose());

    // Return the square root of diagonal entries
    return new double[] {
        Math.sqrt(positionH.getEntry(0, 0)), Math.sqrt(positionH.getEntry(1, 1)),
        Math.sqrt(positionH.getEntry(2, 2)), Math.sqrt(velocityH.getEntry(0, 0)),
        Math.sqrt(velocityH.getEntry(1, 1)), Math.sqrt(velocityH.getEntry(2, 2))};
  }

  /**
   * Calculates the measurement connection matrix H as a function of weightMatrix and
   * geometryMatrix.
   *
   * <p> H = (geometryMatrixTransposed * Weight * geometryMatrix) ^ -1
   *
   * <p> Reference: Global Positioning System: Signals, Measurements, and Performance, P207
   * @param weightMatrix Weights for computing H Matrix
   * @return H Matrix
   */
  private RealMatrix calculateHMatrix
      (RealMatrix weightMatrix, RealMatrix geometryMatrix){

    RealMatrix tempH = geometryMatrix.transpose().multiply(weightMatrix).multiply(geometryMatrix);
    return new LUDecomposition(tempH).getSolver().getInverse();
  }

  /**
   * Applies weighted least square iterations and corrects to the position solution until correction
   * is below threshold. An exception is thrown if the maximum number of iterations:
   * {@value #MAXIMUM_NUMBER_OF_LEAST_SQUARE_ITERATIONS} is reached without convergence.
   */
  private SatellitesPositionPseudorangesResidualAndCovarianceMatrix applyWeightedLeastSquare(
      GpsNavMessageProto navMessageProto,
      List<GpsMeasurementWithRangeAndUncertainty> usefulSatellitesToReceiverMeasurements,
      double receiverGPSTowAtReceptionSeconds,
      int receiverGPSWeek,
      int dayOfYear1To366,
      double[] positionSolutionECEF,
      double[] deltaPositionMeters,
      boolean doAtmosphericCorrections,
      SatellitesPositionPseudorangesResidualAndCovarianceMatrix satPosPseudorangeResidualAndWeight,
      RealMatrix weightMatrixMetersMinus2)
      throws Exception {
    RealMatrix weightedGeometryMatrix;
    int numberOfIterations = 0;

    while ((Math.abs(deltaPositionMeters[0]) + Math.abs(deltaPositionMeters[1])
        + Math.abs(deltaPositionMeters[2])) >= LEAST_SQUARE_TOLERANCE_METERS) {
      // Apply ionospheric and tropospheric corrections only if the applied correction to
      // position is below a specific threshold
      if ((Math.abs(deltaPositionMeters[0]) + Math.abs(deltaPositionMeters[1])
          + Math.abs(deltaPositionMeters[2])) < ATMPOSPHERIC_CORRECTIONS_THRESHOLD_METERS) {
        doAtmosphericCorrections = true;
      }
      // Calculate satellites' positions, measurement residual per visible satellite and
      // weight matrix for the iterative least square
      satPosPseudorangeResidualAndWeight =
          calculateSatPosAndPseudorangeResidual(
              navMessageProto,
              usefulSatellitesToReceiverMeasurements,
              receiverGPSTowAtReceptionSeconds,
              receiverGPSWeek,
              dayOfYear1To366,
              positionSolutionECEF,
              doAtmosphericCorrections);

      // Calculate the geometry matrix according to "Global Positioning System: Theory and
      // Applications", Parkinson and Spilker page 413
      geometryMatrix = new Array2DRowRealMatrix(calculateGeometryMatrix(
          satPosPseudorangeResidualAndWeight.satellitesPositionsMeters, positionSolutionECEF));
      // Apply weighted least square only if the covariance matrix is
      // not singular (has a non-zero determinant), otherwise apply ordinary least square.
      // The reason is to ignore reported signal to noise ratios by the receiver that can
      // lead to such singularities
      if (weightMatrixMetersMinus2 == null) {
        weightedGeometryMatrix = geometryMatrix;
      } else {
        RealMatrix hMatrix =
            calculateHMatrix(weightMatrixMetersMinus2, geometryMatrix);
        weightedGeometryMatrix = hMatrix.multiply(geometryMatrix.transpose())
            .multiply(weightMatrixMetersMinus2);
      }

      // Equation 9 page 413 from "Global Positioning System: Theory and Applicaitons",
      // Parkinson and Spilker
      deltaPositionMeters =
          GpsMathOperations.matrixByColVectMultiplication(
              weightedGeometryMatrix.getData(),
              satPosPseudorangeResidualAndWeight.pseudorangeResidualsMeters);

      // Apply corrections to the position estimate
      positionSolutionECEF[0] += deltaPositionMeters[0];
      positionSolutionECEF[1] += deltaPositionMeters[1];
      positionSolutionECEF[2] += deltaPositionMeters[2];
      positionSolutionECEF[3] += deltaPositionMeters[3];
      numberOfIterations++;
      Preconditions.checkArgument(numberOfIterations <= MAXIMUM_NUMBER_OF_LEAST_SQUARE_ITERATIONS,
          "Maximum number of least square iterations reached without convergance...");
    }
    return satPosPseudorangeResidualAndWeight;
  }

  /**
   * Removes satellites that have residuals above {@value #RESIDUAL_TO_REPEAT_LEAST_SQUARE_METERS}
   * from the {@code usefulSatellitesToReceiverMeasurements} list. Returns true if any satellite is
   * removed.
   */
  private boolean removeHighResidualSats(
      List<GpsMeasurementWithRangeAndUncertainty> usefulSatellitesToReceiverMeasurements,
      boolean repeatLeastSquare,
      SatellitesPositionPseudorangesResidualAndCovarianceMatrix satPosPseudorangeResidualAndWeight,
      int satsWithResidualBelowThreshold) {

    for (int i = 0; i < satPosPseudorangeResidualAndWeight.pseudorangeResidualsMeters.length; i++) {
      if (satsWithResidualBelowThreshold > MINIMUM_NUMER_OF_SATELLITES) {
        if (Math.abs(satPosPseudorangeResidualAndWeight.pseudorangeResidualsMeters[i]) 
            > RESIDUAL_TO_REPEAT_LEAST_SQUARE_METERS) {
          int prn = satPosPseudorangeResidualAndWeight.satellitePRNs[i];
          usefulSatellitesToReceiverMeasurements.set(prn - 1, null);
          satsWithResidualBelowThreshold--;
          repeatLeastSquare = true;
        }
      }
    }
    return repeatLeastSquare;
  }

  /**
   * Calculates position of all visible satellites and pseudorange measurement residual
   * (difference of measured to predicted pseudoranges) needed for the least square computation. The
   * result is stored in an instance of {@link
   * SatellitesPositionPseudorangesResidualAndCovarianceMatrix}
   *
   * @param navMeassageProto parameters of the navigation message
   * @param usefulSatellitesToReceiverMeasurements Map of useful satellite PRN to {@link
   *     GpsMeasurementWithRangeAndUncertainty} containing receiver measurements for computing the
   *     position solution
   * @param receiverGPSTowAtReceptionSeconds Receiver estimate of GPS time of week (seconds)
   * @param receiverGpsWeek Receiver estimate of GPS week (0-1024+)
   * @param dayOfYear1To366 The day of the year between 1 and 366
   * @param userPositionECEFMeters receiver ECEF position in meters
   * @param doAtmosphericCorrections boolean indicating if atmospheric range corrections should be
   *     applied
   * @return SatellitesPositionPseudorangesResidualAndCovarianceMatrix Object containing satellite
   *     prns, satellite positions in ECEF, pseudorange residuals and covariance matrix.
   */
  public SatellitesPositionPseudorangesResidualAndCovarianceMatrix
      calculateSatPosAndPseudorangeResidual(
          GpsNavMessageProto navMeassageProto,
          List<GpsMeasurementWithRangeAndUncertainty> usefulSatellitesToReceiverMeasurements,
          double receiverGPSTowAtReceptionSeconds,
          int receiverGpsWeek,
          int dayOfYear1To366,
          double[] userPositionECEFMeters,
          boolean doAtmosphericCorrections)
          throws Exception {
    int numberOfUsefulSatellites =
        getNumberOfUsefulSatellites(usefulSatellitesToReceiverMeasurements);
    // deltaPseudorange is the pseudorange measurement residual
    double[] deltaPseudorangesMeters = new double[numberOfUsefulSatellites];
    double[][] satellitesPositionsECEFMeters = new double[numberOfUsefulSatellites][3];

    // satellite PRNs
    int[] satellitePRNs = new int[numberOfUsefulSatellites];

    // Ionospheric model parameters
    double[] alpha =
            {navMeassageProto.iono.alpha[0], navMeassageProto.iono.alpha[1],
                    navMeassageProto.iono.alpha[2], navMeassageProto.iono.alpha[3]};
    double[] beta = {navMeassageProto.iono.beta[0], navMeassageProto.iono.beta[1],
            navMeassageProto.iono.beta[2], navMeassageProto.iono.beta[3]};
    // Weight matrix for the weighted least square
    RealMatrix covarianceMatrixMetersSquare =
        new Array2DRowRealMatrix(numberOfUsefulSatellites, numberOfUsefulSatellites);
    calculateSatPosAndResiduals(
        navMeassageProto,
        usefulSatellitesToReceiverMeasurements,
        receiverGPSTowAtReceptionSeconds,
        receiverGpsWeek,
        dayOfYear1To366,
        userPositionECEFMeters,
        doAtmosphericCorrections,
        deltaPseudorangesMeters,
        satellitesPositionsECEFMeters,
        satellitePRNs,
        alpha,
        beta,
        covarianceMatrixMetersSquare);

    return new SatellitesPositionPseudorangesResidualAndCovarianceMatrix(satellitePRNs,
        satellitesPositionsECEFMeters, deltaPseudorangesMeters,
        covarianceMatrixMetersSquare.getData());
  }

  /**
   * Calculates and fill the position of all visible satellites:
   * {@code satellitesPositionsECEFMeters}, pseudorange measurement residual (difference of
   * measured to predicted pseudoranges): {@code deltaPseudorangesMeters} and covariance matrix from
   * the weighted least square: {@code covarianceMatrixMetersSquare}. An array of the satellite PRNs
   * {@code satellitePRNs} is as well filled.
   */
  private void calculateSatPosAndResiduals(
      GpsNavMessageProto navMeassageProto,
      List<GpsMeasurementWithRangeAndUncertainty> usefulSatellitesToReceiverMeasurements,
      double receiverGPSTowAtReceptionSeconds,
      int receiverGpsWeek,
      int dayOfYear1To366,
      double[] userPositionECEFMeters,
      boolean doAtmosphericCorrections,
      double[] deltaPseudorangesMeters,
      double[][] satellitesPositionsECEFMeters,
      int[] satellitePRNs,
      double[] alpha,
      double[] beta,
      RealMatrix covarianceMatrixMetersSquare)
      throws Exception {
    // user position without the clock estimate
    double[] userPositionTempECEFMeters =
        {userPositionECEFMeters[0], userPositionECEFMeters[1], userPositionECEFMeters[2]};
    int satsCounter = 0;
    for (int i = 0; i < GpsNavigationMessageStore.MAX_NUMBER_OF_SATELLITES; i++) {
      if (usefulSatellitesToReceiverMeasurements.get(i) != null) {
        GpsEphemerisProto ephemeridesProto = getEphemerisForSatellite(navMeassageProto, i + 1);
        // Correct the receiver time of week with the estimated receiver clock bias
        receiverGPSTowAtReceptionSeconds =
            receiverGPSTowAtReceptionSeconds - userPositionECEFMeters[3] / SPEED_OF_LIGHT_MPS;

        double pseudorangeMeasurementMeters =
            usefulSatellitesToReceiverMeasurements.get(i).pseudorangeMeters;
        double pseudorangeUncertaintyMeters =
            usefulSatellitesToReceiverMeasurements.get(i).pseudorangeUncertaintyMeters;

        // Assuming uncorrelated pseudorange measurements, the covariance matrix will be diagonal as
        // follows
        covarianceMatrixMetersSquare.setEntry(satsCounter, satsCounter,
            pseudorangeUncertaintyMeters * pseudorangeUncertaintyMeters);

        // Calculate time of week at transmission time corrected with the satellite clock drift
        GpsTimeOfWeekAndWeekNumber correctedTowAndWeek =
            calculateCorrectedTransmitTowAndWeek(ephemeridesProto, receiverGPSTowAtReceptionSeconds,
                receiverGpsWeek, pseudorangeMeasurementMeters);

        // calculate satellite position and velocity
        PositionAndVelocity satPosECEFMetersVelocityMPS = SatellitePositionCalculator
            .calculateSatellitePositionAndVelocityFromEphemeris(ephemeridesProto,
                correctedTowAndWeek.gpsTimeOfWeekSeconds, correctedTowAndWeek.weekNumber,
                userPositionECEFMeters[0], userPositionECEFMeters[1], userPositionECEFMeters[2]);

        satellitesPositionsECEFMeters[satsCounter][0] = satPosECEFMetersVelocityMPS.positionXMeters;
        satellitesPositionsECEFMeters[satsCounter][1] = satPosECEFMetersVelocityMPS.positionYMeters;
        satellitesPositionsECEFMeters[satsCounter][2] = satPosECEFMetersVelocityMPS.positionZMeters;

        // Calculate ionospheric and tropospheric corrections
        double ionosphericCorrectionMeters;
        double troposphericCorrectionMeters;
        if (doAtmosphericCorrections) {
          ionosphericCorrectionMeters =
              IonosphericModel.ionoKloboucharCorrectionSeconds(
                      userPositionTempECEFMeters,
                      satellitesPositionsECEFMeters[satsCounter],
                      correctedTowAndWeek.gpsTimeOfWeekSeconds,
                      alpha,
                      beta,
                      IonosphericModel.L1_FREQ_HZ)
                  * SPEED_OF_LIGHT_MPS;

          troposphericCorrectionMeters =
              calculateTroposphericCorrectionMeters(
                  dayOfYear1To366,
                  satellitesPositionsECEFMeters,
                  userPositionTempECEFMeters,
                  satsCounter);
        } else {
          troposphericCorrectionMeters = 0.0;
          ionosphericCorrectionMeters = 0.0;
        }
        double predictedPseudorangeMeters =
            calculatePredictedPseudorange(userPositionECEFMeters, satellitesPositionsECEFMeters,
                userPositionTempECEFMeters, satsCounter, ephemeridesProto, correctedTowAndWeek,
                ionosphericCorrectionMeters, troposphericCorrectionMeters);

        // Pseudorange residual (difference of measured to predicted pseudoranges)
        deltaPseudorangesMeters[satsCounter] =
            pseudorangeMeasurementMeters - predictedPseudorangeMeters;

        // Satellite PRNs
        satellitePRNs[satsCounter] = i + 1;
        satsCounter++;
      }
    }
  }

  /** Searches ephemerides list for the ephemeris associated with current satellite in process */
  private GpsEphemerisProto getEphemerisForSatellite(GpsNavMessageProto navMeassageProto,
                                                     int satPrn) {
    List<GpsEphemerisProto> ephemeridesList
            = new ArrayList<GpsEphemerisProto>(Arrays.asList(navMeassageProto.ephemerids));
    GpsEphemerisProto ephemeridesProto = null;
    int ephemerisPrn = 0;
    for (GpsEphemerisProto ephProtoFromList : ephemeridesList) {
      ephemerisPrn = ephProtoFromList.prn;
      if (ephemerisPrn == satPrn) {
        ephemeridesProto = ephProtoFromList;
        break;
      }
    }
    return ephemeridesProto;
  }

  /** Calculates predicted pseudorange in meters */
  private double calculatePredictedPseudorange(
      double[] userPositionECEFMeters,
      double[][] satellitesPositionsECEFMeters,
      double[] userPositionNoClockECEFMeters,
      int satsCounter,
      GpsEphemerisProto ephemeridesProto,
      GpsTimeOfWeekAndWeekNumber correctedTowAndWeek,
      double ionosphericCorrectionMeters,
      double troposphericCorrectionMeters)
      throws Exception {
    // Calcualte the satellite clock drift
    double satelliteClockCorrectionMeters =
        SatelliteClockCorrectionCalculator.calculateSatClockCorrAndEccAnomAndTkIteratively(
                ephemeridesProto,
                correctedTowAndWeek.gpsTimeOfWeekSeconds,
                correctedTowAndWeek.weekNumber)
            .satelliteClockCorrectionMeters;

    double satelliteToUserDistanceMeters =
        GpsMathOperations.vectorNorm(GpsMathOperations.subtractTwoVectors(
            satellitesPositionsECEFMeters[satsCounter], userPositionNoClockECEFMeters));
    // Predicted pseudorange
    double predictedPseudorangeMeters =
        satelliteToUserDistanceMeters - satelliteClockCorrectionMeters + ionosphericCorrectionMeters
            + troposphericCorrectionMeters + userPositionECEFMeters[3];
    return predictedPseudorangeMeters;
  }

  /** Calculates the Gps tropospheric correction in meters */
  private double calculateTroposphericCorrectionMeters(int dayOfYear1To366,
      double[][] satellitesPositionsECEFMeters, double[] userPositionTempECEFMeters,
      int satsCounter) {
    double troposphericCorrectionMeters;
    TopocentricAEDValues elevationAzimuthDist =
        EcefToTopocentricConverter.convertCartesianToTopocentericRadMeters(
            userPositionTempECEFMeters, GpsMathOperations.subtractTwoVectors(
                satellitesPositionsECEFMeters[satsCounter], userPositionTempECEFMeters));

    GeodeticLlaValues lla =
        Ecef2LlaConverter.convertECEFToLLACloseForm(userPositionTempECEFMeters[0],
            userPositionTempECEFMeters[1], userPositionTempECEFMeters[2]);

    // Geoid of the area where the receiver is located is calculated once and used for the
    // rest of the dataset as it change very slowly over wide area. This to save the delay
    // associated with accessing Google Elevation API. We assume this very first iteration of WLS
    // will compute the correct altitude above the ellipsoid of the ground at the latitude and
    // longitude
    if (calculateGeoidMeters) {
      double elevationAboveSeaLevelMeters = 0;
      if (elevationApiHelper == null){
        System.out.println("No Google API key is set. Elevation above sea level is set to "
            + "default 0 meters. This may cause inaccuracy in tropospheric correction.");
      } else {
        try {
          elevationAboveSeaLevelMeters = elevationApiHelper
              .getElevationAboveSeaLevelMeters(
                  Math.toDegrees(lla.latitudeRadians), Math.toDegrees(lla.longitudeRadians)
              );
        } catch (Exception e){
          e.printStackTrace();
          System.out.println("Error when getting elevation from Google Server. "
              + "Could be wrong Api key or network error. Elevation above sea level is set to "
              + "default 0 meters. This may cause inaccuracy in tropospheric correction.");
        }
      }

      geoidHeightMeters = ElevationApiHelper.calculateGeoidHeightMeters(
              lla.altitudeMeters,
              elevationAboveSeaLevelMeters
      );
      troposphericCorrectionMeters = TroposphericModelEgnos.calculateTropoCorrectionMeters(
          elevationAzimuthDist.elevationRadians, lla.latitudeRadians, elevationAboveSeaLevelMeters,
          dayOfYear1To366);
    } else {
      troposphericCorrectionMeters = TroposphericModelEgnos.calculateTropoCorrectionMeters(
          elevationAzimuthDist.elevationRadians, lla.latitudeRadians,
          lla.altitudeMeters - geoidHeightMeters, dayOfYear1To366);
    }
    return troposphericCorrectionMeters;
  }

  /**
   * Gets the number of useful satellites from a list of
   * {@link GpsMeasurementWithRangeAndUncertainty}.
   */
  private int getNumberOfUsefulSatellites(
      List<GpsMeasurementWithRangeAndUncertainty> usefulSatellitesToReceiverMeasurements) {
    // calculate the number of useful satellites
    int numberOfUsefulSatellites = 0;
    for (int i = 0; i < usefulSatellitesToReceiverMeasurements.size(); i++) {
      if (usefulSatellitesToReceiverMeasurements.get(i) != null) {
        numberOfUsefulSatellites++;
      }
    }
    return numberOfUsefulSatellites;
  }

  /**
   * Computes the GPS time of week at the time of transmission and as well the corrected GPS week
   * taking into consideration week rollover. The returned GPS time of week is corrected by the
   * computed satellite clock drift. The result is stored in an instance of
   * {@link GpsTimeOfWeekAndWeekNumber}
   *
   * @param ephemerisProto parameters of the navigation message
   * @param receiverGpsTowAtReceptionSeconds Receiver estimate of GPS time of week when signal was
   *        received (seconds)
   * @param receiverGpsWeek Receiver estimate of GPS week (0-1024+)
   * @param pseudorangeMeters Measured pseudorange in meters
   * @return GpsTimeOfWeekAndWeekNumber Object containing Gps time of week and week number.
   */
  private static GpsTimeOfWeekAndWeekNumber calculateCorrectedTransmitTowAndWeek(
      GpsEphemerisProto ephemerisProto, double receiverGpsTowAtReceptionSeconds,
      int receiverGpsWeek, double pseudorangeMeters) throws Exception {
    // GPS time of week at time of transmission: Gps time corrected for transit time (page 98 ICD
    // GPS 200)
    double receiverGpsTowAtTimeOfTransmission =
        receiverGpsTowAtReceptionSeconds - pseudorangeMeters / SPEED_OF_LIGHT_MPS;

    // Adjust for week rollover
    if (receiverGpsTowAtTimeOfTransmission < 0) {
      receiverGpsTowAtTimeOfTransmission += SECONDS_IN_WEEK;
      receiverGpsWeek -= 1;
    } else if (receiverGpsTowAtTimeOfTransmission > SECONDS_IN_WEEK) {
      receiverGpsTowAtTimeOfTransmission -= SECONDS_IN_WEEK;
      receiverGpsWeek += 1;
    }

    // Compute the satellite clock correction term (Seconds)
    double clockCorrectionSeconds =
        SatelliteClockCorrectionCalculator.calculateSatClockCorrAndEccAnomAndTkIteratively(
            ephemerisProto, receiverGpsTowAtTimeOfTransmission,
            receiverGpsWeek).satelliteClockCorrectionMeters / SPEED_OF_LIGHT_MPS;

    // Correct with the satellite clock correction term
    double receiverGpsTowAtTimeOfTransmissionCorrectedSec =
        receiverGpsTowAtTimeOfTransmission + clockCorrectionSeconds;

    // Adjust for week rollover due to satellite clock correction
    if (receiverGpsTowAtTimeOfTransmissionCorrectedSec < 0.0) {
      receiverGpsTowAtTimeOfTransmissionCorrectedSec += SECONDS_IN_WEEK;
      receiverGpsWeek -= 1;
    }
    if (receiverGpsTowAtTimeOfTransmissionCorrectedSec > SECONDS_IN_WEEK) {
      receiverGpsTowAtTimeOfTransmissionCorrectedSec -= SECONDS_IN_WEEK;
      receiverGpsWeek += 1;
    }
    return new GpsTimeOfWeekAndWeekNumber(receiverGpsTowAtTimeOfTransmissionCorrectedSec,
        receiverGpsWeek);
  }

  /**
   * Calculates the Geometry matrix (describing user to satellite geometry) given a list of
   * satellite positions in ECEF coordinates in meters and the user position in ECEF in meters.
   *
   * <p>The geometry matrix has four columns, and rows equal to the number of satellites. For each
   * of the rows (i.e. for each of the satellites used), the columns are filled with the normalized
   * line–of-sight vectors and 1 s for the fourth column.
   *
   * <p>Source: Parkinson, B.W., Spilker Jr., J.J.: ‘Global positioning system: theory and
   * applications’ page 413
   */
  private static double[][] calculateGeometryMatrix(double[][] satellitePositionsECEFMeters,
      double[] userPositionECEFMeters) {

    double[][] geometeryMatrix = new double[satellitePositionsECEFMeters.length][4];
    for (int i = 0; i < satellitePositionsECEFMeters.length; i++) {
      geometeryMatrix[i][3] = 1;
    }
    // iterate over all satellites
    for (int i = 0; i < satellitePositionsECEFMeters.length; i++) {
      double[] r = {satellitePositionsECEFMeters[i][0] - userPositionECEFMeters[0],
          satellitePositionsECEFMeters[i][1] - userPositionECEFMeters[1],
          satellitePositionsECEFMeters[i][2] - userPositionECEFMeters[2]};
      double norm = Math.sqrt(Math.pow(r[0], 2) + Math.pow(r[1], 2) + Math.pow(r[2], 2));
      for (int j = 0; j < 3; j++) {
        geometeryMatrix[i][j] =
            (userPositionECEFMeters[j] - satellitePositionsECEFMeters[i][j]) / norm;
      }
    }
    return geometeryMatrix;
  }

  /**
   * Class containing satellites' PRNs, satellites' positions in ECEF meters, the pseudorange
   * residual per visible satellite in meters and the covariance matrix of the
   * pseudoranges in meters square
   */
  protected static class SatellitesPositionPseudorangesResidualAndCovarianceMatrix {

    /** Satellites' PRNs */
    protected final int[] satellitePRNs;

    /** ECEF positions (meters) of useful satellites */
    protected final double[][] satellitesPositionsMeters;

    /** Pseudorange measurement residuals (difference of measured to predicted pseudoranges) */
    protected final double[] pseudorangeResidualsMeters;

    /** Pseudorange covariance Matrix for the weighted least squares (meters square) */
    protected final double[][] covarianceMatrixMetersSquare;

    /** Constructor */
    private SatellitesPositionPseudorangesResidualAndCovarianceMatrix(int[] satellitePRNs,
        double[][] satellitesPositionsMeters, double[] pseudorangeResidualsMeters,
        double[][] covarianceMatrixMetersSquare) {
      this.satellitePRNs = satellitePRNs;
      this.satellitesPositionsMeters = satellitesPositionsMeters;
      this.pseudorangeResidualsMeters = pseudorangeResidualsMeters;
      this.covarianceMatrixMetersSquare = covarianceMatrixMetersSquare;
    }

  }

  /**
   * Class containing GPS time of week in seconds and GPS week number
   */
  private static class GpsTimeOfWeekAndWeekNumber {
    /** GPS time of week in seconds */
    private final double gpsTimeOfWeekSeconds;

    /** GPS week number */
    private final int weekNumber;

    /** Constructor */
    private GpsTimeOfWeekAndWeekNumber(double gpsTimeOfWeekSeconds, int weekNumber) {
      this.gpsTimeOfWeekSeconds = gpsTimeOfWeekSeconds;
      this.weekNumber = weekNumber;
    }
  }
  
  /**
   * Uses the common reception time approach to calculate pseudoranges from the time of week
   * measurements reported by the receiver according to http://cdn.intechopen.com/pdfs-wm/27712.pdf.
   * As well computes the pseudoranges uncertainties for each input satellite
   */
  @VisibleForTesting
  static List<GpsMeasurementWithRangeAndUncertainty> computePseudorangeAndUncertainties(
      List<GpsMeasurement> usefulSatellitesToReceiverMeasurements,
      Long[] usefulSatellitesToTOWNs,
      long largestTowNs) {

    List<GpsMeasurementWithRangeAndUncertainty> usefulSatellitesToPseudorangeMeasurements =
        Arrays.asList(
            new GpsMeasurementWithRangeAndUncertainty
                [GpsNavigationMessageStore.MAX_NUMBER_OF_SATELLITES]);
    for (int i = 0; i < GpsNavigationMessageStore.MAX_NUMBER_OF_SATELLITES; i++) {
      if (usefulSatellitesToTOWNs[i] != null) {
        double deltai = largestTowNs - usefulSatellitesToTOWNs[i];
        double pseudorangeMeters =
            (AVERAGE_TRAVEL_TIME_SECONDS + deltai * SECONDS_PER_NANO) * SPEED_OF_LIGHT_MPS;

        double signalToNoiseRatioLinear =
            Math.pow(10, usefulSatellitesToReceiverMeasurements.get(i).signalToNoiseRatioDb / 10.0);
        // From Global Positoning System book, Misra and Enge, page 416, the uncertainty of the
        // pseudorange measurement is calculated next.
        // For GPS C/A code chip width Tc = 1 microseconds. Narrow correlator with spacing d = 0.1
        // chip and an average time of DLL correlator T of 20 milliseconds are used.
        double sigmaMeters =
            SPEED_OF_LIGHT_MPS
                * GPS_CHIP_WIDTH_T_C_SEC
                * Math.sqrt(
                    GPS_CORRELATOR_SPACING_IN_CHIPS
                        / (4 * GPS_DLL_AVERAGING_TIME_SEC * signalToNoiseRatioLinear));
        usefulSatellitesToPseudorangeMeasurements.set(
            i,
            new GpsMeasurementWithRangeAndUncertainty(
                usefulSatellitesToReceiverMeasurements.get(i), pseudorangeMeters, sigmaMeters));
      }
    }
    return usefulSatellitesToPseudorangeMeasurements;
  }

}
