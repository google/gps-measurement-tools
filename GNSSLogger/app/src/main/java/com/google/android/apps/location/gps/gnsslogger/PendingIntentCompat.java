/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_MUTABLE;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/** Helper for accessing features in {@link PendingIntent}. */
public class PendingIntentCompat {

  /**
   * Retrieves a {@link PendingIntent} with mandatory mutability flag set on supported platform
   * versions. The caller provides the flag as combination of all the other values except mutability
   * flag. This method combines mutability flag when necessary. See {@link
   * PendingIntent#getActivities(Context, int, Intent[], int, Bundle)}.
   */
  public static @NonNull PendingIntent getActivities(
      @NonNull Context context,
      int requestCode,
      @NonNull @SuppressLint("ArrayReturn") Intent[] intents,
      boolean isMutable,
      int flags,
      @NonNull Bundle options) {
    return PendingIntent.getActivities(
        context, requestCode, intents, addMutabilityFlags(isMutable, flags), options);
  }

  /**
   * Retrieves a {@link PendingIntent} with mandatory mutability flag set on supported platform
   * versions. The caller provides the flag as combination of all the other values except mutability
   * flag. This method combines mutability flag when necessary. See {@link
   * PendingIntent#getActivities(Context, int, Intent[], int, Bundle)}.
   */
  public static @NonNull PendingIntent getActivities(
      @NonNull Context context,
      int requestCode,
      @NonNull @SuppressLint("ArrayReturn") Intent[] intents,
      boolean isMutable,
      int flags) {
    return PendingIntent.getActivities(
        context, requestCode, intents, addMutabilityFlags(isMutable, flags));
  }

  /**
   * Retrieves a {@link PendingIntent} with mandatory mutability flag set on supported platform
   * versions. The caller provides the flag as combination of all the other values except mutability
   * flag. This method combines mutability flag when necessary. See {@link
   * PendingIntent#getActivity(Context, int, Intent, int)}.
   */
  public static @NonNull PendingIntent getActivity(
      @NonNull Context context,
      int requestCode,
      @NonNull Intent intent,
      boolean isMutable,
      int flags) {
    return PendingIntent.getActivity(
        context, requestCode, intent, addMutabilityFlags(isMutable, flags));
  }

  /**
   * Retrieves a {@link PendingIntent} with mandatory mutability flag set on supported platform
   * versions. The caller provides the flag as combination of all the other values except mutability
   * flag. This method combines mutability flag when necessary. See {@link
   * PendingIntent#getActivity(Context, int, Intent, int, Bundle)}.
   */
  public static @NonNull PendingIntent getActivity(
      @NonNull Context context,
      int requestCode,
      @NonNull Intent intent,
      boolean isMutable,
      int flags,
      @NonNull Bundle options) {
    return PendingIntent.getActivity(
        context, requestCode, intent, addMutabilityFlags(isMutable, flags), options);
  }

  /**
   * Retrieves a {@link PendingIntent} with mandatory mutability flag set on supported platform
   * versions. The caller provides the flag as combination of all the other values except mutability
   * flag. This method combines mutability flag when necessary. See {@link
   * PendingIntent#getBroadcast(Context, int, Intent, int)}.
   */
  public static @NonNull PendingIntent getBroadcast(
      @NonNull Context context,
      int requestCode,
      @NonNull Intent intent,
      boolean isMutable,
      int flags) {
    return PendingIntent.getBroadcast(
        context, requestCode, intent, addMutabilityFlags(isMutable, flags));
  }

  /**
   * Retrieves a {@link PendingIntent} with mandatory mutability flag set on supported platform
   * versions. The caller provides the flag as combination of all the other values except mutability
   * flag. This method combines mutability flag when necessary. See {@link
   * PendingIntent#getForegroundService(Context, int, Intent, int)} .
   */
  @RequiresApi(api = Build.VERSION_CODES.O)
  public static @NonNull PendingIntent getForegroundService(
      @NonNull Context context,
      int requestCode,
      @NonNull Intent intent,
      boolean isMutable,
      int flags) {
    return PendingIntent.getForegroundService(
        context, requestCode, intent, addMutabilityFlags(isMutable, flags));
  }

  /**
   * Retrieves a {@link PendingIntent} with mandatory mutability flag set on supported platform
   * versions. The caller provides the flag as combination of all the other values except mutability
   * flag. This method combines mutability flag when necessary. See {@link
   * PendingIntent#getService(Context, int, Intent, int)}.
   */
  public static @NonNull PendingIntent getService(
      @NonNull Context context,
      int requestCode,
      @NonNull Intent intent,
      boolean isMutable,
      int flags) {
    return PendingIntent.getService(
        context, requestCode, intent, addMutabilityFlags(isMutable, flags));
  }

  private static int addMutabilityFlags(boolean isMutable, int flags) {
    if (isMutable) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        flags |= FLAG_MUTABLE;
      }
    } else {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        flags |= FLAG_IMMUTABLE;
      }
    }

    return flags;
  }

  private PendingIntentCompat() {}
}
