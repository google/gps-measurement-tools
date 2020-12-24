function [xTrackError,alongTrackError] = XTrackErrorFromPvt(testPvt, refPvt, fctStartS, fctEndS)
% Compute cross-track and along-track error of positions relative to reference.
%
% inputs:testPvt: test data in PVT structure.
%        refPvt: reference data in PVT structure.
%        [utcStart], [utcEnd]: [optional] [nx6] intervals to consider.
%
% output: XTrackError and AlongTrackError structures with fields:
%   .FctSeconds [Nx1] vector of epoch time tags
%   .ErrorM  [Nx1] error perpendicular to direction of trabel, right = +ve, left = -ve
%   .FirEpochs = [8,15,30,60] %size of epochs for doing FIR filter (i.e. average)
%   .FirErrorM [Nx4] cross track errors after FIR

%% Initialize XTrack & AlongTrack structure.
N = length(testPvt.FctSeconds);
xTrackError.FctSeconds = testPvt.FctSeconds;
xTrackError.ErrorM = zeros(N,1)+NaN;
alongTrackError.FctSeconds = testPvt.FctSeconds;
alongTrackError.ErrorM = zeros(N,1)+NaN;

%% Window data to intervals between utcStart and utcEnd.
if nargin<4
  fctEndS = inf;
  if nargin < 3
    fctStartS = 0;
  end
end

%% At each epoch, compute x-track & along-track error.
for i=1:N
    if ~any(testPvt.FctSeconds(i)>=fctStartS & testPvt.FctSeconds(i)<=fctEndS)
        continue %not in (utcStart, utcEnd) window, skip to next epoch
    end
    refPvti = GetRefPvti(refPvt,testPvt.FctSeconds(i));
    if isempty(refPvti) %no refPvt found close enough to testPvt.FctSeconds(i)
        continue %skip to next epoch
    end
    hSpeedMps = norm(refPvti.VelNedMps(1:2)); %horizontal speed (m/s)
    SPEEDTHRESH_FOR_XTRACK_MPS = 0.5; %threshold of Xtrack speed error (m/s)
    if hSpeedMps < SPEEDTHRESH_FOR_XTRACK_MPS
        continue %speed too low for a well defined course
    end
    if testPvt.hdop(i)>2 %Hdop>2 <=> EA>10m
        continue
    end
    vNMps = refPvti.VelNedMps(1);
    vEMps = refPvti.VelNedMps(2);
    courseRad = Ne2Brg(vNMps,vEMps); %course, in radians cw from N
    nedM = Lla2Ned(testPvt.LlaDegDegM(i,:), refPvti.LlaDegDegM);
    a = Ne2Brg(nedM(1),nedM(2)); %angle of nedM, in radians clockwise from N
    %x-track error = projection of nedM onto line perpendicular to course
    xTrackError.ErrorM(i) = norm(nedM(1:2))*sin(a-courseRad);
    alongTrackError.ErrorM(i) = norm(nedM(1:2))*cos(a-courseRad);
    %sign sense: right of course = positive, left = negative
end

%decimate to finite results
iF = isfinite(xTrackError.ErrorM);
xTrackError.FctSeconds   = xTrackError.FctSeconds(iF);
xTrackError.ErrorM       = xTrackError.ErrorM(iF);
N = length(xTrackError.FctSeconds);
alongTrackError.FctSeconds   = alongTrackError.FctSeconds(iF);
alongTrackError.ErrorM       = alongTrackError.ErrorM(iF);
N = length(alongTrackError.FctSeconds);

%% Do x-track and along-track for FIRs of 8, 15, 30, 60 epochs.
xTrackError.FirEpochs = [8,15,30,60];
numFirs = length(xTrackError.FirEpochs);
xTrackError.FirErrorM = zeros(N,numFirs)+NaN;
for j=1:numFirs
    w = xTrackError.FirEpochs(j); %width of FIR window
    for i=1:(N-w+1)
        k = i+w-1;
        xTrackError.FirErrorM(k,j) = mean(xTrackError.ErrorM(i:k));
    end
end

alongTrackError.FirEpochs = [8,15,30,60];
numFirs = length(alongTrackError.FirEpochs);
alongTrackError.FirErrorM = zeros(N,numFirs)+NaN;
for j=1:numFirs
    w = alongTrackError.FirEpochs(j); %width of FIR window
    for i=1:(N-w+1)
        k = i+w-1;
        alongTrackError.FirErrorM(k,j) = mean(alongTrackError.ErrorM(i:k));
    end
end

end
%end of function XTrackErrorFromPvt
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
