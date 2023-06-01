/*
 * Copyright 2023 Valerio Bonacina. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.bonacina.webviewnativesections.utils

import android.graphics.Rect
import android.view.MotionEvent
import android.view.View

object Utility {

    internal fun convertPixelsToPx(pixel: Int, scale: Float): Float {
        return pixel/scale
    }

    internal fun convertPxToPixels(dp: Float, scale: Float): Double {
        return dp.toDouble() * scale
    }

    internal fun isMotionEventInsideView(view: View, event: MotionEvent): Boolean {
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