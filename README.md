# Aakash Astro

Aakash Astro is an offline-first Vedic astrology toolkit written in Kotlin for Android. It supports Swiss Ephemeris (optional) and a built-in fallback solver, and ships with 20+ specialist screens for dasha, panchanga, transits, ashtakavarga, Jaimini techniques, Tara Bala, Sarvatobhadra, Shadbala, and divisional charts. The domain layer (`com.aakash.astro.astrology`) stays Android-free so calculators are easy to validate, test, and reuse.

## At a glance
- Offline-first; no network required for core calculations
- Dual engines: Swiss Ephemeris if present, Kotlin fallback otherwise
- Dashboard action grid to reach every feature screen
- Domain logic separated from UI for testing and reuse

## Documentation map
- `docs/PROJECT_DOCUMENTATION.md` - architecture, runtime flow, operations checklists
- `readmehowitworks.md` - Swiss Ephemeris + Lahiri ayanamsa accuracy notes
- `docs/SWISS_EPH.md` - Swiss Ephemeris licensing
- `docs/GITHUB_WORKFLOWS.md` - CI build and screenshot pipelines
- `docs/DIAGRAMS.md` - extra diagrams and flow references
- `docs/PRIVACY_POLICY.md` and `docs/privacy-policy.html` - bundled privacy policy
- `docs/THIRD_PARTY_NOTICES.md` - third-party license acknowledgements
- `scripts/capture_screenshots.sh` - Play Store screenshot generator

## Quickstart

### Prerequisites
- Android Studio Ladybug+ or IntelliJ with AGP 8.13.0
- JDK 17
- Android SDK 36 (compile/target) and minimum SDK 24
- Optional: `app/libs/swisseph.jar` and `app/src/main/assets/ephe` for high-precision ephemeris

### Steps
1. Open the project in Android Studio and let it sync Gradle.
2. Optional: verify `swisseph.jar` and ephe assets are present if you want Swiss Ephemeris precision.
3. Build and run:
   - CLI: `./gradlew assembleDebug` then `adb install app/build/outputs/apk/debug/app-debug.apk`
   - Studio: press Run
4. Smoke test: enter birth date, time, and place, then generate a chart.
5. Open a few screens (Panchanga, Dasha, Tara Bala, Sarvatobhadra) to confirm calculations.
6. Save a chart from the menu and reload it from Saved charts to verify persistence.

Expected outcome: you should see the dashboard, a generated chart, and working feature screens.

## Repository layout

```text
.
app/
  build.gradle.kts                  # Android module configuration
  libs/swisseph.jar                 # Optional Swiss Ephemeris runtime
  src/main/
    java/com/aakash/astro/          # Activities, calculators, storage, geo, UI widgets
    res/                            # Layout XML, theming, drawables
    assets/ephe/                    # Bundled Swiss Ephemeris data files
docs/                               # Privacy policy, notices, Swiss Ephemeris licensing, diagrams
scripts/                            # Screenshot capture helpers
readmehowitworks.md                 # Swiss Ephemeris accuracy deep dive
build.gradle.kts / settings.gradle.kts / gradle/  # Gradle wrapper and version catalog
*.txt / *.json resources            # Domain datasets (tara, Sarvatobhadra grids, etc.)
```

## Architecture overview

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
        ChartData["ChartResult and PlanetPosition\n(shared data classes)"]
    end

    subgraph Data_and_Storage
        CityDB["geo/CityDatabase.kt\nBuilt-in Indian city list"]
        Geocode["android.location.Geocoder\nfetchIndiaSuggestions()"]
        EphePrep["EphemerisPreparer.kt\nCopies assets/ephe -> files/ephe"]
        Assets["app/src/main/assets/ephe + *.txt\nSwiss .se1 files and domain tables"]
        Saved["storage/SavedStore.kt\nfiles/horoscopes/<id>.json"]
    end

    subgraph Docs_Compliance
        Privacy["PrivacyActivity.kt + docs/privacy-policy.html\nIn-app policy viewer"]
        Whitepaper["readmehowitworks.md\nSwiss Ephemeris accuracy notes"]
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

