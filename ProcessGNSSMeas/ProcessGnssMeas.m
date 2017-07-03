function gnssMeas = ProcessGnssMeas(gnssRaw)
% gnssMeas = ProcessGnssMeas(gnssRaw)
% Process raw measurements read from ReadGnssLogger
% Using technique explained in "Raw GNSS Measurements from Android" tutorial
%
% Input: gnssRaw, output from ReadGnssLogger
% Output: gnssMeas structure formatted conveniently for batch processing:
%gnssMeas.FctSeconds = Nx1 Full cycle time tag of M batched measurements.
%        .ClkDCount  = Nx1 Hw clock discontinuity count
%        .HwDscDelS  = Nx1 Hw clock change during each discontiuity (seconds)
%        .Svid       = 1xM all svIds found in gnssRaw.
%        .AzDeg      = 1xM azimuth in degrees at last valid epoch
%        .ElDeg      = 1xM elevation, ditto
%        .tRxSeconds = NxM time of reception, seconds of gps week
%        .tTxSeconds = NxM time of transmission, seconds of gps week
%        .PrM        = NxM pseudoranges, row i corresponds to FctSeconds(i)
%        .PrSigmaM   = NxM pseudorange error estimate (1-sigma)
%        .DelPrM     = NxM change in pr while clock continuous
%        .PrrMps     = NxM pseudorange rate 
%        .PrrSigmaMps= NxM
%        .AdrM       = NxM accumulated delta range (= -k * carrier phase) 
%        .AdrSigmaM  = NxM
%        .AdrState   = NxM
%        .Cn0DbHz    = NxM
%
% all gnssMeas values are doubles
%
% Az and El are returned NaN. Compute Az,El using ephemeris and lla,
% or read from NMEA or GnssStatus

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements

% Filter valid values first, so that rollover checks, etc, are on valid data
gnssRaw = FilterValid(gnssRaw);

%anything within 1ms is considered same epoch:
allRxMilliseconds = double(gnssRaw.allRxMillis);
gnssMeas.FctSeconds = (unique(allRxMilliseconds))*1e-3;
N = length(gnssMeas.FctSeconds);
gnssMeas.ClkDCount  = zeros(N,1);
gnssMeas.HwDscDelS  = zeros(N,1);
gnssMeas.Svid       = unique(gnssRaw.Svid)'; %all the sv ids found in gnssRaw
M = length(gnssMeas.Svid);
gnssMeas.AzDeg      = zeros(1,M)+NaN;
gnssMeas.ElDeg      = zeros(1,M)+NaN;
gnssMeas.tRxSeconds = zeros(N,M)+NaN; %time of reception, seconds of gps week
gnssMeas.tTxSeconds = zeros(N,M)+NaN; %time of transmission, seconds of gps week
gnssMeas.PrM        = zeros(N,M)+NaN;
gnssMeas.PrSigmaM   = zeros(N,M)+NaN;
gnssMeas.DelPrM     = zeros(N,M)+NaN;
gnssMeas.PrrMps     = zeros(N,M)+NaN;
gnssMeas.PrrSigmaMps= zeros(N,M)+NaN;
gnssMeas.AdrM       = zeros(N,M)+NaN;
gnssMeas.AdrSigmaM  = zeros(N,M)+NaN;
gnssMeas.AdrState   = zeros(N,M);
gnssMeas.Cn0DbHz    = zeros(N,M)+NaN;

%GPS Week number:
weekNumber = floor(-double(gnssRaw.FullBiasNanos)*1e-9/GpsConstants.WEEKSEC);

%check for fields that are commonly all zero and may be missing from gnssRaw
if ~isfield(gnssRaw,'BiasNanos')
    gnssRaw.BiasNanos = 0;
end
if ~isfield(gnssRaw,'TimeOffsetNanos')
    gnssRaw.TimeOffsetNanos = 0;
end

%compute time of measurement relative to start of week
%subtract big longs (i.e. time from 1980) before casting time of week as double
WEEKNANOS = int64(GpsConstants.WEEKSEC*1e9);
weekNumberNanos = int64(weekNumber)*int64(GpsConstants.WEEKSEC*1e9);

