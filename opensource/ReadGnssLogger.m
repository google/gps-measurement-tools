function [gnssRaw,gnssAnalysis] = ReadGnssLogger(dirName,fileName,dataFilter,gnssAnalysis)
%% [gnssRaw,gnssAnalysis]=ReadGnssLogger(dirName,fileName,[dataFilter],[gnssAnalysis]);
% Read the log file created by Gnss Logger App in Android
% Compatible with Android release N
%
% Input: 
%  dirName = string with directory of fileName, 
%                e.g. '~/Documents/MATLAB/Pseudoranges/2016-03-28'
%  fileName = string with filename
%  optional inputs:
%  [dataFilter], nx2 cell array of pairs of strings, 
%      dataFilter{i,1} is a string with one of 'Raw' header values from the 
%                      GnssLogger log file e.g. 'ConstellationType'
%      dataFilter{i,2} is a string with a valid matlab expression, containing
%                      the header value, e.g. 'ConstellationType==1'
%      See SetDataFilter.m for full rules and examples of dataFilter.
%  [gnssAnalysis] structure containing analysis, incl list of missing fields
%
% Output: 
%  gnssRaw, all GnssClock and GnssMeasurement fields from log file, including:
%           .TimeNanos (int64)
%           .FullBiasNanos (int64)
%           ...
%           .Svid
%           .ReceivedSvTimeNanos (int64)
%           .PseudorangeRateMetersPerSecond
%           ...
%        and data fields created by this function:
%           .allRxMillis (int64), full cycle time of measurement (milliseconds)
%           accurate to one millisecond, it is convenient for matching up time 
%           tags. For computing accurate location, etc, you must still use 
%           TimeNanos and gnssMeas.tRxSeconds
%
%  gnssAnalysis, structure containing analysis, including list of missing fields
%
% see also: SetDataFilter, ProcessGnssMeas

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements

%factored into a few main sub-functions:
% MakeCsv() 
% ReadRawCsv()
% FilterData() 
% PackGnssRaw()
% CheckGnssClock()
% ReportMissingFields()

%% Initialize outputs and inputs
gnssRaw = [];
gnssAnalysis.GnssClockErrors = 'GnssClock Errors.';
gnssAnalysis.GnssMeasurementErrors = 'GnssMeasurement Errors.';
gnssAnalysis.ApiPassFail = '';
if nargin<3, dataFilter = []; end

%% check we have the right kind of fileName
extension = fileName(end-3:end);
if ~any(strcmp(extension,{'.txt','.csv'}))
    error('Expecting file name of the form "*.txt", or "*.csv"');
end

%% read log file into a numeric matrix 'S', and a cell array 'header'
rawCsvFile = MakeCsv(dirName,fileName);
[header,C] = ReadRawCsv(rawCsvFile);

%% apply dataFilter 
[bOk] = CheckDataFilter(dataFilter,header);
if ~bOk, return, end
[bOk,C] = FilterData(C,dataFilter,header);
if ~bOk, return, end

%% pack data into gnssRaw structure
[gnssRaw,missing] = PackGnssRaw(C,header);

%% check clock and measurements
[gnssRaw,gnssAnalysis] = CheckGnssClock(gnssRaw,gnssAnalysis);
gnssAnalysis = ReportMissingFields(gnssAnalysis,missing);


end %end of function ReadGnssLogger
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function csvFileName = MakeCsv(dirName,fileName)
%% make csv file, if necessary. 
%And return extended csv file name (i.e. with full path in the name)

%TBD, maybe, do this entirely with Matlab read/write functions, make independent
%from grep and sed

%make extended file name
if dirName(end)~='/'
    dirName = [dirName,'/']; %add /
end
csvFileName = [dirName,'raw.csv'];
if strcmp(fileName(end-3:end),'.csv')
    return %input file is a csv file, nothing more to do here
end
    
extendedFileName = [dirName,fileName];
fprintf('\nReading file %s\n',extendedFileName)

%% read version
txtfileID = fopen(extendedFileName,'r');
if txtfileID<0
    error('file ''%s'' not found',extendedFileName);
end
line='';
while isempty(strfind(lower(line),'version'))
    line = fgetl(txtfileID);
    if ~ischar(line) %eof or error occurred
        if isempty(line)
            error('\nError occurred while reading file %s\n',fileName)
        end
        break
    end
end
if line==-1
    fprintf('\nCould not find "Version" in input file %s\n',fileName)
    return
