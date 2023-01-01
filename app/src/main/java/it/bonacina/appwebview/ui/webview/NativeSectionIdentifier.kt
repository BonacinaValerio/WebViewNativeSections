package it.bonacina.appwebview.ui.webview

import android.view.View
import androidx.annotation.IdRes

data class NativeSectionViewIdentifier(
    val sectionId: String,
    val type: WebViewZoomable.NativeSectionType,
    val viewAndTouchListeners: List<ViewAndTouchListener>
)

data class ViewAndTouchListener(
    @IdRes val viewId: Int,
    val touchListener: View.OnTouchListener
)
