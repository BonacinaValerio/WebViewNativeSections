/*
 * Copyright 2023 Valerio Bonacina. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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