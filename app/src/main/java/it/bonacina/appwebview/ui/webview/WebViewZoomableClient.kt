package it.bonacina.appwebview.ui.webview

import android.webkit.WebView
import android.webkit.WebViewClient
import timber.log.Timber

open class WebViewZoomableClient : WebViewClient() {

    private var zoomStatusListener: ZoomStatusListener? = null

    open fun setOnZoomStatusChangeListener(onZoomStatusChangeListener: ZoomStatusListener) {
        this.zoomStatusListener = onZoomStatusChangeListener
    }

    override fun onScaleChanged(
        view: WebView?, oldScale: Float,
        newScale: Float
    ) {
        zoomStatusListener?.onZoomStatusChanged(oldScale, newScale)
        super.onScaleChanged(view, oldScale, newScale)
    }
}