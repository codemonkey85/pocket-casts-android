package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist
import java.time.Clock
import java.time.temporal.ChronoUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

data class SmartRules(
    val episodeStatus: EpisodeStatusRule,
    val downloadStatus: DownloadStatusRule,
    val mediaType: MediaTypeRule,
    val releaseDate: ReleaseDateRule,
    val starred: StarredRule,
    val podcastsRule: PodcastsRule,
    val episodeDuration: EpisodeDurationRule,
) {
    fun toSqlWhereClause(clock: Clock, playlistId: Long?) = buildString {
        episodeStatus.run { append(clock, playlistId) }
        downloadStatus.run { append(clock, playlistId) }
        mediaType.run { append(clock, playlistId) }
        releaseDate.run { append(clock, playlistId) }
        starred.run { append(clock, playlistId) }
        podcastsRule.run { append(clock, playlistId) }
        episodeDuration.run { append(clock, playlistId) }
        NonArchivedRule.run { append(clock, playlistId) }
        FollowedPodcastRule.run { append(clock, playlistId) }
    }

    data class EpisodeStatusRule(
        val unplayed: Boolean,
        val inProgress: Boolean,
        val completed: Boolean,
    ) : SmartRule {
        override fun toSqlWhereClause(clock: Clock, playlistId: Long?) = buildString {
            if (!areAllConstraintsTicked && isAnyConstraintTicked) {
                val statuses = buildList {
                    if (unplayed) {
                        add(EpisodePlayingStatus.NOT_PLAYED)
                    }
                    if (inProgress) {
                        add(EpisodePlayingStatus.IN_PROGRESS)
                    }
                    if (completed) {
                        add(EpisodePlayingStatus.COMPLETED)
                    }
                }
                append("episode.playing_status IN (")
                append(statuses.joinToString(separator = ",") { status -> "${status.ordinal}" })
                append(')')
            }
        }

        private val areAllConstraintsTicked get() = unplayed && inProgress && completed

        private val isAnyConstraintTicked get() = unplayed || inProgress || completed
    }

    enum class DownloadStatusRule : SmartRule {
        Any,
        Downloaded,
        NotDownloaded,
        ;

        override fun toSqlWhereClause(clock: Clock, playlistId: Long?) = buildString {
            if (this@DownloadStatusRule != Any) {
                val isSystemDownloadsPlaylist = playlistId == SmartPlaylist.PLAYLIST_ID_SYSTEM_DOWNLOADS
                val includeDownloaded = this@DownloadStatusRule == Downloaded
                val includeDownloading = isSystemDownloadsPlaylist || this@DownloadStatusRule == NotDownloaded
                val includeNotDownloaded = this@DownloadStatusRule == NotDownloaded

                val statuses = buildList {
                    if (includeDownloaded) {
                        add(EpisodeStatusEnum.DOWNLOADED)
                    }
                    if (includeDownloading) {
                        add(EpisodeStatusEnum.DOWNLOADING)
                        add(EpisodeStatusEnum.QUEUED)
                        add(EpisodeStatusEnum.WAITING_FOR_POWER)
                        add(EpisodeStatusEnum.WAITING_FOR_WIFI)
                    }
                    if (includeNotDownloaded) {
                        add(EpisodeStatusEnum.NOT_DOWNLOADED)
                        add(EpisodeStatusEnum.DOWNLOAD_FAILED)
                    }
                }
                append("episode.episode_status IN (")
                append(statuses.joinToString(separator = ",") { status -> "${status.ordinal}" })
                append(')')

                if (isSystemDownloadsPlaylist) {
                    if (isNotEmpty()) {
                        append(" OR ")
                    }
                    append("(episode.episode_status = ")
                    append(EpisodeStatusEnum.DOWNLOAD_FAILED.ordinal)
                    append(" AND episode.last_download_attempt_date > ")
                    append(clock.instant().minus(7, ChronoUnit.DAYS).toEpochMilli())
                    append(')')
                }
            }
        }
    }

    enum class MediaTypeRule : SmartRule {
        Any,
        Audio,
        Video,
        ;

        override fun toSqlWhereClause(clock: Clock, playlistId: Long?) = buildString {
            when (this@MediaTypeRule) {
                Any -> Unit
                Audio -> append("episode.file_type LIKE 'audio/%'")
                Video -> append("episode.file_type LIKE 'video/%'")
            }
        }
    }

    enum class ReleaseDateRule(
        private val duration: Duration,
    ) : SmartRule {
        AnyTime(
            duration = Duration.INFINITE,
        ),
        Last24Hours(
            duration = 1.days,
        ),
        Last3Days(
            duration = 3.days,
        ),
        LastWeek(
            duration = 7.days,
        ),
        Last2Weeks(
            duration = 14.days,
        ),
        LastMonth(
            duration = 31.days,
        ),
        ;

        override fun toSqlWhereClause(clock: Clock, playlistId: Long?) = buildString {
            if (duration != Duration.INFINITE) {
                append("episode.published_date > ")
                append(clock.instant().minusMillis(duration.inWholeMilliseconds).toEpochMilli())
            }
        }
    }

    enum class StarredRule : SmartRule {
        Any,
        Starred,
        ;

        override fun toSqlWhereClause(clock: Clock, playlistId: Long?) = buildString {
            when (this@StarredRule) {
                Any -> Unit
                Starred -> append("episode.starred = 1")
            }
        }
    }

    sealed interface PodcastsRule : SmartRule {
        data object Any : PodcastsRule {
            override fun toSqlWhereClause(clock: Clock, playlistId: Long?) = ""
        }

        data class Selected(
            val uuids: List<String>,
        ) : PodcastsRule {
            override fun toSqlWhereClause(clock: Clock, playlistId: Long?) = buildString {
                append("episode.podcast_id IN (")
                append(uuids.joinToString(separator = ",") { uuid -> "'$uuid'" })
                append(')')
            }
        }
    }

    sealed interface EpisodeDurationRule : SmartRule {
        data object Any : EpisodeDurationRule {
            override fun toSqlWhereClause(clock: Clock, playlistId: Long?) = ""
        }

        data class Constrained(
            val longerThan: Duration,
            val shorterThan: Duration,
        ) : EpisodeDurationRule {
            override fun toSqlWhereClause(clock: Clock, playlistId: Long?) = buildString {
                append("(episode.duration BETWEEN ")
                append(longerThan.inWholeSeconds)
                append(" AND ")
                append(shorterThan.inWholeSeconds)
                append(')')
            }
        }
    }

    data object NonArchivedRule : SmartRule {
        override fun toSqlWhereClause(clock: Clock, playlistId: Long?) = "episode.archived = 0"
    }

    data object FollowedPodcastRule : SmartRule {
        override fun toSqlWhereClause(clock: Clock, playlistId: Long?) = "podcast.subscribed = 1"
    }

    sealed interface SmartRule {
        fun StringBuilder.append(clock: Clock, playlistId: Long?) {
            val sqlString = toSqlWhereClause(clock, playlistId)
            if (sqlString.isNotEmpty()) {
                if (isNotEmpty()) {
                    append(" AND ")
                }
                append('(')
                append(sqlString)
                append(')')
            }
        }

        fun toSqlWhereClause(clock: Clock, playlistId: Long?): String
    }

    companion object {
        val Default = SmartRules(
            EpisodeStatusRule(unplayed = true, inProgress = true, completed = true),
            DownloadStatusRule.Any,
            MediaTypeRule.Any,
            ReleaseDateRule.AnyTime,
            StarredRule.Any,
            PodcastsRule.Any,
            EpisodeDurationRule.Any,
        )
    }
}
