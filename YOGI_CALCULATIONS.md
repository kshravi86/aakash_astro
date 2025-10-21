# YOGI, AVAYOGI & SAHAYOGI CALCULATION GUIDE

Complete documentation of how Yogi, Avayogi, and Sahayogi are calculated in the Android app, with iOS/Swift implementation.

## Android Implementation Reference

**Source Files:**
- `app/src/main/java/com/aakash/astro/astrology/YogiCalculator.kt:3-62` - Core calculation logic
- `app/src/main/java/com/aakash/astro/YogiActivity.kt:48-55` - Usage example

---

## 1. YOGI CALCULATION

### Step 1: Calculate Yogi Point

**Formula:**
```
YogiPoint = (SunLongitude + MoonLongitude + 93°20') mod 360°
```

**Details:**
- **Constant**: `YOGI_OFFSET = 93.0 + 20.0/60.0 = 93.333333°`
- **Input**: Sidereal longitudes of Sun and Moon in degrees (0-360°)
- **Output**: Yogi Point in degrees (0-360°)

**Kotlin Code:**
```kotlin
fun computeYogiPoint(sunSidereal: Double, moonSidereal: Double): Double =
    normalize(sunSidereal + moonSidereal + YOGI_OFFSET)
```

### Step 2: Find Yogi Planet

The Yogi Planet is the **Nakshatra Lord** of the Yogi Point.

**Algorithm:**
```kotlin
// Each nakshatra = 13°20' = 13.333333°
NAK_LEN = 360.0 / 27.0

// Determine which nakshatra (0-26)
nakshatraIndex = floor(YogiPoint / NAK_LEN)

// Vimshottari Dasha Lord sequence (repeats every 9 nakshatras)
vimOrder = ["Ketu", "Venus", "Sun", "Moon", "Mars", "Rahu", "Jupiter", "Saturn", "Mercury"]

// Get the lord
yogiPlanet = vimOrder[nakshatraIndex % 9]
```

**Nakshatra Mapping:**
| Index | Nakshatra | Lord | Degrees |
|-------|-----------|------|---------|
| 0 | Ashwini | Ketu | 0° - 13°20' |
| 1 | Bharani | Venus | 13°20' - 26°40' |
| 2 | Krittika | Sun | 26°40' - 40° |
| 3 | Rohini | Moon | 40° - 53°20' |
| 4 | Mrigashira | Mars | 53°20' - 66°40' |
| 5 | Ardra | Rahu | 66°40' - 80° |
| 6 | Punarvasu | Jupiter | 80° - 93°20' |
| 7 | Pushya | Saturn | 93°20' - 106°40' |
| 8 | Ashlesha | Mercury | 106°40' - 120° |
| 9+ | (repeats) | | |

---

## 2. SAHAYOGI CALCULATION

Sahayogi is the **Zodiac Sign Lord** of the Yogi Point.

### Algorithm

**Step 1: Find which zodiac sign (0-11)**
```kotlin
signIndex = floor(YogiPoint / 30°)
```

**Step 2: Get the sign's ruling planet**

**Sign Lords Mapping:**
```kotlin
SignLords = {
  0  → Mars      // Aries
  1  → Venus     // Taurus
  2  → Mercury   // Gemini
  3  → Moon      // Cancer
  4  → Sun       // Leo
  5  → Mercury   // Virgo
  6  → Venus     // Libra
  7  → Mars      // Scorpio
  8  → Jupiter   // Sagittarius
  9  → Saturn    // Capricorn
  10 → Saturn    // Aquarius
  11 → Jupiter   // Pisces
}

sahayogi = SignLords[signIndex]
```

**Kotlin Code:**
```kotlin
fun signOf(deg: Double): ZodiacSign {
    val d = normalize(deg)
    val idx = (d / 30.0).toInt().coerceIn(0, 11)
    return ZodiacSign.entries[idx]
}

fun signLordOf(sign: ZodiacSign): Planet = when (sign) {
    ZodiacSign.ARIES -> Planet.MARS
    ZodiacSign.TAURUS -> Planet.VENUS
    ZodiacSign.GEMINI -> Planet.MERCURY
    ZodiacSign.CANCER -> Planet.MOON
    ZodiacSign.LEO -> Planet.SUN
    ZodiacSign.VIRGO -> Planet.MERCURY
    ZodiacSign.LIBRA -> Planet.VENUS
    ZodiacSign.SCORPIO -> Planet.MARS
    ZodiacSign.SAGITTARIUS -> Planet.JUPITER
    ZodiacSign.CAPRICORN -> Planet.SATURN
    ZodiacSign.AQUARIUS -> Planet.SATURN
    ZodiacSign.PISCES -> Planet.JUPITER
}
```

