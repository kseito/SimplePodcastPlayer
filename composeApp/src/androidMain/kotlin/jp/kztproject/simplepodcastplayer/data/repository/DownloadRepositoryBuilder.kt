package jp.kztproject.simplepodcastplayer.data.repository

import android.content.Context

actual object DownloadRepositoryBuilder {
    private lateinit var context: Context

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    actual fun build(): DownloadRepository = DownloadRepository(context)
}
