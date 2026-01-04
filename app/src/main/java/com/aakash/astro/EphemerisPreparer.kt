package com.aakash.astro

import android.content.Context
import com.aakash.astro.util.AppLog
import java.io.File

object EphemerisPreparer {
    fun prepare(context: Context): File? {
        return try {
            val am = context.assets
            val outDir = File(context.filesDir, "ephe").apply { mkdirs() }
            val entries = am.list("ephe")
            if (entries.isNullOrEmpty()) {
                AppLog.d("No ephemeris assets found under assets/ephe.")
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
            AppLog.d("Ephemeris assets prepared count=$copied.")
            outDir
        } catch (t: Throwable) {
            AppLog.w("Ephemeris asset preparation failed.", t)
            null
        }
    }
}
