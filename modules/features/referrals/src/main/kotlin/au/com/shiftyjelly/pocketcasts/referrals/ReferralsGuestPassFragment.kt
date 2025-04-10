package au.com.shiftyjelly.pocketcasts.referrals

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.NavigationBarColor
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil.setBackgroundColor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import androidx.compose.ui.graphics.Color as ComposeColor

@AndroidEntryPoint
class ReferralsGuestPassFragment : BaseFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARG, Args::class.java) })
    private val pageType get() = args.pageType

    override var statusBarIconColor: StatusBarIconColor = StatusBarIconColor.Light

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        val context = LocalContext.current
        val windowSize = calculateWindowSizeClass(context.getActivity() as Activity)

        setBackgroundColor(view, ComposeColor.Transparent.toArgb())

        val onDismiss = {
            (activity as? FragmentHostListener)?.bottomSheetClosePressed(this)
        }

        when (pageType) {
            ReferralsPageType.Send -> {
                ReferralsSendGuestPassPage(
                    onDismiss = { onDismiss() },
                )
            }

            ReferralsPageType.Claim -> ReferralsClaimGuestPassPage(
                onDismiss = { onDismiss() },
            )

            ReferralsPageType.InvalidOffer -> ReferralsInvalidOfferPage(
                onDismiss = { onDismiss() },
            )
        }

        LaunchedEffect(Unit) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    if (windowSize.widthSizeClass == WindowWidthSizeClass.Compact ||
                        windowSize.heightSizeClass == WindowHeightSizeClass.Compact
                    ) {
                        delay(200) // To prevent race condition in updating activity's status bar color from tab fragments on fresh install
                        updateStatusAndNavColors()
                    }
                }
            }
        }
    }.apply {
        consumeWindowInsets = false
    }

    private fun updateStatusAndNavColors() {
        activity?.let {
            theme.updateWindowNavigationBarColor(window = it.window, navigationBarColor = NavigationBarColor.Color(color = Color.BLACK, lightIcons = true))
            theme.updateWindowStatusBarIcons(window = it.window, statusBarIconColor = StatusBarIconColor.Light)
        }
    }

    @Parcelize
    private class Args(
        val pageType: ReferralsPageType,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARG = "ReferralsGuestPassFragment"
        fun newInstance(pageType: ReferralsPageType) = ReferralsGuestPassFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARG to Args(pageType))
        }
    }

    enum class ReferralsPageType {
        Send,
        Claim,
        InvalidOffer,
    }
}
