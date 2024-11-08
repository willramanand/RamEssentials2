package dev.willram.ramEssentials2.data.serializers

import dev.willram.ramEssentials2.data.AccountData
import dev.willram.ramcore.configurate.ConfigurationNode
import dev.willram.ramcore.configurate.serialize.SerializationException
import dev.willram.ramcore.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class AccountDataSerializer : TypeSerializer<AccountData> {

    companion object {
        val INSTANCE = AccountDataSerializer()
    }

    private val PLAYER_NAME = "player_name"
    private val CAPITAL = "capital"

    private fun nonVirtualNode(
        source: ConfigurationNode,
        vararg path: Any
    ): ConfigurationNode {
        if (!source.hasChild(*path)) {
            throw SerializationException("Required field " + path.contentToString() + " was not present in node")
        }
        return source.node(*path)
    }

    override fun deserialize(type: Type?, node: ConfigurationNode?): AccountData {
        val playerName = nonVirtualNode(node!!, PLAYER_NAME).string
        val capital = nonVirtualNode(node, CAPITAL).double

        var accountData = AccountData()
        accountData.playerName = playerName!!
        accountData.capital = capital

        return accountData
    }

    override fun serialize(type: Type?, acc: AccountData?, node: ConfigurationNode?) {
        if (acc == null) {
            node!!.raw(null)
            return
        }

        node!!.node(PLAYER_NAME).set(acc.playerName)
        node.node(CAPITAL).set(acc.capital)
    }
}