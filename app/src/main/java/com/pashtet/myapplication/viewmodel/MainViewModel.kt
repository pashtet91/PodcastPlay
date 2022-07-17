package com.pashtet.myapplication.viewmodel

import android.app.Application
import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.*
import com.pashtet.myapplication.repo.ItunesRepo
import com.pashtet.myapplication.repo.PodRepo
import com.pashtet.myapplication.service.FeedService
import com.pashtet.myapplication.service.ItunesService
import com.pashtet.myapplication.service.PodcastResponce
import com.pashtet.myapplication.service.RssFeedService
import com.pashtet.myapplication.util.DateUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val itunesService = ItunesService.instance
    val itunesRepo = ItunesRepo(itunesService)

   suspend fun searchPodcastsByTerm(term:String):List<PodSummaryViewData> {

            val results = itunesRepo.searchByTerm(term)

            if(results != null && results.isSuccessful) {
                val podcasts = results.body()?.results
                if (!podcasts.isNullOrEmpty()) {
                    return podcasts.map{ pod ->
                        podToPodSummaryView(pod)
                    }
                }
                //Log.i(TAG, "Results = " + results.body())
            }

        return emptyList()
    }

    private fun podToPodSummaryView(pod:PodcastResponce.ItunesPodcast)
    : PodSummaryViewData{
        return PodSummaryViewData(
            pod.collectionCensoredName,
            DateUtils.jsonDateToShortDate(pod.releaseDate),//pod.releaseDate,
            pod.artworkUrl30,
            pod.feedUrl
        )
    }



    data class PodSummaryViewData(
        var name: String? = "",
        var lastUpdated: String? = "",
        var imageUrl: String? = "",
        var feedUrl: String? = ""
    )
}