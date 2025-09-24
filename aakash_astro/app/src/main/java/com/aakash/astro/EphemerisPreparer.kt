package com.aakash.astro

import android.content.Context
import java.io.File

object EphemerisPreparer {
    fun prepare(context: Context): File? {
        return try {
            val am = context.assets
            val outDir = File(context.filesDir, "ephe").apply { mkdirs() }
            val entries = am.list("ephe") ?: return null
            for (name in entries) {
                val inStream = am.open("ephe/$name")
                val outFile = File(outDir, name)
                inStream.use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            outDir
        } catch (_: Throwable) {
            null
        }
    }
}
