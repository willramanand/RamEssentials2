package dev.willram.ramEssentials2.config

import dev.willram.ramcore.configurate.objectmapping.ConfigSerializable
import org.bukkit.Bukkit

@ConfigSerializable
class EssentialsConfig {
    var maxHomes = 3
    var percentLostOnDeath = 10
    var commandsPerPage = 10
    var teleportDelay = 5
    var serverSpawn = Bukkit.getWorlds()[0].spawnLocation
    var chatMessageFormat = "<dark_gray>[</dark_gray><white><playername></white><dark_gray>]</dark_gray>"

}