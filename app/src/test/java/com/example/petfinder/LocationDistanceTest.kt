package com.example.petfinder

import com.example.petfinder.util.LocationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationDistanceTest {

    @Test
    fun calculateDistanceKm_samePointIsZero() {
        val distance = LocationHelper.calculateDistanceKm(-4.9403, -37.9753, -4.9403, -37.9753)
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun calculateDistanceKm_knownCitiesIsReasonable() {
        // Russas-CE para Tabuleiro do Norte-CE (~46 km em linha reta)
        val distance = LocationHelper.calculateDistanceKm(-4.9403, -37.9753, -5.2528, -38.1286)
        assertTrue("Expected ~38-45 km, got $distance", distance in 35.0..45.0)
    }
}
