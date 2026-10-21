package com.hotwheelsgarage

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.hotwheelsgarage.alerts.AlertScheduler
import com.hotwheelsgarage.alerts.Notifications
import com.hotwheelsgarage.data.AppDatabase
import com.hotwheelsgarage.data.CarRepository
import com.hotwheelsgarage.data.Http
import com.hotwheelsgarage.data.Settings
import com.hotwheelsgarage.data.WikiApi
import com.hotwheelsgarage.scan.Identifier

class HotWheelsApp : Application(), ImageLoaderFactory {
    val database by lazy { AppDatabase.create(this) }
    val repository by lazy { CarRepository(database.carDao()) }
    val settings by lazy { Settings(this) }
    val wiki by lazy { WikiApi() }
    val identifier by lazy { Identifier(wiki, settings) }

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        AlertScheduler.apply(this)
        // Give cars saved before collector scores existed their score.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { repository.scoreUnscored { wiki.versionCount(it) } }
        }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient(Http.client)
            .crossfade(true)
            .build()
}
