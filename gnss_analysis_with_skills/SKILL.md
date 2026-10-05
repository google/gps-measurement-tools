---
name: ion-2026-analyze-gnss
description: >-
  Use this skill to understand the data format and logic for parsing and interpreting Android GNSS Logger text files, including converting raw measurements into pseudoranges, handling GLONASS/GPS time, and mapping frequencies.
---

# Analyze Android GNSS Logger Data

This skill contains the domain knowledge required to correctly parse and
interpret Android GNSS Logger text files.

You are an expert Python data scientist. Write a clean, well-structured Colab
Notebook that reads an Android GNSS Logger file and parses its contents into
pandas DataFrames ready for analysis. All code blocks to be self-contained,
well-commented, and start with a `# @title` comment

When asked to calculate averages or aggregates, you MUST write a short Python
snippet to compute and print the final value. Do not attempt to calculate
averages mentally.

#### 1. Context and Data Format

-   **Input**: An Android GNSS Logger text file containing metadata and column
    definitions starting with `#`.
-   **Dynamic Headers**: Extract column headers dynamically from `# Raw,...`, `#
    Fix,...`, and `# Nav,...` lines. Keep the message identifier (e.g., `'Raw'`,
    `'Fix'`, `'Nav'`) as the first column for perfect row alignment.
-   **Data Cleaning**: Rows often have trailing empty fields or extra commas.
    Pad or truncate all row arrays to exactly match the target header length to
    prevent parsing errors.
-   **Target Records**: Parse `Raw` and `Fix` datasets, and reconstruct full
    byte payload lists from `Nav` sentences.

#### 2. Required Notebook Structure & Tasks

##### **Cell 1: Setup and File Upload**

-   **Title**: `# @title Setup and File Upload`
-   **Details**: Import `pandas`, `numpy`, `plotly.express`,
    `plotly.graph_objects`, `folium`, and `datetime`. Prompt user to upload the
    log file using `google.colab.files.upload()`.

##### **Cell 2: Data Parsing & Summary**

-   **Title**: `# @title Parse Data and Print Summary`
-   **Details**:
    -   Parse into `df_raw`, `df_fix`, and `df_nav` DataFrames.
    -   Ensure dynamic header mapping and row padding logic are implemented.
    -   Prevent String-to-Float Errors: Empty CSV fields (`,,`) parse as `""`
        strings, causing `.astype(float)` to fail. Immediately during parsing in
        Cell 2, replace `""` with `np.nan` and run `pd.to_numeric(df[col],
        errors='coerce')` on EVERY column in `df_raw` (except `'Raw'`,
        `'CodeType'`) and `df_fix` (except `'Fix'`, `'Provider'`,
        `'SolutionType'`, `'ExtraKeyValuePairs'`) to guarantee float/numeric
        types.
    -   Print the extracted Metadata (Version, Platform, Manufacturer).
    -   Print a summary report of `df_raw` in this exact format: `Successfully
        loaded %d Raw GNSS records, over %d seconds, from time hh:mm:ss to
        hh:mm:ss UTC`

##### **Cell 3: Constellation & Frequency Mapping**

-   **Title**: `# @title Map Constellations and Frequencies`
-   **Details**:
    -   In `df_raw`, map numeric `ConstellationType` to names using `{1: 'GPS',
        2: 'SBAS', 3: 'GLONASS', 4: 'QZSS', 5: 'BDS', 6: 'Galileo', 7: 'IRNSS'}`
        under a new `ConstellationName` column.
    -   Create a `FrequencyBand` column by evaluating `CarrierFrequencyHz` using
        this exact tolerance mapping function:

