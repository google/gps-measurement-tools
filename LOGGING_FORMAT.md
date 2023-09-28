# Logging

GnssLogger allows you to output raw data about GNSS/GPS to files so they can be visualized in the [GNSS Analysis App](https://developer.android.com/guide/topics/sensors/gnss.html#analyze).

## Row prefixes

Each row of the file is prefixed with a string designating the data type:
* `Raw` - Raw [GNSS measurements](https://developer.android.com/reference/android/location/GnssMeasurement)
* `Fix` - [Location](https://developer.android.com/reference/android/location/Location) fix information
* `Nav` - [Navigation message](https://developer.android.com/reference/android/location/GnssNavigationMessage)
* `NMEA` - [NMEA sentences](https://developer.android.com/reference/android/location/OnNmeaMessageListener)
* `UncalAccel` and `Accel` - The [uncalibrated](https://developer.android.com/reference/android/hardware/Sensor#STRING_TYPE_ACCELEROMETER_UNCALIBRATED) and [calibrated](https://developer.android.com/reference/android/hardware/Sensor#STRING_TYPE_ACCELEROMETER) versions of the accelerometer sensor, respectively
* `UncalGyro` and `Gyro` - The [uncalibrated](https://developer.android.com/reference/android/hardware/Sensor#STRING_TYPE_GYROSCOPE_UNCALIBRATED) and [calibrated](https://developer.android.com/reference/android/hardware/Sensor#STRING_TYPE_GYROSCOPE) versions of the gyroscope sensor, respectively
* `UncalMag` and `Mag` - The [uncalibrated](https://developer.android.com/reference/android/hardware/Sensor#TYPE_MAGNETIC_FIELD_UNCALIBRATED) and [calibrated](https://developer.android.com/reference/android/hardware/Sensor#TYPE_MAGNETIC_FIELD) versions of the magnetometer sensor, respectively
* `OrientationDeg` - The [rotation vector](https://developer.android.com/reference/android/hardware/Sensor#TYPE_ROTATION_VECTOR) sensor
* `GnssAntennaInfo` - [Antenna characteristics](https://developer.android.com/reference/android/location/GnssAntennaInfo) for the device model
* `Status` - [GNSS status](https://developer.android.com/reference/android/location/GnssStatus)

If the device supports uncalibrated sensors (accelerometer, gyroscope, magnetometer) then those values are logged, otherwise the calibrated version of those sensors is logged (see [`SensorEvent.values()`](https://developer.android.com/reference/android/hardware/SensorEvent#values) for differences between the two).

## Header

The header of the CSV file explains the fields logged for each data type:

```
# Raw,utcTimeMillis,TimeNanos,LeapSecond,TimeUncertaintyNanos,FullBiasNanos,BiasNanos,BiasUncertaintyNanos,DriftNanosPerSecond,DriftUncertaintyNanosPerSecond,HardwareClockDiscontinuityCount,Svid,TimeOffsetNanos,State,ReceivedSvTimeNanos,ReceivedSvTimeUncertaintyNanos,Cn0DbHz,PseudorangeRateMetersPerSecond,PseudorangeRateUncertaintyMetersPerSecond,AccumulatedDeltaRangeState,AccumulatedDeltaRangeMeters,AccumulatedDeltaRangeUncertaintyMeters,CarrierFrequencyHz,CarrierCycles,CarrierPhase,CarrierPhaseUncertainty,MultipathIndicator,SnrInDb,ConstellationType,AgcDb,BasebandCn0DbHz,FullInterSignalBiasNanos,FullInterSignalBiasUncertaintyNanos,SatelliteInterSignalBiasNanos,SatelliteInterSignalBiasUncertaintyNanos,CodeType,ChipsetElapsedRealtimeNanos
# 
# UncalAccel,utcTimeMillis,elapsedRealtimeNanos,UncalAccelXMps2,UncalAccelYMps2,UncalAccelZMps2,BiasXMps2,BiasYMps2,BiasZMps2
# 
# Accel,utcTimeMillis,elapsedRealtimeNanos,AccelXMps2,AccelYMps2,AccelZMps2
# 
# UncalGyro,utcTimeMillis,elapsedRealtimeNanos,UncalGyroXRadPerSec,UncalGyroYRadPerSec,UncalGyroZRadPerSec,DriftXRadPerSec,DriftYRadPerSec,DriftZRadPerSec
# 
# Gyro,utcTimeMillis,elapsedRealtimeNanos,GyroXRadPerSec,GyroYRadPerSec,GyroZRadPerSec
# 
# UncalMag,utcTimeMillis,elapsedRealtimeNanos,UncalMagXMicroT,UncalMagYMicroT,UncalMagZMicroT,BiasXMicroT,BiasYMicroT,BiasZMicroT
# 
# Mag,utcTimeMillis,elapsedRealtimeNanos,MagXMicroT,MagYMicroT,MagZMicroT
# 
# OrientationDeg,utcTimeMillis,elapsedRealtimeNanos,yawDeg,rollDeg,pitchDeg
# 
# Fix,Provider,LatitudeDegrees,LongitudeDegrees,AltitudeMeters,SpeedMps,AccuracyMeters,BearingDegrees,UnixTimeMillis,SpeedAccuracyMps,BearingAccuracyDegrees,elapsedRealtimeNanos,VerticalAccuracyMeters,MockLocation
# 
# Nav,Svid,Type,Status,MessageId,Sub-messageId,Data(Bytes)
# 
# Status,UnixTimeMillis,SignalCount,SignalIndex,ConstellationType,Svid,CarrierFrequencyHz,Cn0DbHz,AzimuthDegrees,ElevationDegrees,UsedInFix,HasAlmanacData,HasEphemerisData,BasebandCn0DbHz
#
# Agc,utcTimeMillis,TimeNanos,LeapSecond,TimeUncertaintyNanos,FullBiasNanos,BiasNanos,BiasUncertaintyNanos,DriftNanosPerSecond,DriftUncertaintyNanosPerSecond,HardwareClockDiscontinuityCount,AgcDb,CarrierFrequencyHz,ConstellationType
```

Note that on devices that shipped with Android T and higher, Automatic Gain Control is logged to its own records starting with `Agc` - the `AgcDb` field that is part of the `Raw` message will be empty.

## Sample data

Sample data looks like:

```
Fix,GPS,28.1435263000,-82.2971356000,-14.200000762939453,0.09635656,5.857,108.336426,1663712656000,,,445420306913617,11.185,0
Status,1663712658000,18,0,1,1,,12.80,285.0,16.0,1,1,0,
Status,1663712658000,18,1,1,3,,19.00,320.0,14.0,1,1,0,
Raw,1663707711608,32083712000000,18,,-1347710845896978382,0.800237774848938,15000247.00164795,-27.699302103483184,39.875882248753776,31,10,0.0,1,79120,53,29.6,102.51119995117188,2.240000009536743,16,0.0,0.0,1575420030,,,,0,,1,1.11,24.6,0.0,0.0,,,C,30663387810588
Raw,1663707711608,32083712000000,18,,-1347710845896978382,0.800237774848938,15000247.00164795,-27.699302103483184,39.875882248753776,31,21,0.0,35,14,35,32.6,-175.76475524902344,0.8314999938011169,16,0.0,0.0,1575420030,,,,0,,1,1.11,27.6,0.0,0.0,,,C,30663387810588
UncalMag,1663164669191,65972055195160,221.51251,89.793755,-21.16875,213.84875,180.1958,5.4516187
UncalGyro,1663164669189,65972053803181,0.0006108648,-0.020769402,0.023518294,-0.009432952,0.000007726701,-0.008078051
UncalAccel,1663164669190,65972054923181,0.4642076,7.7742805,5.8707905,0.0,0.0,0.0
OrientationDeg,1663164669209,65972073175160,201.0,-5.0,-52.0
Nav,3,769,1,-1,14,116,5,-126,11,42,-69,104,85,-17,63,-96
Nav,30,1281,1,12,5,75,-83,-64,-34,75,-83,-64,-34,75,-83,-64,-34,-64,44,-61,-11,-45,-60,53,-23,-48,14,104,-30,-30,-128,-21,-67,-68,-101,-55,6,-59,80,3,-107,-80,-7,10,35
Agc,1675109588736,4105000000,,,-1359144802631003092,0.0,2200000588,,,52,19.718292236328125,1176450000,1
Agc,1675109588736,4105000000,,,-1359144802631003092,0.0,2200000588,,,52,49.79810333251953,1602000000,3
```

## Field definitions

A description of the different logged fields can be found below.

`Raw` - The raw GNSS measurements of one GNSS signal (each satellite may have 1-2 signals for L5-enabled smartphones), collected from the Android API [GnssMeasurement](https://developer.android.com/reference/android/location/GnssMeasurement):
* utcTimeMillis - Milliseconds since UTC epoch (1970/1/1), converted from GnssClock
* TimeNanos - The GNSS receiver internal hardware clock value in nanoseconds.
* LeapSecond - The leap second associated with the clock's time.
* TimeUncertaintyNanos - The clock's time uncertainty (1-sigma) in nanoseconds.
* FullBiasNanos - The difference between hardware clock getTimeNanos() inside GPS receiver and the true GPS time since 0000Z, January 6, 1980, in nanoseconds.
* BiasNanos - The clock's sub-nanosecond bias.
* BiasUncertaintyNanos - The clock's bias uncertainty (1-sigma) in nanoseconds.
* DriftNanosPerSecond - The clock's drift in nanoseconds per second.
* DriftUncertaintyNanosPerSecond - The clock's drift uncertainty (1-sigma) in nanoseconds per second.
* HardwareClockDiscontinuityCount - Count of hardware clock discontinuities.
* Svid - The satellite ID. More info can be found [here](https://developer.android.com/reference/android/location/GnssStatus#getSvid(int)).
* TimeOffsetNanos - The time offset at which the measurement was taken in nanoseconds.
* State - Integer signifying sync state of the satellite. Each bit in the integer attributes to a particular state information of the measurement.
* ReceivedSvTimeNanos - The received GNSS satellite time, at the measurement time, in nanoseconds.
* ReceivedSvTimeUncertaintyNanos - The error estimate (1-sigma) for the received GNSS time, in nanoseconds.
* Cn0DbHz - The carrier-to-noise density in dB-Hz.
* PseudorangeRateMetersPerSecond - The pseudorange rate at the timestamp in m/s.
* PseudorangeRateUncertaintyMetersPerSecond - The pseudorange's rate uncertainty (1-sigma) in m/s.
* AccumulatedDeltaRangeState - This indicates the state of the 'Accumulated Delta Range' measurement. Each bit in the integer attributes to state of the measurement.
* AccumulatedDeltaRangeMeters - The accumulated delta range since the last channel reset, in meters.
* AccumulatedDeltaRangeUncertaintyMeters - The accumulated delta range's uncertainty (1-sigma) in meters.
* CarrierFrequencyHz - The carrier frequency of the tracked signal.
* CarrierCycles - The number of full carrier cycles between the satellite and the receiver.
* CarrierPhase - The RF phase detected by the receiver.
* CarrierPhaseUncertainty - The carrier-phase's uncertainty (1-sigma).
* MultipathIndicator - A value indicating the 'multipath' state of the event.
* SnrInDb - The (post-correlation & integration) Signal-to-Noise ratio (SNR) in dB.
* ConstellationType - GNSS constellation type. The value is one of those constants with CONSTELLATION_ prefix in [GnssStatus](https://developer.android.com/reference/android/location/GnssStatus). 
* AgcDb **(deprecated)** - The Automatic Gain Control level in dB. On devices that shipped with Android T and higher, Automatic Gain Control is logged to its own records starting with `Agc` - this `AgcDb` field that is part of the `Raw` message will be empty. 
* BasebandCn0DbHz - The baseband carrier-to-noise density in dB-Hz. Only available in Android 11 and higher.
* FullInterSignalBiasNanos - The GNSS measurement's inter-signal bias in nanoseconds with sub-nanosecond accuracy. Only available in Android 11 and higher.
* FullInterSignalBiasUncertaintyNanos - The GNSS measurement's inter-signal bias uncertainty (1 sigma) in nanoseconds with sub-nanosecond accuracy. Only available in Android 11 and higher.
* SatelliteInterSignalBiasNanos - The GNSS measurement's satellite inter-signal bias in nanoseconds with sub-nanosecond accuracy. Only available in Android 11 and higher.
* SatelliteInterSignalBiasUncertaintyNanos - The GNSS measurement's satellite inter-signal bias uncertainty (1 sigma) in nanoseconds with sub-nanosecond accuracy. Only available in Android 11 and higher.
* CodeType - The GNSS measurement's [code type](https://developer.android.com/reference/android/location/GnssMeasurement#getCodeType()).
* ChipsetElapsedRealtimeNanos - The elapsed real-time of this clock since system boot, in nanoseconds.

`Status` - The status of a GNSS signal, as collected from the Android API [GnssStatus](https://developer.android.com/reference/android/location/GnssStatus).
* UnixTimeMillis - Milliseconds since UTC epoch (1970/1/1), recorded from the most recent location update from the GPS_PROVIDER. If no GPS_PROVIDER location has been acquired yet this field will be empty. If the GPS_PROVIDER loses a fix (i.e., there is no location update within two seconds of a given Status epoch), this field will also be empty.
* SignalCount - The total number of satellites in the satellite list.
* SignalIndex - The index of current signal.
* ConstellationType: The constellation type of the satellite at the specified index. The value is one of those constants with CONSTELLATION_ prefix in [GnssStatus](https://developer.android.com/reference/android/location/GnssStatus).
* Svid: The satellite ID. More info can be found [here](https://developer.android.com/reference/android/location/GnssStatus#getSvid(int)).
* CarrierFrequencyHz: The carrier frequency of the signal tracked.
* Cn0DbHz: The carrier-to-noise density at the antenna of the satellite at the specified index in dB-Hz.
* AzimuthDegrees: The azimuth the satellite at the specified index.
* ElevationDegrees: The elevation of the satellite at the specified index.
* UsedInFix: Whether the satellite at the specified index was used in the calculation of the most recent position fix (`0` or `1`).
* HasAlmanacData: Whether the satellite at the specified index has almanac data (`0` or `1`).
* HasEphemerisData: Whether the satellite at the specified index has ephemeris data (`0` or `1`).
* BasebandCn0DbHz: The baseband carrier-to-noise density of the satellite at the specified index in dB-Hz. Only available in Android 11 and higher.

`UncalAccel` - Readings from the uncalibrated accelerometer, as collected from the Android API [Sensor.TYPE_ACCELEROMETER_UNCALIBRATED](https://developer.android.com/reference/android/hardware/SensorEvent#sensor.type_accelerometer_uncalibrated:):
* utcTimeMillis - The sum of elapsedRealtimeNanos below and the estimated device boot time at UTC, after a recent NTP (Network Time Protocol) sync.
* elapsedRealtimeNanos - The time in nanoseconds at which the event happened.
* UncalAccel[X/Y/Z]Mps2 - [x/y/z]_uncalib without bias compensation.
* Bias[X/Y/Z]Mps2 - Estimated [x/y/z]_bias.

`UncalGyro` - Readings from the uncalibrated gyroscope, as collected from the Android API [Sensor.TYPE_GYROSCOPE_UNCALIBRATED](https://developer.android.com/reference/android/hardware/SensorEvent#sensor.type_gyroscope_uncalibrated:):
* utcTimeMillis - The sum of elapsedRealtimeNanos below and the estimated device boot time at UTC, after a recent NTP (Network Time Protocol) sync.
* elapsedRealtimeNanos - The time in nanoseconds at which the event happened.
* UncalGyro[X/Y/Z]RadPerSec - Angular speed (w/o drift compensation) around the [X/Y/Z] axis in rad/s.
* Drift[X/Y/Z]RadPerSec - Estimated drift around [X/Y/Z] axis in rad/s.

`UncalMag` - Readings from the uncalibrated magnetometer as collected from the Android API [Sensor.STRING_TYPE_MAGNETIC_FIELD_UNCALIBRATED](https://developer.android.com/reference/android/hardware/SensorEvent#TYPE_MAGNETIC_FIELD_UNCALIBRATED):
* utcTimeMillis - The sum of elapsedRealtimeNanos below and the estimated device boot time at UTC, after a recent NTP (Network Time Protocol) sync.
* elapsedRealtimeNanos - The time in nanoseconds at which the event happened.
* UncalMag[X/Y/Z]MicroT - [x/y/z]_uncalib without bias compensation.
* Bias[X/Y/Z]MicroT - Estimated [x/y/z]_bias.

`OrientationDeg` - Each row represents an estimated device orientation, collected from Android API [SensorManager.getOrientation()](https://developer.android.com/reference/android/hardware/SensorManager#getOrientation(float[],%20float[])):
* utcTimeMillis - The sum of elapsedRealtimeNanos below and the estimated device boot time at UTC, after a recent NTP (Network Time Protocol) sync.
* elapsedRealtimeNanos - The time in nanoseconds at which the event happened.
* yawDeg - If the screen is in portrait orientation, this value equals the Azimuth degree (modulus to 0° and 360°). If the screen is in landscape orientation, it equals the sum (modulus to 0° and 360°) of the screen rotation angle (either 90° or 270°) and the Azimuth degree. Azimuth, refers to the angle of rotation about the -z axis. This value represents the angle between the device's y axis and the magnetic north pole.
* rollDeg - Roll, angle of rotation about the y axis. This value represents the angle between a plane perpendicular to the device's screen and a plane perpendicular to the ground.
* pitchDeg - Pitch, angle of rotation about the x axis. This value represents the angle between a plane parallel to the device's screen and a plane parallel to the ground.

`Fix` - Each row represents a [Location](https://developer.android.com/reference/android/location/Location), collected from the [Location API](https://developer.android.com/reference/android/location/LocationListener) (for `GPS` from the [`GPS_PROVIDER`](https://developer.android.com/reference/android/location/LocationManager#GPS_PROVIDER) and `NLP` from the [`NETWORK_PROVIDER`](https://developer.android.com/reference/android/location/LocationManager#NETWORK_PROVIDER)) and [Fused Location API](https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient.html) (for `FLP` from the [`FUSED_PROVIDER`](https://developer.android.com/reference/android/location/LocationManager#FUSED_PROVIDER)).

`Agc` - Each row represents a combination of the [GnssClock](https://developer.android.com/reference/android/location/GnssClock) and [GnssAutomaticGainControl](https://developer.android.com/reference/android/location/GnssAutomaticGainControl) that are reported as part of a [GnssMeasurementEvent](https://developer.android.com/reference/android/location/GnssMeasurementsEvent). Note that there will be multiple AGC records (one per carrier frequency per constellation type - i.e., all satellites of the same constellation and same frequency have the same AGC), reported in [GnssMeasurementsEvent.getGnssAutomaticGainControls()](https://developer.android.com/reference/android/location/GnssMeasurementsEvent#getGnssAutomaticGainControls()), for each clock/event.

## References

* Google's documentation for the [Smartphone Decimeter Challenge](https://www.kaggle.com/c/google-smartphone-decimeter-challenge/data#).
