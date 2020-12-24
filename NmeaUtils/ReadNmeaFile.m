function [nmea,Msg] = ReadNmeaFile(dirName,fileName,Msg)
% [nmea,Msg] = ReadNmeaFile(dirName,fileName,[Msg]);
%
% Reads GGA and RMC data from a NMEA file.

% This function recognizes GGA data with quality of
% GPS fix (1)/RTK(4)/Float RTK(5)/Manual input(7) only.

% Output:
% nmea: structure array with one element per epoch.
% nmea(i).Gga,
% nmea(i).Rmc
%
% Each field of Gga, Rmc is a scalar => easy to extract vectors later.
%
% $--GGA,hhmmss.ss,llll.ll,a,yyyyy.yy,a,x,xx,x.x,x.x,M,x.x,M,x.x,xxxx*hh<CR><LF>
%        UTC       lat       lon   q num hdop altAboveMsl geoidHeight ...
% Gga.Time   Day fraction
%    .LatDeg Latitude in degrees
%    .LonDeg Longitude in degrees
%    .AltM   Altitude above Ellipsoid in meters
%    .NumSats = Number of satellites in use
%    .Hdop
%    .GeoidM = Gedoidal separation, "-" = mean-sea-level below ellipsoid
%
% $--RMC,hhmmss.sss,x,llll.ll,a,yyyyy.yy,a,x.x,x.x,xxxxxx,x.x,a,a*hh<CR><LF>
% Rmc.Datenum Date and Time = MATLAB datenum, use datevec(Rmc.Datenum) to get
%                             1x6 date and time vector [yyy mm dd hh mm ss.s]
%    .LatDeg Latitude in degrees
%    .LonDeg Longitude in degrees
%    .TrackDeg (degrees from true north)
%    .SpeedKnots (knots)
%
% Release 2.3 of NMEA added a new field added to RMC just prior to the checksum,
% 'Mode': A=autonomous, D=differential, E=estimated, N=Data not valid.
%
% Msg, cell array of strings with status
%
% TBD
%        .Gsv
%
% TBD remaining fields that are sometimes empty: magvar, valid, status, etc
%
% Examples from https://www.gpsinformation.org/dale/nmea.htm
% GGA Example,
% $GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47
% Where:
%      GGA          Global Positioning System Fix Data
%      123519       Fix taken at 12:35:19 UTC
%      4807.038,N   Latitude 48 deg 07.038' N
%      01131.000,E  Longitude 11 deg 31.000' E
%      1            Fix quality:
%                   0 = invalid
%                   1 = GPS fix (SPS)
%                   2 = DGPS fix
%                   3 = PPS fix
%                   4 = Real Time Kinematic
%                   5 = Float RTK
%                   6 = estimated (dead reckoning) (2.3 feature)
%                   7 = Manual input mode
%                   8 = Simulation mode
%      08           Number of satellites being tracked
%      0.9          Horizontal dilution of position
%      545.4,M      Altitude, Meters, above mean sea level
%      46.9,M       Height of geoid (mean sea level) above WGS84 ellipsoid
%      (empty field) time in seconds since last DGPS update
%      (empty field) DGPS station ID number
%      *47          the checksum data, always begins with *
%
% RMC example,
% $GPRMC,123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A
%
% Where:
%      RMC          Recommended Minimum sentence C
%      123519       Fix taken at 12:35:19 UTC
%      A            Status A=active or V=Void.
%      4807.038,N   Latitude 48 deg 07.038' N
%      01131.000,E  Longitude 11 deg 31.000' E
%      022.4        Speed over the ground in knots
%      084.4        Track angle in degrees True
%      230394       Date - 23rd of March 1994
%      003.1,W      Magnetic Variation
%      *6A          The checksum data, always begins with *
%
%% Initialize fields
if nargin<3
    Msg = {};
end
nmea = [];
nmeaGga = struct('Time',{},'LatDeg',{},'LonDeg',{},'AltM',{},'NumSats',{},...
    'Hdop',{},'GeoidM',{});
nmeaRmc = struct('Datenum',{},'LatDeg',{},'LonDeg',{},...
    'TrackDeg',{},'SpeedKnots',{});

