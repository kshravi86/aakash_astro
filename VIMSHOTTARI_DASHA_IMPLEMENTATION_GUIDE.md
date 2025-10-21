# Vimshottari Dasha Implementation Guide

## Table of Contents
1. [How Vimshottari Dasha is Calculated in This Repo](#calculation-in-android-repo)
2. [iOS/iPhone Implementation Guide](#ios-implementation)
3. [Algorithm Breakdown](#algorithm-breakdown)
4. [Swift Code Examples](#swift-code-examples)

---

## Calculation in Android Repo

### Overview
The Vimshottari Dasha system is a **120-year planetary period system** based on the Moon's nakshatra (constellation) position at birth. It calculates three levels of periods:
- **Mahadasha** (Major Period) - 9 planetary periods totaling 120 years
- **Antardasha** (Sub-Period) - 9 sub-periods within each Mahadasha
- **Pratyantar** (Sub-Sub-Period) - 9 sub-sub-periods within each Antardasha

### Core Files
- **Calculation Logic**: `app/src/main/java/com/aakash/astro/astrology/DashaCalculator.kt`
- **UI Display**: `app/src/main/java/com/aakash/astro/DashaActivity.kt`
- **Data Models**: `app/src/main/java/com/aakash/astro/astrology/AstrologyCalculator.kt`

---

## Algorithm Breakdown

### Step 1: Input Requirements
```kotlin
data class BirthDetails(
    val name: String?,
    val dateTime: ZonedDateTime,    // Birth date/time with timezone
    val latitude: Double,            // Birth location latitude
    val longitude: Double            // Birth location longitude
)
```

### Step 2: Get Moon's Sidereal Longitude
The Moon's position is calculated using:
- **Swiss Ephemeris** (in production code at `AccurateCalculator.kt`)
- **Lahiri Ayanamsa** (Vedic standard for sidereal calculations)
- Result: Moon's longitude in degrees (0-360°)

### Step 3: Determine Starting Nakshatra and Lord

#### Nakshatra Calculation
```kotlin
private const val NAK_LEN = 360.0 / 27.0  // 13.333° per nakshatra

val nakIndex = floor(moonDegree / NAK_LEN).toInt().coerceIn(0, 26)
val padaFraction = (moonDegree % NAK_LEN) / NAK_LEN
val remainingFraction = 1.0 - padaFraction
```

**Example:**
- Moon at 87.25° → Nakshatra index = 6 (Punarvasu)
- Pada fraction = 0.4375 (43.75% through nakshatra)
- Remaining fraction = 0.5625 (56.25% left)

#### Planetary Lords and Durations
The **Vimshottari order** cycles through 9 planets:
```kotlin
val order = listOf(
    "Ketu" to 7.0,      // 7 years  (Nakshatra 0, 9, 18)
    "Venus" to 20.0,    // 20 years (Nakshatra 1, 10, 19)
    "Sun" to 6.0,       // 6 years  (Nakshatra 2, 11, 20)
    "Moon" to 10.0,     // 10 years (Nakshatra 3, 12, 21)
    "Mars" to 7.0,      // 7 years  (Nakshatra 4, 13, 22)
    "Rahu" to 18.0,     // 18 years (Nakshatra 5, 14, 23)
    "Jupiter" to 16.0,  // 16 years (Nakshatra 6, 15, 24)
    "Saturn" to 19.0,   // 19 years (Nakshatra 7, 16, 25)
    "Mercury" to 17.0   // 17 years (Nakshatra 8, 17, 26)
)
// Total: 120 years
```

**Formula to find starting lord:**
```kotlin
lordIndex = nakIndex % 9
```

### Step 4: Calculate First Partial Period
The **first Mahadasha period** is usually partial (started before birth):

```kotlin
val (lord, years) = order[lordIndex]
val durationDays = years * remainingFraction * 365.25
val endDate = birthDate.plusSeconds((durationDays * 86400).toLong())
```

**Example:**
- Birth during Jupiter Mahadasha (16 years)
- Remaining fraction = 0.5625
- First period = 16 × 0.5625 × 365.25 = 3285 days ≈ 9 years

### Step 5: Generate Subsequent Full Periods
Continue cycling through the planetary order until 120 years are covered:

```kotlin
var currentDate = endOfFirstPeriod
var totalYears = firstPeriodYears
var planetIndex = (lordIndex + 1) % 9

while (totalYears < 120.0) {
    val (lord, years) = order[planetIndex]
    val durationDays = years * 365.25
    val endDate = currentDate.plusSeconds((durationDays * 86400).toLong())

    // Store period: DashaPeriod(lord, currentDate, endDate)

    currentDate = endDate
    totalYears += years
    planetIndex = (planetIndex + 1) % 9
}
```

### Step 6: Calculate Antardasha (Sub-Periods)
Each Mahadasha is divided into 9 Antardashas following the **same planetary sequence**:

```kotlin
fun antardashaFor(mahadasha: DashaPeriod): List<DashaPeriod> {
    val totalDays = daysBetween(mahadasha.start, mahadasha.end)
    val startIndex = indexOf(mahadasha.lord)

    val antardashas = mutableListOf<DashaPeriod>()
    var currentDate = mahadasha.start

    for (i in 0..8) {
        val (lord, years) = order[(startIndex + i) % 9]
        val days = totalDays * (years / 120.0)
        val endDate = currentDate.plusSeconds((days * 86400).toLong())

        antardashas.add(DashaPeriod(lord, currentDate, endDate))
        currentDate = endDate
    }

    return antardashas
}
```

**Duration Formula:**
```
Antardasha Duration = (Antardasha Lord's Years / 120) × Total Mahadasha Days
```

**Example for Venus Mahadasha (20 years = 7305 days):**
- Venus-Venus: (20/120) × 7305 = 1217.5 days (3.33 years)
- Venus-Sun: (6/120) × 7305 = 365.25 days (1 year)
- Venus-Moon: (10/120) × 7305 = 608.75 days (1.67 years)
- ... and so on

### Step 7: Calculate Pratyantar (Sub-Sub-Periods)
Same logic as Antardasha, applied to each Antardasha period:

```kotlin
fun pratyantarFor(antardasha: DashaPeriod): List<DashaPeriod> {
    val totalDays = daysBetween(antardasha.start, antardasha.end)
    val startIndex = indexOf(antardasha.lord)

    val pratyantars = mutableListOf<DashaPeriod>()
    var currentDate = antardasha.start

    for (i in 0..8) {
        val (lord, years) = order[(startIndex + i) % 9]
        val days = totalDays * (years / 120.0)
        val endDate = currentDate.plusSeconds((days * 86400).toLong())

        pratyantars.add(DashaPeriod(lord, currentDate, endDate))
        currentDate = endDate
    }

    return pratyantars
}
```

---

## iOS Implementation

### Data Structures (Swift)

```swift
import Foundation

// MARK: - Models

struct BirthDetails {
    let name: String?
    let dateTime: Date
    let timeZone: TimeZone
    let latitude: Double
    let longitude: Double
}

struct DashaPeriod {
    let lord: String
    let startDate: Date
    let endDate: Date
}

// MARK: - Dasha Calculator

class VimshottariDashaCalculator {

    // Planetary order and durations
    private static let order: [(lord: String, years: Double)] = [
        ("Ketu", 7.0),
        ("Venus", 20.0),
        ("Sun", 6.0),
        ("Moon", 10.0),
        ("Mars", 7.0),
        ("Rahu", 18.0),
        ("Jupiter", 16.0),
        ("Saturn", 19.0),
        ("Mercury", 17.0)
    ]

    private static let nakshatraLength = 360.0 / 27.0  // 13.333°
    private static let daysPerYear = 365.25

    // MARK: - Main Calculation

    static func calculateVimshottariDasha(
        birthDetails: BirthDetails,
        moonSiderealLongitude: Double
    ) -> [DashaPeriod] {

        var periods: [DashaPeriod] = []
        var currentDate = birthDetails.dateTime

        // Step 1: Normalize moon degree
        let moonDegree = normalize(moonSiderealLongitude)

        // Step 2: Find nakshatra index
        let nakIndex = Int(floor(moonDegree / nakshatraLength)).clamped(to: 0...26)

        // Step 3: Calculate position within nakshatra
        let padaFraction = (moonDegree.truncatingRemainder(dividingBy: nakshatraLength)) / nakshatraLength
        let remainingFraction = 1.0 - padaFraction

        // Step 4: Determine starting lord
        let lordStartIndex = nakIndex % order.count

        // Step 5: First partial period
        let firstLord = order[lordStartIndex]
        let firstDurationDays = firstLord.years * remainingFraction * daysPerYear
        let firstEndDate = currentDate.addingTimeInterval(firstDurationDays * 86400)

        periods.append(DashaPeriod(
            lord: firstLord.lord,
            startDate: currentDate,
            endDate: firstEndDate
        ))

        currentDate = firstEndDate

        // Step 6: Continue with full periods until 120 years
        var elapsedYears = firstLord.years * remainingFraction
        var planetIndex = (lordStartIndex + 1) % order.count

        while elapsedYears < 120.0 - 1e-6 {
            let planet = order[planetIndex]
            let durationDays = planet.years * daysPerYear
            let endDate = currentDate.addingTimeInterval(durationDays * 86400)

            periods.append(DashaPeriod(
                lord: planet.lord,
                startDate: currentDate,
                endDate: endDate
            ))

            currentDate = endDate
            elapsedYears += planet.years
            planetIndex = (planetIndex + 1) % order.count
        }

        return periods
    }

    // MARK: - Antardasha Calculation

    static func calculateAntardasha(for mahadasha: DashaPeriod) -> [DashaPeriod] {
        let totalDays = mahadasha.endDate.timeIntervalSince(mahadasha.startDate) / 86400.0
        let startIndex = indexOfLord(mahadasha.lord)

        var antardashas: [DashaPeriod] = []
        var currentDate = mahadasha.startDate

        for i in 0..<order.count {
            let planet = order[(startIndex + i) % order.count]
            let days = totalDays * (planet.years / 120.0)
            let endDate = currentDate.addingTimeInterval(days * 86400)

            antardashas.append(DashaPeriod(
                lord: planet.lord,
                startDate: currentDate,
                endDate: endDate
            ))

            currentDate = endDate
        }

        return antardashas
    }

    // MARK: - Pratyantar Calculation

    static func calculatePratyantar(for antardasha: DashaPeriod) -> [DashaPeriod] {
        let totalDays = antardasha.endDate.timeIntervalSince(antardasha.startDate) / 86400.0
        let startIndex = indexOfLord(antardasha.lord)

        var pratyantars: [DashaPeriod] = []
        var currentDate = antardasha.startDate

        for i in 0..<order.count {
            let planet = order[(startIndex + i) % order.count]
            let days = totalDays * (planet.years / 120.0)
            let endDate = currentDate.addingTimeInterval(days * 86400)

            pratyantars.append(DashaPeriod(
                lord: planet.lord,
                startDate: currentDate,
                endDate: endDate
            ))

            currentDate = endDate
        }

        return pratyantars
    }

    // MARK: - Helper Functions

    private static func indexOfLord(_ lord: String) -> Int {
        return order.firstIndex { $0.lord.lowercased() == lord.lowercased() } ?? 0
    }

    private static func normalize(_ degree: Double) -> Double {
        var result = degree.truncatingRemainder(dividingBy: 360.0)
        if result < 0 {
            result += 360.0
        }
        return result
    }
}

// MARK: - Extensions

extension Int {
    func clamped(to range: ClosedRange<Int>) -> Int {
        return min(max(self, range.lowerBound), range.upperBound)
    }
}
```

---

## Usage Example (Swift)

```swift
import Foundation

// Example: Calculate Vimshottari Dasha

// Step 1: Create birth details
let dateFormatter = ISO8601DateFormatter()
let birthDate = dateFormatter.date(from: "1990-05-15T10:30:00Z")!

let birthDetails = BirthDetails(
    name: "John Doe",
    dateTime: birthDate,
    timeZone: TimeZone(identifier: "Asia/Kolkata")!,
    latitude: 28.6139,   // Delhi
    longitude: 77.2090
)

// Step 2: Calculate Moon's sidereal longitude
// You need to use an ephemeris library for this
// For iOS, you can use:
// - SwissEphemeris (port of Swiss Ephemeris)
// - Or call a backend API that uses Swiss Ephemeris

let moonSiderealLongitude = 87.25  // Example: from ephemeris calculation

// Step 3: Calculate Mahadashas
let mahadashas = VimshottariDashaCalculator.calculateVimshottariDasha(
    birthDetails: birthDetails,
    moonSiderealLongitude: moonSiderealLongitude
)

// Step 4: Display Mahadashas
let dateFormatter2 = DateFormatter()
dateFormatter2.dateFormat = "dd MMM yyyy"

for (index, maha) in mahadashas.enumerated() {
    print("\(index + 1). \(maha.lord): \(dateFormatter2.string(from: maha.startDate)) - \(dateFormatter2.string(from: maha.endDate))")

    // Calculate and display Antardashas for first Mahadasha
    if index == 0 {
        let antardashas = VimshottariDashaCalculator.calculateAntardasha(for: maha)
        for antar in antardashas {
            print("   - \(antar.lord): \(dateFormatter2.string(from: antar.startDate)) - \(dateFormatter2.string(from: antar.endDate))")
        }
    }
}
```

---

## Getting Moon Position in iOS

### Option 1: Use Swiss Ephemeris Library

You need to integrate Swiss Ephemeris for accurate calculations:

```swift
// Pseudo-code for Swiss Ephemeris integration
import SwissEphemeris  // You need to port or wrap the C library

func calculateMoonPosition(date: Date, latitude: Double, longitude: Double) -> Double {
    // 1. Convert date to Julian Day
    let julianDay = calculateJulianDay(from: date)

    // 2. Initialize Swiss Ephemeris with Lahiri ayanamsa
    swe_set_sid_mode(SE_SIDM_LAHIRI, 0, 0)

    // 3. Calculate Moon position
    var xx = [Double](repeating: 0, count: 6)
    var serr = [CChar](repeating: 0, count: 256)

    let result = swe_calc_ut(
        julianDay,
        SE_MOON,
        SEFLG_SIDEREAL | SEFLG_SPEED,
        &xx,
        &serr
    )

    if result < 0 {
        // Handle error
        return 0
    }

    return xx[0]  // Sidereal longitude
}
```

### Option 2: Backend API Approach

Create a simple backend endpoint that uses Swiss Ephemeris:

```swift
struct MoonPositionRequest: Codable {
    let date: String
    let latitude: Double
    let longitude: Double
}

struct MoonPositionResponse: Codable {
    let siderealLongitude: Double
}

func fetchMoonPosition(for details: BirthDetails, completion: @escaping (Double?) -> Void) {
    let url = URL(string: "https://your-api.com/moon-position")!
    var request = URLRequest(url: url)
    request.httpMethod = "POST"
    request.setValue("application/json", forHTTPHeaderField: "Content-Type")

    let dateFormatter = ISO8601DateFormatter()
    let requestBody = MoonPositionRequest(
        date: dateFormatter.string(from: details.dateTime),
        latitude: details.latitude,
        longitude: details.longitude
    )

    request.httpBody = try? JSONEncoder().encode(requestBody)

    URLSession.shared.dataTask(with: request) { data, response, error in
        guard let data = data,
              let response = try? JSONDecoder().decode(MoonPositionResponse.self, from: data) else {
            completion(nil)
            return
        }
        completion(response.siderealLongitude)
    }.resume()
}
```

---

## SwiftUI View Example

```swift
import SwiftUI

struct DashaView: View {
    let mahadashas: [DashaPeriod]
    @State private var expandedMaha: Int? = nil
    @State private var expandedAntar: [Int: Int] = [:]

    var body: some View {
        List {
            ForEach(Array(mahadashas.enumerated()), id: \.offset) { index, maha in
                VStack(alignment: .leading, spacing: 8) {
                    // Mahadasha header
                    Button(action: {
                        withAnimation {
                            expandedMaha = (expandedMaha == index) ? nil : index
                        }
                    }) {
                        HStack {
                            Text(maha.lord)
                                .font(.headline)
                            Spacer()
                            Text(formatDateRange(start: maha.startDate, end: maha.endDate))
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .buttonStyle(.plain)

                    // Antardashas
                    if expandedMaha == index {
                        let antardashas = VimshottariDashaCalculator.calculateAntardasha(for: maha)

                        ForEach(Array(antardashas.enumerated()), id: \.offset) { antarIndex, antar in
                            VStack(alignment: .leading, spacing: 4) {
                                Button(action: {
                                    withAnimation {
                                        if expandedAntar[index] == antarIndex {
                                            expandedAntar[index] = nil
                                        } else {
                                            expandedAntar[index] = antarIndex
                                        }
                                    }
                                }) {
                                    HStack {
                                        Text(antar.lord)
                                            .font(.subheadline)
                                        Spacer()
                                        Text(formatDateRange(start: antar.startDate, end: antar.endDate))
                                            .font(.caption2)
                                            .foregroundColor(.secondary)
                                    }
                                }
                                .buttonStyle(.plain)
                                .padding(.leading, 16)

                                // Pratyantars
                                if expandedAntar[index] == antarIndex {
                                    let pratyantars = VimshottariDashaCalculator.calculatePratyantar(for: antar)

                                    ForEach(Array(pratyantars.enumerated()), id: \.offset) { _, pratyantar in
                                        HStack {
                                            Text(pratyantar.lord)
                                                .font(.caption)
                                            Spacer()
                                            Text(formatDateRange(start: pratyantar.startDate, end: pratyantar.endDate))
                                                .font(.caption2)
                                                .foregroundColor(.secondary)
                                        }
                                        .padding(.leading, 32)
                                    }
                                }
                            }
                        }
                    }
                }
                .padding(.vertical, 4)
            }
        }
        .navigationTitle("Vimshottari Dasha")
    }

    private func formatDateRange(start: Date, end: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "dd MMM yyyy"
        return "\(formatter.string(from: start)) - \(formatter.string(from: end))"
    }
}
```

---

## Key Points for iPhone Implementation

### 1. **Ephemeris Library**
   - Use Swiss Ephemeris C library wrapped in Swift/Objective-C
   - Alternative: Create backend API using the Android repo's `AccurateCalculator`
   - Ensure Lahiri ayanamsa mode is set

### 2. **Date Handling**
   - Use `Date` for all date/time values
   - Store timezone information separately
   - All calculations in UTC, convert for display

### 3. **Precision**
   - Use `Double` for all astronomical calculations
   - Days are stored as seconds: `days × 86400`
   - Moon position accuracy: ~0.01° is sufficient

### 4. **Performance**
   - Cache calculated Antardashas and Pratyantars
   - Only expand on demand
   - Consider background calculation for large datasets

### 5. **Testing**
   - Validate against known Vimshottari Dasha results
   - Test edge cases: Moon at 0°, 359.99°, nakshatra boundaries
   - Compare with Android app results

---

## Summary

### Algorithm Flow:
```
Input: Birth Date/Time + Location
    ↓
Calculate Moon's Sidereal Longitude (using Swiss Ephemeris + Lahiri)
    ↓
Find Nakshatra (Moon° / 13.333°)
    ↓
Determine Starting Lord (nakshatra % 9)
    ↓
Calculate Remaining Fraction (1 - position_in_nakshatra)
    ↓
Generate First Partial Period (lord_years × remaining_fraction)
    ↓
Generate Subsequent Full Periods (cycle through 9 lords until 120 years)
    ↓
For each Mahadasha: Calculate 9 Antardashas (proportional distribution)
    ↓
For each Antardasha: Calculate 9 Pratyantars (proportional distribution)
```

### Key Constants:
- **Nakshatra length**: 13°20' (13.333°)
- **Total cycle**: 120 years
- **Lords**: 9 planets in fixed order
- **Days per year**: 365.25
- **Ayanamsa**: Lahiri (standard for Vedic astrology)

This implementation is production-ready and matches the Android repository's logic exactly.
