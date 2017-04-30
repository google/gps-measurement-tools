function [xyzM,dtsvS] = GpsEph2Xyz(gpsEph,gpsTime)
%[xyzM,dtsvS] = GpsEph2Xyz(gpsEph,gpsTime)
% Calculate sv coordinates, in ECEF frame, at ttx = gpsTime
%
% Inputs:
% gpsEph: vector of GPS ephemeris structures, as defined in ReadRinexNav.m
%
% gpsTime = [gpsWeek, ttxSec]: GPS time at time of transmission, ttx
%   ttx = trx - PR/c - dtsvS, where trx is time of reception (receiver clock),
%     dtsvS is the satellite clock error (seconds), can be computed in advance 
%     using Eph2Dtsv.m or iterating this function: gps time = sat time - dtsvS
% gpsWeek, ttxSec must be vectors of length(gpsEph), 
%
% outputs:
%   xyzM = [i,j,k] matrix of coordinates of satellites (ecef meters)
%   dtsvS = vector of satellite clock error (seconds)
% The row dimension of xyzM, dtsvS = length(gpsEph)
%
% xyzM = sat positions at time ttxSec, in terms of ecef coords at the same time
% Use FlightTimeCorrection.m to get xyzM & v in ecef coords at time of reception
%
% See  IS-GPS-200 for details of data

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements

xyzM=[]; dtsvS=[]; 

[bOk,gpsEph,gpsWeek,ttxSec] = CheckGpsEphInputs(gpsEph,gpsTime);
if ~bOk
    return
end
p=length(gpsEph);
%Now we are done checking and manipulating the inputs
% the time vectors: gpsWeek, ttxSec are the same length as gpsEph
%-------------------------------------------------------------------------------

%set fitIntervalSeconds
fitIntervalHours = [gpsEph.Fit_interval]';
%Rinex says "Zero if not known", so adjust for zeros
fitIntervalHours(fitIntervalHours == 0) = 2;
fitIntervalSeconds = fitIntervalHours*3600; 

%Extract variables from gpsEph, into column vectors
%orbit variable names follow RINEX 2.1 Table A4
%clock variables af0, af1, af2 follow IS GPS 200
TGD     = [gpsEph.TGD]';
Toc     = [gpsEph.Toc]';
af2     = [gpsEph.af2]';
af1     = [gpsEph.af1]';
af0     = [gpsEph.af0]';
Crs     = [gpsEph.Crs]';
Delta_n = [gpsEph.Delta_n]';
M0      = [gpsEph.M0]';
Cuc     = [gpsEph.Cuc]';
e       = [gpsEph.e]';
Cus     = [gpsEph.Cus]';
Asqrt   = [gpsEph.Asqrt]';
Toe     = [gpsEph.Toe]';
Cic     = [gpsEph.Cic]';
OMEGA   = [gpsEph.OMEGA]';
Cis     = [gpsEph.Cis]';
i0      = [gpsEph.i0]';
Crc     = [gpsEph.Crc]';
omega   = [gpsEph.omega]';
OMEGA_DOT=[gpsEph.OMEGA_DOT]';  
IDOT    = [gpsEph.IDOT]';
ephGpsWeek = [gpsEph.GPS_Week]';
    
%Calculate dependent variables -------------------------------------------------

%Time since time of applicability accounting for weeks and therefore rollovers
%subtract weeks first, to avoid precision errors:
tk =(gpsWeek-ephGpsWeek)*GpsConstants.WEEKSEC + (ttxSec-Toe);

I = find(abs(tk)>fitIntervalSeconds);
if ~isempty(I)
    numTimes = length(I);
    fprintf(sprintf('WARNING in GpsEph2Xyz.m, %d times outside fit interval.',...
        numTimes));
end

A = Asqrt.^2;    %semi-major axis of orbit
n0=sqrt(GpsConstants.mu./(A.^3));    %Computed mean motion (rad/sec)
n=n0+Delta_n;      %Corrected Mean Motion
h = sqrt(A.*(1-e.^2).*GpsConstants.mu); 
Mk=M0+n.*tk;     %Mean Anomaly
Ek=Kepler(Mk,e);  %Solve Kepler's equation for eccentric anomaly

%Calculate satellite clock bias (See ICD-GPS-200 20.3.3.3.3.1)
%subtract weeks first, to avoid precision errors:
dt =(gpsWeek-ephGpsWeek)*GpsConstants.WEEKSEC + (ttxSec-Toc);

%Calculate satellite clock bias
sin_Ek=sin(Ek);
cos_Ek=cos(Ek);
dtsvS = af0 + af1.*dt + af2.*(dt.^2)  + ...
    GpsConstants.FREL.*e.*Asqrt.*sin_Ek -TGD;

%true anomaly:
vk=atan2(sqrt(1-e.^2).*sin_Ek./(1-e.*cos_Ek),(cos_Ek-e)./(1-e.*cos_Ek));
Phik=vk + omega;   %Argument of latitude          

sin_2Phik=sin(2*Phik);
cos_2Phik=cos(2*Phik);
% The next three terms are the second harmonic perturbations
duk = Cus.*sin_2Phik + Cuc.*cos_2Phik;   %Argument of latitude correction
drk = Crc.*cos_2Phik + Crs.*sin_2Phik;   %Radius Correction
dik = Cic.*cos_2Phik + Cis.*sin_2Phik;   %Correction to Inclination

uk = Phik + duk;  %Corrected argument of latitude
rk = A.*((1-e.^2)./(1+e.*cos(vk))) + drk; %Corrected radius
ik = i0 + IDOT.*tk + dik; %Corrected inclination

sin_uk=sin(uk);
cos_uk=cos(uk);
xkp = rk.*cos_uk; % Position in orbital plane
ykp = rk.*sin_uk; % Position in orbital plane

% Wk = corrected longitude of ascending node, 
Wk  = OMEGA + (OMEGA_DOT - GpsConstants.WE).*tk - GpsConstants.WE*[gpsEph.Toe]';

%for dtflight, see FlightTimeCorrection.m
sin_Wk = sin(Wk);
cos_Wk = cos(Wk);

xyzM=zeros(p,3);
% The matrix xyzM contains the ECEF coordinates of position
sin_ik=sin(ik);
cos_ik=cos(ik);
xyzM(:,1)=xkp.*cos_Wk-ykp.*cos_ik.*sin_Wk;
xyzM(:,2)=xkp.*sin_Wk+ykp.*cos_ik.*cos_Wk;
xyzM(:,3)=ykp.*sin_ik;                                       

end %end of function GpsEph2Xyz
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


