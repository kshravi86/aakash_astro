# Aakash Astro Logic Diagrams

Concise mermaid diagrams covering the core runtime flows. Use these as a quick visual companion to `README.md` and `docs/PROJECT_DOCUMENTATION.md`.

## Birth Input → Chart → Feature Screens

```mermaid
flowchart TD
    U[User inputs date/time/place\n(MainActivity form)] --> BC[Build BirthContext\n(LocalDate, LocalTime, City, ZoneId)]
    BC --> EP[EphemerisPreparer.prepare()\ncopy assets/ephe -> files/ephe]
    EP --> AC{Swiss assets present?}
    AC -- yes --> SWE[AccurateCalculator.generateChart\nSwiss Ephemeris + Lahiri]
    AC -- no --> FALL[AstrologyCalculator.generateChart\npure Kotlin fallback]
    SWE --> CH[ChartResult + planet list]
    FALL --> CH
    CH --> UI[Natal preview\n(VedicChartView + planet chips)]
    UI --> GRID[Action tiles\n(ActionTileAdapter)]
    GRID --> FEAT[Feature Activity\nPanchanga/Dasha/Tara/etc.]
    BC --> FEAT
    FEAT --> RECOMP[Rebuild BirthDetails\nand run feature-specific calculator]
```

## Engine Detection & Fallback Logic

```mermaid
flowchart LR
    START[App launch or Generate] --> DETECT[Check swisseph.jar + assets/ephe]
    DETECT -- found --> SETPATH[Set ephemeris path on AccurateCalculator]
    SETPATH --> RUN[Attempt Swiss compute\n(swe_calc_ut, swe_houses_ex)]
    RUN -- success --> USE[Use Swiss Ephemeris output\nretrograde flags, cusps, positions]
    RUN -- failure --> FALLBACK[Log + snackbar; switch engine label]
    DETECT -- missing --> FALLBACK
    FALLBACK --> KOT[AstrologyCalculator\nKepler solver + Lahiri polynomial]
    KOT --> USE
```

## Saved Chart Lifecycle

```mermaid
flowchart TD
    GEN[Generated chart in MainActivity] --> SAVE[Save action]
    SAVE --> STORE[SavedStore.write()\nfiles/horoscopes/<timestamp>.json\n(dedup by epoch)]
    STORE --> LIST[SavedHoroscopesActivity\nload & list entries]
    LIST --> LOAD[Load button\nreturns to MainActivity with extras]
    LOAD --> REHYDRATE[Rebuild BirthContext\nre-render chart + tiles]
    LIST --> DELETE[Delete button]
    DELETE --> CLEAN[File removed; list refreshed]
```

