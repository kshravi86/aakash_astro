# Pushkara Navamsha Implementation Guide

## Overview

This guide explains how the Pushkara Navamsha logic is implemented in the Android app and provides step-by-step instructions for implementing the same functionality in an iPhone app.

## What is Pushkara Navamsha?

Pushkara Navamsha identifies specific degree ranges within zodiac signs where planets are considered highly auspicious in Vedic astrology. Each element (Fire, Earth, Air, Water) has its own specific degree bands.

---

## Android Implementation Analysis

### Core Logic Location

**File**: `app/src/main/java/com/aakash/astro/astrology/PushkaraNavamsha.kt`

### Degree Bands by Element

The algorithm uses element-based degree bands within each 30° sign:

- **Fire signs** (Aries, Leo, Sagittarius):
  - 20°00'–23°20' (Libra navamsha)
  - 26°40'–30°00' (Sagittarius navamsha)

- **Earth signs** (Taurus, Virgo, Capricorn):
  - 06°40'–10°00' (Pisces navamsha)
  - 13°20'–16°40' (Taurus navamsha)

- **Air signs** (Gemini, Libra, Aquarius):
  - 16°40'–20°00' (Pisces navamsha)
  - 23°20'–26°40' (Taurus navamsha)

- **Water signs** (Cancer, Scorpio, Pisces):
  - 00°00'–03°20' (Cancer navamsha)
  - 06°40'–10°00' (Virgo navamsha)

### Key Functions

```kotlin
// Convert degrees and minutes to decimal
private fun deg(d: Int, m: Int): Double = d + m / 60.0

// Get element bands for a sign
private fun elementBands(sign: ZodiacSign): List<Band> = when (sign) {
    ZodiacSign.ARIES, ZodiacSign.LEO, ZodiacSign.SAGITTARIUS -> fire
    ZodiacSign.TAURUS, ZodiacSign.VIRGO, ZodiacSign.CAPRICORN -> earth
    ZodiacSign.GEMINI, ZodiacSign.LIBRA, ZodiacSign.AQUARIUS -> air
    ZodiacSign.CANCER, ZodiacSign.SCORPIO, ZodiacSign.PISCES -> water
}

// Check if degree is in Pushkara band
fun bandIfPushkara(sign: ZodiacSign, degreeWithinSign: Double): String?
fun isPushkara(sign: ZodiacSign, degreeWithinSign: Double): Boolean
```

### Algorithm Flow

1. Extract degree within sign: `degree % 30.0`
2. Map zodiac sign to its element (Fire/Earth/Air/Water)
3. Get the appropriate degree bands for that element
4. Check if the degree falls within any band range
5. Return the band label if found, else null

---

## iPhone/iOS Implementation

### Step 1: Create Data Models (Swift)

```swift
// MARK: - Enums

enum ZodiacSign: Int, CaseIterable {
    case aries = 0, taurus, gemini, cancer, leo, virgo
    case libra, scorpio, sagittarius, capricorn, aquarius, pisces

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

    static func fromDegree(_ degree: Double) -> ZodiacSign {
        let normalized = degree.truncatingRemainder(dividingBy: 360)
        let index = Int(normalized / 30.0).clamped(to: 0...11)
        return ZodiacSign(rawValue: index) ?? .aries
    }
}

enum Planet: String, CaseIterable {
    case sun, moon, mercury, venus, mars, jupiter, saturn, rahu, ketu

    var displayName: String {
        return rawValue.capitalized
    }
}

// MARK: - Data Structures

struct PlanetPosition {
    let planet: Planet
    let degree: Double
    let sign: ZodiacSign
    let house: Int
    let isRetrograde: Bool

    var name: String { planet.displayName }
}

struct ChartResult {
    let ascendantDegree: Double
    let ascendantSign: ZodiacSign
    let planets: [PlanetPosition]
}
```

### Step 2: Implement Core Pushkara Logic

