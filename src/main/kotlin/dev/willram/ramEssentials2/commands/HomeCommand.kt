package dev.willram.ramEssentials2.commands

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramEssentials2.utils.TeleportUtil
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.RamCommand
import org.bukkit.event.player.PlayerTeleportEvent

class HomeCommand(private val plugin: RamEssentials2) : RamCommand(true, true, "ramessentials.home", "<name>") {
    override fun perform(c: CommandContext) {
        val playerData = plugin.players.get(c.player()?.uniqueId)
        val homeName = c.arg(0).parseOrFail(String::class.java)

        if (!playerData.homes.containsKey(homeName)) {
            c.msg("<red>You do not have a home with that name!")
            return
        }

        c.msg("<yellow>Teleporting to home <light_purple>$homeName")
        TeleportUtil.teleport(c.player()!!, playerData.homes[homeName]!!)
    }

    override fun tabCompletes(c: CommandContext): MutableList<String> {
        if (c.args().size <= 1) {
            return plugin.players.get(c.player()?.uniqueId).homes.keys.toMutableList()
        }
        return mutableListOf()
    }
}