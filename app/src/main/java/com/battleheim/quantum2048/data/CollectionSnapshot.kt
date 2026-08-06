package com.battleheim.quantum2048.data

import com.battleheim.quantum2048.domain.CollectionState
import kotlinx.serialization.Serializable

@Serializable
data class CollectionSnapshot(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val state: CollectionState = CollectionState(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
