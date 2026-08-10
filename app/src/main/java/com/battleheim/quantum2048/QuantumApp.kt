package com.battleheim.quantum2048
import android.app.Application
import com.battleheim.quantum2048.analytics.OfflineAnalyticsGateway
import com.battleheim.quantum2048.config.OfflineRemoteConfigGateway
import com.battleheim.quantum2048.data.DataStoreCollectionRepository
import com.battleheim.quantum2048.data.DataStoreGameRepository
import com.battleheim.quantum2048.data.DataStoreProfileRepository
import com.battleheim.quantum2048.data.DataStoreSettingsRepository
class QuantumApp : Application() {
    val repository by lazy { DataStoreGameRepository(this) }
    val collectionRepository by lazy { DataStoreCollectionRepository(this) }
    val profileRepository by lazy { DataStoreProfileRepository(this) }
    val settingsRepository by lazy { DataStoreSettingsRepository(this) }
    val analyticsGateway by lazy { OfflineAnalyticsGateway() }
    val remoteConfigGateway by lazy { OfflineRemoteConfigGateway() }

    override fun onCreate() {
        super.onCreate()
        remoteConfigGateway.refresh()
    }
}
