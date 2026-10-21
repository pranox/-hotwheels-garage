package com.hotwheelsgarage

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.hotwheelsgarage.data.AppDatabase
import com.hotwheelsgarage.data.CarRepository
import com.hotwheelsgarage.data.Http
import com.hotwheelsgarage.data.Settings
import com.hotwheelsgarage.data.WikiApi
import com.hotwheelsgarage.scan.Identifier

class HotWheelsApp : Application(), ImageLoaderFactory {
    val repository by lazy { CarRepository(AppDatabase.create(this).carDao()) }
    val settings by lazy { Settings(this) }
    val wiki by lazy { WikiApi() }
    val identifier by lazy { Identifier(wiki, settings) }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient(Http.client)
            .crossfade(true)
            .build()
}
