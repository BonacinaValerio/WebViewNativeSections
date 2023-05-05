package it.bonacina.webviewzoomable.view.webview

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

open class WebViewZoomableClient : WebViewClient() {

    private var pageListener: PageListener? = null

    fun setOnPageListener(onPageListener: PageListener) {
        this.pageListener = onPageListener
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        pageListener?.onPageEvent(PageEvent.ON_PAGE_LOADED)
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        super.onPageCommitVisible(view, url)
        pageListener?.onPageEvent(PageEvent.ON_PAGE_LOADED)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        pageListener?.onPageEvent(PageEvent.ON_REDIRECT, request?.url?.toString())
        return true
    }
}