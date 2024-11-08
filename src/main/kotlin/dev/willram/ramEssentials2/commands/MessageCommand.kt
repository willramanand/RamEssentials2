package dev.willram.ramEssentials2.commands

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.RamCommand
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.stream.Collectors

class MessageCommand(private val plugin: RamEssentials2) : RamCommand(true, true, "", "<player> <message>") {
    override fun perform(c: CommandContext) {
        val sender: Player? = c.player()
        val other: Player = c.arg(0).parseOrFail(Player::class.java)
        val message: String = c.args().subList(1, c.args().size).stream().collect(Collectors.joining(" "))

        if (sender?.uniqueId == other.uniqueId) {
            c.msg("<red>You cannot send a message to yourself!")
            return
        }

        val data = plugin.players.get(sender?.uniqueId)

        if (data.doNotDisturb) {
            c.msg("<red>You cannot message other players while do not disturb is enabled!")
            return
        }

        if (data.ignoredPlayers.contains(other.uniqueId)) {
            c.msg("<red>You cannot message a player you are currently ignoring!")
        }

        val otherData = plugin.players.get(other.uniqueId)

        if (otherData.doNotDisturb) {
            c.msg("<red>This player is do not disturb and cannot be messaged.")
            return
        }

        if (otherData.ignoredPlayers.contains(sender?.uniqueId)) {
            c.msg("<red>This player is ignoring you.")
            return
        }

        // Add sender to other player's last received.
        otherData.lastReceivedPlayer = sender?.uniqueId!!

        c.msgOther(other, "<light_purple>FROM <gold>${sender.name}<light_purple>:<white> " + message)
        c.msg("<light_purple>TO <gold>${other.name}<light_purple>:<white> " + message)
    }

    override fun tabCompletes(c: CommandContext): MutableList<String> {
        if (c.args().size <= 1) {
            val players = ArrayList<String>()
            Bukkit.getOnlinePlayers().forEach { p -> players.add(p.name) }
            return players
        }
        return mutableListOf()
    }
}