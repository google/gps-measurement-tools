function Re2n = RotEcef2Ned(latDeg, lonDeg)
% Re2n = RotEcef2Ned(latDeg, lonDeg)
% Rotation matrix to convert an ECEF vector to 
% North, East, Down coordinates, and vice-versa
%
% inputs: latDeg, lonDeg (degrees)
% output: Re2n,   3x3 unitary rotation matrix = 
%              [-sin(lat)*cos(lon), -sin(lat)*sin(lon),  cos(lat);
%               -sin(lon),           cos(lon),           0       ;
%               -cos(lat)*cos(lon),  -cos(lat)*sin(lon),-sin(lat)]
%
% Example:   vNed = Re2n*vEcef, 
%      Re2n'*vNed = vEcef

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements

%CHECK INPUTS
assert(all(size(latDeg) == [1,1]),...
    'Input latDeg are not scalar, should never happen');
assert(all(size(lonDeg) == [1,1]),...
    'Input lonDeg are not scalar, should never happen');

D2R = pi/180; %degrees to radians scale factor
latRad=D2R*latDeg(:); lonRad=D2R*lonDeg(:);

clat = cos(latRad);
slat = sin(latRad);
clon = cos(lonRad);
slon = sin(lonRad);

Re2n = zeros(3,3);
Re2n(1,1) = -slat.*clon;
Re2n(1,2) = -slat.*slon;
Re2n(1,3) = clat;

Re2n(2,1) = -slon;
Re2n(2,2) = clon;
Re2n(2,3) = 0;

Re2n(3,1) = -clat.*clon;
Re2n(3,2) = -clat.*slon;
Re2n(3,3) = -slat;

end %end of function RotEcef2Ned
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
