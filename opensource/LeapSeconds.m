function [leapSecs] = LeapSeconds(utcTime)
% leapSecs = LeapSeconds(utcTime)
% find the number of leap seconds since the GPS Epoch
% 
%  utcTime: [mx6] matrix
%    utcTime(i,:) = [year,month,day,hours,minutes,seconds]
%      year must be specified using four digits, e.g. 1994
%      year valid range: 1980 <= year <= 2099
%
% Output: leapSecs,
%    leapSecs(i) = number of leap seconds between the GPS Epoch and utcTime(i,:)
%
% The function looks up the number of leap seconds from a UTC time table
%
% LATEST LEAP SECOND IN THE TABLE = 31 Dec 2016. 
% On 1 Jan 2017: GPS-UTC=18s
% See IERS Bulletin C, https://hpiers.obspm.fr/iers/bul/bulc/bulletinc.dat  
% and http://tycho.usno.navy.mil/leapsec.html
%
% Aren't Leap Seconds a pain? Yes, and very costly. Ban the Leap Second:
% Leap seconds occur on average once every two years.
% What would happen if we had no leap seconds: 
% 1) Thousand of engineers would NOT spend several weeks each two years fixing 
%    or planning for leap second bugs. 
% 2) About seven thousand years from now, solar time would be 1 hour behind UTC
%    and we would need a 1 hour adjustment - similar to daylight savings time.
% 3) GMT (which is solar time) will lose its significance.


%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements

[m,n] = size(utcTime);
assert(n == 6,...
    'utcTime input does not have 6 columns, should never happen');

% UTC table contains UTC times (in the form of [year,month,day,hours,mins,secs])
% At each of these times a leap second had just occurred
utcTable = [1982 1 1 0 0 0;
            1982 7 1 0 0 0;
            1983 7 1 0 0 0;
            1985 7 1 0 0 0;
            1988 1 1 0 0 0;
            1990 1 1 0 0 0;
            1991 1 1 0 0 0;
            1992 7 1 0 0 0;
            1993 7 1 0 0 0;
            1994 7 1 0 0 0;
            1996 1 1 0 0 0;
            1997 7 1 0 0 0;
            1999 1 1 0 0 0;
            2006 1 1 0 0 0;
            2009 1 1 0 0 0;
            2012 7 1 0 0 0;
            2015 7 1 0 0 0;
            2017 1 1 0 0 0
         ];
%when a new leap second is announced in IERS Bulletin C
%update the table with the UTC time right after the new leap second
% Latest from IERS Bulletin C (as of November 2019):
% "NO leap second will be introduced at the end of December 2019"

GPSEPOCHJD = 2444244.5; %GPS Epoch in Julian Days
tableJDays = JulianDay(utcTable) - GPSEPOCHJD; %days since GPS Epoch
%tableSeconds = tableJDays*GnssConstants.DAYSEC + utcTable(:,4:6)*[3600;60;1];
%NOTE: JulianDay returns a realed value number, corresponding to days and 
% fractions thereof, so we multiply it by DAYSEC to get the full time in seconds
DAYSEC = 86400; %number of seconds in a day
tableSeconds = tableJDays*DAYSEC;
jDays = JulianDay(utcTime)- GPSEPOCHJD; %days since GPS Epoch
%timeSeconds = jDays*GnssConstants.DAYSEC + utcTime(:,4:6)*[3600;60;1];
timeSeconds = jDays*DAYSEC;
% tableSeconds and timeSeconds now contain number of seconds since the GPS epoch

leapSecs=zeros(m,1);
for i=1:m
  %add up the number of leap seconds that have occured by timeSeconds(i)
  leapSecs(i) = sum(tableSeconds<=timeSeconds(i)); 
end

end %end of function LeapSeconds
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

