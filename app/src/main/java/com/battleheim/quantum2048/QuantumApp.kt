package com.battleheim.quantum2048
import android.app.Application
import com.battleheim.quantum2048.analytics.OfflineAnalyticsGateway
import com.battleheim.quantum2048.ads.OfflineAdGateway
import com.battleheim.quantum2048.config.OfflineRemoteConfigGateway
import com.battleheim.quantum2048.data.AssetLevelCatalogRepository
import com.battleheim.quantum2048.data.DataStoreBillingRepository
import com.battleheim.quantum2048.data.DataStoreCollectionRepository
import com.battleheim.quantum2048.data.DataStoreGameRepository
import com.battleheim.quantum2048.data.DataStoreLevelProgressRepository
import com.battleheim.quantum2048.data.DataStoreProfileRepository
import com.battleheim.quantum2048.data.DataStoreSettingsRepository
import com.battleheim.quantum2048.data.DataStoreSocialRepository
import com.battleheim.quantum2048.data.DataStoreStatisticsRepository
import com.battleheim.quantum2048.social.OfflinePlayGamesGateway
class QuantumApp : Application() {
    val repository by lazy { DataStoreGameRepository(this) }
    val collectionRepository by lazy { DataStoreCollectionRepository(this) }
    val profileRepository by lazy { DataStoreProfileRepository(this) }
    val settingsRepository by lazy { DataStoreSettingsRepository(this) }
    val socialRepository by lazy { DataStoreSocialRepository(this) }
    val statisticsRepository by lazy { DataStoreStatisticsRepository(this) }
    val billingRepository by lazy { DataStoreBillingRepository(this) }
    val levelCatalogRepository by lazy { AssetLevelCatalogRepository(this) }
    val levelProgressRepository by lazy { DataStoreLevelProgressRepository(this) }
    val adGateway by lazy { OfflineAdGateway() }
    val analyticsGateway by lazy { OfflineAnalyticsGateway() }
    val remoteConfigGateway by lazy { OfflineRemoteConfigGateway() }
    val playGamesGateway by lazy { OfflinePlayGamesGateway() }

    override fun onCreate() {
        super.onCreate()
        remoteConfigGateway.refresh()
    }
}
