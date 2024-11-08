package dev.willram.ramEssentials2.commands

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramEssentials2.utils.TeleportUtil
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.RamCommand

class BackCommand(private val plugin: RamEssentials2) : RamCommand(true, true, "ramessentials.back", "") {

    override fun perform(c: CommandContext) {
        val data = plugin.players.get(c.player()?.uniqueId)
        if (data.lastLocation == null) {
            c.msg("<red>You do not have a saved previous location.")
            return
        }

        c.msg("<yellow>Returning to saved previous location.");
        TeleportUtil.teleport(c.player()!!, data.lastLocation!!)
    }

    override fun tabCompletes(c: CommandContext): MutableList<String> {
        return mutableListOf()
    }
}