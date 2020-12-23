function refPvti = GetRefPvti(refPvt,fctSeconds)
%refPvti = GetRefPvti(refPvt,fctSeconds);
%
% Find a matching reference Pvt from refPvt at time = fctSeconds
%
% Inputs:
%   refPvt = reference PVT with fields:
%         .FctSeconds (Nx1), non-decreasing time tags
%         .LlaDegDegM (Nx3)
%         .VelNedMps (Nx3) 
%     for stationary position: N=1, FctSeconds = [], and VelNedMps = [0,0,0]
%
%   fctSeconds = scalar time (GPS full cycle time, seconds)
%
% The function looks for matching or straddling time tags for fctSeconds.
% If refPvt doesn't have measurements close, then output refPvti=[]
% "close" = AnalysisThresholds.REFPVTDELTASECONDS

refPvti = [];

%Check stationary case first:
if isempty(refPvt.FctSeconds) && size(refPvt.LlaDegDegM,1)==1 && ...
        all(refPvt.VelNedMps == 0)
    refPvti = refPvt;
    return
end

%TBD add moving case
allSeconds = refPvt.FctSeconds;
assert(all(diff([allSeconds])>=0),...
    'refPvt.FctSeconds must be non-decreasing')

% find two refPvt.FctSeconds straddling fctSeconds
% if one of them equals fctSeconds, we are done
% elseif both refPvt.FctSeconds are < REFPVTDELTASECONDS seconds from fctSeconds
%        interpolate Lla and Vel
if any(allSeconds==fctSeconds)
    iEqual = find(allSeconds==fctSeconds);
    refPvti.FctSeconds = fctSeconds;
    refPvti.LlaDegDegM = refPvt.LlaDegDegM(iEqual(1),:);
    refPvti.VelNedMps = refPvt.VelNedMps(iEqual(1),:);
    return
end
%get index of times that straddle fctSeconds
iL = find(allSeconds < fctSeconds);
iR = find(allSeconds > fctSeconds);
if isempty(iL) || isempty(iR)
    return
end
iL=iL(end);
iR=iR(1); %index into refPvt straddling fctSeconds
dtL = fctSeconds-refPvt.FctSeconds(iL);
dtR = refPvt.FctSeconds(iR)-fctSeconds;
if dtL>AnalysisThresholds.REFPVTDELTASECONDS || ...
        dtR>AnalysisThresholds.REFPVTDELTASECONDS
    return
end
%Now we have refPvt straddling fctSeconds, and within REFPVTDELTASECONDS
%interpolate these refPvt
refPvti.FctSeconds = fctSeconds;
refPvti.LlaDegDegM = (refPvt.LlaDegDegM(iL,:)*dtR + ...
    refPvt.LlaDegDegM(iR,:)*dtL)/(dtL+dtR);
refPvti.VelNedMps = (refPvt.VelNedMps(iL,:)*dtR + ...
    refPvt.VelNedMps(iR,:)*dtL)/(dtL+dtR);

end %end of function GetRefPvti
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

