package io.github.easyalbum.application

import android.app.Application
import android.content.Context
import io.github.easyalbum.BuildConfig

object GlobalConfig {
    @JvmField
    val DEBUG = BuildConfig.DEBUG

    lateinit var appContext: Context

    fun setAppContext(context: Application) {
        appContext = context
    }
}