package dev.willram.ramEssentials2.commands

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.RamCommand
import dev.willram.ramcore.utils.Formatter
import dev.willram.ramcore.utils.TxtUtils
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer

class BalanceCommand(private val plugin: RamEssentials2) : RamCommand(true, false, "ramessentials.balance", "[player]") {

    override fun perform(c: CommandContext) {
        if (c.argIsSet(0)) {
            val player = c.arg(0).parseOrFail(OfflinePlayer::class.java)

            if (!plugin.accounts.registry().containsKey(player.uniqueId)) {
                c.msg("<red>This player does not exist!")
                return
            }

            val balance = plugin.accounts.get(player.uniqueId).capital
            c.msg(TxtUtils.generateHeaderComponent("Accounts"))
            c.msg("<green>Wallet Balance: <gold>${Formatter.formatMoney(balance)}")
            return
        }

        val balance = plugin.accounts.get(c.player()?.uniqueId).capital
        c.msg(TxtUtils.generateHeaderComponent("Accounts"))
        c.msg("<green>Wallet Balance: <gold>${Formatter.formatMoney(balance)}")
    }

    override fun tabCompletes(c: CommandContext): MutableList<String> {
        if (c.args().size <= 1) {
            return Bukkit.getOnlinePlayers().map { it.name }.toMutableList()
        }
        return mutableListOf()
    }

}