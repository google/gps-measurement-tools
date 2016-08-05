function [gnssMeas]= ProcessAdr(gnssMeas)
% [gnssMeas]= ProcessAdr(gnssMeas)
% process the Accumulated Delta Ranges obtained from ProcessGnssMeas
%
%gnssMeas.FctSeconds = Nx1 vector. Rx time tag of measurements.
%        .ClkDCount  = Nx1 vector. Hw clock discontinuity count
%        .Svid       = 1xM vector of all svIds found in gnssRaw.
%        ...
%        .PrM        = NxM pseudoranges, row i corresponds to FctSeconds(i)
%        .DelPrM     = NxM change in pr while clock continuous
%        .AdrM       = NxM accumulated delta range (= -k*carrier phase) 
%        ...
%
% output:
% gnssMeas.DelPrMinusAdrM = NxM DelPrM - AdrM, re-initialized to zero at each
%   discontinuity or reset of DelPrM or AdrM

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements

if ~any(any(isfinite(gnssMeas.AdrM) & gnssMeas.AdrM~=0))
    %Nothing in AdrM but NaNs and zeros
    fprintf(' No ADR recorded\n'), return
end

M = length(gnssMeas.Svid);
N = length(gnssMeas.FctSeconds);
DelPrMinusAdrM = zeros(N,M)+NaN;
for j=1:M %loop over Svid
    AdrM    = gnssMeas.AdrM(:,j); %make local variables for readability
    DelPrM  = gnssMeas.DelPrM(:,j);
    AdrState = gnssMeas.AdrState(:,j);
    %From gps.h:
    %/* However, it is expected that the data is only accurate when:
    % *  'accumulated delta range state' == GPS_ADR_STATE_VALID.
    %*/
    % #define GPS_ADR_STATE_UNKNOWN                       0
    % #define GPS_ADR_STATE_VALID                     (1<<0)
    % #define GPS_ADR_STATE_RESET                     (1<<1)
    % #define GPS_ADR_STATE_CYCLE_SLIP                (1<<2)
    
    %keep valid values of AdrM only
    iValid = bitand(AdrState,2^0);
    iReset = bitand(AdrState,2^1);
    AdrM(~iValid) = NaN;
    
    %% work out DelPrM - AdrM since last discontinuity, plot DelPrM-AdrM
    
    DelPrM0 = NaN; %to store initial offset from AdrM
    for i=1:N %loop over time
        if isfinite(AdrM(i)) && (AdrM(i)~=0) && isfinite(DelPrM(i)) && ...
                ~iReset(i)
            %reinitialize after NaNs or AdrM zero or AdrState reset
            if isnan(DelPrM0)
                DelPrM0 = DelPrM(i) - AdrM(i);
            end
        else %reset at NaNs or AdrM zero
            DelPrM0 = NaN;
        end
        DelPrMinusAdrM(i,j) = DelPrM(i) - DelPrM0 - AdrM(i);
    end
end
    
gnssMeas.DelPrMinusAdrM = DelPrMinusAdrM;

end %end of function ProcessAdr
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
