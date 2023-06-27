package io.github.easyalbum.application

import android.app.Application
import androidx.recyclerview.widget.DefaultItemAnimator
import io.github.album.EasyAlbum
import io.github.easyalbum.album.DoodleImageLoader
import io.github.easyalbum.album.GlideImageLoader
import io.github.easyalbum.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initApplication(this)
    }

    private fun initApplication(context: Application) {
        GlobalConfig.setAppContext(context)
        EasyAlbum.config()
            .setLogger(Logger)
            .setExecutor(Dispatchers.IO.asExecutor())
            //.setImageLoader(GlideImageLoader)
            .setImageLoader(DoodleImageLoader)
            .setDefaultFolderComparator { o1, o2 -> o1.name.compareTo(o2.name)}
            .setItemAnimator(DefaultItemAnimator())
    }
}