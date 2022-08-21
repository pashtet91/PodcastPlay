package com.pashtet.myapplication.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pashtet.myapplication.R
import com.pashtet.myapplication.db.PodPlayDatabase
import com.pashtet.myapplication.repo.PodRepo
import com.pashtet.myapplication.service.RssFeedService
import com.pashtet.myapplication.ui.MainActivity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class EpisodeUpdateWorker (context: Context, params: WorkerParameters)
    : CoroutineWorker(context, params) {

    companion object {
        const val EPISODE_CHANNEL_ID = "podcastplay_episodes_channel"
        const val EXTRA_FEED_URL = "PodcastFeedUrl"
    }

    override suspend fun doWork(): Result = coroutineScope{
        val job = async{
            val db = PodPlayDatabase.getInstance(applicationContext, this)
            val repo = PodRepo(RssFeedService.instance, db.podcastDao())
            val podcastUpdates = repo.updatePodcastEpisodes()

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                createNotificationChannel()

            for(podcastUpdate in podcastUpdates){
                displayNotification(podcastUpdate)
            }
        }

        job.await()

        Result.success()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE)
                as NotificationManager

        if(notificationManager.getNotificationChannel(EPISODE_CHANNEL_ID) == null){
            val channel = NotificationChannel(
                EPISODE_CHANNEL_ID,
                "Episodes",
                NotificationManager.IMPORTANCE_DEFAULT)

            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun displayNotification(podcastInfo: PodRepo.PodcastUpdateInfo){
        val contentIntent = Intent(applicationContext, MainActivity::class.java)
        contentIntent.putExtra(EXTRA_FEED_URL, podcastInfo.feedUrl)

        val pendingContentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat
            .Builder(applicationContext, EPISODE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_episode_icon)
            .setContentTitle(applicationContext.getString(R.string.episode_notification_title))
            .setContentText(applicationContext.getString(
                R.string.episode_notification_text,
                podcastInfo.newCount, podcastInfo.name))
            .setNumber(podcastInfo.newCount)
            .setAutoCancel(true)
            .setContentIntent(pendingContentIntent)
            .build()

        var notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE)
                as NotificationManager

        notificationManager.notify(podcastInfo.name, 0, notification)
    }
}
