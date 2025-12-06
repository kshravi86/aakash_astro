# Aakash Astro – Technical Documentation

This document describes the structure, runtime flow, feature surface, data dependencies, and extension guidelines for the **Aakash Astro** Android application. It complements `README.md` (product pitch) and `readmehowitworks.md` (ephemeris deep dive) by giving a full-stack view that engineers, QA, and release managers can use to operate and evolve the project safely.

---

**Related docs**
- `README.md` - entry point + quickstart.
- `docs/GITHUB_WORKFLOWS.md` - CI build and screenshot jobs.
- `docs/SWISS_EPH.md` - Swiss Ephemeris license notes.
- `docs/PRIVACY_POLICY.md` / `docs/privacy-policy.html` - legal copy surfaced in-app.
- `readmehowitworks.md` - Swiss Ephemeris + Lahiri ayanamsa accuracy notes.

## 1. Product & Scope

| Item | Details |
| --- | --- |
| Mission | Offline-first Vedic astrology companion delivering Swiss Ephemeris–grade accuracy across dasa timelines, panchanga, divisional charts, and tara/vedha utilities. |
| Platforms | Android (minSdk 24, target/compile 36), Kotlin + View Binding UI; no server components. |
| Core promise | Works fully offline with bundled city database, pre-packaged Swiss Ephemeris files, and local JSON persistence. Falls back to a pure-Kotlin astronomy solver when the Swiss artifacts are missing. |
| Users | Practicing astrologers and serious hobbyists who need fast birth-chart capture, exploratory dashboards, and compliance (in-app privacy policy). |

---

## 2. Repository Layout

```
.
app/                               # The primary Android application module. This is where all application-specific code, resources, and configuration reside.
  build.gradle.kts                 # Module-level Gradle build script. Configures Android + Kotlin plugins, defines dependencies (libraries, SDK versions), sets up build types (debug/release), and enables features like View Binding and desugaring.
  src/main/                        # Contains the main source set for the application.
    java/com/aakash/astro/         # Kotlin/Java source code for activities, services, custom views, and the core domain calculators. This is where the application's logic is implemented.
    assets/ephe/                   # Raw asset files that are bundled directly into the APK without being compiled. Specifically, this directory holds the Swiss Ephemeris data files (`.se1` files) required for precise astrological calculations.
    res/                           # Application resources such as layouts (XML files defining UI), drawables (images, vector assets), strings (translatable text), styles (theming), and other static content.
  libs/swisseph.jar                # External Java Archive (JAR) file containing the Swiss Ephemeris library. Placing JARs here automatically includes them as dependencies in the build.
docs/                              # Contains various documentation files including policies and architectural details.
scripts/capture_screenshots.sh     # A shell script used to automate the capture of deterministic screenshots for the Play Store.
readmehowitworks.md                # A detailed whitepaper explaining the accuracy choices for Swiss Ephemeris and Lahiri ayanamsa.
*.txt / *.json tables              # Root-level domain datasets (e.g., tara tables, D60 data, Sarvatobhadra grid definitions). These are often used by import scripts or for reference, but typically not directly packaged into the APK.
vedic-light-policy/                # Contains specific privacy policy copy, potentially for different platform listings or legal requirements.
```

Other helper sources (`FeatureGen.java`, `IconGen.java`, `TabletShots.java`) live at the repo root and generate store artwork; they do not participate in the Android build.

---

## 3. Build & Runtime Stack

The Android application build process is orchestrated by Gradle, an advanced build toolkit. It automates tasks such as compiling source code, packaging resources, managing dependencies, and generating the final APK (Android Package Kit) or AAB (Android App Bundle).

