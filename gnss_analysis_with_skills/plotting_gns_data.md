General Rules
Libraries: Use plotly.express for charts and folium for maps. No static matplotlib.
Time formatting: Always convert utcTimeMillis to readable UTC pandas datetime.

Signal Counts: Time plots showing signal or satellite counts MUST use a 'stairs' line shape.
Computed Positions: When plotting WLS or KF positions, match the styling of df_fix maps (dots + thin lines, legends, layer controls).

# @title GNSS Performance Plots
CN0 Histograms: Plotly facet grid by Constellation Name. Overlay Frequency Bands using different colors (barmode='overlay').
Satellite Tracking: Stairs-style time series of satellite counts vs. time, organized/colored by signal type (constellation + frequency band).

# @title GNSS Fix Positions Map
Plot df_fix LatitudeDegrees & LongitudeDegrees on a folium map.
Style: Position dots connected by light thin lines.
Group traces by the Provider column and add folium.LayerControl() to toggle them.
Tooltip: When adding CircleMarkers, configure the tooltip to display the Fix number (index), UTC Time, Provider, and the coordinates on a single line formatted exactly as `LLA = {lat:.6f}, {lon:.6f}, {alt:.1f}` (e.g., LLA = xx.xxxxxx, yyy.yyyyyy, h.h).

# @title Interactive Time Series Explorer
UI: Use a native Colab Form (fields_to_plot = "AgcDb, Cn0DbHz" #@param {type:"string"}). Never use ipywidgets or input().
Menu: Print all valid, non-empty plot fields from df_raw (excluding 'Raw'). Format: alphabetical, comma-separated, new line for each new starting letter.
Plots: Generate time series for the selected fields, organized by constellation and frequency type; and within each of these categories, plot the values by signal, so each separate signal has a unique line.

# @title Sky Plot of Satellite Positions
Az/El Calculation: Use median Lat/Lon/Alt (default Alt=0) from df_fix as reference. Calculate Azimuth and Elevation using df_raw ECEF columns (SvPositionEcefXMeters, Y, Z).
Strict Vectorization: Use !pip install pymap3d and its vectorized functions (CRITICAL: `pymap3d.ecef2aer` returns values in the exact order `azimuth, elevation, slant_range`), or vectorized numpy. No for-loops.
Missing Data: If ECEF columns are NaN/empty, print: "SvPositionEcef[XYZ]Meters are needed in the log file for the skyplot."
SatLabels: Create standard RINEX labels: prefix (GPS:G, GLO:R, QZS:J, BDS:C, GAL:E, IRNSS:I, SBAS:S) + 2-digit zero-padded Svid (e.g., G01). To show them on the polar plot, DO NOT use standard annotations (like fig.add_annotation). Instead, add a new `go.Scatterpolar` trace with `mode='text'`, `showlegend=False`, and `hoverinfo='skip'` , and textposition='bottom right', using the final elevation/azimuth (r/theta) positions of each satellite to display the label in small font next to it.
Polar Plot: px.scatter_polar (r=Elevation, theta=Azimuth). Color by constellation.
Layout: range=[90, 0] (zenith center), direction='clockwise', rotation=90 (North top). Explicitly set `tickfont=dict(color='lightgray')` for both radialaxis and angularaxis to make the axis text labels lightgray.
