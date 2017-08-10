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

import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.OnNmeaMessageListener;
import android.os.Bundle;

/**  A class representing an interface for logging GPS information. */
public interface GnssListener {

  /** @see LocationListener#onProviderEnabled(String) */
  void onProviderEnabled(String provider);
  /** @see LocationListener#onProviderDisabled(String) */
  void onProviderDisabled(String provider);
  /** @see LocationListener#onLocationChanged(Location) */
  void onLocationChanged(Location location);
  /** @see LocationListener#onStatusChanged(String, int, Bundle) */
  void onLocationStatusChanged(String provider, int status, Bundle extras);
  /**
   * @see GnssMeasurementsEvent.Callback#
   *     onGnssMeasurementsReceived(GnssMeasurementsEvent)
   */
  void onGnssMeasurementsReceived(GnssMeasurementsEvent event);
  /** @see GnssMeasurementsEvent.Callback#onStatusChanged(int) */
  void onGnssMeasurementsStatusChanged(int status);
  /** @see GnssNavigationMessage.Callback# onGnssNavigationMessageReceived(GnssNavigationMessage) */
  void onGnssNavigationMessageReceived(GnssNavigationMessage event);
  /** @see GnssNavigationMessage.Callback#onStatusChanged(int) */
  void onGnssNavigationMessageStatusChanged(int status);
  /** @see GnssStatus.Callback#onSatelliteStatusChanged(GnssStatus) */
  void onGnssStatusChanged(GnssStatus gnssStatus);
  /** Called when the listener is registered to listen to GNSS events */
  void onListenerRegistration(String listener, boolean result);
  /** @see OnNmeaMessageListener#onNmeaMessage(String, long) */
  void onNmeaReceived(long l, String s);
  void onTTFFReceived(long l);
}
