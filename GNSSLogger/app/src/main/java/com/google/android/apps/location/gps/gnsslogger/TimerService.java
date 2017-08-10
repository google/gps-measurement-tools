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

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import java.util.concurrent.TimeUnit;

/** A {@link Service} to be bound to that exposes a timer. */
public class TimerService extends Service {
  static final String TIMER_ACTION =
      String.format("%s.TIMER_UPDATE", TimerService.class.getPackage().getName());
  static final String EXTRA_KEY_TYPE = "type";
  static final String EXTRA_KEY_UPDATE_REMAINING = "remaining";
  static final byte TYPE_UNKNOWN = -1;
  static final byte TYPE_UPDATE = 0;
  static final byte TYPE_FINISH = 1;
  static final int NOTIFICATION_ID = 7777;

  private final IBinder mBinder = new TimerBinder();
  private CountDownTimer mCountDownTimer;
  private boolean mTimerStarted;

  /** Handles response from {@link TimerFragment} */
  public interface TimerListener {
    /**
     * Process a {@link TimerValues} result
     *
     * @param values The set {@link TimerValues}
     */
    public void processTimerValues(TimerValues values);
  }

  /** A {@link Binder} that exposes a {@link TimerService}. */
  public class TimerBinder extends Binder {
    TimerService getService() {
      return TimerService.this;
    }
  }

  @Override
  public void onCreate() {
    mTimerStarted = false;
  }

  @Override
  public IBinder onBind(Intent intent) {
    Notification notification = new Notification();
    startForeground(NOTIFICATION_ID, notification);
    return mBinder;
  }

  @Override
  public void onDestroy() {
    if (mCountDownTimer != null) {
      mCountDownTimer.cancel();
    }
    mTimerStarted = false;
  }

  void setTimer(TimerValues values) {
    // Only allow setting when not already running
    if (!mTimerStarted) {
      mCountDownTimer =
          new CountDownTimer(
              values.getTotalMilliseconds(),
              TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS) /* countDownInterval */) {
            @Override
            public void onTick(long millisUntilFinished) {
              Intent broadcast = new Intent(TIMER_ACTION);
              broadcast.putExtra(EXTRA_KEY_TYPE, TYPE_UPDATE);
              broadcast.putExtra(EXTRA_KEY_UPDATE_REMAINING, millisUntilFinished);
              LocalBroadcastManager.getInstance(TimerService.this).sendBroadcast(broadcast);
            }

            @Override
            public void onFinish() {
              mTimerStarted = false;
              Intent broadcast = new Intent(TIMER_ACTION);
              broadcast.putExtra(EXTRA_KEY_TYPE, TYPE_FINISH);
              LocalBroadcastManager.getInstance(TimerService.this).sendBroadcast(broadcast);
            }
          };
    }
  }

  void startTimer() {
    if ((mCountDownTimer != null) && !mTimerStarted) {
      mCountDownTimer.start();
      mTimerStarted = true;
    }
  }

  void stopTimer() {
    if (mCountDownTimer != null) {
      mCountDownTimer.cancel();
      mTimerStarted = false;
    }
  }
}
