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

import java.util.Arrays;

/**
 * Helper class containing the basic vector and matrix operations used for calculating the position
 * solution from pseudoranges
 *
 */
public class GpsMathOperations {

  /**
   * Calculates the norm of a vector
   */
  public static double vectorNorm(double[] inputVector) {
    double normSqured = 0;
    for (int i = 0; i < inputVector.length; i++) {
      normSqured = Math.pow(inputVector[i], 2) + normSqured;
    }

    return Math.sqrt(normSqured);
  }

  /**
   * Subtract two vectors {@code firstVector} - {@code secondVector}. Both vectors should be of the
   * same length.
   */
  public static double[] subtractTwoVectors(double[] firstVector, double[] secondVector)
      throws ArithmeticException {
    double[] result = new double[firstVector.length];
    if (firstVector.length != secondVector.length) {
      throw new ArithmeticException("Input vectors are of different lengths");
    }

    for (int i = 0; i < firstVector.length; i++) {
      result[i] = firstVector[i] - secondVector[i];
    }

    return result;
  }

  /**
   * Multiply a matrix {@code matrix} by a column vector {@code vector}
   * ({@code matrix} * {@code vector}) and return the resulting vector {@resultVector}.
   * {@code matrix} and {@resultVector} dimensions must match.
   */
  public static double[] matrixByColVectMultiplication(double[][] matrix, double[] resultVector)
      throws ArithmeticException {
    double[] result = new double[matrix.length];
    int matrixLength = matrix.length;
    int vectorLength = resultVector.length;
    if (vectorLength != matrix[0].length) {
      throw new ArithmeticException("Matrix and vector dimensions do not match");
    }

    for (int i = 0; i < matrixLength; i++) {
      for (int j = 0; j < vectorLength; j++) {
        result[i] += matrix[i][j] * resultVector[j];
      }
    }

    return result;
  }

  /**
   * Dot product of a raw vector {@code firstVector} and a column vector {@code secondVector}.
   * Both vectors should be of the same length.
   */
  public static double dotProduct(double[] firstVector, double[] secondVector)
      throws ArithmeticException {
    if (firstVector.length != secondVector.length) {
      throw new ArithmeticException("Input vectors are of different lengths");
    }
    double result = 0;
    for (int i = 0; i < firstVector.length; i++) {
      result = firstVector[i] * secondVector[i] + result;
    }
    return result;
  }

  /**
   * Finds the index of max value in a vector {@code vector} filtering out NaNs, return -1 if
   * the vector is empty or only contain NaNs.
   */
  public static int maxIndexOfVector(double[] vector) {
    double max = Double.NEGATIVE_INFINITY;
    int index = -1;

    for (int i = 0; i < vector.length; i++) {
      if (!Double.isNaN(vector[i])) {
        if (vector[i] > max) {
          index = i;
          max = vector[i];
        }
      }
    }

    return index;
  }

  /**
   * Subtracts every element in a vector {@code vector} by a scalar {@code scalar}. We do not need
   *  to filter out NaN in this case because NaN subtract by a real number will still be NaN.
   */
  public static double[] subtractByScalar(double[] vector, double scalar) {
    double[] result = new double[vector.length];

    for (int i = 0; i < vector.length; i++) {
      result[i] = vector[i] - scalar;
    }

    return result;
  }

  /**
   * Computes the mean value of a vector {@code vector}, filtering out NaNs. If no non-NaN exists,
   * return Double.NaN {@link Double#NaN}
   */
  public static double meanOfVector(double[] vector) {
    double sum = 0;
    double size = 0;

    for (int i = 0; i < vector.length; i++) {
      if (!Double.isNaN(vector[i])) {
        sum += vector[i];
        size++;
      }
    }
    return size == 0 ? Double.NaN : sum / size;
  }

  /** Creates a numeric array of size {@code size} and fills it with the value {@code value} */
  public static double[] createAndFillArray(int size, double value) {
    double[] vector = new double[size];

    Arrays.fill(vector, value);

    return vector;
  }
}
