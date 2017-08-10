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
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

/** The UI fragment that hosts a logging view. */
public class AgnssFragment extends Fragment {

  public static final String TAG = ":AgnssFragment";
  private TextView mLogView;
  private ScrollView mScrollView;
  private GnssContainer mGpsContainer;
  private AgnssUiLogger mUiLogger;

  private final AgnssUIFragmentComponent mUiComponent = new AgnssUIFragmentComponent();

  public void setGpsContainer(GnssContainer value) {
    mGpsContainer = value;
  }

  public void setUILogger(AgnssUiLogger value) {
    mUiLogger = value;
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View newView = inflater.inflate(R.layout.fragment_agnss, container, false /* attachToRoot */);
    mLogView = (TextView) newView.findViewById(R.id.log_view);
    mScrollView = (ScrollView) newView.findViewById(R.id.log_scroll);

    if (mUiLogger != null) {
      mUiLogger.setUiFragmentComponent(mUiComponent);
    }

    Button clearAgps = (Button) newView.findViewById(R.id.clearAgps);
    clearAgps.setOnClickListener(
        new OnClickListener() {
          @Override
          public void onClick(View view) {
            Log.i(GnssContainer.TAG + TAG, "Clearing AGPS");
            LocationManager locationManager = mGpsContainer.getLocationManager();
            locationManager.sendExtraCommand(
                LocationManager.GPS_PROVIDER, "delete_aiding_data", null);
            Log.i(GnssContainer.TAG + TAG, "Clearing AGPS command sent");
          }
        });

    Button fetchExtraData = (Button) newView.findViewById(R.id.fetchExtraData);
    fetchExtraData.setOnClickListener(
        new OnClickListener() {
          @Override
          public void onClick(View view) {
            Log.i(GnssContainer.TAG + TAG, "Fetching Extra data");
            LocationManager locationManager = mGpsContainer.getLocationManager();
            Bundle bundle = new Bundle();
            locationManager.sendExtraCommand("gps", "force_xtra_injection", bundle);
            Log.i(GnssContainer.TAG + TAG, "Fetching Extra data Command sent");
          }
        });

    Button fetchTimeData = (Button) newView.findViewById(R.id.fetchTimeData);
    fetchTimeData.setOnClickListener(
        new OnClickListener() {
          @Override
          public void onClick(View view) {
            Log.i(GnssContainer.TAG + TAG, "Fetching Time data");
            LocationManager locationManager = mGpsContainer.getLocationManager();
            Bundle bundle = new Bundle();
            locationManager.sendExtraCommand("gps", "force_time_injection", bundle);
            Log.i(GnssContainer.TAG + TAG, "Fetching Time data Command sent");
          }
        });

    Button requestSingleNlp = (Button) newView.findViewById(R.id.requestSingleNlp);
    requestSingleNlp.setOnClickListener(
        new OnClickListener() {
          @Override
          public void onClick(View view) {
            Log.i(GnssContainer.TAG + TAG, "Requesting Single NLP Location");
            mGpsContainer.registerSingleNetworkLocation();
            Log.i(GnssContainer.TAG + TAG, "Single NLP Location Requested");
          }
        });

    Button requestSingleGps = (Button) newView.findViewById(R.id.requestSingleGps);
    requestSingleGps.setOnClickListener(
        new OnClickListener() {
          @Override
          public void onClick(View view) {
            Log.i(GnssContainer.TAG + TAG, "Requesting Single GPS Location");
            mGpsContainer.registerSingleGpsLocation();
            Log.i(GnssContainer.TAG + TAG, "Single GPS Location Requested");
          }
        });
    Button clear = (Button) newView.findViewById(R.id.clear_log);
    clear.setOnClickListener(
        new OnClickListener() {
          @Override
          public void onClick(View view) {
            mLogView.setText("");
          }
        });

    return newView;
  }
  /** A facade for Agnss UI related operations. */
  public class AgnssUIFragmentComponent {

    private static final int MAX_LENGTH = 12000;
    private static final int LOWER_THRESHOLD = (int) (MAX_LENGTH * 0.5);

    public synchronized void logTextFragment(final String tag, final String text, int color) {
      final SpannableStringBuilder builder = new SpannableStringBuilder();
      builder.append(tag).append(" | ").append(text).append("\n");
      builder.setSpan(
          new ForegroundColorSpan(color),
          0 /* start */,
          builder.length(),
          SpannableStringBuilder.SPAN_INCLUSIVE_EXCLUSIVE);

      Activity activity = getActivity();
      if (activity == null) {
        return;
      }
      activity.runOnUiThread(
          new Runnable() {
            @Override
            public void run() {
              mLogView.append(builder);
              Editable editable = mLogView.getEditableText();
              int length = editable.length();
              if (length > MAX_LENGTH) {
                editable.delete(0, length - LOWER_THRESHOLD);
              }
            }
          });
    }

    public void startActivity(Intent intent) {
      getActivity().startActivity(intent);
    }
  }
}
