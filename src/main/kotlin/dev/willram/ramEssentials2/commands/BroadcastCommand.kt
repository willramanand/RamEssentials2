package dev.willram.ramEssentials2.commands

import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.RamCommand
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import java.util.stream.Collectors

class BroadcastCommand : RamCommand(true, false, "ramessentials.broadcast", "<message>") {

    override fun perform(c: CommandContext) {
        val message: String = c.args().subList(0, c.args().size).stream().collect(Collectors.joining(" "))
        val component = MiniMessage.miniMessage().deserialize("<rainbow>[Broadcast] <white>$message")

        Bukkit.broadcast(component)
    }

    override fun tabCompletes(c: CommandContext): MutableList<String> {
        return mutableListOf()
    }

}