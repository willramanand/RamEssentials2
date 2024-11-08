package dev.willram.ramEssentials2.commands

import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.RamCommand
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player

class GamemodeCommand : RamCommand(true, true, "ramessentials.gamemode", "<gamemode> [player]") {
    override fun perform(c: CommandContext) {
        val gm = c.arg(0).parseOrFail(String::class.java)

        var gameMode: GameMode? = null
        when (gm) {
            "s", "0" -> gameMode = GameMode.SURVIVAL
            "c", "1" -> gameMode = GameMode.CREATIVE
            "a", "2" -> gameMode = GameMode.ADVENTURE
            "sp", "3" -> gameMode = GameMode.SPECTATOR
        }

        if (gameMode == null) {
            try {
                gameMode = GameMode.valueOf(gm.uppercase())
            } catch (e: Exception) {
                c.msg("<red>The gamemode you entered was not valid!")
                return
            }
        }


        if (!c.argIsSet(1)) {
            c.player()?.gameMode = gameMode;
            c.msg("<yellow>Your gamemode has been set to <light_purple>${gameMode.name.lowercase()}")
            return;
        }


        c.arg(1).parse(Player::class.java).ifPresent { other ->
            other.gameMode = gameMode;
            c.msg("<yellow>You have set <light_purple>${other.name}'s <yellow>gamemode to <light_purple>${gameMode.name.lowercase()}")
            c.msgOther(other, "<yellow>Your gamemode has been set to <light_purple>${gameMode.name.lowercase()}")
        }
    }

    override fun tabCompletes(c: CommandContext): MutableList<String> {
        if (c.args().size <= 1) {
            return GameMode.entries.map { it.name.lowercase() }.toMutableList()
        }

        if (c.args().size == 2) {
            return Bukkit.getOnlinePlayers().map { it.name }.toMutableList()
        }

        return mutableListOf()
    }
}