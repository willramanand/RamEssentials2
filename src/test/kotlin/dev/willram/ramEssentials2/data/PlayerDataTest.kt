package dev.willram.ramEssentials2.data

import org.bukkit.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerDataTest {
    @Test
    fun backStackKeepsNewestLocationsInsideConfiguredLimit() {
        val data = PlayerData()
        data.markClean()

        val first = Location(null, 1.0, 64.0, 1.0)
        val second = Location(null, 2.0, 64.0, 2.0)
        val third = Location(null, 3.0, 64.0, 3.0)

        data.pushBackLocation(first, 2)
        data.pushBackLocation(second, 2)
        data.pushBackLocation(third, 2)

        assertEquals(2, data.backStack.size)
        assertEquals(second, data.backStack[0])
        assertEquals(third, data.backStack[1])
        assertTrue(data.dirty())
    }

    @Test
    fun popBackLocationFallsBackToLastLocation() {
        val data = PlayerData()
        val fallback = Location(null, 4.0, 65.0, 4.0)
        data.lastLocation = fallback

        assertEquals(fallback, data.popBackLocation())
        assertNull(data.lastLocation)
    }
}
