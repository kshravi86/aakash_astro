package com.aakash.astro.storage

import android.content.Context
import com.aakash.astro.util.AppLog
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SavedHoroscope(
    val id: String,
    val name: String,
    val epochMillis: Long,
    val zoneId: String,
    val lat: Double,
    val lon: Double
)

object SavedStore {
    private const val DIR_NAME = "horoscopes"

    private fun dir(ctx: Context): File = File(ctx.filesDir, DIR_NAME).apply { mkdirs() }

    fun sanitizeName(raw: String): String = raw.replace(Regex("[^A-Za-z0-9_ -]"), "_").trim().ifEmpty { "Unnamed" }

    fun formatDate(millis: Long): String = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date(millis))

    fun save(
        ctx: Context,
        name: String?,
        epochMillis: Long,
        zoneId: String,
        lat: Double,
        lon: Double
    ): SavedHoroscope {
        // Deduplicate: if any record has the same exact epochMillis (same date+time),
        // remove those and keep only this latest one.
        runCatching {
            list(ctx).filter { it.epochMillis == epochMillis }.forEach { old ->
                File(dir(ctx), "${old.id}.json").delete()
            }
        }

        val safeName = sanitizeName(name ?: "Unnamed")
        val datePart = formatDate(epochMillis)
        // Use only the date-time as the stable ID so saving the same
        // date+time always targets the same filename and truly overrides.
        // We still store the (possibly updated) name inside the JSON.
        val id = datePart
        val obj = JSONObject()
            .put("id", id)
            .put("name", safeName)
            .put("epochMillis", epochMillis)
            .put("zoneId", zoneId)
            .put("lat", lat)
            .put("lon", lon)
        val file = File(dir(ctx), "$id.json")
        file.writeText(obj.toString())
        AppLog.d("Saved chart file=${file.name}")
        return SavedHoroscope(id, safeName, epochMillis, zoneId, lat, lon)
    }

    fun list(ctx: Context): List<SavedHoroscope> {
        val d = dir(ctx)
        val files = d.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: emptyArray()
        return files.mapNotNull { f ->
            try {
                val t = f.readText()
                val o = JSONObject(t)
                SavedHoroscope(
                    id = o.optString("id", f.nameWithoutExtension),
                    name = o.optString("name", f.nameWithoutExtension),
                    epochMillis = o.optLong("epochMillis", 0L),
                    zoneId = o.optString("zoneId", java.time.ZoneId.systemDefault().id),
                    lat = o.optDouble("lat", 0.0),
                    lon = o.optDouble("lon", 0.0)
                )
            } catch (t: Throwable) {
                AppLog.w("Failed to parse saved chart file=${f.name}", t)
                null
            }
        }.sortedByDescending { it.epochMillis }
    }

    fun load(ctx: Context, id: String): SavedHoroscope? {
        val file = File(dir(ctx), "$id.json")
        if (!file.exists()) {
            AppLog.d("Saved chart not found id=$id")
            return null
        }
        return try {
            val o = JSONObject(file.readText())
            SavedHoroscope(
                id = o.getString("id"),
                name = o.getString("name"),
                epochMillis = o.getLong("epochMillis"),
                zoneId = o.getString("zoneId"),
                lat = o.getDouble("lat"),
                lon = o.getDouble("lon")
            )
        } catch (t: Throwable) {
            AppLog.w("Failed to load saved chart id=$id", t)
            null
        }
    }

    fun delete(ctx: Context, id: String): Boolean {
        val file = File(dir(ctx), "$id.json")
        val deleted = file.delete()
        AppLog.d("Deleted saved chart id=$id success=$deleted")
        return deleted
    }
}
