package com.pashtet.myapplication.service

data class PodcastResponce(
    val resCount: Int,
    val results: List<ItunesPodcast>
){
    data class ItunesPodcast(
        val collectionCensoredName: String,
        val feedUrl: String,
        val artworkUrl30: String,
        val releaseDate: String
    )
}
