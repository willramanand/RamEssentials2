package dev.willram.ramEssentials2.commands

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.RamCommand

class GodCommand(private val plugin: RamEssentials2) : RamCommand(true, true, "ramessentials.god", "") {
    override fun perform(c: CommandContext) {
        val uniqueId = c.player()?.uniqueId
        val playerData = plugin.players.get(uniqueId)
        if (playerData.godMode) {
            playerData.godMode = false
            c.msg("<yellow>God mode set to <red>disabled")
            return
        }

        playerData.godMode = true
        c.msg("<yellow>God mode set to <green>enabled")
    }

    override fun tabCompletes(c: CommandContext): MutableList<String> {
        return mutableListOf()
    }
}