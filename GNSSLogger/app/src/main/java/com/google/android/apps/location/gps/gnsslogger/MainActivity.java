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

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TabLayout.TabLayoutOnPageChangeListener;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import java.util.Locale;

/** The activity for the application. */
public class MainActivity extends AppCompatActivity
    implements OnConnectionFailedListener, ConnectionCallbacks, GroundTruthModeSwitcher {
  private static final int LOCATION_REQUEST_ID = 1;
  private static final String[] REQUIRED_PERMISSIONS = {
    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE
  };
  private static final int NUMBER_OF_FRAGMENTS = 6;
  private static final int FRAGMENT_INDEX_SETTING = 0;
  private static final int FRAGMENT_INDEX_LOGGER = 1;
  private static final int FRAGMENT_INDEX_RESULT = 2;
  private static final int FRAGMENT_INDEX_MAP = 3;
  private static final int FRAGMENT_INDEX_AGNSS = 4;
  private static final int FRAGMENT_INDEX_PLOT = 5;
  private static final String TAG = "MainActivity";

  private GnssContainer mGnssContainer;
  private UiLogger mUiLogger;
  private RealTimePositionVelocityCalculator mRealTimePositionVelocityCalculator;
  private FileLogger mFileLogger;
  private AgnssUiLogger mAgnssUiLogger;
  private Fragment[] mFragments;
  private GoogleApiClient mGoogleApiClient;
  private boolean mAutoSwitchGroundTruthMode;
  private final ActivityDetectionBroadcastReceiver mBroadcastReceiver =
      new ActivityDetectionBroadcastReceiver();

  private ServiceConnection mConnection =
      new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
          // Empty
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
          // Empty
        }
      };

  @Override
  protected void onStart() {
    super.onStart();
    // Bind to the timer service to ensure it is available when app is running
    bindService(new Intent(this, TimerService.class), mConnection, Context.BIND_AUTO_CREATE);
  }

  @Override
  protected void onResume() {
    super.onResume();
    LocalBroadcastManager.getInstance(this)
        .registerReceiver(
            mBroadcastReceiver, new IntentFilter(
                DetectedActivitiesIntentReceiver.AR_RESULT_BROADCAST_ACTION));
  }

  @Override
  protected void onPause() {
    LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    super.onPause();
  }

  @Override
  protected void onStop() {
    super.onStop();
    unbindService(mConnection);
  }

  @Override
  protected void onDestroy(){
    mGnssContainer.unregisterAll();
    super.onDestroy();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    SharedPreferences sharedPreferences = PreferenceManager.
        getDefaultSharedPreferences(this);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putBoolean(SettingsFragment.PREFERENCE_KEY_AUTO_SCROLL, false);
    editor.commit();
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    buildGoogleApiClient();
    requestPermissionAndSetupFragments(this);
  }

  protected PendingIntent createActivityDetectionPendingIntent() {
    Intent intent = new Intent(this, DetectedActivitiesIntentReceiver.class);
    return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  private synchronized void buildGoogleApiClient() {
    mGoogleApiClient =
        new GoogleApiClient.Builder(this)
            .enableAutoManage(this, this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(ActivityRecognition.API)
            .build();
  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult result) {
    if (Log.isLoggable(TAG, Log.INFO)){
      Log.i(TAG,  "Connection failed: ErrorCode = " + result.getErrorCode());
    }
  }

  @Override
  public void onConnected(Bundle connectionHint) {
    if (Log.isLoggable(TAG, Log.INFO)) {
      Log.i(TAG, "Connected to GoogleApiClient");
    }
    ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
        mGoogleApiClient, 0, createActivityDetectionPendingIntent());
  }

  @Override
  public void onConnectionSuspended(int cause) {
    if (Log.isLoggable(TAG, Log.INFO)) {
      Log.i(TAG, "Connection suspended");
    }
  }

  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the
   * sections/tabs/pages.
   */
  public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    public ViewPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
      switch (position) {
        case FRAGMENT_INDEX_SETTING:
          return mFragments[FRAGMENT_INDEX_SETTING];
        case FRAGMENT_INDEX_LOGGER:
          return mFragments[FRAGMENT_INDEX_LOGGER];
        case FRAGMENT_INDEX_RESULT:
          return mFragments[FRAGMENT_INDEX_RESULT];
        case FRAGMENT_INDEX_MAP:
          return mFragments[FRAGMENT_INDEX_MAP];
        case FRAGMENT_INDEX_AGNSS:
          return mFragments[FRAGMENT_INDEX_AGNSS];
        case FRAGMENT_INDEX_PLOT:
          return mFragments[FRAGMENT_INDEX_PLOT];
        default:
          throw new IllegalArgumentException("Invalid section: " + position);
      }
    }

    @Override
    public int getCount() {
      // Show total pages.
      return NUMBER_OF_FRAGMENTS;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      Locale locale = Locale.getDefault();
      switch (position) {
        case FRAGMENT_INDEX_SETTING:
          return getString(R.string.title_settings).toUpperCase(locale);
        case FRAGMENT_INDEX_LOGGER:
          return getString(R.string.title_log).toUpperCase(locale);
        case FRAGMENT_INDEX_RESULT:
          return getString(R.string.title_offset).toUpperCase(locale);
        case FRAGMENT_INDEX_MAP:
          return getString(R.string.title_map).toUpperCase(locale);
        case FRAGMENT_INDEX_AGNSS:
          return getString(R.string.title_agnss).toUpperCase(locale);
        case FRAGMENT_INDEX_PLOT:
          return getString(R.string.title_plot).toLowerCase(locale);
        default:
          return super.getPageTitle(position);
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode == LOCATION_REQUEST_ID) {
      // If request is cancelled, the result arrays are empty.
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        setupFragments();
      }
    }
  }

  private void setupFragments() {
    mUiLogger = new UiLogger();
    mRealTimePositionVelocityCalculator = new RealTimePositionVelocityCalculator();
    mRealTimePositionVelocityCalculator.setMainActivity(this);
    mRealTimePositionVelocityCalculator.setResidualPlotMode(
        RealTimePositionVelocityCalculator.RESIDUAL_MODE_DISABLED, null /* fixedGroundTruth */);

    mFileLogger = new FileLogger(getApplicationContext());
    mAgnssUiLogger = new AgnssUiLogger();
    mGnssContainer =
        new GnssContainer(
            getApplicationContext(),
            mUiLogger,
            mFileLogger,
            mRealTimePositionVelocityCalculator,
            mAgnssUiLogger);
    mFragments = new Fragment[NUMBER_OF_FRAGMENTS];
    SettingsFragment settingsFragment = new SettingsFragment();
    settingsFragment.setGpsContainer(mGnssContainer);
    settingsFragment.setRealTimePositionVelocityCalculator(mRealTimePositionVelocityCalculator);
    settingsFragment.setAutoModeSwitcher(this);
    mFragments[FRAGMENT_INDEX_SETTING] = settingsFragment;

    LoggerFragment loggerFragment = new LoggerFragment();
    loggerFragment.setUILogger(mUiLogger);
    loggerFragment.setFileLogger(mFileLogger);
    mFragments[FRAGMENT_INDEX_LOGGER] = loggerFragment;

    ResultFragment resultFragment = new ResultFragment();
    resultFragment.setPositionVelocityCalculator(mRealTimePositionVelocityCalculator);
    mFragments[FRAGMENT_INDEX_RESULT] = resultFragment;

    MapFragment mapFragment = new MapFragment();
    mapFragment.setPositionVelocityCalculator(mRealTimePositionVelocityCalculator);
    mFragments[FRAGMENT_INDEX_MAP] = mapFragment;

    AgnssFragment agnssFragment = new AgnssFragment();
    agnssFragment.setGpsContainer(mGnssContainer);
    agnssFragment.setUILogger(mAgnssUiLogger);
    mFragments[FRAGMENT_INDEX_AGNSS] = agnssFragment;

    PlotFragment plotFragment = new PlotFragment();
    mFragments[FRAGMENT_INDEX_PLOT] = plotFragment;
    mRealTimePositionVelocityCalculator.setPlotFragment(plotFragment);


    // The viewpager that will host the section contents.
    ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
    viewPager.setOffscreenPageLimit(5);
    ViewPagerAdapter adapter = new ViewPagerAdapter(getFragmentManager());
    viewPager.setAdapter(adapter);

    TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
    tabLayout.setTabsFromPagerAdapter(adapter);

    // Set a listener via setOnTabSelectedListener(OnTabSelectedListener) to be notified when any
    // tab's selection state has been changed.
    tabLayout.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));

    // Use a TabLayout.TabLayoutOnPageChangeListener to forward the scroll and selection changes to
    // this layout
    viewPager.addOnPageChangeListener(new TabLayoutOnPageChangeListener(tabLayout));
  }

  private boolean hasPermissions(Activity activity) {
    if (Build.VERSION.SDK_INT < VERSION_CODES.M) {
      // Permissions granted at install time.
      return true;
    }
    for (String p : REQUIRED_PERMISSIONS) {
      if (ContextCompat.checkSelfPermission(activity, p) != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  private void requestPermissionAndSetupFragments(final Activity activity) {
    if (hasPermissions(activity)) {
      setupFragments();
    } else {
      ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, LOCATION_REQUEST_ID);
    }
  }

  /**
   * Toggles the flag to allow Activity Recognition updates to change ground truth mode
   */
  public void setAutoSwitchGroundTruthModeEnabled(boolean enabled) {
    mAutoSwitchGroundTruthMode = enabled;
  }

  /**
   * A receiver for result of
   * {@link ActivityRecognition#ActivityRecognitionApi#requestActivityUpdates()} broadcast by {@link
   * DetectedActivitiesIntentReceiver}
   */
  public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

      // Modify the status of mRealTimePositionVelocityCalculator only if the status is set to auto
      // (indicated by mAutoSwitchGroundTruthMode).
      if (mAutoSwitchGroundTruthMode) {
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        setGroundTruthModeOnResult(result);
      }
    }
  }

  /**
   * Sets up the ground truth mode of {@link RealTimePositionVelocityCalculator} given an result
   * from Activity Recognition update. For activities other than {@link DetectedActivity#STILL}
   * and {@link DetectedActivity#TILTING}, we conservatively assume the user is moving and use the
   * last WLS position solution as ground truth for corrected residual computation.
   */
  private void setGroundTruthModeOnResult(ActivityRecognitionResult result){
    if (result != null){
      int detectedActivityType = result.getMostProbableActivity().getType();
      if (detectedActivityType == DetectedActivity.STILL
          || detectedActivityType == DetectedActivity.TILTING){
        mRealTimePositionVelocityCalculator.setResidualPlotMode(
            RealTimePositionVelocityCalculator.RESIDUAL_MODE_STILL, null);
      } else {
        mRealTimePositionVelocityCalculator.setResidualPlotMode(
            RealTimePositionVelocityCalculator.RESIDUAL_MODE_MOVING, null);
      }
    }
  }
}
