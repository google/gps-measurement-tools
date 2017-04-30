function [dtsvS] = GpsEph2Dtsv(gpsEph,tS)
%[dtsvS] = GpsEph2Dtsv(gpsEph,tS)
%
% Calculate satellite clock bias
% Inputs:
% gpsEph = vector of GPS ephemeris structures defined in ReadRinexNav
% tS GPS time of week (secs) at time of transmission, calculate this as:
%   tS = trx - PR/c, 
%    where trx = time of reception (receiver clock)
%           PR = pseudorange
%
% tS may be a vector, gpsEph is a structure or vector of structures
% If gpsEph is a vector then t must have the same number of rows as gpsEph
%   tS(i) is interpreted as being associated with gpsEph(i,:)
% If gpsEph is a single structure then tS may be any length
%
% Outputs:
%   dtsvS = sat clock bias  (seconds). GPS time = satellite time - dtsvS
%   length(dtsvS) = length(tS)
%
% See  IS-GPS-200 and RTCM Paper 99-92 SC104-88 for details of data

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements

%% Check size of inputs
if min(size(tS))>1
  error('tS must be a vector or a scalar, not a matrix')
end
tS=tS(:)';%make t a row vector, to match [gpsEph.*] vectors below
pt=length(tS);
[p]=length(gpsEph);
if (p>1 && pt~=p), 
  error('If gpsEph is a vector tS must be a vector with #rows = length(gpsEph),\n')
end
%%
%%Extract the necessary variables from gpsEph 
TGD     = [gpsEph.TGD];
Toc     = [gpsEph.Toc];
af2     = [gpsEph.af2];
af1     = [gpsEph.af1];
af0     = [gpsEph.af0];
Delta_n = [gpsEph.Delta_n];
M0      = [gpsEph.M0];
e       = [gpsEph.e];
Asqrt   = [gpsEph.Asqrt];
Toe     = [gpsEph.Toe];
%%
%%Calculate dependent variables ------------------------------------------------

tk = tS - Toe; %time since time of applicability
I = find(tk > 302400.0);
if any(I),  tk(I) = tk(I)-GpsConstants.WEEKSEC; end,
I = find(tk < -302400.0);
if (I), tk(I) = tk(I)+GpsConstants.WEEKSEC; end,

A = Asqrt.^2;    %semi-major axis of orbit
n0=sqrt(GpsConstants.mu./(A.^3));    %Computed mean motion (rad/sec)
n=n0+Delta_n;      %Corrected Mean Motion
Mk=M0+n.*tk;     %Mean Anomaly
Ek=Kepler(Mk,e);  %Solve Kepler's equation for eccentric anomaly
%%
%%Calculate satellite clock bias (See ICD-GPS-200 20.3.3.3.3.1) ----------------
dt = tS - Toc;
I = find(dt > 302400.0);
if any(I),  dt(I) = dt(I)-GpsConstants.WEEKSEC; end,
I = find(dt < -302400.0);
if (I), dt(I) = dt(I)+GpsConstants.WEEKSEC; end,

dtsvS = af0 + af1.*dt + af2.*(dt.^2)  + ...
    GpsConstants.FREL.*e.*Asqrt.*sin(Ek) -TGD;

end % end of function GpsEph2Dtsv
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

