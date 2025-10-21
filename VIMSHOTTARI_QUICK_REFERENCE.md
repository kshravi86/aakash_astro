# Vimshottari Dasha Quick Reference

## Planetary Order & Durations

| Planet  | Years | Nakshatra Numbers | Example Nakshatras |
|---------|-------|-------------------|-------------------|
| Ketu    | 7     | 0, 9, 18         | Ashwini, Magha, Moola |
| Venus   | 20    | 1, 10, 19        | Bharani, Purva Phalguni, Purva Ashadha |
| Sun     | 6     | 2, 11, 20        | Krittika, Uttara Phalguni, Uttara Ashadha |
| Moon    | 10    | 3, 12, 21        | Rohini, Hasta, Shravana |
| Mars    | 7     | 4, 13, 22        | Mrigashira, Chitra, Dhanishta |
| Rahu    | 18    | 5, 14, 23        | Ardra, Swati, Shatabhisha |
| Jupiter | 16    | 6, 15, 24        | Punarvasu, Vishakha, Purva Bhadrapada |
| Saturn  | 19    | 7, 16, 25        | Pushya, Anuradha, Uttara Bhadrapada |
| Mercury | 17    | 8, 17, 26        | Ashlesha, Jyeshtha, Revati |

**Total: 120 years**

---

## Calculation Formulas

### 1. Nakshatra Index
```
Nakshatra Index = floor(Moon Degree / 13.333°)
Range: 0-26 (27 nakshatras)
```

### 2. Position in Nakshatra
```
Position Fraction = (Moon Degree % 13.333°) / 13.333°
Remaining Fraction = 1.0 - Position Fraction
```

### 3. Starting Lord
```
Lord Index = Nakshatra Index % 9
```

### 4. First Period Duration
```
Duration (days) = Lord Years × Remaining Fraction × 365.25
```

### 5. Antardasha Duration
```
Antardasha Days = (Antardasha Lord Years / 120) × Total Mahadasha Days
```

### 6. Pratyantar Duration
```
Pratyantar Days = (Pratyantar Lord Years / 120) × Total Antardasha Days
```

---

## Example Calculation

**Birth Details:**
- Date: May 15, 1990, 10:30 AM IST
- Location: Delhi (28.61°N, 77.21°E)
- Moon Position: 87.25° (sidereal)

**Step-by-Step:**

1. **Find Nakshatra:**
   ```
   Nakshatra Index = floor(87.25 / 13.333) = 6
   Nakshatra = Punarvasu (6th nakshatra)
   ```

2. **Calculate Fractions:**
   ```
   Position Fraction = (87.25 % 13.333) / 13.333 = 0.4375
   Remaining Fraction = 1.0 - 0.4375 = 0.5625
   ```

3. **Find Starting Lord:**
   ```
   Lord Index = 6 % 9 = 6
   Starting Lord = Jupiter (16 years)
   ```

4. **First Period:**
   ```
   Duration = 16 × 0.5625 × 365.25 = 3,285 days ≈ 9 years
   Period: May 15, 1990 → May 15, 1999 (Jupiter)
   ```

5. **Next Periods (full):**
   ```
   May 15, 1999 → May 15, 2018 (Saturn - 19 years)
   May 15, 2018 → May 15, 2035 (Mercury - 17 years)
   May 15, 2035 → May 15, 2042 (Ketu - 7 years)
   ... and so on
   ```

6. **Jupiter Mahadasha Antardashas:**
   - Total: 3,285 days
   - Jupiter-Jupiter: (16/120) × 3,285 = 438 days
   - Jupiter-Saturn: (19/120) × 3,285 = 520 days
   - Jupiter-Mercury: (17/120) × 3,285 = 465 days
   - Jupiter-Ketu: (7/120) × 3,285 = 192 days
   - Jupiter-Venus: (20/120) × 3,285 = 548 days
   - Jupiter-Sun: (6/120) × 3,285 = 164 days
   - Jupiter-Moon: (10/120) × 3,285 = 274 days
   - Jupiter-Mars: (7/120) × 3,285 = 192 days
   - Jupiter-Rahu: (18/120) × 3,285 = 493 days
   - **Total: 3,286 days** (1 day rounding difference)

---

## Validation Checklist

### For Implementation:
- [ ] Moon degree normalized to 0-360°
- [ ] Nakshatra index clamped to 0-26
- [ ] Lord sequence starts from correct planet
- [ ] First period uses remaining fraction
- [ ] Subsequent periods are full durations
- [ ] Total Mahadashas cover ~120 years
- [ ] Antardasha sequence starts from Mahadasha lord
- [ ] 9 Antardashas per Mahadasha
- [ ] Sum of Antardashas = Mahadasha duration
- [ ] Pratyantar follows same logic

