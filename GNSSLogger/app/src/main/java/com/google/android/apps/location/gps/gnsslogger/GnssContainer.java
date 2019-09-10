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
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Bundle;
import android.os.SystemClock;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A container for GPS related API calls, it binds the {@link LocationManager} with {@link UiLogger}
 */
public class GnssContainer {

  public static final String TAG = "GnssLogger";

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

  private final List<GnssListener> mLoggers;

  private final LocationManager mLocationManager;
  private final LocationListener mLocationListener =
      new LocationListener() {

        @Override
        public void onProviderEnabled(String provider) {
          if (mLogLocations) {
            for (GnssListener logger : mLoggers) {
              if (logger instanceof AgnssUiLogger && !firstTime) {
                continue;
              }
              logger.onProviderEnabled(provider);
            }
          }
        }

        @Override
        public void onProviderDisabled(String provider) {
          if (mLogLocations) {
            for (GnssListener logger : mLoggers) {
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
              for (GnssListener logger : mLoggers) {
                firstLocationTimeNanos = SystemClock.elapsedRealtimeNanos();
                ttff = firstLocationTimeNanos - registrationTimeNanos;
                logger.onTTFFReceived(ttff);
              }
            }
            firstTime = false;
          }
          if (mLogLocations) {
            for (GnssListener logger : mLoggers) {
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
            for (GnssListener logger : mLoggers) {
              logger.onLocationStatusChanged(provider, status, extras);
            }
          }
        }
      };

  private final GnssMeasurementsEvent.Callback gnssMeasurementsEventListener =
      new GnssMeasurementsEvent.Callback() {
        @Override
        public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
          if (mLogMeasurements) {
            for (GnssListener logger : mLoggers) {
              logger.onGnssMeasurementsReceived(event);
            }
          }
        }

        @Override
        public void onStatusChanged(int status) {
          if (mLogMeasurements) {
            for (GnssListener logger : mLoggers) {
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
            for (GnssListener logger : mLoggers) {
              logger.onGnssNavigationMessageReceived(event);
            }
          }
        }

        @Override
        public void onStatusChanged(int status) {
          if (mLogNavigationMessages) {
            for (GnssListener logger : mLoggers) {
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
          for (GnssListener logger : mLoggers) {
            logger.onGnssStatusChanged(status);
          }
        }
      };

  private final OnNmeaMessageListener nmeaListener =
      new OnNmeaMessageListener() {
        @Override
        public void onNmeaMessage(String s, long l) {
          if (mLogNmeas) {
            for (GnssListener logger : mLoggers) {
              logger.onNmeaReceived(l, s);
            }
          }
        }
      };

  public GnssContainer(Context context, GnssListener... loggers) {
    this.mLoggers = Arrays.asList(loggers);
    mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
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
    }
    logRegistration("LocationUpdates", isGpsProviderEnabled);
  }

  public void registerSingleNetworkLocation() {
    boolean isNetworkProviderEnabled =
        mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    if (isNetworkProviderEnabled) {
      mLocationManager.requestSingleUpdate(
          LocationManager.NETWORK_PROVIDER, mLocationListener, null);
    }
    logRegistration("LocationUpdates", isNetworkProviderEnabled);
  }

  public void registerSingleGpsLocation() {
    boolean isGpsProviderEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    if (isGpsProviderEnabled) {
      this.firstTime = true;
      registrationTimeNanos = SystemClock.elapsedRealtimeNanos();
      mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocationListener, null);
    }
    logRegistration("LocationUpdates", isGpsProviderEnabled);
  }

  public void unregisterLocation() {
    mLocationManager.removeUpdates(mLocationListener);
  }

  public void registerMeasurements() {
    logRegistration(
        "GnssMeasurements",
        mLocationManager.registerGnssMeasurementsCallback(gnssMeasurementsEventListener));
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
    logRegistration("GnssStatus", mLocationManager.registerGnssStatusCallback(gnssStatusListener));
  }

  public void unregisterGpsStatus() {
    mLocationManager.unregisterGnssStatusCallback(gnssStatusListener);
  }

  public void registerNmea() {
    logRegistration("Nmea", mLocationManager.addNmeaListener(nmeaListener));
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
    for (GnssListener logger : mLoggers) {
      if (logger instanceof AgnssUiLogger && !firstTime) {
        continue;
      }
      logger.onListenerRegistration(listener, result);
    }
  }
}
