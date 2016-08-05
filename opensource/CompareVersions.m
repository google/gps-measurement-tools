function s = CompareVersions(v,w)
%s = CompareVersions(v,w);
%compares version v to w
% s = 'before','equal','after'
% 'before <=> version v is before version w
%
% v and w must be vectors of the same length
%
% example: v = [1,0,0,0]; w = [1,4,0,0]; s = 'before'

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements

if length(v)~=length(w)
    error('The two inputs must be scalars or vectors of the same length')
end

d = v(:)-w(:);
%find first d that differs from zero
i = find(d~=0);
if isempty(i)
    s = 'equal';
elseif d(i(1)) < 0 %the first field that differs is the most significant
    s = 'before';
else
    s = 'after';
end
    
end %end function CompareVersions
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
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

