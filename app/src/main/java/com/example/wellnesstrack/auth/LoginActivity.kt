package com.example.wellnesstrack.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wellnesstrack.MainActivity
import com.example.wellnesstrack.data.Prefs
import com.example.wellnesstrack.databinding.ActivityLoginBinding
import org.json.JSONArray
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs(this)

        // Always show login on app open as requested (no auto-skip to Main)

        // Tab toggles (chips)
        binding.btnTabLogin.setOnClickListener {
            showLogin()
        }
        binding.btnTabSignup.setOnClickListener {
            showSignup()
        }

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.btnSignup.setOnClickListener { attemptSignup() }

        binding.tvSwitch.setOnClickListener {
            // toggle to signup
            showSignup()
            binding.btnTabSignup.isChecked = true
        }

        showLogin()
    }

    private fun showLogin() {
        binding.loginForm.visibility = View.VISIBLE
        binding.signupForm.visibility = View.GONE
    }

    private fun showSignup() {
        binding.loginForm.visibility = View.GONE
        binding.signupForm.visibility = View.VISIBLE
    }

    private fun attemptLogin() {
        val id = binding.etLoginIdentifier.text.toString().trim()
        val pw = binding.etLoginPassword.text.toString()
        // reset errors
        binding.etLoginIdentifier.error = null
        binding.etLoginPassword.error = null
        if (id.isEmpty()) { binding.etLoginIdentifier.error = "Required"; return }
        if (pw.isEmpty()) { binding.etLoginPassword.error = "Required"; return }
        val user = prefs.findUserByIdentifier(id)
        if (user == null) { Toast.makeText(this, "User not found. Please sign up.", Toast.LENGTH_SHORT).show(); return }
        if (user.password != pw) { Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show(); return }
        prefs.setLoggedIn(user.username)
        startMainAndFinish()
    }

    private fun attemptSignup() {
        val username = binding.etSignupUsername.text.toString().trim()
        val email = binding.etSignupEmail.text.toString().trim()
        val pw = binding.etSignupPassword.text.toString()
        val pw2 = binding.etSignupConfirm.text.toString()
        // reset errors
        binding.etSignupUsername.error = null
        binding.etSignupEmail.error = null
        binding.etSignupPassword.error = null
        binding.etSignupConfirm.error = null

        if (username.length < 3) { binding.etSignupUsername.error = "Username must be 3+ characters"; return }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.etSignupEmail.error = "Invalid email format"; return }
        if (pw.length < 6) { binding.etSignupPassword.error = "Password must be at least 6 characters"; return }
        if (pw != pw2) { binding.etSignupConfirm.error = "Passwords do not match"; return }
        val ok = prefs.registerUser(Prefs.User(username, email, pw))
        if (!ok) { Toast.makeText(this, "Email or username already exists", Toast.LENGTH_SHORT).show(); return }
        prefs.setLoggedIn(username)
        startMainAndFinish()
    }

    private fun startMainAndFinish() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
