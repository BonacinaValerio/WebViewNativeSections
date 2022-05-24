package it.bonacina.appwebview.ui.webview

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import timber.log.Timber
import java.lang.Exception

class WebViewInjectedView : RelativeLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun calculateCorrectHeaderHeight() {
        val setCorrectHeightRunnable = Runnable {
            try {
                val lp: ViewGroup.LayoutParams = layoutParams
                lp.height = LinearLayout.LayoutParams.WRAP_CONTENT
                layoutParams = lp
                setCorrectHeaderHeight()
            } catch (ignored: Exception) { }
        }
        try {
            post(setCorrectHeightRunnable)
        } catch (ignored: Exception) { }
    }

    private fun setCorrectHeaderHeight() {
        val forceCorrectHeightRunnable = Runnable {
            try {
                measure(
                    MeasureSpec.makeMeasureSpec(this.width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                )
                val targetHeight: Int = measuredHeight
                Timber.d("New targetHeight: %s", targetHeight)
                val lp: ViewGroup.LayoutParams = layoutParams
                if (lp.height != targetHeight) {
                    lp.height = targetHeight
                    layoutParams = lp
                    requestLayout()
                }
            } catch (ignored: Exception) {
            }
        }
        try {
            post(forceCorrectHeightRunnable)
        } catch (ignored: Exception) { }
    }
}