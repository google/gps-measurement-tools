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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Preconditions;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A helper class to access the Google Elevation API for computing the Terrain Elevation Above Sea
 * level at a given location (lat, lng). An Elevation API key is required for getting elevation
 * above sea level from Google server.
 *
 * <p> For more information please see:
 * https://developers.google.com/maps/documentation/elevation/start
 *
 * <p> A key can be conveniently acquired from:
 *  https://developers.google.com/maps/documentation/elevation/get-api-key
 */

public class ElevationApiHelper {

  private static final String ELEVATION_XML_STRING = "<elevation>";
  private static final String GOOGLE_ELEVATION_API_HTTP_ADDRESS =
      "https://maps.googleapis.com/maps/api/elevation/xml?locations=";
  private String elevationApiKey = "";

  /**
   * A constructor that passes the {@code elevationApiKey}. If the user pass an empty string for
   * API Key, an {@code IllegalArgumentException} will be thrown.
   */
  public ElevationApiHelper(String elevationApiKey){
    // An Elevation API key must be provided for getting elevation from Google Server.
    Preconditions.checkArgument(!elevationApiKey.isEmpty());
    this.elevationApiKey = elevationApiKey;
  }

  /**
   *  Calculates the geoid height by subtracting the elevation above sea level from the ellipsoid
   *  height in altitude meters.
   */
  public static double calculateGeoidHeightMeters(double altitudeMeters,
      double elevationAboveSeaLevelMeters){
    return altitudeMeters - elevationAboveSeaLevelMeters;
  }

  /**
   * Gets elevation (height above sea level) via the Google elevation API by requesting
   * elevation for a given latitude and longitude. Longitude and latitude should be in decimal
   * degrees and the returned elevation will be in meters.
   */
  public double getElevationAboveSeaLevelMeters(double latitudeDegrees,
      double longitudeDegrees) throws Exception{

    String url =
        GOOGLE_ELEVATION_API_HTTP_ADDRESS
            + latitudeDegrees
            + ","
            + longitudeDegrees
            + "&key="
            + elevationApiKey;
    String elevationMeters = "0.0";

    HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
    InputStream content = urlConnection.getInputStream();
    BufferedReader buffer = new BufferedReader(new InputStreamReader(content, UTF_8));
    String line;
    while ((line = buffer.readLine()) != null) {
      line = line.trim();
      if (line.startsWith(ELEVATION_XML_STRING)) {
        // read the part of the line after the opening tag <elevation>
        String substring = line.substring(ELEVATION_XML_STRING.length(), line.length());
        // read the part of the line until before the closing tag <elevation>
        elevationMeters =
            substring.substring(0, substring.length() - ELEVATION_XML_STRING.length() - 1);
      }
    }
    return Double.parseDouble(elevationMeters);
  }

}
