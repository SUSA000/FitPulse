package com.example.wellnesstrack.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.wellnesstrack.data.Prefs
import com.example.wellnesstrack.databinding.FragmentEditProfileBinding

class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private var pickedAvatarUri: Uri? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = Prefs(requireContext())
        // Back arrow
        binding.root.findViewById<com.google.android.material.appbar.MaterialToolbar>(com.example.wellnesstrack.R.id.toolbar)?.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.etName.setText(prefs.getString("profile_name", ""))
        binding.etEmail.setText(prefs.getString("profile_email", ""))
        val age = prefs.getIntSafe("profile_age", 0)
        val height = prefs.getIntSafe("profile_height", 0)
        val weight = prefs.getIntSafe("profile_weight", 0)
        val avatar = prefs.getString("profile_avatar_uri", "")
        if (avatar.isNotBlank()) {
            try { binding.ivAvatarPreview.setImageURI(Uri.parse(avatar)) } catch (_: Exception) {}
        }
        if (age > 0) binding.etAge.setText(age.toString())
        if (height > 0) binding.etHeight.setText(height.toString())
        if (weight > 0) binding.etWeight.setText(weight.toString())

        binding.btnPickPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivityForResult(intent, 2001)
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text?.toString()?.trim().orEmpty()
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val ageVal = binding.etAge.text?.toString()?.toIntOrNull() ?: 0
            val heightVal = binding.etHeight.text?.toString()?.toIntOrNull() ?: 0
            val weightVal = binding.etWeight.text?.toString()?.toIntOrNull() ?: 0

            prefs.putString("profile_name", name)
            prefs.putString("profile_email", email)
            prefs.putIntSafe("profile_age", ageVal)
            prefs.putIntSafe("profile_height", heightVal)
            prefs.putIntSafe("profile_weight", weightVal)
            pickedAvatarUri?.let { prefs.putString("profile_avatar_uri", it.toString()) }

            Toast.makeText(requireContext(), "Profile saved", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2001 && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.data
            if (uri != null) {
                // Persist read permission so we can load after restarts
                try {
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
                pickedAvatarUri = uri
                try { binding.ivAvatarPreview.setImageURI(uri) } catch (_: Exception) {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
