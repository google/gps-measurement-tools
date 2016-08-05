function [gpsEph,iSv] = ClosestGpsEph(allGpsEph,svIds,fctSeconds)
%[gpsEph,iSv] = ClosestGpsEph(allGpsEph,svIds,fctSeconds);
%find ephemeris in a GPS ephemeris structure allGpsEph for all svIds listed
%return gpsEph = unique ephemeris for svIds, with fctToe closest to fctSeconds,
% and |fctToe - fctSeconds| < fitInterval
%
%output: gpsEph, iSv
%        gpsEph = valid ephemeris corresponding to svIds(iSv)

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements

gpsEph = allGpsEph(1);%initialize gpsEph
numEph = 0;

%find all ephemeris corresponding to svIds(i)
for i=1:length(svIds)
    iThisSv = [allGpsEph.PRN] == svIds(i);
    if any(iThisSv)
        ephThisSv = allGpsEph(iThisSv);
        %find Toe within fit interval
        %set fit interval
        fitIntervalHours = [ephThisSv.Fit_interval];
        %Rinex says "Zero if not known", so adjust for zeros
        fitIntervalHours(fitIntervalHours == 0) = 4;
        %full cycle time of ephemeris Toe
        fctToe = [ephThisSv.GPS_Week]*GpsConstants.WEEKSEC + [ephThisSv.Toe];
        %find freshest Toe
        [ageSeconds, iMin] = min(abs(fctToe - fctSeconds));
        if ageSeconds < (fitIntervalHours/2)*3600;
            numEph = numEph+1;
            gpsEph(numEph) = ephThisSv(iMin);
            iSv(numEph) = i;%save index into svIds
        else
            fprintf('No valid ephemeris found for svId %d,', svIds(i))
            ageHours = (fctToe(iMin)-fctSeconds)/3600;
            fprintf(' closest Toe is %.1f hours away.\n',ageHours)
        end
    end
end

if numEph==0
    gpsEph=[];iSv=[];%return empty matrices
end
    

end %of function ClosestGpsEph
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


