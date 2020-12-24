function [brgRad,brgDeg] = Ne2Brg(n,e)
%[brgRad,brgDeg] = Ne2Brg(n,e);
%
% convert North and East coorindates to bearing
%
% Inputs
%   n,e: North and East components. Scalar, vector or matrix of any size, but
%     matching each other
%
% Outputs
%   brgRad,brgDeg bearing clockwise from North, in Radians and Degrees, 
%     matching the size of inputs
%
% brgRad in the range [0,2*pi)
% brgDeg in the range [0,360)

assert(isequal(size(n),size(e)),'Size of inputs n and e must be the same')

brgRad = pi/2-atan2(n,e);
brgRad = Modulus(brgRad,2*pi,1);
brgDeg = brgRad*180/pi;

end %end of function
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%