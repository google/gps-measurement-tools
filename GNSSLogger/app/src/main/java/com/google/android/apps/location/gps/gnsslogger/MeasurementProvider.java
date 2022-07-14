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

import android.content.Context;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Bundle;
import android.os.SystemClock;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A container for measurement-related API calls. It binds the measurement providers with the
 * various {@link MeasurementListener} implementations.
 */
public class MeasurementProvider {

  public static final String TAG = "MeasurementProvider";

  private static final long LOCATION_RATE_GPS_MS = TimeUnit.SECONDS.toMillis(1L);
  private static final long LOCATION_RATE_NETWORK_MS = TimeUnit.SECONDS.toMillis(60L);

  private boolean mLogLocations = true;
  private boolean mLogNavigationMessages = true;
  private boolean mLogMeasurements = true;
  private boolean mLogStatuses = true;
  private boolean mLogNmeas = true;
  private long registrationTimeNanos = 0L;
  private long firstLocationTimeNanos = 0L;
  private long ttff = 0L;
  private boolean firstTime = true;

  GoogleApiClient mGoogleApiClient;
  private final List<MeasurementListener> mListeners;

  private final LocationManager mLocationManager;
  private final android.location.LocationListener mLocationListener =
      new android.location.LocationListener() {

        @Override
        public void onProviderEnabled(String provider) {
          if (mLogLocations) {
            for (MeasurementListener listener : mListeners) {
              if (listener instanceof AgnssUiLogger && !firstTime) {
                continue;
              }
              listener.onProviderEnabled(provider);
            }
          }
        }

        @Override
        public void onProviderDisabled(String provider) {
          if (mLogLocations) {
            for (MeasurementListener logger : mListeners) {
              if (logger instanceof AgnssUiLogger && !firstTime) {
                continue;
              }
              logger.onProviderDisabled(provider);
            }
          }
        }

        @Override
        public void onLocationChanged(Location location) {
          if (firstTime && location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            if (mLogLocations) {
              for (MeasurementListener logger : mListeners) {
                firstLocationTimeNanos = SystemClock.elapsedRealtimeNanos();
                ttff = firstLocationTimeNanos - registrationTimeNanos;
                logger.onTTFFReceived(ttff);
              }
            }
            firstTime = false;
          }
          if (mLogLocations) {
            for (MeasurementListener logger : mListeners) {
              if (logger instanceof AgnssUiLogger && !firstTime) {
                continue;
              }
              logger.onLocationChanged(location);
            }
          }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
          if (mLogLocations) {
            for (MeasurementListener logger : mListeners) {
              logger.onLocationStatusChanged(provider, status, extras);
            }
          }
        }
      };

