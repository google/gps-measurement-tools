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
L5idx = find(gnssRaw.CarrierFrequencyHz < 1.2e9);
FreqNum(L5idx) = 5;

ConstSVFreq = gnssRaw.Svid+constmult*gnssRaw.ConstellationType + i*FreqNum;
UniqueConstSVFreq = unique(ConstSVFreq);
nUniqueConstSVFreq = length(UniqueConstSVFreq);

%%
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
%      title([ConstStr(const,:) ' PRN ' num2str(svid) ' L' num2str(imag(UniqueConstSVFreq(n)))]);% ' ' datefileplot]);
          title([ConstStr(const,:) ' PRN ' num2str(svid) ' L' num2str(imag(UniqueConstSVFreq(n))) ' ' datestrtest]);
     pause
end

freqDesired = 1176.45e6;
freqidx = find((gnssRaw.CarrierFrequencyHz == freqDesired);
agcFreq = gnssRaw.AgcDb(freqidx)
%% 

uniqueFreq = unique(gnssRaw.CarrierFrequencyHz);
nuniqueFreq = length(uniqueFreq);

for m = 1: nuniqueFreq
    uniqueFreqidx = find(uniqueFreq(m)== gnssRaw.CarrierFrequencyHz);    
    uniqueConstFreq = unique(gnssRaw.ConstellationType(uniqueFreqidx));
    nconstfreq = length(uniqueConstFreq);
    
    for n = 1:nconstfreq
        
        uniqueFreqConstidx = find(gnssRaw.ConstellationType(uniqueFreqidx) == uniqueConstFreq(n));        
        freqTimeNanos = gnssRaw.TimeNanos(uniqueFreqidx(uniqueFreqConstidx));
        %     freqCn0Db = gnssRaw.Cn0DbHz(uniqueFreqidx);
        freqAgcDb = gnssRaw.AgcDb(uniqueFreqidx(uniqueFreqConstidx));
        
        figure(m*100+n),
        plot(freqTimeNanos./1e9,freqAgcDb, 'ro')
        ylabel('AGC [dB]')
        xlabel('time of day (sec)')
        title([ConstStr(uniqueConstFreq(n),:) ' Freq ' num2str(round(uniqueFreq(m)/1e6)) ' MHz ' datestrtest]);
    end
    
end

%%

uniqueTimeNanos = unique(gnssRaw.TimeNanos);
nuniqueTimeNanos = length(uniqueTimeNanos);

%initialize
nfreq = zeros(nuniqueTimeNanos,2);
nconst = zeros(nuniqueTimeNanos,6);
nconstL1 = zeros(nuniqueTimeNanos,6);
nconstL5 = zeros(nuniqueTimeNanos,6);
nsigs = zeros(nuniqueTimeNanos,1)
% nL1 = zeros(1, nuniqueTimeNanos); nL5 = nL1;
% nGPSL1 = nL1; nGPSL5 = nL1;
% nGALL1 = nL1; nGALL5 = nL1;
% nGOLL1 = nL1; nBDSL1 = nL1;
% nQZSL1 = nL1; nSBSL5 = nL1;

for m = 1: nuniqueTimeNanos
    uniqueTimeidx = find(uniqueTimeNanos(m) == gnssRaw.TimeNanos);
    FreqNumTime = FreqNum(uniqueTimeidx); %unique(gnssRaw.CarrierFrequencyHz(uniqueTimeidx));  
    uniqueFreqNum = unique(FreqNumTime);
    nuniqueFreqNum = length(uniqueFreqNum);
    
    [GC,GR] = groupcounts(FreqNum(uniqueTimeidx));
    nsigs(m) = length(uniqueTimeidx);
    nfreq(m,ceil(GR./4)) = GC;  % divide by 4 results in the correct index GC =1 => 1 and GC = 5 => 2 
            
%     pause
    [GC,GR] = groupcounts(gnssRaw.ConstellationType(uniqueTimeidx));           
    nconst(m, GR) = GC;
    
    for n = 1:nuniqueFreqNum
       idx2 = find(FreqNumTime == uniqueFreqNum(n));
       [GC,GR] = groupcounts(gnssRaw.ConstellationType(uniqueTimeidx(idx2)));

       if uniqueFreqNum(n) == 1 % L1
           nconstL1(m, GR) = GC;
       else % L5
           nconstL5(m, GR) = GC;
       end
    end
end

figure(1000), plot(uniqueTimeNanos./1e9, nsigs, '.-'), ylabel('Number Sigs'); xlabel('time of day (sec)'),
title(['Num Signals ' datestrtest]);
    
figure(1001), plot(uniqueTimeNanos./1e9, nfreq, '.-'), ylabel('Number Sigs'); xlabel('time of day (sec)'), legend('L1', 'L5')
title(['Num Signals at Freq ' datestrtest]);

figure(1002), plot(uniqueTimeNanos./1e9, nconst, '.-'), ylabel('Number Sigs'); xlabel('time of day (sec)'), legend(ConstStr)
title(['Num Signals of Const ' datestrtest]);

figure(1003), plot(uniqueTimeNanos./1e9, nconstL1, '.-'), ylabel('Number Sigs L1'); xlabel('time of day (sec)'), legend(ConstStr)
title(['Num Signals of Const ' datestrtest]);

if sum(nfreq(2,:)) > 0
figure(1004), plot(uniqueTimeNanos./1e9, nconstL5, '.-'), ylabel('Number Sigs L5'); xlabel('time of day (sec)'), legend(ConstStr)
title(['Num Signals of Const ' datestrtest]);
end