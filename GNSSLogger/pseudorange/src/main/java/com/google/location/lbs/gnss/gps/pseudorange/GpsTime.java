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

package com.google.location.lbs.gnss.gps.pseudorange;

import android.util.Pair;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Longs;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * A simple class to represent time unit used by GPS.
 */
public class GpsTime implements Comparable<GpsTime> {
  public static final int MILLIS_IN_SECOND = 1000;
  public static final int SECONDS_IN_MINUTE = 60;
  public static final int MINUTES_IN_HOUR = 60;
  public static final int HOURS_IN_DAY = 24;
  public static final int SECONDS_IN_DAY =
      HOURS_IN_DAY * MINUTES_IN_HOUR * SECONDS_IN_MINUTE;
  public static final int DAYS_IN_WEEK = 7;
  public static final long MILLIS_IN_DAY = TimeUnit.DAYS.toMillis(1);
  public static final long MILLIS_IN_WEEK = TimeUnit.DAYS.toMillis(7);
  public static final long NANOS_IN_WEEK = TimeUnit.DAYS.toNanos(7);
  // GPS epoch is 1980/01/06
  public static final long GPS_DAYS_SINCE_JAVA_EPOCH = 3657;
  public static final long GPS_UTC_EPOCH_OFFSET_SECONDS =
      TimeUnit.DAYS.toSeconds(GPS_DAYS_SINCE_JAVA_EPOCH);
  public static final long GPS_UTC_EPOCH_OFFSET_NANOS =
      TimeUnit.SECONDS.toNanos(GPS_UTC_EPOCH_OFFSET_SECONDS);
  private static final DateTimeZone UTC_ZONE = DateTimeZone.UTC;
  private static final DateTime LEAP_SECOND_DATE_1981 =
      new DateTime(1981, 7, 1, 0, 0, UTC_ZONE);
  private static final DateTime LEAP_SECOND_DATE_2012 =
      new DateTime(2012, 7, 1, 0, 0, UTC_ZONE);
  private static final DateTime LEAP_SECOND_DATE_2015 =
      new DateTime(2015, 7, 1, 0, 0, UTC_ZONE);
  private static final DateTime LEAP_SECOND_DATE_2017 =
      new DateTime(2017, 1, 1, 0, 0, UTC_ZONE);
  // nanoseconds since GPS epoch (1980/1/6).
  private long gpsNanos;

  /**
   * Constructor for GpsTime. Input values are all in GPS time.
   * @param year Year
   * @param month Month from 1 to 12
   * @param day Day from 1 to 31
   * @param hour Hour from 0 to 23
   * @param minute Minute from 0 to 59
   * @param second Second from 0 to 59
   */
  public GpsTime(int year, int month, int day, int hour, int minute, double second) {
    DateTime utcDateTime = new DateTime(year, month, day, hour, minute,
        (int) second, (int) (second * 1000) % 1000, UTC_ZONE);

    // Since input time is already specify in GPS time, no need to count leap second here.
    gpsNanos = TimeUnit.MILLISECONDS.toNanos(utcDateTime.getMillis())
        - GPS_UTC_EPOCH_OFFSET_NANOS;
  }

  /**
   * Constructor
   * @param dateTime is created using GPS time values.
   */
  public GpsTime(DateTime dateTime) {
    gpsNanos = TimeUnit.MILLISECONDS.toNanos(dateTime.getMillis())
        - GPS_UTC_EPOCH_OFFSET_NANOS;
  }

  /**
   * Constructor
   * @param gpsNanos nanoseconds since GPS epoch.
   */
  public GpsTime(long gpsNanos) {
    this.gpsNanos = gpsNanos;
  }