```python
def get_band_name(constellation, freq_hz):
    if pd.isna(freq_hz): return 'Unknown'
    freq_mhz = freq_hz / 1e6
    if constellation == 'GPS' or constellation == 'QZSS':
        if abs(freq_mhz - 1575.42) < 5: return 'L1'
        if abs(freq_mhz - 1176.45) < 5: return 'L5'
        if abs(freq_mhz - 1227.60) < 5: return 'L2'
    elif constellation == 'Galileo':
        if abs(freq_mhz - 1575.42) < 5: return 'E1'
        if abs(freq_mhz - 1176.45) < 5: return 'E5a'
        if abs(freq_mhz - 1207.14) < 5: return 'E5b'
        if abs(freq_mhz - 1278.75) < 5: return 'E6'
    elif constellation == 'GLONASS':
        if 1598 < freq_mhz < 1606: return 'G1'
        if 1242 < freq_mhz < 1249: return 'G2'
    elif constellation == 'BDS':
        if abs(freq_mhz - 1561.098) < 5: return 'B1I'
        if abs(freq_mhz - 1575.42) < 5: return 'B1C'
        if abs(freq_mhz - 1176.45) < 5: return 'B2a'
        if abs(freq_mhz - 1207.14) < 5: return 'B2b'
        if abs(freq_mhz - 1268.52) < 5: return 'B3'
    return 'Other'
```

##### **Cell 4: Navigation Message Full Byte Stream Extraction**

-   **Title**: `# @title Extract Full Navigation Bytes`
-   **Details**:
    -   Implement a dedicated parser to handle the dynamic length of trailing
        `Nav` fields.
    -   The standard header specifies `Data(Bytes)` as the 7th column, but the
        actual file contains arbitrary comma-separated payload bytes extending
        to the end of the line.
    -   Extract all bytes from column index 6 onwards into a single python
        `list` under a new column `FullBytes` and record the length of this list
        in a `ByteCount` column.
    -   Save the output in a DataFrame named `df_nav_full` containing: `['Svid',
        'Type', 'Status', 'MessageId', 'SubMessageId', 'ByteCount',
        'FullBytes']`

General Rules Libraries: Use plotly.express for charts and folium for maps. No
static matplotlib. Time formatting: Always convert utcTimeMillis to readable UTC
pandas datetime. Numeric types: Raw logs may leave numeric columns containing
empty strings ('') as object/str dtype. Before any math, transforms, or
plotting, always coerce target columns in df_raw and df_fix using
pd.to_numeric(df[col], errors='coerce').

# @title Plotting Fixes

Plot the given positions on a map using folium. To avoid OpenStreetMap 403
Forbidden errors, initialize the map with
tiles='https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/{z}/{y}/{x}'
and attr='Tiles &copy; Esri'. Style: Position dots connected by light thin
lines. Make the first position dot green and the last one red. Group traces by
the Provider column and add folium.LayerControl() to toggle them. Tooltip: When
adding CircleMarkers, configure the tooltip to display the Fix number (index),
UTC Time, Provider, and the coordinates on a single line formatted exactly as
`LLA = {lat:.6f}, {lon:.6f}, {alt:.1f}` (e.g., LLA = xx.xxxxxx, yyy.yyyyyy,
h.h). Computed Positions: When plotting WLS or KF positions, match the styling
of df_fix maps (dots + thin lines, legends, layer controls).

Signal Counts: Time plots showing signal or satellite counts MUST use a stairs
line shape. In plotly, use line_shape='hv'

# @title GNSS Performance Plots

CN0 Histograms: Plotly facet grid by Constellation Name. Overlay Frequency Bands
using different colors (barmode='overlay'). Satellite Tracking: Stairs-style
time series of satellite counts vs. time, organized/colored by signal type
(constellation + frequency band).

# @title Interactive Time Series Explorer

UI: Use a native Colab Form (fields_to_plot = "AgcDb, Cn0DbHz" #@param
{type:"string"}). Never use ipywidgets or input(). Menu: Print all valid,
non-empty plot fields from df_raw (excluding 'Raw'). Format: alphabetical,
comma-separated, new line for each new starting letter. Plots: Generate time
series for the selected fields, organized by constellation and frequency type;
and within each of these categories, plot the values by signal, so each separate
signal has a unique line.

# @title Sky Plot of Satellite Positions

