package com.pashtet.myapplication.repo

import com.pashtet.myapplication.service.ItunesService

class ItunesRepo(private val itunesService: ItunesService) {
    suspend fun searchByTerm(term:String) =
        itunesService.searchPodcastByTerm(term)
}