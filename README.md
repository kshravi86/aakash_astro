# Aakash Astro

Aakash Astro is a fully offline-first Vedic astrology toolkit written in Kotlin/Android. It bundles Swiss Ephemeris based calculations, a fallback astronomic solver, and more than twenty specialist screens that cover dasa timelines, panchanga, transit overlays, ashtakavarga, Jaimini techniques, Tara Bala, Sarvatobhadra, Shadbala, and divisional charts. The app is structured so the computational layer (`com.aakash.astro.astrology`) stays decoupled from Android UI code, which makes it easy to validate, extend, and reuse the calculators.

## Documentation Map
- `docs/PROJECT_DOCUMENTATION.md` - architecture, runtime flow, operations checklists.
- `readmehowitworks.md` - Swiss Ephemeris + Lahiri ayanamsa accuracy deep dive.
- `docs/SWISS_EPH.md` - Swiss Ephemeris license notice; keep in sync with releases.
- `docs/GITHUB_WORKFLOWS.md` - CI build and screenshot pipelines.
- `docs/PRIVACY_POLICY.md` / `docs/privacy-policy.html` - bundled policy copy (also in `vedic-light-policy/`).
- `scripts/capture_screenshots.sh` - reproducible Play Store screenshot generator.

## Highlights
- **Guided birth input** - `MainActivity` pairs Material date/time pickers with debounced Geocoder lookups and the bundled `CityDatabase` to capture full birth context before any calculation runs (`app/src/main/java/com/aakash/astro/MainActivity.kt`).
- **Action grid navigation** - every analytical screen is exposed as an `ActionTile`, so adding a new feature means wiring just one adapter entry plus an activity (`app/src/main/java/com/aakash/astro/ui/ActionTileAdapter.kt`).
- **Tiered calculation engines** - `AccurateCalculator` calls Swiss Ephemeris (if `swisseph.jar` and `.se1` assets are present) while `AstrologyCalculator` keeps the app usable with a pure-Kotlin fallback (`app/src/main/java/com/aakash/astro/astrology`).
- **Rich domain layer** - standalone calculators exist for dasas, Jaimini karakas, varga extraction, yogas, shadbala, tara bala, yogi points, etc., letting feature screens focus on presentation logic.
- **Self-contained data & privacy** - ephemeris files ship inside `app/src/main/assets/ephe`, saved horoscopes live in app-private storage via `SavedStore`, and an embedded privacy policy (`docs/privacy-policy.html` + `PrivacyActivity`) keeps compliance simple.

## Developer Quickstart (10 minutes)
1. Open the project in Android Studio (JDK 17+); let it download the right SDKs when prompted.
2. Keep the bundled Swiss ephemeris (`app/libs/swisseph.jar` + `app/src/main/assets/ephe`) or remove them to validate the built-in solver; the engine label under the natal chart shows which path is active.
3. Build and install: `./gradlew assembleDebug` then `adb install app/build/outputs/apk/debug/app-debug.apk`, or hit **Run** in Android Studio.
4. Smoke test: enter any birth date/time/place (or tap **Quick now**), generate the chart, and open `Panchanga`, `Dasha`, `Tara Bala`, and `Sarvatobhadra` to confirm calculators wire up.
5. Persistence check: use the overflow menu to **Save**, then open **Saved charts** to reload and delete a record—this exercises `SavedStore`.
6. Need more depth? `docs/PROJECT_DOCUMENTATION.md` walks through every subsystem; `readmehowitworks.md` covers Swiss Ephemeris accuracy choices.

## Repository Layout

