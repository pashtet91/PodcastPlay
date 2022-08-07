package com.pashtet.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.pashtet.myapplication.db.PodDao
import com.pashtet.myapplication.db.PodPlayDatabase
import com.pashtet.myapplication.model.Episode
import com.pashtet.myapplication.model.Podcast
import com.pashtet.myapplication.repo.ItunesRepo
import com.pashtet.myapplication.repo.PodRepo
import com.pashtet.myapplication.service.ItunesService
import com.pashtet.myapplication.service.RssFeedService
import com.pashtet.myapplication.util.DateUtils
import kotlinx.coroutines.launch
import java.util.*

class PodViewModel(application: Application) :
        AndroidViewModel(application) {

    private val podDao : PodDao = PodPlayDatabase
                                    .getInstance(application, viewModelScope)
                                    .podcastDao()

    val podcastRepo: PodRepo = PodRepo(RssFeedService.instance, podDao)
    var activePodcastViewData: PodViewData? = null
    var livePodcastSummaryData: LiveData<List<MainViewModel.PodSummaryViewData>>? = null

    private var activePodcast: Podcast? = null
    private val _podLiveData = MutableLiveData<PodViewData?>()
    val podLiveData: LiveData<PodViewData?> = _podLiveData

    data class PodViewData(
        var subscribed: Boolean = false,
        var feedTitle: String? = "",
        var feedUrl: String? = "",
        var feedDesc: String? = "",
        var imageUrl: String? = "",
        var episodes: List<EpisodeViewData>
    )

    data class EpisodeViewData (
        var guid: String? = "",
        var title: String? = "",
        var description: String? = "",
        var mediaUrl: String? = "",
        var releaseDate: Date? = null,
        var duration: String? = ""
    )

    suspend fun getPodcast(podcastSummaryViewData:
                   MainViewModel.PodSummaryViewData
    ){

        podcastSummaryViewData.feedUrl?.let{
            url-> viewModelScope.launch {
                podcastRepo?.getPodcast(url)?.let{
                    it.feedTitle = podcastSummaryViewData.name ?: ""
                    it.imageUrl = podcastSummaryViewData.imageUrl ?: ""
                    _podLiveData.value = podcastToPodcastView(it)
                    activePodcast = it
                } ?: run{
                    _podLiveData.value = null
                }
            }
        } ?: run{
            _podLiveData.value = null
        }
    }

    fun getPodcasts():LiveData<List<MainViewModel.PodSummaryViewData>>? {
        val repo = podcastRepo ?: return null
        if(livePodcastSummaryData == null){
            val liveData = repo.getAll()
            livePodcastSummaryData = Transformations.map(liveData){
                podcastList ->
                podcastList.map{
                    podcast ->
                    podcastToSummaryView(podcast)
                }
            }
        }
        return livePodcastSummaryData
    }

    fun saveActivePodcast(){
        val repo = podcastRepo ?: return
        activePodcast?.let{
            repo.save(it)
        }
    }

    fun deleteActivePodcast(){
        val repo = podcastRepo ?: return
        activePodcast?.let{
            repo.delete(it)
        }
    }

    suspend fun getRss(feedUrl: String?){

        if (feedUrl != null) {
            podcastRepo.getRss(feedUrl)
        }
    }

    private fun episodesToEpisodesView(episodes: List<Episode>):
            List<EpisodeViewData> {
        return episodes.map {
            EpisodeViewData(
                it.guid,
                it.title,
                it.description,
                it.mediaUrl,
                it.releaseDate,
                it.duration
            )
        }
    }

    private fun podcastToPodcastView(podcast: Podcast):
            PodViewData {
        return PodViewData(
            podcast.id != null,
            podcast.feedTitle,
            podcast.feedUrl,
            podcast.feedDesc,
            podcast.imageUrl,
            episodesToEpisodesView(podcast.episodes)
        )
    }

    private fun podcastToSummaryView(podcast:Podcast):
            MainViewModel.PodSummaryViewData{

        return MainViewModel.PodSummaryViewData(
            podcast.feedTitle,
            DateUtils.dateToShortDate(podcast.lastUpdated),
            podcast.imageUrl,
            podcast.feedUrl
        )


    }
}