package com.pashtet.myapplication.db

import androidx.annotation.NonNull
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.pashtet.myapplication.model.Episode
import com.pashtet.myapplication.model.Podcast


@Dao
interface PodDao {
    @Query("SELECT * FROM Podcast ORDER BY FeedTitle")
    fun loadPodcasts(): LiveData<List<Podcast>>

    @Query("SELECT * FROM Podcast ORDER BY FeedTitle")
    fun loadPodcastsStatic(): List<Podcast>

    @Query("SELECT * FROM Podcast WHERE feedUrl = :url")
    suspend fun loadPodcast(url:String): Podcast?

    @Query("SELECT * FROM Episode WHERE podcastId = :podcastId " +
            "ORDER BY releaseDate DESC")
    suspend fun loadEpisodes(podcastId: Long): List<Episode>

    @Insert(onConflict = REPLACE)
    suspend fun insertPodcast(podcast: Podcast):Long

    @Insert(onConflict = REPLACE)
    suspend fun insertEpisode(episode:Episode):Long

    @Delete
    fun deletePodcast(podcast: Podcast)
}