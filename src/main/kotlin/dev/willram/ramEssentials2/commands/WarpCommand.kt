package dev.willram.ramEssentials2.commands

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramEssentials2.utils.TeleportUtil
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.RamCommand
import org.bukkit.event.player.PlayerTeleportEvent

class WarpCommand(private val plugin: RamEssentials2) : RamCommand(true, true, "ramessentials.warp", "<name>") {

    override fun perform(c: CommandContext) {
        val name = c.arg(0).parseOrFail(String::class.java)

        if (!plugin.warps.contains(name)) {
            c.msg("<red>No warp with the name <light_purple>$name <red>exists!")
            return
        }

        c.msg("<yellow>Teleporting to warp <light_purple>$name")
        c.player()?.teleportAsync(plugin.warps[name]!!, PlayerTeleportEvent.TeleportCause.COMMAND)
        TeleportUtil.teleport(c.player()!!, plugin.warps[name]!!)
    }

    override fun tabCompletes(c: CommandContext): MutableList<String> {
        if (c.args().size <= 1) {
            return plugin.warps.keys.map { it }.toMutableList()
        }

        return mutableListOf()
    }
}