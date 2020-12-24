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
assert(size(llaDegDegM,2) == 3,...
    'Input llaDegDegM does not have three columns, should never happen');

latDeg = llaDegDegM(:,1); lonDeg = llaDegDegM(:,2); altM = llaDegDegM(:,3);
%No rotation of longitude, by definition of ECEF

% Compute sines and cosines.
D2R = pi/180;
clat = cos(latDeg*D2R);
clon = cos(lonDeg*D2R);
slat = sin(latDeg*D2R);
slon = sin(lonDeg*D2R);

% Compute position vector in ECEF coordinates.
EARTHECCEN2 = 6.69437999014e-3; %WGS 84 (Earth eccentricity)^2 (m^2)
EARTHSEMIMAJOR = 6378137; %WGS 84 Earth semi-major axis (m)
r0 = EARTHSEMIMAJOR * ...
    (sqrt(1.0 - EARTHECCEN2 .* slat .* slat)).^(-1);
xM = (altM + r0) .* clat .* clon;                           % x coordinate
yM = (altM + r0) .* clat .* slon;                           % y coordinate
zM = (altM + r0 .* (1.0 - EARTHECCEN2)).* slat;% z coordinate

[xyzM] = [xM,yM,zM];

end %end of function Lla2Xyz
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