---

## 3. AVAYOGI CALCULATION

### Step 1: Calculate Avayogi Point

**Formula:**
```
AvayogiPoint = (YogiPoint + 186°40') mod 360°
```

**Details:**
- **Constant**: `AVAYOGI_OFFSET = 186.0 + 40.0/60.0 = 186.666667°`
- This is exactly 14 nakshatras (14 × 13°20' = 186°40') from the Yogi Point

**Kotlin Code:**
```kotlin
fun computeAvayogiPoint(yogiPoint: Double): Double =
    normalize(yogiPoint + AVAYOGI_OFFSET)
```

### Step 2: Find Avayogi Planet

Same process as Yogi - find the **Nakshatra Lord** of the Avayogi Point.

```kotlin
nakshatraIndex = floor(AvayogiPoint / 13.333333°)
avayogiPlanet = vimOrder[nakshatraIndex % 9]
```

### Alternative Method: 6th from Yogi

There's a **verification method** that counts 6 positions from the Yogi Planet in the Vimshottari sequence.

**Algorithm:**
```kotlin
// Find Yogi's position in vimOrder array
yogiIndex = vimOrder.indexOf(yogiPlanet)

// 6th from Yogi (inclusive counting = +5 in zero-based indexing)
avayogiVia6th = vimOrder[(yogiIndex + 5) % 9]
```

**Example:**
- If Yogi is **Ketu** (index 0) → Avayogi via 6th = vimOrder[5] = **Rahu**
- If Yogi is **Venus** (index 1) → Avayogi via 6th = vimOrder[6] = **Jupiter**

---

## iOS/Swift Implementation

### Complete Swift Code