end
%look for the beginning of the version number, e.g. 1.4.0.0
iDigits = regexp(line,'\d'); %index into the first number found in line
v = sscanf(line(iDigits(1):end),'%d.%d.%d.%d',4);
if length(v)<4
    v(end+1:4,1)=0; %make v into a length 4 column vector
end
%Now extract the platform
k = strfind(line,'Platform:');
if any(k)
    sPlatform = line(k+9:end);
else
    sPlatform = '';%set empty if 'Platform:' not found
end
if isempty(strfind(sPlatform,'N'))
    %add || strfind(platform,'O') and so on for future platforms
    fprintf('\nThis version of ReadGnssLogger supports Android N\n')
    fprintf('WARNING: did not find "Platform" type in log file, expected "Platform: N"\n')
    fprintf('Please Update GnssLogger\n')
    sPlatform = 'N';%assume version N
end

v1 = [1;4;0;0];
sCompare = CompareVersions(v,v1);
%Note, we need to check both the logger version (e.g. v1.0.0.0) and the
%Platform version "e.g. Platform: N" for any logic based on version
if strcmp(sCompare,'before')
    fprintf('\nThis version of ReadGnssLogger supports v1.4.0.0 onwards\n')
    error('Found "%s" in log file',line)
end

%% write csv file with header and numbers
%We could use grep and sed to make a csv file
%fclose(txtfileID);
% system(['grep -e ''Raw,'' ',extendedFileName,...
%     ' | sed -e ''s/true/1/'' -e ''s/false/0/'' -e ''s/# //'' ',...
%     ' -e ''s/Raw,//'' ',... %replace "Raw," with nothing
%     '-e ''s/(//g'' -e ''s/)//g'' > ',csvFileName]);
%
% On versions from v1.4.0.0 N:
% grep on "Raw," replace alpha characters amongst the numbers,
% remove parentheses in the header,
% note use of /g for "global" so sed acts on every occurrence in each line
% csv file "prs.csv" now contains a header row followed by numerical data
%
%But we'll do the same thing using Matlab, so people don't need grep/sed:
csvfileID = fopen(csvFileName,'w');
while ischar(line)
   line = fgetl(txtfileID);
   if isempty(strfind(line,'Raw,'))
       continue %skip to next line
   end
   %Now 'line' contains the raw measurements header or data
   line = strrep(line,'Raw,',''); 
   line = strrep(line,'#',''); line = strrep(line,' ','');%remove '#' and spaces
   %from versions v1.4.0.0 N we actually dont need to look for '(',')','true'
   %or 'false' anymore. So we are done with replacing. That was easy.
   fprintf(csvfileID,'%s\n',line);
end
fclose(txtfileID);
fclose(csvfileID);
if isempty(line) %line should be -1 at eof
    error('\nError occurred while reading file %s\n',fileName)
end

end %end of function MakeCsv
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function [header,C] = ReadRawCsv(rawCsvFile)
%% read data from csv file into a numerical matrix 'S' and cell array 'header'
S = csvread(rawCsvFile,1,0);%read numerical data from second row onwards
%Note csvread fills ,, with zero, so we will need a lower level read function to
%tell the difference between empty fields and valid zeros
%T = readtable(csvFileName,'FileType','text'); %use this to debug

%read header row:
fid = fopen(rawCsvFile);
if fid<0
    error('file ''%s'' not found',rawCsvFile);
end
headerString = fgetl(fid);
if isempty(strfind(headerString,'TimeNanos'))
    error('\n"TimeNanos" string not found in file %s\n',fileName)
end

header=textscan(headerString,'%s','Delimiter',','); 
header = header{1}; %this makes header a numFieldsx1 cell array
numFields = size(header,1);

%check that numFields == size(S,2)
[~,M] = size(S); %M = number of columns
assert(numFields==M,...
 '# of header names is different from # of columns of numerical data')

%read lines using formatSpec so we get TimeNanos and FullBiasNanos as
%int64, everything else as doubles, and empty values as NaN
formatSpec='';
for i=1:M
    %lotsa || here, because we are comparing a vector, 'header'
    %to a specific string each time. Not sure how to do this another way
    %and still be able to easily read and debug. Better safe than too clever.
    
    %longs
    if i == find(strcmp(header,'TimeNanos')) || ...
            i == find(strcmp(header,'FullBiasNanos')) || ...
            i == find(strcmp(header,'ReceivedSvTimeNanos')) || ...
            i == find(strcmp(header,'ReceivedSvTimeUncertaintyNanos')) || ...
            i == find(strcmp(header,'CarrierCycles'))
        formatSpec = sprintf('%s %%d64',formatSpec);
    elseif 0
        %ints
        % TBD maybe %d32 for ints: AccumulatedDeltaRangeState, ...
        %  ConstellationType, MultipathIndicator, State, Svid
        formatSpec = sprintf('%s %%d32',formatSpec);
    else
        %everything else doubles
        formatSpec = sprintf('%s %%f',formatSpec);
    end
