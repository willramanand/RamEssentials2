package dev.willram.ramEssentials2.utils

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.promise.Promise
import dev.willram.ramcore.scheduler.Schedulers
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask


class TeleportUtil private constructor() {

    companion object {
        private val teleportingPlayers: MutableMap<Player, Promise<Void>> = HashMap()

        fun teleport(player: Player, newLoc: Location) {
            if (RamEssentials2.get().conf.teleportDelay <= 0) {
                val previousLoc: Location = player.location

                player.teleportAsync(newLoc, PlayerTeleportEvent.TeleportCause.COMMAND)

                val data = RamEssentials2.get().players.get(player.uniqueId)
                data.lastLocation = previousLoc
            } else {
                if (teleportingPlayers[player] != null) {
                    player.sendRichMessage("<red>You are already waiting to teleport!")
                    return
                }

                val seconds: Int = RamEssentials2.get().conf.teleportDelay
                val delay = seconds * 20
                player.sendRichMessage("<yellow>Teleporting in <light_purple>$seconds <yellow>seconds! Teleport will cancel if you move or are damaged!")
                val task = Schedulers.async().runLater({
                    teleportingPlayers.remove(player)
                    val previousLoc: Location = player.location
                    player.teleportAsync(newLoc, PlayerTeleportEvent.TeleportCause.COMMAND)
                    val data = RamEssentials2.get().players.get(player.uniqueId)
                    data.lastLocation = previousLoc
                }, delay.toLong())
                teleportingPlayers[player] = task
            }
        }

        fun hasTPTask(player: Player): Boolean {
            return teleportingPlayers[player] != null
        }

        fun clearTPTask(player: Player) {
            teleportingPlayers[player]!!.cancel()
            teleportingPlayers.remove(player)
        }
    }
}