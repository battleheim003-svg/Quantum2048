package com.battleheim.quantum2048.security

import android.content.Context

data class IntegrityVerdict(
    val allowsLocalSaves: Boolean,
    val source: String,
)

interface AppIntegrityGateway {
    suspend fun localSaveVerdict(context: Context): IntegrityVerdict
}

object LocalIntegrityGateway : AppIntegrityGateway {
    override suspend fun localSaveVerdict(context: Context): IntegrityVerdict =
        IntegrityVerdict(
            allowsLocalSaves = true,
            source = "local-placeholder",
        )
}
