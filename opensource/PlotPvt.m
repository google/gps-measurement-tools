function PlotPvt(gpsPvt,prFileName,llaTrueDegDegM,titleString)
%PlotGpsPvt(gpsPvt,prFileName,[llaTrueDegDegM],[titleString])
%Plot the results of GpsLsPvt:
%
% gpsPvt.FctSeconds    Nx1 time vector, same as gpsMeas.FctSeconds
%       .allLlaDegDegM Nx3 matrix, (i,:) = [lat (deg), lon (deg), alt (m)]
%       .sigmaLlaM     Nx3 standard deviation of [lat,lon,alt] (m)
%       .allBcMeters   Nx1 common bias computed with lla
%       .allVelMps     Nx3 (i,:) = velocity in NED coords
%       .sigmaVelMps   Nx3 standard deviation of velocity (m/s)
%       .allBcDotMps   Nx1 common freq bias computed with velocity
%       .numSvs        Nx1 number of satellites used in corresponding lla
%       .hdop          Nx1 hdop of corresponding fix
%
% Optional inputs: [llaTrueDegDegM] = reference position, [ts] = title srtring

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements

gray   = [.5 .5 .5];
ltgray = [.8 .8 .8];

%set reference lla for plots
iFi = isfinite(gpsPvt.allLlaDegDegM(:,1));%index into finite results
if ~any(iFi)
    return
end
llaMed = median(gpsPvt.allLlaDegDegM(iFi,:));%median position
%print median lla so user can use it as reference position if they want:
fprintf('Median llaDegDegM = [%.7f %.7f %.2f]\n',llaMed)

if nargin < 3, llaTrueDegDegM = []; end
if nargin < 4, titleString = 'PVT solution'; end
bGotLlaTrue = ~isempty(llaTrueDegDegM) && any(llaTrueDegDegM);
%not empty and not all zeros
if bGotLlaTrue
    llaRef = llaTrueDegDegM;
else
    llaRef = llaMed;
end

%% plot ne errors vs llaTrueDegDegM --------------------------------------------
nedM = Lla2Ned(gpsPvt.allLlaDegDegM,llaRef);%keep the NaNs in for the plot
%so we see a break in the lines where there was no position
h123=subplot(4,1,1:2);
h1 = plot(nedM(:,2),nedM(:,1));
set(h1,'LineStyle','-','LineWidth',0.1,'Color',ltgray)
hold on, plot(nedM(:,2),nedM(:,1),'cx'); 
lls = sprintf(' [%.6f^o, %.6f^o]',llaMed(1:2));
nedMedM = Lla2Ned(llaMed,llaRef);
h=plot(nedMedM(2),nedMedM(1),'+k','MarkerSize',18);
ts = ['  median  ',lls];
ht1 = text(nedMedM(2),nedMedM(1),ts,'color','k');
lls = sprintf(' [%.6f^o, %.6f^o]',llaRef(1:2));
if bGotLlaTrue
    h=plot(0,0,'+r','MarkerSize',18);
    ts = [' true pos ',lls];
    ht2 = text(0,0,ts,'color','r');
    %adjust VerticalAlignment to avoid overwriting previous text
    if nedMedM(1)<0
        set(ht1,'VerticalAlignment','top');%moves the 'median' label  down
        set(ht2,'VerticalAlignment','bottom');
    else
        set(ht1,'VerticalAlignment','bottom');
        set(ht2,'VerticalAlignment','top');
    end
    %print median position error
    ts = sprintf('|median - true pos| = %.1f m',norm(nedMedM(1:2)));
    xm = min(0,nedMedM(2)); %align label with median x value, or zero
    ym = min(nedM(:,1)); %align with smallest y
    ht3 = text(xm,ym,ts);
    set(ht3,'Color','k','VerticalAlignment','bottom');
end
title(titleString);
axis equal, grid on
ylabel('North (m)'),xlabel('East (m)')
% compute error distribution and plot circle
distM = sqrt(sum(nedM(iFi,1:2).^2,2));%use only finite values here
medM = median(distM);
%plot a circle using 'rectangle' (yes really :)
hr=rectangle('Position',[-1 -1 2 2]*medM,'Curvature',[1 1]);
set(hr,'EdgeColor',gray)
ts = sprintf('50%% distribution = %.1f m',medM);
text(0,medM,ts,'VerticalAlignment','bottom','Color',gray)
%% end of plot ne errors vs llaTrueDegDegM -------------------------------------

%time variable for plots
tSeconds = gpsPvt.FctSeconds-gpsPvt.FctSeconds(1);

%% plot speed
h123(2)=subplot(4,1,3);
N = length(gpsPvt.FctSeconds);
iGood = isfinite(gpsPvt.allVelMps(:,1));
speedMps = zeros(1,N)+NaN;
speedMps(iGood) = sqrt(sum(gpsPvt.allVelMps(iGood,1:2)'.^2)); %horizontal speed
plot(tSeconds,speedMps);grid on
ylabel('Horiz. speed (m/s)')

%% plot hdop & # sats
h123(3)=subplot(4,1,4);
[hyy,h1]=plotyy(tSeconds,gpsPvt.hdop,tSeconds,gpsPvt.numSvs,'plot','stairs');
grid on
set(h1,'LineWidth',1)
ylabel(hyy(1),'HDOP'); ylabel(hyy(2),'# sats'); 
xs = sprintf('time(seconds)\n%s',prFileName);
xlabel(xs,'Interpreter','none')

set(hyy,'Box','off')

linkaxes(h123(2:3),'x');
linkaxes(hyy,'x')


end %end of function PlotPvt
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
