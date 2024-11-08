package dev.willram.ramEssentials2.commands

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.RamCommand
import org.bukkit.Bukkit
import java.util.stream.Collectors

class ReplyCommand(private val plugin: RamEssentials2) : RamCommand(true, true, "ramessentials.reply", "<message>") {

    override fun perform(c: CommandContext) {
        val message: String = c.args().subList(0, c.args().size).stream().collect(Collectors.joining(" "))
        val data = plugin.players.get(c.player()?.uniqueId)

        if (data.lastReceivedPlayer == null) {
            c.msg("<red>You have no players who have recently messaged you.")
            return
        }

        val otherId = data.lastReceivedPlayer
        val other = Bukkit.getPlayer(otherId!!)

        if (other == null || !other.isOnline) {
            c.msg("<red>That player does not exist or is not online!")
            return
        }

        val otherData = plugin.players.get(otherId)
        // Add sender to other player's last received.
        otherData.lastReceivedPlayer = c.player()?.uniqueId!!

        c.msgOther(other, "<light_purple>FROM <gold>${c.player()!!.name}<light_purple>:<white> " + message)
        c.msg("<light_purple>TO <gold>${other.name}<light_purple>:<white> " + message)
    }

    override fun tabCompletes(c: CommandContext): MutableList<String> {
        return mutableListOf()
    }


}