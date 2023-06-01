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

package it.bonacina.webviewnativesections.view.webview

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import it.bonacina.webviewnativesections.utils.Utility.isMotionEventInsideView
import timber.log.Timber
import kotlin.math.absoluteValue

/**
 * DefaultWebViewTouchListener is a default touch listener that handles touch events on clickable objects within the footer and header of WebViewInjectedView.
 *
 * It is designed to provide basic functionality for managing touch interactions on components like buttons. This touch listener can be set for the internal views associated with WebViewInjectedView.
 *
 * Implements the [View.OnTouchListener] interface.
 */
class DefaultWebViewTouchListener: View.OnTouchListener {

    private var startEvent: MotionEvent? = null
    private var clickCancelled: Boolean = false

    @SuppressLint("Recycle")
    private fun obtainScrollParameters(event: MotionEvent) {
        startEvent = MotionEvent.obtainNoHistory(event)
    }

    private fun recycleScrollParameters() {
        startEvent?.recycle()
        startEvent = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        if (isMotionEventInsideView(view, event)) {
            val currentTouchSlop = ViewConfiguration.get(view.context).scaledTouchSlop/5
            when (event.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN,
                MotionEvent.ACTION_DOWN -> {
                    clickCancelled = false
                    obtainScrollParameters(event)
                    return false
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    recycleScrollParameters()
                    if (!clickCancelled) {
                        clickCancelled = true
                        Timber.d("TouchListener: onInterceptTouchEvent -> false")
                        return false
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!clickCancelled) {
                        startEvent?.let { e1 ->
                            val dX = e1.rawX - event.rawX

                            Timber.d("TouchListener: $dX $currentTouchSlop")
                            if (dX.absoluteValue > currentTouchSlop) {

                                view.onTouchEvent(MotionEvent.obtainNoHistory(event).apply {
                                    action = MotionEvent.ACTION_CANCEL
                                })
                                clickCancelled = true
                                Timber.d("TouchListener: onInterceptTouchEvent -> true")
                                return true
                            }
                        }
                    }
                }
            }
        }

        Timber.d("TouchListener: onInterceptTouchEvent -> $clickCancelled")
        return clickCancelled
    }

}