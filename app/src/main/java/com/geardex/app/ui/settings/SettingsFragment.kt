package com.geardex.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.geardex.app.R
import com.geardex.app.databinding.FragmentSettingsBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

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

        binding.btnSignOut.setOnClickListener { viewModel.signOut() }
        binding.btnSyncNow.setOnClickListener { viewModel.syncNow() }
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