```text
.
app/
  build.gradle.kts                  # Android module configuration (minSdk 24, target/compile 36, Kotlin 2.0.21)
  libs/swisseph.jar                 # Optional Swiss Ephemeris runtime
  src/main/
    java/com/aakash/astro/          # Activities, calculators, storage, geo helpers, UI widgets
    res/                            # Layout XML, theming, drawables
    assets/ephe/                    # Bundled Swiss ephemeris data files
docs/                               # Privacy policy, third-party notices, Swiss Ephemeris licensing notes
scripts/capture_screenshots.sh      # Helper used to refresh Play Store imagery
readmehowitworks.md                 # Deep dive on Lahiri ayanamsa + Swiss Ephemeris accuracy choices
build.gradle.kts / settings.gradle.kts / gradle/  # Gradle wrapper + version catalogs
*.txt / *.json resources            # Domain datasets (tara table, Sarvatobhadra grid, etc.)
```


## Architecture Diagram

```mermaid
graph LR
    subgraph UI_Layer
        MA["MainActivity.kt\nCollect inputs, build BirthContext, render dashboard"]
        Tiles["ActionTileAdapter.kt\nGrid navigation"]
        VC["ui/VedicChartView.kt\nSouth Indian fixed-sign layout"]
        SavedUI["SavedHoroscopesActivity.kt\nList + load/delete saved charts"]
        Feature["Feature Activities\n(e.g., PanchangaActivity, DashaActivity, TaraBalaActivity, etc.)"]
    end

    subgraph Domain_Layer
        Accurate["astrology/AccurateCalculator.kt\nSwiss Ephemeris bridge"]
        Fallback["astrology/AstrologyCalculator.kt\nPure Kotlin solver"]
        Support["astrology/* calculators\nAshtakavarga, Dasha, Jaimini, Tara Bala,\nShadbala, Pushkara, etc."]
        ChartData["ChartResult & PlanetPosition\n(shared data classes)"]
    end

    subgraph Data_and_Storage
        CityDB["geo/CityDatabase.kt\nBuilt-in Indian city list"]
        Geocode["android.location.Geocoder\nfetchIndiaSuggestions()"]
        EphePrep["EphemerisPreparer.kt\nCopies assets/ephe -> files/ephe"]
        Assets["app/src/main/assets/ephe + *.txt\nSwiss .se1 files & domain tables"]
        Saved["storage/SavedStore.kt\nfiles/horoscopes/<id>.json"]
    end

    subgraph Docs_Compliance
        Privacy["PrivacyActivity.kt + docs/privacy-policy.html\nIn-app policy viewer"]
        Whitepaper["readmehowitworks.md\nSwiss Ephemeris accuracy deep dive"]
    end

    MA -->|RecyclerView| Tiles
    MA --> VC
    MA --> SavedUI
    Tiles --> Feature
    MA -->|BirthContext extras| Feature
    Feature --> Support
    MA -->|generateChart()| Accurate
    MA -->|fallback| Fallback
    Accurate -->|ChartResult| ChartData
    Fallback -->|ChartResult| ChartData
    ChartData --> VC
    ChartData --> Feature
    Accurate -->|setEphePath| EphePrep
    EphePrep --> Assets
    MA -->|city dropdown| CityDB
    MA -->|debounced lookup| Geocode
    MA -->|save/load| Saved
    SavedUI --> Saved
    Privacy --> Whitepaper
```

**How to read the diagram**
- `MainActivity.kt` orchestrates the UI layer: it owns the action grid (`ActionTileAdapter`), renders the South-Indian chart via `VedicChartView`, launches all specialist activities with serialized birth context, and surfaces saved charts/privacy links.
- The domain layer is split between `AccurateCalculator` (Swiss Ephemeris + Lahiri ayanamsa), the fallback `AstrologyCalculator`, and dozens of focused calculators in `app/src/main/java/com/aakash/astro/astrology` that power individual screens. Both calculators emit the shared `ChartResult`/`PlanetPosition` models consumed across the app.
- Data helpers include `CityDatabase` and on-demand `Geocoder` lookups for place resolution, `EphemerisPreparer` plus bundled `.se1` files for high-precision astronomy, and `SavedStore` JSON files that back `SavedHoroscopesActivity`.
- Compliance/documentation pieces (`PrivacyActivity`, `readmehowitworks.md`, and files under `docs/`) stay separate but are linked from the main UI so users can review policies and the Swiss Ephemeris whitepaper.

