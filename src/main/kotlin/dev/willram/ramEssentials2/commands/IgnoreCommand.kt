package dev.willram.ramEssentials2.commands

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.RamCommand
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class IgnoreCommand(private val plugin: RamEssentials2) : RamCommand(true, true, "ramessentials.ignore", "<player>") {
    override fun perform(c: CommandContext) {
        val playerToIgnore = c.arg(0).parseOrFail(Player::class.java)
        val playerData = plugin.players.get(c.player()?.uniqueId)

        if (playerData.ignoredPlayers.contains(playerToIgnore.uniqueId)) {
            playerData.ignoredPlayers -= playerToIgnore.uniqueId
            c.msg("<yellow>You have removed <light_purple>${playerToIgnore.name} <yellow>from your ignore list.")
            return
        }

        playerData.ignoredPlayers += playerToIgnore.uniqueId
        c.msg("<yellow>You have added <light_purple>${playerToIgnore.name} <yellow>from your ignore list.")
    }

    override fun tabCompletes(c: CommandContext): MutableList<String> {
        if (c.args().size <= 1) {
            return Bukkit.getOnlinePlayers().map { it.name }.toMutableList()
        }
        return mutableListOf()
    }
}