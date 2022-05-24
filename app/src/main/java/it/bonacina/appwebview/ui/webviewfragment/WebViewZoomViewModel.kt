package it.bonacina.appwebview.ui.webviewfragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NotNull
import java.lang.Exception
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber


class WebViewZoomViewModel: ViewModel() {

    private val _webViewHtml = MutableLiveData<String>()
    val webViewHtml: LiveData<String> = _webViewHtml

    fun getHtmlFromUrl(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
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

                _webViewHtml.postValue(doc.outerHtml())
            } catch (e: Exception) {
                Timber.e(e)
            }

        }
    }

    private fun scriptResizing(): String {
        return """
            <script type="text/javascript">
            (function () {
                let viewport = document.querySelector("meta[name=viewport]");
                var maxViewport
                if (viewport.getAttribute('content') == 'width=device-width') {
                  maxViewport = window.screen.width
                } else {
                  maxViewport = viewport.getAttribute('content').split('=')[1]
                }
                var items = document.getElementsByTagName('*');
                for(var i=0;i<items.length;i++) {
                    var item = items[i];

                    var pixels = item.offsetWidth;
                    if (pixels > maxViewport) {
                      maxViewport = pixels
                      viewport.setAttribute('content', 'width='+pixels);
                    }
                    
                    if (item.tagName.toUpperCase() == 'IMG') {
                        const style = getComputedStyle(item)
                        if (style.width == 'none' || style.width == '0px') {
                            if (style.maxWidth == 'none' || style.maxWidth > window.screen.width) {
                                item.style.maxWidth = window.screen.width + "px"
                            }
                        }
                    }
                }
            })();
            </script>
        """
    }
}