package it.bonacina.webviewnativesections.view.webview

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import it.bonacina.webviewnativesections.domain.PageEvent
import it.bonacina.webviewnativesections.domain.PageListener

open class WebViewNativeSectionsClient : WebViewClient() {

    private var pageListener: PageListener? = null

    internal fun setOnPageListener(onPageListener: PageListener) {
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