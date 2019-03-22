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

package com.google.android.apps.location.gps.gnsslogger;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * A map fragment to show the computed least square position and the device computed position on
 * Google map.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback {
  private static final float ZOOM_LEVEL = 15;
  private static final String TAG = "MapFragment";
  private RealTimePositionVelocityCalculator mPositionVelocityCalculator;

  private static final SimpleDateFormat DATE_SDF = new SimpleDateFormat("HH:mm:ss");

  // UI members
  private GoogleMap mMap; // Might be null if Google Play services APK is not available.
  private MapView mMapView;
  private final Set<Object> mSetOfFeatures = new HashSet<Object>();

  private Marker mLastLocationMarkerRaw = null;
  private Marker mLastLocationMarkerDevice = null;

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.map_fragment, container, false);
    mMapView = ((MapView) rootView.findViewById(R.id.map));
    mMapView.onCreate(savedInstanceState);
    mMapView.getMapAsync(this);
    MapsInitializer.initialize(getActivity());

    RealTimePositionVelocityCalculator currentPositionVelocityCalculator =
        mPositionVelocityCalculator;
    if (currentPositionVelocityCalculator != null) {
      currentPositionVelocityCalculator.setMapFragment(this);
    }

    return rootView;
  }

  @Override
  public void onResume() {
    super.onResume();
    mMapView.onResume();
    if (mMap != null) {
      mMap.clear();
    }
    mLastLocationMarkerRaw = null;
    mLastLocationMarkerDevice = null;
  }

  @Override
  public void onPause() {
    mMapView.onPause();
    super.onPause();
  }

  @Override
  public void onDestroy() {
    mMapView.onDestroy();
    super.onDestroy();
  }

  @Override
  public void onMapReady(GoogleMap googleMap) {
    mMap = googleMap;
    mMap.setMyLocationEnabled(false);
    mMap.getUiSettings().setZoomControlsEnabled(true);
    mMap.getUiSettings().setZoomGesturesEnabled(true);
    mMap.getUiSettings().setMapToolbarEnabled(false);
  }

  public void setPositionVelocityCalculator(RealTimePositionVelocityCalculator value) {
    mPositionVelocityCalculator = value;
  }

  public void updateMapViewWithPositions(
      final double latDegRaw,
      final double lngDegRaw,
      final double latDegDevice,
      final double lngDegDevice,
      final long timeMillis) {
    Activity activity = getActivity();
    if (activity == null) {
      return;
    }
    activity.runOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            Log.i(TAG, "onLocationChanged");
            LatLng latLngRaw = new LatLng(latDegRaw, lngDegRaw);
            LatLng latLngDevice = new LatLng(latDegDevice, lngDegDevice);
            if (mLastLocationMarkerRaw == null && mLastLocationMarkerDevice == null) {
              if (mMap != null) {
                mLastLocationMarkerDevice =
                    mMap.addMarker(
                        new MarkerOptions()
                            .position(latLngDevice)
                            .title(getResources().getString(R.string.title_device))
                            .icon(
                                BitmapDescriptorFactory.defaultMarker(
                                    BitmapDescriptorFactory.HUE_BLUE)));
                mLastLocationMarkerDevice.showInfoWindow();

                mLastLocationMarkerRaw =
                    mMap.addMarker(
                        new MarkerOptions()
                            .position(latLngRaw)
                            .title(getResources().getString(R.string.title_wls))
                            .icon(
                                BitmapDescriptorFactory.defaultMarker(
                                    BitmapDescriptorFactory.HUE_GREEN)));
                mLastLocationMarkerRaw.showInfoWindow();

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngRaw, ZOOM_LEVEL));
              }
            } else {
              mLastLocationMarkerRaw.setPosition(latLngRaw);
              mLastLocationMarkerDevice.setPosition(latLngDevice);
            }
            if (mLastLocationMarkerRaw == null && mLastLocationMarkerDevice == null) {
              String formattedDate = DATE_SDF.format(new Date(timeMillis));
              mLastLocationMarkerRaw.setTitle("time: " + formattedDate);
              mLastLocationMarkerDevice.showInfoWindow();

              mLastLocationMarkerRaw.setTitle("time: " + formattedDate);
              mLastLocationMarkerDevice.showInfoWindow();
            }
          }
        });
  }

  public void clearMarkers() {
    Activity activity = getActivity();
    if (activity == null) {
      return;
    }
    activity.runOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            if (mLastLocationMarkerRaw != null) {
              mLastLocationMarkerRaw.remove();
              mLastLocationMarkerRaw = null;
            }
            if (mLastLocationMarkerDevice != null) {
              mLastLocationMarkerDevice.remove();
              mLastLocationMarkerDevice = null;
            }
          }
        });
  }
}
