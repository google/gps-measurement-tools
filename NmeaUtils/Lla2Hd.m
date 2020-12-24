function [distM,NeM]=Lla2Hd(lla1DegDegM,lla2DegDegM)
% [distM,NeM]=Lla2Hd(lla1DegDegM,lla2DegDegM)
% function to difference latitude, longitude
% and provide the horizontal distance
%
% Inputs lla1DegDegM, Nx3 [lat,lon,alt] (degree, degrees, meters)
%        lla2DegDegM, Nx3, or 1x3
% Outputs:
%           distM, horizontal distance in meters
%           NeM matrix of N and E distances from lla2DegDegM to lla1DegDegM (m)          

ned = Lla2Ned(lla1DegDegM,lla2DegDegM);
NeM  = ned(:,1:2);

distM = sqrt(ned(:,1).^2 + ned(:,2).^2);
end %end of function Lla2Hd
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%