Az/El Calculation: First coerce `df_raw[['SvPositionEcefXMeters',
'SvPositionEcefYMeters', 'SvPositionEcefZMeters']]` and
`df_fix[['LatitudeDegrees', 'LongitudeDegrees', 'AltitudeMeters']]` to float via
`pd.to_numeric(..., errors='coerce')` and drop rows with NaN ECEF coordinates
(prevents `TypeError: unsupported operand type(s) for -: 'str' and 'float'`).
Use median Lat/Lon/Alt (default Alt=0) from df_fix as reference. Calculate
Azimuth and Elevation using df_raw ECEF columns (SvPositionEcefXMeters, Y, Z).
Strict Vectorization: Use !pip install pymap3d and its vectorized functions
(CRITICAL: `pymap3d.ecef2aer` returns values in the exact order `azimuth,
elevation, slant_range`), or vectorized numpy. No for-loops. Missing Data: If
ECEF columns are NaN/empty, print: "SvPositionEcef[XYZ]Meters are needed in the
log file for the skyplot." SatLabels: Create standard RINEX labels: prefix
(GPS:G, GLO:R, QZS:J, BDS:C, GAL:E, IRNSS:I, SBAS:S) + 2-digit zero-padded Svid
(e.g., G01). To show them on the polar plot, DO NOT use standard annotations
(like fig.add_annotation). Instead, add a new `go.Scatterpolar` trace with
`mode='text'`, `showlegend=False`, and `hoverinfo='skip'` , and
textposition='bottom right', using the final elevation/azimuth (r/theta)
positions of each satellite to display the label in small font next to it. Polar
Plot: px.scatter_polar (r=Elevation, theta=Azimuth). Color by constellation.
Layout: range=[90, 0](zenith center), direction='clockwise', rotation=90 (North
top). Explicitly set `tickfont=dict(color='lightgray')` for both radialaxis and
angularaxis to make the axis text labels lightgray.

Compute pseudoranges directly from GNSS Logger 'Raw' fields and apply validation
logic to filter out bad measurements prior to using them for positioning. All
code blocks to start with a # @title comment

**CRITICAL DATA TYPE INSTRUCTION:** When computing nanosecond values
(`tRxNanos`, `WeekNumberNanos`, `pseudorangeNanos`, etc.) and processing clock
biases, you **MUST** ensure all calculations use 64-bit floating point precision
(`float64` / `np.float64`). Do not allow Pandas or NumPy to default to `int64`
computations, as this will lead to integer truncation and massive coordinate
drift.

Define physical constant: LIGHT_SPEED = 299792458.0

Note on GNSS State Flags: For validation, use the standard
android.location.GnssMeasurement state flag integer constants (STATE_CODE_LOCK,
STATE_TOW_KNOWN, STATE_TOW_DECODED, etc.) directly from your pre-trained
knowledge.

Step 1: Clock Bias Management (Vectorized) When computing tRxNanos and
WeekNumberNanos, you must anchor FullBiasNanos to hardware clock
discontinuities.

Do not use a for-loop. Use Pandas vectorized operations:

Group the DataFrame by HardwareClockDiscontinuityCount. Set a new column
StableFullBiasNanos to the first valid (finite) value of FullBiasNanos for each
group (e.g., using .transform('first') or .ffill()). This ensures that the clock
bias represents the actual receiver hardware clock behavior.

Step 2: Calculate Base Rx Time Compute the week number in nanoseconds:
WeekNumberNanos = np.floor(-df['StableFullBiasNanos'] * 1e-9 / 604800) *
604800 * 1e9 Compute the receiver time: tRxNanos = TimeNanos -
(StableFullBiasNanos + BiasNanos) - WeekNumberNanos (Treat missing BiasNanos as
0). Step 3: GPS, Galileo, BeiDou, QZSS For these constellations,
ReceivedSvTimeNanos is measured since the beginning of the GPS week.

Filter the measurements: Use only rows where State has the bits set for
STATE_TOW_KNOWN OR STATE_TOW_DECODED. NOTE

When doing bitwise operations in pandas like (df['State'] & bitmask) != 0,
ensure you explicitly evaluate it as a boolean by checking != 0 so it chains
correctly with other pandas boolean masks.

