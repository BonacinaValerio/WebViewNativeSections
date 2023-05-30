package it.bonacina.webviewnativesections.domain

internal interface PageListener {
    fun onPageEvent(pageEvent: PageEvent, data: String? = null)
    fun canGoBackOrForward(steps: Int): Boolean
}

enum class PageEvent {
    ON_PAGE_LOADED,
    ON_REDIRECT
}