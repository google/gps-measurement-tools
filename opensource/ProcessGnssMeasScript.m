tic;
clc;clear all; close(findall(0,'Type','figure')); 
%%
%ProcessGnssMeasScript.m, script to read GnssLogger output, compute and plot:
% pseudoranges, C/No, and weighted least squares PVT solution
%
% you can run the data in pseudoranges log files provided for you:
prFileName = 'workshop_trials01.txt'; %CHANGE HERE
%prFileName = 'pseudoranges_log_2016_06_30_21_26_07.txt'; %with duty cycling, no carrier phase
% prFileName = 'pseudoranges_log_2016_08_22_14_45_50.txt'; %no duty cycling, with carrier phase
% as follows
% 1) copy everything from GitHub google/gps-measurement-tools/ to 
%    a local directory on your machine
% 2) change 'dirName = ...' to match the local directory you are using:
%dirName = '~/Documents/MATLAB/gpstools/opensource/demoFiles';

softPath = pwd;
addpath(softPath);
%CHANGE this to your data folder
dirName = sprintf('%s%s',softPath,'\demoFiles');
%'d:/tmp/Dropbox/Edu/ION_GNSS/gps-measurement-tools/opensource/demoFiles/'

% 3) run ProcessGnssMeasScript.m script file 

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements
%% debug and profiling tools
% profile on;
% profile clear;
display('DEBUG MODE');
dbstop if error
% dbstop if naninf
% dbstop in subRoutine at 17 if idx==7
dbstatus

param.llaTrueDegDegM = [];
% get display screen file
HW_ScrSize = get(0,'ScreenSize');%in pixels
%% data
%To add your own data:
% save data from GnssLogger App, and edit dirName and prFileName appropriately
%dirName = 'put the full path for your directory here';
%prFileName = 'put the pseuoranges log file name here';

%% parameters
%param.llaTrueDegDegM = [];
%enter true WGS84 lla, if you know it:
%param.llaTrueDegDegM = [37.422578, -122.081678, -28];%Charleston Park Test Site
param.llaTrueDegDegM = [45.5298979 -122.6619045 24.16] %trial
%% Set the data filter and Read log file
dataFilter = SetDataFilter;
[gnssRaw,gnssAnalysis] = ReadGnssLogger(dirName,prFileName,dataFilter,param);
if isempty(gnssRaw), return, end

%% Get online ephemeris from Nasa ftp, first compute UTC Time from gnssRaw:
fctSeconds = 1e-3*double(gnssRaw.allRxMillis(end));
utcTime = Gps2Utc([],fctSeconds);
allGpsEph = GetNasaHourlyEphemeris(utcTime,dirName);
if isempty(allGpsEph), return, end

%% process raw measurements, compute pseudoranges:
[gnssMeas] = ProcessGnssMeas(gnssRaw);

%% plot pseudoranges and pseudorange rates
%h1 = figure('Color','white','MenuBar','figure','Position',[0 0 HW_ScrSize(3) HW_ScrSize(4)]);
h1 = figure('Color','white','MenuBar','figure','Position',[0 0 HW_ScrSize(3) HW_ScrSize(4)]);
[colors] = PlotPseudoranges(gnssMeas,prFileName);
h2 = figure('Color','white','MenuBar','figure','Position',[0 0 HW_ScrSize(3) HW_ScrSize(4)]);
PlotPseudorangeRates(gnssMeas,prFileName,colors);
h3 = figure('Color','white','MenuBar','figure','Position',[0 0 HW_ScrSize(3) HW_ScrSize(4)]);
PlotCno(gnssMeas,prFileName,colors);

%% compute WLS position and velocity
gpsPvt = GpsWlsPvt(gnssMeas,allGpsEph);

%% plot Pvt results
h4 = figure('Color','white','MenuBar','figure','Position',[0 0 HW_ScrSize(3) HW_ScrSize(4)]);
ts = 'Raw Pseudoranges, Weighted Least Squares solution';
PlotPvt(gpsPvt,prFileName,param.llaTrueDegDegM,ts); drawnow;
h5 = figure('Color','white','MenuBar','figure','Position',[0 0 HW_ScrSize(3) HW_ScrSize(4)]);
PlotPvtStates(gpsPvt,prFileName);

%% Plot Accumulated Delta Range 
if any(any(isfinite(gnssMeas.AdrM) & gnssMeas.AdrM~=0))
    [gnssMeas]= ProcessAdr(gnssMeas);
    h6 = figure('Color','white','MenuBar','figure','Position',[0 0 HW_ScrSize(3) HW_ScrSize(4)]);
    PlotAdr(gnssMeas,prFileName,colors);
    [adrResid]= GpsAdrResiduals(gnssMeas,allGpsEph,param.llaTrueDegDegM);drawnow
    h7 = figure('Color','white','MenuBar','figure','Position',[0 0 HW_ScrSize(3) HW_ScrSize(4)]);
    PlotAdrResids(adrResid,gnssMeas,prFileName,colors);
end

%% end of ProcessGnssMeasScript
rmpath(softPath)
toc
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Copyright 2016 Google Inc.
% 
% Licensed under the Apache License, Version 2.0 (the "License");
% you may not use this file except in compliance with the License.
% You may obtain a copy of the License at
% 
%     http://www.apache.org/licenses/LICENSE-2.0
% 
% Unless required by applicable law or agreed to in writing, software
% distributed under the License is distributed on an "AS IS" BASIS,
% WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
% See the License for the specific language governing permissions and
% limitations under the License.