```swift
// MARK: - Pushkara Navamsha Calculator

struct PushkaraNavamsha {

    // MARK: - Band Definition

    private struct Band {
        let startDeg: Double
        let endDeg: Double
        let label: String
    }

    // MARK: - Element Bands

    private static let fire: [Band] = [
        Band(startDeg: 20.0,
             endDeg: deg(23, 20),
             label: "20°00'–23°20' (Libra navamsha)"),
        Band(startDeg: deg(26, 40),
             endDeg: 30.0,
             label: "26°40'–30°00' (Sagittarius navamsha)")
    ]

    private static let earth: [Band] = [
        Band(startDeg: deg(6, 40),
             endDeg: 10.0,
             label: "06°40'–10°00' (Pisces navamsha)"),
        Band(startDeg: deg(13, 20),
             endDeg: deg(16, 40),
             label: "13°20'–16°40' (Taurus navamsha)")
    ]

    private static let air: [Band] = [
        Band(startDeg: deg(16, 40),
             endDeg: 20.0,
             label: "16°40'–20°00' (Pisces navamsha)"),
        Band(startDeg: deg(23, 20),
             endDeg: deg(26, 40),
             label: "23°20'–26°40' (Taurus navamsha)")
    ]

    private static let water: [Band] = [
        Band(startDeg: 0.0,
             endDeg: deg(3, 20),
             label: "00°00'–03°20' (Cancer navamsha)"),
        Band(startDeg: deg(6, 40),
             endDeg: 10.0,
             label: "06°40'–10°00' (Virgo navamsha)")
    ]

    // MARK: - Helper Functions

    private static func deg(_ degrees: Int, _ minutes: Int) -> Double {
        return Double(degrees) + Double(minutes) / 60.0
    }

    private static func elementBands(for sign: ZodiacSign) -> [Band] {
        switch sign {
        case .aries, .leo, .sagittarius:
            return fire
        case .taurus, .virgo, .capricorn:
            return earth
        case .gemini, .libra, .aquarius:
            return air
        case .cancer, .scorpio, .pisces:
            return water
        }
    }

    // MARK: - Public API

    /// Returns the band label if the degree falls within a Pushkara band, else nil
    static func bandIfPushkara(sign: ZodiacSign, degreeWithinSign: Double) -> String? {
        let bands = elementBands(for: sign)
        let d = degreeWithinSign.clamped(to: 0.0...30.0)

        return bands.first { band in
            d >= band.startDeg - 1e-9 && d <= band.endDeg + 1e-9
        }?.label
    }

    /// Returns true if the degree falls within a Pushkara band
    static func isPushkara(sign: ZodiacSign, degreeWithinSign: Double) -> Bool {
        return bandIfPushkara(sign: sign, degreeWithinSign: degreeWithinSign) != nil
    }
}

// MARK: - Extensions

extension Double {
    func clamped(to range: ClosedRange<Double>) -> Double {
        return min(max(self, range.lowerBound), range.upperBound)
    }
}

extension Int {
    func clamped(to range: ClosedRange<Int>) -> Int {
        return min(max(self, range.lowerBound), range.upperBound)
    }
}
```

### Step 3: Create the View Model

```swift
// MARK: - Pushkara Item for Display

struct PushkaraItem: Identifiable {
    let id = UUID()
    let name: String
    let sign: ZodiacSign
    let degreeWithinSign: Double
    let isPushkara: Bool
    let bandLabel: String?

    var positionText: String {
        let deg = Int(degreeWithinSign)
        let minFloat = (degreeWithinSign - Double(deg)) * 60.0
        let min = Int(minFloat)
        let sec = Int((minFloat - Double(min)) * 60.0)
        return String(format: "%@ %02d° %02d' %02d\"",
                     sign.displayName, deg, min, sec)
    }
}

// MARK: - View Model

class PushkaraNavamshaViewModel: ObservableObject {
    @Published var items: [PushkaraItem] = []
    @Published var title: String = ""
    @Published var subtitle: String = ""

    func loadChart(chart: ChartResult, name: String?) {
        var result: [PushkaraItem] = []

        // Add Ascendant
        let ascDegInSign = chart.ascendantDegree.truncatingRemainder(dividingBy: 30.0)
        let ascBand = PushkaraNavamsha.bandIfPushkara(
            sign: chart.ascendantSign,
            degreeWithinSign: ascDegInSign
        )
        result.append(PushkaraItem(
            name: "Ascendant (Lagna)",
            sign: chart.ascendantSign,
            degreeWithinSign: ascDegInSign,
            isPushkara: ascBand != nil,
            bandLabel: ascBand
        ))

        // Add all planets
        for planet in chart.planets {
            let degInSign = planet.degree.truncatingRemainder(dividingBy: 30.0)
            let band = PushkaraNavamsha.bandIfPushkara(
                sign: planet.sign,
                degreeWithinSign: degInSign
            )
            result.append(PushkaraItem(
                name: planet.name,
                sign: planet.sign,
                degreeWithinSign: degInSign,
                isPushkara: band != nil,
                bandLabel: band
            ))
        }

        self.items = result
        self.title = "Pushkara Navamsha"
        self.subtitle = name != nil ? "Chart for \(name!)" : ""
    }
}
```

### Step 4: Create SwiftUI View

