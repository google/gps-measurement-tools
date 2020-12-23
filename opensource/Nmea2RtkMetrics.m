function rtkMetrics = Nmea2RtkMetrics(testFileName,refFileName,dirName,...
  thresh1MeterSec)
% Calculate metrics for RTK evaluation given Nmea and reference files.
%
% Args:
%   testFileName, refFileName: strings, NMEA files.
%       refFileName may be a 1x3 vector [latDeg,lonDeg,altM], in which case
%       this is treated as the stationary reference.
%   dirName: directory where files are.
%   thresh1MeterSec: [optional] minimal continuous seconds with < 1 meter
%       error to be consider reached 1 meter accuracy. Default 10 seconds.
%
% Returns:
%  rtkMetrics: variuos metrics for evaluating RTK.
%     .d50M: 50pct accuracy (horizontal error) in meters.
%     .d80M: 80pct accuracy in meters.
%     .d95M: 95pct accuracy in meters.
%     .tT1MSec: time to 1 meter accuracy in seconds.
%     .avgTT1MSec: average time at 1 meter accuracy in seconds.
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

%% read NMEA files, and pack into pvt structure.
if ischar(refFileName)
    [refNmea, Msg]     = ReadNmeaFile(dirName,refFileName);
    fprintf('%s\n', Msg{:});
    if isempty(refNmea)
      error('Error reading reference file %s', refFileName)
    end
    refPvt      = Nmea2Pvt(refNmea);
elseif isvector(refFileName) && isequal(size(refFileName),[1,3])
    refPvt  = refFileName;
else
    error(['refFileName input must be a character string, '...
      'or a 1x3 numerical vector'])
end
[testNmea, Msg]    = ReadNmeaFile(dirName,testFileName);
fprintf('%s\n', Msg{:});
if isempty(testNmea)
  error('Error reading test file %s', testFileName)
end
testPvt     = Nmea2Pvt(testNmea);

if nargin < 4
  thresh1MeterSec = 10;
end

%% Horizontal Error.
hError = HorizontalErrorFromPvt(testPvt, refPvt);

%% XTrack error and AlongTrack error
[xTrackError,alongTrackError] = XTrackErrorFromPvt(testPvt, refPvt);

%% Metrics from Horizontal Error, XTrackError, and AlongTrackError
rtkMetrics = Error2RtkMetrics(hError, xTrackError, alongTrackError, thresh1MeterSec);

% Print metrics in console for easier copy/paste.
fprintf(['NumValidPts, 50%% (m), 80%% (m), 95%% (m), TT1M (s), TA1M (s), AA5S (m), '... 
    'AA10S (m), XTrack 50%% (m), XTrack 80%% (m), XTrack 95%% (m), '...
    'AlongTrack 50%% (m), AlongTrack 80%% (m), AlongTrack 95%% (m)\n'...
    '%.2f, %.2f, %.2f, %.2f, %.0f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f, '...
    '%.2f, %.2f\n'],...
    numel(rtkMetrics.xTrackError.ErrorM),rtkMetrics.d50M, rtkMetrics.d80M, ...
    rtkMetrics.d95M, rtkMetrics.tT1MSec,...
    rtkMetrics.avgTimeAt1MSec,rtkMetrics.accAt5Sec,rtkMetrics.accAt10Sec,...
    rtkMetrics.xTrackErrorM50,rtkMetrics.xTrackErrorM80, rtkMetrics.xTrackErrorM95,...
    rtkMetrics.alongTrackErrorM50,rtkMetrics.alongTrackErrorM80,...
    rtkMetrics.alongTrackErrorM95)

end
%end of function Nmea2RtkMetrics
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

