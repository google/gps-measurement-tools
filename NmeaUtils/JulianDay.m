function jDays = JulianDay(utcTime)
% jDays = JulianDay(utcTime);
% 
% input: utcTime [mx6] matrix [year,month,day,hours,minutes,seconds]
%
% output: totalDays in Julian Days [mx1] vector (real number of days)
%
% Valid input range: 1900 < year < 2100

%Algorithm from Meeus, (1991) Astronomical Algorithms, 
%see http://www.geoastro.de/elevaz/basics/meeus.htm for online summary
% valid range 1900/3/1 to 2100/2/28
% but we limit inputs to 1901 through 2099, because it's simpler

%Author: Frank van Diggelen

% check inputs
assert(size(utcTime,2) == 6,...
    'utcTime does not have 6 columns, should never happen');

y = utcTime(:,1);
m = utcTime(:,2);
d = utcTime(:,3);
h = utcTime(:,4) + utcTime(:,5)/60 + utcTime(:,6)/3600;

%check that date is in valid range
assert(all(y >= 1901) && all(y <= 2099),...
    'utcTime(:,1) not in allowed range: 1900 < year < 2100, should never happen');

i2 = m<=2; %index into months <=2
m(i2) = m(i2)+12;
y(i2) = y(i2)-1;

jDays = floor(365.25*y) + floor(30.6001*(m+1)) - 15 + 1720996.5 + d + h/24;

end %end of function JulianDay
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

