package it.bonacina.appwebview.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import it.bonacina.appwebview.databinding.MainActivityBinding

class MainActivity : AppCompatActivity() {

    private var _binding: MainActivityBinding? = null
    private val binding get() = _binding!!

    private var adapter: PagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val webViewUrls = listOf(
            "https://example.com/",
            null,
            "https://example.com/",
        )

        if (adapter == null) {
            adapter = PagerAdapter(supportFragmentManager, lifecycle, webViewUrls)
            if (binding.viewpager.adapter !== adapter)
                binding.viewpager.adapter = adapter
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter = null
        _binding = null
    }
}