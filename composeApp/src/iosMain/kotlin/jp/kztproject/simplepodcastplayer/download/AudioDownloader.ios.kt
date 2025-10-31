package jp.kztproject.simplepodcastplayer.download

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class AudioDownloader {
    private val httpClient = HttpClient()

    private fun getDownloadDirectory(): String {
        val documentDirectory =
            NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            )
        val downloadsPath = "${documentDirectory?.path}/podcast_downloads"

        // Create directory if it doesn't exist
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(downloadsPath)) {
            fileManager.createDirectoryAtPath(
                downloadsPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
        }

        return downloadsPath
    }

    actual suspend fun downloadAudio(url: String, episodeId: String): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0f))

        try {
            val downloadDir = getDownloadDirectory()
            val fileName = episodeId.replace(Regex("[^a-zA-Z0-9]"), "_") + ".mp3"
            val filePath = "$downloadDir/$fileName"

            withContext(Dispatchers.IO) {
                val response: HttpResponse = httpClient.get(url)
                val channel = response.bodyAsChannel()
                val contentLength = response.contentLength() ?: 0L
                var totalBytesRead = 0L

                platform.Foundation.NSOutputStream.outputStreamToFileAtPath(filePath, append = false)
                    ?.let { output ->
                        output.open()

                        try {
                            val buffer = ByteArray(8192)
                            while (true) {
                                val bytesRead = channel.readAvailable(buffer)
                                if (bytesRead == -1) break

                                buffer.usePinned { pinned ->
                                    output.write(pinned.addressOf(0), bytesRead.toULong())
                                }

                                totalBytesRead += bytesRead

                                if (contentLength > 0) {
                                    val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                                    emit(DownloadState.Downloading(progress))
                                }
                            }
                        } finally {
                            output.close()
                        }
                    }
            }

            emit(DownloadState.Completed)
        } catch (e: Exception) {
            emit(DownloadState.Failed(e.message ?: "Download failed"))
        }
    }

    actual fun getLocalFilePath(episodeId: String): String? {
        val downloadDir = getDownloadDirectory()
        val fileName = episodeId.replace(Regex("[^a-zA-Z0-9]"), "_") + ".mp3"
        val filePath = "$downloadDir/$fileName"

        return if (NSFileManager.defaultManager.fileExistsAtPath(filePath)) {
            filePath
        } else {
            null
        }
    }

    actual suspend fun deleteDownload(episodeId: String): Boolean = withContext(Dispatchers.IO) {
        getLocalFilePath(episodeId)?.let { filePath ->
            NSFileManager.defaultManager.removeItemAtPath(filePath, null)
        } ?: false
    }

    actual fun isDownloaded(episodeId: String): Boolean = getLocalFilePath(episodeId) != null
}
