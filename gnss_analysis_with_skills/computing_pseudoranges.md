Compute pseudoranges directly from GNSS Logger 'Raw' fields and apply validation logic to filter out bad measurements prior to using them for positioning.


Before starting, define the following physical constant:

python
LIGHT_SPEED = 299792458.0

Note on GNSS State Flags: When applying validation logic, utilize the standard state flags defined in the Android developer documentation for the android.location.GnssMeasurement class (such as STATE_CODE_LOCK, STATE_TOW_KNOWN, STATE_TOW_DECODED, etc.) Apply your pre-trained knowledge of these standard Android API integer values directly in the code.

Step 1: Clock Bias Management (Vectorized)
When computing tRxNanos and WeekNumberNanos, you must anchor FullBiasNanos to hardware clock discontinuities.

Do not use a for-loop. Use Pandas vectorized operations:

Group the DataFrame by HardwareClockDiscontinuityCount.
Set a new column StableFullBiasNanos to the first valid (finite) value of FullBiasNanos for each group (e.g., using .transform('first') or .ffill()).
This ensures that the clock bias represents the actual receiver hardware clock behavior.

Step 2: Calculate Base Rx Time
Compute the week number in nanoseconds: WeekNumberNanos = np.floor(-df['StableFullBiasNanos'] * 1e-9 / 604800) * 604800 * 1e9
Compute the receiver time: tRxNanos = TimeNanos - (StableFullBiasNanos + BiasNanos) - WeekNumberNanos (Treat missing BiasNanos as 0).
Step 3: GPS, Galileo, BeiDou, QZSS
For these constellations, ReceivedSvTimeNanos is measured since the beginning of the GPS week.

Filter the measurements: Use only rows where State has the bits set for STATE_TOW_KNOWN OR STATE_TOW_DECODED.
NOTE

When doing bitwise operations in pandas like (df['State'] & bitmask) != 0, ensure you explicitly evaluate it as a boolean by checking != 0 so it chains correctly with other pandas boolean masks.

Compute: pseudorangeNanos = tRxNanos - ReceivedSvTimeNanos
Step 4: GLONASS
For GLONASS, ReceivedSvTimeNanos is measured since the beginning of the GLONASS day (UTC+3 Moscow Time). To adjust for this, you must offset tRxNanos to match GLONASS Time of Day:

Filter the measurements: Use only rows where State has the bits set for STATE_GLO_TOD_KNOWN OR STATE_GLO_TOD_DECODED.
Handle Leap Seconds:
GLONASS time is UTC+3 (10,800 seconds). GPS time is UTC + LeapSeconds.
If the LeapSecond column is missing from the raw data, create it and default it to 18 for all rows before doing the math to avoid Pandas broadcasting errors.
Apply the Offset & Compute:
Offset tRxNanos by adding (10800 - LeapSecond) * 1e9 nanoseconds.
Compute the difference: diff_nanos = (tRxNanos + offset_nanos) - ReceivedSvTimeNanos
Apply modulo arithmetic to handle GLONASS time of day boundaries seamlessly: pseudorangeNanos = diff_nanos % (86400 * 1e9)
Step 5: Conversion and Plotting
Convert to Meters: Compute the pseudoranges (in meters) for all signals using: PseudorangeMeters = pseudorangeNanos * 1e-9 * LIGHT_SPEED Add this to the data frame.
Prepare for Plotting:
Create a TimeUTC column by converting utcTimeMillis to Pandas datetime.
Filter for realistic bounds (e.g., between 1e6 and 1e8 meters).
Plot:
Plot all these pseudoranges vs time (TimeUTC), grouping by constellation and by frequency band.
Use plotly.express.
Display the plots as side-by-side subplots (faceting by ConstellationName) strictly in a single row without wrapping to new rows (do NOT use facet_col_wrap).
Ensure that the y-axes are matched (shared) across all these subplots so the viewer can easily compare the relative height of the pseudoranges for different constellations.
Step 6: Pseudorange Selection and Validation
Before using a measurement to compute downstream positioning, verify that the pseudorange is completely valid by inspecting its state flags:

Galileo E1 Signals (GAL_E1_UNKNOWN, GAL_E1_B, GAL_E1_C):
Must have either STATE_CODE_LOCK or STATE_GAL_E1BC_CODE_LOCK.
Buggy Chipset Check: Discard the measurement if STATE_GAL_E1C_2ND_CODE_LOCK is absent, AND STATE_2ND_CODE_LOCK is absent, AND STATE_TOW_DECODED is present, AND STATE_GAL_E1B_PAGE_SYNC is present.
All Other Signals:
Must have STATE_CODE_LOCK.
Time of Week (TOW) Check:
For GLONASS: Must have either STATE_GLO_TOD_KNOWN or STATE_GLO_TOD_DECODED.
For all others: Must have either STATE_TOW_DECODED or STATE_TOW_KNOWN.

Create a field ‘pr_uncertainty_m’ = ReceivedSvTimeUncertaintyNanos * LIGHT_SPEED * 1e-9, this will be used later for WLS position calculations.