  /**
   * Creates a GPS time using a UTC based date and time.
   * @param dateTime represents the current time in UTC time, must be after 2009
   */
  public static GpsTime fromUtc(DateTime dateTime) {
    return new GpsTime(
        TimeUnit.MILLISECONDS.toNanos(dateTime.getMillis())
            + TimeUnit.SECONDS.toNanos(
            GpsTime.getLeapSecond(dateTime) - GPS_UTC_EPOCH_OFFSET_SECONDS));
  }

  /**
   * Creates a GPS time based upon the current time.
   */
  public static GpsTime now() {
    return fromUtc(DateTime.now(DateTimeZone.UTC));
  }

  /**
   * Creates a GPS time using absolute GPS week number, and the time of week.
   * @param gpsWeek
   * @param towSec GPS time of week in second
   * @return actual time in GpsTime.
   */
  public static GpsTime fromWeekTow(int gpsWeek, int towSec) {
    long nanos = gpsWeek * NANOS_IN_WEEK + TimeUnit.SECONDS.toNanos(towSec);
    return new GpsTime(nanos);
  }

  /**
   * Creates a GPS time using YUMA GPS week number (0..1023), and the time of week.
   * @param yumaWeek (0..1023)
   * @param towSec GPS time of week in second
   * @return actual time in GpsTime.
   */
  public static GpsTime fromYumaWeekTow(int yumaWeek, int towSec) {
    Preconditions.checkArgument(yumaWeek >= 0);
    Preconditions.checkArgument(yumaWeek < 1024);

    // Estimate the multiplier of current week.
    DateTime currentTime = DateTime.now(UTC_ZONE);
    GpsTime refTime = new GpsTime(currentTime);
    Pair<Integer, Integer> refWeekSec = refTime.getGpsWeekSecond();
    int weekMultiplier = refWeekSec.first / 1024;

    int gpsWeek = weekMultiplier * 1024 + yumaWeek;
    return fromWeekTow(gpsWeek, towSec);
  }

  public static GpsTime fromTimeSinceGpsEpoch(long gpsSec) {
    return new GpsTime(TimeUnit.SECONDS.toNanos(gpsSec));
  }

  /**
   * Computes leap seconds. Only accurate after 2009.
   * @param time
   * @return number of leap seconds since GPS epoch.
   */
  public static int getLeapSecond(DateTime time) {
    if (LEAP_SECOND_DATE_2017.compareTo(time) <= 0) {
      return 18;
    } else if (LEAP_SECOND_DATE_2015.compareTo(time) <= 0) {
      return 17;
    } else if (LEAP_SECOND_DATE_2012.compareTo(time) <= 0) {
      return 16;
    } else if (LEAP_SECOND_DATE_1981.compareTo(time) <= 0) {
      // Only correct between 2012/7/1 to 2008/12/31
      return 15;
    } else {
      return 0;
    }
  }

  /**
   * Computes GPS weekly epoch of the reference time.
   * <p>GPS weekly epoch are defined as of every Sunday 00:00:000 (mor
   * @param refTime reference time
   * @return nanoseconds since GPS epoch, for the week epoch.
   */
  public static Long getGpsWeekEpochNano(GpsTime refTime) {
    Pair<Integer, Integer> weekSecond = refTime.getGpsWeekSecond();
    return weekSecond.first * NANOS_IN_WEEK;
  }

  /**
   * @return week count since GPS epoch, and second count since the beginning of
   *         that week.
   */
  public Pair<Integer, Integer> getGpsWeekSecond() {
    // JAVA/UNIX epoch: January 1, 1970 in msec
    // GPS epoch: January 6, 1980 in second
    int week = (int) (gpsNanos / NANOS_IN_WEEK);
    int second = (int) TimeUnit.NANOSECONDS.toSeconds(gpsNanos % NANOS_IN_WEEK);
    return Pair.create(week, second);
  }

