package dev.willram.ramEssentials2.commands

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.RamCommand

class SetWarpCommand(private val plugin: RamEssentials2) : RamCommand(true, true, "ramessentials.setwarp", "<name>") {

    override fun perform(c: CommandContext) {
        val name = c.arg(0).parseOrFail(String::class.java)

        if (plugin.warps.contains(name)) {
            c.msg("<red>A warp with the name <light_purple>$name <red>already exists!")
            return
        }

        plugin.warps[name] = c.player()?.location!!
        c.msg("<yellow>You have set a warp with name <light_purple>$name <yellow>at your current location.")
    }

    override fun tabCompletes(c: CommandContext): MutableList<String> {
        return mutableListOf()
    }

}