function ek = Kepler(mk,e)

% ek = Kepler(mk,e) 
%    Kepler - Solves Kepler's equation for ek through iteration. 
%                                                                
% Inputs:  mk: mean anomaly (rad)
%           e:  eccentricity
% Outputs: ek: eccentric anomaly
%
% NOTE: mk and e may be a vectors of the same dimensions, or one may be a scalar
%       the output is a vector of the same dimensions as the input vector

%Author: Frank van Diggelen
%Open Source code for processing Android GNSS Measurements
  
%Check inputs size
if min(size(mk))>1
  error('mk must be a vector or a scalar, not a matrix')
end
if min(size(e))>1
  error('e must be a vector or a scalar, not a matrix')
end
if length(mk)>1 && length(e)>1 && any(size(mk)~=size(e)) 
    %both are vectors, they must have the same dimensions
  error('If mk and e are both vectors they must have the same dimensions')
end

err = 1;

ek=mk;
iterCount = 0;
maxIterCount = 20;
while any(abs(err) > 1e-8) && iterCount < maxIterCount
  err = ek - mk - (e.*sin(ek));
  ek = ek - err;
  iterCount = iterCount + 1;
  
  if iterCount == maxIterCount
      fprintf('Failed convergence on Kepler''s equation.\n')
  end
end

end %end of function Kepler
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

