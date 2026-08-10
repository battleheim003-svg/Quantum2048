package com.battleheim.quantum2048.config

interface RemoteConfigGateway {
    fun refresh()
    fun double(key: String, defaultValue: Double): Double
    fun long(key: String, defaultValue: Long): Long
    fun boolean(key: String, defaultValue: Boolean): Boolean
}

object NoOpRemoteConfigGateway : RemoteConfigGateway {
    override fun refresh() = Unit
    override fun double(key: String, defaultValue: Double): Double = defaultValue
    override fun long(key: String, defaultValue: Long): Long = defaultValue
    override fun boolean(key: String, defaultValue: Boolean): Boolean = defaultValue
}
