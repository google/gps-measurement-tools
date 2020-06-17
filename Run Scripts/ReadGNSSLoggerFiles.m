clear all, close all

dirName = 'C:\Users\Sherman\Stanford Onedrive\OneDrive - Leland Stanford Junior University';
dirName = 'C:\Users\Sherman\Stanford Onedrive\OneDrive - Leland Stanford Junior University\Matlab\Smartphone\GNSSLoggerRawData';
dirName = 'C:\Users\daeda\Documents\gnss_log'

% datefile = '2018_09_10_17_57_44';
% fileName = ['gnss_log_' datefile '.txt']
% fileName = ['Approach and Landing at SEA.txt']
% fileName = ['gnss_log_2018_11_28_12_24_10.txt'];
% fileName = ['gnss_log_2018_12_08_00_11_42.txt'];
% fileName = ['Stationary room data.txt'];
% fileName = ['walk from caltrain.txt'];
% fileName = ['Stationary data in the courtyard.txt'];
% fileName = ['stationary courtyard data - 12-18-18.txt'];
% fileName = ['gnss_log_2018_05_03_22_14_07.txt']; % fort huachuaca
% fileName = ['gnss_log_2018_02_19_10_44_37.txt'];
% fileName = ['gnss_log_2018_02_19_10_07_22.txt'];
% fileName = 'gnss_log_2018_08_08_11_29_45.txt'

fileName    = ['gnss_log_2020_06_08_20_01_59.txt'];
fileName    = ['gnss_log_2020_06_02_19_49_41.txt'];
fileName    = ['gnss_log_2020_05_12_17_07_41.txt'];
under_idx   = strfind(fileName, '_');
datayear    = str2num(fileName(under_idx(2)+1:under_idx(3)-1));
datamonth   = str2num(fileName(under_idx(3)+1:under_idx(4)-1));
dataday     = str2num(fileName(under_idx(4)+1:under_idx(5)-1));
datahour    = str2num(fileName(under_idx(5)+1:under_idx(6)-1));
datamin     = str2num(fileName(under_idx(6)+1:under_idx(7)-1));
datasec     = str2num(fileName(under_idx(7)+1:under_idx(7)+2));
datefile    = fileName(under_idx(2)+1:under_idx(7)+2)

% addpathname = 'C:\Users\Sherman\Dropbox\MATLAB\gpstools\opensource';
addpathname = 'C:\Users\daeda\Documents\GitHub\gps-measurement-tools\opensource';
path(path,addpathname);

% addpathname = 'C:\Users\Sherman\Dropbox\MATLAB\gpstools\opensource';
% path(path,addpathname);
dataFilter = SetDataFilter
[gnssRaw,gnssAnalysis] = ReadGnssLogger(dirName,fileName,dataFilter);

% [gnssRaw,gnssAnalysis]=ReadGnssLogger(dirName,fileName,dataFilter);
%% Get online ephemeris from Nasa ftp, first compute UTC Time from gnssRaw:
fctSeconds = 1e-3*double(gnssRaw.allRxMillis(end));
utcTime = Gps2Utc([],fctSeconds);
allGpsEph = GetNasaHourlyEphemeris(utcTime,dirName);
if isempty(allGpsEph), return, end

%% process raw measurements, compute pseudoranges:
[gnssMeas] = ProcessGnssMeas(gnssRaw);


pause

% pvt currently only works for GPS only
gpsPvt = GpsWlsPvt(gnssMeas,allGpsEph)


%%
pause
datedash = strfind(datefile,'_')

gpslat = gpsPvt.allLlaDegDegM(:,1);
gpslon = gpsPvt.allLlaDegDegM(:,2);
gpsht  = gpsPvt.allLlaDegDegM(:,3);
gpstime = gpsPvt.FctSeconds;

savestr = ['save gpsproc' datefile ' gpslat gpslon gpsht gpstime'];
eval(savestr);


%% Plot google earth
% path(path, 'C:\Users\daeda\Dropbox\MATLAB\SharkTag\CoarseTimePosition\utilities\plot_google_map')
path(path, 'C:\Users\daeda\Documents\GitHub\gps-measurement-tools\Run Scripts')
% path(path, 'C:\Users\Sherman\Dropbox\MATLAB\SharkTag\CoarseTimePosition\utilities\plot_google_map')
figure(100),
plot(gpslon, gpslat,'x')
plot_google_map('APIKey', 'AIzaSyBHzFFKC26AGOr0L2HuKzOqkt0duzCC-kk')
