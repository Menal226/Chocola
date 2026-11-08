@file:OptIn(KoinExperimentalAPI::class)

package com.sosauce.chocola.wear.app

import android.app.Application
import com.sosauce.chocola.wear.app.di.wearModule
import org.koin.android.ext.koin.androidContext
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinConfiguration

class App : Application(), KoinStartup {
    override fun onKoinStartup() = koinConfiguration {
        androidContext(this@App)
        modules(wearModule)
    }
}
