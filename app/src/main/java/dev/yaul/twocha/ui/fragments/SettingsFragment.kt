package dev.yaul.twocha.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import dagger.hilt.android.AndroidEntryPoint
import dev.yaul.twocha.ui.screens.SettingsScreen
import dev.yaul.twocha.ui.theme.TwochaTheme
import dev.yaul.twocha.viewmodel.VpnViewModel

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val viewModel: VpnViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed(viewLifecycleOwner)
            )
            ViewTreeLifecycleOwner.set(this, viewLifecycleOwner)
            ViewTreeViewModelStoreOwner.set(this, viewModelStoreOwner)
            ViewTreeSavedStateRegistryOwner.set(this, viewLifecycleOwner)
            setContent {
                val themeStyle = viewModel.themeStyle.collectAsState()
                val dynamicColor = viewModel.dynamicColor.collectAsState()

                TwochaTheme(
                    themeStyle = themeStyle.value,
                    dynamicColor = dynamicColor.value
                ) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { parentFragmentManager.popBackStack() }
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "SettingsFragment"
    }
}
