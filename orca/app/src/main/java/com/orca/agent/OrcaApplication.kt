package com.orca.agent

import android.app.Application
import com.orca.agent.core.OrcaCore
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class OrcaApplication : Application() {
    
    @Inject
    lateinit var orcaCore: OrcaCore

    override fun onCreate() {
        super.onCreate()
        instance = this
        orcaCore.initialize()
    }

    companion object {
        lateinit var instance: OrcaApplication
            private set
    }
}