%define NaN fields for missing nmea messages
nanGga.Time=NaN; nanGga.LatDeg=NaN; nanGga.LonDeg=NaN; nanGga.AltM=NaN;
nanGga.NumSats=NaN; nanGga.Hdop=NaN; nanGga.GeoidM=NaN;

nanRmc.Datenum=NaN; nanRmc.LatDeg=NaN; nanRmc.LonDeg=NaN; nanRmc.TrackDeg=NaN;
nanRmc.SpeedKnots=NaN;

% Define recognized GGA fix qualities. Only lines with these qualities will be
% parsed as GGA data.
knownGgaQualities = [GgaQuality.GPS, GgaQuality.RTK,...
    GgaQuality.FLOAT_RTK, GgaQuality.MANUAL];

%% read file and call parsers
fid = fopen(fullfile(dirName,fileName),'r');
if fid<0
    Msg{end+1} = 'Error: file not found';
    return
end
line = 'x';%initialize to some character
while ischar(line)
   line = fgetl(fid);
   %% remove characters before '$'
   if ~ischar(line) || ~contains(line,'$')
       continue
   else
       iDollar = strfind(line,'$');
       line = line(iDollar(1):end);
   end

   %% parse nmea lines
   [nmeaField,fieldType,quality] = parseGga(line);
   if ~isempty(fieldType)
       if any(knownGgaQualities(:) == quality)
           nmeaGga(end+1) = nmeaField; %#ok<AGROW>
       end
       continue
   end
   %TBD:
   %if isempty(fieldType)
   %[nmeaField,fieldType] = parseGsv(line);
   %end
   [nmeaField,fieldType] = parseRmc(line);
   if ~isempty(fieldType)
       nmeaRmc(end+1) = nmeaField; %#ok<AGROW>
       continue
   end
end
fclose(fid);
if isempty(line) %line should be -1 at eof
    Msg{end+1} = 'Error occurred while reading file';
    nmea = [];
    return
end

%% Pack all matching time tags:
N = length(nmeaGga); %make one nmea structure for each Gga field
if N==0
    Msg{end+1} = sprintf('Found no recognized GGA data in %s.', fileName);
    return
end
if isempty(nmeaRmc)
    Msg{end+1} = sprintf('Found no RMC data in %s.', fileName);
    return
end
nmea.Gga = nmeaGga(1);
nmea.Rmc = nmeaRmc(1);
nmea = repelem(nmea,N);
for i=1:N
    nmea(i).Gga = nmeaGga(i);
    ti = nmea(i).Gga.Time;
    iR = find(ti == rem([nmeaRmc.Datenum],1));
    if ~isempty(iR)
        nmea(i).Rmc = nmeaRmc(iR(1));
    else
        nmea(i).Rmc=nanRmc;
    end
end

end %end of function ReadNmeaFile
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function [Gga,fieldType,quality] = parseGga(line)
%% parse a gga message from the string 'line'
%      $--GGA,hhmmss.ss,llll.ll,a,yyyyy.yy,a,x,xx,x.x,x.x,M,x.x,M,x.x,xxxx*hh
%format: %s   %s        %s     %s %s      %s %d %d %f %f %s %f %s %f   %d %s
%fields: 1    2         3      4  5       6  7  8  9  10 11 12 13 14   15 16
%             UTC       lat       lon       qual
%                                8=num, 9=hdop, 10=altAboveMsl, 12=geoidHeight
Gga = ''; fieldType = ''; quality=-1;
line = upper(line);
if ~contains(line,'GGA,')
    return
end
fieldType = 'Gga';
line = strrep(line,'*',','); %replace '*' with ','
C = textscan(line,'%s %s %s %s %s %s %d %d %f %f %s %f %s %f %d %s',...
    'Delimiter',',','EmptyValue',NaN);
if ~isempty(C{2}{1})
    hhmmss = sprintf('%010.3f',str2double(C{2}{1}));%hours mins secs, to 3 digits
    %this allows us to capture the milliseconds in datenum:
    d = datenum(hhmmss,'HHMMSS.FFF');
    Gga.Time = d-floor(d);
else
    Gga.Time = NaN;
end
ddmm = C{3}{1}; %deg and minutes
if isempty(ddmm)
    Gga.LatDeg = NaN;