How to read the diagram:
- `MainActivity.kt` orchestrates the dashboard, action grid, and chart rendering.
- The domain layer is split between `AccurateCalculator` (Swiss Ephemeris) and `AstrologyCalculator` (fallback), both emitting `ChartResult` and `PlanetPosition`.
- Data helpers include `CityDatabase`, `Geocoder`, `EphemerisPreparer`, and `SavedStore`.
- Compliance docs are linked in-app via `PrivacyActivity`.

## Birth input and navigation flow
1. Input widgets (Material date/time pickers) update `selectedDate` and `selectedTime`.
2. Location helpers (`CityDatabase` and debounced `Geocoder`) resolve a `City`.
3. `withBirthContext` guards all actions until date, time, and location are set.
4. `generateChart()` prefers `AccurateCalculator` and falls back to `AstrologyCalculator` with a snackbar.
5. `VedicChartView` and the planet list render the `ChartResult`.
6. The action grid launches feature activities with the serialized birth context.

## Feature reference

| Feature / Screen | Activity | Domain classes and assets |
| --- | --- | --- |
| Dashboard + Vedic chart | `MainActivity`, `VedicChartView` | `AccurateCalculator`, `AstrologyCalculator`, `ChartResult` |
| Vimshottari and Yogini dashas | `DashaActivity`, `YoginiDashaActivity`, `CharaDashaActivity` | `DashaCalculator`, `YoginiDasha`, `CharaDasha`, `BirthDetails` |
| Panchanga and calendar helpers | `PanchangaActivity`, Today Panchanga shortcut | `Panchanga`, `SunriseCalc`, `Nakshatra`, `TaraBala`, `tara.txt` |
| Transit and overlay suite | `TransitActivity`, `TransitAnyActivity`, `TransitComboAnyActivity`, `OverlayActivity`, `OverlayNodesActivity` | `AstrologyCalculator`, `AccurateCalculator`, overlay view models |
| Tara Bala calculators | `TaraBalaActivity`, `TaraBalaAnyActivity`, `TaraCalculatorActivity` | `TaraBala`, `TransitTara`, `tara.txt` |
| Ashtakavarga (SAV/BAV) | `SarvaAshtakavargaActivity`, `AshtakavargaBavActivity` | `Ashtakavarga`, `SavChartView`, `item_sav_*` layouts |
| Jaimini toolkit | `JaiminiKarakasActivity`, `ArudhaPadasActivity`, `SpecialLagnasActivity` | `JaiminiKarakas`, `JaiminiArudha`, `HoraLagna`, `GhatikaLagna`, `InduLagna` |
| Ishta, Ishta-Kashta-Harsha, Ishta Devata | `IshtaKashtaHarshaActivity`, `IshtaDevataActivity` | `IshtaKashtaHarsha`, `IshtaDevata`, `Karakamsa` helpers |
| Strength and yogas | `ShadbalaActivity`, `YogasActivity`, `YogiActivity`, `PushkaraNavamshaActivity`, `SixtyFourTwentyTwoActivity`, `D60Activity`, `DivisionalChartsActivity` | `ShadbalaCalculator`, `YogaDetector`, `YogiCalculator`, `PushkaraNavamsha`, `SixtyFourTwentyTwo`, `VargaCalculator`, `D60Shashtiamsa` |
| Sarvatobhadra chakra | `SarvatobhadraActivity`, `SbcOverlayView` | `Sarvatobhadra` datasets (`tree.json`, `tree2.json`, `sarvotbhadrachakra.txt`) |
| Saved horoscopes | `SavedHoroscopesActivity` | `SavedStore` JSON persistence (`files/horoscopes`) |

