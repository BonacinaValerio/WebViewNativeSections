package it.bonacina.appwebview.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import it.bonacina.appwebview.ui.webviewfragment.WebViewNativeSectionsFragment

class PagerAdapter(
    fm: FragmentManager,
    lc: Lifecycle,
    var urls: List<String?>
) : FragmentStateAdapter(fm, lc) {

    override fun getItemCount(): Int {
        return urls.size
    }

    override fun createFragment(position: Int): Fragment {
        return WebViewNativeSectionsFragment.newInstance(urls[position])
    }

}
