package dev.yaul.twocha.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import dev.yaul.twocha.ui.screens.LogsScreen
import dev.yaul.twocha.ui.theme.TwochaTheme
import dev.yaul.twocha.viewmodel.VpnViewModel

@AndroidEntryPoint
class LogsFragment : Fragment() {

    private val viewModel: VpnViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val settings = viewModel.settings.collectAsState()

                TwochaTheme(
                    darkTheme = settings.value.darkMode,
                    dynamicColor = settings.value.dynamicColor
                ) {
                    LogsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { requireActivity().onBackPressedDispatcher.onBackPressed() }
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "LogsFragment"
    }
}
