package com.pashtet.myapplication.ui

import androidx.appcompat.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.*
import com.pashtet.myapplication.R
import com.pashtet.myapplication.adapter.PodListAdapter
import com.pashtet.myapplication.databinding.ActivityMainBinding
import com.pashtet.myapplication.repo.ItunesRepo
import com.pashtet.myapplication.service.ItunesService
import com.pashtet.myapplication.viewmodel.MainViewModel
import com.pashtet.myapplication.viewmodel.PodViewModel
import com.pashtet.myapplication.worker.EpisodeUpdateWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), PodListAdapter.PodListAdapterListener,
                PodDetailsFragment.OnPodcastDetailsListener{

    val TAG = javaClass.simpleName
    private val mainViewModel by viewModels<MainViewModel>()
    private lateinit var podListAdapter: PodListAdapter
    private lateinit var binding: ActivityMainBinding
    private lateinit var searchMenuItem: MenuItem

    private val podViewModel by viewModels<PodViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        updateControls()
        createSubscription()
        searchPodcastByTerm()

        handleIntent(intent)
        addBackStackListener()
        setupPodcastListView()

        scheduleJobs()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_search, menu)

        searchMenuItem = menu.findItem(R.id.search_item)
        searchMenuItem.setOnActionExpandListener(object:MenuItem.OnActionExpandListener{
            override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                showSubscribedPodcasts()
                return true
            }
        })
        val searchView = searchMenuItem.actionView as SearchView

        val searchManager = getSystemService(Context.SEARCH_SERVICE)
            as SearchManager

        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        if(supportFragmentManager.backStackEntryCount > 0) {
            binding.podcastRecyclerView.visibility = View.INVISIBLE
        }

        return true
    }

    private fun setupToolbar(){
        setSupportActionBar(binding.toolbar)
    }

    private fun updateControls(){
        binding.podcastRecyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(this)
        binding.podcastRecyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            binding.podcastRecyclerView.context,
            layoutManager.orientation)

        binding.podcastRecyclerView.addItemDecoration(dividerItemDecoration)

        podListAdapter = PodListAdapter(null, this, this)
        binding.podcastRecyclerView.adapter = podListAdapter

    }

    private fun searchPodcastByTerm(term: String = "Android"){
        showProgressBar()
        lifecycleScope.launch {
            val results = mainViewModel.searchPodcastsByTerm(term)
            Log.d("Pod results", results.toString())
            withContext(Dispatchers.Main){
                hideProgressBar()
                binding.toolbar.title = term
                podListAdapter.setListData(results)
            }
        }
    }

    override fun onNewIntent(intent: Intent){
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent:Intent){
        if(Intent.ACTION_SEARCH == intent.action){
            val query = intent.getStringExtra(SearchManager.QUERY)
                ?: return
                        searchPodcastByTerm(query)
        }

        val podcastFeedUrl = intent.getStringExtra(EpisodeUpdateWorker.EXTRA_FEED_URL)
        if(podcastFeedUrl != null){
            podViewModel.viewModelScope.launch {
                val podcasSummaryViewData = podViewModel.setActivePodcast(podcastFeedUrl)
                podcasSummaryViewData?.let{
                    podcastSummaryView -> onShowDetails(podcastSummaryView)
                }
            }
        }
    }

    override fun onShowDetails(podSummaryViewData: MainViewModel.PodSummaryViewData) {
        podSummaryViewData.feedUrl?.let {
            showProgressBar()
            podViewModel.viewModelScope.launch (context = Dispatchers.Main) {
                podViewModel.getPodcast(podSummaryViewData)
                //hideProgressBar()
                //showDetailsFragment()
            }

        }
    }

    private fun createSubscription(){
        podViewModel.podLiveData.observe(this,{
            hideProgressBar()
            if(it != null){
                showDetailsFragment()
            }else{
                showError("Error loading feed")
            }
        })
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }
    private fun hideProgressBar() {
        binding.progressBar.visibility = View.INVISIBLE
    }

    private fun showError(message: String){
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok_button), null)
            .create()
            .show()
    }

    private fun showSubscribedPodcasts(){
        val podcasts = podViewModel.getPodcasts()?.value
        if(podcasts != null){
            binding.toolbar.title = getString(R.string.subscribed_podcasts)
            podListAdapter.setListData(podcasts)
        }
    }

    private fun setupPodcastListView(){
        podViewModel.getPodcasts()?.observe(this, {
            if(it != null){
                showSubscribedPodcasts()
            }
        })
    }

    private fun createPodDetailsFragment(): PodDetailsFragment{
        var podDetailsFragment = supportFragmentManager.findFragmentByTag(TAG_DETAILS_FRAGMENT)
            as PodDetailsFragment?

        if(podDetailsFragment == null)
            podDetailsFragment = PodDetailsFragment.newInstance()

        return podDetailsFragment

    }

    private fun showDetailsFragment(){
        val podDetailsFragment = createPodDetailsFragment()

        supportFragmentManager.beginTransaction().add(R.id.podDetailsContainer,
        podDetailsFragment, TAG_DETAILS_FRAGMENT)
            .addToBackStack("DetailsFragment").commit()
        binding.podcastRecyclerView.visibility = View.INVISIBLE
        searchMenuItem.isVisible = false
    }

    private fun addBackStackListener(){
        supportFragmentManager.addOnBackStackChangedListener {
            if(supportFragmentManager.backStackEntryCount == 0){
                binding.podcastRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    override fun onSubscribe() {
       podViewModel.saveActivePodcast()
        supportFragmentManager.popBackStack()
    }

    override fun onUnsubscribe() {
        podViewModel.deleteActivePodcast()
        supportFragmentManager.popBackStack()
    }

    private fun scheduleJobs(){
        val constraints: Constraints = Constraints.Builder().apply{
            setRequiredNetworkType(NetworkType.CONNECTED)
            setRequiresCharging(true)
        }.build()

        val request = PeriodicWorkRequestBuilder<EpisodeUpdateWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            TAG_EPISODE_UPDATE_JOB,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    private fun createEpisodePlayerFragment(): EpisodePlayerFragment{
        var episodePlayerFragment = supportFragmentManager.findFragmentByTag(TAG_PLAYER_FRAGMENT) as
                EpisodePlayerFragment?
        if(episodePlayerFragment == null) {
            episodePlayerFragment = EpisodePlayerFragment.newInstance()
        }

        return episodePlayerFragment
    }

    private fun showPlayerFragment() {
        val episodePlayerFragment = createEpisodePlayerFragment()

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.podDetailsContainer, episodePlayerFragment, TAG_PLAYER_FRAGMENT)
            .addToBackStack("PlayerFragment")
            .commit()

        binding.podcastRecyclerView.visibility = View.INVISIBLE
        searchMenuItem.isVisible= false
    }

    override fun onShowEpisodePlayer(episodeViewData: PodViewModel.EpisodeViewData) {
        podViewModel.activeEpisodeViewData = episodeViewData
        showPlayerFragment()
    }

    companion object{
        private const val TAG_DETAILS_FRAGMENT = "DetailsFragment"
        private const val TAG_EPISODE_UPDATE_JOB =
            "com.pashtet.podcastplay.episodes"
        private const val TAG_PLAYER_FRAGMENT = "PlayerFragment"
    }
}