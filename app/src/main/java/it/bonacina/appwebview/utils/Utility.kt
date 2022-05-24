package it.bonacina.appwebview.utils

import android.content.res.Resources
import android.util.TypedValue

object Utility {

    fun toPx(dp: Int, resources: Resources): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()
    }

    fun convertPixelsToPx(pixel: Int, scale: Float): Float {
        return pixel/scale
    }
}