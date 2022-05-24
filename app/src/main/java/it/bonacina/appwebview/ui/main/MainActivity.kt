package it.bonacina.appwebview.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import it.bonacina.appwebview.databinding.MainActivityBinding

class MainActivity : AppCompatActivity() {

    private var _binding: MainActivityBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val webViewUrls = listOf(
            "https://github.com/BonacinaValerio",
            "https://github.com/BonacinaValerio",
            "https://github.com/BonacinaValerio"
        )

        if (binding.viewpager.adapter == null) {
            binding.viewpager.adapter =
                PagerAdapter(supportFragmentManager, lifecycle, webViewUrls)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}