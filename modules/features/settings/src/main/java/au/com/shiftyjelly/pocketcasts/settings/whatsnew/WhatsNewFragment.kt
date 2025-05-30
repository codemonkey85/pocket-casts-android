package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.settings.whatsnew.WhatsNewViewModel.NavigationState
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WhatsNewFragment : BaseFragment() {

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    override var statusBarIconColor: StatusBarIconColor = StatusBarIconColor.ThemeNoToolbar
    override var backgroundTransparent: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppTheme(theme.activeTheme) {
            CallOnce {
                analyticsTracker.track(
                    AnalyticsEvent.WHATSNEW_SHOWN,
                    mapOf("version" to Settings.WHATS_NEW_VERSION_CODE),
                )
            }

            val onClose: () -> Unit = {
                @Suppress("DEPRECATION")
                activity?.onBackPressed()
            }
            var confirmActionClicked: Boolean by remember { mutableStateOf(false) }
            WhatsNewPage(
                onConfirm = {
                    analyticsTracker.track(
                        AnalyticsEvent.WHATSNEW_CONFIRM_BUTTON_TAPPED,
                        mapOf("version" to Settings.WHATS_NEW_VERSION_CODE),
                    )
                    if (it.shouldCloseOnConfirm) {
                        onClose()
                    }
                    performConfirmAction(it)
                    confirmActionClicked = true
                },
                onClose = {
                    analyticsTracker.track(
                        AnalyticsEvent.WHATSNEW_DISMISSED,
                        mapOf("version" to Settings.WHATS_NEW_VERSION_CODE),
                    )
                    onClose()
                },

            )

            DisposableEffect(Unit) {
                onDispose {
                    val fragmentHostListener = activity as? FragmentHostListener
                        ?: throw IllegalStateException(FRAGMENT_HOST_LISTENER_NOT_IMPLEMENTED)
                    fragmentHostListener.whatsNewDismissed(fromConfirmAction = confirmActionClicked)
                }
            }
        }
    }

    private fun performConfirmAction(navigationState: NavigationState) {
        when (navigationState) {
            is NavigationState.StartUpsellFlow -> startUpsellFlow(navigationState.source)
            is NavigationState.ForceClose -> {
                @Suppress("DEPRECATION")
                activity?.onBackPressed()
            }
        }
    }

    private fun startUpsellFlow(source: OnboardingUpgradeSource) {
        val onboardingFlow = OnboardingFlow.Upsell(
            source = source,
        )
        OnboardingLauncher.openOnboardingFlow(requireActivity(), onboardingFlow)
    }

    companion object {
        private const val FRAGMENT_HOST_LISTENER_NOT_IMPLEMENTED = "Activity must implement FragmentHostListener"
        fun isWhatsNewNewerThan(versionCode: Int?): Boolean {
            return Settings.WHATS_NEW_VERSION_CODE > (versionCode ?: 0)
        }
    }
}
