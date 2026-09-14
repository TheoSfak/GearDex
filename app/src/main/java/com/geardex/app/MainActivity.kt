package com.geardex.app

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.geardex.app.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var prefs: SharedPreferences

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        prefs.edit { putBoolean(PREF_NOTIFICATION_PERMISSION_PROMPTED, true) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Edge-to-edge: the toolbar draws behind the status bar and the dock above the
        // navigation bar, with both kept clear of display cutouts in landscape. Base
        // padding and margins are captured once so repeated inset passes don't compound.
        val toolbarPaddingLeft = binding.toolbar.paddingLeft
        val toolbarPaddingTop = binding.toolbar.paddingTop
        val toolbarPaddingRight = binding.toolbar.paddingRight
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(
                left = toolbarPaddingLeft + bars.left,
                top = toolbarPaddingTop + bars.top,
                right = toolbarPaddingRight + bars.right
            )
            insets
        }

        val dockMargin = (binding.bottomNav.layoutParams as ViewGroup.MarginLayoutParams).leftMargin
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = dockMargin + bars.left
                rightMargin = dockMargin + bars.right
                bottomMargin = bars.bottom + resources.getDimensionPixelSize(R.dimen.spacing_sm)
            }
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setSupportActionBar(binding.toolbar)

        val topLevelIds = setOf(
            R.id.garageFragment,
            R.id.logsFragment,
            R.id.gloveboxFragment,
            R.id.parkingFragment,
            R.id.ekdromesFragment
        )
        val appBarConfig = AppBarConfiguration(topLevelIds)
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfig)

        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility = if (destination.id in topLevelIds) View.VISIBLE else View.GONE
            binding.toolbar.title = ""
            binding.toolbar.subtitle = null
            binding.headerBrand.translationX = if (destination.id in topLevelIds) {
                0f
            } else {
                resources.getDimensionPixelSize(R.dimen.spacing_xxl).toFloat()
            }
        }

        requestNotificationPermissionIfNeeded()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                navController.navigate(R.id.action_global_settings)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        val alreadyPrompted = prefs.getBoolean(PREF_NOTIFICATION_PERMISSION_PROMPTED, false)
        if (!alreadyGranted && !alreadyPrompted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        private const val PREF_NOTIFICATION_PERMISSION_PROMPTED = "notification_permission_prompted"
    }
}
