package it.bonacina.appwebview.ui.webview

import android.view.MotionEvent
import android.view.View
import it.bonacina.appwebview.utils.Utility.isMotionEventInsideView

class DefaultWebViewTouchListener: WebViewTouchListener {

    private var touchStayedWithinViewBounds = false

    override fun onTouchEvent(view: View, event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (isMotionEventInsideView(view, event)) {
                    touchStayedWithinViewBounds = true
                    view.onTouchEvent(event)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchStayedWithinViewBounds) {
                    view.onTouchEvent(MotionEvent.obtainNoHistory(event).apply {
                        action = MotionEvent.ACTION_CANCEL
                    })
                    touchStayedWithinViewBounds = false
                }
            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                if (touchStayedWithinViewBounds) {
                    view.onTouchEvent(event)
                    touchStayedWithinViewBounds = false
                }
            }
        }
    }

}