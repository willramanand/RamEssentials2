package dev.willram.ramEssentials2.data

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.data.DataKeyCodec
import dev.willram.ramcore.data.FileDataRepository
import dev.willram.ramcore.data.GsonDataSerializer
import dev.willram.ramcore.scheduler.Schedulers
import org.bukkit.Material

class WarpDataRepository(plugin: RamEssentials2) : FileDataRepository<String, WarpData>(
    plugin.dataFolder.toPath().resolve("warps"),
    DataKeyCodec.stringKeys(),
    GsonDataSerializer.pretty(WarpData::class.java),
    Schedulers.async()
) {
    override fun setup() {
        migrateTo(2) { warp, _ ->
            if (warp.displayName.isBlank()) {
                warp.displayName = warp.name
            }
            warp
        }
        super.setup()
    }

    fun getIgnoreCase(name: String): WarpData? {
        return registry().entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
    }

    fun validateItems(): List<String> {
        val warnings = mutableListOf<String>()
        registry().forEach { (key, warp) ->
            if (warp.location == null) {
                warnings.add("warp '$key' has no location")
            }
            if (warp.name.isBlank()) {
                warnings.add("warp '$key' has a blank name")
            }
            if (Material.matchMaterial(warp.icon) == null) {
                warnings.add("warp '$key' has unknown icon '${warp.icon}'")
            }
        }
        return warnings
    }
}
