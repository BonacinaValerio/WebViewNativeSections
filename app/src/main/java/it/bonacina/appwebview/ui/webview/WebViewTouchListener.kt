package it.bonacina.appwebview.ui.webview

import android.graphics.Rect
import android.view.MotionEvent
import android.view.View

interface WebViewTouchListener {
    fun onTouchEvent(view: View, event: MotionEvent)
}