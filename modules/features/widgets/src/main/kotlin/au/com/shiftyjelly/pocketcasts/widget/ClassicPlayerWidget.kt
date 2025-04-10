package au.com.shiftyjelly.pocketcasts.widget

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.state.PreferencesGlanceStateDefinition
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.widget.data.ClassicPlayerWidgetState
import au.com.shiftyjelly.pocketcasts.widget.data.LocalSource
import au.com.shiftyjelly.pocketcasts.widget.ui.ClassicPlayer

internal class ClassicPlayerWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val adapter = ClassicPlayerWidgetState.Adapter(context)
        val updatedState = adapter.updateState(id) { it }

        provideContent {
            CompositionLocalProvider(LocalSource provides SourceView.WIDGET_PLAYER_OLD) {
                ClassicPlayer(adapter.currentState() ?: updatedState)
            }
        }
    }
}
