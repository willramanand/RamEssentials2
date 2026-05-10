package dev.willram.ramEssentials2.data

import dev.willram.ramcore.data.DataItem
import org.bukkit.Location
import java.util.*

class PlayerData : DataItem() {
    var godMode = false
    var doNotDisturb = false
    var muted = false
    var muteReason: String? = null
    var lastLocation: Location? = null
    var backStack: MutableList<Location> = mutableListOf()
    val homes: MutableMap<String, Location> = HashMap()
    var ignoredPlayers: List<UUID> = mutableListOf()
    var lastReceivedPlayer: UUID? = null
    var nickname: String? = null

    fun pushBackLocation(location: Location, maxSize: Int) {
        backStack.add(location.clone())
        while (backStack.size > maxSize) {
            backStack.removeAt(0)
        }
        lastLocation = location.clone()
        markDirty()
    }

    fun popBackLocation(): Location? {
        val location = backStack.removeLastOrNull() ?: lastLocation ?: return null
        lastLocation = backStack.lastOrNull()
        markDirty()
        return location
    }
}
