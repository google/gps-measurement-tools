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

import android.graphics.Color;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import com.google.android.apps.location.gps.gnsslogger.AgnssFragment.AgnssUIFragmentComponent;
import java.util.concurrent.TimeUnit;

/**
 * A class representing a UI logger for the application. Its responsibility is show information in
 * the UI.
 */
public class AgnssUiLogger implements GnssListener {

  private static final int USED_COLOR = Color.rgb(0x4a, 0x5f, 0x70);

  public AgnssUiLogger() {}

  private AgnssUIFragmentComponent mUiFragmentComponent;

  public synchronized AgnssUIFragmentComponent getUiFragmentComponent() {
    return mUiFragmentComponent;
  }

  public synchronized void setUiFragmentComponent(AgnssUIFragmentComponent value) {
    mUiFragmentComponent = value;
  }

  @Override
  public void onProviderEnabled(String provider) {
    logLocationEvent("onProviderEnabled: " + provider);
  }

  @Override
  public void onTTFFReceived(long l) {
    logLocationEvent("timeToFirstFix: " + TimeUnit.NANOSECONDS.toMillis(l) + "millis");
  }

  @Override
  public void onProviderDisabled(String provider) {
    logLocationEvent("onProviderDisabled: " + provider);
  }

  @Override
  public void onLocationChanged(Location location) {
    logLocationEvent("onLocationChanged: " + location);
  }

  @Override
  public void onLocationStatusChanged(String provider, int status, Bundle extras) {}

  @Override
  public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {}

  @Override
  public void onGnssMeasurementsStatusChanged(int status) {}

  @Override
  public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {}

  @Override
  public void onGnssNavigationMessageStatusChanged(int status) {}

  @Override
  public void onGnssStatusChanged(GnssStatus gnssStatus) {}

  @Override
  public void onNmeaReceived(long timestamp, String s) {}

  @Override
  public void onListenerRegistration(String listener, boolean result) {
    logEvent("Registration", String.format("add%sListener: %b", listener, result), USED_COLOR);
  }

  private void logEvent(String tag, String message, int color) {
    String composedTag = GnssContainer.TAG + tag;
    Log.d(composedTag, message);
    logText(tag, message, color);
  }

  private void logText(String tag, String text, int color) {
    AgnssUIFragmentComponent component = getUiFragmentComponent();
    if (component != null) {
      component.logTextFragment(tag, text, color);
    }
  }

  private void logLocationEvent(String event) {
    logEvent("Location", event, USED_COLOR);
  }
}
