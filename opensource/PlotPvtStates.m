function PlotPvtStates(gnssPvt,prFileName)
%PlotPvtStates(gnssPvt,prFileName);
%Plot the Position, Velocity and Time/clock states in gnssPvt
%
% gnssPvt.FctSeconds    Nx1 time vector, same as gpsMeas.FctSeconds
%       .allLlaDegDegM Nx3 matrix, (i,:) = [lat (deg), lon (deg), alt (m)]
%       .sigmaLlaM     Nx3 standard deviation of [lat,lon,alt] (m)
%       .allBcMeters   Nx1 common bias computed with lla
%       .allVelMps     Nx3 (i,:) = velocity in NED coords
%       .sigmaVelMps   Nx3 standard deviation of velocity (m/s)
%       .allBcDotMps   Nx1 common freq bias computed with velocity

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements

%find median values
iFi = isfinite(gnssPvt.allLlaDegDegM(:,1));%index into finite results
if ~any(iFi)
    return
end
llaMed = median(gnssPvt.allLlaDegDegM(iFi,:));%use medians as reference

%time variable for plots
tSeconds = gnssPvt.FctSeconds-gnssPvt.FctSeconds(1);

h1234 = zeros(1,4); %handles for subplots
%plot ned errors vs medians ----------------------------------------------------
ned = Lla2Ned(gnssPvt.allLlaDegDegM,llaMed);
h1234(1)=subplot(4,1,1);
plot(tSeconds,ned(:,1),'r');hold on
plot(tSeconds,ned(:,2),'g');
plot(tSeconds,ned(:,3),'b');
title('WLS: Position states offset from medians [Lat,Lon,Alt]');
grid on, ylabel('(meters)'),

%label Latitude, Longitude and Altitude
iFi = isfinite(ned(:,1));%index into finite results
h=zeros(1,3); %handles for Lat, Lon, Alt
h(1)=text(tSeconds(end),ned(iFi(end),1),'Lat');
set(h(1),'Color','r')
h(2)=text(tSeconds(end),ned(iFi(end),2),'Lon');
set(h(2),'Color','g')
h(3)=text(tSeconds(end),ned(iFi(end),3),'Alt');
set(h(3),'Color','b')
%shift the highest a little higher, so it doesnt overwrite the others
[~,iMax] = max(ned(iFi(end),:));
set(h(iMax),'VerticalAlignment','bottom');
%shift the lowest a little lower
[~,iMin] = min(ned(iFi(end),:));
set(h(iMin),'VerticalAlignment','top');

%plot common bias, in microseconds and meters ---------------------------------
h1234(2)=subplot(4,1,2);
iFi = find(isfinite(gnssPvt.allBcMeters));%index into finite results
if any(iFi)
    plot(tSeconds,gnssPvt.allBcMeters - gnssPvt.allBcMeters(iFi(1)))
    grid on
    bc0Microseconds = gnssPvt.allBcMeters(iFi(1))/GpsConstants.LIGHTSPEED*1e6;
    bc0Text = sprintf('%.1f',bc0Microseconds);
    title(['Common bias ''clock'' offset from initial value of ',...
        bc0Text,' {\mu}s'])
    ylabel(('meters'))
end

%add microseconds label on right
ax=axis;
axMeters = ax(3:4);
set(gca,'YLim',axMeters); %fix this, else rescaling fig breaks axis proportion
axMicroseconds = axMeters/GpsConstants.LIGHTSPEED*1e6;
set(gca,'Box','off');   %# Turn off the box surrounding the whole axes
axesPosition = get(gca,'Position');          %# Get the current axes position
hNewAxes = axes('Position',axesPosition,...  %# Place a new axes on top...
                'Color','none',...           %#   ... with no background color
                'YLim',axMicroseconds,...    %#   ... and a different scale
                'YAxisLocation','right',...  %#   ... located on the right
                'XTick',[],...               %#   ... with no x tick marks
                'Box','off');                %#   ... and no surrounding box
ylabel(hNewAxes,'(microseconds)');  %# Add label to right
%you must link the axes, else proportion will change when you scale figure:
linkaxes([hNewAxes, h1234(2)],'x'); 

%plot three components of speed ------------------------------------------------
h1234(3)=subplot(4,1,3);
vel = gnssPvt.allVelMps;
plot(tSeconds,vel(:,1),'r');hold on
plot(tSeconds,vel(:,2),'g');
plot(tSeconds,vel(:,3),'b');
title('Velocity states [North,East,Down]');
grid on, ylabel('(m/s)'),

%label North, East, Down
iFi = isfinite(vel(:,1));%index into finite results
h=zeros(1,3); %handles for Lat, Lon, Alt
h(1)=text(tSeconds(end),vel(iFi(end),1),'North');
set(h(1),'Color','r')
h(2)=text(tSeconds(end),vel(iFi(end),2),'East');
set(h(2),'Color','g')
h(3)=text(tSeconds(end),vel(iFi(end),3),'Down');
set(h(3),'Color','b')
%shift the highest a little higher, so it doesnt overwrite the others
[~,iMax] = max(vel(iFi(end),:));
set(h(iMax),'VerticalAlignment','bottom');
%shift the lowest a little lower
[~,iMin] = min(vel(iFi(end),:));
set(h(iMin),'VerticalAlignment','top');

%plot common frequency offset -------------------------------------------------
h1234(4)=subplot(4,1,4);
plot(tSeconds,gnssPvt.allBcDotMps)
grid on
title('Common frequency offset'); ylabel(('m/s'))

%add microseconds label on right
ax=axis;
axMps = ax(3:4);
set(gca,'YLim',axMps);%fix this, else rescaling fig breaks axis proportion
PPMPERMPS = 1/GpsConstants.LIGHTSPEED*1E6; %true for any frequency
axPpm = axMps*PPMPERMPS;
set(gca,'Box','off');   %# Turn off the box surrounding the whole axes
axesPosition = get(gca,'Position');          %# Get the current axes position
hNewAxes = axes('Position',axesPosition,...  %# Place a new axes on top...
                'Color','none',...           %#   ... with no background color
                'YLim',axPpm,...             %#   ... and a different scale
                'YAxisLocation','right',...  %#   ... located on the right
                'XTick',[],...               %#   ... with no x tick marks
                'Box','off');                %#   ... and no surrounding box
ylabel(hNewAxes,'(ppm)');  %# Add label to right
linkaxes([hNewAxes, h1234(4)],'x')

xs = sprintf('\ntime(seconds)\n%s',prFileName);
xlabel(xs,'Interpreter','none')

linkaxes(h1234,'x'); %link all x axes

end %end of function PlotPvtStates
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

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
