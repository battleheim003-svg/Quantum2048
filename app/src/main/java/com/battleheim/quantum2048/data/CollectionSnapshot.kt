package com.battleheim.quantum2048.data

import com.battleheim.quantum2048.domain.CollectionState
import kotlinx.serialization.Serializable

const val COLLECTION_SNAPSHOT_SCHEMA_VERSION = 1

@Serializable
data class CollectionSnapshot(
    val schemaVersion: Int = COLLECTION_SNAPSHOT_SCHEMA_VERSION,
    val state: CollectionState = CollectionState(),
)
