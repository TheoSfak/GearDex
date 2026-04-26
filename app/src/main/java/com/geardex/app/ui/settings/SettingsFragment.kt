@file:Suppress("DEPRECATION")

package com.geardex.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.geardex.app.BuildConfig
import com.geardex.app.R
import com.geardex.app.databinding.FragmentSettingsBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG = "SettingsFragment"

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.signInWithGoogle(idToken)
            } else {
                Log.w(TAG, "Google sign-in: no ID token")
            }
        } catch (e: ApiException) {
            Log.w(TAG, "Google sign-in failed", e)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLanguageChips()
        setupAuthSection()
        setupExportSection()
        setupUpdateSection()
        setupGoogleApiSection()
        observeViewModel()
    }

    private fun setupLanguageChips() {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val currentLanguage = if (appLocales.isEmpty) {
            Locale.getDefault().language
        } else {
            appLocales.get(0)?.language ?: Locale.getDefault().language
        }
        binding.chipEn.isChecked = currentLanguage != "el"
        binding.chipEl.isChecked = currentLanguage == "el"

        binding.chipGroupLanguage.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val localeTag = when (checkedIds[0]) {
                R.id.chip_el -> "el"
                else -> "en"
            }
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTag))
        }
    }

    private fun setupAuthSection() {
        if (!viewModel.isFirebaseConfigured) {
            binding.tvFirebaseNotConfigured.visibility = View.VISIBLE
            binding.layoutLoggedOut.visibility = View.GONE
            binding.layoutLoggedIn.visibility = View.GONE
            return
        }

        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString() ?: ""
            if (email.isBlank() || password.isBlank()) {
                binding.tvAuthError.visibility = View.VISIBLE
                binding.tvAuthError.text = getString(R.string.error_required_field)
                return@setOnClickListener
            }
            binding.tvAuthError.visibility = View.GONE
            viewModel.signIn(email, password)
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString() ?: ""
            if (email.isBlank() || password.isBlank()) {
                binding.tvAuthError.visibility = View.VISIBLE
                binding.tvAuthError.text = getString(R.string.error_required_field)
                return@setOnClickListener
            }
            binding.tvAuthError.visibility = View.GONE
            viewModel.register(email, password)
        }

        binding.btnGoogleSignIn.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(viewModel.webClientId)
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(requireActivity(), gso)
            googleSignInLauncher.launch(client.signInIntent)
        }

        binding.btnSignOut.setOnClickListener { viewModel.signOut() }
        binding.btnSyncNow.setOnClickListener { viewModel.syncNow() }
    }

    private fun setupExportSection() {
        binding.btnExportAllPdf.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val file = viewModel.exportAllVehiclesPdf()
                if (file != null) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        requireContext(), "${requireContext().packageName}.provider", file
                    )
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(android.content.Intent.createChooser(intent, getString(R.string.pdf_share_title)))
                } else {
                    Snackbar.make(binding.root, getString(R.string.pdf_no_vehicles), Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupUpdateSection() {
        binding.cardGithubUpdates.visibility = if (BuildConfig.ENABLE_UPDATE_CHECK) View.VISIBLE else View.GONE
        binding.tvGithubVersion.text = getString(R.string.settings_update_current_version, BuildConfig.VERSION_NAME)
        binding.btnCheckUpdates.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                "https://github.com/${BuildConfig.GITHUB_REPO}/releases/latest".toUri()
            )
            startActivity(intent)
        }
    }

    private fun setupGoogleApiSection() {
        val savedKey = viewModel.getSavedApiKey()
        if (!savedKey.isNullOrBlank()) {
            binding.etGoogleApiKey.setText(savedKey)
            binding.tvApiKeyStatus.text = getString(R.string.settings_google_api_saved)
            binding.tvApiKeyStatus.setTextColor(resources.getColor(R.color.color_success, null))
            binding.tvApiKeyStatus.visibility = View.VISIBLE
        }

        binding.btnSaveApiKey.setOnClickListener {
            val key = binding.etGoogleApiKey.text?.toString()?.trim() ?: ""
            if (key.isBlank()) {
                viewModel.clearApiKey()
                binding.tvApiKeyStatus.text = getString(R.string.settings_google_api_cleared)
                binding.tvApiKeyStatus.setTextColor(resources.getColor(R.color.text_secondary, null))
                binding.tvApiKeyStatus.visibility = View.VISIBLE
            } else {
                viewModel.saveApiKey(key)
                binding.tvApiKeyStatus.text = getString(R.string.settings_google_api_saved)
                binding.tvApiKeyStatus.setTextColor(resources.getColor(R.color.color_success, null))
                binding.tvApiKeyStatus.visibility = View.VISIBLE
            }
        }

        binding.btnOpenGoogleConsole.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, "https://console.cloud.google.com/apis/credentials".toUri())
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.authState.collect { state -> renderAuthState(state) }
                }
                launch {
                    viewModel.syncMessage.collect { message ->
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun renderAuthState(state: AuthState) {
        if (!viewModel.isFirebaseConfigured) return

        when (state) {
            is AuthState.Loading -> {
                binding.progressAuth.visibility = View.VISIBLE
                binding.layoutLoggedOut.visibility = View.GONE
                binding.layoutLoggedIn.visibility = View.GONE
            }
            is AuthState.LoggedOut -> {
                binding.progressAuth.visibility = View.GONE
                binding.layoutLoggedOut.visibility = View.VISIBLE
                binding.layoutLoggedIn.visibility = View.GONE
            }
            is AuthState.LoggedIn -> {
                binding.progressAuth.visibility = View.GONE
                binding.layoutLoggedOut.visibility = View.GONE
                binding.layoutLoggedIn.visibility = View.VISIBLE
                binding.tvSignedInAs.text = getString(
                    R.string.settings_signed_in_as,
                    state.user.email ?: ""
                )
            }
            is AuthState.Error -> {
                binding.progressAuth.visibility = View.GONE
                binding.layoutLoggedOut.visibility = View.VISIBLE
                binding.layoutLoggedIn.visibility = View.GONE
                binding.tvAuthError.visibility = View.VISIBLE
                binding.tvAuthError.text = state.message
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