else
    assert(strfind(ddmm,'.')==5,...
        'GGA Latitude not defined correctly, should be ddmm.m');
    Gga.LatDeg  = str2double(ddmm(1:2)) + (str2double(ddmm(3:end))/60);
    if strcmp(C{4}{1},'S')
        Gga.LatDeg = -Gga.LatDeg;
    end
end
dddmm = C{5}{1}; %deg and minutes
if isempty(dddmm)
    Gga.LonDeg = NaN;
else
    assert(strfind(dddmm,'.')==6,...
        'GGA Longitude not defined correctly, should be dddmm.m');
    Gga.LonDeg  = str2double(dddmm(1:3)) + (str2double(dddmm(4:end))/60);
    if strcmp(C{6}{1},'W')
        Gga.LonDeg = -Gga.LonDeg;
    end
end
Gga.AltM = C{10}+C{12}; %height above the ellipsoid (m)
quality = C{7}; %quality - identifies the source of this location
Gga.NumSats = C{8}; % Number of satellites in use
Gga.Hdop    = C{9};  % HDOP
Gga.GeoidM  = C{12}; % Gedoidal separation,"-" = mean-sea-level below ellipsoid
%leave the following out for now, to keep things simple:
% Gga.DiffAgeS = C{14}; %Age of Differental data
% Gga.DiffRefId= C{15}; % Differential reference station ID

end %end of function parseGga
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function [Rmc,fieldType] = parseRmc(line)
%% parse an rmc message from the string 'line'
%     $--RMC,hhmmss.sss,x,llll.ll,a,yyyyy.yy,a,x.x,x.x,xxxxxx,x.x,a,a*hh
%format: %s   %s       %s  %s     %s %s     %s %f  %f  %s    %f  %s %s %s
%fields: 1    2        3   4      5  6      7  8   9   10    11  12 13 14

%example:
%     $GPRMC,123519.0,A,4807.038,N,01131.000,E,022.4,084.4,230394,3.1,E,A*0B
% fields   1 2        3  4       5  6        7    8     9     10 11 12 13 14
%           hhmmss.s status lat      lon        speed track ddmmyy magvar mode

Rmc = ''; fieldType = '';
line = upper(line);
if ~contains(line,'RMC,')
    return
end
fieldType = 'Rmc';
line = strrep(line,'*',','); %replace '*' with ','
C = textscan(line,'%s %s %s %s %s %s %s %f %f %s %f %s %s %s',...
    'Delimiter',',','EmptyValue',NaN);
if ~isempty(C{2}{1}) && ~isempty(C{10}{1})
    hhmmss = sprintf('%010.3f',str2double(C{2}{1})); %hrs mins secs, to 3 digits
    %this allows us to capture the milliseconds in datenum:
    ddmmyy = C{10}{1}; %day month year
    Rmc.Datenum = datenum([ddmmyy,hhmmss],'ddmmyyHHMMSS.FFF');
else
    Rmc.Datenum = NaN;
end

ddmm = C{4}{1}; %deg and minutes
if isempty(ddmm)
    Rmc.LatDeg = NaN;
else
    assert(strfind(ddmm,'.')==5,...
        'RMC Latitude not defined correctly, should be ddmm.m');
    Rmc.LatDeg  = str2double(ddmm(1:2)) + (str2double(ddmm(3:end))/60);
    if strcmp(C{5}{1},'S')
        Rmc.LatDeg = -Rmc.LatDeg;
    end
end
dddmm = C{6}{1}; %deg and minutes
if isempty(dddmm)
    Rmc.LonDeg = NaN;
else
    assert(strfind(dddmm,'.')==6,...
        'RMC Longitude not defined correctly, should be dddmm.m');
    Rmc.LonDeg  = str2double(dddmm(1:3)) + (str2double(dddmm(4:end))/60);
    if strcmp(C{7}{1},'W')
        Rmc.LonDeg = -Rmc.LonDeg;
    end
end

trackDeg = C(9); %track in degrees from true North
Rmc.TrackDeg = trackDeg{1};

speedKnots = C(8); %speed in knots
Rmc.SpeedKnots = speedKnots{1};

end %end of function parseRmc
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

