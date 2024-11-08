package dev.willram.ramEssentials2.commands

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.RamCommand

class SetHomeCommand(private val plugin: RamEssentials2) : RamCommand(true, true, "ramessentials.sethome", "<name>") {

    override fun perform(c: CommandContext) {
        val name = c.arg(0).parseOrFail(String::class.java)
        val playerData = plugin.players.get(c.player()?.uniqueId)
        val location = c.player()?.location

        if (playerData.homes.size >= plugin.conf.maxHomes) {
            c.msg("<red>You cannot set another home!")
            return
        }

        playerData.homes[name] = location!!
        c.msg("<yellow>Set home <light_purple>$name<yellow> at this location.")
    }

    override fun tabCompletes(c: CommandContext): MutableList<String> {
        return mutableListOf()
    }
}