## Birth Data & Navigation Flow
The dashboard screen (`MainActivity`) is responsible for collecting input, generating a chart, and routing to analysis screens:

1. **Input widgets** - Material pickers (`MaterialDatePicker`, `MaterialTimePicker`) feed `selectedDate/selectedTime`. Quick-select chips (`Quick now`, `Morning`, `Evening`) keep `selectedTime` in sync with the form and shared preferences persist the last-used values.
2. **Location helpers** - `CityDatabase` provides instant Indian metros while debounced `Geocoder` lookups (`fetchIndiaSuggestions` & `geocodeFirstIndia`) add remote suggestions once a user types three or more characters. Resolved `City` objects update both the latitude/longitude fields and the `BirthContext`.
3. **BirthContext guard** - `withBirthContext` short-circuits every action if date/time/location are missing, ensuring downstream screens always receive `EXTRA_*` payloads (epochMillis, zoneId, lat, lon, name).
4. **Chart generation** - tapping **Generate** invokes `AccurateCalculator.generateChart`; on failure (e.g., missing Swiss Ephemeris) a snackbar warns the user and `AstrologyCalculator` keeps the UI responsive. The resulting `ChartResult` fills `VedicChartView` plus the planet RecyclerView (`ItemPlanetPositionBinding` rows).
5. **Action grid** - `RecyclerView` + `ActionTileAdapter` renders a two-column menu of analytics (Vimshottari Dasha, Panchanga, Yogas, SAV/BAV, Jaimini tools, transit overlays, Tara Bala, etc.). Selecting a tile launches the matching activity with the serialized birth context.
6. **Overflow actions** - the top app bar exposes `Save` (persists via `SavedStore`) and `Saved charts`, `Share`, and `Privacy`. Saved charts reopen the dashboard with intent extras so users can immediately jump back into any downstream analyzer.

## Feature Reference

| Feature / Screen | Activity | Domain classes & assets |
| --- | --- | --- |
| Dashboard + Vedic chart | `MainActivity`, `VedicChartView` | `AccurateCalculator`, `AstrologyCalculator`, `ChartResult` |
| Vimshottari & Yogini dasas | `DashaActivity`, `YoginiDashaActivity`, `CharaDashaActivity` | `DashaCalculator`, `YoginiDasha`, `CharaDasha`, `BirthDetails` |
| Panchanga & calendar helpers | `PanchangaActivity`, `Today Panchanga` shortcut | `Panchanga`, `SunriseCalc`, `Nakshatra`, `TaraBala`, `tara.txt` |
| Transit & overlay suite | `TransitActivity`, `TransitAnyActivity`, `TransitComboAnyActivity`, `OverlayActivity`, `OverlayNodesActivity` | `AstrologyCalculator`, `AccurateCalculator`, overlay view models |
| Tara Bala calculators | `TaraBalaActivity`, `TaraBalaAnyActivity`, `TaraCalculatorActivity` | `TaraBala`, `TransitTara`, `tara.txt` |
| Ashtakavarga (SAV/BAV) | `SarvaAshtakavargaActivity`, `AshtakavargaBavActivity` | `Ashtakavarga`, `SavChartView`, `item_sav_*` layouts |
| Jaimini toolkit | `JaiminiKarakasActivity`, `ArudhaPadasActivity`, `SpecialLagnasActivity` | `JaiminiKarakas`, `JaiminiArudha`, `HoraLagna`, `GhatikaLagna`, `InduLagna` |
| Ishta/Ishta-Kashta-Harsha & Ishta Devata | `IshtaKashtaHarshaActivity`, `IshtaDevataActivity` | `IshtaKashtaHarsha`, `IshtaDevata`, `Karakamsa` helpers |
| Strength & yogas | `ShadbalaActivity`, `YogasActivity`, `YogiActivity`, `PushkaraNavamshaActivity`, `SixtyFourTwentyTwoActivity`, `D60Activity`, `DivisionalChartsActivity` | `ShadbalaCalculator`, `YogaDetector`, `YogiCalculator`, `PushkaraNavamsha`, `SixtyFourTwentyTwo`, `VargaCalculator`, `D60Shashtiamsa` |
| Sarvatobhadra chakra | `SarvatobhadraActivity`, `SbcOverlayView` | `Sarvatobhadra` dataset (`tree.json`, `tree2.json`, `sarvotbhadrachakra.txt`) |
| Saved horoscopes | `SavedHoroscopesActivity` | `SavedStore` JSON persistence (app-private `files/horoscopes`) |

