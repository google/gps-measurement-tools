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
  public void getActivities_notMutableOnPreM() {
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
    assertThat(shadow.getFlags()).isEqualTo(PendingIntent.FLAG_UPDATE_CURRENT);
    assertThat(shadow.getRequestCode()).isEqualTo(requestCode);
  }

  @Config(sdk = Build.VERSION_CODES.M)
  @Test
  public void getActivities_notMutableOnMPlus() {
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
    assertThat(shadow.getFlags())
        .isEqualTo(PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    assertThat(shadow.getRequestCode()).isEqualTo(requestCode);
  }

  @Config(sdk = Build.VERSION_CODES.R)
  @Test
  public void getActivities_notMutableOnPreS() {
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
    assertThat(shadow.getFlags()).isEqualTo(PendingIntent.FLAG_UPDATE_CURRENT);
    assertThat(shadow.getRequestCode()).isEqualTo(requestCode);
  }

  @Config(sdk = Build.VERSION_CODES.R)
  @Test
  public void getActivities_MutableOnPreS() {
    int requestCode = 7465;
    Intent[] intents = new Intent[] {};
    Bundle options = new Bundle();
    ShadowPendingIntent shadow =
        shadowOf(
            PendingIntentCompat.getActivities(
                context,
                requestCode,
                intents,
                /* isMutable= */ true,
                PendingIntent.FLAG_UPDATE_CURRENT,
                options));
    assertThat(shadow.isActivityIntent()).isTrue();
    assertThat(shadow.getFlags()).isEqualTo(PendingIntent.FLAG_UPDATE_CURRENT);
    assertThat(shadow.getRequestCode()).isEqualTo(requestCode);
  }

  @Config(sdk = Build.VERSION_CODES.R)
  @Test
  public void getActivities_MutableOnSPlus() {
    int requestCode = 7465;
    Intent[] intents = new Intent[] {};
    Bundle options = new Bundle();
    ShadowPendingIntent shadow =
        shadowOf(
            PendingIntentCompat.getActivities(
                context,
                requestCode,
                intents,
                /* isMutable= */ true,
                PendingIntent.FLAG_UPDATE_CURRENT,
                options));
    assertThat(shadow.isActivityIntent()).isTrue();
    assertThat(shadow.getFlags())
        .isEqualTo(PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    assertThat(shadow.getRequestCode()).isEqualTo(requestCode);
  }

  @Config(sdk = Build.VERSION_CODES.R)
  @Test
  public void getActivities_notMutableOnSPlus() {
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
    assertThat(shadow.getFlags())
        .isEqualTo(PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    assertThat(shadow.getRequestCode()).isEqualTo(requestCode);
  }

  @Config(sdk = Build.VERSION_CODES.R)
  @Test
  public void getService() {
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
    assertThat(shadow.isService()).isTrue();
  }

  @Config(sdk = Build.VERSION_CODES.R)
  @Test
  public void getBroadcast() {
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
    assertThat(shadow.isBroadcast()).isTrue();
  }
}
