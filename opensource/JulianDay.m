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
%Open Source code for processing Android GNSS Measurements

% check inputs
if size(utcTime,2)~=6
  error('utcTime must have 6 columns')
end
y = utcTime(:,1);
m = utcTime(:,2);
d = utcTime(:,3);
h = utcTime(:,4) + utcTime(:,5)/60 + utcTime(:,5)/3600;

%check that date is in valid range
if ( any(y<1901) || any (y>2099) )
  error('utcTime(:,1) not in allowed range: 1900 < year < 2100')
end

i2 = m<=2; %index into months <=2
m(i2) = m(i2)+12;
y(i2) = y(i2)-1;

jDays = floor(365.25*y) + floor(30.6001*(m+1)) - 15 + 1720996.5 + d + h/24;

end %end of function JulianDay
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

