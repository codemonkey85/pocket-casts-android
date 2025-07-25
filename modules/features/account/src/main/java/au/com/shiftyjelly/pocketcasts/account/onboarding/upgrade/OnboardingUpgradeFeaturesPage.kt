package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.activity.SystemBarStyle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.account.components.SubscriptionPriceLabel
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.UpgradeFeatureItem
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.UpgradeRowButton
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesViewModel
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationIconButton
import au.com.shiftyjelly.pocketcasts.compose.bars.SystemBarsStyles
import au.com.shiftyjelly.pocketcasts.compose.components.AutoResizeText
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalPagerWrapper
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.components.SegmentedTabBar
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.images.OfferBadge
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val MAX_OFFER_BADGE_TEXT_LENGTH = 23
private const val MIN_SCREEN_WIDTH_FOR_HORIZONTAL_DISPLAY = 400

@Composable
internal fun OnboardingUpgradeFeaturesPage(
    viewModel: OnboardingUpgradeFeaturesViewModel,
    state: OnboardingUpgradeFeaturesState,
    flow: OnboardingFlow,
    source: OnboardingUpgradeSource,
    onBackPress: () -> Unit,
    onClickSubscribe: (showUpgradeBottomSheet: Boolean) -> Unit,
    onNotNowPress: () -> Unit,
    onUpdateSystemBars: (SystemBarsStyles) -> Unit,
) {
    @Suppress("NAME_SHADOWING")
    val onNotNowPress = {
        viewModel.onNotNow(flow, source)
        onNotNowPress()
    }

    @Suppress("NAME_SHADOWING")
    val onBackPress = {
        viewModel.onDismiss(flow, source)
        onBackPress()
    }

    CallOnce {
        viewModel.onShown(flow, source)
    }

    val scrollState = rememberScrollState()
    SetStatusBarBackground(scrollState, onUpdateSystemBars)

    when (state) {
        is OnboardingUpgradeFeaturesState.Loading -> Unit // Do Nothing
        is OnboardingUpgradeFeaturesState.Loaded -> {
            if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
                OnboardingUpgradeScreen(
                    variant = Variants.VARIANT_FEATURES,
                    onClosePress = onBackPress,
                    state = state,
                    onChangeSelectedPlan = { viewModel.changeBillingCycle(it.billingCycle) },
                    onSubscribePress = { onClickSubscribe(false) },
                    onClickPrivacyPolicy = { viewModel.onPrivacyPolicyPressed() },
                    onClickTermsAndConditions = { viewModel.onTermsAndConditionsPressed() },
                )
            } else {
                UpgradeLayout(
                    state = state,
                    source = source,
                    scrollState = scrollState,
                    onBackPress = onBackPress,
                    onNotNowPress = onNotNowPress,
                    onChangeBillingCycle = { viewModel.changeBillingCycle(it) },
                    onChangeSubscriptionTier = { viewModel.changeSubscriptionTier(it) },
                    onClickSubscribe = { onClickSubscribe(false) },
                    onClickPrivacyPolicy = { viewModel.onPrivacyPolicyPressed() },
                    onClickTermsAndConditions = { viewModel.onTermsAndConditionsPressed() },
                )
            }
        }

        is OnboardingUpgradeFeaturesState.NoSubscriptions -> {
            NoSubscriptionsLayout(
                showNotNow = source == OnboardingUpgradeSource.RECOMMENDATIONS,
                onTryAgainClick = { viewModel.loadSubscriptionPlans() },
                onNotNowClick = onNotNowPress,
                onBackPress = onBackPress,
            )
        }
    }
}

