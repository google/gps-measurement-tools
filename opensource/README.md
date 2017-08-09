# GPS Measurement Tools

The GNSS Measurement Tools code is provided for you to:

* read data from GnssLogger App,
* compute and visualize pseudoranges, 
* compute weighted least squares position and velocity,
* view and analyze carrier phase (if it is present in the log file).

# Origin

This code is maintained on GitHub at the following link:

https://github.com/google/gps-measurement-tools

# Matlab

## Initial setup:

1. Extract the contents of the zip file to a directory, for example:

        ~/gpstools/*

    and include the directory '~/gpstools/opensource' in your matlab path:

        addpath('~/gpstools/opensource');

    (Note: the tilde '~' is a place holder, don't actually use it, fill in
    the actual complete path)

2. Edit ProcessGnssMeasScript.m to add the demoFiles directory, as follows:

        dirName = '~/gpstools/opensource/demoFiles'

    (again, replace tilde '~' with actual complete path)

3. Run ProcessGnssMeasScript.m, it will run with pre-recorded log files.

### To process a log file you collected from GnssLogger:

1. save the log file in a directory
2. edit ProcessGpsMeasScript.m, specify the file name and directory path
3. run ProcessGpsMeasScript.m

The code includes a function (GetNasaHourlyEphemeris.m) to read ephemeris
files from the NASA's archive of Space Geodesy Data, ftp://cddis.gsfc.nasa.gov
It will automatically go to the ftp when you have a new log file.
On some systems you need to use passive mode FTP; if this is required, see 
The Mathworks site for how to do it.
Or (simpler): get the appropriate ephemeris file 'by hand' from the Nasa ftp 
site (GetNasaHourlyEphemeris.m will tell you the correct url and filename), 
copy the file to the directory where your log file is, 
and GetNasaHourlyEphemeris.m will read it from there.

### For a summary of the open source GNSS Measurements Tools

See  ~/gpstools/opensource/Contents.m or type 'help opensource' in matlab
command window.

## Platform specific notes:

For Windows: use '\' (backslash), instead of '/' for directories.

For Mac: when installing MATLAB. 
System Preferences --> Security & Privacy -->
Allow Apps to be downloaded from: Mac App Store and identified developers

Uncompress/Unzip utility called from GetNasaHourlyEphemeris.m:
The ephemeris on the Nasa ftp is Unix-compressed. GetNasaHourlyEphemeris will 
automatically uncompress it, if you have the right uncompress function on your 
computer. If you need to install an unzip utility, see http://www.gpzip.org
Then search for 'uncompress' in the GetNasaHourlyEphemeris function to find and 
edit the name of the unzip utility:

    unzipCommand='uncompress';%edit if your platform uses something different 
  
If you uncompress the file 'by hand' and rerun GetNasaHourlyEphemeris.m, it will
read the uncompressed file.

# GNSSLogger

Sample App that allows registering for various Android location related measurements
and log the measurements to the screen and optionally to a text file.

This source code is supplied as an Android Studio project that can be built and run
with [Android Studio](https://developer.android.com/studio/index.html).

The APK is also provided for convenience.

# Pseudorange Library

Position Solution Engine from Gnss Raw measurements as a dependency android library of
GnssLogger application. Part of the dependencies concerning communicating with SUPL server
to retrieve ephemeris info has been packed in Jar for project cleanness. To access the SUPL server
related code, please visit git repository of Android CTS.
https://android.googlesource.com/platform/cts/+/master/tests/tests/location/src/android/location/cts

# Copyright Notice

Copyright 2016 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


