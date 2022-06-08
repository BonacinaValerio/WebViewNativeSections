package it.bonacina.appwebview.ui.webviewfragment

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.LinearLayout
import androidx.lifecycle.ViewModelProvider
import it.bonacina.appwebview.R
import it.bonacina.appwebview.databinding.FragmentWebViewZoomBinding
import it.bonacina.appwebview.databinding.WebviewHeaderBinding
import it.bonacina.appwebview.ui.webview.DefaultWebViewTouchListener
import it.bonacina.appwebview.ui.webview.WebViewZoomableClient
import org.jsoup.Jsoup

class WebViewZoomFragment : Fragment() {
    private var currentUrl: String? = null
    private lateinit var viewModel: WebViewZoomViewModel

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
        val binding = FragmentWebViewZoomBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[WebViewZoomViewModel::class.java]

        currentUrl?.let { url ->
            viewModel.getHtmlFromUrl(url)

            viewModel.webViewHtml.observe(viewLifecycleOwner) { html ->
                binding.myWebviewZoomable.displayHtmlContent(
                    WebViewZoomableClient(),
                    url,
                    html,
                )
            }
        }

        binding.myWebviewZoomable.addInternalViewTouchListener(
            listOf(R.id.button),
            DefaultWebViewTouchListener()
        )

        binding.myWebviewZoomable.getHeaderInternalView()?.let { view ->
            val mHeader = WebviewHeaderBinding.bind(view)
            mHeader.button.setOnClickListener {
                binding.myWebviewZoomable.toggleHideContent {

                }
            }
        }

        return binding.root
    }

    companion object {

        private const val ARG_URL = "ARG_URL"

        fun newInstance(url: String) =
            WebViewZoomFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_URL, url)
                }
            }
    }
}