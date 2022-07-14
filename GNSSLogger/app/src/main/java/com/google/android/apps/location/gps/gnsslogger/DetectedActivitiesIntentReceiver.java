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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * A {@link BroadcastReceiver} that receives and broadcasts the result of {@link
 * com.google.android.gms.location.ActivityRecognition
 * #ActivityRecognitionApi#requestActivityUpdates()} to {@link MainActivity} to be further analyzed.
 */
public class DetectedActivitiesIntentReceiver extends BroadcastReceiver {
  public static String AR_RESULT_BROADCAST_ACTION =
      "com.google.android.apps.location.gps.gnsslogger.AR_RESULT_BROADCAST_ACTION";

  /**
   * Gets called when the result of {@link com.google.android.gms.location.ActivityRecognition
   * #ActivityRecognitionApi#requestActivityUpdates()} is available and handles incoming intents.
   *
   * @param intent The Intent is provided (inside a {@link android.app.PendingIntent}) when {@link
   *     com.google.android.gms.location.ActivityRecognition
   *     #ActivityRecognitionApi#requestActivityUpdates()} is called.
   */
  public void onReceive(Context context, Intent intent) {

    intent.setAction(AR_RESULT_BROADCAST_ACTION);
    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
  }
}
