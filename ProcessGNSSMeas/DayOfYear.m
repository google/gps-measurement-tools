function dayNumber = DayOfYear(utcTime)
%dayNumber = DayOfYear(utcTime);
%
% Return the day number of the year
%  utcTime: [1x6] [year,month,day,hours,minutes,seconds]

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements

if any(size(utcTime)~=[1,6])
    error('utcTime must be 1x6 for DayOfYear function')
end

jDay = JulianDay([utcTime(1:3),0,0,0]);%midnight morning of this day
jDayJan1   = JulianDay([utcTime(1),1,1,0,0,0]);%midnight morning of Jan 1st
dayNumber = jDay - jDayJan1 + 1;

end %end of function DayOfYear
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