Every activity follows the same contract: read the serialized birth context (`EXTRA_NAME`, `EXTRA_EPOCH_MILLIS`, `EXTRA_ZONE_ID`, `EXTRA_LAT`, `EXTRA_LON`), run the domain computation, and bind into the layout XML.

## Astrology and calculation layer
- `EphemerisPreparer` copies `.se1` files from `assets/ephe` to `files/ephe` on first launch so Swiss Ephemeris can read them.
- `AccurateCalculator` loads Swiss Ephemeris by reflection, enforces Lahiri ayanamsa, and returns a `ChartResult`. When `swisseph.jar` is missing, it returns `null`.
- `AstrologyCalculator` implements a simplified Kotlin solver (Kepler equations, ayanamsa, sidereal conversion) to keep the UI usable offline.
- The `com.aakash.astro.astrology` package also includes reusable calculators for vargas, dashas, shadbala, yogas, Jaimini logic, and panchanga helpers.

## Data, storage, and assets
- City lookups: `CityDatabase` ships with curated Indian metros; `Geocoder` extends coverage when available.
- Saved charts: `SavedStore` writes JSON to `files/horoscopes/<timestamp>.json` and exposes list/load/delete helpers.
- Ephemeris and tables: `app/src/main/assets/ephe` contains Swiss `.se1` files; root `*.txt` and `*.json` files provide static tables.
- Custom views: `VedicChartView`, `SavChartView`, and `SbcOverlayView` encapsulate complex layouts.

## Build and run

### Gradle tasks
```bash
./gradlew assembleDebug
./gradlew test
./gradlew lintDebug
./gradlew ktlintCheck
./gradlew ktlintFormat
./gradlew bundleRelease
```

### Swiss Ephemeris setup
1. Place `swisseph.jar` inside `app/libs` (optional but recommended for accuracy).
2. Keep `.se1` files under `app/src/main/assets/ephe`.
3. On first launch `EphemerisPreparer` copies them to internal storage and `MainActivity.prepareEphemeris()` sets the ephemeris path.
4. The engine indicator under the chart toggles between Swiss Ephemeris and built-in solver.

### Signing
Create `keystore.properties` with `storeFile`, `storePassword`, `keyAlias`, and `keyPassword`. The release build picks it up automatically.

## Quality and testing
- JVM tests exist for core calculators and helpers under `app/src/test/java/com/aakash/astro/astrology`.
- Instrumented tests live under `app/src/androidTest`.
- Prefer adding regression vectors (known chart inputs and expected ascendants) when touching calculators.
- Run `./gradlew check` to execute unit tests, ktlint, and lint in one pass.

## Extending the app
1. Add or update a calculator in `com.aakash.astro.astrology` (keep it pure Kotlin).
2. Build a layout under `app/src/main/res/layout`.
3. Implement an `Activity` that consumes the calculator output.
4. Register the entry in `MainActivity.setupActionGrid()` and pass the standard `BirthContext` extras.
5. Update `AndroidManifest.xml`, string resources, and any assets as needed.
6. Add tests and update `docs/THIRD_PARTY_NOTICES.md` if new datasets or licenses are introduced.

## Contribution guide
- Follow the steps in "Extending the app" for new features.
- Keep domain logic Android-free and covered by tests.
- Run `./gradlew check` before sending changes.

## Utilities
- `scripts/capture_screenshots.sh` generates deterministic Play Store imagery.
- Root-level `*.png` assets are already sized for Play Store listings.

## Troubleshooting
- Swiss Ephemeris missing snackbar: verify `app/libs/swisseph.jar` and `app/src/main/assets/ephe`.
- No geocoder suggestions: emulator images often lack Geocoder; `CityDatabase` still works offline.
- Saved chart not loading: entries are de-duplicated by epoch; delete JSON under `files/horoscopes` if needed.
- Layout overlap on small screens: check nested scrolling containers and adjust layout XML.
