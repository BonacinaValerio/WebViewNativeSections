package it.bonacina.webviewzoomable.utils

import android.content.res.Resources
import android.graphics.Rect
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

object Utility {

    fun convertPixelsToPx(pixel: Int, scale: Float): Float {
        return pixel/scale
    }
    fun convertPxToPixels(dp: Float, scale: Float): Double {
        return dp.toDouble() * scale
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