| Layer | Notes |
| --- | --- |
| **Gradle Build System** | This is a single-module project using Gradle 8.13 plugin and Kotlin 2.0.21, with configuration managed through `gradle/libs.versions.toml` for dependency versions. Gradle handles the entire build lifecycle: <br/> 1. **Dependency Resolution:** Downloads and includes all specified libraries (e.g., AndroidX, Material) and local JARs (`app/libs/swisseph.jar`). <br/> 2. **Compilation:** Compiles Kotlin source code (`.kt` files) into Java bytecode, and then converts Java bytecode into DEX (Dalvik Executable) format, which runs on the Android Runtime. <br/> 3. **Resource Packaging:** Processes and packages all resources (`res/` and `assets/` directories) into the final APK. <br/> 4. **Manifest Merging:** Combines `AndroidManifest.xml` files from the app module and any libraries into a single manifest. <br/> 5. **Signing:** Signs the application with a debug or release keystore for security and verification. <br/> 6. **APK/AAB Generation:** Outputs the final distributable package. View Binding is enabled, providing type-safe access to views, while Jetpack Compose is not used in this project. |
| **Dependencies** | The project relies on standard AndroidX Core/AppCompat for core functionalities, Material Design components for UI, ConstraintLayout for flexible layouts, and Espresso/JUnit for testing. `com.android.tools:desugar_jdk_libs` is used to enable newer Java language features (`java.time` API) on older Android API levels (min API 24). Any JAR files placed into `app/libs/` (e.g., `swisseph.jar`) are automatically added to the classpath and packaged with the application. |
| **Signing** | For `release` builds, `app/build.gradle.kts` is configured to read signing key details from `keystore.properties` (if present) to secure the application. Debug builds automatically use a default debug keystore provided by the Android SDK for development convenience. |
| **Assets** | Crucial Swiss ephemeris `.se1` files are stored under `app/src/main/assets/ephe/`. These raw files are copied to the app's internal storage at runtime. Additionally, the privacy policy HTML is duplicated in both `assets/privacy_policy.html` and `docs/privacy-policy.html` to ensure it's bundled in the app and available in documentation. |
| **Runtime setup** | On the first launch, `EphemerisPreparer` copies the `.se1` data from `assets/ephe` to the app's private storage. `AccurateCalculator` then uses this path to interface with the Swiss Ephemeris library. If these Swiss Ephemeris artifacts are missing, the application gracefully falls back to `AstrologyCalculator`, which is a pure Kotlin astronomical solver, ensuring basic functionality remains. |

Build commands:

```bash
./gradlew assembleDebug        # Builds a debug APK for development and testing.
./gradlew assembleRelease      # Builds a release APK; requires signing configuration in keystore.properties.
./gradlew test                 # Runs JVM unit tests for core logic (calculators, CityDatabase, etc.).
./gradlew lint                 # Executes Android Lint to check for code quality, correctness, and performance issues.
./gradlew connectedDebugAndroidTest  # Runs instrumented tests on a connected device or emulator for UI and integration testing.
```

`scripts/capture_screenshots.sh` assumes an ADB-connected device/emulator and captures a few deterministic screens after installing `app/build/outputs/apk/debug/app-debug.apk`. This is used to generate consistent imagery for app store listings.

---

## 4. High-Level Architecture

### 4.1 Layers

1. **Input & Navigation (`MainActivity.kt`)**
   - Captures birth context via Material date/time pickers, `android.location.Geocoder`, and a debounced `CityDatabase` autocomplete. Stores defaults in `SharedPreferences` and exposes quick chips for “Now/Morning/Evening”.
   - Renders natal summary (Lagna + planet tiles) and a RecyclerView of `ActionTile`s grouped into Foundation, Predictive, and Utility categories (`ui/ActionTileAdapter.kt`).
   - Persists favorite charts using `SavedStore` and exposes a Recent Actions chip row sourced from `recent_actions` preferences.

2. **Feature Activities**
   - Each feature (Panchanga, Dasha, Tara Bala, Transit overlays, etc.) is its own `AppCompatActivity` under `app/src/main/java/com/aakash/astro/`.
   - Activities expect an intent payload containing `EXTRA_NAME`, `EXTRA_EPOCH_MILLIS`, `EXTRA_ZONE_ID`, `EXTRA_LAT`, `EXTRA_LON` (constants defined per screen) and rebuild the necessary `BirthDetails`.
   - Layouts rely on view binding (e.g., `ActivityPanchangaBinding`) and shared widgets such as `VedicChartView` and `SavChartView`.

3. **Domain Layer (`astrology/`)**
   - `AccurateCalculator` bridges Swiss Ephemeris through reflection, enabling Lahiri sidereal mode, topocentric observers, and retrograde detection.
   - `AstrologyCalculator` provides a deterministic fallback using astronomical formulas (Julian day calculations, orbital elements, etc.).
   - Specialized calculators (`PanchangaCalc`, `DashaCalculator`, `VargaCalculator`, `ShadbalaCalculator`, `TaraBalaCalc`, `SixtyFourTwentyTwoCalc`, `JaiminiArudha`, etc.) operate on `ChartResult`.

