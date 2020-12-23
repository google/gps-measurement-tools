function [nedM] = Lla2Ned(lla1DegDegM,lla2DegDegM)

% [nedM] = Lla2Ned(lla1DegDegM,lla2DegDegM)
% function to difference latitude, longitude and altitude
% and provide an answer in NED coordinates in meters.
%
% Inputs: lla1DegDegM: mx3 matrix, [latitude(deg),longitude(deg),altitude(m)]
%         lla2DegDegM: mx3 or 1x3, [latitude(deg),longitude(deg),altitude(m)]
% Output: nedM =  lla1DegDegM - lla2DegDegM in NED coords (meters), 
%
% Useful rules of thumb for quick conversions: 
% 1e-5 (5th decimal place) of a degree of latitude approx= 1.1 meters
% 1e-5 of a degree of longitude approx= cos(latitude) * 1.1 meters
%
% see also Pvt2Ned

%Author: Frank van Diggelen
assert(nargin == 2, 'Not enough arguments provided, should never happen');

[m1,n1]=size(lla1DegDegM);
[m2,n2]=size(lla2DegDegM);
if m2==1
  lla2DegDegM=ones(m1,1)*lla2DegDegM;
else
  assert(m1 == m2,...
      'Inputs do not have the same number of rows, that should never happen when second input has more than one row');
end
assert(n1 == 3, 'First input does not have 3 columns, should never happen');
assert(n2 == 3, 'Second input does not have 3 columns, should never happen');

[xyz1M] = Lla2Xyz(lla1DegDegM);
[xyz2M] = Lla2Xyz(lla2DegDegM);
refXyz  = (xyz1M+xyz2M)/2;
[llaDegDegM] = Xyz2Lla(refXyz);
northM  = zeros(m1,1);
eastM   = zeros(m1,1);
for i=1:m1
    Ce2n = RotEcef2Ned(llaDegDegM(i,1),llaDegDegM(i,2));
    v = Ce2n*(xyz1M(i,:)-xyz2M(i,:))';
    northM(i)=v(1);
    eastM(i)=v(2);
end
downM = -lla1DegDegM(:,3)+lla2DegDegM(:,3);

nedM = [northM,eastM,downM];

end %end of function Lla2Ned
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

