function [xM,dtsvS,vMps,dtsvSDot] = GpsEph2Pvt(gpsEph,gpsTime)
%[xM,dtsvS,vMps,dtsvSDot] = GpsEph2Pvt(gpsEph,gpsTime)
%
% Calculate sv coordinates, in ECEF frame, sv clock bias, and sv velocity
% Inputs:
% gpsEph: vector of ephemeris structures, as defined in ReadRinexEph.m
%
% gpsTime = [gpsWeek, ttxSec]: GPS time at time of transmission, ttx
%   ttx = trx - PR/c - dtsvS, where trx is time of reception (receiver clock),
%     dtsvS is the satellite clock error (seconds), can be computed in advance 
%     using eph2dtsv.m or iterating this function: gps time = sat time - dtsvS
% gpsWeek, ttxSec must be vectors of length(gpsEph), 
%
% outputs:
%   xM = [i,j,k] matrix of coordinates of satellites (ecef meters)
%   dtsvS = vector of satellite clock error (seconds)
%   vMps = [i,j,k] matrix of satellite velocity (ecef m/s)
%   dtsvSDot = vector of satellite clock error rate (seconds/second)
% The row dimension of xM, dtsvS, vMps, dtsvSDot = length(gpsEph)
%
% xM and vMps are the satellite positions and velocities
% at time ttxSec, in terms of ecef coords at the same time
% Use FlightTimeCorrection.m to get xM & vMps in ecef coord at time of reception
%
% functions called: GpsEph2Xyz.m
%
% See  IS-GPS-200 for details of data

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements

vMps = []; dtsvSDot = [];

[xM,dtsvS]=GpsEph2Xyz(gpsEph,gpsTime);
if isempty(xM)
    return
end 

%compute velocity from delta position & dtsvS at (t+0.5) - (t-0.5)
%This is better than differentiating, because both the orbital and relativity 
%terms have nonlinearities that are not easily differentiable
t1 = [gpsTime(:,1), gpsTime(:,2)+0.5]; %time + 0.5 seconds
[xPlus,dtsvPlus] = GpsEph2Xyz(gpsEph,t1);
t1 = [gpsTime(:,1), gpsTime(:,2)-0.5]; %time - 0.5 seconds
[xMinus,dtsvMinus]= GpsEph2Xyz(gpsEph,t1);
vMps = xPlus - xMinus; 
dtsvSDot = dtsvPlus - dtsvMinus;

end %end of function GpsEph2Pvt
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


