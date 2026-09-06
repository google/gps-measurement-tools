Solve GNSS position & Inter-Signal Range Biases (ISRBs) from pseudoranges simultaneously.

State Vector:
States: X, Y, Z, rx clock bias, + ISRB states.
Index unique signal types per epoch (signal_tau_idx). Ref signal (e.g., GPS_L1) = 0. New signals = 1, 2...
Size: num_states = 3 (pos) + 1 (ref clock) + max(signal_tau_idx).
Init: x_vec = np.zeros(num_states).
Corrections: meas_df['corrected_pr_m'] = meas_df['PseudorangeMeters'] + meas_df['SvClockBiasMeters'] 
Get SV pos from SvPositionEcef[XYZ]Meters as a NumPy array. 
extract and stack the columns before the solver loop like this:
sv_xyz = meas_df[['SvPositionEcefXMeters', 'SvPositionEcefYMeters', 'SvPositionEcefZMeters']].to_numpy()

Geometry:
def calculate_geometry(obs_ecef_m, sv_xyz_at_ttx):
    delta = sv_xyz_at_ttx - obs_ecef_m
    l2_norm = np.linalg.norm(delta, axis=1)
    los = delta / l2_norm[:, None]

    # Sagnac (Earth rotation during signal flight time)
    EARTH_ROTATION_RPS = 7.2921151467e-5
    LIGHTSPEED = 299792458.0
    trx = (EARTH_ROTATION_RPS * (sv_xyz_at_ttx[:,0]*obs_ecef_m[1] - sv_xyz_at_ttx[:,1]*obs_ecef_m[0]) / LIGHTSPEED)

    return l2_norm + trx, los

H Matrix (num_meas x num_states): H[:, 0:3] = -1 * los H[:, 3] = 1 H[np.arange(num_meas), 3 + signal_tau_idx] = 1

Solver Loop:
# Inside loop until dx < tol_m:
geo_range, los = calculate_geometry(x_vec[:3], sv_xyz)
# Build H as above...
pr_hat = geo_range + H[:, 3:] @ x_vec[3:]
w_pr = 1.0 / meas_df['uncertainty_m']
z_pr = w_pr * (meas_df['corrected_pr_m'] - pr_hat)
WH = H * w_pr[:, None]
if np.linalg.cond(WH.T @ WH) > 1e12: return [np.nan]
dx = np.linalg.lstsq(WH, z_pr, rcond=None)[0]
x_vec += dx

Compute the unweighted post-fit residuals: z_post = meas_df['corrected_pr_m'] - pr_hat, where pr_hat is computed from the final state after the solver loop has terminated.

Crucial Data Handling:
Ensure a Signal column exists before indexing unique signal types (e.g., create it using df['Signal'] = df['ConstellationName'] + '_' + df['FrequencyBand'] if missing).
Save the individual z_post residuals as a new column (e.g., PostFitResidualMeters) in the per-epoch DataFrame.
Concatenate these per-epoch DataFrames into a single comprehensive DataFrame (e.g., df_wls_meas) so that the individual post-fit residuals for every satellite and signal type are preserved for downstream plotting.

Outputs:
- A DataFrame (`df_wls`) with epoch-level states: position (X,Y,Z), common bias, and ISRBs.
- A DataFrame (`df_wls_meas`) with the measurement-level post-fit residuals for every signal.

Optional Inputs:
known_xyz_m, 3-vector of ECEF position; if this is provided, then it is used in the solver loop: remove the los columns from the H matrix, and solve only for the remaining states.
known_isrbs_m, ISRBs by signal type; if this is provided,  then it is used in the solver loop: remove the relevant columns from the H matrix, and solve only for the remaining states.
