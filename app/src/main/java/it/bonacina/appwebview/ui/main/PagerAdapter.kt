package it.bonacina.appwebview.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import it.bonacina.appwebview.ui.webviewfragment.WebViewZoomFragment
import java.lang.ref.WeakReference

class PagerAdapter(
    fm: FragmentManager,
    lc: Lifecycle,
    var urls: List<String>
) : FragmentStateAdapter(fm, lc) {

    private val fragmentCache = mutableMapOf<Int, WeakReference<WebViewZoomFragment>>()

    override fun getItemCount(): Int {
        return urls.size
    }

    override fun createFragment(position: Int): Fragment {
        val fragment = WebViewZoomFragment.newInstance(urls[position])
        fragmentCache[position] = WeakReference(fragment)
        return fragment
    }

    fun getMessageViewFragment(position: Int): WebViewZoomFragment? {
        return fragmentCache[position]?.get()
    }
}
