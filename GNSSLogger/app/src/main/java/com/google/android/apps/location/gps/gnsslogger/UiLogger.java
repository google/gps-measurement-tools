/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import com.google.android.apps.location.gps.gnsslogger.LoggerFragment.UIFragmentComponent;
import java.util.concurrent.TimeUnit;

/**
 * A class representing a UI logger for the application. Its responsibility is show information in
 * the UI.
 */
public class UiLogger implements GnssListener {

    private static final long EARTH_RADIUS_METERS = 6371000;
    private static final int USED_COLOR = Color.rgb(0x4a, 0x5f, 0x70);

    public UiLogger() {}

    private UIFragmentComponent mUiFragmentComponent;

    public synchronized UIFragmentComponent getUiFragmentComponent() {
        return mUiFragmentComponent;
    }

    public synchronized void setUiFragmentComponent(UIFragmentComponent value) {
        mUiFragmentComponent = value;
    }

    @Override
    public void onProviderEnabled(String provider) {
        logLocationEvent("onProviderEnabled: " + provider);
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
    public void onLocationStatusChanged(String provider, int status, Bundle extras) {
        String message =
                String.format(
                        "onStatusChanged: provider=%s, status=%s, extras=%s",
                        provider, locationStatusToString(status), extras);
        logLocationEvent(message);
    }

    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
        logMeasurementEvent("onGnsssMeasurementsReceived: " + event);
    }

    @Override
    public void onGnssMeasurementsStatusChanged(int status) {
        logMeasurementEvent("onStatusChanged: " + gnssMeasurementsStatusToString(status));
    }

    @Override
    public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
        logNavigationMessageEvent("onGnssNavigationMessageReceived: " + event);
    }

    @Override
    public void onGnssNavigationMessageStatusChanged(int status) {
        logNavigationMessageEvent("onStatusChanged: " + getGnssNavigationMessageStatus(status));
    }

    @Override
    public void onGnssStatusChanged(GnssStatus gnssStatus) {
        logStatusEvent("onGnssStatusChanged: " + gnssStatusToString(gnssStatus));
    }

    @Override
    public void onNmeaReceived(long timestamp, String s) {
        logNmeaEvent(String.format("onNmeaReceived: timestamp=%d, %s", timestamp, s));
    }

    @Override
    public void onListenerRegistration(String listener, boolean result) {
        logEvent("Registration", String.format("add%sListener: %b", listener, result), USED_COLOR);
    }

    private void logMeasurementEvent(String event) {
        logEvent("Measurement", event, USED_COLOR);
    }

    private void logNavigationMessageEvent(String event) {
        logEvent("NavigationMsg", event, USED_COLOR);
    }

    private void logStatusEvent(String event) {
        logEvent("Status", event, USED_COLOR);
    }

    private void logNmeaEvent(String event) {
        logEvent("Nmea", event, USED_COLOR);
    }

    private void logEvent(String tag, String message, int color) {
        String composedTag = GnssContainer.TAG + tag;
        Log.d(composedTag, message);
        logText(tag, message, color);
    }

    private void logText(String tag, String text, int color) {
        UIFragmentComponent component = getUiFragmentComponent();
        if (component != null) {
            component.logTextFragment(tag, text, color);
        }
    }

    private String locationStatusToString(int status) {
        switch (status) {
            case LocationProvider.AVAILABLE:
                return "AVAILABLE";
            case LocationProvider.OUT_OF_SERVICE:
                return "OUT_OF_SERVICE";
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                return "TEMPORARILY_UNAVAILABLE";
            default:
                return "<Unknown>";
        }
    }

    private String gnssMeasurementsStatusToString(int status) {
        switch (status) {
            case GnssMeasurementsEvent.Callback.STATUS_NOT_SUPPORTED:
                return "NOT_SUPPORTED";
            case GnssMeasurementsEvent.Callback.STATUS_READY:
                return "READY";
            case GnssMeasurementsEvent.Callback.STATUS_LOCATION_DISABLED:
                return "GNSS_LOCATION_DISABLED";
            default:
                return "<Unknown>";
        }
    }

    private String getGnssNavigationMessageStatus(int status) {
        switch (status) {
            case GnssNavigationMessage.STATUS_UNKNOWN:
                return "Status Unknown";
            case GnssNavigationMessage.STATUS_PARITY_PASSED:
                return "READY";
            case GnssNavigationMessage.STATUS_PARITY_REBUILT:
                return "Status Parity Rebuilt";
            default:
                return "<Unknown>";
        }
    }

    private String gnssStatusToString(GnssStatus gnssStatus) {

        StringBuilder builder = new StringBuilder("SATELLITE_STATUS | [Satellites:\n");
        for (int i = 0; i < gnssStatus.getSatelliteCount(); i++) {
            builder
                    .append("Constellation = ")
                    .append(getConstellationName(gnssStatus.getConstellationType(i)))
                    .append(", ");
            builder.append("Svid = ").append(gnssStatus.getSvid(i)).append(", ");
            builder.append("Cn0DbHz = ").append(gnssStatus.getCn0DbHz(i)).append(", ");
            builder.append("Elevation = ").append(gnssStatus.getElevationDegrees(i)).append(", ");
            builder.append("Azimuth = ").append(gnssStatus.getAzimuthDegrees(i)).append(", ");
            builder.append("hasEphemeris = ").append(gnssStatus.hasEphemerisData(i)).append(", ");
            builder.append("hasAlmanac = ").append(gnssStatus.hasAlmanacData(i)).append(", ");
            builder.append("usedInFix = ").append(gnssStatus.usedInFix(i)).append("\n");
        }
        builder.append("]");
        return builder.toString();
    }

    private void logLocationEvent(String event) {
        logEvent("Location", event, USED_COLOR);
    }

    private String getConstellationName(int id) {
        switch (id) {
            case 1:
                return "GPS";
            case 2:
                return "SBAS";
            case 3:
                return "GLONASS";
            case 4:
                return "QZSS";
            case 5:
                return "BEIDOU";
            case 6:
                return "GALILEO";
            default:
                return "UNKNOWN";
        }
    }
}
