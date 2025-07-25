package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.list.ListServiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class ShareListIncomingViewModel
@Inject constructor(
    val podcastManager: PodcastManager,
    val listServiceManager: ListServiceManager,
    val playbackManager: PlaybackManager,
    val analyticsTracker: AnalyticsTracker,
) : ViewModel(),
    CoroutineScope {
    var isFragmentChangingConfigurations: Boolean = false
    val share = MutableLiveData<ShareState>()
    val subscribedUuids =
        podcastManager.getSubscribedPodcastUuidsRxSingle()
            .subscribeOn(Schedulers.io())
            .toFlowable()
            .mergeWith(podcastManager.podcastSubscriptionsRxFlowable())
            .toLiveData()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    fun loadShareUrl(url: String) {
        share.postValue(ShareState.Loading)
        val id = listServiceManager.extractShareListIdFromWebUrl(url)
        viewModelScope.launch {
            try {
                val list = listServiceManager.openPodcastList(id)
                share.postValue(ShareState.Loaded(title = list.title, description = list.description, podcasts = list.fullPodcasts))
            } catch (ex: Exception) {
                share.postValue(ShareState.Error)
            }
        }
    }

    fun subscribeToPodcast(uuid: String) {
        launch {
            val podcast = podcastManager.findPodcastByUuidBlocking(uuid)
            if (podcast == null || !podcast.isSubscribed) {
                podcastManager.subscribeToPodcast(uuid, true)
            }
        }
    }

    fun unsubscribeFromPodcast(uuid: String) {
        podcastManager.unsubscribeAsync(uuid, playbackManager)
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }

    fun trackShareEvent(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) {
        analyticsTracker.track(event, properties)
    }
}

sealed class ShareState {
    object Loading : ShareState()
    data class Loaded(val title: String?, val description: String?, val podcasts: List<Podcast>?) : ShareState()
    object Error : ShareState()
}
