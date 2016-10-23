function dataFilter = SetDataFilter
% dataFilter = SetDataFilter;
% Function to set data filter for use with ReadGnssLogger
% This function has no inputs. Edit it directly to change the data filter
%
% Rule for setting dataFilter: see CheckDataFilter.m

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements

%filter out FullBiasNanos == 0
dataFilter{1,1} = 'FullBiasNanos'; 
dataFilter{1,2}   = 'FullBiasNanos ~= 0'; 

%you can create other filters in the same way ...
%for example, suppose you want to remove Svid 23:
% dataFilter{end+1,1} = 'Svid'; 
% dataFilter{end,2}   = 'Svid ~= 23';
% 
%or suppose you want to keep only Svid 2,5,10, & 17
% dataFilter{end,2} = 'Svid==2 | Svid==5 | Svid==10 | Svid==17';
% NOTE: you *cannot* use 'any(Svid)==[2,5,10,17]' because Svid refers to a 
% vector variable and you must compare it to a scalar.

%filter for fine time measurements only  <=> uncertainty < 10 ms = 1e7 ns
%For Nexus 5x and 6p this field is not filled, so comment out these next 2 lines
% dataFilter{end+1,1} = 'BiasUncertaintyNanos'; 
% dataFilter{end,2} = 'BiasUncertaintyNanos < 1e7'; 

%keep only Svid 2
% dataFilter{end+1,1} = 'Svid'; 
% dataFilter{end,2}   = 'Svid==2';

%limit to GPS only:
dataFilter{end+1,1} = 'ConstellationType'; 
dataFilter{end,2}   = 'ConstellationType==1'; 
%ConstellationType values are defined in Android HAL Documentation, gps.h, 
%   typedef uint8_t                         GnssConstellationType;
%   #define GNSS_CONSTELLATION_UNKNOWN      0
%   #define GNSS_CONSTELLATION_GPS          1
%   #define GNSS_CONSTELLATION_SBAS         2
%   #define GNSS_CONSTELLATION_GLONASS      3
%   #define GNSS_CONSTELLATION_QZSS         4
%   #define GNSS_CONSTELLATION_BEIDOU       5
%   #define GNSS_CONSTELLATION_GALILEO      6

%Example of how to select satellites from GPS and GLONASS:
% dataFilter{end+1} = '(ConstellationType)==1 | (ConstellationType)==3'; 
%You may use the heading value e.g. '(ConstellationType)' more than once,
%so long as you dont use different heading types in one dataFilter{} entry

%bitwise data filters
%some fields are defined bitwise, including: State, AccumulatedDeltaRangeState
%
% GnssMeasurementState values are defined in Android HAL Documentation, gps.h, 
%   typedef uint32_t GnssMeasurementState;
%   #define GNSS_MEASUREMENT_STATE_UNKNOWN                   0
%   #define GNSS_MEASUREMENT_STATE_CODE_LOCK             (1<<0)
%   #define GNSS_MEASUREMENT_STATE_BIT_SYNC              (1<<1)
%   #define GNSS_MEASUREMENT_STATE_SUBFRAME_SYNC         (1<<2)
%   #define GNSS_MEASUREMENT_STATE_TOW_DECODED           (1<<3)
%   #define GNSS_MEASUREMENT_STATE_MSEC_AMBIGUOUS        (1<<4)
%   #define GNSS_MEASUREMENT_STATE_SYMBOL_SYNC           (1<<5)
%   #define GNSS_MEASUREMENT_STATE_GLO_STRING_SYNC       (1<<6)
%   #define GNSS_MEASUREMENT_STATE_GLO_TOD_DECODED       (1<<7)
%   #define GNSS_MEASUREMENT_STATE_BDS_D2_BIT_SYNC       (1<<8)
%   #define GNSS_MEASUREMENT_STATE_BDS_D2_SUBFRAME_SYNC  (1<<9)
%   #define GNSS_MEASUREMENT_STATE_GAL_E1BC_CODE_LOCK    (1<<10)
%   #define GNSS_MEASUREMENT_STATE_GAL_E1C_2ND_CODE_LOCK (1<<11)
%   #define GNSS_MEASUREMENT_STATE_GAL_E1B_PAGE_SYNC     (1<<12)
%   #define GNSS_MEASUREMENT_STATE_SBAS_SYNC             (1<<13)

%Example of how to use dataFilter for GnssMeasurementState 'State' bit fields: 
%filter on GPS measurements with Code lock & TOW decoded:
dataFilter{end+1,1} = 'State';
dataFilter{end,2}   = 'bitand(State,2^0) & bitand(State,2^3)';
% GNSS_MEASUREMENT_STATE_CODE_LOCK & GNSS_MEASUREMENT_STATE_TOW_DECODED

% GnssAccumulatedDeltaRangeState values are defined gps.h, 
%   typedef uint16_t GnssAccumulatedDeltaRangeState;
%   #define GNSS_ADR_STATE_UNKNOWN                       0
%   #define GNSS_ADR_STATE_VALID                     (1<<0)
%   #define GNSS_ADR_STATE_RESET                     (1<<1)
%   #define GNSS_ADR_STATE_CYCLE_SLIP                (1<<2)
%
%Example of how to use dataFilter for ADR State bit fields: 
% get AccumulatedDeltaRangeState values that are valid
% dataFilter{end+1,1} = 'AccumulatedDeltaRangeState'; 
% dataFilter{end,2}   = 'bitand(AccumulatedDeltaRangeState,1)'; 
end %end of function SetDataFilter
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

