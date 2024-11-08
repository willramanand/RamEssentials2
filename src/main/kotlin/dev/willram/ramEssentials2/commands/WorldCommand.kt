package dev.willram.ramEssentials2.commands

import dev.willram.ramEssentials2.utils.TeleportUtil
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.RamCommand
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.event.player.PlayerTeleportEvent

class WorldCommand : RamCommand(true, true, "ramessentials.world", "<world>") {

    override fun perform(c: CommandContext) {
        val world = c.arg(0).parseOrFail(World::class.java)
        c.player()?.teleportAsync(world.spawnLocation, PlayerTeleportEvent.TeleportCause.COMMAND)
        TeleportUtil.teleport(c.player()!!, world.spawnLocation)
        c.msg("<yellow>Teleporting to world <light_purple>${world.name}")
    }

    override fun tabCompletes(c: CommandContext): MutableList<String> {
        if (c.args().size <= 1) {
            val worlds = ArrayList<String>()
            Bukkit.getWorlds().forEach {
                worlds.add(it.name)
            }
            return worlds
        }
        return mutableListOf()
    }

}