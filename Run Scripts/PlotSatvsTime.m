%function PlotSatvsTime(gnssRaw)

% gnssRaw.Svid
% gnssRaw.ConstellationType
% gnssRaw.CarrierFrequencyHz
% gnssRaw.Cn0DbHz

constmult = 1000;
ConstStr = ['GPS';'SBA'; 'GLO'; 'QZS'; 'BDS'; 'GAL'];
    
%  #define GNSS_CONSTELLATION_SBAS         2
% %   #define GNSS_CONSTELLATION_GLONASS      3
% %   #define GNSS_CONSTELLATION_QZSS         4
% %   #define GNSS_CONSTELLATION_BEIDOU       5
% %   #define GNSS_CONSTELLATION_GALILEO      6

% Find unique satellite signals
FreqNum = ones(size(gnssRaw.CarrierFrequencyHz));
L5idx = find(gnssRaw.CarrierFrequencyHz < 1.2e9)
FreqNum(L5idx) = 5;

ConstSVFreq = gnssRaw.Svid+constmult*gnssRaw.ConstellationType + i*FreqNum;
UniqueConstSVFreq = unique(ConstSVFreq);
nUniqueConstSVFreq = length(UniqueConstSVFreq);

for n = 1:nUniqueConstSVFreq
    satsigidx = find(ConstSVFreq == UniqueConstSVFreq(n));
    svTimeNanos = gnssRaw.TimeNanos(satsigidx);
    svCn0Db = gnssRaw.Cn0DbHz(satsigidx);
    svAgcDb = gnssRaw.AgcDb(satsigidx);
        
   figure(n),
      yyaxis left
     plot(svTimeNanos./1e9,svCn0Db, 'b.-')
     ylabel('Cn0 [dB-Hz]')
     yyaxis right
     plot(svTimeNanos./1e9,svAgcDb, 'ro')     
     ylabel('AGC [dB]')
     xlabel('time of day (sec)')
     const = floor( UniqueConstSVFreq(n)/constmult)
     svid = real(UniqueConstSVFreq(n))- const*constmult;
     title([ConstStr(const,:) ' PRN ' num2str(svid) ' L' num2str(imag( UniqueConstSVFreq(n)))]);
     pause
end