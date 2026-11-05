package com.myexperience.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.myexperience.app.data.AppDatabase
import com.myexperience.app.data.Repository
import java.io.File

class MyExperienceApp : Application(), ImageLoaderFactory {

    val database by lazy { AppDatabase.build(this) }
    val repository by lazy { Repository(this, database) }

    override fun onCreate() {
        super.onCreate()
        val cfg = org.osmdroid.config.Configuration.getInstance()
        cfg.userAgentValue = packageName
        val base = File(cacheDir, "osmdroid")
        cfg.osmdroidBasePath = base
        cfg.osmdroidTileCache = File(base, "tiles")
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()
}
