package com.myexperience.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.myexperience.app.data.AppDatabase
import com.myexperience.app.data.Repository

class MyExperienceApp : Application(), ImageLoaderFactory {

    val database by lazy { AppDatabase.build(this) }
    val repository by lazy { Repository(this, database) }

    override fun onCreate() {
        super.onCreate()
        org.maplibre.android.MapLibre.getInstance(this)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()
}
