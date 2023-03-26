package io.github.easyalbum.util

import android.widget.Toast
import androidx.annotation.StringRes
import io.github.easyalbum.application.GlobalConfig.appContext
import io.github.easyalbum.getStr

object ToastUtil {
    fun showTips(tips: String) {
        Toast.makeText(appContext, tips, Toast.LENGTH_SHORT).show()
    }

    fun showTips(@StringRes resID: Int) {
        Toast.makeText(appContext, getStr(resID), Toast.LENGTH_SHORT).show()
    }

}