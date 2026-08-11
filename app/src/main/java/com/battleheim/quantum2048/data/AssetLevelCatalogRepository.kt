package com.battleheim.quantum2048.data

import android.content.Context
import com.battleheim.quantum2048.domain.LevelCatalog
import com.battleheim.quantum2048.domain.LevelCatalogRepository
import kotlinx.serialization.json.Json

class AssetLevelCatalogRepository(private val context: Context) : LevelCatalogRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var cached: LevelCatalog? = null

    override suspend fun catalog(): LevelCatalog {
        cached?.let { return it }
        val decoded = context.assets.open(CATALOG_PATH).bufferedReader().use { reader ->
            json.decodeFromString<LevelCatalog>(reader.readText())
        }
        cached = decoded
        return decoded
    }

    private companion object {
        const val CATALOG_PATH = "levels/periodic_path_v1.json"
    }
}
