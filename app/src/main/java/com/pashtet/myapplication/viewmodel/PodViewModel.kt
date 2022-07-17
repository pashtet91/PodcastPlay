package com.pashtet.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pashtet.myapplication.model.Episode
import com.pashtet.myapplication.model.Podcast
import com.pashtet.myapplication.repo.ItunesRepo
import com.pashtet.myapplication.repo.PodRepo
import com.pashtet.myapplication.service.ItunesService
import com.pashtet.myapplication.service.RssFeedService
import kotlinx.coroutines.launch
import java.util.*

class PodViewModel(application: Application) :
        AndroidViewModel(application) {

    val podcastRepo: PodRepo = PodRepo(RssFeedService.instance)
    var activePodcastViewData: PodViewData? = null

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

    fun getPodcast(podcastSummaryViewData:
                   MainViewModel.PodSummaryViewData
    ){

        podcastSummaryViewData.feedUrl?.let{
            url-> viewModelScope.launch {
                podcastRepo?.getPodcast(url)?.let{
                    it.feedTitle = podcastSummaryViewData.name ?: ""
                    it.imageUrl = podcastSummaryViewData.imageUrl ?: ""
                    _podLiveData.value = podcastToPodcastView(it)
                } ?: run{
                    _podLiveData.value = null
                }
            }
        } ?: run{
            _podLiveData.value = null
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
            false,
            podcast.feedTitle,
            podcast.feedUrl,
            podcast.feedDesc,
            podcast.imageUrl,
            episodesToEpisodesView(podcast.episodes)
        )
    }
}