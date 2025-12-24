package com.example.wellnesstrack.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.wellnesstrack.R
import com.example.wellnesstrack.data.Prefs
import com.example.wellnesstrack.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = Prefs(requireContext())
        // Toolbar back arrow navigates up
        view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)?.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val name = prefs.getString("profile_name", "")
        val email = prefs.getString("profile_email", "")
        val age = prefs.getIntSafe("profile_age", 0)
        val height = prefs.getIntSafe("profile_height", 0)
        val weight = prefs.getIntSafe("profile_weight", 0)
        val avatar = prefs.getString("profile_avatar_uri", "")

        binding.tvName.text = if (name.isNotBlank()) name else "—"
        binding.tvEmail.text = if (email.isNotBlank()) email else "—"
        binding.tvAge.text = if (age > 0) "$age" else "—"
        binding.tvHeight.text = if (height > 0) "$height cm" else "—"
        binding.tvWeight.text = if (weight > 0) "$weight kg" else "—"
        if (avatar.isNotBlank()) {
            try {
                binding.root.findViewById<android.widget.ImageView>(R.id.ivAvatar)?.setImageURI(Uri.parse(avatar))
                // Also update collapsing/header image if present in layout
                binding.root.findViewById<android.widget.ImageView>(R.id.headerImage)?.setImageURI(Uri.parse(avatar))
            } catch (_: Exception) {}
        } else {
            // Fallback: ensure header uses default when no avatar is set
            binding.root.findViewById<android.widget.ImageView>(R.id.headerImage)?.setImageResource(R.drawable.profilepoto)
        }

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.editProfileFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