```swift
import Foundation

struct YogiCalculator {
    // Constants
    private static let NAK_LEN = 360.0 / 27.0  // 13.333333° per nakshatra
    private static let YOGI_OFFSET = 93.0 + 20.0 / 60.0  // 93.333333°
    private static let AVAYOGI_OFFSET = 186.0 + 40.0 / 60.0  // 186.666667°

    // Vimshottari Dasha Lords in order
    private static let vimOrder = [
        "Ketu", "Venus", "Sun", "Moon", "Mars",
        "Rahu", "Jupiter", "Saturn", "Mercury"
    ]

    // Sign lords mapping
    private static let signLords: [String] = [
        "Mars",     // 0  - Aries
        "Venus",    // 1  - Taurus
        "Mercury",  // 2  - Gemini
        "Moon",     // 3  - Cancer
        "Sun",      // 4  - Leo
        "Mercury",  // 5  - Virgo
        "Venus",    // 6  - Libra
        "Mars",     // 7  - Scorpio
        "Jupiter",  // 8  - Sagittarius
        "Saturn",   // 9  - Capricorn
        "Saturn",   // 10 - Aquarius
        "Jupiter"   // 11 - Pisces
    ]

    // MARK: - Helper Functions

    /// Normalize degrees to 0-360 range
    private static func normalize(_ degrees: Double) -> Double {
        var result = degrees.truncatingRemainder(dividingBy: 360.0)
        if result < 0 {
            result += 360.0
        }
        return result
    }

    /// Get nakshatra index (0-26) from longitude
    private static func nakshatraIndex(from degrees: Double) -> Int {
        let normalized = normalize(degrees)
        let index = Int(floor(normalized / NAK_LEN))
        return min(max(index, 0), 26)
    }

    /// Get nakshatra lord from longitude
    private static func nakshatraLord(from degrees: Double) -> String {
        let index = nakshatraIndex(from: degrees)
        return vimOrder[index % vimOrder.count]
    }

    /// Get zodiac sign index (0-11) from longitude
    private static func signIndex(from degrees: Double) -> Int {
        let normalized = normalize(degrees)
        let index = Int(normalized / 30.0)
        return min(max(index, 0), 11)
    }

    /// Get sign lord from longitude
    private static func signLord(from degrees: Double) -> String {
        let index = signIndex(from: degrees)
        return signLords[index]
    }

    // MARK: - Main Calculations

    /// Calculate Yogi Point
    /// - Parameters:
    ///   - sunLongitude: Sun's sidereal longitude in degrees
    ///   - moonLongitude: Moon's sidereal longitude in degrees
    /// - Returns: Yogi Point in degrees (0-360)
    static func computeYogiPoint(sunLongitude: Double, moonLongitude: Double) -> Double {
        return normalize(sunLongitude + moonLongitude + YOGI_OFFSET)
    }

    /// Calculate Avayogi Point
    /// - Parameter yogiPoint: The Yogi Point in degrees
    /// - Returns: Avayogi Point in degrees (0-360)
    static func computeAvayogiPoint(yogiPoint: Double) -> Double {
        return normalize(yogiPoint + AVAYOGI_OFFSET)
    }

    /// Get Avayogi planet using 6th-from-Yogi method
    /// - Parameter yogiLord: The Yogi planet name
    /// - Returns: Avayogi planet name, or nil if yogiLord not found
    static func avayogiBy6th(yogiLord: String) -> String? {
        guard let index = vimOrder.firstIndex(where: { $0.caseInsensitiveCompare(yogiLord) == .orderedSame }) else {
            return nil
        }
        let avayogiIndex = (index + 5) % vimOrder.count
        return vimOrder[avayogiIndex]
    }

    // MARK: - Result Structure

    struct YogiResult {
        let yogiPoint: Double
        let yogiPlanet: String
        let sahayogi: String
        let avayogiPoint: Double
        let avayogiPlanet: String
        let avayogiVia6th: String?

        /// Format degrees as "DDD° MM'"
        func formatDegrees(_ degrees: Double) -> String {
            let normalized = normalize(degrees)
            let deg = Int(normalized)
            let min = Int((normalized - Double(deg)) * 60.0)
            return String(format: "%03d° %02d'", deg, min)
        }

        /// Get formatted description
        var description: String {
            return """
            Yogi Point: \(formatDegrees(yogiPoint))
            Yogi Planet: \(yogiPlanet)
            Sahayogi: \(sahayogi)
            Avayogi Point: \(formatDegrees(avayogiPoint))
            Avayogi Planet: \(avayogiPlanet) (6th method: \(avayogiVia6th ?? "N/A"))
            """
        }
    }

    /// Calculate all Yogi, Avayogi, and Sahayogi values
    /// - Parameters:
    ///   - sunLongitude: Sun's sidereal longitude in degrees
    ///   - moonLongitude: Moon's sidereal longitude in degrees
    /// - Returns: Complete YogiResult with all calculated values
    static func calculate(sunLongitude: Double, moonLongitude: Double) -> YogiResult {
        let yogiPoint = computeYogiPoint(sunLongitude: sunLongitude, moonLongitude: moonLongitude)
        let yogiPlanet = nakshatraLord(from: yogiPoint)
        let sahayogi = signLord(from: yogiPoint)
        let avayogiPoint = computeAvayogiPoint(yogiPoint: yogiPoint)
        let avayogiPlanet = nakshatraLord(from: avayogiPoint)
        let avayogiVia6th = avayogiBy6th(yogiLord: yogiPlanet)

        return YogiResult(
            yogiPoint: yogiPoint,
            yogiPlanet: yogiPlanet,
            sahayogi: sahayogi,
            avayogiPoint: avayogiPoint,
            avayogiPlanet: avayogiPlanet,
            avayogiVia6th: avayogiVia6th
        )
    }
}
```

### Usage Example

```swift
// Example planetary positions (sidereal longitudes)
let sunSidereal = 45.5      // Sun at 45.5°
let moonSidereal = 120.3    // Moon at 120.3°

// Calculate all values
let result = YogiCalculator.calculate(
    sunLongitude: sunSidereal,
    moonLongitude: moonSidereal
)

// Print results
print(result.description)

// Access individual values
print("Yogi Point: \(result.yogiPoint)°")
print("Yogi Planet: \(result.yogiPlanet)")
print("Sahayogi: \(result.sahayogi)")
print("Avayogi Point: \(result.avayogiPoint)°")
print("Avayogi Planet: \(result.avayogiPlanet)")

// Formatted output
print("Yogi Point: \(result.formatDegrees(result.yogiPoint))")
print("Avayogi Point: \(result.formatDegrees(result.avayogiPoint))")
```

