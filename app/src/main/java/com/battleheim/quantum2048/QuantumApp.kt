package com.battleheim.quantum2048
import android.app.Application
import com.battleheim.quantum2048.data.DataStoreGameRepository
class QuantumApp : Application() { val repository by lazy { DataStoreGameRepository(this) } }