@Composable
private fun UpgradeLayout(
    state: OnboardingUpgradeFeaturesState.Loaded,
    source: OnboardingUpgradeSource,
    scrollState: ScrollState,
    onBackPress: () -> Unit,
    onNotNowPress: () -> Unit,
    onChangeBillingCycle: (BillingCycle) -> Unit,
    onChangeSubscriptionTier: (SubscriptionTier) -> Unit,
    onClickSubscribe: () -> Unit,
    onClickPrivacyPolicy: () -> Unit,
    onClickTermsAndConditions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val focusPager = remember { FocusRequester() }
        val snackbarHostState = remember { SnackbarHostState() }

        // Need this BoxWithConstraints so we can force the inner column to fill the screen with vertical scroll enabled
        BoxWithConstraints(
            Modifier
                .fillMaxHeight()
                .background(OnboardingUpgradeHelper.backgroundColor),
        ) {
            OnboardingUpgradeHelper.UpgradeBackground(
                modifier = Modifier.verticalScroll(scrollState),
                tier = state.selectedPlan.key.tier,
                backgroundGlowsRes = state.selectedPlan.backgroundGlowsRes,
            ) {
                Column(
                    Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .heightIn(min = this.calculateMinimumHeightWithInsets())
                        .padding(bottom = 100.dp), // Added to allow scrolling feature cards beyond upgrade button in large font sizes
                ) {
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NavigationIconButton(
                            onNavigationClick = onBackPress,
                            iconColor = Color.White,
                            modifier = Modifier
                                .height(48.dp)
                                .width(48.dp),
                        )
                        if (source == OnboardingUpgradeSource.RECOMMENDATIONS) {
                            TextH30(
                                text = stringResource(LR.string.not_now),
                                color = Color.White,
                                modifier = Modifier
                                    .padding(horizontal = 24.dp)
                                    .clickable { onNotNowPress() },
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Column {
                        Box(
                            modifier = Modifier.heightIn(min = 70.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            AutoResizeText(
                                text = stringResource(state.selectedPlan.customFeatureTitle(source)),
                                color = Color.White,
                                maxFontSize = 22.sp,
                                lineHeight = 30.sp,
                                fontWeight = FontWeight.W700,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                isFocusable = true,
                                modifier = Modifier
                                    .padding(horizontal = 24.dp)
                                    .fillMaxWidth(),
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            val billingCycles = remember(state.availablePlans) {
                                state.availablePlans
                                    .map { it.key.billingCycle }
                                    .distinct()
                            }
                            SegmentedTabBar(
                                items = billingCycles.map { stringResource(it.labelRes) },
                                selectedIndex = billingCycles.indexOf(state.selectedPlan.key.billingCycle),
                                onSelectItem = { index -> onChangeBillingCycle(billingCycles[index]) },
                                modifier = Modifier.width(IntrinsicSize.Max),
                            )
                        }

                        FeatureCards(
                            modifier = Modifier.focusRequester(focusPager),
                            state = state,
                            onChangeSubscriptionTier = onChangeSubscriptionTier,
                            onPrivacyPolicyClick = onClickPrivacyPolicy,
                            onTermsAndConditionsClick = onClickTermsAndConditions,
                        )
                    }

                    Spacer(Modifier.weight(1f))
                }
            }
        }

        UpgradeButton(
            selectedPlan = state.selectedPlan,
            upFocusRequester = focusPager,
            onClickSubscribe = onClickSubscribe,
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 42.dp),
        ) { snackbarData ->
            Snackbar(
                backgroundColor = Color.White,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Image(
                        painter = painterResource(IR.drawable.ic_warning),
                        colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.support05),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    TextH60(
                        text = snackbarData.message,
                        color = Color.Black,
                    )
                }
            }
        }

        if (state.purchaseFailed) {
            val message = stringResource(LR.string.onboarding_upgrade_purchase_failure_message)
            LaunchedEffect(state.purchaseFailed) {
                snackbarHostState.showSnackbar(message)
            }
        }
    }
}

@Composable
fun FeatureCards(
    state: OnboardingUpgradeFeaturesState.Loaded,
    onChangeSubscriptionTier: (SubscriptionTier) -> Unit,
    modifier: Modifier = Modifier,
    onPrivacyPolicyClick: () -> Unit = {},
    onTermsAndConditionsClick: () -> Unit = {},
) {
    val subscriptionPlans = state.availablePlans.filter { it.key.billingCycle == state.selectedPlan.key.billingCycle }
    HorizontalPagerWrapper(
        modifier = modifier,
        pageCount = subscriptionPlans.size,
        initialPage = subscriptionPlans.indexOf(state.selectedPlan).takeIf { it != -1 } ?: 0,
        onPageChange = { index -> onChangeSubscriptionTier(subscriptionPlans[index].key.tier) },
        showPageIndicator = subscriptionPlans.size > 1,
        pageSize = PageSize.Fixed(LocalConfiguration.current.screenWidthDp.dp - 64.dp),
        contentPadding = PaddingValues(horizontal = 32.dp),
    ) { index, pagerHeight, focusRequester ->
        FeatureCard(
            subscriptionPlan = subscriptionPlans[index],
            onPrivacyPolicyClick = onPrivacyPolicyClick,
            onTermsAndConditionsClick = onTermsAndConditionsClick,
            modifier = if (pagerHeight > 0) {
                Modifier.height(pagerHeight.pxToDp(LocalContext.current).dp)
            } else {
                Modifier
            }.then(
                Modifier.focusRequester(focusRequester),
            ),
        )
    }
}

private val secondaryTextColor = Color(0xFF6F7580)

@Composable
private fun FeatureCard(
    subscriptionPlan: OnboardingSubscriptionPlan,
    modifier: Modifier = Modifier,
    onPrivacyPolicyClick: () -> Unit = {},
    onTermsAndConditionsClick: () -> Unit = {},
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = 8.dp,
        backgroundColor = Color.White,
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
        ) {
            var offerBadgeTextLength by remember { mutableIntStateOf(MAX_OFFER_BADGE_TEXT_LENGTH) }
            val screenWidth = LocalConfiguration.current.screenWidthDp
            val displayInHorizontal = screenWidth >= MIN_SCREEN_WIDTH_FOR_HORIZONTAL_DISPLAY && offerBadgeTextLength <= MAX_OFFER_BADGE_TEXT_LENGTH

            if (displayInHorizontal) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                ) {
                    SubscriptionBadge(
                        iconRes = subscriptionPlan.badgeIconRes,
                        shortNameRes = subscriptionPlan.shortNameRes,
                        backgroundColor = Color.Black,
                        textColor = Color.White,
                        modifier = Modifier
                            .wrapContentWidth()
                            .wrapContentHeight(),
                    )

                    val offerBadgeText = subscriptionPlan.offerBadgeText
                    if (offerBadgeText != null) {
                        offerBadgeTextLength = offerBadgeText.length
                        OfferBadge(
                            text = offerBadgeText,
                            backgroundColor = subscriptionPlan.offerBadgeColorRes,
                            textColor = subscriptionPlan.offerBadgeTextColorRes,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                ) {
                    SubscriptionBadge(
                        iconRes = subscriptionPlan.badgeIconRes,
                        shortNameRes = subscriptionPlan.shortNameRes,
                        backgroundColor = Color.Black,
                        textColor = Color.White,
                        modifier = Modifier
                            .wrapContentWidth()
                            .wrapContentHeight(),
                    )

                    val offerBadgeText = subscriptionPlan.offerBadgeText
                    if (offerBadgeText != null) {
                        offerBadgeTextLength = offerBadgeText.length
                        OfferBadge(
                            text = offerBadgeText,
                            backgroundColor = subscriptionPlan.offerBadgeColorRes,
                            textColor = subscriptionPlan.offerBadgeTextColorRes,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.focusGroup(),
            ) {
                SubscriptionPriceLabel(
                    subscriptionPlan = subscriptionPlan,
                    isFocusable = true,
                    primaryTextColor = Color.Black,
                    secondaryTextColor = secondaryTextColor,
                )

                Spacer(
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                subscriptionPlan.featureItems.forEach { item ->
                    UpgradeFeatureItem(item = item)
                }

                Spacer(
                    modifier = Modifier.weight(1f),
                )

                OnboardingUpgradeHelper.PrivacyPolicy(
                    modifier = Modifier.focusable(),
                    color = secondaryTextColor,
                    textAlign = TextAlign.Start,
                    lineHeight = 18.sp,
                    onPrivacyPolicyClick = onPrivacyPolicyClick,
                    onTermsAndConditionsClick = onTermsAndConditionsClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun UpgradeButton(
    selectedPlan: OnboardingSubscriptionPlan,
    onClickSubscribe: () -> Unit,
    upFocusRequester: FocusRequester,
) {
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fadeBackground()
            .focusProperties {
                up = upFocusRequester
                down = FocusRequester.Cancel
                left = FocusRequester.Cancel
                right = FocusRequester.Cancel
            },
    ) {
        Column {
            UpgradeRowButton(
                primaryText = selectedPlan.ctaButtonText(isRenewingSubscription = false),
                backgroundColor = selectedPlan.ctaButtonBackgroundColor,
                textColor = selectedPlan.ctaButtonTextColor,
                fontWeight = FontWeight.W500,
                onClick = onClickSubscribe,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .heightIn(min = 48.dp),
            )
            Spacer(
                modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars),
            )
        }
    }
}

@Composable
private fun SetStatusBarBackground(
    scrollState: ScrollState,
    onUpdateSystemBars: (SystemBarsStyles) -> Unit,
) {
    val hasScrolled = scrollState.value > 0

    val scrimAlpha: Float by animateFloatAsState(
        targetValue = if (hasScrolled) 0.6f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "scrimAlpha",
    )

    val statusBarBackground = if (scrimAlpha > 0) {
        OnboardingUpgradeHelper.backgroundColor.copy(alpha = scrimAlpha).toArgb()
    } else {
        Color.Transparent.toArgb()
    }

    LaunchedEffect(statusBarBackground, onUpdateSystemBars) {
        val statusBar = SystemBarStyle.dark(statusBarBackground)
        val navigationBar = SystemBarStyle.dark(Color.Transparent.toArgb())
        onUpdateSystemBars(SystemBarsStyles(statusBar, navigationBar))
    }
}

@Composable
internal fun BoxWithConstraintsScope.calculateMinimumHeightWithInsets(): Dp {
    val statusBarPadding = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()
    val navigationBarPadding = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    val fullHeight = this.maxHeight
    return fullHeight - statusBarPadding - navigationBarPadding
}

@Composable
private fun NoSubscriptionsLayout(
    showNotNow: Boolean,
    onTryAgainClick: () -> Unit,
    onNotNowClick: () -> Unit,
    onBackPress: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .fillMaxSize(),
    ) {
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavigationIconButton(
                onNavigationClick = onBackPress,
                iconColor = MaterialTheme.theme.colors.primaryText01,
                modifier = Modifier
                    .height(48.dp)
                    .width(48.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))

        NoContentBanner(
            title = stringResource(LR.string.onboarding_upgrade_no_plans_found_title),
            body = stringResource(LR.string.onboarding_upgrade_no_plans_found_body),
            iconResourceId = IR.drawable.ic_warning,
            primaryButtonText = stringResource(LR.string.try_again),
            onPrimaryButtonClick = onTryAgainClick,
            secondaryButtonText = if (showNotNow) stringResource(LR.string.skip_for_now) else null,
            onSecondaryButtonClick = onNotNowClick,
        )

        Spacer(modifier = Modifier.weight(1.5f))
    }
}

private fun Modifier.fadeBackground() = this
    .graphicsLayer { alpha = 0.99f }
    .drawWithCache {
        onDrawWithContent {
            drawRect(Color.Black)
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black),
                ),
                blendMode = BlendMode.DstIn,
            )
            drawContent()
        }
    }

private val BillingCycle.labelRes get() = when (this) {
    BillingCycle.Monthly -> LR.string.plus_monthly
    BillingCycle.Yearly -> LR.string.plus_yearly
}
