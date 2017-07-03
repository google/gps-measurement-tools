function [utcTime] = Gps2Utc(gpsTime,fctSeconds)
% [utcTime] = Gps2Utc(gpsTime,[fctSeconds])
%   Convert GPS time (week & seconds), or Full Cycle Time (seconds) to UTC
% 
% Input: gpsTime, [mx2] matrix [gpsWeek, gpsSeconds], 
%        fctSeconds, [optional] Full Cycle Time (seconds)
%
% Outputs: utcTime, [mx6] matrix = [year,month,day,hours,minutes,seconds]
% 
% If fctSeconds is provided, gpsTime is ignored
%
% Valid range of inputs: 
%   gps times corresponding to 1980/6/1 <= time < 2100/1/1
%   i.e. [0,0] <= gpsTime < [6260, 432000]
%        0 <= fctSeconds < 3786480000
%
% See also: Utc2Gps

% Other functions needed: LeapSeconds.m

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements

%Algorithm for handling leap seconds:
% When a leap second happens, utc time stands still for one second, so
% gps seconds get further ahead, so we subtract leapSecs to move from gps time
%
% 1) convert gpsTime to time = [yyyy,mm,dd,hh,mm,ss] (with no leap seconds)
% 2) look up leap seconds for time: ls = LeapSeconds(time);
%    This is (usually) the correct leap second value. Unless:
%      If (utcTime=time) and (utcTime=time+ls) straddle a leap second
%      then we need to add 1 to ls
% So, after step 2) ...
% 3) convert gpsTime-ls to timeMLs
% 4) look up leap seconds: ls1 = LeapSeconds(timeMLs);
% 5) if ls1~=ls, convert (gpsTime-ls1) to UTC Time

%% Check inputs
if nargin<2 && size(gpsTime,2)~=2
    error('gpsTime must have two columns')
end
if nargin<2
    fctSeconds = gpsTime*[GpsConstants.WEEKSEC; 1];
end

%fct at 2100/1/1 00:00:00, not counting leap seconds:
fct2100 =  [6260, 432000]*[GpsConstants.WEEKSEC; 1];
if any(fctSeconds<0) || any (fctSeconds >= fct2100)
  error('gpsTime must be in this range: [0,0] <= gpsTime < [6260, 432000]');
end
%% Finished checks

%from now on work with fct
%% Apply algorithm for handling leaps seconds
% 1) convert gpsTime to time = [yyyy,mm,dd,hh,mm,ss] (with no leap seconds)
time = Fct2Ymdhms(fctSeconds);
% 2) look up leap seconds for time: ls = LeapSeconds(time);
ls = LeapSeconds(time);
% 3) convert gpsTime-ls to timeMLs
timeMLs = Fct2Ymdhms(fctSeconds-ls);
% 4) look up leap seconds: ls1 = LeapSeconds(timeMLs);
ls1 = LeapSeconds(timeMLs);
% 5) if ls1~=ls, convert (gpsTime-ls1) to UTC Time
if all(ls1==ls)
    utcTime = timeMLs;
else
    utcTime = Fct2Ymdhms(fctSeconds-ls1);
end

% NOTE:
% Gps2Utc.m doesn't produce 23:59:60, at a leap second.
% Instead, as the leap second occurs, the Gps2Utc.m sequence of 
% UTC hh:mm:ss is  23:59:59, 00:00:00, 00:00:00
% and we keep it like that for code simplicity.
% Here are a sequence of UTC and GPS times around a leap second:
% formalUtcTimes = [1981 12 31 23 59 59; 1981 12 31 23 59 60; 1982 1 1 0 0 0]; 
% gpsTimes = [103 431999; 103 432000; 103 432001];
% >> Gps2Utc(gpsTimes)
% ans =
%         1981          12          31          23          59          59
%         1982           1           1           0           0           0
%         1982           1           1           0           0           0

% If you want to change this you could check LeapSeconds.m to see if you're
% exactly on the addition of a leap second, and then change the UTC format
% to include the '60' seconds

end %end of function Gps2Utc
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function time = Fct2Ymdhms(fctSeconds)
%Utility function for Gps2Utc
%Convert GPS full cycle time to [yyyy,mm,dd,hh,mm,ss.s] format
HOURSEC = 3600; MINSEC = 60;
monthDays=[31,28,31,30,31,30,31,31,30,31,30,31];%days each month (not leap year)

m=length(fctSeconds);
days = floor(fctSeconds / GpsConstants.DAYSEC) + 6;%days since 1980/1/1
years=zeros(m,1)+1980;
%decrement days by a year at a time, until we have calculated the year:
leap=ones(m,1); %1980 was a leap year
while (any(days > (leap+365)))
  I = find(days > (leap+365) );
  days(I) = days(I) - (leap(I) + 365);
  years(I)=years(I)+1;
  leap(I) = (rem(years(I),4) == 0); %  leap = 1 on a leap year, 0 otherwise
  % This works from 1901 till 2099, 2100 isn't a leap year (2000 is).
  % Calculate the year, ie time(1)
end, 
time=zeros(m,6);%initialize matrix
time(:,1)=years;

%decrement days by a month at a time, until we have calculated the month
% Calculate the month, ie time(:,2)
% Loop through m:
for i=1:m
  month = 1;
  if (rem(years(i),4) == 0)  %This works from 1901 till 2099
    monthDays(2)=29; %Make February have 29 days in a leap year
  else
    monthDays(2)=28;
  end
  while (days(i) > monthDays(month))
    days(i) = days(i)-monthDays(month);
    month = month+1;
  end
  time(i,2)=month;
end
  
time(:,3) = days;

sinceMidnightSeconds = rem(fctSeconds, GpsConstants.DAYSEC);
time(:,4) = fix(sinceMidnightSeconds/HOURSEC);
lastHourSeconds = rem(sinceMidnightSeconds, HOURSEC);
time(:,5) = fix(lastHourSeconds/MINSEC);
time(:,6) = rem(lastHourSeconds,MINSEC);

end %end of function Fct2Ymdhms
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

