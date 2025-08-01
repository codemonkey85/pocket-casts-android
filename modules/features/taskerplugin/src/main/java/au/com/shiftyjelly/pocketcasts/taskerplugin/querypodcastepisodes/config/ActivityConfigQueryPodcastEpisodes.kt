package au.com.shiftyjelly.pocketcasts.taskerplugin.querypodcastepisodes.config

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ActivityConfigBase
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ComposableTaskerInputFieldList
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.TaskerInputFieldState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import au.com.shiftyjelly.pocketcasts.images.R as RD

@AndroidEntryPoint
class ActivityConfigQueryPodcastEpisodes : ActivityConfigBase<ViewModelConfigQueryPodcastEpisodes>() {
    override val viewModel: ViewModelConfigQueryPodcastEpisodes by viewModels()
}

@Preview(showBackground = true)
@Composable
private fun ComposableConfigQueryPodcastEpisodesPreview() {
    AppTheme(Theme.ThemeType.CLASSIC_LIGHT) {
        ComposableTaskerInputFieldList(
            listOf(
                TaskerInputFieldState.Content(
                    MutableStateFlow("All About Android"),
                    R.string.podcast_id_or_title,
                    RD.drawable.auto_tab_podcasts,
                    MutableStateFlow(true),
                    {},
                    listOf("%test"),
                    MutableStateFlow(listOf("New Releases", "Up Next")),
                ),
            ),
            onFinish = {},
        )
    }
}
