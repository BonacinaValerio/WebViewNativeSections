package it.bonacina.appwebview.ui.webview

interface PageListener {
    fun onPageEvent(pageEvent: PageEvent)
}

enum class PageEvent {
    ON_PAGE_STARTED,
    ON_PAGE_FINISHED
}