4. **Data & Storage**
   - `CityDatabase` supplies ~70 major Indian cities for offline lookup; dynamic `Geocoder` hits augment this map at runtime.
   - `SavedStore` serializes charts into `files/horoscopes/<yyyy-MM-dd_HH-mm>.json`, deduplicates by epoch, and exposes CRUD helpers used by `SavedHoroscopesActivity`.
   - Static tables (e.g., `tara.txt`, `d60.txt`, `sarvotbhadrachakra.txt`, `tree.json`) are shipped at the repository root for reference/import scripts.

5. **Compliance & Docs**
   - `PrivacyActivity` (WebView wrapper) loads `file:///android_asset/privacy_policy.html`.
   - `docs/PRIVACY_POLICY.md`, `docs/THIRD_PARTY_NOTICES.md`, and `docs/SWISS_EPH.md` capture text required for stores and attribution.

### 4.2 Birth Context Flow

1. User supplies date/time/place; `MainActivity` composes a `BirthContext` (LocalDate, LocalTime, `City`, `ZoneId`, and derived `BirthDetails`).
2. `generateChart()` calls `AccurateCalculator.generateChart()` if SwissEph artifacts exist, otherwise falls back to `AstrologyCalculator`.
3. Natal chart summary (planet chips + `VedicChartView`) is rendered immediately; `BirthContext` is passed via intents when the user launches a tile.
4. Each feature recalculates or derives its specific data (e.g., `PanchangaCalc.compute`, `VargaCalculator.computeVargaChart`, `TaraBalaCalc.tClass`) and feeds dedicated layouts.

---

## 5. Domain Calculators & Utilities

| File | Responsibility | Downstream Screens |
| --- | --- | --- |
| `astrology/AccurateCalculator.kt` | Swiss Ephemeris bridge, Lahiri sidereal mode, topocentric observer, planet list with retrograde flags. | All feature activities when Swiss assets are present. |
| `astrology/AstrologyCalculator.kt` | Pure Kotlin fallback computing ascendant, houses, and planetary positions via orbital elements. | `MainActivity` preview + all screens when SwissEph is absent. |
| `astrology/VargaCalculator.kt` | Maps longitudes to divisional charts (D1–D60) with modality-aware rules. | `DivisionalChartsActivity`, `D60Activity`, `SixtyFourTwentyTwoActivity`. |
| `astrology/DashaCalculator.kt` & `astrology/YoginiDasha.kt` | Compute Vimshottari and Yogini mahadasha/antardasha timelines using epoch-seconds arithmetic. | `DashaActivity`, `YoginiDashaActivity`, `CharaDashaActivity` (Jaimini variant via `CharaDasha.kt`). |
| `astrology/Panchanga.kt` | Calculates tithi, vara, nakshatra (with pada), yoga, karana, and their lords plus sunrise/sunset helpers (`SunriseCalc.kt`). | `PanchangaActivity`, “Today’s Panchanga” quick action. |
| `astrology/Ashtakavarga.kt` | Computes Bhinnashtakavarga matrices and Sarva Ashtakavarga totals. | `AshtakavargaBavActivity`, `SarvaAshtakavargaActivity`, `SavChartView`. |
| `astrology/TaraBala.kt` | Classifies tara results based on nakshatra relationships and provides annotations per tara class. | `TaraBalaActivity`, `TaraBalaAnyActivity`, `TransitComboAnyActivity`, `TaraCalculatorActivity`. |
| `astrology/ShadbalaCalculator.kt` | Aggregates Sthana, Dig, Kala, Chestha, Naisargika strengths. | `ShadbalaActivity`. |
| `astrology/SixtyFourTwentyTwo.kt` | Finds 64th Navamsa and 22nd Drekkana from Lagna and Moon including lords + ranges. | `SixtyFourTwentyTwoActivity`. |
| `astrology/YogaDetector.kt`, `NabhasaYoga.kt`, etc. | Detect notable yogas across Nabhasa and other categories. | `YogasActivity`. |
| `astrology/JaiminiArudha.kt`, `JaiminiKarakas.kt`, `SpecialLagnas` helpers (`GhatikaLagna.kt`, `HoraLagna.kt`, etc.) | Provide house padas, karakas, alternate lagnas. | `ArudhaPadasActivity`, `JaiminiKarakasActivity`, `SpecialLagnasActivity`. |
| `astrology/IshtaDevata.kt`, `IshtaKashtaHarsha.kt`, `PushkaraNavamsha.kt`, `YogiCalculator.kt` | Scalar calculators feeding niche screens with share-to-text actions. | Corresponding `Activity` classes. |
| `EphemerisPreparer.kt` | Copies `assets/ephe/*` to `files/ephe/` and returns the folder path. | Called by `MainActivity` and every feature activity before using `AccurateCalculator`. |
| `storage/SavedStore.kt` | JSON CRUD for saved charts, with sanitized filenames and duplicate suppression. | `MainActivity` (save action), `SavedHoroscopesActivity`. |

