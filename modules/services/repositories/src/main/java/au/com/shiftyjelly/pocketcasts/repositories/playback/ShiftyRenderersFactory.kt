package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager

/**
 * An [AudioProcessor] that skips silence in the input stream. Input and output are 16-bit
 * PCM.
 */
@OptIn(UnstableApi::class)
class ShiftyRenderersFactory(
    context: Context,
    statsManager: StatsManager,
    private var boostVolume: Boolean,
) : DefaultRenderersFactory(context),
    AnalyticsListener {
    private var playbackSpeed = 0f
    private var internalRenderer: ShiftyAudioRendererV2? = null
    private var audioSink: AudioSink? = null
    private var processorChain: ShiftyAudioProcessorChain? = null
    private val customAudio = ShiftyCustomAudio(statsManager)

    fun setRemoveSilence(trimMode: TrimMode) {
        processorChain?.applyTrimModeForNextUpdate(trimMode)
        audioSink?.skipSilenceEnabled = trimMode != TrimMode.OFF
    }

    fun setBoostVolume(boostVolume: Boolean) {
        this.boostVolume = boostVolume
        internalRenderer?.customAudio?.setBoostVolume(boostVolume)
    }

    fun setPlaybackSpeed(playbackSpeed: Float) {
        this.playbackSpeed = playbackSpeed
        internalRenderer?.customAudio?.playbackSpeed = playbackSpeed
    }

    override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): AudioSink {
        processorChain = ShiftyAudioProcessorChain(customAudio)
        return DefaultAudioSink.Builder(context)
            .setAudioProcessorChain(processorChain!!)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .build()
    }

    override fun buildAudioRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        audioSink: AudioSink,
        eventHandler: Handler,
        eventListener: AudioRendererEventListener,
        out: ArrayList<Renderer>,
    ) {
        this.audioSink = audioSink
        internalRenderer = ShiftyAudioRendererV2(
            customAudio,
            context,
            mediaCodecSelector,
            eventHandler,
            eventListener,
            audioSink,
        ).apply {
            customAudio.setBoostVolume(boostVolume)
            customAudio.playbackSpeed = playbackSpeed
            out.add(this)
        }
    }

    override fun onAudioSessionIdChanged(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {
        customAudio.setupVolumeBoost(audioSessionId)
    }
}
