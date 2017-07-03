function [bOk] = CheckDataFilter(dataFilter,header)
% [bOk] = CheckDataFilter(dataFilter,[header])
% Check that dataFilter is defined correctly
%
% Rule for setting dataFilter.
%   dataFilter values must be defined in pairs:
%   dataFilter{i,1} is a string with one of 'Raw' header values from the 
%                    GnssLogger log file e.g. 'ConstellationType'
%   dataFilter{i,2} is a string with a valid matlab expression, containing
%                   the header value, e.g. 'ConstellationType==1'
% Any comparison must be against a scalar value.
% The heading type may be repeated, for example,
% dataFilter{i,2} = 'ConstellationType==1 | ConstellationType==3';
% but you may not have different heading types in a single dataFilter cell
%
% header, [optional input], is  an mx1 cell array of strings containing
% 'Raw' header values from GnssLogger log file
%
% CheckDataFilter checks the consistency of dataFilter.
% if header is provided, then dataFilter is checked against it.

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements

bOk = true;

%% check the basic structure of dataFilter
if isempty(dataFilter)
    return
end
[N,M] = size(dataFilter);
if M~=2 || ~iscell(dataFilter)
    error('dataFilter must be an nx2 cell array\n')
end
for i=1:N
    for j=1:2
        if ~ischar(dataFilter{i,j})
            error('dataFilter{%d,%d} is not a string\n',i,j);
        end
    end
end

%% Check that the value in dataFilter{i,1} occurs in dataFilter{i,2}
for i=1:N
    if ~any(strfind(dataFilter{i,2},dataFilter{i,1}))
        error('dataFilter{%d,1} string, ''%s'' not found in dataFilter{%d,2}\n',...
            i,dataFilter{i,1},i);
    end
end
if nargin<2 || isempty(header)
    return
end

%% check that dataFilter has a matching value in the header, 
for i=1:N
    iMatch = strcmp(dataFilter{i,1},header); %iMatch is logical array
    if ~any(iMatch) %no match found
        error('dataFilter value ''%s'' has no matches in log file header',...
            dataFilter{i,1});
    end
end

%TBD check for occurrence of two different header types in dataFilter{i,2}
% this is  little tricky, because if you use strfind you will find
% supersets of shorter header types: like FullBiasNanos and BiasNanos
% so you have to take care of that.
% Also, this is spoon feeding the user - if they follow the rules defined
% above, they won't do this. So, we leave it for now and add later when we
% have nothing else to do (ha ha).

end% of function CheckDataFilter
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

% Copyright 2016 Google Inc.
% 
% Licensed under the Apache License, Version 2.0 (the "License");
% you may not use this file except in compliance with the License.
% You may obtain a copy of the License at
% 
%      http://www.apache.org/licenses/LICENSE-2.0
% 
% Unless required by applicable law or agreed to in writing, software
% distributed under the License is distributed on an "AS IS" BASIS,
% WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
% See the License for the specific language governing permissions and
% limitations under the License.
