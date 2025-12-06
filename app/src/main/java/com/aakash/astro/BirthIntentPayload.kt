package com.aakash.astro

import android.content.Intent
import com.aakash.astro.astrology.BirthDetails
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Shared contract for passing birth details between screens.
 */
object BirthIntentExtras {
    const val NAME = "name"
    const val EPOCH_MILLIS = "epochMillis"
    const val ZONE_ID = "zoneId"
    const val LAT = "lat"
    const val LON = "lon"
}

data class BirthIntentPayload(
    val name: String?,
    val epochMillis: Long?,
    val zoneId: ZoneId,
    val latitude: Double?,
    val longitude: Double?
) {
    val zonedDateTime: ZonedDateTime?
        get() = epochMillis?.let { Instant.ofEpochMilli(it).atZone(zoneId) }

    fun toBirthDetailsOrNull(): BirthDetails? {
        val dateTime = zonedDateTime ?: return null
        val lat = latitude ?: return null
        val lon = longitude ?: return null
        return BirthDetails(name, dateTime, lat, lon)
    }
}

fun Intent.putBirthPayload(payload: BirthIntentPayload): Intent = apply {
    putExtra(BirthIntentExtras.NAME, payload.name)
    payload.epochMillis?.let { putExtra(BirthIntentExtras.EPOCH_MILLIS, it) }
    putExtra(BirthIntentExtras.ZONE_ID, payload.zoneId.id)
    payload.latitude?.let { putExtra(BirthIntentExtras.LAT, it) }
    payload.longitude?.let { putExtra(BirthIntentExtras.LON, it) }
}

fun Intent.readBirthPayload(
    epochSentinel: Long = -1L,
    coordinateSentinel: Double = Double.NaN,
    defaultZone: ZoneId = ZoneId.systemDefault()
): BirthIntentPayload {
    val epoch = getLongExtra(BirthIntentExtras.EPOCH_MILLIS, epochSentinel)
        .takeUnless { it == epochSentinel }
    val zone = getStringExtra(BirthIntentExtras.ZONE_ID)?.let(ZoneId::of) ?: defaultZone
    val lat = getDoubleExtra(BirthIntentExtras.LAT, coordinateSentinel)
        .takeUnless { it.isSentinel(coordinateSentinel) }
    val lon = getDoubleExtra(BirthIntentExtras.LON, coordinateSentinel)
        .takeUnless { it.isSentinel(coordinateSentinel) }
    val name = getStringExtra(BirthIntentExtras.NAME)
    return BirthIntentPayload(name, epoch, zone, lat, lon)
}

private fun Double.isSentinel(sentinel: Double): Boolean {
    return if (sentinel.isNaN()) {
        this.isNaN()
    } else {
        this == sentinel
    }
}
