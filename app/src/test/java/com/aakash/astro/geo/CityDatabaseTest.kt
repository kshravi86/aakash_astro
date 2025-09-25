package com.aakash.astro.geo

import org.junit.Assert.*
import org.junit.Test

class CityDatabaseTest {

    @Test
    fun names_notEmpty_and_containsMajorCities() {
        val names = CityDatabase.names()
        assertTrue(names.isNotEmpty())
        assertTrue(names.any { it.equals("Mumbai", ignoreCase = true) })
        assertTrue(names.any { it.equals("Delhi", ignoreCase = true) })
        assertTrue(names.any { it.equals("Bengaluru", ignoreCase = true) || it.equals("Bangalore", ignoreCase = true) })
    }

    @Test
    fun findByName_caseInsensitive_returnsCoordinates() {
        val city = CityDatabase.findByName("mumbai")
        assertNotNull(city)
        // Rough bounds for sanity
        assertTrue(city!!.latitude in 18.0..20.0)
        assertTrue(city.longitude in 72.0..74.0)
    }

    @Test
    fun bengaluru_coordinates_sanity() {
        val city = CityDatabase.findByName("Bengaluru") ?: CityDatabase.findByName("Bangalore")
        assertNotNull(city)
        city!!
        // Expected approx: 12.9716 N, 77.5946 E
        assertEquals(12.9716, city.latitude, 0.01)
        assertEquals(77.5946, city.longitude, 0.01)
    }
}
