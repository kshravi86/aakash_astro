package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.databinding.ActivitySavedHoroscopesBinding
import com.aakash.astro.storage.SavedStore
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SavedHoroscopesActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySavedHoroscopesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedHoroscopesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.topBar.setNavigationOnClickListener { finish() }

        val list = SavedStore.list(this)
        if (list.isEmpty()) {
            binding.emptyView.text = getString(R.string.saved_empty)
            binding.emptyView.visibility = android.view.View.VISIBLE
            return
        }
        val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
        val inflater = LayoutInflater.from(this)
        list.forEach { s ->
            val item = inflater.inflate(R.layout.item_saved_horoscope, binding.listContainer, false)
            val title = item.findViewById<TextView>(R.id.title)
            val subtitle = item.findViewById<TextView>(R.id.subtitle)
            val loadBtn = item.findViewById<com.google.android.material.button.MaterialButton>(R.id.loadButton)
            val z = ZoneId.of(s.zoneId)
            val zdt = java.time.Instant.ofEpochMilli(s.epochMillis).atZone(z)
            title.text = s.name
            subtitle.text = "${fmt.format(zdt)}  •  ${String.format("%.4f", s.lat)}, ${String.format("%.4f", s.lon)}"
            loadBtn.setOnClickListener {
                // Default action: open Main and from there user can navigate to desired pages
                val intent = android.content.Intent(this, MainActivity::class.java).apply {
                    putExtra("prefill_name", s.name)
                    putExtra("prefill_epochMillis", s.epochMillis)
                    putExtra("prefill_zoneId", s.zoneId)
                    putExtra("prefill_lat", s.lat)
                    putExtra("prefill_lon", s.lon)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(intent)
            }
            binding.listContainer.addView(item)
        }
    }
}

