function [h1,h2,hError] = Nmea2ErrorPlot(testFileName,refFileName,dirName)
%[h1,h2,hError] = Nmea2ErrorPlot(testFileName,refFileName,[dirName])
%
% Compute horizontal error of positions relative to reference, and plot
%
% Inputs
%   testFileName, refFileName: strings, NMEA files
%     refFileName may be a 1x3 vector [latDeg,lonDeg,altM], in which case
%     this is treated as the stationary reference.
%   [dirName]: (optional) directory where files are,
%     if not provided, then either the file must be in the working
%     directory, or the file names must be full file names (including
%     the directories)
%% read NMEA files, and pack into pvt structure
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
    error('refFileName input must be a character string, or a 1x3 numerical vector')
end
[testNmea, Msg]    = ReadNmeaFile(dirName,testFileName);
fprintf('%s\n', Msg{:});
if isempty(testNmea)
  error('Error reading test file %s', testFileName)
end
testPvt     = Nmea2Pvt(testNmea);

%% Compute horizontal errors.
hError = HorizontalErrorFromPvt(testPvt, refPvt);

figure,
%% plot lines joining Ref to Test
h1 = subplot(211);hold on
X = [hError.TestNeM(:,2), hError.RefNeM(:,2)]';
Y = [hError.TestNeM(:,1), hError.RefNeM(:,1)]';
h=line(X,Y,'color',[.8 .8 .8]);
plot(hError.RefNeM(:,2),hError.RefNeM(:,1),'g','Marker','.');
plot(hError.TestNeM(:,2),hError.TestNeM(:,1),'b','Marker','.');
title(testFileName,'interpreter','none'); %'interpreter','none' for underscores
axis equal


%% Plot cdf, and print 50% 95% 100%
h2 = subplot(212);hold on
iF = isfinite(hError.distM);%decimate to finite results
dM = sort(hError.distM(iF));
h=cdfplot(dM);
ts = sprintf('cdf (%d points)',length(dM));
title(ts)
xlabel('Horizontal error (m)')
ylabel('fraction of distribution')
text(0,0,' Analysis and plots from Android gnsstools/utils/Nmea2ErrorPlot.m',...
    'HorizontalAlignment','left','VerticalAlignment','bottom',...
    'Color',[.5 .5 .5],'FontSize',8);
hold on

dM50 = median(dM);
plot(dM50,.5,'.r')
ts = sprintf(' 50%% %.2fm',dM50);
text(dM50,.5,ts)

dM67 = dM(round(length(dM)*.67));
plot(dM67,.67,'.r')
ts = sprintf(' 67%% %.2fm',dM67);
text(dM67,.67,ts,'VerticalAlignment','Top')

dM95 = dM(round(length(dM)*.95));
plot(dM95,.95,'.r')
ts = sprintf(' 95%% %.2fm',dM95);
text(dM95,.95,ts,'VerticalAlignment','Top')

dM100 = max(dM);
plot(dM100,1,'.r')
ts = sprintf(' %.2fm',dM100);
text(dM100,1,ts,'VerticalAlignment','Top')



end
%end of function Nmea2ErrorPlot
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

