function [colorsOut]= PlotAdr(gnssMeas,prFileName,colors)
% [colors] = PlotAdr(gnssMeas,[prFileName],[colors])
% plot Valid Accumulated Delta Ranges obtained from ProcessAdr
%
%gnssMeas.FctSeconds = Nx1 vector. Rx time tag of measurements.
%        .ClkDCount  = Nx1 vector. Hw clock discontinuity count
%        .Svid       = 1xM vector of all svIds found in gnssRaw.
%        ...
%        .AdrM       = NxM accumulated delta range (= -k*carrier phase) 
%        .DelPrMinusAdrM = NxM DelPrM - AdrM
%
% Optional inputs: prFileName = string with file name
%                  colors, Mx3 color matrix
%                  if colors is not Mx3, it will be ignored
%
%Output: colors, color matrix, so we match colors each time we plot the same sv

if ~any(any(isfinite(gnssMeas.AdrM) & gnssMeas.AdrM~=0))
    %Nothing in AdrM but NaNs and zeros
    fprintf(' No ADR to plot\n'), return
end
if nargin<2
    prFileName = '';
end

M = length(gnssMeas.Svid);
N = length(gnssMeas.FctSeconds);
if nargin<3 || any(size(colors)~=[M,3])
    colors = zeros(M,3); %initialize color matrix for storing colors
    bGotColors = false;
else
    bGotColors = true;
end
gray = [.5 .5 .5];

timeSeconds =gnssMeas.FctSeconds-gnssMeas.FctSeconds(1);%elapsed time in seconds

for j=1:M %loop over Svid
    %% plot AdrM
    h123(1) = subplot(5,1,1:2); grid on, hold on,
    AdrM = gnssMeas.AdrM(:,j);%local variables for convenience
    AdrState = gnssMeas.AdrState(:,j);

    %From gps.h:
    %/* However, it is expected that the data is only accurate when:
    % *  'accumulated delta range state' == GPS_ADR_STATE_VALID.
    %*/
    % #define GPS_ADR_STATE_UNKNOWN                       0
    % #define GPS_ADR_STATE_VALID                     (1<<0)
    % #define GPS_ADR_STATE_RESET                     (1<<1)
    % #define GPS_ADR_STATE_CYCLE_SLIP                (1<<2)
    iValid = bitand(AdrState,2^0);
    iFi = find(isfinite(AdrM) & iValid);
    if ~isempty(iFi)
        ti = timeSeconds(iFi(end));
        h=plot(timeSeconds,AdrM); set(h,'Marker','.','MarkerSize',4)
        if bGotColors
            set(h,'Color',colors(j,:));
        else
            colors(j,:) = get(h,'Color');
        end
        text(ti,AdrM(iFi(end)),int2str(gnssMeas.Svid(j)),'Color',colors(j,:));
        
        h123(2) = subplot(5,1,3:4); grid on, 
        h=plot(timeSeconds,gnssMeas.DelPrMinusAdrM(:,j)); hold on
        set(h,'Marker','.','MarkerSize',4)
        set(h,'Color',colors(j,:));
    end
end
subplot(5,1,1:2); ax=axis;
title('Valid Accumulated Delta Range (= -k*carrier phase) vs time'), ylabel('(meters)')
subplot(5,1,3:4); set(gca,'XLim',ax(1:2));
title('DelPrM - AdrM'),ylabel('(meters)')

bClockDis = [0;diff(gnssMeas.ClkDCount)~=0];%binary, 1 <=> clock discontinuity

%plot Clock discontinuity
h123(3) = subplot(5,1,5);
iCont = ~bClockDis;%index into where clock is continuous
plot(timeSeconds(iCont),bClockDis(iCont),'.b');%blue dots for continuous
hold on
plot(timeSeconds(~iCont),bClockDis(~iCont),'.r');%red dots for discontinuous

set(gca,'XLim',ax(1:2),'YLim',[-0.5 1.5]); grid on
set(gca,'YTick',[0 1],'YTickLabel',{'continuous ','discontinuous'})
set(gca,'YTickLabelRotation',45)

title('HW Clock Discontinuity')
xs = sprintf('time (seconds)\n%s',prFileName);
xlabel(xs,'Interpreter','none')
linkaxes(h123,'x');

if nargout>1
    colorsOut = colors;
end

end %end of function PlotAdr
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
