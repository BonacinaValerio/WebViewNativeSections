package it.bonacina.appwebview.utils

import android.content.res.Resources
import android.graphics.Rect
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

object Utility {

    fun toPx(dp: Int, resources: Resources): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()
    }

    fun convertPixelsToPx(pixel: Int, scale: Float): Float {
        return pixel/scale
    }

    fun isMotionEventInsideView(view: View, event: MotionEvent): Boolean {
        val eventX = event.rawX.toInt()
        val eventY = event.rawY.toInt()

        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val width: Int = view.width
        val height: Int = view.height
        val left = location[0]
        val top = location[1]
        val right = left + width
        val bottom = top + height

        val rect = Rect(left, top, right, bottom)
        return rect.contains(eventX, eventY)
    }
}