end
%for empty fields, enter 'NaN' into csv
C = textscan(fid,formatSpec,'Delimiter',',','EmptyValue',NaN);
fclose(fid);

end% of function ReadRawCsv
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function [bOk,C] = FilterData(C,dataFilter,header)
%% filter C based on contents of dataFilter

bOk = true;
iS = ones(size(C{1})); %initialize index into rows of C
for i=1:size(dataFilter,1)
    j=find(strcmp(header,dataFilter{i,1}));%j = index into header
    %we should always be a value of j, because checkDataFilter checks for this:
    assert(any(j),'dataFilter{i} = %s not found in header\n',dataFilter{i,1})
    
    %now we must evaluate the expression in dataFilter{i,2}, for example:
    % 'BiasUncertaintyNanos < 1e7'
    %assign the relevant cell of C to a variable with same name as the header
    ts = sprintf('%s = C{%d};',header{j},j);
    eval(ts);
    %create an index vector from the expression in dataFilter{i,2}
    ts = sprintf('iSi = %s;',dataFilter{i,2});
    eval(ts);
    
    %AND the iS index values on each iteration of i
    iS = iS & iSi;
end
% Check if filter removes all values
if ~any(iS) %if all zeros
    fprintf('\nAll measurements removed. Specify dataFilter less strictly than this:, ')
    dataFilter(:,2)
    bOk=false;
    C=[];
    return
end

% Keep only those values of C indexed by iS
for i=1:length(C)
    C{i} = C{i}(iS);
end

end %end of function FilterDataS
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


function [gnssRaw,missing] = PackGnssRaw(C,header)
%% pack data into gnssRaw, and report missing fields
assert(length(C)==length(header),...
    'length(C) ~= length(header). This should have been checked before here')

gnssRaw = [];
%report clock fields present/missing, based on: 
gnssClockFields = {...
    'TimeNanos'
    'TimeUncertaintyNanos'
    'LeapSecond'
    'FullBiasNanos'
    'BiasUncertaintyNanos'
    'DriftNanosPerSecond'
    'DriftUncertaintyNanosPerSecond'
    'HardwareClockDiscontinuityCount'
    'BiasNanos'
    };
missing.ClockFields = {};

%report measurements fields present/missing, based on: 
gnssMeasurementFields = {...
    'Cn0DbHz'
    'ConstellationType'
    'MultipathIndicator'
    'PseudorangeRateMetersPerSecond'
    'PseudorangeRateUncertaintyMetersPerSecond'
    'ReceivedSvTimeNanos'
    'ReceivedSvTimeUncertaintyNanos'
    'State'
    'Svid'
    'AccumulatedDeltaRangeMeters'
    'AccumulatedDeltaRangeUncertaintyMeters'
    };
%leave these out for now, 'cause we dont care (for now), or they're deprecated, 
% or they could legitimately be left out (because they are not computed in
% a particular GNSS implementation)
% SnrInDb, TimeOffsetNanos, CarrierFrequencyHz, CarrierCycles, CarrierPhase, 
% CarrierPhaseUncertainty
missing.MeasurementFields = {};

%pack data into vector variables, if the fields are not NaNs
for j = 1:length(header)
    if any(isfinite(C{j})) %not all NaNs
        %TBD what if there are some NaNs, but not all. i.e. some missing
        %data in the log file - TBD deal with this
        eval(['gnssRaw.',header{j}, '=C{j};']);
    elseif any(strcmp(header{j},gnssClockFields))
        missing.ClockFields{end+1} = header{j};
    elseif any(strcmp(header{j},gnssMeasurementFields))
        missing.MeasurementFields{end+1} = header{j};
    end
end
%So, if a field is not reported, it will be all NaNs from makeCsv, and the above
%code will not load it into gnssRaw. So when we call 'CheckGnssClock' it can
%check for missing fields in gnssRaw.

