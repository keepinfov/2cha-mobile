package dev.yaul.twocha

import android.app.Activity
import android.net.VpnService
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import dev.yaul.twocha.ui.fragments.ConfigFragment
import dev.yaul.twocha.ui.fragments.HomeFragment
import dev.yaul.twocha.ui.fragments.LogsFragment
import dev.yaul.twocha.ui.fragments.SettingsFragment
import dev.yaul.twocha.viewmodel.VpnViewModel

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: VpnViewModel by viewModels()

    private val vpnPermissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                startVpnService()
            } else {
                viewModel.onVpnPermissionDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        applyEdgeToEdgeInsets()

        if (savedInstanceState == null) {
            openHome()
        }

        findViewById<BottomNavigationView>(R.id.bottom_navigation).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    openHome()
                    true
                }

                R.id.nav_logs -> {
                    openLogs()
                    true
                }

                R.id.nav_settings -> {
                    openSettings()
                    true
                }

                else -> false
            }
        }
    }

    private fun applyEdgeToEdgeInsets() {
        val container = findViewById<android.view.View>(R.id.fragment_container)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        ViewCompat.setOnApplyWindowInsetsListener(container) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.updatePadding(left = bars.left, right = bars.right, bottom = bars.bottom)
            insets
        }
    }

    fun openConfig() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, ConfigFragment(), ConfigFragment.TAG)
            .addToBackStack(ConfigFragment.TAG)
            .commit()
    }

    private fun openHome() {
        findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.nav_home
        replaceRootFragment(HomeFragment(), HomeFragment.TAG)
    }

    fun openLogs() {
        findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.nav_logs
        replaceRootFragment(LogsFragment(), LogsFragment.TAG)
    }

    fun openSettings() {
        findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.nav_settings
        replaceRootFragment(SettingsFragment(), SettingsFragment.TAG)
    }

    private fun replaceRootFragment(fragment: androidx.fragment.app.Fragment, tag: String) {
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        val existing = supportFragmentManager.findFragmentByTag(tag)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, existing ?: fragment, tag)
            .commit()
    }

    fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        viewModel.connect()
        viewModel.onVpnStarted()
    }
}
