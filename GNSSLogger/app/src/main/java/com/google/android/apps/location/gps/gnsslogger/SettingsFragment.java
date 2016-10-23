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

import android.app.Fragment;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.apps.location.gps.gnsslogger.GnssContainer;
import java.lang.reflect.InvocationTargetException;
import android.widget.Button;

/**
 * The UI fragment showing a set of configurable settings for the client to request GPS data.
 */
public class SettingsFragment extends Fragment {

    public static final String TAG = ":SettingsFragment";
    private GnssContainer mGpsContainer;
    private HelpDialog helpDialog;

    public void setGpsContainer(GnssContainer value) {
        mGpsContainer = value;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false /* attachToRoot */);

        final Switch registerLocation = (Switch) view.findViewById(R.id.register_location);
        final TextView registerLocationLabel =
                (TextView) view.findViewById(R.id.register_location_label);
        //set the switch to OFF
        registerLocation.setChecked(false);
        registerLocationLabel.setText("Switch is OFF");
        registerLocation.setOnCheckedChangeListener(
                new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                        if (isChecked) {
                            mGpsContainer.registerLocation();
                            registerLocationLabel.setText("Switch is ON");
                        } else {
                            mGpsContainer.unregisterLocation();
                            registerLocationLabel.setText("Switch is OFF");
                        }
                    }
                });

        final Switch registerMeasurements = (Switch) view.findViewById(R.id.register_measurements);
        final TextView registerMeasurementsLabel =
                (TextView) view.findViewById(R.id.register_measurement_label);
        //set the switch to OFF
        registerMeasurements.setChecked(false);
        registerMeasurementsLabel.setText("Switch is OFF");
        registerMeasurements.setOnCheckedChangeListener(
                new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                        if (isChecked) {
                            mGpsContainer.registerMeasurements();
                            registerMeasurementsLabel.setText("Switch is ON");
                        } else {
                            mGpsContainer.unregisterMeasurements();
                            registerMeasurementsLabel.setText("Switch is OFF");
                        }
                    }
                });

        final Switch registerNavigation = (Switch) view.findViewById(R.id.register_navigation);
        final TextView registerNavigationLabel =
                (TextView) view.findViewById(R.id.register_navigation_label);
        //set the switch to OFF
        registerNavigation.setChecked(false);
        registerNavigationLabel.setText("Switch is OFF");
        registerNavigation.setOnCheckedChangeListener(
                new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                        if (isChecked) {
                            mGpsContainer.registerNavigation();
                            registerNavigationLabel.setText("Switch is ON");
                        } else {
                            mGpsContainer.unregisterNavigation();
                            registerNavigationLabel.setText("Switch is OFF");
                        }
                    }
                });

        final Switch registerGpsStatus = (Switch) view.findViewById(R.id.register_status);
        final TextView registerGpsStatusLabel =
                (TextView) view.findViewById(R.id.register_status_label);
        //set the switch to OFF
        registerGpsStatus.setChecked(false);
        registerGpsStatusLabel.setText("Switch is OFF");
        registerGpsStatus.setOnCheckedChangeListener(
                new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                        if (isChecked) {
                            mGpsContainer.registerGnssStatus();
                            registerGpsStatusLabel.setText("Switch is ON");
                        } else {
                            mGpsContainer.unregisterGpsStatus();
                            registerGpsStatusLabel.setText("Switch is OFF");
                        }
                    }
                });

        final Switch registerNmea = (Switch) view.findViewById(R.id.register_nmea);
        final TextView registerNmeaLabel = (TextView) view.findViewById(R.id.register_nmea_label);
        //set the switch to OFF
        registerNmea.setChecked(false);
        registerNmeaLabel.setText("Switch is OFF");
        registerNmea.setOnCheckedChangeListener(
                new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                        if (isChecked) {
                            mGpsContainer.registerNmea();
                            registerNmeaLabel.setText("Switch is ON");
                        } else {
                            mGpsContainer.unregisterNmea();
                            registerNmeaLabel.setText("Switch is OFF");
                        }
                    }
                });

        Button help = (Button) view.findViewById(R.id.help);
        helpDialog = new HelpDialog(getContext());
        helpDialog.setTitle("Help contents");
        helpDialog.create();

        help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                helpDialog.show();
            }
        });

        Button exit = (Button) view.findViewById(R.id.exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finishAffinity();
            }
        });

        TextView swInfo = (TextView) view.findViewById(R.id.sw_info);

        java.lang.reflect.Method method;
        LocationManager locationManager = mGpsContainer.getLocationManager();
        try {
            method = locationManager.getClass().getMethod("getGnssYearOfHardware");
            int hwYear = (int) method.invoke(locationManager);
            if (hwYear == 0) {
                swInfo.append("HW Year: " + "2015 or older \n");
            } else {
                swInfo.append("HW Year: " + hwYear + "\n");
            }

        } catch (NoSuchMethodException e) {
            logException("No such method exception: ", e);
            return null;
        } catch (IllegalAccessException e) {
            logException("Illegal Access exception: ", e);
            return null;
        } catch (InvocationTargetException e) {
            logException("Invocation Target Exception: ", e);
            return null;
        }

        String platfromVersionString = Build.VERSION.RELEASE;
        swInfo.append("Platform: " + platfromVersionString + "\n");
        int apiLivelInt = Build.VERSION.SDK_INT;
        swInfo.append("Api Level: " + apiLivelInt);

        return view;
    }

    private void logException(String errorMessage, Exception e) {
        Log.e(GnssContainer.TAG + TAG, errorMessage, e);
        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
    }
}
