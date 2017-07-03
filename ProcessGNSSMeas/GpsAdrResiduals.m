function [adrResid]= GpsAdrResiduals(gnssMeas,allGpsEph,llaDegDegM)
% [adrResid]= GpsAdrResiduals(gnssMeas,allGpsEph,llaDegDegM)
% Compute residuals from GPS Accumulated Delta Ranges
%
% Inputs:
% gnssMeas.FctSeconds = Nx1 vector. Rx time tag of measurements.
%         .ClkDCount  = Nx1 vector. Hw clock discontinuity count
%         .Svid       = 1xM vector of all svIds found in gnssRaw.
%         ...
%         .tRxSeconds = NxM time of reception, seconds of gps week
%         .tTxSeconds = NxM time of tranmission, seconds of gps week
%         .AdrM       = NxM accumulated delta range (= -k*carrier phase) 
%         ...
%
% allGpsEph, structure with all ephemeris
% llaDegDegM [1x3] true position
%
% Output: 
% adrResid.FctSeconds = Nx1 time vector, same as gnssMeas.FctSeconds
%         .Svid0      = reference satellite for single differences
%         .Svid       = 1xM vector of all svid
%         .ResidM     = [NxM] adr residuals
%
%Algorithm: compute single difference from sv to reference satellite svid0, then
% diff from reference time: tk - t0 (where t0 is the first common epoch for 
% sv &  svid0), then subtract expected values

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements

adrResid = [];
if ~any(any(isfinite(gnssMeas.AdrM) & gnssMeas.AdrM~=0))
    %Nothing in AdrM but NaNs and zeros
    return
end
if nargin<3 || isempty(llaDegDegM)
    fprintf('GpsAdrResiduals needs the true position: llaDegDegM\n')
    return
end
xyz0M = Lla2Xyz(llaDegDegM);

M = length(gnssMeas.Svid);
N = length(gnssMeas.FctSeconds);

weekNum     = floor(gnssMeas.FctSeconds/GpsConstants.WEEKSEC);

adrResid.FctSeconds = gnssMeas.FctSeconds;
adrResid.Svid0      = [];
adrResid.Svid       = gnssMeas.Svid;
adrResid.ResidM     = zeros(N,M)+NaN;

%From gps.h:
%/* However, it is expected that the data is only accurate when:
% *  'accumulated delta range state' == GPS_ADR_STATE_VALID.
%*/
% #define GPS_ADR_STATE_UNKNOWN                       0
% #define GPS_ADR_STATE_VALID                     (1<<0)
% #define GPS_ADR_STATE_RESET                     (1<<1)
% #define GPS_ADR_STATE_CYCLE_SLIP                (1<<2)

%choose Svid0 as the satellite that has most valid adr
numValidAdr = zeros(1,M);
for j=1:M
    numValidAdr(j) = length(find(bitand(gnssMeas.AdrState(:,j),2^0)));
end
[~,j0] = max(numValidAdr);
adrResid.Svid0 = gnssMeas.Svid(j0);
svid = gnssMeas.Svid;

%% Compute expected pseudoranges
prHatM = zeros(N,M)+NaN; %to store expected pseudoranges
%"pseudo" here refers to the clock error in the satellite, not the receiver
%compute expected pr at each epoch
for i=1:N
    for j=1:M
        ttxSeconds = gnssMeas.tTxSeconds(i,j);
        if isnan(ttxSeconds)
            continue %skip to next
        end
        [gpsEph,iSv]= ClosestGpsEph(allGpsEph,svid(j),gnssMeas.FctSeconds(i));
        if isempty(iSv)
            continue; %skip to next
        end
        %compute pr for this sv
        dtsv = GpsEph2Dtsv(gpsEph,ttxSeconds);
        ttxSeconds = ttxSeconds - dtsv;%subtract dtsv from sv time to get true gps time
        
        %calculate satellite position at ttx:
        [svXyzTtxM,dtsv]=GpsEph2Xyz(gpsEph,[weekNum(i),ttxSeconds]);
        %in ECEF coordinates at trx:
        dtflightSeconds = norm(xyz0M - svXyzTtxM)/GpsConstants.LIGHTSPEED;
        svXyzTrxM = FlightTimeCorrection(svXyzTtxM, dtflightSeconds);
        prHatM(i,j) = norm(xyz0M - svXyzTrxM) - GpsConstants.LIGHTSPEED*dtsv;
        % Use of dtsv: dtsv>0 <=> pr too small
    end
end

%% Compute single difference, then delta from t0, then residuals
iT0 = zeros(1,M); %to store common start index for svid and svid0
for i=1:N %loop over time
    if ~bitand(gnssMeas.AdrState(i,j0),2^0)
        continue; %skip to next epoch
    end
    %now we have valid Adr for reference satellite, svid(j0) at this epoch
    for j=1:M %loop over svid
        if j==j0
            continue; %skip over reference satellite
        end
        if ~bitand(gnssMeas.AdrState(i,j),2^0) %no valid adr for this svid
            iT0(j) = 0;%reset common start index for this svid
            continue %skip to next svid
        elseif ~iT0(j) %we haven't yet set a common start, 
            if all(isfinite(prHatM(i,[j,j0]))) %we have valid prHat at this epoch
                iT0(j) = i;%set common start index
            end
        end
        %Now we have valid Adr for svid(j) and svid(j0)
        
        i0 = iT0(j); %common start index for svid(j) and svid(j0)
        if i>i0 && all(isfinite(prHatM(i,[j,j0])))%valid prHat at this epoch
            delAdrM = (gnssMeas.AdrM(i,j) - gnssMeas.AdrM(i,j0)) - ...
                (gnssMeas.AdrM(i0,j) - gnssMeas.AdrM(i0,j0));
            delPrHatM = (prHatM(i,j) - prHatM(i,j0)) - ...
                (prHatM(i0,j) - prHatM(i0,j0));
            adrResid.ResidM(i,j) = delAdrM - delPrHatM;
            %by all the above 'isfinite' checks, residM should not be NaN
            assert(isfinite(adrResid.ResidM(i,j)),...
                'Residual should be finite, check the above logic');
        end
    end
end

end %end of function GpsAdrResiduals
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
