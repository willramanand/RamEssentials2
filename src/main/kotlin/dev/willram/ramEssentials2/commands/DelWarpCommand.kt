package dev.willram.ramEssentials2.commands

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.RamCommand

class DelWarpCommand(private val plugin: RamEssentials2) : RamCommand(true, true, "ramessentials.delwarp", "<name>") {

    override fun perform(c: CommandContext) {
        val name = c.arg(0).parseOrFail(String::class.java)

        if (!plugin.warps.contains(name)) {
            c.msg("<red>No warp with the name <light_purple>$name <yellow>exists!")
            return
        }

        plugin.warps.remove(name)
        c.msg("<yellow>Warp <light_purple>$name <yellow>has been deleted!")
    }

    override fun tabCompletes(c: CommandContext): MutableList<String> {
        if (c.args().size <= 1) {
            return plugin.warps.keys.map { it }.toMutableList()
        }

        return mutableListOf()
    }
}