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
if any(size(latDeg)~=[1,1]) || any(size(lonDeg)~=[1,1])
    error('Inputs latDeg, lonDeg must be scalars')
end

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