%compute tRxNanos using gnssRaw.FullBiasNanos(1), so that
% tRxNanos includes rx clock drift since the first epoch:
tRxNanos = gnssRaw.TimeNanos -gnssRaw.FullBiasNanos(1) - weekNumberNanos;

%Assert if Tow state ~=1, because then gnssRaw.FullBiasNanos(1) might be wrong
State = gnssRaw.State(1);
assert(bitand(State,2^0) &  bitand(State,2^3),...
  'gnssRaw.State(1) must have bits 0 and 3 true before calling ProcessGnssMeas')

%tRxNanos now since beginning of the week, unless we had a week rollover
%assert(all(tRxNanos <= WEEKNANOS),'tRxNanos should be <= WEEKNANOS')
%TBD check week rollover code, and add assert tRxNanos <= WEEKNANOS after
assert(all(tRxNanos >= 0),'tRxNanos should be >= 0')

%subtract the fractional offsets TimeOffsetNanos and BiasNanos:
tRxSeconds  = (double(tRxNanos)-gnssRaw.TimeOffsetNanos-gnssRaw.BiasNanos)*1e-9;
tTxSeconds  = double(gnssRaw.ReceivedSvTimeNanos)*1e-9;

%check for week rollover in tRxSeconds
[prSeconds,tRxSeconds]  = CheckGpsWeekRollover(tRxSeconds,tTxSeconds);
%we are ready to compute pseudorange in meters:
PrM         = prSeconds*GpsConstants.LIGHTSPEED;

PrSigmaM    = double(gnssRaw.ReceivedSvTimeUncertaintyNanos)*1e-9*...
    GpsConstants.LIGHTSPEED;
PrrMps      = gnssRaw.PseudorangeRateMetersPerSecond;
PrrSigmaMps = gnssRaw.PseudorangeRateUncertaintyMetersPerSecond;
AdrM        = gnssRaw.AccumulatedDeltaRangeMeters;
AdrSigmaM   = gnssRaw.AccumulatedDeltaRangeUncertaintyMeters;
AdrState    = gnssRaw.AccumulatedDeltaRangeState;
Cn0DbHz     = gnssRaw.Cn0DbHz;

%Now pack these vectors into the NxM matrices
for i=1:N %i is index into gnssMeas.FctSeconds and matrix rows
    %get index of measurements within 1ms of this time tag
    J = find(abs(gnssMeas.FctSeconds(i)*1e3 - allRxMilliseconds)<1); 
    for j=1:length(J) %J(j) is index into gnssRaw.*
        k = find(gnssMeas.Svid==gnssRaw.Svid(J(j)));
        %k is the index into gnssMeas.Svid and matrix columns
        gnssMeas.tRxSeconds(i,k) = tRxSeconds(J(j));
        gnssMeas.tTxSeconds(i,k) = tTxSeconds(J(j));
        gnssMeas.PrM(i,k)        = PrM(J(j));
        gnssMeas.PrSigmaM(i,k)   = PrSigmaM(J(j));
        gnssMeas.PrrMps(i,k)     = PrrMps(J(j));
        gnssMeas.PrrSigmaMps(i,k)= PrrSigmaMps(J(j));
        gnssMeas.AdrM(i,k)       = AdrM(J(j));
        gnssMeas.AdrSigmaM(i,k)  = AdrSigmaM(J(j));
        gnssMeas.AdrState(i,k)   = AdrState(J(j));
        gnssMeas.Cn0DbHz(i,k)    = Cn0DbHz(J(j));
    end
    %save the hw clock discontinuity count for this epoch:
    gnssMeas.ClkDCount(i) = gnssRaw.HardwareClockDiscontinuityCount(J(1));
    
    if gnssRaw.HardwareClockDiscontinuityCount(J(1)) ~= ...
            gnssRaw.HardwareClockDiscontinuityCount(J(end))
        error('HardwareClockDiscontinuityCount changed within the same epoch');
    end
end

