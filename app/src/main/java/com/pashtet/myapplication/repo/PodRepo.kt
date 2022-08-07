package com.pashtet.myapplication.repo

import androidx.lifecycle.LiveData
import com.pashtet.myapplication.db.PodDao
import com.pashtet.myapplication.model.Episode
import com.pashtet.myapplication.model.Podcast
import com.pashtet.myapplication.service.RssFeedResponse
import com.pashtet.myapplication.service.RssFeedService
import com.pashtet.myapplication.util.DateUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PodRepo(private var rssFeedService: RssFeedService,
              private var podDao:PodDao) {
    //val rssFeedService = RssFeedService.instance
     suspend fun getPodcast(feedUrl: String): Podcast? {
        var podcast: Podcast? = null
        val podcastLocal = podDao.loadPodcast(feedUrl)
        if(podcastLocal != null){
            podcastLocal.id?.let{
                podcastLocal.episodes = podDao.loadEpisodes(it)
                return podcastLocal
            }
        }

         val feedResponse = rssFeedService.getFeed(feedUrl)
         if(feedResponse != null)
             podcast = rssResponseToPodcast(feedUrl, "", feedResponse)

         return  podcast
        //return Podcast(feedUrl, "No Name","No description", "No image")
    }

    suspend fun getRss(feedUrl: String){

        rssFeedService.getFeed(feedUrl)
    }

    private fun rssItemsToEpisodes(episodeResponse: List<RssFeedResponse.EpisodeResponse>)
        : List<Episode>{
        return episodeResponse.map{
            Episode(
                it.guid ?: "",
                null,
                it.title ?: "",
                it.description ?: "",
                it.url ?: "",
                it.type ?: "",
                DateUtils.xmlDateToDate(it.pubDate),
                it.duration ?: ""
            )
        }
    }
    private fun rssResponseToPodcast(feedUrl: String, imageUrl:String, rssResponce: RssFeedResponse)
        : Podcast?{
        val items = rssResponce.episodes ?: return null
        val description = if(rssResponce.description == "")
            rssResponce.summary else rssResponce.description

        return Podcast(null, feedUrl, rssResponce.title, description,
            imageUrl, rssResponce.lastUpdated, episodes = rssItemsToEpisodes(items))
    }

    fun save(podcast: Podcast){
        GlobalScope.launch {
            val podcastId = podDao.insertPodcast(podcast)
            for (episode in podcast.episodes){
                episode.podcastId = podcastId
                podDao.insertEpisode(episode)
            }
        }
    }

    fun delete(podcast: Podcast){
        GlobalScope.launch {
            podDao.deletePodcast(podcast)
        }
    }

    fun getAll(): LiveData<List<Podcast>>{
        return podDao.loadPodcasts()
    }
}