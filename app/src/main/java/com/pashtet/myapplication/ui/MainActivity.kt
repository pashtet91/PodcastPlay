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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.pashtet.myapplication.R
import com.pashtet.myapplication.adapter.PodListAdapter
import com.pashtet.myapplication.databinding.ActivityMainBinding
import com.pashtet.myapplication.repo.ItunesRepo
import com.pashtet.myapplication.service.ItunesService
import com.pashtet.myapplication.viewmodel.MainViewModel
import com.pashtet.myapplication.viewmodel.PodViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), PodListAdapter.PodListAdapterListener {

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

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_search, menu)

        searchMenuItem = menu.findItem(R.id.search_item)
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
    }

    override fun onShowDetails(podSummaryViewData: MainViewModel.PodSummaryViewData) {
        podSummaryViewData.feedUrl?.let {
            showProgressBar()
            podViewModel.getPodcast(podSummaryViewData)
            hideProgressBar()

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

    companion object{
        private const val TAG_DETAILS_FRAGMENT = "DetailsFragment"
    }
}