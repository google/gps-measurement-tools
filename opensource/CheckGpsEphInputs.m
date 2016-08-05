function [bOk,gpsEph,gpsWeek,ttxSec] = CheckGpsEphInputs(gpsEph,gpsTime)
%[bOk,gpsEph,gpsWeek,ttxSec] = CheckGpsEphInputs(gpsEph,gpsTime)
%check the inputs for GpsEph2Pvt, GpsEph2Xyz, GpsEph2Dtsv

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements

bOk=false;
if ~isstruct(gpsEph)
    error('gpsEph input must be a structure, as defined by ReadRinexNav')
end

p=length(gpsEph);
%Check that gpsTime is a px2 vector
if any(size(gpsTime) ~= [p 2])
    error('gpsTime must be px2 [gpsWeek, gpsSec], where p =length(gpsEph)')
end
gpsWeek = gpsTime(:,1);
ttxSec  = gpsTime(:,2);

bOk = true;
end %end of function CheckGpsEphInputs
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
