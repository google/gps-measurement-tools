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

import static com.google.common.base.Preconditions.checkArgument;

import android.os.Bundle;
import android.widget.NumberPicker;
import java.util.concurrent.TimeUnit;

/** A representation of a time as "hours:minutes:seconds" */
public class TimerValues {
  private static final String EMPTY = "N/A";
  private static final String HOURS = "hours";
  private static final String MINUTES = "minutes";
  private static final String SECONDS = "seconds";
  private int mHours;
  private int mMinutes;
  private int mSeconds;

  /**
   * Creates a {@link TimerValues}
   *
   * @param hours The number of hours to represent
   * @param minutes The number of minutes to represent
   * @param seconds The number of seconds to represent
   */
  public TimerValues(int hours, int minutes, int seconds) {
    checkArgument(hours >= 0, "Hours is negative: %s", hours);
    checkArgument(minutes >= 0, "Minutes is negative: %s", minutes);
    checkArgument(seconds >= 0, "Seconds is negative: %s", seconds);

    mHours = hours;
    mMinutes = minutes;
    mSeconds = seconds;

    normalizeValues();
  }

  /**
   * Creates a {@link TimerValues}
   *
   * @param milliseconds The number of milliseconds to represent
   */
  public TimerValues(long milliseconds) {
    this(
        0 /* hours */,
        0 /* minutes */,
        (int) TimeUnit.SECONDS.convert(milliseconds, TimeUnit.MILLISECONDS));
  }

  /** Creates a {@link TimerValues} from a {@link Bundle} */
  public static TimerValues fromBundle(Bundle bundle) {
    checkArgument(bundle != null, "Bundle is null");

    return new TimerValues(
        bundle.getInt(HOURS, 0), bundle.getInt(MINUTES, 0), bundle.getInt(SECONDS, 0));
  }

  /** Returns a {@link Bundle} from the {@link TimerValues} */
  public Bundle toBundle() {
    Bundle content = new Bundle();
    content.putInt(HOURS, mHours);
    content.putInt(MINUTES, mMinutes);
    content.putInt(SECONDS, mSeconds);

    return content;
  }

  /**
   * Configures a {@link NumberPicker} with appropriate bounds and initial value for displaying
   * "Hours"
   */
  public void configureHours(NumberPicker picker) {
    picker.setMinValue(0);
    picker.setMaxValue((int) TimeUnit.HOURS.convert(1, TimeUnit.DAYS) - 1);
    picker.setValue(mHours);
  }

  /**
   * Configures a {@link NumberPicker} with appropriate bounds and initial value for displaying
   * "Minutes"
   */
  public void configureMinutes(NumberPicker picker) {
    picker.setMinValue(0);
    picker.setMaxValue((int) TimeUnit.MINUTES.convert(1, TimeUnit.HOURS) - 1);
    picker.setValue(mMinutes);
  }

  /**
   * Configures a {@link NumberPicker} with appropriate bounds and initial value for displaying
   * "Seconds"
   */
  public void configureSeconds(NumberPicker picker) {
    picker.setMinValue(0);
    picker.setMaxValue((int) TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES) - 1);
    picker.setValue(mSeconds);
  }

  /** Returns the {@link TimerValues} in milliseconds */
  public long getTotalMilliseconds() {
    return (TimeUnit.MILLISECONDS.convert(mHours, TimeUnit.HOURS)
        + TimeUnit.MILLISECONDS.convert(mMinutes, TimeUnit.MINUTES)
        + TimeUnit.MILLISECONDS.convert(mSeconds, TimeUnit.SECONDS));
  }

  /** Returns {@code true} if {@link TimerValues} is zero. */
  public boolean isZero() {
    return ((mHours == 0) && (mMinutes == 0) && (mSeconds == 0));
  }

  /** Returns string representation that includes "00:00:00" */
  public String toCountdownString() {
    return String.format("%02d:%02d:%02d", mHours, mMinutes, mSeconds);
  }

  /** Normalize seconds and minutes */
  private void normalizeValues() {
    long minuteOverflow = TimeUnit.MINUTES.convert(mSeconds, TimeUnit.SECONDS);
    long hourOverflow = TimeUnit.HOURS.convert(mMinutes, TimeUnit.MINUTES);

    // Apply overflow
    mMinutes += minuteOverflow;
    mHours += hourOverflow;

    // Apply bounds
    mSeconds -= TimeUnit.SECONDS.convert(minuteOverflow, TimeUnit.MINUTES);
    mMinutes -= TimeUnit.MINUTES.convert(hourOverflow, TimeUnit.HOURS);
  }

  @Override
  public String toString() {
    if (isZero()) {
      return EMPTY;
    } else {
      return toCountdownString();
    }
  }
}
