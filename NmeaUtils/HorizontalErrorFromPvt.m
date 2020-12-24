function hError = HorizontalErrorFromPvt(testPvt, refPvt)
% hError = HorizontalErrorFromPvt(testPvt, refPvt)
%
% Compute horizontal error of positions relative to reference.
%
% Args:
%   testPvt: test data in GnssPvt struct format.
%   refPvt: reference data in GnssPvt struct format.
%
% Returns:
%   hError: horizontal errors of test data from reference data.
%     .FctSeconds [Nx1] vector of epoch time tags
%     .distM [Nx1] vector of distance in meters between test data point and
%         reference data point at corresponding FctSeconds epoch.
%     .TestNeM horizontal distance from the reference data starting point.
%     .RefNeM horizontal distance from the reference data starting point.

hdopThresh = inf; %only plot where hdop<hdopThresh

%% Initialize hError structure, rows <=> time.
N = length(testPvt.FctSeconds);
hError.FctSeconds = testPvt.FctSeconds;
hError.distM = zeros(N,1)+NaN;
hError.TestNeM = zeros(N,2)+NaN; %for path of testFileName
hError.RefNeM = zeros(N,2)+NaN; %for path of refNmea

lla0 = []; %this will store the reference lla for the plot origin

%% At each epoch, compute horizontal error.
for i=1:N
    if isstruct(refPvt)
        refPvti = GetRefPvti(refPvt,testPvt.FctSeconds(i));
        if isempty(refPvti) %no refPvt found close enough to testPvt.FctSeconds(i)
            continue %skip to next epoch
        end
    else
        refPvti.LlaDegDegM = refPvt;
    end
    if isempty(lla0)
        lla0 = refPvti.LlaDegDegM; %reference lla for the plot origin
    end
    if testPvt.hdop(i)>hdopThresh
        continue
    end
    [~,hError.TestNeM(i,:)]=Lla2Hd(testPvt.LlaDegDegM(i,:),lla0);
    [~,hError.RefNeM(i,:)]=Lla2Hd(refPvti.LlaDegDegM,lla0);
    hError.distM(i) = norm(hError.TestNeM(i,:) - hError.RefNeM(i,:));
end
%end of function HorizontalErrorFromPvt
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%