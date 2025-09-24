Swiss Ephemeris Integration (High Accuracy)

To obtain professional‑grade (arc‑minute) positions, include the Swiss Ephemeris Java library and ephemeris data files.

Steps

1) Library JAR
- Download `swisseph.jar` from the official Swiss Ephemeris distribution.
- Place it at: `app/libs/swisseph.jar`.
- Gradle is already configured to include any JARs from `app/libs`.

2) Ephemeris Files
- Download the ephemeris data files (e.g., `seas_*.se1`, `sepl_*.se1`, `semo_*.se1`, etc.).
- Place all files under: `app/src/main/assets/ephe`.

3) Runtime
- The app copies `assets/ephe/*` into app private storage on first launch and points the Swiss Ephemeris engine to that directory.
- If the library or ephemeris files are missing, the app falls back to the approximate calculator and shows a notice.

Notes
- Sidereal mode is set to Lahiri and positions are topocentric.
- Ketu longitude is computed as the 180° complement of the True Node (Rahu).