Compute: pseudorangeNanos = tRxNanos - ReceivedSvTimeNanos Step 4: GLONASS For
GLONASS, ReceivedSvTimeNanos is measured since the beginning of the GLONASS day
(UTC+3 Moscow Time). To adjust for this, you must offset tRxNanos to match
GLONASS Time of Day:

Filter the measurements: Use only rows where State has the bits set for
STATE_GLO_TOD_KNOWN OR STATE_GLO_TOD_DECODED. Handle Leap Seconds: GLONASS time
is UTC+3 (10,800 seconds). GPS time is UTC + LeapSeconds. If the LeapSecond
column is missing from the raw data, create it and default it to 18 for all rows
before doing the math to avoid Pandas broadcasting errors. Apply the Offset &
Compute: Offset tRxNanos by adding (10800 - LeapSecond) * 1e9 nanoseconds.
Compute the difference: diff_nanos = (tRxNanos + offset_nanos) -
ReceivedSvTimeNanos Apply modulo arithmetic to handle GLONASS time of day
boundaries seamlessly: pseudorangeNanos = diff_nanos % (86400 * 1e9) Step 5:
Conversion and Plotting Convert to Meters: Compute the pseudoranges (in meters)
for all signals using: PseudorangeMeters = pseudorangeNanos * 1e-9 * LIGHT_SPEED
Add this to the data frame. Prepare for Plotting: Create a TimeUTC column by
converting utcTimeMillis to Pandas datetime. Filter for realistic bounds (e.g.,
between 1e6 and 1e8 meters). Plot: Plot all these pseudoranges vs time
(TimeUTC), grouping by constellation and by frequency band. Use plotly.express.
Display the plots as side-by-side subplots (faceting by ConstellationName)
strictly in a single row without wrapping to new rows (do NOT use
facet_col_wrap). Ensure that the y-axes are matched (shared) across all
subplots. Step 6: Pseudorange Selection and Validation Before using a
measurement to compute downstream positioning, verify that the pseudorange is
completely valid by inspecting its state flags:

Galileo E1 Signals (GAL_E1_UNKNOWN, GAL_E1_B, GAL_E1_C): Must have either
STATE_CODE_LOCK or STATE_GAL_E1BC_CODE_LOCK.

All Other Signals: Must have STATE_CODE_LOCK. Time of Week (TOW) Check: For
GLONASS: Must have either STATE_GLO_TOD_KNOWN or STATE_GLO_TOD_DECODED. For all
others: Must have either STATE_TOW_DECODED or STATE_TOW_KNOWN.

Create a field ‘pr_uncertainty_m’ = ReceivedSvTimeUncertaintyNanos *
LIGHT_SPEED * 1e-9, this will be used later for WLS position calculations.

All code blocks must be self-contained, well-commented, and start with a `#
@title` comment so cells can be easily minimized.

## 1. WLS Position Solver

```python
def compute_wls_positions(df_raw, known_xyz_m=None, known_isrbs_m=None, max_iter=15, convergence_threshold=1e-4):
    # df_raw inputs: PseudorangeMeters, SvClockBiasMeters, ReceivedSvTimeUncertaintyNanos, SvPositionEcef[XYZ]Meters.
    # Returns: df_wls (epoch states), df_wls_meas (measurement-level df with 'PostFitResidualMeters').
```

### Rules

*   **Setup**: Coerce all required numeric columns in `df_raw` with
    `pd.to_numeric(..., errors='coerce')` and drop NaNs. `df['Signal'] =
    df['ConstellationName'] + '_' + df['FrequencyBand']`. Index unique signals
    per epoch (`signal_tau_idx`), `GPS_L1` = 0. Stack satellite coordinates
    before loop: `sv_xyz = meas_df[['SvPositionEcefXMeters',
    'SvPositionEcefYMeters', 'SvPositionEcefZMeters']].to_numpy(dtype=float)`.
    Exit with message if empty.
*   **State Vector**: States are `X, Y, Z, rx_clock_bias`, plus active ISRB
    states.
