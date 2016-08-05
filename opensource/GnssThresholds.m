classdef GnssThresholds
    % GNSS validity thresholds we use in the code

    %Author: Frank van Diggelen
    %Open Source code for processing Android GNSS Measurements
   
    properties (Constant) %listed alphabetically
        MAXDELPOSFORNAVM = 20; %maximum position can change on one iteration of 
        % nav solution without los vector changing by more than 1 microradian
        MAXPRRUNCMPS = 10; %max pseudorange rate (Doppler) uncertainty.
        %bigger values may just be the search bin size, thus not valid for nav.
        MAXTOWUNCNS = 500; %maximum Tow uncertainty, 500 ns. Satellite range 
        %can change by about half a millimeter in this time
        MINNUMGPSEPH = 24; %minimum number of GPS ephemeris considered OK
    end
end
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



