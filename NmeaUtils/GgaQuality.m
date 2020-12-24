classdef GgaQuality < uint32
  % NMEA GGA qualities.
  % Reference: NMEA-0183 v4.10 
  enumeration
    INVALID     (0)
    GPS         (1) % SPS
    DGPS        (2) % Differential GPS, SPS
    PPS         (3) 
    RTK         (4) % Fixed RTK
    FLOAT_RTK   (5)
    ESTIMATED   (6)
    MANUAL      (7)
    SIMULATION  (8)
  end
end
