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
package com.google.android.apps.location.gps.gnsslogger.util

import android.content.Intent
import android.app.PendingIntent
import android.content.Context
import com.google.android.apps.location.gps.gnsslogger.util.PendingIntentCompat
import android.os.Build

/** Helper for accessing features in [PendingIntent].  */
object PendingIntentCompat {
    /**
     * Retrieves a [PendingIntent] with mandatory mutability flag set on supported platform
     * versions. The caller provides the flag as combination of all the other values except mutability
     * flag. This method combines mutability flag when necessary. See [ ][PendingIntent.getBroadcast].
     */
    @JvmStatic
    fun getBroadcast(
            context: Context,
            requestCode: Int,
            intent: Intent,
            flags: Int,
            isMutable: Boolean): PendingIntent {
        return PendingIntent.getBroadcast(
                context, requestCode, intent, addMutabilityFlags(isMutable, flags))
    }

    private fun addMutabilityFlags(isMutable: Boolean, flags: Int): Int {
        var flags = flags
        if (isMutable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags = flags or PendingIntent.FLAG_MUTABLE
            }
        } else {
            // Minimum support SDK is 24.
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return flags
    }
}