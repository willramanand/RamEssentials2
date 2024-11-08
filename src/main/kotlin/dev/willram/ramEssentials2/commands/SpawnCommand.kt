package dev.willram.ramEssentials2.commands

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramEssentials2.utils.TeleportUtil
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.RamCommand
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.event.player.PlayerTeleportEvent

class SpawnCommand(private val plugin: RamEssentials2) : RamCommand(true, true, "ramessentials.spawn", "[world]") {
    override fun perform(c: CommandContext) {
        if (c.argIsSet(0)) {
            val world = c.arg(0).parseOrFail(World::class.java)
            c.msg("<yellow>Teleporting to spawn of world <light_purple>${world.name}")
            TeleportUtil.teleport(c.player()!!, world.spawnLocation)
            return
        }

        c.msg("<yellow>Teleporting to <light_purple>server spawn")
        plugin.conf.serverSpawn.let { TeleportUtil.teleport(c.player()!!, it) }
    }

    override fun tabCompletes(c: CommandContext): MutableList<String> {
        if (c.args().size <= 1) {
            return Bukkit.getWorlds().map { it.name }.toMutableList()
        }
        return mutableListOf()
    }

}