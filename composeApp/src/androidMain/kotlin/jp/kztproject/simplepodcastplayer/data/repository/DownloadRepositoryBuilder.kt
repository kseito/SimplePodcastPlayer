package jp.kztproject.simplepodcastplayer.data.repository

import android.content.Context

actual object DownloadRepositoryBuilder {
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    actual fun build(): DownloadRepository {
        val appContext = context ?: error(
            "DownloadRepositoryBuilder has not been initialized. Call init() in your Application class.",
        )
        return DownloadRepository(appContext)
    }
}
