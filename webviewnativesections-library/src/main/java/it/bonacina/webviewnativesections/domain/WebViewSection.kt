package it.bonacina.webviewnativesections.domain

import androidx.annotation.LayoutRes
import java.util.*

/**
 * Represents a section of HTML content to be loaded and displayed in the WebView.
 *
 * @param htmlText The HTML content to be loaded in the WebView.
 * @param url (Optional) The base URL for the section, which helps load any additional resources if needed.
 * @param headerLayoutId (Optional) The layout resource ID for the header view associated with the section.
 * @param footerLayoutId (Optional) The layout resource ID for the footer view associated with the section.
 * @param visibility The initial visibility state of the section.
 */
data class WebViewSection(
    val htmlText: String,
    val url: String? = null,
    @LayoutRes val headerLayoutId: Int? = null,
    @LayoutRes val footerLayoutId: Int? = null,
    var visibility: SectionVisibility = SectionVisibility.VISIBLE
) {
    val id = UUID.randomUUID().toString()
}

enum class SectionVisibility {
    VISIBLE,
    GONE
}