---

## 6. Feature Reference

All activities live under `app/src/main/java/com/aakash/astro/`. The manifest (`app/src/main/AndroidManifest.xml`) registers each with `android:exported="false"` except `MainActivity`. Highlights:

### 6.1 Foundations

| Activity | Description | Data sources |
| --- | --- | --- |
| `DashaActivity.kt` | Displays Vimshottari mahadasha/antardasha timeline cards with duration math in days/years. | `DashaCalculator` |
| `YoginiDashaActivity.kt` | Similar timeline using Yogini sequence with goddess labels. | `YoginiDasha` |
| `CharaDashaActivity.kt` | Jaimini chara dasha progression with configurable focus (Lagna/Moon). | `CharaDasha` |
| `PanchangaActivity.kt` | Tithi, vara, nakshatra+pada, yoga, karana, sunrise/sunset; optional “today” mode. | `PanchangaCalc`, `SunriseCalc` |
| `YogasActivity.kt` | Lists detected Nabhasa + planetary yogas with rationale text. | `YogaDetector`, `NabhasaYoga` |

### 6.2 Predictive / Analytical

| Activity | Focus | Notes |
| --- | --- | --- |
| `SarvaAshtakavargaActivity.kt` | Renders Sarva Ashtakavarga in `SavChartView` with highlight filters. | Accepts natal context extras. |
| `AshtakavargaBavActivity.kt` | Detailed Bhinnashtakavarga tables per planet plus chips to jump between signs. | Uses `RecyclerView` subsections inside a scroll view. |
| `JaiminiKarakasActivity.kt` | Calculates chara karakas with tie-breaking and degree display. | `JaiminiKarakas.kt`. |
| `ArudhaPadasActivity.kt` | For all houses, shows pada sign + calculation notes. | `JaiminiArudha.kt`. |
| `SpecialLagnasActivity.kt` | Displays Ghatika, Hora (Parashara + Jaimini), Indu lagna, etc. | `GhatikaLagna.kt`, `HoraLagna.kt`, `HoraLagnaJaimini.kt`, `InduLagna.kt`. |
| `IshtaDevataActivity.kt` | Computes Ishta Devata & Palana Devata with share buttons. | `IshtaDevata.kt`. |
| `IshtaKashtaHarshaActivity.kt` | Renders IKH cycle table (benefic/malefic segments) using `IshtaKashtaHarsha.kt`. | - |
| `PushkaraNavamshaActivity.kt` | Highlights Pushkara navamsa positions for each planet. | `PushkaraNavamsha.kt`. |
| `ShadbalaActivity.kt` | Table of Sthana/Dig/Kala/Chestha/Naisargika values plus verdict coloring. | `ShadbalaCalculator.kt`. |
| `DivisionalChartsActivity.kt` | Scrollable cards showing D1..D60 `VedicChartView`s with textual descriptions sourced from `VargaCalculator.description()`. | - |
| `D60Activity.kt` | Tabular D60 layout listing amsha numbers, ranges, and devata/nature metadata using `D60Shashtiamsa`. | `VargaCalculator`. |
| `SixtyFourTwentyTwoActivity.kt` | Shows 64th Navamsa + 22nd Drekkana results from Lagna and Moon. | `SixtyFourTwentyTwoCalc`. |
| `TaraBalaActivity.kt` | Natal + transit tara bala verdicts, filters, and share actions. | `TaraBalaCalc`, `NakshatraCalc`. |
| `TaraBalaAnyActivity.kt` | User picks transit date/time/place to recompute tara results; optional natal context for Moon reference. | `TaraBalaCalc`. |
| `TaraCalculatorActivity.kt` | Quick lookup by selecting any two nakshatras/padas; useful for manual cross-checks. | `TaraBalaCalc`. |
| `TransitActivity.kt` | Displays live transit chart, planet cards, retrograde flags, and natal house overlay text. | `AccurateCalculator`. |
| `TransitAnyActivity.kt` | Date/time picker for arbitrary transit instants using the same rendering components. | `AccurateCalculator`, `MaterialDatePicker`. |
| `TransitComboAnyActivity.kt` | Combines a transit chart snapshot with tara bala verdicts for each planet row. | `AccurateCalculator`, `TaraBalaCalc`. |
| `OverlayActivity.kt` | Overlays Saturn/Jupiter transits on natal houses (house-level verdict chips). | `AccurateCalculator`. |
| `OverlayNodesActivity.kt` | Same overlay visualization but for Rahu/Ketu nodes. | `AccurateCalculator`. |
| `SarvatobhadraActivity.kt` | Builds a 9×9 grid with 28-star outer ring, letter rings, and zodiac belt; tapping planets paints vedha lines using `SbcOverlayView`. | `AccurateCalculator`, `SbcOverlayView`. |
| `YogiActivity.kt` | Computes Yogi, Avayogi, and Duplicate Yogi points with textual interpretation and share button. | `YogiCalculator.kt`. |

