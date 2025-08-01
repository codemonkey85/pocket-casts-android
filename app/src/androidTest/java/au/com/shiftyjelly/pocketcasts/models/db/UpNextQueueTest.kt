package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoPlaySource
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueueImpl
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.squareup.moshi.Moshi
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class UpNextQueueTest {
    lateinit var appDatabase: AppDatabase
    lateinit var upNextQueue: UpNextQueue
    lateinit var downloadManager: DownloadManager

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        appDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
            .build()
        downloadManager = mock {}
        val episodeManager = mock<EpisodeManager> {}
        val settings = mock<Settings> {
            on { autoDownloadUpNext } doReturn UserSetting.Mock(true, mock())
            on { lastAutoPlaySource } doReturn UserSetting.Mock(AutoPlaySource.Predefined.None, mock())
            on { trackingAutoPlaySource } doReturn UserSetting.Mock(AutoPlaySource.Predefined.None, mock())
        }
        val syncManager = mock<SyncManager> {}

        upNextQueue = UpNextQueueImpl(appDatabase, settings, episodeManager, syncManager, context)
        upNextQueue.setupBlocking()
    }

    @Test
    fun testPlayNext() {
        val uuids = mutableListOf<String>()

        runBlocking {
            for (i in 0..25) {
                val uuid = UUID.randomUUID().toString()
                val episode = PodcastEpisode(uuid = uuid, publishedDate = Date())
                uuids.add(uuid)
                appDatabase.episodeDao().insertBlocking(episode)
                upNextQueue.playNextBlocking(episode, downloadManager, null)
            }
        }

        val currentEpisode = upNextQueue.currentEpisode
        assertTrue("Current episode should be first uuid", currentEpisode?.uuid == uuids.first())
        val queue = upNextQueue.queueEpisodes
        assertTrue("Queue should be the next episodes ordered", queue.map { it.uuid } == uuids.subList(1, uuids.size).reversed())
    }

    @Test
    fun testPlayNextList() {
        val episodes = mutableListOf<BaseEpisode>()

        runBlocking {
            for (i in 0..25) {
                val uuid = UUID.randomUUID().toString()
                val episode = PodcastEpisode(uuid = uuid, publishedDate = Date())
                appDatabase.episodeDao().insertBlocking(episode)
                episodes.add(episode)
            }

            upNextQueue.playAllNext(episodes, downloadManager)
        }

        val currentEpisode = upNextQueue.currentEpisode
        assertTrue("Current episode should be first uuid", currentEpisode?.uuid == episodes.first().uuid)
        val queue = upNextQueue.queueEpisodes
        assertTrue("Queue should be the next episodes ordered", queue.map { it.uuid } == episodes.subList(1, episodes.size).map { it.uuid })
    }

    @Test
    fun testPlayLast() {
        val uuids = mutableListOf<String>()

        runBlocking {
            for (i in 0..25) {
                val uuid = UUID.randomUUID().toString()
                val episode = PodcastEpisode(uuid = uuid, publishedDate = Date())
                uuids.add(uuid)
                appDatabase.episodeDao().insertBlocking(episode)
                upNextQueue.playLastBlocking(episode, downloadManager, null)
            }
        }

        val currentEpisode = upNextQueue.currentEpisode
        assertTrue("Current episode should be first uuid", currentEpisode?.uuid == uuids.first())
        val queue = upNextQueue.queueEpisodes
        assertTrue("Queue should be the last episodes ordered", queue.map { it.uuid } == uuids.subList(1, uuids.size))
    }

    @Test
    fun testPlayLastList() {
        val episodes = mutableListOf<BaseEpisode>()

        runBlocking {
            for (i in 0..5) {
                val uuid = UUID.randomUUID().toString()
                val episode = PodcastEpisode(uuid = uuid, publishedDate = Date())
                appDatabase.episodeDao().insertBlocking(episode)
                episodes.add(episode)
            }

            upNextQueue.playAllLast(episodes, downloadManager)
        }

        val currentEpisode = upNextQueue.currentEpisode
        assertTrue("Current episode should be first uuid", currentEpisode?.uuid == episodes.first().uuid)
        val queue = upNextQueue.queueEpisodes
        assertTrue("Queue should be the next episodes ordered", queue.map { it.uuid } == episodes.subList(1, episodes.size).map { it.uuid })
    }

    @Test
    fun testDelete() {
        val uuids = mutableListOf<String>()

        runBlocking {
            for (i in 0..5) {
                val uuid = UUID.randomUUID().toString()
                val episode = PodcastEpisode(uuid = uuid, publishedDate = Date())
                uuids.add(uuid)
                appDatabase.episodeDao().insertBlocking(episode)
                upNextQueue.playLastBlocking(episode, downloadManager, null)
            }
        }

        val currentEpisode = upNextQueue.currentEpisode
        assertTrue("Current episode should be first uuid", currentEpisode?.uuid == uuids.first())
        runBlocking {
            upNextQueue.removeEpisode(currentEpisode!!)
        }

        val newCurrentEpisode = upNextQueue.currentEpisode
        assertTrue("Current episode should be second uuid", newCurrentEpisode?.uuid == uuids[1])

        val queue = upNextQueue.queueEpisodes
        assertTrue("Queue should be the remaining episodes ordered", queue.map { it.uuid } == uuids.subList(2, uuids.size))
    }

    @Test
    fun testDeleteThenPlayLast() {
        val uuids = mutableListOf<String>()

        runBlocking {
            for (i in 0..5) {
                val uuid = UUID.randomUUID().toString()
                val episode = PodcastEpisode(uuid = uuid, publishedDate = Date())
                uuids.add(uuid)
                appDatabase.episodeDao().insertBlocking(episode)
                upNextQueue.playLastBlocking(episode, downloadManager, null)
            }
        }

        val currentEpisode = upNextQueue.currentEpisode
        assertTrue("Current episode should be first uuid", currentEpisode?.uuid == uuids.first())
        runBlocking {
            upNextQueue.removeEpisode(currentEpisode!!)
        }

        val newCurrentEpisode = upNextQueue.currentEpisode
        assertTrue("Current episode should be second uuid", newCurrentEpisode?.uuid == uuids[1])

        val lastUuid = UUID.randomUUID().toString()
        val playLastEpisode = PodcastEpisode(uuid = lastUuid, publishedDate = Date())
        runBlocking {
            upNextQueue.removeEpisode(newCurrentEpisode!!)
            appDatabase.episodeDao().insertBlocking(playLastEpisode)
            upNextQueue.playLastBlocking(playLastEpisode, downloadManager, null)
        }

        val queue = upNextQueue.queueEpisodes
        assertTrue("Last place should be our play last episode", queue.last().uuid == lastUuid)
    }

    @Test
    fun testDeleteThenPlayNext() {
        val uuids = mutableListOf<String>()

        runBlocking {
            for (i in 0..5) {
                val uuid = UUID.randomUUID().toString()
                val episode = PodcastEpisode(uuid = uuid, publishedDate = Date())
                uuids.add(uuid)
                appDatabase.episodeDao().insertBlocking(episode)
                upNextQueue.playLastBlocking(episode, downloadManager, null)
            }
        }

        val currentEpisode = upNextQueue.currentEpisode
        assertTrue("Current episode should be first uuid", currentEpisode?.uuid == uuids.first())
        runBlocking {
            upNextQueue.removeEpisode(currentEpisode!!)
        }

        val newCurrentEpisode = upNextQueue.currentEpisode
        assertTrue("Current episode should be second uuid", newCurrentEpisode?.uuid == uuids[1])

        val nextUuid = UUID.randomUUID().toString()
        val playLastEpisode = PodcastEpisode(uuid = nextUuid, publishedDate = Date())
        runBlocking {
            upNextQueue.removeEpisode(newCurrentEpisode!!)
            appDatabase.episodeDao().insertBlocking(playLastEpisode)
            upNextQueue.playNextBlocking(playLastEpisode, downloadManager, null)
        }

        val queue = upNextQueue.queueEpisodes
        assertTrue("First place in queue should be our play next episode", queue.first().uuid == nextUuid)
    }

    @Test
    fun testClearUpNext() {
        val uuids = mutableListOf<String>()

        runBlocking {
            for (i in 0..5) {
                val uuid = UUID.randomUUID().toString()
                val episode = PodcastEpisode(uuid = uuid, publishedDate = Date())
                uuids.add(uuid)
                appDatabase.episodeDao().insertBlocking(episode)
                upNextQueue.playLastBlocking(episode, downloadManager, null)
            }

            upNextQueue.clearUpNext()
        }

        val currentEpisode = upNextQueue.currentEpisode
        assertTrue("Current episode should still be first", currentEpisode?.uuid == uuids.first())
        assertTrue("Queue should be empty", upNextQueue.queueEpisodes.isEmpty())
    }

    @Test
    fun testRemoveAndShuffle() {
        val uuids = mutableListOf<String>()
        val episodes = mutableListOf<PodcastEpisode>()

        runBlocking {
            for (i in 0..5) {
                val uuid = UUID.randomUUID().toString()
                val episode = PodcastEpisode(uuid = uuid, publishedDate = Date())
                uuids.add(uuid)
                episodes.add(episode)
                appDatabase.episodeDao().insertBlocking(episode)
                upNextQueue.playLastBlocking(episode, downloadManager, null)
            }

            val initialEpisode = upNextQueue.currentEpisode
            assertTrue("Initial current episode should be the first uuid", initialEpisode?.uuid == uuids.first())

            upNextQueue.removeEpisode(initialEpisode!!, shouldShuffleUpNext = true)
        }

        val newCurrentEpisode = upNextQueue.currentEpisode
        assertTrue("New current episode should not be the removed episode", newCurrentEpisode?.uuid != uuids.first())
        assertTrue("New current episode should be one of the remaining episodes", uuids.contains(newCurrentEpisode?.uuid))
    }
}
