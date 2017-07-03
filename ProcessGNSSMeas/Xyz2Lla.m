function [llaDegDegM] = Xyz2Lla(xyzM)
% [llaDegDegM] = Xyz2Lla(xyzM)
%
% Transform Earth-Centered-Earth-Fixed x,y,z, coordinates to lat,lon,alt. 
% Input:    xyzM = [mx3] matrix of ECEF coordinates in meters
% Output:   llaDegDegM = [mx3] matrix = [latDeg,lonDeg,altM]
% latitude, longitude are returned in degrees and altitude in meters
%
% See also Lla2Xyz

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements

% check inputs
if size(xyzM,2)~=3
    error('Input xyzM must have three columns');
end
% algorithm: Hoffman-Wellenhof, Lichtenegger & Collins "GPS Theory & Practice"
R2D = 180/pi;

%if x and y ecef positions are both zero then lla is undefined
iZero = ( xyzM(:,1)==0 & xyzM(:,2)==0);
xyzM(iZero,:) = NaN; %set to NaN, so lla will also be NaN

xM = xyzM(:,1); yM = xyzM(:,2); zM = xyzM(:,3);
%following algorithm from Hoffman-Wellenhof, et al. "GPS Theory & Practice":
a = GpsConstants.EARTHSEMIMAJOR;
a2 = a^2;
b2 = a2*(1-GpsConstants.EARTHECCEN2);
b = sqrt(b2);
e2 = GpsConstants.EARTHECCEN2;
ep2 = (a2-b2)/b2;
p=sqrt(xM.^2 + yM.^2);

% two sides and hypotenuse of right angle triangle with one angle = theta:
s1 = zM*a;
s2 = p*b;
h = sqrt(s1.^2 + s2.^2);
sin_theta = s1./h;
cos_theta = s2./h;
%theta = atan(s1./s2);

% two sides and hypotenuse of right angle triangle with one angle = lat:
s1 = zM+ep2*b*(sin_theta.^3);
s2 = p-a*e2.*(cos_theta.^3);
h = sqrt(s1.^2 + s2.^2);
tan_lat = s1./s2;
sin_lat = s1./h;
cos_lat = s2./h;
latDeg = atan(tan_lat);
latDeg = latDeg*R2D;

N = a2*((a2*(cos_lat.^2) + b2*(sin_lat.^2)).^(-0.5));
altM = p./cos_lat - N;

% rotate longitude to where it would be for a fixed point in ECI
%lonDeg = atan2(yM,xM) - GpsConstants.WE*deltaTime;
lonDeg = atan2(yM,xM); %since deltaTime = 0 for ECEF
lonDeg = rem(lonDeg,2*pi)*R2D;
if (lonDeg>180)
    lonDeg = lonDeg-360;
end
llaDegDegM = [latDeg, lonDeg,altM];

end %end of function Xyz2Lla
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

