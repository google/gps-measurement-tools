function [gpsTime,fctSeconds,leapSecs] = Utc2Gps(utcTime)
% [gpsTime,fctSeconds,leapSecs] = Utc2Gps(utcTime)
%   Convert the UTC date and time to GPS week & seconds
%
% Inputs
%  utcTime: [mx6] matrix
%    utcTime(i,:) = [year,month,day,hours,minutes,seconds]
%      year must be specified using four digits, e.g. 1994
%    year valid range: 1980 <= year <= 2099
%
% Outputs
%  gpsTime: [mx2] matrix [gpsWeek, gpsSeconds],
%    gpsWeek = number of weeks since GPS epoch
%    gpsSeconds  = number of seconds into gpsWeek,
%  fctSeconds: full cycle time = seconds since GPS epoch (1980/01/06 00:00 UTC)
%  leapSecs: number of leap seconds since GPS epoch

% Other functions needed: JulianDay.m, LeapSeconds.m

%initialize outputs
gpsTime=[];
fctSeconds=[];

[bOk]=CheckUtcTimeInputs(utcTime);
if ~bOk
    return
end

HOURSEC = 3600; MINSEC = 60;
GPSEPOCHJD = 2444244.5; %GPS Epoch in Julian Days
daysSinceEpoch = floor(JulianDay(utcTime) - GPSEPOCHJD);
%TBD add test vector close to JulianDay = integer day, and see if few seconds
%  difference in JulianDay matters

gpsWeek = fix(daysSinceEpoch/7);
dayofweek = rem(daysSinceEpoch,7);
% calculate the number of seconds since Sunday at midnight:
DAYSEC = 86400; %number of seconds in a day
WEEKSEC = 604800; %number of seconds in a week
gpsSeconds = dayofweek*DAYSEC + utcTime(:,4)*HOURSEC + ...
    utcTime(:,5)*MINSEC + utcTime(:,6);
gpsWeek = gpsWeek + fix(gpsSeconds/WEEKSEC);
gpsSeconds = rem(gpsSeconds,WEEKSEC);

% now add leapseconds
leapSecs = LeapSeconds(utcTime);
fctSeconds = gpsWeek(:)*WEEKSEC + gpsSeconds(:) + leapSecs(:);
% when a leap second happens, utc time stands still for one second,
% so gps seconds get further ahead, so we add leapsecs in going to gps time

gpsWeek = fix(fctSeconds/WEEKSEC);
iZ  = gpsWeek==0;
gpsSeconds(iZ) = fctSeconds(iZ); %set gpsSeconds directly, because rem(x,0)=NaN
gpsSeconds(~iZ) = rem(fctSeconds(~iZ),gpsWeek(~iZ)*WEEKSEC);

gpsTime=[gpsWeek,gpsSeconds];
assert(all(fctSeconds==gpsWeek*WEEKSEC+gpsSeconds),...
    'Error in computing gpsWeek, gpsSeconds');

end %end of function Utc2Gps
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
