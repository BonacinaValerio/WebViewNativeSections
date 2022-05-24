package it.bonacina.appwebview.ui.webview

interface ZoomStatusListener {
    fun onZoomStatusChanged(oldScale: Float, newScale: Float);
}
