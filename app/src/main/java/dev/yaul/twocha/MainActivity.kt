package dev.yaul.twocha

import android.app.Activity
import android.net.VpnService
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.yaul.twocha.ui.fragments.ConfigFragment
import dev.yaul.twocha.ui.fragments.HomeFragment
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
    }

    private fun applyEdgeToEdgeInsets() {
        val container = findViewById<android.view.View>(R.id.fragment_container)

        ViewCompat.setOnApplyWindowInsetsListener(container) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
    }

    fun openConfig() {
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
            .replace(R.id.fragment_container, ConfigFragment(), ConfigFragment.TAG)
            .addToBackStack(ConfigFragment.TAG)
            .commit()
    }

    private fun openHome() {
        replaceRootFragment(HomeFragment(), HomeFragment.TAG)
    }

    fun openSettings() {
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, SettingsFragment(), SettingsFragment.TAG)
            .addToBackStack(SettingsFragment.TAG)
            .commit()
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