```swift
import SwiftUI

struct PushkaraNavamshaView: View {
    @StateObject private var viewModel = PushkaraNavamshaViewModel()
    let chart: ChartResult
    let name: String?

    var body: some View {
        NavigationView {
            List(viewModel.items) { item in
                PushkaraItemRow(item: item)
            }
            .navigationTitle(viewModel.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .principal) {
                    VStack {
                        Text(viewModel.title)
                            .font(.headline)
                        if !viewModel.subtitle.isEmpty {
                            Text(viewModel.subtitle)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
        }
        .onAppear {
            viewModel.loadChart(chart: chart, name: name)
        }
    }
}

struct PushkaraItemRow: View {
    let item: PushkaraItem

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(item.name)
                    .font(.headline)
                Spacer()
                if item.isPushkara {
                    Text("*")
                        .font(.system(size: 16))
                        .foregroundColor(.orange)
                }
            }

            Text(item.positionText)
                .font(.subheadline)
                .foregroundColor(.secondary)

            HStack {
                Text("Pushkara:")
                    .font(.subheadline)
                Text(item.isPushkara ? "Yes" : "No")
                    .font(.subheadline)
                    .foregroundColor(item.isPushkara ? .green : .red)
            }

            if let bandLabel = item.bandLabel {
                Text(bandLabel)
                    .font(.caption)
                    .foregroundColor(.blue)
            }
        }
        .padding(.vertical, 4)
    }
}
```

### Step 5: UIKit Alternative (If Not Using SwiftUI)

```swift
import UIKit

class PushkaraNavamshaViewController: UIViewController {
    private var tableView: UITableView!
    private var items: [PushkaraItem] = []
    private let chart: ChartResult
    private let personName: String?

    init(chart: ChartResult, name: String?) {
        self.chart = chart
        self.personName = name
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        loadData()
    }

    private func setupUI() {
        title = "Pushkara Navamsha"
        view.backgroundColor = .systemBackground

        tableView = UITableView(frame: view.bounds, style: .insetGrouped)
        tableView.delegate = self
        tableView.dataSource = self
        tableView.register(PushkaraCell.self, forCellReuseIdentifier: "PushkaraCell")
        tableView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(tableView)

        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: view.topAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
    }

    private func loadData() {
        var result: [PushkaraItem] = []

        // Ascendant
        let ascDegInSign = chart.ascendantDegree.truncatingRemainder(dividingBy: 30.0)
        let ascBand = PushkaraNavamsha.bandIfPushkara(
            sign: chart.ascendantSign,
            degreeWithinSign: ascDegInSign
        )
        result.append(PushkaraItem(
            name: "Ascendant (Lagna)",
            sign: chart.ascendantSign,
            degreeWithinSign: ascDegInSign,
            isPushkara: ascBand != nil,
            bandLabel: ascBand
        ))

        // Planets
        for planet in chart.planets {
            let degInSign = planet.degree.truncatingRemainder(dividingBy: 30.0)
            let band = PushkaraNavamsha.bandIfPushkara(
                sign: planet.sign,
                degreeWithinSign: degInSign
            )
            result.append(PushkaraItem(
                name: planet.name,
                sign: planet.sign,
                degreeWithinSign: degInSign,
                isPushkara: band != nil,
                bandLabel: band
            ))
        }

        items = result
        tableView.reloadData()
    }
}

extension PushkaraNavamshaViewController: UITableViewDelegate, UITableViewDataSource {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return items.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "PushkaraCell", for: indexPath) as! PushkaraCell
        cell.configure(with: items[indexPath.row])
        return cell
    }
}

class PushkaraCell: UITableViewCell {
    private let nameLabel = UILabel()
    private let positionLabel = UILabel()
    private let statusLabel = UILabel()
    private let bandLabel = UILabel()
    private let indicator = UILabel()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupViews()
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func setupViews() {
        let stackView = UIStackView()
        stackView.axis = .vertical
        stackView.spacing = 4
        stackView.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(stackView)

        let headerStack = UIStackView()
        headerStack.axis = .horizontal

        nameLabel.font = .boldSystemFont(ofSize: 16)
        indicator.text = "*"
        indicator.font = .systemFont(ofSize: 16)
        indicator.textColor = .systemOrange

        headerStack.addArrangedSubview(nameLabel)
        headerStack.addArrangedSubview(UIView()) // Spacer
        headerStack.addArrangedSubview(indicator)

        positionLabel.font = .systemFont(ofSize: 14)
        positionLabel.textColor = .secondaryLabel

        statusLabel.font = .systemFont(ofSize: 14)

        bandLabel.font = .systemFont(ofSize: 12)
        bandLabel.textColor = .systemBlue
        bandLabel.numberOfLines = 0

        stackView.addArrangedSubview(headerStack)
        stackView.addArrangedSubview(positionLabel)
        stackView.addArrangedSubview(statusLabel)
        stackView.addArrangedSubview(bandLabel)

        NSLayoutConstraint.activate([
            stackView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 8),
            stackView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            stackView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            stackView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -8)
        ])
    }

    func configure(with item: PushkaraItem) {
        nameLabel.text = item.name
        positionLabel.text = item.positionText
        statusLabel.text = "Pushkara: \(item.isPushkara ? "Yes" : "No")"
        statusLabel.textColor = item.isPushkara ? .systemGreen : .systemRed
        bandLabel.text = item.bandLabel ?? ""
        bandLabel.isHidden = item.bandLabel == nil
        indicator.isHidden = !item.isPushkara
    }
}
```

