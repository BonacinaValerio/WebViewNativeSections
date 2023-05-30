# WebViewNativeSections - Official Documentation

## Introduction
WebViewNativeSections is a custom Android library that provides an advanced WebView with native header/footer and section management capabilities. This library allows you to load and organize multiple HTML pages within a WebView, with the ability to add custom headers and footers for each section. Additionally, you can use global headers and footers that apply to the entire WebView. All header and footer components are native Android views and are not part of the HTML content.

> Note: The WebView provided by the library is capable of efficiently
> handling the zoom feature, even when there are injected native
> components such as headers and footers. This capability extends even
> when the WebView is nested within a ViewPager2. This means that you
> can zoom in and out of the web page, including the elements added
> through native injection, while maintaining high performance even when
> navigating between different pages within the ViewPager2. This feature
> allows users to explore HTML content conveniently and intuitively,
> without compromising the zoom experience even when the WebView is
> integrated into a complex interface like a ViewPager2.

## System Requirements
- Android SDK 21+ (Android 5.0 Lollipop or later).

## Installation

To use the WebViewNativeSections library in your Android project, follow the steps below:

1. Add the following repository in the `build.gradle` file of your project:

```groovy
repositories {
    mavenCentral()
}
```

2. Add the dependency in the `build.gradle` file of your app module:

```groovy
dependencies {
    implementation 'it.bonacina:webviewnativesections:1.0.0'
}
```

3. Sync your Android project with the changes made to the `build.gradle` file.

Now, you have successfully added the WebViewNativeSections library to your project and can start using its features.

## Using WebViewNativeSections

### Adding WebViewNativeSections to the User Interface
To use WebViewNativeSections in your user interface, add the `WebViewNativeSections` element to your desired XML layout:

```xml
<it.bonacina.webviewnativesections.WebViewNativeSections
    android:id="@+id/webViewNativeSections"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### Managing Global Header and Footer
WebViewNativeSections allows you to add global headers and footers that apply to the entire WebView. These components are native Android views and are not part of the HTML content.
You can declare the layout resources for the global header and footer directly in XML using the following attributes:

- `app:global_header_view`: Specifies the layout resource for the global header.
- `app:global_footer_view`: Specifies the layout resource for the global footer.

For example, in your XML layout file, you can define the WebViewNativeSections element with the global header and footer layouts as follows:

```xml
<it.bonacina.webviewnativesections.WebViewNativeSections
    android:id="@+id/webViewNativeSections"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:global_header_view="@layout/global_header"
    app:global_footer_view="@layout/global_footer" />