gnssMeas = GetDelPr(gnssMeas);

end %of function ProcessGnssMeas
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function gnssRaw = FilterValid(gnssRaw)
%utility function for ProcessGnssMeas, 
%remove fields corresponding to measurements that are invalid
%NOTE: this makes it simpler to process data. But it removes data,
% so if you want to investigate *why* fields are invalid, then do so 
% before calling this function

%check ReceivedSvTimeUncertaintyNanos, PseudorangeRateUncertaintyMetersPerSecond
%for now keep only Svid with towUnc<0.5 microseconds and prrUnc < 10 mps
iTowUnc = gnssRaw.ReceivedSvTimeUncertaintyNanos > GnssThresholds.MAXTOWUNCNS;
iPrrUnc = gnssRaw.PseudorangeRateUncertaintyMetersPerSecond > ...
    GnssThresholds.MAXPRRUNCMPS;
iBad = iTowUnc | iPrrUnc;

if any(iBad)
    numBad = sum(iBad);
    %assert if we're about to remove everything:
    assert(numBad<length(iBad),'Removing all measurements in gnssRaw')
    
    names = fieldnames(gnssRaw);
    for i=1:length(names)
        ts = sprintf('gnssRaw.%s(iBad) = [];',names{i});
        eval(ts); %remove fields for invalid meas
    end
    %explain to user what happened:
    fprintf('\nRemoved %d bad meas inside ProcessGnssMeas>FilterValid because:\n',...
        sum(iBad))
    if any(iTowUnc)
        fprintf('towUnc > %.0f ns\n',GnssThresholds.MAXTOWUNCNS)
    end
    if any(iPrrUnc)
        fprintf('prrUnc > %.0f m/s\n',GnssThresholds.MAXPRRUNCMPS)
    end
end

end %end of function FilterValid
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function gnssMeas = GetDelPr(gnssMeas)
% utility function for ProcessGnssMeas, compute DelPr
% gnssMeas.DelPrM = NxM, change in pr while clock is continuous

N = length(gnssMeas.FctSeconds);
M = length(gnssMeas.Svid);

bClockDis = [0;diff(gnssMeas.ClkDCount)~=0];%binary, 1 <=> clock discontinuity

%initialize first epoch to zero (by definition), rest to NaN
delPrM = zeros(N,M); delPrM(2:end,:) = NaN; 

for j=1:M
    i0=1; %i0 = index from where we compute DelPr
    for i=2:N
        if bClockDis(i) || isnan(gnssMeas.PrM(i0,j))
            i0 = i; %reset to i if clock discont or a break in tracking
        end
        if bClockDis(i)
            delPrM(i,j) = NaN;
        else
            delPrM(i,j) = gnssMeas.PrM(i,j) - gnssMeas.PrM(i0,j);
        end
    end
end
gnssMeas.DelPrM = delPrM;
end %of function GetDelPr
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function [prSeconds,tRxSeconds]  = CheckGpsWeekRollover(tRxSeconds,tTxSeconds)
%utility function for ProcessGnssMeas

prSeconds  = tRxSeconds - tTxSeconds;

iRollover = prSeconds > GpsConstants.WEEKSEC/2;
if any(iRollover)
    fprintf('\nWARNING: week rollover detected in time tags. Adjusting ...\n')
    prS = prSeconds(iRollover);
    delS = round(prS/GpsConstants.WEEKSEC)*GpsConstants.WEEKSEC;
    prS = prS - delS;
    %prS are in the range [-WEEKSEC/2 : WEEKSEC/2];
    %check that common bias is not huge (like, bigger than 10s)
    maxBiasSeconds = 10; 
    if any(prS>maxBiasSeconds)
        error('Failed to correct week rollover\n')
    else
        prSeconds(iRollover) = prS; %put back into prSeconds vector
        %Now adjust tRxSeconds by the same amount:
        tRxSeconds(iRollover) = tRxSeconds(iRollover) - delS;
        fprintf('Corrected week rollover\n')
    end
end
%TBD Unit test this

end %end of function CheckGpsWeekRollover
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


