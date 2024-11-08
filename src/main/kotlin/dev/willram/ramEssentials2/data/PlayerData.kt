package dev.willram.ramEssentials2.data

import dev.willram.ramcore.configurate.objectmapping.ConfigSerializable
import dev.willram.ramcore.data.DataItem
import org.bukkit.Location
import java.util.*

@ConfigSerializable
class PlayerData : DataItem() {
    var godMode = false
    var doNotDisturb = false
    var muted = false
    var muteReason: String? = null
    //private var muteExpire: Calendar? = null
    var lastLocation: Location? = null
    val homes: MutableMap<String, Location> = HashMap()
    var ignoredPlayers: List<UUID> = mutableListOf()
    var lastReceivedPlayer: UUID? = null
    var nickname: String? = null
}