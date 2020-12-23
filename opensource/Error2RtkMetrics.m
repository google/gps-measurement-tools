function [rtkMetrics] = Error2RtkMetrics(hError, xTrackError, alongTrackError, thresh1MeterSec)
% Calculate metrics for RTK evaluation given horizontal erros from Nmea data.
%
% input:
%   hError: horizontal error calculated from Pvt. Expecting the following,
%     .FctSeconds [Nx1] vector of epoch time tags.
%     . distM [Nx1] vector of errors in meters.
%   xTrackError: XTrack error calcualted from Pvt. Expecting the following,
%     .ErrorM [Nx1] vector of errors in meters.
%   alongTrackError: AlongTrack error calcualted from Pvt. 
%     .ErrorM [Nx1] vector of errors in meters.
%   thresh1MeterSec: minimal continuous seconds with < 1 meter
%       error to be consider reached 1 meter accuracy.
% output:
%  rtkMetrics: variuos metrics for evaluating RTK.
%     .d50M: 50pct accuracy (horizontal error) in meters.
%     .d80M: 80pct accuracy in meters.
%     .d95M: 95pct accuracy in meters.
%     .tT1MSec: time to 1 meter accuracy in seconds.
%     .avgTimeAt1MSec: average time at 1 meter accuracy in seconds.
%     .accAt5Sec: accuracy at 5 seconds in meters.
%     .accAt10Sec: accuracy at 10 seconds in meters.
%     .xTrackErrorM50: 50pct of x-track horizontal error in meters.
%     .xTrackErrorM80: 80pct of x-track horizontal error in meters.
%     .xTrackErrorM95: 95pct of x-track horizontal error in meters.
%     .hError: Horizontal error structure.
%     .xTrackError: XTrackError structure.
%     .alongTrackErrorM50: 50pct of along-track horizontal error in meters.
%     .alongTrackErrorM80: 80pct of along-track horizontal error in meters.
%     .alongTrackErrorM95: 95pct of along-track horizontal error in meters.
%     .alongTrackError: AlongTrackError structure.

%% Horizontal error related metrics.
iF = isfinite(hError.distM); % decimate to finite results
min_data = max(thresh1MeterSec, 10);
assert(length(iF) >= min_data,...
  sprintf('InvalidArgument: Need at least %s data points. %s are given',...
    min_data, length(iF)));

dM = hError.distM(iF);
elapsedTimeSec = hError.FctSeconds(iF) - hError.FctSeconds(1);

% Find all the datapoints with errors less than 1 meter, i.e. reached 1 meter
% accuracy.
reached1MIndices = transpose((dM <=1));
% Find the segments that are
% 1) reached 1 meter accuracy,
% 2) with at least $thresh1MeterSec datapoints.
tT1MSec = strfind(reached1MIndices, ones(1, thresh1MeterSec));
if (~isempty(tT1MSec))
  % Record the timestamp of 1st segment as the time to 1 meter accuracy.
  rtkMetrics.tT1MSec = elapsedTimeSec(tT1MSec(1));
else
  rtkMetrics.tT1MSec= NaN;
end

% Indices where <1m error starts.
start1MAccIndex = strfind([0 reached1MIndices 0], [0 1]);
% Indices where <1m error ends.
end1MAccuIndex = strfind([0 reached1MIndices 0], [1 0]);
timeAt1mAccSec = end1MAccuIndex - start1MAccIndex;
rtkMetrics.avgTimeAt1MSec = sum(timeAt1mAccSec) / length(timeAt1mAccSec);

accAt5Sec = find(elapsedTimeSec >= 5);
rtkMetrics.accAt5Sec = dM(accAt5Sec(1));

accAt10Sec = find(elapsedTimeSec >= 10);
rtkMetrics.accAt10Sec = dM(accAt10Sec(1));

dM = sort(dM);
rtkMetrics.d50M = median(dM);
rtkMetrics.d80M = dM(round(length(dM)*.80));
rtkMetrics.d95M = dM(round(length(dM)*.95));

rtkMetrics.hError = hError;

%% XTrack & AlongTrack error related metrics.
absXtrackErrorSortedM = sort(abs(xTrackError.ErrorM));
rtkMetrics.xTrackError = xTrackError;
rtkMetrics.xTrackErrorM50 = median(absXtrackErrorSortedM);
rtkMetrics.xTrackErrorM80 = absXtrackErrorSortedM(round(length(absXtrackErrorSortedM) *.80));
rtkMetrics.xTrackErrorM95 = absXtrackErrorSortedM(round(length(absXtrackErrorSortedM) *.95));

absAlongTrackErrorSortedM = sort(abs(alongTrackError.ErrorM));
rtkMetrics.alongTrackError = alongTrackError;
rtkMetrics.alongTrackErrorM50 = median(absAlongTrackErrorSortedM);
rtkMetrics.alongTrackErrorM80 = absAlongTrackErrorSortedM(round(length(absAlongTrackErrorSortedM) *.80));
rtkMetrics.alongTrackErrorM95 = absAlongTrackErrorSortedM(round(length(absAlongTrackErrorSortedM) *.95));

end
%end of function Error2RtkMetrics
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
