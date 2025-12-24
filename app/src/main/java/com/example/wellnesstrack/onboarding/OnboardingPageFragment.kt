package com.example.wellnesstrack.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.wellnesstrack.databinding.ItemOnboardingPageBinding

class OnboardingPageFragment : Fragment() {
    private var _b: ItemOnboardingPageBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _b = ItemOnboardingPageBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val position = requireArguments().getInt("position")
        val img = requireArguments().getInt("img")
        val title = requireArguments().getString("title")
        val subtitle = requireArguments().getString("subtitle")
        // Hero mode for all onboarding pages (full-bleed background)
        if (position in 0..2) {
            b.bgImage.visibility = View.VISIBLE
            b.vGradient.visibility = View.VISIBLE
            b.tvBrand.visibility = View.VISIBLE
            b.centerContainer.visibility = View.GONE
            b.bottomTexts.visibility = View.VISIBLE

            b.bgImage.setImageResource(img)
            b.tvTitleHero.text = title
            b.tvSubtitleHero.text = subtitle
        } else {
            // Fallback (not used with current 3 pages)
            b.centerContainer.visibility = View.VISIBLE
            b.bgImage.visibility = View.GONE
            b.vGradient.visibility = View.GONE
            b.tvBrand.visibility = View.GONE
            b.bottomTexts.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }

    companion object {
        fun newInstance(position: Int, img: Int, title: String, subtitle: String): OnboardingPageFragment {
            val f = OnboardingPageFragment()
            f.arguments = Bundle().apply {
                putInt("position", position)
                putInt("img", img)
                putString("title", title)
                putString("subtitle", subtitle)
            }
            return f
        }
    }
}
