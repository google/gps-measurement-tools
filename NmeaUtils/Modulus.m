function z = Modulus(x,y,def)
% z = Modulus(x,y,[def])                       
%
% z = modulus after division, with four definitions
% def:
% 1: z = x-floor(x/y)*y, z in the range [0,y)
% 2: z = x-round(x/y)*y, z in the range [-y/2,y/2]
% 3: z = x-fix(x/y)*y,   z in the range (-y,y)
% 4: z = x-ceil(x/y)*y,  z in the range (-y,0]
%
% Inputs:
%   x: scalar, vector or matrix; 
%   y: scalar
%   [def] (optional) is the definition [default=1]
% Output:
%   z has same dimensions as x
%
% in all cases z differs from x by an integer number of y
%
% Notes: 
%  Modulus(x,y,[1]) = mod(x,y) (MATLAB mod function)
%  By convention  Modulus(x,0) is x.
%
% see also: mod, rem
assert(all(size(y) == 1),...
    'Second input is not a scalar, should never happen');
if nargin<3, 
    def=1; 
else
    assert(any(def == [1,2,3,4]),...
        'Input ''def'' is out of range, should never happen');
end

if (y==0)
    z = x;
else
    switch def
        case 1
            z = x - floor(x/y)*y;
        case 2
            z = x - round(x/y)*y;
        case 3
            z = x - fix(x/y)*y;
        case 4
            z = x - ceil(x/y)*y;
    end
end

end %end of function Modulus
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

