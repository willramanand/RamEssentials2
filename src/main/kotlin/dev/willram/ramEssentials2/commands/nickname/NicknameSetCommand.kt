package dev.willram.ramEssentials2.commands.nickname

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.RamCommand
import net.kyori.adventure.text.minimessage.MiniMessage

class NicknameSetCommand(private val plugin: RamEssentials2) : RamCommand(true, true, "ramessentials.nickname.set", "<nickname>") {

    init {
        this.aliases = listOf("set", "s")
    }

    override fun perform(c: CommandContext) {
        val nickname = c.arg(0).parseOrFail(String::class.java)
        val nicknameComponent = MiniMessage.miniMessage().deserialize(nickname)
        val playerData = plugin.players.get(c.player()?.uniqueId)

        playerData.nickname = nickname
        c.player()?.displayName(nicknameComponent)
        c.msg("<yellow>Nickname has been set to: $nickname")
    }

    override fun tabCompletes(c: CommandContext): MutableList<String> {
        return mutableListOf()
    }

}