Every activity follows the same contract: read the serialized birth context (`EXTRA_NAME`, `EXTRA_EPOCH_MILLIS`, `EXTRA_ZONE_ID`, `EXTRA_LAT`, `EXTRA_LON`), run the domain computation, and bind into RecyclerViews/custom views declared in `app/src/main/res/layout`.

## Astrology & Calculation Layer

- **Ephemeris preparation** - `EphemerisPreparer` copies `.se1` files from `assets/ephe` to `files/ephe` on first launch so Swiss Ephemeris can read them from internal storage. `AccurateCalculator.setEphePath` points the library to that folder.
- **Swiss Ephemeris bridge** - `AccurateCalculator` loads `swisseph.SwissEph` via reflection, enforces Lahiri ayanamsa (`SE_SIDM_LAHIRI`), toggles sidereal flags, and outputs `ChartResult` objects. When `swisseph.jar` is missing, it simply returns `null`.
- **Built-in solver** - `AstrologyCalculator` implements simplified heliocentric orbital math (Kepler equations, ayanamsa, sidereal conversion) so the UI continues to function without external jars.
- **Support calculators** - The `com.aakash.astro.astrology` package contains reusable engines for:
  - divisional charts (`VargaCalculator`, `Vargottama`, `DrekkanaUtils`, `D60Shashtiamsa`)
  - dasas (`DashaCalculator`, `CharaDasha`, `YoginiDasha`)
  - strength and points (`Ashtakavarga`, `ShadbalaCalculator`, `YogiCalculator`, `PushkaraNavamsha`, `SixtyFourTwentyTwo`)
  - yoga detection (`YogaDetector`, `NabhasaYoga`, `IshtaKashtaHarsha`)
  - panchanga/tithi utilities (`Panchanga`, `SunriseCalc`, `Nakshatra`, `TaraBala`)
  - Jaimini-specific logic (`JaiminiKarakas`, `JaiminiArudha`, `HoraLagna`, `HoraLagnaJaimini`, `GhatikaLagna`, `InduLagna`)

Decoupling ensures each activity only imports the calculators it needs, which keeps UI classes readable and simplifies adding tests later.

## Data, Storage, and Assets
- **City lookups** - `CityDatabase` ships with curated Indian metros and quick aliases (`Bengaluru`, `Bangalore`, etc.) while Geocoder extends coverage for online users.
- **Saved charts** - `SavedStore` writes JSON records to `files/horoscopes/<timestamp>.json`, deduplicates by epoch, and exposes `list/load/delete` helpers consumed by `SavedHoroscopesActivity`.
- **Ephemeris & lookup tables** - Domain text files in the repo root (`tara.txt`, `d60.txt`, `sarvotbhadrachakra.txt`, `tree.json`, etc.) feed calculators that need static mappings.
- **Custom views** - `VedicChartView`, `SavChartView`, and `SbcOverlayView` encapsulate non-standard layouts so feature screens simply bind data.
- **Privacy & notices** - `docs/privacy-policy.html`, `docs/PRIVACY_POLICY.md`, and `docs/THIRD_PARTY_NOTICES.md` are surfaced via `PrivacyActivity` and the in-app menu.

