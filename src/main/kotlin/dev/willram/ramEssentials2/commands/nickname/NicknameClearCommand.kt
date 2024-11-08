package dev.willram.ramEssentials2.commands.nickname

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.RamCommand

class NicknameClearCommand(private val plugin: RamEssentials2) : RamCommand(true, true, "ramessentials.nickname.clear", "") {

    init {
        this.aliases = listOf("clear", "c")
    }

    override fun perform(c: CommandContext) {
       val data =  plugin.players.get(c.player()?.uniqueId)
       data.nickname = null
       c.player()?.displayName(c.player()?.name())

       c.msg("<yellow>Nickname cleared.")
    }

    override fun tabCompletes(p0: CommandContext?): MutableList<String> {
        return mutableListOf()
    }
}