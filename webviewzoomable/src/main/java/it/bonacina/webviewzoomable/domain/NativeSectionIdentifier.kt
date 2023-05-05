package it.bonacina.webviewzoomable.domain

import android.view.View
import androidx.annotation.IdRes
import it.bonacina.webviewzoomable.view.webview.WebViewZoomable

data class NativeSectionViewIdentifier(
    val sectionId: String,
    val type: WebViewZoomable.NativeSectionType,
    val viewAndTouchListeners: List<ViewAndTouchListener>
)

data class ViewAndTouchListener(
    @IdRes val viewId: Int,
    val touchListener: View.OnTouchListener
)
