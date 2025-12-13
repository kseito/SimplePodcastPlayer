package jp.kztproject.simplepodcastplayer.download

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

actual class AudioDownloader(private val context: Context) {
    private val httpClient = HttpClient()

    private fun getDownloadDirectory(): File {
        val downloadDir = File(context.filesDir, "podcast_downloads")
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        return downloadDir
    }

    actual suspend fun downloadAudio(url: String, episodeId: String): Flow<DownloadState> = channelFlow {
        send(DownloadState.Downloading(0f))

        try {
            val downloadDir = getDownloadDirectory()
            val fileName = "${episodeId.replace("[^a-zA-Z0-9]".toRegex(), "_")}.mp3"
            val file = File(downloadDir, fileName)

            val response: HttpResponse = httpClient.get(url)
            val channel = response.bodyAsChannel()
            val contentLength = response.contentLength() ?: 0L

            file.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var totalBytesRead = 0L

                while (true) {
                    val bytesRead = channel.readAvailable(buffer)
                    if (bytesRead == -1) break

                    output.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    if (contentLength > 0) {
                        val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                        send(DownloadState.Downloading(progress))
                    }
                }
            }

            send(DownloadState.Completed)
        } catch (e: Exception) {
            send(DownloadState.Failed(e.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    actual fun getLocalFilePath(episodeId: String): String? {
        val downloadDir = getDownloadDirectory()
        val fileName = "${episodeId.replace("[^a-zA-Z0-9]".toRegex(), "_")}.mp3"
        val file = File(downloadDir, fileName)
        return if (file.exists()) file.absolutePath else null
    }

    actual suspend fun deleteDownload(episodeId: String): Boolean = withContext(Dispatchers.IO) {
        val filePath = getLocalFilePath(episodeId)
        filePath?.let { File(it).delete() } ?: false
    }

    actual fun isDownloaded(episodeId: String): Boolean = getLocalFilePath(episodeId) != null
}
