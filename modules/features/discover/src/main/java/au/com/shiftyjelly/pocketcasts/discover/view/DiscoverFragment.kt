package au.com.shiftyjelly.pocketcasts.discover.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.discover.databinding.FragmentDiscoverBinding
import au.com.shiftyjelly.pocketcasts.discover.viewmodel.DiscoverState
import au.com.shiftyjelly.pocketcasts.discover.viewmodel.DiscoverViewModel
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeContainerFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastFragment
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.search.SearchFragment
import au.com.shiftyjelly.pocketcasts.servers.cdn.StaticServiceManagerImpl
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverEpisode
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRegion
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.servers.model.NetworkLoadableList
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DiscoverFragment : BaseFragment(), DiscoverAdapter.Listener, RegionSelectFragment.Listener {

    @Inject lateinit var settings: Settings

    @Inject lateinit var staticServiceManager: StaticServiceManagerImpl

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    private val viewModel: DiscoverViewModel by viewModels()
    private var adapter: DiscoverAdapter? = null
    private var binding: FragmentDiscoverBinding? = null

    override fun onPause() {
        super.onPause()
        adapter?.enablePageTracking(enable = false)
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
    }

    override fun onResume() {
        super.onResume()
        adapter?.enablePageTracking(enable = true)
    }

    override fun onPodcastClicked(podcast: DiscoverPodcast, listUuid: String?, isFeatured: Boolean) {
        val fragment = PodcastFragment.newInstance(podcastUuid = podcast.uuid, fromListUuid = listUuid, sourceView = SourceView.DISCOVER, featuredPodcast = isFeatured)
        (activity as FragmentHostListener).addFragment(fragment)
    }

    override fun onPodcastSubscribe(podcast: DiscoverPodcast, listUuid: String?) {
        viewModel.subscribeToPodcast(podcast)
        analyticsTracker.track(
            AnalyticsEvent.PODCAST_SUBSCRIBED,
            mapOf(SOURCE_KEY to SourceView.DISCOVER.analyticsValue, UUID_KEY to podcast.uuid),
        )
    }

    override fun onPodcastListClicked(list: NetworkLoadableList) {
        val transformedList = viewModel.transformNetworkLoadableList(list, resources) // Replace any [regionCode] etc references
        val listId = list.listUuid
        if (listId != null) {
            analyticsTracker.track(AnalyticsEvent.DISCOVER_LIST_SHOW_ALL_TAPPED, mapOf(LIST_ID_KEY to listId))
        } else {
            analyticsTracker.track(AnalyticsEvent.DISCOVER_SHOW_ALL_TAPPED, mapOf(LIST_ID_KEY to transformedList.inferredId()))
        }
        if (list is DiscoverCategory) {
            trackCategoryShownImpression(list)
        }

        if (list.expandedStyle is ExpandedStyle.GridList) {
            val fragment = PodcastGridFragment.newInstance(transformedList)
            (activity as FragmentHostListener).addFragment(fragment)
        } else {
            val fragment = PodcastListFragment.newInstance(transformedList)
            (activity as FragmentHostListener).addFragment(fragment)
        }
    }

    override fun onEpisodeClicked(episode: DiscoverEpisode, listUuid: String?) {
        val fragment = EpisodeContainerFragment.newInstance(
            episodeUuid = episode.uuid,
            source = EpisodeViewSource.DISCOVER,
            podcastUuid = episode.podcast_uuid,
            fromListUuid = listUuid,
        )
        fragment.show(parentFragmentManager, "episode_card")
    }

    override fun onEpisodePlayClicked(episode: DiscoverEpisode) {
        viewModel.findOrDownloadEpisode(episode) { databaseEpisode ->
            viewModel.playEpisode(databaseEpisode)
        }
    }

    override fun onEpisodeStopClicked() {
        viewModel.stopPlayback()
    }

    override fun onSearchClicked() {
        val searchFragment = SearchFragment.newInstance(
            floating = true,
            onlySearchRemote = true,
            source = SourceView.DISCOVER,
        )
        (activity as FragmentHostListener).addFragment(searchFragment, onTop = true)
        binding?.recyclerView?.smoothScrollToPosition(0)
    }

    override fun onCategoryClick(selectedCategory: CategoryPill, onCategorySelectionSuccess: () -> Unit) {
        val categoryWithRegionUpdated =
            viewModel.transformNetworkLoadableList(selectedCategory.discoverCategory, resources)

        viewModel.filterPodcasts(categoryWithRegionUpdated.source) {
            val podcasts = it.podcasts

            val mostPopularPodcasts =
                MostPopularPodcastsByCategoryRow(it.listId, it.title, podcasts.take(MOST_POPULAR_PODCASTS))

            val remainingPodcasts =
                RemainingPodcastsByCategoryRow(it.listId, it.title, podcasts.drop(MOST_POPULAR_PODCASTS))

            updateDiscoverWithCategorySelected(selectedCategory.discoverCategory, mostPopularPodcasts, remainingPodcasts)

            onCategorySelectionSuccess()
        }

        trackCategoryShownImpression(selectedCategory.discoverCategory)
    }
    override fun onAllCategoriesClick(source: String, onCategorySelectionSuccess: (CategoryPill) -> Unit, onCategorySelectionCancel: () -> Unit) {
        viewModel.loadCategories(source) { categories ->
            CategoriesBottomSheet(
                categories = categories,
                onCategoryClick = {
                    trackDropDownListCategoryPickImpression(it.discoverCategory.name, it.discoverCategory.id)
                    this.onCategoryClick(it) { onCategorySelectionSuccess(it) }
                },
                onCategorySelectionCancel = onCategorySelectionCancel,
            ).show(childFragmentManager, "categories_bottom_sheet")
        }
    }
    override fun onClearCategoryFilterClick(source: String, onCategoryClearSuccess: (List<CategoryPill>) -> Unit) {
        viewModel.loadCategories(source) { categories ->
            onCategoryClearSuccess(categories)
            viewModel.loadData(resources) // Reload discover
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDiscoverBinding.inflate(inflater, container, false)

        if (viewModel.state.value !is DiscoverState.DataLoaded) {
            viewModel.loadData(resources)
        }

        viewModel.onShown()

        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = binding?.recyclerView ?: return
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        if (adapter == null) {
            adapter = DiscoverAdapter(
                context = requireContext(),
                service = viewModel.repository,
                staticServiceManager = staticServiceManager,
                listener = this,
                theme = theme,
                loadPodcastList = viewModel::loadPodcastList,
                loadCarouselSponsoredPodcastList = viewModel::loadCarouselSponsoredPodcasts,
                loadCategories = viewModel::loadCategories,
                analyticsTracker = analyticsTracker,
            )
        }
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = null

        viewModel.state.observe(
            viewLifecycleOwner,
            Observer { state ->
                val binding = binding ?: return@Observer
                when (state) {
                    is DiscoverState.DataLoaded -> {
                        binding.errorLayout.isVisible = false
                        binding.recyclerView.isVisible = true
                        binding.loading.isVisible = false

                        val content = state.data.plus(ChangeRegionRow(state.selectedRegion))
                        val onChangeRegion: () -> Unit = {
                            val fragment = RegionSelectFragment.newInstance(state.regionList, state.selectedRegion)
                            (activity as FragmentHostListener).addFragment(fragment)
                            fragment.listener = this
                        }
                        adapter?.onChangeRegion = onChangeRegion

                        val updatedContent = updateDiscoverRowsAndRemoveCategoryAds(content, state.selectedRegion.code)

                        adapter?.submitList(updatedContent)
                    }
                    is DiscoverState.Error -> {
                        binding.errorLayout.isVisible = true
                        binding.recyclerView.isVisible = false
                        binding.loading.isVisible = false

                        binding.btnRetry.setOnClickListener { viewModel.loadData(resources) }
                    }
                    is DiscoverState.Loading -> {
                        binding.errorLayout.isVisible = false
                        binding.recyclerView.isVisible = false
                        binding.loading.isVisible = true
                    }
                    is DiscoverState.FilteringPodcastsByCategory -> {
                        binding.loading.isVisible = true
                        binding.errorLayout.isVisible = false
                    }
                    is DiscoverState.PodcastsFilteredByCategory -> {
                        binding.errorLayout.isVisible = false
                        binding.recyclerView.isVisible = true
                        binding.loading.isVisible = false
                    }
                }
            },
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settings.bottomInset.collect {
                    binding?.recyclerView?.updatePadding(bottom = it)
                }
            }
        }
    }

    override fun onRegionSelected(region: DiscoverRegion) {
        viewModel.changeRegion(region, resources)

        @Suppress("DEPRECATION")
        activity?.onBackPressed()

        binding?.recyclerView?.scrollToPosition(0)
    }

    private fun updateDiscoverRowsAndRemoveCategoryAds(content: List<Any>, region: String): MutableList<Any> {
        val mutableContentList = content.toMutableList()

        val categoriesIndex = mutableContentList.indexOfFirst { it is DiscoverRow && it.type is ListType.Categories }

        if (categoriesIndex != -1) {
            val categoriesItem = mutableContentList[categoriesIndex] as DiscoverRow
            mutableContentList[categoriesIndex] = categoriesItem.copy(regionCode = region)
        }

        mutableContentList.removeAll { it is DiscoverRow && it.categoryId != null } // Remove ads exclusive to category view

        return mutableContentList
    }

    private fun updateDiscoverWithCategorySelected(
        category: DiscoverCategory,
        mostPopularPodcasts: MostPopularPodcastsByCategoryRow,
        remainingPodcasts: RemainingPodcastsByCategoryRow,
    ) {
        adapter?.currentList?.let { discoverList ->
            val updatedList = discoverList.filter { it is DiscoverRow && it.type is ListType.Categories }.toMutableList()

            // First, we insert the most popular podcasts.
            updatedList.add(mostPopularPodcasts)

            // If there is ad, we add it.
            viewModel.getAdForCategoryView(category.id)?.let {
                updatedList.add(CategoryAdRow(categoryId = category.id, categoryName = category.name, region = viewModel.currentRegionCode, discoverRow = it))
            }

            // Lastly, we add the remaining podcast list.
            updatedList.add(remainingPodcasts)

            adapter?.submitList(updatedList)
        }
    }
    private fun trackCategoryShownImpression(category: DiscoverCategory) {
        viewModel.currentRegionCode?.let {
            analyticsTracker.track(
                AnalyticsEvent.DISCOVER_CATEGORY_SHOWN,
                mapOf(
                    NAME_KEY to category.name,
                    REGION_KEY to it,
                    ID_KEY to category.id,
                ),
            )
        }
    }
    private fun trackDropDownListCategoryPickImpression(name: String, id: Int) {
        viewModel.currentRegionCode?.let {
            analyticsTracker.track(
                AnalyticsEvent.DISCOVER_CATEGORIES_PICKER_PICK,
                mapOf(
                    NAME_KEY to name,
                    REGION_KEY to it,
                    ID_KEY to id,
                ),
            )
        }
    }
    companion object {
        private const val ID_KEY = "id"
        private const val NAME_KEY = "name"
        private const val REGION_KEY = "region"
        private const val MOST_POPULAR_PODCASTS = 5
        const val LIST_ID_KEY = "list_id"
        const val PODCAST_UUID_KEY = "podcast_uuid"
        const val EPISODE_UUID_KEY = "episode_uuid"
        const val SOURCE_KEY = "source"
        const val UUID_KEY = "uuid"
    }
}