## Build & Run

### Requirements
- Android Studio Ladybug+ or IntelliJ with AGP 8.13.0
- JDK 17+ (Gradle toolchain produces Java 11 bytecode / desugaring is enabled)
- Android SDK 36 (compile/target) and minimum SDK 24
- Optional: Swiss Ephemeris jar (`app/libs/swisseph.jar`) and `.se1` data inside `app/src/main/assets/ephe`

### Commands
```bash
# Build + install debug
./gradlew assembleDebug

# Run connected tests (if/when added)
./gradlew connectedDebugAndroidTest

# Generate release bundle (requires keystore.properties)
./gradlew bundleRelease
```

### Swiss Ephemeris setup
1. Place `swisseph.jar` inside `app/libs` (already committed for local builds).
2. Keep the `.se1` ephemeris files under `app/src/main/assets/ephe`.
3. On first launch `EphemerisPreparer` copies them to internal storage; `MainActivity.prepareEphemeris()` then points `AccurateCalculator` to that folder.
4. The engine indicator under the chart toggles between “Swiss Ephemeris” and “Built-in solver” so you can immediately confirm which path is active.

### Signing
Create `keystore.properties` with `storeFile`, `storePassword`, `keyAlias`, `keyPassword`. `app/build.gradle.kts` automatically hooks it into the release build.

## Quality & Testing
- JVM tests exist for core calculators and helpers (`app/src/test/java/com/aakash/astro/astrology/*Test.kt`, `CityDatabaseTest.kt`); run them with `./gradlew test`.
- Instrumented tests live under `app/src/androidTest`; run with `./gradlew connectedDebugAndroidTest` when a device/emulator is attached.
- When touching calculators, prefer adding regression vectors (e.g., known horoscope vs expected ascendant) before refactoring.
- Style and lint guardrails live in Gradle: run `./gradlew ktlintCheck` (automatically wired into `./gradlew check`) to enforce the `.editorconfig` rules, and `./gradlew ktlintFormat` to auto-fix formatting drifts.
- Run `./gradlew lintDebug` locally; AGP 8.13 also supports `lintVitalRelease` if you add a CI gate.

## Extending the App
1. **Add or update a calculator** under `com.aakash.astro.astrology` (keep pure Kotlin, no Android dependencies).
2. **Create the UI** - add a layout under `res/layout`, generate a binding, and implement a new `Activity` or `Fragment` that consumes the calculator.
3. **Register navigation** - add an `ActionTile` entry (or menu option) in `MainActivity.setupActionGrid()` and wire it to a launcher method that passes the standard extras.
4. **Hook resources** - update `AndroidManifest.xml`, string resources, and (optionally) screenshot scripts if the feature targets Play Store imagery.
5. **Document/new datasets** - drop supporting tables under `/` or `assets/` and mention them in `docs/THIRD_PARTY_NOTICES.md` if licensing applies.

## Utilities
- `scripts/capture_screenshots.sh` generates deterministic Play Store imagery (used by `screenshot-capture.yml`).
- Root-level `*.png` assets (chakra diagrams, feature graphics) are already Play Store ready.

## Troubleshooting
- **"Swiss Ephemeris missing" snackbar** - the UI falls back to the built-in solver; verify `app/libs/swisseph.jar` is present and the `.se1` assets copied correctly.
- **No geocoder suggestions** - many emulator images ship without Geocoder backends; the static `CityDatabase` list will still work, but for live suggestions install Google Play services or test on device.
- **Saved chart not loading** - entries are de-duplicated by epoch; ensure the birth time truly differs or delete the JSON under `files/horoscopes` before saving again.
- **Layout overlap on small screens** - most detail activities use nested scroll containers; adjust their layout XML if you add long descriptions or extra RecyclerViews.

This README should give you enough context to onboard quickly, trace any feature to the relevant code, and extend the astrology engines with confidence. For deeper math/astronomy notes, keep `readmehowitworks.md` handy.
