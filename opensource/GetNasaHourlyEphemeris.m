function [allGpsEph,allGloEph] = GetNasaHourlyEphemeris(utcTime,dirName)
%[allGpsEph,allGloEph] = GetNasaHourlyEphemeris(utcTime,dirName)
%Get hourly ephemeris files, 
% If a GPS ephemeris file is in dirName, with valid ephemeris for at
% least 24 svs, then read it; else download from NASA's archive of 
% Space Geodesy Data
%
%Output allGpsEph structure of ephemerides in format defined in ReadRinexNav.m
%       TBD: allGloEph, and any other supported by the NSA ftp site
%
%examples of how to call GetNasaHourlyEphemeris:
%1) with /demoFiles and utcTime where ephemeris has already been downloaded:
%  replace '~/...' with actual path:
%  dirName = '~/Documents/MATLAB/gpstools/opensource/demoFiles';
%  utcTime = [2016,8,22,22,50,00];
%  [allGpsEph,allGloEph] = GetNasaHourlyEphemeris(utcTime,dirName)
%
%2) with utcTime for which ephemeris has not been downloaded, 
%  this exercises the  automatic ftp download. 
%  Replace '~' with actual path:
%  dirName = '~/Documents/MATLAB/gpstools/opensource/demoFiles';
%  utcTime = [2016,5,ceil(rand(1,2).*[30,24]),0,0],%a random Hour and Day in May
%  [allGpsEph,allGloEph] = GetNasaHourlyEphemeris(utcTime,dirName)
%
% More details:
% The Nasa ephemeris Unix-compressed.
% GetNasaHourlyEphemeris will automatically uncompress it, if you have the right 
% uncompress function on you computer. If you need to install an unzip utility, 
% see http://cddis.nasa.gov/ and http://www.gpzip.org
% Search for 'uncompress' in the GetNasaHourlyEphemeris function to find and 
% edit the name of the unzip utility.

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements

allGpsEph=[];allGloEph=[];
if nargin<2
    dirName = [];
end
[bOk,dirName] = CheckInputs(utcTime,dirName);
if ~bOk, return, end

%Description of file names, see:
%http://cddis.gsfc.nasa.gov/Data_and_Derived_Products/GNSS/...
%       broadcast_ephemeris_data.html#GPShourly
yearNumber4Digit = utcTime(1);
yearNumber2Digit = rem(utcTime(1),100);
dayNumber = DayOfYear(utcTime);

hourlyZFile = sprintf('hour%03d0.%02dn.Z',dayNumber,yearNumber2Digit);
ephFilename = hourlyZFile(1:end-2);
fullEphFilename = [dirName,ephFilename]; %full name (with directory specified)

%check if ephemeris file already exists (e.g. you downloaded it 'by hand')
%and if there are fresh ephemeris for lotsa sats within 2 hours of fctSeconds
bGotGpsEph = false;
if exist(fullEphFilename,'file')==2
    %% file exists locally, so read it
    fprintf('Reading GPS ephemeris from ''%s'' file in local directory\n',...
        ephFilename);
    fprintf('%s\n',dirName);
    allGpsEph = ReadRinexNav(fullEphFilename);
    [~,fctSeconds] = Utc2Gps(utcTime);
    ephAge = [allGpsEph.GPS_Week]*GpsConstants.WEEKSEC + [allGpsEph.Toe] - ...
        fctSeconds;
    %get index into fresh and healthy ephemeris (health bit set => unhealthy)
    iFreshAndHealthy = abs(ephAge)<GpsConstants.EPHVALIDSECONDS & ...
        ~[allGpsEph.health];
    %TBD look at allEph.Fit_interval, and deal with values > 0
    goodEphSvs = unique([allGpsEph(iFreshAndHealthy).PRN]);
    if length(goodEphSvs)>=GnssThresholds.MINNUMGPSEPH
        bGotGpsEph = true;
    end
end

if ~bGotGpsEph
    %% get ephemeris from Nasa site
    ftpSiteName = 'cddis.gsfc.nasa.gov';
    hourlyDir=sprintf('/gnss/data/hourly/%4d/%03d/',yearNumber4Digit,dayNumber);
    fprintf('\nGetting GPS ephemeris ''%s'' from NASA ftp site ...',hourlyZFile)
    fprintf('\nftp://%s%s\n',ftpSiteName,hourlyDir);

    %check that the dirName has been properly specified
    if strfind(dirName,'~')
        fprintf('\nYou set: dirName = ''%s''\n',dirName)
        fprintf('To download ephemeris from ftp,')
        fprintf(' specify ''dirName'' with full path, and no tilde ''~''\n')
        fprintf('Change ''dirName'' or download ephemeris file ''%s'' by hand\n',...
        hourlyZFile);
        fprintf('To unzip the *.Z file, see http://www.gzip.org/\n')
        return
    end
    
    try
        nasaFtp=ftp(ftpSiteName); %connect to ftp server and create ftp object
        cd(nasaFtp,hourlyDir);
        zF = mget(nasaFtp,hourlyZFile,dirName); %get compressed (.Z) file
    catch
      %failed automatic ftp, ask user to do this by hand
      fprintf('\nAutomatic ftp download failed.\n')
      fprintf('Please go to this ftp, ftp://cddis.gsfc.nasa.gov/ \n')
      fprintf('and get this file:%s%s \n',hourlyDir,hourlyZFile);
      fprintf('Extract contents to the directory with your pseudorange log file:\n')
      fprintf('%s\n',dirName)
      fprintf('To unzip the *.Z file, see http://www.gzip.org/\n')
      fprintf('Then run this function again, it will read from that directory\n')
      return
    end
    
    %Now we have the zipped file from nasa. Unzip it:
    unzipCommand='uncompress';%edit if your platform uses something different 
    if any(strfind(computer,'PCWIN'))
        unzipCommand='gunzip';
    end
    s = sprintf('%s "%s"',unzipCommand,zF{1}); %string for system command
    [sysFlag,result] = system(s);
    if any(sysFlag)
        fprintf('\nError in GetNasaHourlyEphemeris.m\n')
        fprintf('%s',result);
        fprintf('System command ''%s'' failed\n',unzipCommand)
        fprintf('Unzip contents of %s by hand. See http://www.gzip.org/\n',zF{1})
        fprintf('Then run this function again, it will read the uncompressed file\n')
        return
    end
    
    bOk = bOk && ~sysFlag;
    if bOk
        fprintf('\nSuccessfully downloaded ephemeris file ''%s''\n',ephFilename)
        if ~isempty(dirName)
            fprintf('to: %s\n',dirName);
        end
    else
    end
end
allGpsEph = ReadRinexNav(fullEphFilename);

end %end of function GetNasaHourlyEphemeris
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function [bOk,dirName] = CheckInputs(utcTime,dirName)
%% check we have the utcTime and right kind of dirName
if any(size(utcTime)~=[1,6])
    error('utcTime must be a (1x6) vector')
end
if isempty(dirName), return, end

bOk = true;
if ~exist(dirName,'dir')
    bOk = false;
    fprintf('Error: directory ''%s'' not found\n',dirName);
    if any(strfind(computer,'PCWIN')) && any(strfind(dirName,'/'))
        fprintf('Looks like you''re using a PC, be sure your directory is specified with back-slashes ''\\''\n');
    end
else
    % check if there is a slash at the end of dirName
    %decide what type of slash to use:
    slashChar = '/'; %default
    if any(strfind(dirName,'\'))
        slashChar = '\';
    end
    if dirName(end)~=slashChar
        dirName = [dirName,slashChar]; %add slash
    end
end


end %end of function CheckInputs
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

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

