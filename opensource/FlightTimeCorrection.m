function xERot = FlightTimeCorrection(xE, dTflightSeconds)
%xERot = FlightTimeCorrection(xE, dtflight);
% Compute rotated satellite ECEF coordinates caused by Earth
% rotation during signal flight time
%
%   Inputs:
%       xE         - satellite ECEF position at time of transmission
%       dtflight   - signal flight time (seconds)
%
%   Outputs:
%       XeRot     - rotated satelite position vector (ECEF at trx)
%
% Reference: 
%  IS GPS 200, 20.3.3.4.3.3.2 Earth-Centered, Inertial (ECI) Coordinate System

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements
%See also "A software receiver for GPS and Galileo", K. Borre at al.

%Rotation angle (radians):
theta = GpsConstants.WE * dTflightSeconds;

%Apply rotation from IS GPS 200-E, 20.3.3.4.3.3.2
%Note: IS GPS 200-E shows the rotation from ecef to eci
% so our rotation R3, is in the opposite direction:
R3 = [ cos(theta)    sin(theta)   0;
      -sin(theta)    cos(theta)   0;
       0                0         1];

xERot  = (R3*xE(:))';

end %end of function FlightTimeCorrection
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