%TBD look for all zeros that can not legitimately be all zero, 
%e.g. AccumulatedDeltaRangeMeters, and report these as missing data

end %end of function PackGnssRaw
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function [gnssRaw,gnssAnalysis,bOk] = CheckGnssClock(gnssRaw,gnssAnalysis)
%% check clock values in gnssRaw
bOk = true;
sFail = ''; %initialize string to record failure messafes
N = length(gnssRaw.ReceivedSvTimeNanos);
%Insist on the presence of TimeNanos (time from hw clock)
if ~isfield(gnssRaw,'TimeNanos')
    s = ' TimeNanos  missing from GnssLogger File.';
    fprintf('WARNING: %s\n',s);
    sFail = [sFail,s];
    bOk = false;
end
if ~isfield(gnssRaw,'FullBiasNanos')
    s = 'FullBiasNanos missing from GnssLogger file.';
    fprintf('WARNING: %s, we need it to get the week number\n',s);
    sFail = [sFail,s];
    bOk = false;
end
if ~isfield(gnssRaw,'BiasNanos')
    gnssRaw.BiasNanos = zeros(N,1);
end
if ~isfield(gnssRaw,'HardwareClockDiscontinuityCount')
    %possibly non fatal error, we assume there is no hardware clock discontinuity
    %so we set to zero and move on, but we print a warning
    gnssRaw.HardwareClockDiscontinuityCount = zeros(N,1);
    fprintf('WARNING: Added HardwareClockDiscontinuityCount=0 because it is missing from GNSS Logger file\n');
end

%check FullBiasNanos, it should be negative values
bChangeSign = any(gnssRaw.FullBiasNanos<0) & any(gnssRaw.FullBiasNanos>0);
assert(~bChangeSign,...
    'FullBiasNanos changes sign within log file, this should never happen');
%Now we know FullBiasNanos doesnt change sign,auto-detect sign of FullBiasNanos, 
%if it is positive, give warning and change
if any(gnssRaw.FullBiasNanos>0)
    gnssRaw.FullBiasNanos = -1*gnssRaw.FullBiasNanos;
    fprintf('WARNING: FullBiasNanos wrong sign. Should be negative. Auto changing inside ReadGpsLogger\n');
    gnssAnalysis.GnssClockErrors = [gnssAnalysis.GnssClockErrors,...
        sprintf(' FullBiasNanos wrong sign.')];
end

%compute full cycle time of measurement, in milliseonds
gnssRaw.allRxMillis = int64((gnssRaw.TimeNanos - gnssRaw.FullBiasNanos)*1e-6);
%allRxMillis is now accurate to one millisecond (because it's an integer)

if ~bOk
    gnssAnalysis.ApiPassFail = ['FAIL ',sFail]; 
end

end %end of function CheckGnssClock
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function gnssAnalysis = ReportMissingFields(gnssAnalysis,missing)
%% report missing clock and measurement fields in gnssAnalysis

%report missing clock fields
if ~isempty(missing.ClockFields)
    gnssAnalysis.GnssClockErrors = sprintf(...
        '%s Missing Fields:',gnssAnalysis.GnssClockErrors);
    for i=1:length(missing.ClockFields)
    gnssAnalysis.GnssClockErrors = sprintf(...
        '%s %s,',gnssAnalysis.GnssClockErrors,missing.ClockFields{i});
    end
    gnssAnalysis.GnssClockErrors(end) = '.';%replace final comma with period
end

%report missing measurement fields
if ~isempty(missing.MeasurementFields)
    gnssAnalysis.GnssMeasurementErrors = sprintf(...
        '%s Missing Fields:',gnssAnalysis.GnssMeasurementErrors);
    for i=1:length(missing.MeasurementFields)
    gnssAnalysis.GnssMeasurementErrors = sprintf(...
        '%s %s,',gnssAnalysis.GnssMeasurementErrors,...
        missing.MeasurementFields{i});
    end
    gnssAnalysis.GnssMeasurementErrors(end) = '.';%replace last comma with period
end

%assign pass/fail
if ~any(strfind(gnssAnalysis.ApiPassFail,'FAIL')) %didn't already fail
    if isempty(missing.ClockFields) && isempty(missing.MeasurementFields)
        gnssAnalysis.ApiPassFail = 'PASS';
    else
        gnssAnalysis.ApiPassFail = 'FAIL BECAUSE OF MISSING FIELDS';
    end
end
end %end of function ReportMissingFields
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

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