### 6.3 Utilities & System

| Activity | Purpose |
| --- | --- |
| `SavedHoroscopesActivity.kt` | Lists saved charts from `SavedStore`, with Load/Delete buttons and formatted timestamps. |
| `PrivacyActivity.kt` | WebView wrapper for the bundled privacy policy. |
| `MainActivity.kt` | Entry screen with birth capture, quick presets, natal preview, action grid, saved/recent chips, Swiss ephemeris detection, and menu shortcuts to Privacy/Saved charts. |

---

## 7. UI Components & Resources

| Component | File | Usage |
| --- | --- | --- |
| `VedicChartView` | `ui/VedicChartView.kt` + `res/layout/view_vedic_chart.xml` | South Indian fixed-sign chart; highlights Lagna, abbreviates planets, updates content descriptions for accessibility. |
| `SavChartView` | `ui/SavChartView.kt` | Reuses the chart layout to show Sarva Ashtakavarga bindus per sign with optional highlight background. |
| `SbcOverlayView` | `ui/SbcOverlayView.kt` | Draws vedha lines across the Sarvatobhadra grid by scaling normalized coordinates into canvas pixels. |
| Action tiles | `ui/ActionTileAdapter.kt`, `res/layout/item_action_tile.xml`, `item_action_header.xml` | RecyclerView grid with sticky headers, icon tinting, and category badges. |
| Theming | `res/values/themes.xml`, accent colors (`R.color.accent_*`) feed tile backgrounds and highlight states. |
| Layout bindings | Every `Activity*.kt` inflates its generated `Activity…Binding` class; layouts stick to `ConstraintLayout` or nested `LinearLayout` + `MaterialToolbar`. |

---

## 8. Data, Assets, and External Tables

| Artifact | Where | Notes |
| --- | --- | --- |
| Swiss Ephemeris `.se1` files | `app/src/main/assets/ephe/` | Copied to internal storage at runtime (`EphemerisPreparer.prepare`). |
| Privacy Policy HTML | `app/src/main/assets/privacy_policy.html`, `docs/privacy-policy.html`, `vedic-light-policy/privacy-policy.html` | Keep all three in sync when editing legal copy. |
| Static lookup tables | Root-level `.txt` files (`d60.txt`, `64-22.txt`, `sarvotbhadrachakra.txt`, `tara.txt`, `tree.json`, `tree2.json`). | Serve as source material/reference when updating calculators; some values are embedded directly in Kotlin classes. |
| City list | Hardcoded in `geo/CityDatabase.kt`. Extend by adding entries to `cities`. |
| Marketing art scripts | `FeatureGen.java`, `IconGen*.java`, `TabletShots.java` | Require desktop JDK + AWT; output PNGs for the Play Store. |

When adding new data files, prefer `app/src/main/assets/` if they must be packaged into the APK. Root-level `.txt` files are ignored by Gradle and intended for documentation/import scripts only.

---

## 9. Persistence, Privacy & Compliance

