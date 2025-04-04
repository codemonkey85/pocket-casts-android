package au.com.shiftyjelly.pocketcasts.settings.privacy

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import javax.inject.Inject

class UserAnalyticsSettings @Inject constructor(
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTracker,
) {

    fun updateAnalyticsSetting(enabled: Boolean) {
        if (enabled) {
            settings.collectAnalytics.set(true, updateModifiedAt = true)
            analyticsTracker.track(AnalyticsEvent.ANALYTICS_OPT_IN)
        } else {
            analyticsTracker.track(AnalyticsEvent.ANALYTICS_OPT_OUT)
            settings.collectAnalytics.set(false, updateModifiedAt = false)
            analyticsTracker.clearAllData()
        }
    }

    fun updateAnalyticsThirdPartySetting(enabled: Boolean) {
        if (enabled) {
            settings.collectAnalyticsThirdParty.set(value = true, updateModifiedAt = true)
            analyticsTracker.track(AnalyticsEvent.ANALYTICS_THIRD_PARTY_OPT_IN)
        } else {
            analyticsTracker.track(AnalyticsEvent.ANALYTICS_THIRD_PARTY_OPT_OUT)
            settings.collectAnalyticsThirdParty.set(value = false, updateModifiedAt = true)
            analyticsTracker.clearAllData()
        }
        settings.isTrackingConsentRequired.set(value = false, updateModifiedAt = true)
    }

    fun updateCrashReportsSetting(enabled: Boolean) {
        analyticsTracker.track(AnalyticsEvent.CRASH_REPORTS_TOGGLED, mapOf("enabled" to enabled))
        settings.sendCrashReports.set(enabled, updateModifiedAt = true)
    }

    fun updateLinkAccountSetting(enabled: Boolean) {
        settings.linkCrashReportsToUser.set(enabled, updateModifiedAt = true)
    }
}
