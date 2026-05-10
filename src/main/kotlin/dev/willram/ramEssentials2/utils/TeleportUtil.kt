package dev.willram.ramEssentials2.utils

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.promise.Promise
import dev.willram.ramcore.scheduler.Schedulers
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent
import java.util.UUID

class TeleportUtil private constructor() {
    companion object {
        private val teleportingPlayers: MutableMap<UUID, Promise<Void>> = HashMap()

        fun teleport(player: Player, newLoc: Location, recordBack: Boolean = true) {
            val plugin = RamEssentials2.get()
            val warmup = if (player.hasPermission("ramessentials.teleport.bypass-warmup")) {
                0
            } else {
                plugin.conf.teleportDelay
            }

            if (warmup <= 0) {
                Schedulers.run(player) {
                    executeTeleport(player, newLoc, recordBack)
                }
                return
            }

            if (teleportingPlayers[player.uniqueId] != null) {
                player.sendRichMessage(plugin.conf.message("already-teleporting"))
                return
            }

            player.sendRichMessage(plugin.conf.message("teleport-warmup", mapOf("seconds" to warmup.toString())))
            val task = Schedulers.runLater(player, {
                teleportingPlayers.remove(player.uniqueId)
                executeTeleport(player, newLoc, recordBack)
            }, warmup * 20L)
            teleportingPlayers[player.uniqueId] = task
        }

        fun hasTPTask(player: Player): Boolean {
            return teleportingPlayers[player.uniqueId] != null
        }

        fun clearTPTask(player: Player, notify: Boolean = true) {
            teleportingPlayers[player.uniqueId]?.cancel()
            teleportingPlayers.remove(player.uniqueId)
            if (notify) {
                player.sendRichMessage(RamEssentials2.get().conf.message("teleport-cancelled"))
            }
        }

        fun pendingCount(): Int {
            return teleportingPlayers.size
        }

        private fun executeTeleport(player: Player, requestedLocation: Location, recordBack: Boolean) {
            val plugin = RamEssentials2.get()
            val target = if (plugin.conf.safeTeleportSearch) {
                findSafeLocation(requestedLocation)
            } else {
                requestedLocation
            }

            if (target == null) {
                player.sendRichMessage(plugin.conf.message("teleport-unsafe"))
                return
            }

            val previousLoc = player.location.clone()
            player.teleportAsync(target, PlayerTeleportEvent.TeleportCause.COMMAND).thenAccept { success ->
                if (success && recordBack) {
                    plugin.players.get(player.uniqueId)?.pushBackLocation(previousLoc, plugin.conf.backStackSize)
                }
            }
        }

        private fun findSafeLocation(location: Location): Location? {
            val plugin = RamEssentials2.get()
            val world = location.world ?: return null
            val chunkX = location.blockX shr 4
            val chunkZ = location.blockZ shr 4

            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                if (!plugin.conf.loadTargetChunk) {
                    return null
                }
                world.loadChunk(chunkX, chunkZ)
            }

            for (radius in 0..3) {
                for (dx in -radius..radius) {
                    for (dz in -radius..radius) {
                        val x = location.blockX + dx
                        val z = location.blockZ + dz
                        val centerY = location.blockY.coerceIn(world.minHeight + 1, world.maxHeight - 2)
                        for (dy in -4..4) {
                            val y = (centerY + dy).coerceIn(world.minHeight + 1, world.maxHeight - 2)
                            if (isSafe(world.getBlockAt(x, y, z).location)) {
                                return Location(world, x + 0.5, y.toDouble(), z + 0.5, location.yaw, location.pitch)
                            }
                        }

                        val surfaceY = world.getHighestBlockYAt(x, z) + 1
                        if (surfaceY in (world.minHeight + 1) until (world.maxHeight - 1)) {
                            val surface = Location(world, x + 0.5, surfaceY.toDouble(), z + 0.5, location.yaw, location.pitch)
                            if (isSafe(surface)) {
                                return surface
                            }
                        }
                    }
                }
            }
            return null
        }

        private fun isSafe(location: Location): Boolean {
            val world = location.world ?: return false
            if (world.environment == org.bukkit.World.Environment.NETHER && location.blockY >= world.maxHeight - 2) {
                return false
            }

            val feet = location.block
            val head = world.getBlockAt(location.blockX, location.blockY + 1, location.blockZ)
            val ground = world.getBlockAt(location.blockX, location.blockY - 1, location.blockZ)
            return ground.type.isSolid &&
                !DANGEROUS.contains(ground.type) &&
                feet.isPassable &&
                head.isPassable &&
                !DANGEROUS.contains(feet.type) &&
                !DANGEROUS.contains(head.type)
        }

        private val DANGEROUS = setOf(
            Material.LAVA,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.MAGMA_BLOCK,
            Material.CACTUS,
            Material.POWDER_SNOW
        )
    }
}
