# Uttama Drekkana Implementation Guide for iOS

## Overview

**Uttama Drekkana** is an astrological concept where each zodiac sign is divided into three parts (drekkaṇas), each spanning 10 degrees. The "uttama" (best/superior) drekkana varies based on the sign's modality.

## Android Implementation Analysis

### Core Logic (DrekkanaUtils.kt:17-24)

The implementation consists of:

1. **Modality Classification**: Signs are grouped into three modalities:
   - **MOVABLE**: Aries, Cancer, Libra, Capricorn
   - **FIXED**: Taurus, Leo, Scorpio, Aquarius
   - **DUAL**: Gemini, Virgo, Sagittarius, Pisces

2. **Uttama Drekkana Rules**:
   - MOVABLE signs: Uttama is **0° to 10°**
   - FIXED signs: Uttama is **10° to 20°**
   - DUAL signs: Uttama is **20° to 30°**

3. **Degree Calculation**: Extract the degree within the sign (0-30°) from the absolute celestial longitude

### Usage in Android App

The function is called for:
- Ascendant/Lagna (MainActivity.kt:907)
- All planets (MainActivity.kt:934)
- Ghatika Lagna (MainActivity.kt:977)
- Hora Lagna (MainActivity.kt:1027)

Results are displayed as "Uttama Drekkana: Yes/No" in the planet position cards.

## iOS Implementation Instructions

### Step 1: Create the Modality Enum

```swift
enum Modality {
    case movable
    case fixed
    case dual
}
```

### Step 2: Create the Zodiac Sign Enum (if not already exists)

```swift
enum ZodiacSign: Int, CaseIterable {
    case aries = 0
    case taurus = 1
    case gemini = 2
    case cancer = 3
    case leo = 4
    case virgo = 5
    case libra = 6
    case scorpio = 7
    case sagittarius = 8
    case capricorn = 9
    case aquarius = 10
    case pisces = 11

    var displayName: String {
        switch self {
        case .aries: return "Aries"
        case .taurus: return "Taurus"
        case .gemini: return "Gemini"
        case .cancer: return "Cancer"
        case .leo: return "Leo"
        case .virgo: return "Virgo"
        case .libra: return "Libra"
        case .scorpio: return "Scorpio"
        case .sagittarius: return "Sagittarius"
        case .capricorn: return "Capricorn"
        case .aquarius: return "Aquarius"
        case .pisces: return "Pisces"
        }
    }
}
```

### Step 3: Create the DrekkanaUtils Class/Struct

```swift
struct DrekkanaUtils {

    // MARK: - Private Helper Functions

    /// Determines the modality of a zodiac sign
    private static func modality(of sign: ZodiacSign) -> Modality {
        switch sign {
        case .aries, .cancer, .libra, .capricorn:
            return .movable
        case .taurus, .leo, .scorpio, .aquarius:
            return .fixed
        case .gemini, .virgo, .sagittarius, .pisces:
            return .dual
        }
    }

    /// Extracts the degree within the sign (0-30) from absolute degree
    /// - Parameter absoluteDegree: The celestial longitude (0-360)
    /// - Returns: Degree within the sign (0-30)
    private static func degreeInSign(_ absoluteDegree: Double) -> Double {
        // Normalize to 0-360
        let normalized = ((absoluteDegree.truncatingRemainder(dividingBy: 360.0)) + 360.0)
            .truncatingRemainder(dividingBy: 360.0)

        // Get degree within sign (0-30)
        let inSign = ((normalized.truncatingRemainder(dividingBy: 30.0)) + 30.0)
            .truncatingRemainder(dividingBy: 30.0)

        return inSign
    }

    // MARK: - Public API

    /// Determines if a planet/point is in Uttama Drekkana
    /// - Parameters:
    ///   - sign: The zodiac sign
    ///   - absoluteDegree: The celestial longitude (0-360)
    /// - Returns: True if in uttama drekkana, false otherwise
    static func isUttamaDrekkana(sign: ZodiacSign, absoluteDegree: Double) -> Bool {
        let degreeInSign = degreeInSign(absoluteDegree)
        let signModality = modality(of: sign)

        switch signModality {
        case .movable:
            return degreeInSign >= 0.0 && degreeInSign < 10.0
        case .fixed:
            return degreeInSign >= 10.0 && degreeInSign < 20.0
        case .dual:
            return degreeInSign >= 20.0 && degreeInSign < 30.0
        }
    }
}
```

