package io.github.easyalbum.util

import android.content.res.Resources
import kotlin.math.roundToInt

object Utils {
    private var density = 1f

    private fun takeDensity(): Float {
        if (density == 1f) {
            density = Resources.getSystem().displayMetrics.density
        }
        return density
    }

    fun dp2px(dp: Float): Int {
        return (dp * takeDensity()).roundToInt()
    }
}