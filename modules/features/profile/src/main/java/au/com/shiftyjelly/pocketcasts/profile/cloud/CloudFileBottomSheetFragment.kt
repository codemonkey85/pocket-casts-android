package au.com.shiftyjelly.pocketcasts.profile.cloud

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarksContainerFragment
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.profile.R
import au.com.shiftyjelly.pocketcasts.profile.cloud.CloudBottomSheetViewModel.Companion.DOWNLOAD
import au.com.shiftyjelly.pocketcasts.profile.cloud.CloudBottomSheetViewModel.Companion.EDIT
import au.com.shiftyjelly.pocketcasts.profile.cloud.CloudBottomSheetViewModel.Companion.UPLOAD
import au.com.shiftyjelly.pocketcasts.profile.cloud.CloudBottomSheetViewModel.Companion.UPLOAD_UPGRADE_REQUIRED
import au.com.shiftyjelly.pocketcasts.profile.databinding.BottomSheetCloudFileBinding
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.extensions.setSystemWindowInsetToPadding
import au.com.shiftyjelly.pocketcasts.views.helper.CloudDeleteHelper
import au.com.shiftyjelly.pocketcasts.views.helper.WarningsHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.rx2.asObservable
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.profile.R as PR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class CloudFileBottomSheetFragment : BottomSheetDialogFragment() {
    companion object {
        private const val NEW_INSTANCE_ARG = "CloudFileBottomSheetArg"

        fun newInstance(
            userEpisodeId: String,
            forceDark: Boolean = false,
            source: EpisodeViewSource = EpisodeViewSource.UNKNOWN,
        ) = CloudFileBottomSheetFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARG to Args(userEpisodeId, forceDark, source))
        }
    }

    @Parcelize
    private class Args(
        val episodeId: String,
        val forceDark: Boolean,
        val source: EpisodeViewSource,
    ) : Parcelable

    @Inject lateinit var downloadManager: DownloadManager

    @Inject lateinit var playbackManager: PlaybackManager

    @Inject lateinit var settings: Settings

    @Inject lateinit var theme: Theme

    @Inject lateinit var upNextQueue: UpNextQueue

    @Inject lateinit var warningsHelper: WarningsHelper

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    @Inject lateinit var bookmarkManager: BookmarkManager

    private var pocketCastsImageRequestFactory: PocketCastsImageRequestFactory? = null
    private val viewModel: CloudBottomSheetViewModel by viewModels()
    private var binding: BottomSheetCloudFileBinding? = null

    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARG, Args::class.java) })

    val activeTheme: Theme.ThemeType
        get() = if (args.forceDark && theme.isLightTheme) Theme.ThemeType.DARK else theme.activeTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (savedInstanceState == null) {
            analyticsTracker.track(AnalyticsEvent.USER_FILE_DETAIL_SHOWN, mapOf("source" to args.source.value))
        }
        if (!args.forceDark) {
            return super.onCreateDialog(savedInstanceState)
        }

        val context = ContextThemeWrapper(requireContext(), UR.style.ThemeDark)
        return BottomSheetDialog(context, UR.style.BottomSheetDialogThemeDark)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = BottomSheetCloudFileBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        analyticsTracker.track(AnalyticsEvent.USER_FILE_DETAIL_DISMISSED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.doOnLayout {
            val dialog = dialog as BottomSheetDialog
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
            val behavior = BottomSheetBehavior.from(bottomSheet!!)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.skipCollapsed = true
        }

        binding?.root?.setSystemWindowInsetToPadding(bottom = true)

        viewModel.setup(args.episodeId)
        viewModel.state.observe(
            viewLifecycleOwner,
            Observer { state ->
                val binding = binding ?: return@Observer

                val episode = state.episode
                val inUpNext = state.inUpNext
                val isPlaying = state.isPlaying

                binding.lblTitle.text = episode.title
                val btnPlay = binding.btnPlay
                val tintColor = view.context.getThemeColor(UR.attr.primary_icon_01)
                btnPlay.setCircleTintColor(tintColor)
                btnPlay.setPlaying(isPlaying, false)
                btnPlay.setOnPlayClicked {
                    if (!isPlaying) {
                        viewModel.playNow(episode, false)
                        dialog?.dismiss()
                    } else {
                        viewModel.pause()
                        btnPlay.setPlaying(false, animate = true)
                    }
                }

                val imgIconUpNext = binding.imgIconUpNext
                val lblUpNext = binding.lblUpNext
                val layoutUpNext = binding.layoutUpNext
                if (inUpNext) {
                    imgIconUpNext.setImageResource(IR.drawable.ic_upnext_remove)
                    lblUpNext.setText(LR.string.profile_cloud_remove_from_up_next)
                    layoutUpNext.setOnClickListener {
                        viewModel.removeFromUpNext(episode)
                        dialog?.dismiss()
                    }
                } else {
                    imgIconUpNext.setImageResource(IR.drawable.ic_upnext_playnext)
                    lblUpNext.setText(LR.string.profile_cloud_add_to_up_next)
                    layoutUpNext.setOnClickListener {
                        val upNextDialog = OptionsDialog()
                            .setIconColor(ThemeColor.primaryIcon01(theme.activeTheme))
                            .addCheckedOption(
                                LR.string.play_next,
                                imageId = IR.drawable.ic_upnext_playnext,
                                click = {
                                    viewModel.playNext(episode)
                                },
                            )
                            .addCheckedOption(
                                LR.string.play_last,
                                imageId = IR.drawable.ic_upnext_playlast,
                                click = {
                                    viewModel.playLast(episode)
                                },
                            )
                        activity?.supportFragmentManager?.let {
                            upNextDialog.show(it, "upnext")
                        }

                        dialog?.dismiss()
                    }
                }

                val imgIconPlayed = binding.imgIconPlayed
                val lblPlayed = binding.lblPlayed
                val layoutPlayed = binding.layoutPlayed
                if (episode.isFinished) {
                    imgIconPlayed.setImageResource(IR.drawable.ic_markasunplayed)
                    lblPlayed.setText(LR.string.mark_as_unplayed)
                    layoutPlayed.setOnClickListener {
                        viewModel.markAsUnplayed(episode)
                        dialog?.dismiss()
                    }
                } else {
                    imgIconPlayed.setImageResource(IR.drawable.ic_markasplayed)
                    lblPlayed.setText(LR.string.mark_as_played)
                    layoutPlayed.setOnClickListener {
                        viewModel.markAsPlayed(episode)
                        dialog?.dismiss()
                    }
                }

                val layoutBookmark = binding.layoutBookmark
                layoutBookmark.setOnClickListener {
                    dialog?.dismiss()
                    viewModel.trackOptionTapped(CloudBottomSheetViewModel.BOOKMARKS)
                    BookmarksContainerFragment.newInstance(
                        episodeUuid = args.episodeId,
                        sourceView = SourceView.FILES,
                    ).show(parentFragmentManager, "bookmarks_container")
                }

                val errorLayout = binding.errorLayout
                val lblError = binding.lblError
                val lblErrorDetail = binding.lblErrorDetail
                if (episode.downloadErrorDetails != null) {
                    errorLayout.visibility = View.VISIBLE
                    lblError.setText(LR.string.profile_cloud_download_failed)
                    lblErrorDetail.text = episode.downloadErrorDetails
                } else if (episode.uploadErrorDetails != null) {
                    errorLayout.visibility = View.VISIBLE
                    lblError.setText(LR.string.profile_cloud_upload_failed)
                    lblErrorDetail.text = episode.uploadErrorDetails
                } else if (episode.playErrorDetails != null) {
                    errorLayout.visibility = View.VISIBLE
                    lblError.setText(LR.string.profile_cloud_playback_failed)
                    lblErrorDetail.text = episode.playErrorDetails
                } else {
                    errorLayout.visibility = View.GONE
                }
                val userBookmarksObservable = bookmarkManager.findUserEpisodesBookmarksFlow().asObservable()
                binding.fileStatusIconsView.setup(
                    episode = episode,
                    downloadProgressUpdates = downloadManager.progressUpdateRelay,
                    playbackStateUpdates = playbackManager.playbackStateRelay,
                    upNextChangesObservable = upNextQueue.changesObservable,
                    userBookmarksObservable = userBookmarksObservable,
                    hideErrorDetails = true,
                    tintColor = tintColor,
                )

                binding.lblCloud.text = when (episode.serverStatus) {
                    UserEpisodeServerStatus.LOCAL -> getString(LR.string.profile_cloud_upload)
                    UserEpisodeServerStatus.UPLOADING, UserEpisodeServerStatus.WAITING_FOR_WIFI, UserEpisodeServerStatus.QUEUED -> getString(LR.string.profile_cloud_cancel_upload)
                    UserEpisodeServerStatus.UPLOADED -> getString(
                        if (episode.isDownloaded) {
                            LR.string.profile_cloud_remove
                        } else if (episode.isDownloading) {
                            LR.string.cancel_download
                        } else {
                            LR.string.download
                        },
                    )
                    UserEpisodeServerStatus.MISSING -> ""
                }

                val cloudRes = when (episode.serverStatus) {
                    UserEpisodeServerStatus.LOCAL, UserEpisodeServerStatus.MISSING -> PR.drawable.ic_upload_file
                    UserEpisodeServerStatus.UPLOADING, UserEpisodeServerStatus.WAITING_FOR_WIFI, UserEpisodeServerStatus.QUEUED -> IR.drawable.ic_downloading
                    UserEpisodeServerStatus.UPLOADED -> if (episode.isDownloaded) IR.drawable.ic_remove_from_cloud else IR.drawable.ic_download
                }
                binding.imgIconCloud.setImageResource(cloudRes)

                binding.layoutCloud.setOnClickListener {
                    when (episode.serverStatus) {
                        UserEpisodeServerStatus.LOCAL, UserEpisodeServerStatus.MISSING -> upload(episode, Network.isUnmeteredConnection(binding.layoutCloud.context))
                        UserEpisodeServerStatus.UPLOADING, UserEpisodeServerStatus.WAITING_FOR_WIFI, UserEpisodeServerStatus.QUEUED -> viewModel.cancelUpload(episode)
                        UserEpisodeServerStatus.UPLOADED -> if (episode.isDownloaded) {
                            viewModel.removeEpisode(episode)
                        } else if (episode.isDownloading) {
                            viewModel.cancelDownload(episode)
                        } else {
                            download(episode, Network.isUnmeteredConnection(binding.layoutCloud.context))
                        }
                    }

                    dialog?.dismiss()
                }

                binding.layoutLockedCloud.setOnClickListener {
                    viewModel.trackOptionTapped(UPLOAD_UPGRADE_REQUIRED)
                    OnboardingLauncher.openOnboardingFlow(
                        requireActivity(),
                        OnboardingFlow.Upsell(OnboardingUpgradeSource.FILES),
                    )
                }

                binding.layoutDelete.setOnClickListener {
                    val deleteState = viewModel.getDeleteStateOnDeleteClick(episode)
                    val confirmationDialog = CloudDeleteHelper.getDeleteDialog(episode, deleteState, viewModel::deleteEpisode, resources)
                    confirmationDialog.show(parentFragmentManager, "delete_confirm")
                    dialog?.dismiss()
                }

                binding.layoutEdit.setOnClickListener {
                    activity?.let { activity ->
                        viewModel.trackOptionTapped(EDIT)
                        val intent = AddFileActivity.newEditInstance(activity, episode.uuid)
                        activity.startActivity(intent)

                        dialog?.dismiss()
                    }
                }
                pocketCastsImageRequestFactory?.create(episode, useEpisodeArtwork = false)?.loadInto(binding.imgFile)
            },
        )

        viewModel.signInState.observe(
            viewLifecycleOwner,
            Observer { signInState ->
                val binding = binding ?: return@Observer
                val layoutCloud = binding.layoutCloud
                val layoutLockedCloud = binding.layoutLockedCloud
                when (signInState) {
                    is SignInState.SignedIn -> {
                        if (signInState.subscription != null) {
                            layoutCloud.isVisible = true
                            layoutLockedCloud.isVisible = false
                        } else {
                            layoutCloud.isVisible = false
                            layoutLockedCloud.isVisible = true
                        }
                    }
                    else -> {
                        layoutCloud.isVisible = false
                        layoutLockedCloud.isVisible = true
                    }
                }
            },
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        pocketCastsImageRequestFactory = PocketCastsImageRequestFactory(context).themed().smallSize()
    }

    override fun onDetach() {
        super.onDetach()
        pocketCastsImageRequestFactory = null
    }

    fun download(episode: UserEpisode, isOnWifi: Boolean) {
        viewModel.trackOptionTapped(DOWNLOAD)
        if (settings.warnOnMeteredNetwork.value && !isOnWifi) {
            warningsHelper.downloadWarning(args.episodeId, "user episode sheet")
                .show(parentFragmentManager, "download_warning")
        } else {
            viewModel.download(episode)
        }
    }

    private fun upload(episode: UserEpisode, isOnWifi: Boolean) {
        viewModel.trackOptionTapped(UPLOAD)
        if (settings.warnOnMeteredNetwork.value && !isOnWifi) {
            warningsHelper.uploadWarning(args.episodeId, source = SourceView.FILES)
                .show(parentFragmentManager, "upload_warning")
        } else {
            viewModel.uploadEpisode(episode)
        }
    }
}
