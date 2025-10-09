package com.aakash.astro

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.AccurateCalculator
import com.aakash.astro.astrology.BirthDetails
import com.aakash.astro.astrology.SixtyFourTwentyTwoCalc
import com.aakash.astro.databinding.ActivitySixtyFourTwentyTwoBinding
import java.time.Instant
import java.time.ZoneId

class SixtyFourTwentyTwoActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySixtyFourTwentyTwoBinding
    private val accurate = AccurateCalculator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySixtyFourTwentyTwoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.topBar.setNavigationOnClickListener { finish() }

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        val name = intent.getStringExtra(EXTRA_NAME)
        val epochMillis = intent.getLongExtra(EXTRA_EPOCH_MILLIS, 0L)
        val zoneId = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lon = intent.getDoubleExtra(EXTRA_LON, 0.0)

        val zdt = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val chart = accurate.generateChart(BirthDetails(name, zdt, lat, lon))
        if (chart == null) {
            binding.subtitle.text = getString(R.string.transit_engine_missing)
            return
        }

        binding.title.text = getString(R.string.sixtyfour_twenty_two_title)
        binding.subtitle.text = getString(R.string.sixtyfour_twenty_two_subtitle)

        val res = SixtyFourTwentyTwoCalc.compute(chart)

        // From Lagna
        binding.lagnaDrekkanaSign.text = res.fromLagnaDrekkanaSign.displayName + " (Drekkana #${res.fromLagnaDrekkanaNo})"
        binding.lagnaDrekkanaLord.text = res.fromLagnaDrekkanaLord.displayName
        binding.lagnaNavamsaSign.text = res.fromLagnaNavamsaSign.displayName + " (Navamsha #${res.fromLagnaNavamsaNo})"
        binding.lagnaNavamsaLord.text = res.fromLagnaNavamsaLord.displayName

        // From Moon
        binding.moonDrekkanaSign.text = res.fromMoonDrekkanaSign.displayName + " (Drekkana #${res.fromMoonDrekkanaNo})"
        binding.moonDrekkanaLord.text = res.fromMoonDrekkanaLord.displayName
        binding.moonNavamsaSign.text = res.fromMoonNavamsaSign.displayName + " (Navamsha #${res.fromMoonNavamsaNo})"
        binding.moonNavamsaLord.text = res.fromMoonNavamsaLord.displayName
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}