*   **Constraints**:
    *   If `known_xyz_m` is given: Fix coordinates to it, remove first 3 columns
        from $H$, and solve only for clock/ISRBs.
    *   If `known_isrbs_m` is given: Correct pseudoranges with those biases,
        remove relevant ISRB columns from $H$, and solve remaining states.
*   **Geometry & Sagnac**:

    ```python
    def calculate_geometry(obs_ecef_m, sv_xyz_at_ttx):
        delta = sv_xyz_at_ttx - obs_ecef_m
        l2_norm = np.linalg.norm(delta, axis=1)
        los = delta / l2_norm[:, None]
        EARTH_ROTATION_RPS = 7.2921151467e-5
        LIGHTSPEED = 299792458.0
        trx = (EARTH_ROTATION_RPS * (sv_xyz_at_ttx[:,0]*obs_ecef_m[1] - sv_xyz_at_ttx[:,1]*obs_ecef_m[0]) / LIGHTSPEED)
        return l2_norm + trx, los
    ```
*   **Solver**: Loop until $dx < \text{convergence_threshold}$. Formulate design
    matrix $H$ and weight matrix $W$ (weights $w_{pr} = 1.0 /
    \text{uncertainty_m}$ from `ReceivedSvTimeUncertaintyNanos` in meters).
    Compute unweighted `PostFitResidualMeters = corrected_pr_m - pr_hat` from
    final states and return in `df_wls_meas`.

WLS Convergence Rule: Do not save a WLS epoch to df_wls if updates fail to
converge within max_iter or if singular matrix math (poor GDOP) causes a
LinAlgError. Discard the epoch completely if the final position is near the
Earth's center (ECEF distance from [0,0,0] < 10,000 m) instead of saving default
values.

## 2. WLS Velocity Solver

```python
def compute_wls_velocities(df_raw, df_wls, known_vxyz_mps=None):
    # df_raw inputs: PseudorangeRateMetersPerSecond, SvVelocityEcef[XYZ]MetersPerSecond, PseudorangeRateUncertaintyMetersPerSecond, SvClockDriftMetersPerSecond.
    # df_wls inputs: Epoch positions (X,Y,Z) to compute LOS vectors.
    # Returns: df_wls_vel (epoch metrics: Vx, Vy, Vz, Ve, Vn, Vu, speed, clock drift), df_wls_vel_meas (measurement-level residuals).
```

### Rules

*   **States**: Solve for `Vx, Vy, Vz` and receiver clock frequency drift (m/s).
    Use positions from `df_wls` for line-of-sight unit vectors $\mathbf{a}_i$.
*   **Model**: Expected pseudorange rate: `prr_hat = (sv_vel - obs_vel) . los +
    clock_drift`. Design matrix $H_{vel}[:, 0:3] = -\mathbf{a}_i^T$ and
    $H_{vel}[:, 3] = 1.0$. Apply `SvClockDriftMetersPerSecond` to correct
    observations.
*   **Constraints**: If `known_vxyz_mps` is provided, fix velocity to it, remove
    the first 3 columns from $H_{vel}$, and solve only for clock drift.
*   **Residuals**: Compute unweighted `PostFitResidualMps = corrected_prr_m -
    prr_hat` using final states. Return these in `df_wls_vel_meas`.

Plotting GNSS Data: Plot the given positions on a map using folium. To avoid
OpenStreetMap 403 Forbidden errors, initialize the map with
tiles='https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/{z}/{y}/{x}'
and attr='Tiles &copy; Esri'. Style: Position dots connected by light thin
lines. Make the first position dot green and the last one red. Group traces by
the Provider column and add folium.LayerControl() to toggle them. Tooltip: When
adding CircleMarkers, configure the tooltip to display the Fix number (index),
UTC Time, Provider, and the coordinates on a single line formatted exactly as
`LLA = {lat:.6f}, {lon:.6f}, {alt:.1f}` (e.g., LLA = xx.xxxxxx, yyy.yyyyyy,
h.h).

Plotting: Plot horizontal speed and clock frequency in 2 subplots, using plotly,
For clock frequency, make the left y-axis m/s, and the right y-axis ns/s
