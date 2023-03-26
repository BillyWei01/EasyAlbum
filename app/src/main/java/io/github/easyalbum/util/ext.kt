package io.github.easyalbum

import android.view.View
import androidx.annotation.StringRes
import io.github.album.ui.ClickHelper
import io.github.easyalbum.application.GlobalConfig

fun <T : View> T.onClick(block: () -> Unit) {
    ClickHelper.listen(this) {
        block.invoke()
    }
}

fun getStr(@StringRes resId: Int): String {
    return GlobalConfig.appContext.getString(resId)
}

fun getStr(@StringRes resId: Int, vararg formatArgs: Any?): String {
    return GlobalConfig.appContext.getString(resId, *formatArgs)
}

