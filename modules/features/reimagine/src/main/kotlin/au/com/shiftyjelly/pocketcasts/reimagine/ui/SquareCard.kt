package au.com.shiftyjelly.pocketcasts.reimagine.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.PocketCastsPill
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.CaptureController
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import java.sql.Date
import java.time.Instant

@Composable
internal fun SquarePodcastCard(
    podcast: Podcast,
    episodeCount: Int,
    shareColors: ShareColors,
    captureController: CaptureController,
    modifier: Modifier = Modifier,
    constrainedSize: (maxWidth: Dp, maxHeight: Dp) -> DpSize = { width, height -> DpSize(width, height) },
) = SquareCard(
    data = PodcastCardData(
        podcast = podcast,
        episodeCount = episodeCount,
    ),
    shareColors = shareColors,
    constrainedSize = constrainedSize,
    captureController = captureController,
    modifier = modifier,
)

@Composable
internal fun SquareEpisodeCard(
    episode: PodcastEpisode,
    podcast: Podcast,
    useEpisodeArtwork: Boolean,
    shareColors: ShareColors,
    captureController: CaptureController,
    modifier: Modifier = Modifier,
    constrainedSize: (maxWidth: Dp, maxHeight: Dp) -> DpSize = { width, height -> DpSize(width, height) },
) = SquareCard(
    data = EpisodeCardData(
        episode = episode,
        podcast = podcast,
        useEpisodeArtwork = useEpisodeArtwork,
    ),
    shareColors = shareColors,
    constrainedSize = constrainedSize,
    captureController = captureController,
    modifier = modifier,
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SquareCard(
    data: CardData,
    shareColors: ShareColors,
    captureController: CaptureController,
    modifier: Modifier = Modifier,
    constrainedSize: (maxWidth: Dp, maxHeight: Dp) -> DpSize = { width, height -> DpSize(width, height) },
) = BoxWithConstraints(
    contentAlignment = Alignment.Center,
    modifier = modifier,
) {
    val backgroundGradient = Brush.verticalGradient(
        listOf(
            shareColors.cardTop,
            shareColors.cardBottom,
        ),
    )
    val size = constrainedSize(maxWidth, maxHeight)
    val minDimension = minOf(size.width, size.height)
    val isCardSmall = minDimension < 280.dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .capturable(captureController)
            .background(backgroundGradient, RoundedCornerShape(12.dp))
            .size(minDimension),
    ) {
        Spacer(
            modifier = Modifier.height(if (isCardSmall) 26.dp else 32.dp),
        )
        data.Image(
            modifier = Modifier
                .size(minDimension * 0.4f)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(
            modifier = Modifier.height(if (isCardSmall) 12.dp else 16.dp),
        )
        TextH70(
            text = data.topText(),
            disableAutoScale = true,
            maxLines = 1,
            color = shareColors.cardTextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(
            modifier = Modifier.height(6.dp),
        )
        TextH40(
            text = data.middleText(),
            disableAutoScale = true,
            maxLines = 2,
            textAlign = TextAlign.Center,
            color = shareColors.cardTextPrimary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(
            modifier = Modifier.height(6.dp),
        )
        TextH70(
            text = data.bottomText(),
            disableAutoScale = true,
            maxLines = 1,
            textAlign = TextAlign.Center,
            color = shareColors.cardTextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(
            modifier = Modifier.weight(1f),
        )
        PocketCastsPill(
            disableScale = true,
        )
        Spacer(
            modifier = Modifier.weight(2f),
        )
    }
}

@ShowkaseComposable(name = "Square podcast card", group = "Sharing")
@Preview(name = "SquarePodcastCardDark")
@Composable
private fun SquarePodcastCardDarkPreview() = SquarePodcastCardPreview(
    baseColor = Color(0xFFEC0404),
)

@Preview(name = "SquarePodcastCardLight")
@Composable
private fun SquarePodcastCardLightPreview() = SquarePodcastCardPreview(
    baseColor = Color(0xFFFBCB04),
)

@ShowkaseComposable(name = "Square episode card", group = "Sharing")
@Preview(name = "SquareEpisodeCardDark")
@Composable
private fun SquareEpisodeCardDarkPreview() = SquareEpisodeCardPreview(
    baseColor = Color(0xFFEC0404),
)

@Preview(name = "SquareEpisodeCardLight")
@Composable
private fun SquareEpisodeCardLightPreview() = SquareEpisodeCardPreview(
    baseColor = Color(0xFFFBCB04),
)

@Composable
private fun SquarePodcastCardPreview(
    baseColor: Color,
) = SquareCardPreview(
    data = PodcastCardData(
        podcast = Podcast(
            uuid = "podcast-id",
            title = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            episodeFrequency = "monthly",
        ),
        episodeCount = 120,
    ),
    baseColor = baseColor,
)

@Composable
private fun SquareEpisodeCardPreview(
    baseColor: Color,
) = SquareCardPreview(
    data = EpisodeCardData(
        episode = PodcastEpisode(
            uuid = "episode-id",
            podcastUuid = "podcast-id",
            publishedDate = Date.from(Instant.parse("2024-12-03T10:15:30.00Z")),
            title = "Nobis sapiente fugit vitae. Iusto magnam nam nam et odio. Debitis cupiditate officiis et. Sit quia in voluptate sit voluptatem magni.",
        ),
        podcast = Podcast(
            uuid = "podcast-id",
            title = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        ),
        useEpisodeArtwork = true,
    ),
    baseColor = baseColor,
)

@Composable
private fun SquareCardPreview(
    data: CardData,
    baseColor: Color,
) = SquareCard(
    data = data,
    shareColors = ShareColors(baseColor),
    captureController = rememberCaptureController(),
)
