package it.bonacina.appwebview.ui.webviewfragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.bonacina.appwebview.R
import it.bonacina.webviewzoomable.domain.SectionVisibility
import it.bonacina.webviewzoomable.domain.WebViewSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber

class WebViewZoomViewModel: ViewModel() {

    private val _webViewHtml = MutableLiveData<List<WebViewSection>>()
    val webViewHtml: LiveData<List<WebViewSection>> = _webViewHtml

    fun getHtmlFromUrl(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (webViewHtml.value == null) {
                try {
                    val doc: Document = Jsoup.connect(url).get()

                    val head = doc.head()
                    val metaViewports = head.select("meta[name=viewport]")
                    if (!metaViewports.isEmpty()) {
                        for (viewPort in metaViewports) {
                            viewPort.remove()
                        }
                    }
                    head.append("<meta name=\"viewport\" content=\"width=device-width\"/>")

                    _webViewHtml.postValue(
                        listOf(
                            WebViewSection(
                                """
                                <html>
                                LALAALALALALALALALALALA
                                </html>
                            """.trimIndent(),
                                headerLayoutId = R.layout.webview_header,
                                visibility = SectionVisibility.GONE
                            ),
                            /*
                        WebViewSection(
                            doc.outerHtml(),
                            headerLayoutId = R.layout.webview_header,
                            visibility = SectionVisibility.GONE
                        ),

                         */
                            WebViewSection(
                                """
                                <html>
                                ULTIMA SEZIONE
                                <a href="http://www.google.com">test</a>
                                </html>
                            """.trimIndent(),
                                headerLayoutId = R.layout.webview_header
                            )
                        )
                    )
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }
}