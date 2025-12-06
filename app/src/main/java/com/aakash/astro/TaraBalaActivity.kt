package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aakash.astro.astrology.AccurateCalculator
import com.aakash.astro.astrology.BirthDetails
import com.aakash.astro.astrology.NakshatraCalc
import com.aakash.astro.astrology.TaraBalaCalc
import com.aakash.astro.databinding.ItemTaraRowBinding
import com.aakash.astro.databinding.ItemTransitTaraBinding
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.time.Instant
import java.time.ZoneId

class TaraBalaActivity : AppCompatActivity() {
    private val accurate = AccurateCalculator()
    private var lastNatalChart: com.aakash.astro.astrology.ChartResult? = null
    private var lastTransitChart: com.aakash.astro.astrology.ChartResult? = null
    private lateinit var transitAdapter: TransitTaraAdapter
    private lateinit var natalAdapter: NatalTaraAdapter
    private var allTransitRows: List<TransitRow> = emptyList()
    private var currentFilter: TaraResultFilter = TaraResultFilter.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tara_bala)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.topBar)
            .setNavigationOnClickListener { finish() }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val root = findViewById<View>(android.R.id.content)
        val startPaddingLeft = root.paddingLeft
        val startPaddingTop = root.paddingTop
        val startPaddingRight = root.paddingRight
        val startPaddingBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                startPaddingLeft + systemBars.left,
                startPaddingTop + systemBars.top,
                startPaddingRight + systemBars.right,
                startPaddingBottom + systemBars.bottom
            )
            insets
        }

        transitAdapter = TransitTaraAdapter()
        natalAdapter = NatalTaraAdapter()

        findViewById<RecyclerView>(R.id.transitList).apply {
            layoutManager = LinearLayoutManager(this@TaraBalaActivity)
            adapter = transitAdapter
        }
        findViewById<RecyclerView>(R.id.natalList).apply {
            layoutManager = LinearLayoutManager(this@TaraBalaActivity)
            adapter = natalAdapter
        }

        val filterChips = findViewById<ChipGroup>(R.id.filterChips)
        findViewById<Chip>(R.id.chipFilterAll).isChecked = true
        filterChips.setOnCheckedChangeListener { _, checkedId ->
            val filter = when (checkedId) {
                R.id.chipFilterFav -> TaraResultFilter.FAVORABLE
                R.id.chipFilterNeutral -> TaraResultFilter.NEUTRAL
                R.id.chipFilterUnf -> TaraResultFilter.UNFAVORABLE
                else -> TaraResultFilter.ALL
            }
            applyTransitFilter(filter)
        }

        findViewById<BottomAppBar>(R.id.bottomShareBar).setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_share_transit -> {
                    shareTransitTara()
                    true
                }
                R.id.action_share_natal -> {
                    shareNatalTara()
                    true
                }
                else -> false
            }
        }

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        val name = intent.getStringExtra(EXTRA_NAME)
        val epochMillis = intent.getLongExtra(EXTRA_EPOCH_MILLIS, 0L)
        val zoneId = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lon = intent.getDoubleExtra(EXTRA_LON, 0.0)

        val birthZdt = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val natalChart = accurate.generateChart(BirthDetails(name, birthZdt, lat, lon))
        if (natalChart == null) {
            findViewById<TextView>(R.id.engineNote).text = getString(R.string.transit_engine_missing)
            updateSummary(emptyList())
            return
        }

        val nowZdt = Instant.now().atZone(zoneId)
        val transitChart = accurate.generateChart(BirthDetails(name, nowZdt, lat, lon))
        lastNatalChart = natalChart
        lastTransitChart = transitChart

        val natalRows = TaraBalaCalc.compute(natalChart).map { row ->
            val result = when (row.result) {
                "Favorable" -> TaraResult.FAVORABLE
                "Unfavorable" -> TaraResult.UNFAVORABLE
                else -> TaraResult.NEUTRAL
            }
            NatalRow(
                planetLabel = row.planet.displayName,
                taraLabel = "${row.tara} (${row.note})",
                resultLabel = row.result,
                result = result
            )
        }
        natalAdapter.submitList(natalRows)
        findViewById<TextView>(R.id.engineNote).text = getString(R.string.tara_engine_note)

        if (transitChart != null) {
            renderTransitTaraBala(transitChart, natalChart)
        } else {
            updateSummary(emptyList())
            applyTransitFilter(TaraResultFilter.ALL)
        }
    }

    private fun renderTransitTaraBala(
        transitChart: com.aakash.astro.astrology.ChartResult,
        natalChart: com.aakash.astro.astrology.ChartResult
    ) {
        val natalMoonDeg = natalChart.planets.find { it.planet == com.aakash.astro.astrology.Planet.MOON }?.degree ?: 0.0
        val natalMoonNak = TaraBalaCalc.nakshatraNumber1Based(natalMoonDeg)

        val rows = transitChart.planets.map { transitPlanet ->
            val (nakName, pada) = NakshatraCalc.fromLongitude(transitPlanet.degree)
            val transitNak = TaraBalaCalc.nakshatraNumber1Based(transitPlanet.degree)
            val taraClass = TaraBalaCalc.tClass(natalMoonNak, transitNak)
            val taraName = TaraBalaCalc.taraNames[taraClass - 1]
            val taraNote = TaraBalaCalc.taraNotes[taraClass] ?: ""
            val result = when (taraClass) {
                2, 4, 6, 8, 9 -> TaraResult.FAVORABLE
                3, 5, 7 -> TaraResult.UNFAVORABLE
                else -> TaraResult.NEUTRAL
            }
            val label = when (result) {
                TaraResult.FAVORABLE -> getString(R.string.tara_filter_fav)
                TaraResult.UNFAVORABLE -> getString(R.string.tara_filter_unf)
                TaraResult.NEUTRAL -> getString(R.string.tara_filter_neutral)
            }
            TransitRow(
                planetLabel = if (transitPlanet.isRetrograde) "${transitPlanet.name} (R)" else transitPlanet.name,
                nakshatraLabel = "$nakName (Pada $pada)",
                taraLabel = if (taraNote.isBlank()) taraName else "$taraName ($taraNote)",
                resultLabel = label,
                result = result
            )
        }

        allTransitRows = rows
        updateSummary(rows)
        applyTransitFilter(currentFilter)
    }

    private fun updateSummary(rows: List<TransitRow>) {
        val fav = rows.count { it.result == TaraResult.FAVORABLE }
        val neutral = rows.count { it.result == TaraResult.NEUTRAL }
        val unf = rows.count { it.result == TaraResult.UNFAVORABLE }
        findViewById<TextView>(R.id.countFav).text = fav.toString()
        findViewById<TextView>(R.id.countNeutral).text = neutral.toString()
        findViewById<TextView>(R.id.countUnf).text = unf.toString()
        findViewById<TextView>(R.id.summarySubtitle).text = getString(R.string.tara_summary_subtitle)
    }

    private fun applyTransitFilter(filter: TaraResultFilter) {
        currentFilter = filter
        val filtered = when (filter) {
            TaraResultFilter.ALL -> allTransitRows
            TaraResultFilter.FAVORABLE -> allTransitRows.filter { it.result == TaraResult.FAVORABLE }
            TaraResultFilter.NEUTRAL -> allTransitRows.filter { it.result == TaraResult.NEUTRAL }
            TaraResultFilter.UNFAVORABLE -> allTransitRows.filter { it.result == TaraResult.UNFAVORABLE }
        }
        transitAdapter.submitList(filtered)
        findViewById<TextView>(R.id.transitEmptyState).isVisible = filtered.isEmpty()
    }

    private fun shareTransitTara() {
        val natal = lastNatalChart ?: return
        val transit = lastTransitChart ?: return
        val sb = StringBuilder()
        sb.appendLine("Transit Tara Bala")
        val natalMoonDeg = natal.planets.find { it.planet == com.aakash.astro.astrology.Planet.MOON }?.degree ?: 0.0
        val natalMoonNak = TaraBalaCalc.nakshatraNumber1Based(natalMoonDeg)
        transit.planets.forEach { tp ->
            val (nakName, pada) = NakshatraCalc.fromLongitude(tp.degree)
            val transitNak = TaraBalaCalc.nakshatraNumber1Based(tp.degree)
            val tClass = TaraBalaCalc.tClass(natalMoonNak, transitNak)
            val tName = TaraBalaCalc.taraNames[tClass - 1]
            val tNote = TaraBalaCalc.taraNotes[tClass] ?: ""
            val result = when (tClass) {
                2, 4, 6, 8, 9 -> "Favorable"
                3, 5, 7 -> "Unfavorable"
                else -> "Neutral"
            }
            val nameWithRetro = if (tp.isRetrograde) "${tp.name} (R)" else tp.name
            sb.appendLine("$nameWithRetro - $nakName (Pada $pada) - $tName ($tNote) - $result")
        }
        shareText(sb.toString())
    }

    private fun shareNatalTara() {
        val natal = lastNatalChart ?: return
        val rows = TaraBalaCalc.compute(natal)
        val sb = StringBuilder()
        sb.appendLine("Natal Tara Bala (ref. Moon)")
        rows.forEach { r ->
            sb.appendLine("${r.planet.displayName} - ${r.tara} (${r.note}) - ${r.result}")
        }
        shareText(sb.toString())
    }

    private fun shareText(text: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(android.content.Intent.EXTRA_TEXT, text)
        startActivity(android.content.Intent.createChooser(intent, getString(R.string.share_result)))
    }

    private data class TransitRow(
        val planetLabel: String,
        val nakshatraLabel: String,
        val taraLabel: String,
        val resultLabel: String,
        val result: TaraResult
    )

    private data class NatalRow(
        val planetLabel: String,
        val taraLabel: String,
        val resultLabel: String,
        val result: TaraResult
    )

    private enum class TaraResult(val colorRes: Int) {
        FAVORABLE(R.color.planet_favorable),
        NEUTRAL(R.color.planet_neutral),
        UNFAVORABLE(R.color.planet_unfavorable)
    }

    private enum class TaraResultFilter {
        ALL, FAVORABLE, NEUTRAL, UNFAVORABLE
    }

    private class TransitTaraAdapter : RecyclerView.Adapter<TransitTaraAdapter.VH>() {
        private var items: List<TransitRow> = emptyList()

        fun submitList(newItems: List<TransitRow>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val inflater = LayoutInflater.from(parent.context)
            return VH(ItemTransitTaraBinding.inflate(inflater, parent, false))
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class VH(private val binding: ItemTransitTaraBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(row: TransitRow) {
                binding.planetName.text = row.planetLabel
                binding.nakshatraInfo.text = row.nakshatraLabel
                binding.taraValue.text = row.taraLabel
                binding.resultValue.text = row.resultLabel
                val color = ContextCompat.getColor(binding.root.context, row.result.colorRes)
                binding.resultValue.setTextColor(color)
                binding.taraValue.setTextColor(color)
            }
        }
    }

    private class NatalTaraAdapter : RecyclerView.Adapter<NatalTaraAdapter.VH>() {
        private var items: List<NatalRow> = emptyList()

        fun submitList(newItems: List<NatalRow>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val inflater = LayoutInflater.from(parent.context)
            return VH(ItemTaraRowBinding.inflate(inflater, parent, false))
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class VH(private val binding: ItemTaraRowBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(row: NatalRow) {
                binding.planetName.text = row.planetLabel
                binding.taraValue.text = row.taraLabel
                binding.resultValue.text = row.resultLabel
                val color = ContextCompat.getColor(binding.root.context, row.result.colorRes)
                binding.resultValue.setTextColor(color)
                binding.taraValue.setTextColor(color)
            }
        }
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}
