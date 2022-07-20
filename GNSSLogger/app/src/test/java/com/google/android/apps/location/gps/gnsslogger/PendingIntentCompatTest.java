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

  @Config(sdk = Build.VERSION_CODES.LOLLIPOP_MR1)
  @Test
  public void addMutabilityFlags_immutableOnPreM() {
    int requestCode = 7465;
    Intent intent = new Intent();
    Bundle options = new Bundle();
    ShadowPendingIntent shadow =
        shadowOf(
            PendingIntentCompat.getActivity(
                context,
                requestCode,
                intent,
                /* isMutable= */ false,
                PendingIntent.FLAG_UPDATE_CURRENT,
                options));
    assertThat(shadow.isActivityIntent()).isTrue();
    assertThat(shadow.getFlags()).isEqualTo(PendingIntent.FLAG_UPDATE_CURRENT);
    assertThat(shadow.getRequestCode()).isEqualTo(requestCode);
  }

  @Config(sdk = Build.VERSION_CODES.M)
  @Test
  public void addMutabilityFlags_immutableOnMPlus() {
    int requestCode = 7465;
    Intent intent = new Intent();
    Bundle options = new Bundle();
    ShadowPendingIntent shadow =
        shadowOf(
            PendingIntentCompat.getActivity(
                context,
                requestCode,
                intent,
                /* isMutable= */ false,
                PendingIntent.FLAG_UPDATE_CURRENT,
                options));
    assertThat(shadow.isActivityIntent()).isTrue();
    assertThat(shadow.getFlags())
        .isEqualTo(PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    assertThat(shadow.getRequestCode()).isEqualTo(requestCode);
  }

  @Config(sdk = Build.VERSION_CODES.R)
  @Test
  public void addMutabilityFlags_mutableOnPreS() {
    int requestCode = 7465;
    Intent intent = new Intent();
    Bundle options = new Bundle();
    ShadowPendingIntent shadow =
        shadowOf(
            PendingIntentCompat.getActivity(
                context,
                requestCode,
                intent,
                /* isMutable= */ true,
                PendingIntent.FLAG_UPDATE_CURRENT,
                options));
    assertThat(shadow.isActivityIntent()).isTrue();
    assertThat(shadow.getFlags()).isEqualTo(PendingIntent.FLAG_UPDATE_CURRENT);
    assertThat(shadow.getRequestCode()).isEqualTo(requestCode);
  }

  @Config(sdk = Build.VERSION_CODES.S)
  @Test
  public void addMutabilityFlags_mutableOnSPlus() {
    int requestCode = 7465;
    Intent intent = new Intent();
    Bundle options = new Bundle();
    ShadowPendingIntent shadow =
        shadowOf(
            PendingIntentCompat.getActivity(
                context,
                requestCode,
                intent,
                /* isMutable= */ true,
                PendingIntent.FLAG_UPDATE_CURRENT,
                options));
    assertThat(shadow.isActivityIntent()).isTrue();
    assertThat(shadow.getFlags())
        .isEqualTo(PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    assertThat(shadow.getRequestCode()).isEqualTo(requestCode);
  }

  @Test
  public void getActivities_withBundle() {
    int requestCode = 7465;
    Intent[] intents = new Intent[] {};
    Bundle options = new Bundle();
    ShadowPendingIntent shadow =
        shadowOf(
            PendingIntentCompat.getActivities(
                context,
                requestCode,
                intents,
                /* isMutable= */ false,
                PendingIntent.FLAG_UPDATE_CURRENT,
                options));
    assertThat(shadow.isActivityIntent()).isTrue();
  }

  @Test
  public void getActivities() {
    int requestCode = 7465;
    Intent[] intents = new Intent[] {};
    Bundle options = new Bundle();
    ShadowPendingIntent shadow =
        shadowOf(
            PendingIntentCompat.getActivities(
                context,
                requestCode,
                intents,
                /* isMutable= */ false,
                PendingIntent.FLAG_UPDATE_CURRENT));
    assertThat(shadow.isActivityIntent()).isTrue();
  }

  @Test
  public void getActivity_withBundle() {
    int requestCode = 7465;
    Intent intent = new Intent();
    Bundle options = new Bundle();
    ShadowPendingIntent shadow =
        shadowOf(
            PendingIntentCompat.getActivity(
                context,
                requestCode,
                intent,
                /* isMutable= */ false,
                PendingIntent.FLAG_UPDATE_CURRENT,
                options));
    assertThat(shadow.isActivityIntent()).isTrue();
  }

  @Test
  public void getActivity() {
    int requestCode = 7465;
    Intent intent = new Intent();
    Bundle options = new Bundle();
    ShadowPendingIntent shadow =
        shadowOf(
            PendingIntentCompat.getActivity(
                context,
                requestCode,
                intent,
                /* isMutable= */ false,
                PendingIntent.FLAG_UPDATE_CURRENT));
    assertThat(shadow.isActivityIntent()).isTrue();
  }

  @Test
  public void getService() {
    int requestCode = 7465;
    Intent intent = new Intent();
    Bundle options = new Bundle();
    ShadowPendingIntent shadow =
        shadowOf(
            PendingIntentCompat.getService(
                context,
                requestCode,
                intent,
                /* isMutable= */ false,
                PendingIntent.FLAG_UPDATE_CURRENT));
    assertThat(shadow.isService()).isTrue();
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

  @Config(sdk = Build.VERSION_CODES.S)
  @Test
  public void getForegroundService() {
    int requestCode = 7465;
    Intent intent = new Intent();
    Bundle options = new Bundle();
    ShadowPendingIntent shadow =
        shadowOf(
            PendingIntentCompat.getForegroundService(
                context,
                requestCode,
                intent,
                /* isMutable= */ false,
                PendingIntent.FLAG_UPDATE_CURRENT));
    assertThat(shadow.isForegroundService()).isTrue();
  }
}
