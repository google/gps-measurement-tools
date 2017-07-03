% GNSS Tools Open Source, MATLAB tools to:
%   read data from GnssLogger App,
%   compute and visualize pseudoranges, 
%   compute weighted least squares position and velocity,
%   view and analyze carrier phase (if it is present in the log file).
%
% ProcessGnssMeasScript - script to set file name and call all other functions
% Start with this file, and the run the demo log files provided with this code
%
% Coordinate transformations
% Lla2Ned   - Difference of [lat,lon,alt], return NED coords in meters
% Lla2Xyz   - lat,lon,alt transform to x,y,z (Earth Centered Earth Fixed)
% RotEcef2Ned - Rotation matrix to convert ECEF vector to NED, & vice versa
% Xyz2Lla   - x,y,z (Earth Centered Earth Fixed) transform to lat,lon,alt
%
% Ephemeris and Orbit functions
% CheckGpsEphInputs - Check the inputs for all GpsEph2* functions
% ClosestGpsEph - find unique fresh ephemeris from a GPS ephemeris structure
% GpsEph2Dtsv   - Satellite clock bias from GPS ephemeris
% GpsEph2Pvt    - Satellite position, velocity and clock bias from GPS ephemeris
% GpsEph2Xyz    - Satellite position from GPS ephemeris
% FlightTimeCorrection - Rotated coords from Earth rotation during flight time
% Kepler        - Solve Kepler's equation for eccentric anomaly
% ReadRinexNav  - Read ephemeris & iono data from an ASCII formatted RINEX2 file
% 
% Navigation, Pseudorange and Accumulated Delta Range functions
% GpsAdrResiduals - Residuals from GPS Accumulated Delta Ranges (carrier)
% GpsWlsPvt       - Position Velocity and Time from GPS measurements
% WlsPvt          - Weighted least squares PVT solution from pr and prr
% ProcessAdr      - Compute Delta PR minus ADR (carrier)
% ProcessGnssMeas - Process raw GnssLogger measurements and compute pseudoranges
%
% Plotting functions
% PlotAdrResids - Plot the Accumulated Delta Range (carrier) residuals 
% PlotCno       - Plot the carrier-to-noise-density ratio, C/No, from gnssMeas
% PlotPseudoranges - Plot the pseudoranges obtained from ProcessGnssMeas
% PlotPvt       - Plot the results of GpsWlsPvt
% PlotPvtStates - Plot Position, Velocity and Time/clock states
%
% Time functions
% DayOfYear     - Day number of the year
% Gps2Utc       - Convert GPS time (week & seconds) to UTC
% JulianDay     - Number of days since first GPS week
% LeapSeconds   - Number of leap seconds since the first GPS week
% Utc2Gps       - Convert UTC time to GPS time
%
% File reading
% GetNasaHourlyEphemeris - Read hourly ephemeris file
% ReadRinexNav          - Read ephemeris & iono data from a RINEX2 Nav file
% ReadGnssLogger        - Read the file created by Gnss Logger App in Android
%
% General functions and classes
% CompareVersions - Compare two version numbers
% GnssThresholds  - GNSS validity thresholds we use in the code
% GpsConstants    - GPS constants, from WGS84 and IS-GPS-200

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

% Copyright 2016 Google Inc.
% 
% Licensed under the Apache License, Version 2.0 (the "License");
% you may not use this file except in compliance with the License.
% You may obtain a copy of the License at
% 
%     http://www.apache.org/licenses/LICENSE-2.0
% 
% Unless required by applicable law or agreed to in writing, software
% distributed under the License is distributed on an "AS IS" BASIS,
% WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
% See the License for the specific language governing permissions and
% limitations under the License.
    
