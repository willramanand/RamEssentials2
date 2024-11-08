package dev.willram.ramEssentials2.commands

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.RamCommand
import org.bukkit.Bukkit
import org.bukkit.World

class SetSpawnCommand(private val plugin: RamEssentials2) : RamCommand(true, true, "ramessentials.setspawn", "[world]") {
    override fun perform(c: CommandContext) {
        if (c.argIsSet(0)) {
            val world = c.arg(0).parseOrFail(World::class.java)
            world.spawnLocation = c.player()?.location!!
            c.msg("<yellow>Set spawn for world <light_purple>${world.name}")
            return
        }

        plugin.conf.serverSpawn = c.player()?.location!!
        c.msg("<yellow>Set spawn for <light_purple>server")
    }

    override fun tabCompletes(c: CommandContext): MutableList<String> {
        if (c.args().size <= 1) {
            return Bukkit.getWorlds().map { it.name }.toMutableList()
        }
        return mutableListOf()
    }
}