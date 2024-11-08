package dev.willram.ramEssentials2.commands

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.RamCommand

class DoNotDisturbCommand(private val plugin: RamEssentials2) : RamCommand(true, true, "ramessentials.donotdisturb", "") {
    override fun perform(c: CommandContext) {
        val playerData = plugin.players.get(c.player()?.uniqueId)

        if (playerData.doNotDisturb) {
            playerData.doNotDisturb = false
            c.msg("<yellow>You have <green>enabled<yellow> do not disturb.")
            return
        }

        playerData.doNotDisturb = true
        c.msg("<yellow>You have <red>disabled<yellow> do not disturb.")
    }

    override fun tabCompletes(c: CommandContext): MutableList<String> {
        return mutableListOf()
    }
}