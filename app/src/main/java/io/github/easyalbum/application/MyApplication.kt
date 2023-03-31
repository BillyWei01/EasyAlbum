package io.github.easyalbum.application

import android.app.Application
import androidx.recyclerview.widget.DefaultItemAnimator
import io.github.album.EasyAlbum
import io.github.easyalbum.album.DoodleImageLoader
import io.github.easyalbum.album.GlideImageLoader
import io.github.easyalbum.util.IOExecutor
import io.github.easyalbum.util.Logger

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initApplication(this)
    }

    private fun initApplication(context: Application) {
        GlobalConfig.setAppContext(context)
        EasyAlbum.config()
            .setLogger(Logger)
            .setExecutor(IOExecutor)
            .enableInfoChecking()
            .setImageLoader(GlideImageLoader)
            // .setImageLoader(DoodleImageLoader)
            .setDefaultFolderComparator { o1, o2 -> o1.name.compareTo(o2.name)}
            .setItemAnimator(DefaultItemAnimator())
    }
}