```

By declaring the global header and footer layouts in XML, WebViewNativeSections automatically inflates and adds them to the WebView. This simplifies the process of setting up the global header and footer views.

In addition to declaring the layouts in XML, you can also programmatically interact with the global header and footer views using the following methods:

#### setGlobalHeader(@LayoutRes viewId: Int)
Sets the global header for the entire WebView using the specified layout. For example:

```kotlin
webViewNativeSections.setGlobalHeader(R.layout.global_header)
```

#### getGlobalHeaderView(): WebViewInjectedView?
Retrieves the global header as a `WebViewInjectedView` object. This method returns `null` if the global header has not been set. For example:

```kotlin
val globalHeaderView = webViewNativeSections.getGlobalHeaderView()
if (globalHeaderView != null) {
    // Perform desired operations with the global header
}
```

#### setGlobalFooter(@LayoutRes viewId: Int)
Sets the global footer for the entire WebView using the specified layout. For example:

```kotlin
webViewNativeSections.setGlobalFooter(R.layout.global_footer)
```

#### getGlobalFooterView(): WebViewInjectedView?
Retrieves the global footer as a `WebViewInjectedView` object. This method returns `null` if the global footer has not been set. For example:

```kotlin
val globalFooterView = webViewNativeSections.getGlobalFooterView()
if (globalFooterView != null) {
    // Perform desired operations with the global footer
}
```

### Managing Section Headers and Footers
For each section added to the WebView, you can specify a layout for the header and footer. These components are native Android views and are not part of the HTML content of the section. The following methods are provided to manage section headers and footers:

#### getHeaderView(sectionId: String): WebViewInjectedView?
Retrieves the header of a specific section using the section ID. This method returns `null` if the header has not been set for the corresponding section. For example:

```kotlin
val sectionId = "section1"
val headerView = webViewNativeSections.getHeaderView(sectionId)
if (headerView != null) {
    // Perform desired operations with the section header
}
```

#### getFooterView(sectionId: String): WebViewInjectedView?
Retrieves the footer of a specific section using the section ID. This method returns `null` if the footer has not been set for the corresponding section. For example:

```kotlin
val sectionId = "section1"
val footerView = webViewNativeSections.getFooterView(sectionId)
if (footerView != null) {
    // Perform desired operations with the section footer
}
```
### WebViewInjectedView Class

The `WebViewInjectedView` class is a container class that represents a native view injected into the WebView through the WebViewNativeSections library. This class provides the following methods:

#### getInternalView(): View
This method returns the original view injected into the WebView. You can use this method to obtain a reference to the view and perform operations on it. For example:
```kotlin
val headerView = webViewNativeSections.getHeaderView(sectionId)
val internalView = headerView?.getInternalView()
if (internalView != null) {
    // Perform desired operations on the internal view
}
```
#### calculateCorrectHeaderHeight(onDone: () -> Unit = {})
This method is used to calculate the correct header height. It should be called whenever an internal modification is made to the user interface during runtime. You can provide an optional `onDone` callback to perform operations when the height calculation is completed. For example:

```kotlin
val headerView = webViewNativeSections.getHeaderView(sectionId)
val internalView = headerView?.getInternalView()
if (internalView != null) {
    // Perform desired operations on the internal view
	val mHeaderView = WebviewHeaderBinding.bind(internalView)
    mHeaderView.label1.text = "runtime change of the label1"
    headerView?.calculateCorrectHeaderHeight {
	    // Perform operations when the header height calculation is completed
	}
}
```

The `WebViewInjectedView` class provides these helpful methods to interact with native views inside the WebViewNativeSections. You can use these methods to further customize the appearance and behavior of the native views within the WebView.

Note: Make sure to call the `calculateCorrectHeaderHeight` method whenever an internal modification is made to the user interface to ensure proper header sizing.

### Adding Internal Touch Listeners
If the headers or footers contain components that can interact with touch events, you can add an internal touch listener to handle the events. The `DefaultWebViewTouchListener` class provided by the library can be used to handle simple clickable objects like buttons. Here's an example of adding an internal touch listener:

```kotlin
val sectionId = "section1"
val type = NativeSectionType.HEADER
val viewsAndListeners = listOf(
    ViewAndTouchListener(
        viewId = R.id.button1,
        touchListener = DefaultWebViewTouchListener()
    ),
    ViewAndTouchListener(
        viewId = R.id.button2,
        touchListener = DefaultWebViewTouchListener()
    )
)
webViewNativeSections.addInternalViewTouchListener(sectionId, type, viewsAndListeners)
```
Here's the updated "Loading and Displaying HTML Content" section, including the explanation of the `WebViewSection` object and its additional properties:

### Loading and Displaying HTML Content

To load and display HTML content in WebViewNativeSections, you can use the `WebViewSection` object. The `WebViewSection` class represents a section of HTML content to be loaded and displayed in the WebView. It has the following properties:

```kotlin
data class WebViewSection(
    val htmlText: String,
    val url: String? = null,
    @LayoutRes val headerLayoutId: Int? = null,
    @LayoutRes val footerLayoutId: Int? = null,
    var visibility: SectionVisibility = SectionVisibility.VISIBLE
) {
    val id = UUID.randomUUID().toString()
}
```

- `htmlText`: The HTML content to be loaded in the WebView.
- `url` (optional): The base URL for the section, which helps load any additional resources if needed.
- `headerLayoutId` (optional): The layout resource ID for the header view associated with the section.
- `footerLayoutId` (optional): The layout resource ID for the footer view associated with the section.
- `visibility`: The initial visibility state of the section. It is set to `SectionVisibility.VISIBLE` by default.

The `WebViewSection` object allows you to define different sections of HTML content with their respective header and footer layouts. You can customize the appearance and behavior of each section by specifying the corresponding layout resources.

To load and display HTML content using the `displayHtmlContent()` method, you can follow this example:

```kotlin
val sections = listOf(
    WebViewSection(
        htmlText = "<html><body><h1>Welcome!</h1></body></html>",
        url = "https://example.com",
        headerLayoutId = R.layout.custom_header,
        footerLayoutId = R.layout.custom_footer,
        visibility = SectionVisibility.VISIBLE
    ),
    // Add more WebViewSection objects for additional sections if needed
)

webViewNativeSections.displayHtmlContent(
    webViewClient = WebViewNativeSectionsClient(),
    sections = sections,
    listener = object : WebViewListener {
        override fun onPageLoaded() {
            // Logic to be executed after the HTML pages have finished loading
        }
    }
)
```

In the above example, we create a list of `WebViewSection` objects, where each object represents a section with its HTML content, base URL, custom header and footer layouts, and initial visibility state. You can customize the properties of each section according to your requirements.

Make sure you have defined the necessary custom header and footer layouts in your XML layout files (e.g., `R.layout.custom_header` and `R.layout.custom_footer`).

### Managing Content Visibility
You can manage the visibility of the content in a specific section using the following methods:

#### setContentVisibility(sectionId: String, show: Boolean, onChangeContentVisibility: (Boolean) -> Unit = {})
Sets the visibility of the content in a specific section using the section ID and the boolean value `show`. You can provide an optional `onChangeContentVisibility` callback to perform operations when the content visibility changes. For example:

```kotlin
val sectionId = "section1"
val showSection = true

webViewNativeSections.setContentVisibility(sectionId, showSection) { isVisible ->
    // Logic to be executed when the visibility of the section content changes
}
```

#### toggleHideContent(sectionId: String, onChangeContentVisibility: (Boolean) -> Unit = {})
Hides or shows the content of a specific section using the section ID. You can provide an optional `onChangeContentVisibility` callback to perform operations when the content visibility changes. For example:

```kotlin
val sectionId = "section1"

webViewNativeSections.toggleHideContent(sectionId) { isVisible ->
    // Logic to be executed when the visibility of the section content changes
}
```

## Conclusion
The WebViewNativeSections library offers advanced features for managing sections, headers, and footers in Android WebViews. By using the methods and examples described in this documentation, you can easily integrate WebViewNativeSections into your project and customize the appearance and behavior of the WebView according to your needs. We hope this library is useful to you and simplifies the development of your Android applications.