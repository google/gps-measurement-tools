function [gpsTime,fctSeconds] = Utc2Gps(utcTime)
% [gpsTime,fctSeconds] = Utc2Gps(utcTime)
%   Convert the UTC date and time to GPS week & seconds
% 
% Inputs: 
%  utcTime: [mx6] matrix
%    utcTime(i,:) = [year,month,day,hours,minutes,seconds]
%      year must be specified using four digits, e.g. 1994
%    year valid range: 1980 <= year <= 2099
%
% Outputs: 
%  gpsTime: [mx2] matrix [gpsWeek, gpsSeconds], 
%    gpsWeek = number of weeks since GPS epoch
%    gpsSeconds  = number of seconds into gpsWeek, 
% fctSeconds: full cycle time = seconds since GPS epoch (1980/01/06 00:00 UTC)

% Other functions needed: JulianDay.m, LeapSeconds.m

%initialize outputs
gpsTime=[];
fctSeconds=[];

[bOk]=CheckUtcTimeInputs(utcTime);
if ~bOk
    return
end

HOURSEC = 3600; MINSEC = 60;
daysSinceEpoch = floor(JulianDay(utcTime) - GpsConstants.GPSEPOCHJD);

gpsWeek = fix(daysSinceEpoch/7);
dayofweek = rem(daysSinceEpoch,7);
% calculate the number of seconds since Sunday at midnight:
gpsSeconds = dayofweek*GpsConstants.DAYSEC + utcTime(:,4)*HOURSEC + ...
		utcTime(:,5)*MINSEC + utcTime(:,6);
gpsWeek = gpsWeek + fix(gpsSeconds/GpsConstants.WEEKSEC);
gpsSeconds = rem(gpsSeconds,GpsConstants.WEEKSEC);

% now add leapseconds
leapSecs = LeapSeconds(utcTime);
fctSeconds = gpsWeek(:)*GpsConstants.WEEKSEC + gpsSeconds(:) + leapSecs(:);
% when a leap second happens, utc time stands still for one second, 
% so gps seconds get further ahead, so we add leapsecs in going to gps time

gpsWeek = fix(fctSeconds/GpsConstants.WEEKSEC);
iZ  = gpsWeek==0; 
gpsSeconds(iZ) = fctSeconds(iZ); %set gpsSeconds directly, because rem(x,0)=NaN
gpsSeconds(~iZ) = rem(fctSeconds(~iZ),gpsWeek(~iZ)*GpsConstants.WEEKSEC);

gpsTime=[gpsWeek,gpsSeconds];
assert(all(fctSeconds==gpsWeek*GpsConstants.WEEKSEC+gpsSeconds),...
    'Error in computing gpsWeek, gpsSeconds');

end %end of function Utc2Gps
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function [bOk] = CheckUtcTimeInputs(utcTime)
%utility function for Utc2Gps

%check inputs
if size(utcTime,2)~=6
    error('utcTime must have 6 columns')
end

%check that year, month & day are integers
x = utcTime(:,1:3);
if any(any( (x-fix(x)) ~= 0 ))
  error('year,month & day must be integers')
end

%check that year is in valid range
if ( any(utcTime(:,1)<1980) || any(utcTime(:,1)>2099) )
  error('year must have values in the range: [1980:2099]')
end

%check validity of month, day and time
if (any(utcTime(:,2))<1 || any(utcTime(:,2))>12 )
  error('The month in utcTime must be a number in the set [1:12]')
end
if (any(utcTime(:,3)<1) || any(utcTime(:,3)>31))
  error('The day in utcTime must be a number in the set [1:31]')
end
if (any(utcTime(:,4)<0) || any(utcTime(:,4)>=24))
  error('The hour in utcTime must be in the range [0,24)')
end
if (any(utcTime(:,5)<0) || any(utcTime(:,5)>=60))
  error('The minutes in utcTime must be in the range [0,60)')
end
if (any(utcTime(:,6)<0) || any(utcTime(:,6)>60))
  %Note: seconds can equal 60 exactly, on the second a leap second is added
  error('The seconds in utcTime must be in the range [0,60]')
end

bOk = true;

end %end of function CheckUtcTimeInputs
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
