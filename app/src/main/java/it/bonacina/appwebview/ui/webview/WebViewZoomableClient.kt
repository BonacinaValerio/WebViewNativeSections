package it.bonacina.appwebview.ui.webview

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient

open class WebViewZoomableClient : WebViewClient() {

    private var pageListener: PageListener? = null

    fun setOnPageListener(onPageListener: PageListener) {
        this.pageListener = onPageListener
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        pageListener?.onPageEvent(PageEvent.ON_PAGE_STARTED)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        pageListener?.onPageEvent(PageEvent.ON_PAGE_STARTED)
    }
}