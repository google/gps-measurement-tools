package com.google.android.apps.location.gps.gnsslogger;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPendingIntent;

/** Unit test for {@link PendingIntentCompat}. */
@RunWith(RobolectricTestRunner.class)
public class PendingIntentCompatTest {
  private final Context context = ApplicationProvider.getApplicationContext();

  /** Immutable is introduced in M, but the project's minimum support version is 24 */
  @Config(sdk = Build.VERSION_CODES.N)
  @Test
  public void addMutabilityFlags_immutableOnMPlus() {
    int requestCode = 7465;
    Intent intent = new Intent();
    Bundle options = new Bundle();
    ShadowPendingIntent shadow =
        shadowOf(
            PendingIntentCompat.getBroadcast(
                context,
                requestCode,
                intent,
                /* isMutable= */ false,
                PendingIntent.FLAG_UPDATE_CURRENT));
    assertThat(shadow.getFlags())
        .isEqualTo(PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  @Config(sdk = Build.VERSION_CODES.R)
  @Test
  public void addMutabilityFlags_mutableOnPreS() {
    int requestCode = 7465;
    Intent intent = new Intent();
    ShadowPendingIntent shadow =
        shadowOf(
            PendingIntentCompat.getBroadcast(
                context,
                requestCode,
                intent,
                /* isMutable= */ true,
                PendingIntent.FLAG_UPDATE_CURRENT));
    assertThat(shadow.getFlags()).isEqualTo(PendingIntent.FLAG_UPDATE_CURRENT);
  }

  @Config(sdk = Build.VERSION_CODES.S)
  @Test
  public void addMutabilityFlags_mutableOnSPlus() {
    int requestCode = 7465;
    Intent intent = new Intent();
    ShadowPendingIntent shadow =
        shadowOf(
            PendingIntentCompat.getBroadcast(
                context,
                requestCode,
                intent,
                /* isMutable= */ true,
                PendingIntent.FLAG_UPDATE_CURRENT));
    assertThat(shadow.getFlags())
        .isEqualTo(PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
  }

  @Test
  public void getBroadcast() {
    int requestCode = 7465;
    Intent intent = new Intent();
    Bundle options = new Bundle();
    ShadowPendingIntent shadow =
        shadowOf(
            PendingIntentCompat.getBroadcast(
                context,
                requestCode,
                intent,
                /* isMutable= */ false,
                PendingIntent.FLAG_UPDATE_CURRENT));
    assertThat(shadow.isBroadcast()).isTrue();
  }
}
