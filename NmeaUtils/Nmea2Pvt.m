function gnssPvt = Nmea2Pvt(nmea)
% gnssPvt = Nmea2Pvt(nmea);
%
% pack the gnssPvt structure from the nmea structure.
% Sort times so they are never decreasing.
% slow speeds (< AnalysisThresholds.ZEROTHRESHMPS) with invalid
% track are replaced by zero, so that the residual and clock 
% analysis is as-if the receiver were stationary.
%
% input nmea from ReadNmea.m
% output: gnssPvt for clock and residual analysis.
% Using same naming convention as GnssPvt:
% gnssPvt.Source = 'nmea'
%    .FctSeconds    Nx1 time vector, same as gnssMeas.FctSeconds
%    .allLlaDegDegM Nx3 matrix, (i,:) = [lat (deg), lon (deg), alt (m)]
%    .LlaDegDegM same as allLlaDegDegM (legacy inconsistency, to be fixed)
%    .sigmaLlaM     Nx3 standard deviation of [lat,lon,alt] (m)
%       sigma lat,lon derived from hdop:
%       sigmaLlaM(1) = sigmaLlaM(2) = (hdop/sqrt(2))*HDOP_SCALE_TO_M
%       sigmaLlaM(3)=1 (nominal)
%    .allVelMps     Nx3 (i,:) = velocity in NED coords
%    .VelNedMps same as allVelMps (legacy inconsistency, to be fixed)
%    .sigmaVelMps   Nx3 standard deviation of velocity (m/s)
%    .numSvs        Nx1 number of satellites used in corresponding llaDegDegM
%    .hdop          Nx1 hdop of corresponding fix

gnssPvt = [];
if isempty(nmea) || ~isfield(nmea,'Gga') || ~isfield(nmea,'Rmc')
    return
end

DEG2RAD = pi/180;

%% unpack data from nmea
gga = [nmea.Gga]; %unpack so we can easily extract vectors
rmc = [nmea.Rmc];

%% eliminate all non-finite p and v,
%we'll do this one variable at a time for clarity:
iP = isfinite([gga.LatDeg]);%finite position
knots2mps = 0.514444;
speedMps = [rmc.SpeedKnots]*knots2mps;
iZ = speedMps<AnalysisThresholds.ZEROTHRESHMPS; %"zero" speed
%if track is NaN and speed < ZEROTHRESHMPS, set both to zero
trackDeg = [rmc.TrackDeg];
iT = isfinite(trackDeg); %finite track
iTZ = ~iT & iZ; %index where track is NaN and speed is "zero"
trackDeg(iTZ) = 0;
speedMps(iTZ) = 0;
%keep only gga, rmc, track and speed indexed by iP:
gga      = gga(iP);
rmc      = rmc(iP); %we will use this for Time
trackDeg = (trackDeg(iP))';%make a column vector for convenience below
speedMps = (speedMps(iP))';

%% initialize output
N = length(gga);
gnssPvt.Source          = 'nmea';
gnssPvt.FctSeconds      = nan(N,1);
gnssPvt.allLlaDegDegM   = nan(N,3);
gnssPvt.LlaDegDegM      = nan(N,3); %unfortunate inconsistency in variable names
gnssPvt.sigmaLlaM       = zeros(N,3);
gnssPvt.allVelMps       = nan(N,3);
gnssPvt.VelNedMps       = nan(N,3); %unfortunate inconsistency in variable names
gnssPvt.sigmaVelMps     = zeros(N,3);
gnssPvt.numSvs          = nan(N,1);
gnssPvt.hdop            = nan(N,1);

%% pack gnssPvt
utcTime = datevec([rmc.Datenum]);
[~,gnssPvt.FctSeconds] = Utc2Gps(utcTime);
gnssPvt.allLlaDegDegM = [[gga.LatDeg]', [gga.LonDeg]',[gga.AltM]'];
gnssPvt.LlaDegDegM = gnssPvt.allLlaDegDegM;
gnssPvt.allVelMps = [(cos(trackDeg*DEG2RAD)).*speedMps, ...
    (sin(trackDeg*DEG2RAD)).*speedMps, ...
    zeros(N,1)];
gnssPvt.VelNedMps = gnssPvt.allVelMps;
gnssPvt.numSvs = [gga.NumSats]';
gnssPvt.hdop = [gga.Hdop]';
%% recover sigmaLlaM(1:2) from hdop;
gnssPvt.sigmaLlaM(:,1) = (gnssPvt.hdop/sqrt(2))*10; 
gnssPvt.sigmaLlaM(:,2) = gnssPvt.sigmaLlaM(:,1);
gnssPvt.sigmaLlaM(:,3) = 1;

%% ensure times are non-decreasing
if any(diff([gnssPvt.FctSeconds])<0)
    [gnssPvt.FctSeconds,iS] = sort([gnssPvt.FctSeconds]);
    gnssPvt.allLlaDegDegM = gnssPvt.allLlaDegDegM(iS,:);
    gnssPvt.allVelMps = gnssPvt.allVelMps(iS,:);
end

%% check for constant heading, this is a common bugs in NMEA creation from log files
iNZ = find(speedMps>AnalysisThresholds.ZEROTHRESHMPS); %non-zero speed
if length(iNZ)>100
    assert(~all(trackDeg(iNZ) == trackDeg(iNZ(1))),...
        'All rmc.TrackDeg are equal while speed>0, should not happen. Bug in NMEA creation.')
end

end %end of function Nmea2Pvt
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