  private final com.google.android.gms.location.LocationListener mFusedLocationListener =
      new com.google.android.gms.location.LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
          if (firstTime && location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            if (mLogLocations) {
              for (MeasurementListener logger : mListeners) {
                firstLocationTimeNanos = SystemClock.elapsedRealtimeNanos();
                ttff = firstLocationTimeNanos - registrationTimeNanos;
                logger.onTTFFReceived(ttff);
              }
            }
            firstTime = false;
          }
          if (mLogLocations) {
            for (MeasurementListener logger : mListeners) {
              if (logger instanceof AgnssUiLogger && !firstTime) {
                continue;
              }
              logger.onLocationChanged(location);
            }
          }
        }
      };

  private final GnssMeasurementsEvent.Callback gnssMeasurementsEventListener =
      new GnssMeasurementsEvent.Callback() {
        @Override
        public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
          if (mLogMeasurements) {
            for (MeasurementListener logger : mListeners) {
              logger.onGnssMeasurementsReceived(event);
            }
          }
        }

        @Override
        public void onStatusChanged(int status) {
          if (mLogMeasurements) {
            for (MeasurementListener logger : mListeners) {
              logger.onGnssMeasurementsStatusChanged(status);
            }
          }
        }
      };

  private final GnssNavigationMessage.Callback gnssNavigationMessageListener =
      new GnssNavigationMessage.Callback() {
        @Override
        public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
          if (mLogNavigationMessages) {
            for (MeasurementListener logger : mListeners) {
              logger.onGnssNavigationMessageReceived(event);
            }
          }
        }

        @Override
        public void onStatusChanged(int status) {
          if (mLogNavigationMessages) {
            for (MeasurementListener logger : mListeners) {
              logger.onGnssNavigationMessageStatusChanged(status);
            }
          }
        }
      };

  private final GnssStatus.Callback gnssStatusListener =
      new GnssStatus.Callback() {
        @Override
        public void onStarted() {}

        @Override
        public void onStopped() {}

        @Override
        public void onFirstFix(int ttff) {}

        @Override
        public void onSatelliteStatusChanged(GnssStatus status) {
          for (MeasurementListener logger : mListeners) {
            logger.onGnssStatusChanged(status);
          }
        }
      };

  private final OnNmeaMessageListener nmeaListener =
      new OnNmeaMessageListener() {
        @Override
        public void onNmeaMessage(String s, long l) {
          if (mLogNmeas) {
            for (MeasurementListener logger : mListeners) {
              logger.onNmeaReceived(l, s);
            }
          }
        }
      };

  public MeasurementProvider(
      Context context, GoogleApiClient client, MeasurementListener... loggers) {
    this.mListeners = Arrays.asList(loggers);
    mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    this.mGoogleApiClient = client;
  }

  public LocationManager getLocationManager() {
    return mLocationManager;
  }

  public void setLogLocations(boolean value) {
    mLogLocations = value;
  }

  public boolean canLogLocations() {
    return mLogLocations;
  }

  public void setLogNavigationMessages(boolean value) {
    mLogNavigationMessages = value;
  }

  public boolean canLogNavigationMessages() {
    return mLogNavigationMessages;
  }

  public void setLogMeasurements(boolean value) {
    mLogMeasurements = value;
  }

  public boolean canLogMeasurements() {
    return mLogMeasurements;
  }

  public void setLogStatuses(boolean value) {
    mLogStatuses = value;
  }

  public boolean canLogStatuses() {
    return mLogStatuses;
  }

  public void setLogNmeas(boolean value) {
    mLogNmeas = value;
  }

  public boolean canLogNmeas() {
    return mLogNmeas;
  }

  public void registerLocation() {
    boolean isGpsProviderEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    if (isGpsProviderEnabled) {
      try {
        mLocationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            LOCATION_RATE_NETWORK_MS,
            0.0f /* minDistance */,
            mLocationListener);
        mLocationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            LOCATION_RATE_GPS_MS,
            0.0f /* minDistance */,
            mLocationListener);
      } catch (SecurityException e) {
        // TODO(adaext)
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
      }
    }
    logRegistration("LocationUpdates", isGpsProviderEnabled);
  }

  public void unregisterLocation() {
    mLocationManager.removeUpdates(mLocationListener);
  }

  public void registerFusedLocation() {
    LocationRequest locationRequest = new LocationRequest();
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    locationRequest.setInterval(1000);
    locationRequest.setFastestInterval(100);
    try {
      LocationServices.FusedLocationApi.requestLocationUpdates(
          mGoogleApiClient, locationRequest, mFusedLocationListener);
    } catch (SecurityException e) {
      // TODO(adaext):
      //    ActivityCompat#requestPermissions
      // here to request the missing permissions, and then overriding
      //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
      //                                          int[] grantResults)
      // to handle the case where the user grants the permission. See the documentation
      // for ActivityCompat#requestPermissions for more details.
    }
  }

  public void unRegisterFusedLocation() {
    if (mGoogleApiClient != null) {
      LocationServices.FusedLocationApi.removeLocationUpdates(
          mGoogleApiClient, mFusedLocationListener);
    }
  }

  public void registerSingleNetworkLocation() {
    boolean isNetworkProviderEnabled =
        mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    if (isNetworkProviderEnabled) {
      try {
        mLocationManager.requestSingleUpdate(
            LocationManager.NETWORK_PROVIDER, mLocationListener, null);
      } catch (SecurityException e) {
        // TODO(adaext):
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
      }
    }
    logRegistration("LocationUpdates", isNetworkProviderEnabled);
  }

  public void registerSingleGpsLocation() {
    boolean isGpsProviderEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    if (isGpsProviderEnabled) {
      this.firstTime = true;
      registrationTimeNanos = SystemClock.elapsedRealtimeNanos();
      try {
        mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocationListener, null);
      } catch (SecurityException e) {
        // TODO(adaext):
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
      }
    }
    logRegistration("LocationUpdates", isGpsProviderEnabled);
  }

  public void registerMeasurements() {
    try {
      logRegistration(
          "GnssMeasurements",
          mLocationManager.registerGnssMeasurementsCallback(gnssMeasurementsEventListener));
    } catch (SecurityException e) {
      // TODO(adaext):
      //    ActivityCompat#requestPermissions
      // here to request the missing permissions, and then overriding
      //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
      //                                          int[] grantResults)
      // to handle the case where the user grants the permission. See the documentation
      // for ActivityCompat#requestPermissions for more details.
    }
  }

  public void unregisterMeasurements() {
    mLocationManager.unregisterGnssMeasurementsCallback(gnssMeasurementsEventListener);
  }

  public void registerNavigation() {
    logRegistration(
        "GpsNavigationMessage",
        mLocationManager.registerGnssNavigationMessageCallback(gnssNavigationMessageListener));
  }

  public void unregisterNavigation() {
    mLocationManager.unregisterGnssNavigationMessageCallback(gnssNavigationMessageListener);
  }

  public void registerGnssStatus() {
    try {
      logRegistration(
          "GnssStatus", mLocationManager.registerGnssStatusCallback(gnssStatusListener));
    } catch (SecurityException e) {
      // TODO(adaext):
      //    ActivityCompat#requestPermissions
      // here to request the missing permissions, and then overriding
      //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
      //                                          int[] grantResults)
      // to handle the case where the user grants the permission. See the documentation
      // for ActivityCompat#requestPermissions for more details.
    }
  }

  public void unregisterGpsStatus() {
    mLocationManager.unregisterGnssStatusCallback(gnssStatusListener);
  }

  public void registerNmea() {
    try {
      logRegistration("Nmea", mLocationManager.addNmeaListener(nmeaListener));
    } catch (SecurityException e) {
      // TODO(adaext):
      //    ActivityCompat#requestPermissions
      // here to request the missing permissions, and then overriding
      //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
      //                                          int[] grantResults)
      // to handle the case where the user grants the permission. See the documentation
      // for ActivityCompat#requestPermissions for more details.
    }
  }

  public void unregisterNmea() {
    mLocationManager.removeNmeaListener(nmeaListener);
  }

  public void registerAll() {
    registerLocation();
    registerMeasurements();
    registerNavigation();
    registerGnssStatus();
    registerNmea();
  }

  public void unregisterAll() {
    unregisterLocation();
    unregisterMeasurements();
    unregisterNavigation();
    unregisterGpsStatus();
    unregisterNmea();
  }

  private void logRegistration(String listener, boolean result) {
    for (MeasurementListener logger : mListeners) {
      if (logger instanceof AgnssUiLogger && !firstTime) {
        continue;
      }
      logger.onListenerRegistration(listener, result);
    }
  }
}
