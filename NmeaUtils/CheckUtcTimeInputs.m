function [bOk] = CheckUtcTimeInputs(utcTime)
%utility function to check if gnss utc time is in expected format

%check inputs
assert(size(utcTime,2) == 6,...
    'utcTime does not have 6 columns, should not happen');

%check that year, month & day are integers
x = utcTime(:,1:3);
assert(all(all((x-fix(x)) == 0)),...
    'year,month or day are not integers, should never happen');

%check that year is in valid range
assert(all(utcTime(:,1) >= 1980) && all(utcTime(:,1) <= 2099),...
    'year is not in the range: [1980:2099], should never happen');

%check validity of month, day and time
assert(all(utcTime(:,2)>=1) && all(utcTime(:,2)<=12),...
    'The month in utcTime is not a number in the set [1:12], should never happen');
assert(all(utcTime(:,3)>=1) && all(utcTime(:,3)<=31),...
    'The day in utcTime is not a number in the set [1:31], should never happen');
assert(all(utcTime(:,4)>=0) && all(utcTime(:,4)<24),...
    'The hour in utcTime is not a number in the set [0:23], should never happen');
assert(all(utcTime(:,5)>=0) && all(utcTime(:,5)<60),...
    'The minutes in utcTime is not a number in the set [0:59], should never happen');
%Note: seconds can equal 60 exactly, on the second a leap second is added
assert(all(utcTime(:,6)>=0) && all(utcTime(:,6)<=60),...
    'The seconds in utcTime is not a number in the set [0:60], should never happen');

bOk = true;

end %end of function CheckUtcTimeInputs
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