  /**
   * @return week count since GPS epoch, and second count in 0.08 sec
   *         resolution, 23-bit presentation (required by RRLP.)"
   */
  public Pair<Integer, Integer> getGpsWeekTow23b() {
    // UNIX epoch: January 1, 1970 in msec
    // GPS epoch: January 6, 1980 in second
    int week = (int) (gpsNanos / NANOS_IN_WEEK);
    // 80 millis is 0.08 second.
    int tow23b = (int) TimeUnit.NANOSECONDS.toMillis(gpsNanos % NANOS_IN_WEEK) / 80;
    return Pair.create(week, tow23b);
  }

  public long[] getBreakdownEpoch(TimeUnit... units) {
    long nanos = this.gpsNanos;
    long[] values = new long[units.length];
    for (int idx = 0; idx < units.length; ++idx) {
      TimeUnit unit = units[idx];
      long value = unit.convert(nanos, TimeUnit.NANOSECONDS);
      values[idx] = value;
      nanos -= unit.toNanos(value);
    }
    return values;
  }

  /**
   * @return Day of year in GPS time (GMT time)
   */
  public static int getCurrentDayOfYear() {
    DateTime current = DateTime.now(DateTimeZone.UTC);
    // Since current is derived from UTC time, we need to add leap second here.
    long gpsTimeMillis = current.getMillis() + getLeapSecond(current);
    DateTime gpsCurrent = new DateTime(gpsTimeMillis, UTC_ZONE);
    return gpsCurrent.getDayOfYear();
  }

  /**
   * @return milliseconds since JAVA/UNIX epoch.
   */
  public final long getMillisSinceJavaEpoch() {
    return TimeUnit.NANOSECONDS.toMillis(gpsNanos + GPS_UTC_EPOCH_OFFSET_NANOS);
  }

  /**
   * @return milliseconds since GPS epoch.
   */
  public final long getMillisSinceGpsEpoch() {
    return TimeUnit.NANOSECONDS.toMillis(gpsNanos);
  }

  /**
   * @return microseconds since GPS epoch.
   */
  public final long getMicrosSinceGpsEpoch() {
    return TimeUnit.NANOSECONDS.toMicros(gpsNanos);
  }

  /**
   * @return nanoseconds since GPS epoch.
   */
  public final long getNanosSinceGpsEpoch() {
    return gpsNanos;
  }

  /**
   * @return the GPS time in Calendar.
   */
  public Calendar getTimeInCalendar() {
    return getGpsDateTime().toGregorianCalendar();
  }

  /**
   * @return a DateTime with leap seconds considered.
   */
  public DateTime getUtcDateTime() {
    DateTime gpsDateTime = getGpsDateTime();
    return new DateTime(
        gpsDateTime.getMillis() - TimeUnit.SECONDS.toMillis(getLeapSecond(gpsDateTime)), UTC_ZONE);
  }

  /**
   * @return a DateTime based on the pure GPS time (without considering leap second).
   */
  public DateTime getGpsDateTime() {
    return new DateTime(TimeUnit.NANOSECONDS.toMillis(gpsNanos
        + GPS_UTC_EPOCH_OFFSET_NANOS), UTC_ZONE);
  }

  /**
   * Compares two {@code GpsTime} objects temporally.
   *
   * @param   other   the {@code GpsTime} to be compared.
   * @return  the value {@code 0} if this {@code GpsTime} is simultaneous with
   *          the argument {@code GpsTime}; a value less than {@code 0} if this
   *          {@code GpsTime} occurs before the argument {@code GpsTime}; and
   *          a value greater than {@code 0} if this {@code GpsTime} occurs
   *          after the argument {@code GpsTime} (signed comparison).
   */
  @Override
  public int compareTo(GpsTime other) {
    return Long.compare(this.getNanosSinceGpsEpoch(), other.getNanosSinceGpsEpoch());
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof GpsTime)) {
      return false;
    }
    GpsTime time = (GpsTime) other;
    return getNanosSinceGpsEpoch() == time.getNanosSinceGpsEpoch();
  }

  @Override
  public int hashCode() {
    return Longs.hashCode(getNanosSinceGpsEpoch());
  }
}