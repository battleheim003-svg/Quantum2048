package com.battleheim.quantum2048.config

class OfflineRemoteConfigGateway : RemoteConfigGateway {
    private val defaults = mapOf(
        "quantum_superposition_spawn_chance" to 0.10,
        "quantum_entanglement_spawn_chance" to 0.12,
        "quantum_initial_energy" to 30L,
        "quantum_daily_enabled" to true,
    )

    override fun refresh() {
        Unit
    }

    override fun double(key: String, defaultValue: Double): Double =
        defaults[key] as? Double ?: defaultValue

    override fun long(key: String, defaultValue: Long): Long =
        defaults[key] as? Long ?: defaultValue

    override fun boolean(key: String, defaultValue: Boolean): Boolean =
        defaults[key] as? Boolean ?: defaultValue
}
