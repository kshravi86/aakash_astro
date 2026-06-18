package com.aakash.astro

import android.content.Context
import com.aakash.astro.util.AppLog
import java.io.File

/**
 * Copies Swiss Ephemeris data files from the APK's `assets/ephe/` folder into
 * the app's internal storage on first launch (or whenever the files are missing).
 *
 * The ephemeris files are required by [AccurateCalculator] to compute precise
 * sidereal planetary positions. If preparation fails the app falls back to the
 * built-in trigonometric solver in [AstrologyCalculator].
 */
object EphemerisPreparer {
    /**
     * Copies every file found under `assets/ephe/` to `<filesDir>/ephe/` and
     * returns that directory, or `null` if no assets were found or an error
     * occurred during the copy.
     */
    fun prepare(context: Context): File? {
        return try {
            val am = context.assets
            val outDir = File(context.filesDir, "ephe").apply { mkdirs() }
            val entries = am.list("ephe")
            if (entries.isNullOrEmpty()) {
                AppLog.d("No ephemeris assets found under assets/ephe — Swiss Ephemeris will be skipped.")
                return null
            }
            var copied = 0
            for (name in entries) {
                val inStream = am.open("ephe/$name")
                val outFile = File(outDir, name)
                inStream.use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                copied += 1
            }
            AppLog.d("Ephemeris assets copied to ${outDir.absolutePath} (count=$copied).")
            outDir
        } catch (t: Throwable) {
            AppLog.w("Failed to copy ephemeris assets; Swiss Ephemeris will be unavailable.", t)
            null
        }
    }
}
