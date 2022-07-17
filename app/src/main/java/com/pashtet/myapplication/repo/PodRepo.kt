package com.pashtet.myapplication.repo

import com.pashtet.myapplication.model.Episode
import com.pashtet.myapplication.model.Podcast
import com.pashtet.myapplication.service.FeedService
import com.pashtet.myapplication.service.RssFeedResponse
import com.pashtet.myapplication.service.RssFeedService
import com.pashtet.myapplication.util.DateUtils

class PodRepo(private var rssFeedService: RssFeedService) {
    //val rssFeedService = RssFeedService.instance
     suspend fun getPodcast(feedUrl: String): Podcast? {
        var podcast: Podcast? = null

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

        return Podcast(feedUrl, rssResponce.title, description,
            imageUrl, rssResponce.lastUpdated, episodes = rssItemsToEpisodes(items))
    }
}