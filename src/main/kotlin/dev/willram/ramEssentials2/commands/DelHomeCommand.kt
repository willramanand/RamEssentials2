package dev.willram.ramEssentials2.commands

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.RamCommand

class DelHomeCommand(private val plugin: RamEssentials2): RamCommand(true, true, "ramessentials.delhome", "<name>") {
    override fun perform(c: CommandContext) {
        val name = c.arg(0).parseOrFail(String::class.java)
        val playerData = plugin.players.get(c.player()?.uniqueId)

        if (!playerData.homes.containsKey(name)) {
            c.sender().sendRichMessage("<red>A home with the name <light_purple>$name <red>does not exist.")
            return
        }

        playerData.homes.remove(name)
        c.sender().sendRichMessage("<yellow>Removed player home named <light_purple>$name")
    }

    override fun tabCompletes(c: CommandContext): MutableList<String> {
        if (c.args().size <= 1) {
            return plugin.players.get(c.player()?.uniqueId).homes.keys.toMutableList()
        }
        return mutableListOf()
    }

}