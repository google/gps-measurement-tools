function [xyzM] = Lla2Xyz(llaDegDegM)
% [xyzM] = Lla2Xyz(llaDegDegM)
% Transform latitude, longitude, altitude to ECEF coordinates.
%
% input: llaDegDegM = [mx3] matrix = [latDeg,lonDeg,altM]
%        latitude, longitude are in degrees, altitude in meters
% output: xyzM = [mx3] matrix of ECEF coordinates in meters
%
% See also Xyz2Lla

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements

% check inputs
if size(llaDegDegM,2)~=3
    error('Input llaDegDegM must have three columns');
end

latDeg = llaDegDegM(:,1); lonDeg = llaDegDegM(:,2); altM = llaDegDegM(:,3);
%No rotation of longitude, by definition of ECEF

% Compute sines and cosines.
D2R = pi/180;
clat = cos(latDeg*D2R);
clon = cos(lonDeg*D2R);
slat = sin(latDeg*D2R);
slon = sin(lonDeg*D2R);

% Compute position vector in ECEF coordinates.
r0 = GpsConstants.EARTHSEMIMAJOR * ...
    (sqrt(1.0 - GpsConstants.EARTHECCEN2 .* slat .* slat)).^(-1);
xM = (altM + r0) .* clat .* clon;                           % x coordinate
yM = (altM + r0) .* clat .* slon;                           % y coordinate
zM = (altM + r0 .* (1.0 - GpsConstants.EARTHECCEN2)).* slat;% z coordinate

[xyzM] = [xM,yM,zM];

end %end of function Lla2Xyz
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

