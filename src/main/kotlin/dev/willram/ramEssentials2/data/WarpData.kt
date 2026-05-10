package dev.willram.ramEssentials2.data

import dev.willram.ramcore.data.DataItem
import org.bukkit.Location
import java.util.UUID

class WarpData : DataItem() {
    var name: String = ""
    var displayName: String = ""
    var location: Location? = null
    var createdBy: UUID? = null
    var createdAt: Long = System.currentTimeMillis()
    var permission: String? = null
    var category: String = "default"
    var icon: String = "ENDER_PEARL"

    fun visibleName(): String = displayName.ifBlank { name }

    fun visibleTo(hasPermission: (String) -> Boolean): Boolean {
        val required = permission
        return required.isNullOrBlank() || hasPermission(required)
    }
}