### Step 4: Usage Example

```swift
// Example usage for a planet or ascendant
let sunSign = ZodiacSign.aries
let sunDegree = 5.5  // 5°30' in Aries

let isUttama = DrekkanaUtils.isUttamaDrekkana(sign: sunSign, absoluteDegree: sunDegree)
print("Sun in Uttama Drekkana: \(isUttama ? "Yes" : "No")")  // Output: "Yes"

// For Moon at 15° Taurus (absolute: 45°)
let moonSign = ZodiacSign.taurus
let moonDegree = 45.0  // 15° Taurus

let moonUttama = DrekkanaUtils.isUttamaDrekkana(sign: moonSign, absoluteDegree: moonDegree)
print("Moon in Uttama Drekkana: \(moonUttama ? "Yes" : "No")")  // Output: "Yes"
```

### Step 5: Integration with Planet Position Model

```swift
struct PlanetPosition {
    let planet: Planet
    let degree: Double
    let sign: ZodiacSign
    let house: Int
    let isRetrograde: Bool

    // Computed property for uttama drekkana
    var isInUttamaDrekkana: Bool {
        return DrekkanaUtils.isUttamaDrekkana(sign: sign, absoluteDegree: degree)
    }
}
```

### Step 6: UI Display

In your SwiftUI view or UIKit cell:

```swift
// SwiftUI
Text("Uttama Drekkana: \(planet.isInUttamaDrekkana ? "Yes" : "No")")
    .font(.caption)
    .foregroundColor(.secondary)

// UIKit
let uttamaText = "Uttama Drekkana: \(isUttama ? "Yes" : "No")"
uttamaLabel.text = uttamaText
```

## Test Cases

To verify your implementation:

```swift
func testUttamaDrekkana() {
    // MOVABLE: Aries 0-10° should be uttama
    assert(DrekkanaUtils.isUttamaDrekkana(sign: .aries, absoluteDegree: 5.0) == true)
    assert(DrekkanaUtils.isUttamaDrekkana(sign: .aries, absoluteDegree: 15.0) == false)

    // FIXED: Taurus 10-20° should be uttama
    assert(DrekkanaUtils.isUttamaDrekkana(sign: .taurus, absoluteDegree: 45.0) == true)  // 15° Taurus
    assert(DrekkanaUtils.isUttamaDrekkana(sign: .taurus, absoluteDegree: 35.0) == false) // 5° Taurus

    // DUAL: Gemini 20-30° should be uttama
    assert(DrekkanaUtils.isUttamaDrekkana(sign: .gemini, absoluteDegree: 85.0) == true)  // 25° Gemini
    assert(DrekkanaUtils.isUttamaDrekkana(sign: .gemini, absoluteDegree: 65.0) == false) // 5° Gemini

    print("All tests passed!")
}
```

## Key Points

1. **Degree Normalization**: Always normalize the degree to handle edge cases (negative values, >360°)
2. **Sign-Degree Relationship**: The absolute degree must be converted to degree within sign (0-30°)
3. **Apply to All Entities**: Calculate for Ascendant, all planets, and special lagnas (Ghatika, Hora, etc.)
4. **Display**: Show as "Yes/No" or boolean indicator in the UI

This implementation is straightforward and requires no external dependencies beyond your existing astrology calculation system.

## Reference

- Android implementation: `app/src/main/java/com/aakash/astro/astrology/DrekkanaUtils.kt`
- Usage example: `app/src/main/java/com/aakash/astro/MainActivity.kt`
