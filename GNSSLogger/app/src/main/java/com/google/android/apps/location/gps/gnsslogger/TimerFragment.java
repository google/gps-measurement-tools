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

import static com.google.common.base.Preconditions.checkState;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;
import com.google.android.apps.location.gps.gnsslogger.TimerService.TimerListener;

/** A {@link Dialog} allowing "Hours", "Minutes", and "Seconds" to be selected for a timer */
public class TimerFragment extends DialogFragment {
  private TimerListener mListener;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    checkState(
        getTargetFragment() instanceof TimerListener,
        "Target fragment is not instance of TimerListener");

    mListener = (TimerListener) getTargetFragment();
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

    View view = getActivity().getLayoutInflater().inflate(R.layout.timer, null);
    final NumberPicker timerHours = (NumberPicker) view.findViewById(R.id.hours_picker);
    final NumberPicker timerMinutes = (NumberPicker) view.findViewById(R.id.minutes_picker);
    final NumberPicker timerSeconds = (NumberPicker) view.findViewById(R.id.seconds_picker);

    final TimerValues values;

    if (getArguments() != null) {
      values = TimerValues.fromBundle(getArguments());
    } else {
      values = new TimerValues(0 /* hours */, 0 /* minutes */, 0 /* seconds */);
    }

    values.configureHours(timerHours);
    values.configureMinutes(timerMinutes);
    values.configureSeconds(timerSeconds);

    builder.setTitle(R.string.timer_title);
    builder.setView(view);
    builder.setPositiveButton(
        R.string.timer_set,
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {
            mListener.processTimerValues(
                new TimerValues(
                    timerHours.getValue(), timerMinutes.getValue(), timerSeconds.getValue()));
          }
        });
    builder.setNeutralButton(
        R.string.timer_cancel,
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {
            mListener.processTimerValues(values);
          }
        });
    builder.setNegativeButton(
        R.string.timer_reset,
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {
            mListener.processTimerValues(
                new TimerValues(0 /* hours */, 0 /* minutes */, 0 /* seconds */));
          }
        });

    return builder.create();
  }
}
