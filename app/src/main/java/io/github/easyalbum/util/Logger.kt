package io.github.easyalbum.util

import io.github.album.interfaces.AlbumLogger
import io.github.easyalbum.util.LogUtil

object Logger : AlbumLogger {
    override fun d(tag: String, message: String) {
        LogUtil.d(tag, message)
    }

    override fun e(tag: String, e: Throwable) {
        LogUtil.e(tag, e)
    }
}