---

## Critical Requirements for iOS Implementation

### 1. Input Data Requirements

**You MUST provide:**
- **Sun's Sidereal Longitude** (0-360°)
- **Moon's Sidereal Longitude** (0-360°)

**IMPORTANT:** These must be **SIDEREAL** coordinates, not tropical. Use **Lahiri Ayanamsa** for conversion.

### 2. Ephemeris Library

To calculate planetary positions, you need an ephemeris library. Options:

**Option 1: Swiss Ephemeris (Recommended)**
- Same library used by the Android app
- Most accurate
- C library with Swift bridging
- Free for non-commercial use

**Option 2: Alternative Libraries**
- AstroSwift (if available)
- Custom calculation library
- Online API (requires internet)

### 3. Coordinate System

**Tropical vs Sidereal:**
```
Sidereal Longitude = Tropical Longitude - Ayanamsa

// Lahiri Ayanamsa for 2024: ~24.15°
// (changes yearly, calculate based on date)
```

---

## Test Cases

### Test Case 1: Basic Calculation

**Input:**
```swift
sunSidereal = 45.0°
moonSidereal = 87.25°
```

**Expected Output:**
```
YogiPoint = (45.0 + 87.25 + 93.333333) mod 360 = 225.583333°

Nakshatra Index = floor(225.583 / 13.333) = 16
YogiPlanet = vimOrder[16 % 9] = vimOrder[7] = "Saturn"

Sign Index = floor(225.583 / 30) = 7
Sahayogi = signLords[7] = "Mars" (Scorpio)

AvayogiPoint = (225.583 + 186.667) mod 360 = 412.25 mod 360 = 52.25°

Nakshatra Index = floor(52.25 / 13.333) = 3
AvayogiPlanet = vimOrder[3] = "Moon"

Avayogi via 6th = vimOrder[(7 + 5) % 9] = vimOrder[3] = "Moon" ✓
```

### Test Case 2: Wraparound

**Input:**
```swift
sunSidereal = 320.0°
moonSidereal = 280.0°
```

**Expected Output:**
```
YogiPoint = (320 + 280 + 93.333) mod 360 = 693.333 mod 360 = 333.333°
YogiPlanet = vimOrder[floor(333.333/13.333) % 9] = vimOrder[25 % 9] = vimOrder[7] = "Saturn"
```

---

## Formula Summary

| Calculation | Formula | Offset |
|-------------|---------|--------|
| **Yogi Point** | (Sun° + Moon° + 93°20') mod 360° | 93°20' = 93.333333° |
| **Yogi Planet** | Nakshatra Lord of Yogi Point | - |
| **Sahayogi** | Sign Lord of Yogi Point | - |
| **Avayogi Point** | (Yogi Point + 186°40') mod 360° | 186°40' = 186.666667° |
| **Avayogi Planet** | Nakshatra Lord of Avayogi Point | - |
| **Avayogi (Alt)** | 6th from Yogi in Vimshottari sequence | +5 positions |

---

## References

**Android Source Files:**
- `app/src/main/java/com/aakash/astro/astrology/YogiCalculator.kt` - Main calculator
- `app/src/main/java/com/aakash/astro/YogiActivity.kt` - UI implementation
- `app/src/test/java/com/aakash/astro/astrology/YoginiDashaTest.kt` - Unit tests

**Constants Used:**
- Nakshatra length: 13°20' (13.333333°)
- Yogi offset: 93°20' (93.333333°)
- Avayogi offset: 186°40' (186.666667°)
- Vimshottari sequence: Ketu → Venus → Sun → Moon → Mars → Rahu → Jupiter → Saturn → Mercury

---

## Notes

1. **Accuracy**: The calculations use double-precision floating-point arithmetic
2. **Normalization**: All degree values are normalized to 0-360° range
3. **Index Safety**: Array indices are clamped to valid ranges (0-26 for nakshatras, 0-11 for signs)
4. **6th Counting**: The "6th from" method uses inclusive counting (hence +5 in zero-based indexing)
5. **Case Sensitivity**: Planet name comparisons are case-insensitive

---

Generated from Android app source code analysis - 2025