---

## Key Implementation Points

### 1. Degree Calculation
Always extract the degree within sign (0-30°) using:
```swift
let degreeWithinSign = degree.truncatingRemainder(dividingBy: 30.0)
```

### 2. Precision Handling
Use a small epsilon (1e-9) for floating-point comparison to handle rounding errors:
```swift
d >= band.startDeg - 1e-9 && d <= band.endDeg + 1e-9
```

### 3. Element Mapping
Map zodiac signs to their elements:
- **Fire**: Aries, Leo, Sagittarius
- **Earth**: Taurus, Virgo, Capricorn
- **Air**: Gemini, Libra, Aquarius
- **Water**: Cancer, Scorpio, Pisces

### 4. Display Format
Format degrees in the traditional notation:
```swift
String.format("%02d° %02d' %02d\"", degrees, minutes, seconds)
```

### 5. Band Detection Logic
1. Get the zodiac sign of the planet/ascendant
2. Calculate degree within that sign (0-30°)
3. Retrieve the element bands for that sign
4. Check if degree falls within any band
5. Return the band label or nil

---

## Testing

Test your implementation with these known cases:

| Position | Expected Result |
|----------|----------------|
| Aries 21°30' | **Pushkara** (Fire: 20°00'–23°20') |
| Leo 27°00' | **Pushkara** (Fire: 26°40'–30°00') |
| Taurus 8°00' | **Pushkara** (Earth: 6°40'–10°00') |
| Virgo 15°00' | **Pushkara** (Earth: 13°20'–16°40') |
| Gemini 18°00' | **Pushkara** (Air: 16°40'–20°00') |
| Libra 25°00' | **Pushkara** (Air: 23°20'–26°40') |
| Cancer 2°00' | **Pushkara** (Water: 0°00'–3°20') |
| Pisces 8°00' | **Pushkara** (Water: 6°40'–10°00') |
| Aries 15°00' | **Not Pushkara** |
| Taurus 20°00' | **Not Pushkara** |

---

## Integration with Existing Astrology Calculator

Your iOS app will need:

1. **Ephemeris calculations** - Use Swiss Ephemeris library (swe) or similar
2. **Birth chart generation** - Calculate planetary positions
3. **Zodiac sign mapping** - Convert absolute degrees to sign + degree within sign
4. **Pushkara detection** - Apply the logic from this guide

### Example Integration

```swift
// After generating the chart
let chart = astrologyCalculator.generateChart(birthDetails: details)

// Navigate to Pushkara view
let pushkaraView = PushkaraNavamshaView(chart: chart, name: details.name)
navigationController.pushViewController(
    UIHostingController(rootView: pushkaraView),
    animated: true
)
```

---

## File Organization Recommendation

```
YourApp/
├── Models/
│   ├── ZodiacSign.swift
│   ├── Planet.swift
│   ├── PlanetPosition.swift
│   └── ChartResult.swift
├── Calculators/
│   ├── AstrologyCalculator.swift
│   └── PushkaraNavamsha.swift
├── ViewModels/
│   └── PushkaraNavamshaViewModel.swift
└── Views/
    ├── PushkaraNavamshaView.swift (SwiftUI)
    └── PushkaraNavamshaViewController.swift (UIKit)
```

---

## Additional Notes

- The Android app uses the Lahiri ayanamsa for sidereal calculations
- All degrees are in sidereal zodiac (not tropical)
- The algorithm is deterministic and doesn't require external API calls
- Precision is maintained using Double/Float64 types
- The logic can be unit tested independently of UI

---

## References

- Android Implementation: `app/src/main/java/com/aakash/astro/astrology/PushkaraNavamsha.kt`
- Android Activity: `app/src/main/java/com/aakash/astro/PushkaraNavamshaActivity.kt`
- Layout: `app/src/main/res/layout/activity_pushkara_navamsha.xml`

---

**Last Updated**: 2025-10-21
**Version**: 1.0