* **Storage** – `SavedStore` writes unencrypted JSON. Files live in app-private storage (`Context.filesDir/horoscopes`); Android backups are opt-in via `android:dataExtractionRules` and `android:fullBackupContent`.
* **Permissions** – The app does not request runtime permissions. It uses `Geocoder` with the default backend (network or device). If connectivity is absent, the offline `CityDatabase` ensures chart generation still works.
* **Privacy policy** – Linked from `MainActivity` overflow menu; `PrivacyActivity` loads the asset version and Material toolbar offers back navigation.
* **3rd-party notices** – `docs/THIRD_PARTY_NOTICES.md` and `docs/SWISS_EPH.md` describe license obligations (Swiss Ephemeris GPL + CLA). Include these when publishing to app stores.

---

## 10. Operational Playbook

1. **Swiss Ephemeris validation**
   - Ensure `app/libs/swisseph.jar` exists.
   - Drop Swiss `.se1` files into `app/src/main/assets/ephe`.
   - Install on a device once; `EphemerisPreparer` copies data and `AccurateCalculator` reads actual ephemeris output. Without these assets the UI shows `transit_engine_missing` notes.

2. **Build variants**
   - Debug builds use instant run + debuggable default.
   - Release builds sign with `release` config. Provide `keystore.properties` with `storeFile`, `storePassword`, `keyAlias`, `keyPassword`.

3. **Smoke tests**
   - Launch `MainActivity`, enter sample birth data, verify natal chart + planet chips populate.
   - Open high-risk screens (`PanchangaActivity`, `TaraBalaActivity`, `SarvatobhadraActivity`, `DivisionalChartsActivity`) to confirm Swiss ephemeris path works.
   - Save a chart, reopen via `SavedHoroscopesActivity`, and delete it.

4. **Screen capture**
   - Run `./scripts/capture_screenshots.sh` with an emulator connected; script unlocks the device, launches key activities, waits 8 seconds each, and stores PNGs under `artifacts/screenshots/`.

---

## 11. Extending the App

1. **Add a new analytic screen**
   - Create `NewFeatureActivity.kt` under `com.aakash.astro` and its XML layout under `res/layout/`.
   - Define required intent extras (usually name/epoch/zone/lat/lon) and call `EphemerisPreparer.prepare(this)` before using `AccurateCalculator`.
   - Register the activity in `AndroidManifest.xml` with `android:exported="false"`.
   - Add an `ActionTile` entry in `MainActivity.setupActionGrid()` and handle the tile ID in `handleActionTileClick`.

2. **Extend the domain layer**
   - Prefer placing new calculators in `astrology/` as standalone functions/classes that consume `ChartResult`.
   - Reuse helpers (`normalizeDegree`, `ZodiacSign`, `Planet`) to keep calculations consistent.

3. **Add static data**
   - For runtime consumption, place files in `app/src/main/assets` (and access via `assets.open()`).
   - For developer notes only, place under `docs/` or the repository root and document usage here.

4. **UI consistency**
   - Use Material components (toolbars, chips, buttons) and typography defined in `Theme.Aakash_astro`.
   - Prefer `VedicChartView` or `SavChartView` over bespoke chart panels to keep visuals consistent.

5. **Testing & QA**
   - No automated tests exist; when adding calculators, include at least a JVM unit test under `app/src/test` comparing known charts (Swiss ephemeris vs fallback).
   - Update this document and `README.md` whenever you add major screens or calculators.

---

## 12. Troubleshooting

| Symptom | Likely Cause | Fix |
| --- | --- | --- |
| `transit_engine_missing` message on feature screens | Missing `swisseph.jar` and/or `.se1` files, or `EphemerisPreparer` failed due to I/O. | Verify assets exist, reinstall app to recopy, or rely on fallback (reduced accuracy). |
| City autocomplete blank | Network Geocoder throttled or disabled. | Type a city from the offline `CityDatabase` list; long-press recents to reset. |
| Saved charts not appearing | File I/O failure or invalid JSON in `/files/horoscopes`. | Check Logcat for stack traces; delete corrupted files via Device File Explorer. |
| Screenshot script hangs | Device not connected/authorized. | `adb devices`, accept prompt, rerun script. |

---

## 13. Related Documents

* `README.md` – marketing/feature bullet list and architecture diagram.
* `readmehowitworks.md` – Swiss Ephemeris integration + Lahiri ayanamsa math.
* `docs/SWISS_EPH.md` – short how-to for bundling Swiss artifacts.
* `docs/PRIVACY_POLICY.md`, `docs/THIRD_PARTY_NOTICES.md` – compliance collateral.

Keep this document updated whenever:

1. New activities or calculators are added.
2. Build tooling or dependency versions change materially.
3. Privacy/storage behavior changes.
