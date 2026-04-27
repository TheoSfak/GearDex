@file:Suppress("DEPRECATION")

package com.geardex.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
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
        setupExportSection()
        setupUpdateSection()
        setupSupportSection()
        observeViewModel()
    }

    private fun setupLanguageChips() {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val currentLanguage = if (appLocales.isEmpty) {
            Locale.getDefault().language
        } else {
            appLocales.get(0)?.language ?: Locale.getDefault().language
        }
        binding.chipEn.isChecked = currentLanguage != "el" && currentLanguage != "de" && currentLanguage != "fr" && currentLanguage != "it" && currentLanguage != "es"
        binding.chipEl.isChecked = currentLanguage == "el"
        binding.chipDe.isChecked = currentLanguage == "de"
        binding.chipFr.isChecked = currentLanguage == "fr"
        binding.chipIt.isChecked = currentLanguage == "it"
        binding.chipEs.isChecked = currentLanguage == "es"

        binding.chipGroupLanguage.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val localeTag = when (checkedIds[0]) {
                R.id.chip_el -> "el"
                R.id.chip_de -> "de"
                R.id.chip_fr -> "fr"
                R.id.chip_it -> "it"
                R.id.chip_es -> "es"
                else -> "en"
            }
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTag))
        }
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
        binding.btnCheckUpdates.setOnClickListener { viewModel.checkForUpdates() }
    }

    private fun setupSupportSection() {
        binding.btnDonate.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DONATE_URL)))
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.updateState.collect { state -> renderUpdateState(state) }
                }
            }
        }
    }

    private fun renderUpdateState(state: UpdateState) {
        binding.progressUpdate.visibility = when (state) {
            UpdateState.Checking,
            is UpdateState.Downloading -> View.VISIBLE
            else -> View.GONE
        }
        binding.btnCheckUpdates.isEnabled = state !is UpdateState.Checking && state !is UpdateState.Downloading
        binding.btnInstallUpdate.visibility = View.GONE
        binding.btnInstallUpdate.setOnClickListener(null)

        when (state) {
            UpdateState.Idle -> {
                binding.tvUpdateStatus.text = getString(R.string.settings_update_description)
            }
            UpdateState.Checking -> {
                binding.tvUpdateStatus.text = getString(R.string.settings_update_checking)
            }
            is UpdateState.UpToDate -> {
                binding.tvUpdateStatus.text = getString(R.string.settings_update_up_to_date, state.latestVersion)
            }
            is UpdateState.Available -> {
                binding.tvUpdateStatus.text = getString(R.string.settings_update_available, state.release.version)
                binding.btnInstallUpdate.visibility = View.VISIBLE
                binding.btnInstallUpdate.text = getString(R.string.settings_update_download_install)
                binding.btnInstallUpdate.setOnClickListener { viewModel.downloadUpdate(state.release) }
            }
            is UpdateState.Downloading -> {
                binding.tvUpdateStatus.text = getString(R.string.settings_update_downloading)
            }
            is UpdateState.Downloaded -> {
                binding.tvUpdateStatus.text = getString(R.string.settings_update_ready)
                binding.btnInstallUpdate.visibility = View.VISIBLE
                binding.btnInstallUpdate.text = getString(R.string.settings_update_install)
                binding.btnInstallUpdate.setOnClickListener { installUpdate(state.file) }
            }
            is UpdateState.Error -> {
                val message = if (state.message == "No APK found") {
                    getString(R.string.settings_update_no_apk)
                } else {
                    getString(R.string.settings_update_error, state.message)
                }
                binding.tvUpdateStatus.text = message
            }
        }
    }

    private fun installUpdate(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !requireContext().packageManager.canRequestPackageInstalls()) {
            binding.tvUpdateStatus.text = getString(R.string.settings_update_unknown_sources)
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                "package:${requireContext().packageName}".toUri()
            )
            startActivity(intent)
            return
        }

        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val DONATE_URL =
            "https://www.paypal.com/donate/?business=HRKPFT6JPU5P8&no_recurring=0&item_name=Every+donation+helps+turn+coffee+into+code+%E2%80%94+thank+you+for+supporting+the+next+update.&currency_code=EUR"
    }
}
