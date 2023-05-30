package it.bonacina.appwebview.ui.webviewfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import it.bonacina.appwebview.R
import it.bonacina.appwebview.databinding.FragmentWebViewBinding
import it.bonacina.appwebview.databinding.WebviewHeaderBinding
import it.bonacina.webviewnativesections.domain.NativeSectionType
import it.bonacina.webviewnativesections.domain.SectionVisibility
import it.bonacina.webviewnativesections.domain.ViewAndTouchListener
import it.bonacina.webviewnativesections.domain.WebViewSection
import it.bonacina.webviewnativesections.view.WebViewInjectedView
import it.bonacina.webviewnativesections.view.webview.DefaultWebViewTouchListener
import it.bonacina.webviewnativesections.view.webview.WebViewListener
import it.bonacina.webviewnativesections.view.webview.WebViewNativeSectionsClient

class WebViewNativeSectionsFragment : Fragment() {
    private var _binding: FragmentWebViewBinding? = null
    private val binding get() = _binding!!

    private var onBackPressedCallback: OnBackPressedCallback? = null
    private var currentUrl: String? = null
    private lateinit var viewModel: WebViewNativeSectionsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentUrl = it.getString(ARG_URL)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentWebViewBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[WebViewNativeSectionsViewModel::class.java]

        currentUrl.let { url ->
            viewModel.getHtmlFromUrl(url)

            viewModel.webViewHtml.observe(viewLifecycleOwner) { webViewSections ->
                binding.myWebviewNativeSections.displayHtmlContent(
                    WebViewNativeSectionsClient(),
                    webViewSections,
                    object : WebViewListener {
                        override fun onPageLoaded() {

                        }
                    }
                )
                setupWebView(binding, webViewSections)
            }
        }

        return binding.root
    }

    private fun setupWebView(binding: FragmentWebViewBinding, webViewSections: List<WebViewSection>) {
        binding.myWebviewNativeSections.addInternalViewTouchListener(
            null,
            NativeSectionType.HEADER,
            listOf(
                ViewAndTouchListener(
                    R.id.btn_favorite,
                    DefaultWebViewTouchListener()
                )
            )
        )
        webViewSections.forEach { section ->
            binding.myWebviewNativeSections.addInternalViewTouchListener(
                section.id,
                NativeSectionType.HEADER,
                listOf(
                    ViewAndTouchListener(
                        R.id.message_container,
                        DefaultWebViewTouchListener()
                    ),
                    ViewAndTouchListener(
                        R.id.recipient_box,
                        DefaultWebViewTouchListener()
                    ),
                    ViewAndTouchListener(
                        R.id.all_mail,
                        DefaultWebViewTouchListener()
                    )
                )
            )

            binding.myWebviewNativeSections.getHeaderView(section.id)?.let { mHeader ->
                val mHeaderView = WebviewHeaderBinding.bind(mHeader.getInternalView())

                if (section.visibility == SectionVisibility.GONE) {
                    collapseHeader(mHeaderView, mHeader)
                } else {
                    expandHeader(mHeaderView, mHeader)
                }
                mHeaderView.messageContainer.setOnClickListener {
                    if (mHeaderView.snippet.visibility == View.VISIBLE) {
                        expandHeader(mHeaderView, mHeader) {
                            binding.myWebviewNativeSections.setContentVisibility(mHeader.sectionId, true)
                        }
                    } else {
                        collapseHeader(mHeaderView, mHeader) {
                            binding.myWebviewNativeSections.setContentVisibility(mHeader.sectionId, false)
                        }
                    }
                }
                mHeaderView.allMail.setOnClickListener {
                    toggleRecipientBox(mHeaderView, mHeader)
                }
                mHeaderView.recipientBox.setOnClickListener {
                    toggleRecipientBox(mHeaderView, mHeader)
                }
            }
        }
    }

    private fun expandHeader(
        mHeaderView: WebviewHeaderBinding,
        mHeader: WebViewInjectedView,
        onWebViewPaddingUpdated: () -> Unit = {}
    ) {
        mHeaderView.allMailBox.visibility = View.VISIBLE
        mHeaderView.recipientBox.visibility = View.VISIBLE
        mHeaderView.btnReply.visibility = View.VISIBLE
        mHeaderView.btnOther.visibility = View.VISIBLE
        mHeaderView.snippet.visibility = View.GONE
        mHeader.calculateCorrectHeaderHeight(onWebViewPaddingUpdated)
    }

    private fun collapseHeader(
        mHeaderView: WebviewHeaderBinding,
        mHeader: WebViewInjectedView,
        onWebViewPaddingUpdated: () -> Unit = {}
    ) {
        mHeaderView.allMailBox.visibility = View.GONE
        mHeaderView.recipientBox.visibility = View.INVISIBLE
        mHeaderView.btnReply.visibility = View.GONE
        mHeaderView.btnOther.visibility = View.GONE
        mHeaderView.snippet.visibility = View.VISIBLE
        mHeader.calculateCorrectHeaderHeight(onWebViewPaddingUpdated)
    }

    private fun toggleRecipientBox(
        mHeaderView: WebviewHeaderBinding,
        mHeader: WebViewInjectedView
    ) {
        if (mHeaderView.allMail.visibility == View.VISIBLE) {
            mHeaderView.allMail.visibility = View.GONE
            mHeaderView.recipientArrow.rotation = 0f
        } else {
            mHeaderView.allMail.visibility = View.VISIBLE
            mHeaderView.recipientArrow.rotation = 180f
        }
        mHeader.calculateCorrectHeaderHeight()
    }

    override fun onResume() {
        super.onResume()
        onBackPressedCallback?.remove()
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.myWebviewNativeSections.canGoBack()) {
                    binding.myWebviewNativeSections.goBack()
                } else {
                    requireActivity().finish()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
        onBackPressedCallback = callback
    }

    override fun onPause() {
        super.onPause()
        onBackPressedCallback?.remove()
        onBackPressedCallback = null
    }

    companion object {

        private const val ARG_URL = "ARG_URL"

        fun newInstance(url: String?) =
            WebViewNativeSectionsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_URL, url)
                }
            }
    }
}