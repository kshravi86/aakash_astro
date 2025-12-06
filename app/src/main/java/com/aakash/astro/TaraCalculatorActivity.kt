package com.aakash.astro

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.aakash.astro.astrology.TaraBalaCalc
import com.aakash.astro.databinding.ActivityTaraCalculatorBinding
import com.aakash.astro.databinding.ItemTaraRowBinding

class TaraCalculatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaraCalculatorBinding

    private val nakshatraNames = listOf(
        "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra", "Punarvasu",
        "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni", "Hasta",
        "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha", "Mula", "Purva Ashadha",
        "Uttara Ashadha", "Shravana", "Dhanishtha", "Shatabhisha", "Purva Bhadrapada",
        "Uttara Bhadrapada", "Revati"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaraCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topBar.setNavigationOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

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

        setupNakshatraSpinner()
        binding.calculateButton.setOnClickListener {
            calculateTaraBala()
        }
    }

    private fun setupNakshatraSpinner() {
        val displayNames = nakshatraNames.mapIndexed { index, name ->
            "${index + 1}. $name"
        }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            displayNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.moonNakshatraSpinner.adapter = adapter
        binding.moonNakshatraSpinner.setSelection(16)
    }

    private fun calculateTaraBala() {
        val selectedPosition = binding.moonNakshatraSpinner.selectedItemPosition
        val moonNakshatraIndex = selectedPosition + 1

        // Show loading feedback
        binding.calculateButton.isEnabled = false
        binding.calculateButton.text = "Calculating..."
        Toast.makeText(this, "Generating Tara Bala chart...", Toast.LENGTH_SHORT).show()

        // Simulate brief calculation time for better UX
        Handler(Looper.getMainLooper()).postDelayed({
            binding.selectedMoonNakshatra.text = "Natal Moon: ${nakshatraNames[selectedPosition]}"
            binding.resultCard.visibility = View.VISIBLE

            val results = TaraBalaCalc.computeForMoonNakshatra(moonNakshatraIndex)

            val inflater = LayoutInflater.from(this)
            binding.rowContainer.removeAllViews()

            results.forEachIndexed { index, entry ->
                val item = ItemTaraRowBinding.inflate(inflater, binding.rowContainer, false)
                item.planetName.text = entry.nakshatraName
                item.taraValue.text = entry.tara
                item.resultValue.text = entry.result

                val resultColor = when (entry.taraNumber) {
                    2, 4, 6, 8, 9 -> getColor(R.color.planet_favorable)
                    3, 5, 7 -> getColor(R.color.planet_unfavorable)
                    else -> getColor(R.color.planet_neutral)
                }
                item.resultValue.setTextColor(resultColor)
                item.taraValue.setTextColor(resultColor)

                binding.rowContainer.addView(item.root)
            }

            // Reset button state
            binding.calculateButton.isEnabled = true
            binding.calculateButton.text = "Calculate Tara Bala"

            // Success feedback and guided focus to results
            Toast.makeText(this@TaraCalculatorActivity, "Chart generated successfully!", Toast.LENGTH_SHORT).show()
            Snackbar.make(binding.root, "Tara Bala chart generated", Snackbar.LENGTH_LONG)
                .setAction("View") { scrollAndHighlightResults() }
                .show()

            // Auto navigate and highlight
            scrollAndHighlightResults()

        }, 500) // 500ms delay for better UX
    }

    private fun scrollAndHighlightResults() {
        // Ensure results are visible
        binding.resultCard.visibility = View.VISIBLE

        // Smooth scroll to the result card position
        binding.scrollView.post {
            val y = binding.resultCard.top - dp(12)
            binding.scrollView.smoothScrollTo(0, if (y > 0) y else 0)
        }

        // Briefly accent the result card stroke and fade-in pulse
        val originalStroke = resources.getColor(R.color.divider, theme)
        val accent = resources.getColor(R.color.accent_teal, theme)
        if (binding.resultCard is com.google.android.material.card.MaterialCardView) {
            val card = binding.resultCard as com.google.android.material.card.MaterialCardView
            card.strokeWidth = dp(2)
            card.strokeColor = accent

            // Fade pulse
            ObjectAnimator.ofFloat(card, "alpha", 0.6f, 1.0f).apply {
                duration = 800
                start()
            }

            // Revert stroke after delay
            Handler(Looper.getMainLooper()).postDelayed({
                card.strokeColor = originalStroke
                card.strokeWidth = dp(1)
            }, 1600)
        }
    }

    private fun dp(value: Int): Int {
        val density = resources.displayMetrics.density
        return (value * density).toInt()
    }
}