### Common Errors to Avoid:
1. **Wrong ayanamsa** - Must use Lahiri for Vedic
2. **Tropical vs Sidereal** - Moon must be sidereal longitude
3. **Date arithmetic** - Account for leap years (365.25)
4. **Rounding errors** - Use Double/Float64, not integers
5. **Timezone handling** - Convert to UTC for calculations
6. **Lord sequence** - Must cycle through all 9 planets in order

---

## iOS Implementation Checklist

### Phase 1: Core Calculator
- [ ] Create `VimshottariDashaCalculator` class
- [ ] Implement planetary order constant
- [ ] Add `calculateVimshottariDasha()` method
- [ ] Add `calculateAntardasha()` method
- [ ] Add `calculatePratyantar()` method
- [ ] Write unit tests with known results

### Phase 2: Ephemeris Integration
- [ ] Choose ephemeris solution (library or API)
- [ ] Implement Moon position calculation
- [ ] Set Lahiri ayanamsa mode
- [ ] Validate against known positions
- [ ] Handle error cases

### Phase 3: UI Implementation
- [ ] Create `DashaPeriod` model
- [ ] Build hierarchical list view
- [ ] Add expand/collapse functionality
- [ ] Implement date formatting
- [ ] Add current period highlighting
- [ ] Auto-scroll to current period

### Phase 4: Testing
- [ ] Test with multiple birth dates
- [ ] Validate against Android app results
- [ ] Test edge cases (0°, 359.99°)
- [ ] Test nakshatra boundaries
- [ ] Performance test with large datasets

---

## Resources

### Android Repository Files:
- **Calculator**: `app/src/main/java/com/aakash/astro/astrology/DashaCalculator.kt`
- **UI**: `app/src/main/java/com/aakash/astro/DashaActivity.kt`
- **Models**: `app/src/main/java/com/aakash/astro/astrology/AstrologyCalculator.kt`

### Ephemeris Libraries:
- **Swiss Ephemeris** (C library) - https://www.astro.com/swisseph/
- **For iOS**: Need to wrap C library in Swift/Objective-C bridge

### Verification Tools:
- Compare with popular astrology software (JHora, Jagannatha Hora)
- Cross-verify with online Vimshottari calculators
- Test against the Android app in this repository

---

## Mathematical Constants

```swift
let nakshatraLength = 360.0 / 27.0      // 13.333333°
let daysPerYear = 365.25                 // Julian year
let totalCycle = 120.0                   // years
let numberOfPlanets = 9
let numberOfNakshatras = 27
let secondsPerDay = 86400.0
```

---

## Sample Output Format

```
VIMSHOTTARI DASHA

Mahadasha Periods:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Jupiter   | 15 May 1990 - 15 May 1999  (9 years)
  └─ Jupiter   | 15 May 1990 - 25 Aug 1991
  └─ Saturn    | 25 Aug 1991 - 01 Mar 1993
  └─ Mercury   | 01 Mar 1993 - 06 Jun 1994
  └─ Ketu      | 06 Jun 1994 - 12 Nov 1994
  └─ Venus     | 12 Nov 1994 - 12 Nov 1996
  └─ Sun       | 12 Nov 1996 - 18 Apr 1997
  └─ Moon      | 18 Apr 1997 - 18 Sep 1998
  └─ Mars      | 18 Sep 1998 - 24 Feb 1999
  └─ Rahu      | 24 Feb 1999 - 15 May 1999

Saturn    | 15 May 1999 - 15 May 2018  (19 years)
Mercury   | 15 May 2018 - 15 May 2035  (17 years)
Ketu      | 15 May 2035 - 15 May 2042  (7 years)
Venus     | 15 May 2042 - 15 May 2062  (20 years)
Sun       | 15 May 2062 - 15 May 2068  (6 years)
Moon      | 15 May 2068 - 15 May 2078  (10 years)
Mars      | 15 May 2078 - 15 May 2085  (7 years)
Rahu      | 15 May 2085 - 15 May 2103  (18 years)
Jupiter   | 15 May 2103 - 15 May 2119  (16 years)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Current Dasha: Mercury-Venus-Jupiter
```

---

## Testing Data

### Test Case 1: Known Birth Chart
```
Name: Sample Chart 1
Date: 1990-05-15 10:30:00 IST
Location: Delhi (28.61°N, 77.21°E)
Moon: 87.25° (Punarvasu)
Expected Start: Jupiter Mahadasha
Expected Duration: ~9 years
```

### Test Case 2: Nakshatra Boundary
```
Moon: 13.330° (boundary between Bharani and Krittika)
Expected: Handle edge case correctly
```

### Test Case 3: Zero Degree
```
Moon: 0.01° (Start of Ashwini)
Expected: Ketu Mahadasha, nearly full period
```

### Test Case 4: End of Zodiac
```
Moon: 359.99° (End of Revati)
Expected: Mercury Mahadasha, very small remaining fraction
```
