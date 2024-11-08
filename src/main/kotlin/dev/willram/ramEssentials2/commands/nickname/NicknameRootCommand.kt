package dev.willram.ramEssentials2.commands.nickname

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.RamCommand


class NicknameRootCommand(plugin: RamEssentials2) : RamCommand(true, true, "ramessentials.nickname", "") {

    init {
        this.subCommands.add(NicknameSetCommand(plugin))
        this.subCommands.add(NicknameClearCommand(plugin))
    }

    override fun perform(c: CommandContext) {
        c.commandChain.add(this);
    }

    override fun tabCompletes(c: CommandContext): MutableList<String> {
        return mutableListOf()
    }


}