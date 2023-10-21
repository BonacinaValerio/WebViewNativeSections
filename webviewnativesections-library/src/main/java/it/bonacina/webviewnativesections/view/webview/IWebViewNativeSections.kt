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

import androidx.annotation.LayoutRes
import it.bonacina.webviewnativesections.domain.NativeSectionType
import it.bonacina.webviewnativesections.domain.ViewAndTouchListener
import it.bonacina.webviewnativesections.domain.WebViewSection
import it.bonacina.webviewnativesections.view.WebViewInjectedView

interface IWebViewNativeSections {
    /**
     * Sets the layout resource ID for the global header view to be injected into the WebView.
     *
     * @param viewId The layout resource ID for the global header view.
     */
    fun setGlobalHeader(@LayoutRes viewId: Int)

    /**
     * Returns the global header view that has been set for the WebView.
     *
     * @return [WebViewInjectedView] The global header view, or null if no header view has been set.
     */
    fun getGlobalHeaderView(): WebViewInjectedView?

    /**
     * Sets the layout resource ID for the global footer view to be injected into the WebView.
     *
     * @param viewId The layout resource ID for the global footer view.
     */
    fun setGlobalFooter(@LayoutRes viewId: Int)

    /**
     * Returns the global footer view that has been set for the WebView.
     *
     * @return [WebViewInjectedView] The global footer view, or null if no footer view has been set.
     */
    fun getGlobalFooterView(): WebViewInjectedView?

    /**
     * Returns the footer view associated with the specified section ID.
     *
     * @param sectionId The ID of the section.
     * @return [WebViewInjectedView] The footer view associated with the section ID, or null if not found.
     */
    fun getFooterView(sectionId: String): WebViewInjectedView?

    /**
     * Returns the header view associated with the specified section ID.
     *
     * @param sectionId The ID of the section.
     * @return [WebViewInjectedView] The header view associated with the section ID, or null if not found.
     */
    fun getHeaderView(sectionId: String): WebViewInjectedView?

    /**
     * Sets the touch slop for handling touch events.
     *
     * @param slop The touch slop value.
     */
    fun setTouchSlop(slop: Int)

    /**
     * Displays HTML content in the WebView with native sections.
     *
     * @param webViewClient The [WebViewNativeSectionsClient] for handling WebView events.
     * @param sections The list of [WebViewSection] objects representing the HTML content to be displayed.
     * @param listener Optional [WebViewListener] for WebView events.
     */
    fun displayHtmlContent(
        webViewClient: WebViewNativeSectionsClient,
        sections: List<WebViewSection>,
        listener: WebViewListener? = null
    )

    /**
     * Adds an additional section to the WebView.
     *
     * @param section The [WebViewSection] to be added.
     */
    fun addAdditionalSection(
        section: WebViewSection
    )

    /**
     * Adds internal view touch listeners to a specific section in the WebView.
     *
     * @param sectionId The ID of the section or null for global sections.
     * @param type The type of the native section.
     * @param viewsAndListeners The list of [ViewAndTouchListener] representing the views and touch listeners.
     */
    fun addInternalViewTouchListener(sectionId: String?, type: NativeSectionType, viewsAndListeners: List<ViewAndTouchListener>)

    /**
     * Sets the visibility of the content within a specific section in the WebView.
     *
     * @param sectionId The ID of the section.
     * @param show True to show the content, false to hide it.
     * @param onChangeContentVisibility Callback function to be invoked when the content visibility changes.
     */
    fun setContentVisibility(sectionId: String, show: Boolean, onChangeContentVisibility: (Boolean) -> Unit = {})

    /**
     * Toggles the visibility of the content within a specific section in the WebView.
     *
     * @param sectionId The ID of the section.
     * @param onChangeContentVisibility Callback function to be invoked when the content visibility changes.
     */
    fun toggleHideContent(sectionId: String, onChangeContentVisibility: (Boolean) -> Unit = {})
}