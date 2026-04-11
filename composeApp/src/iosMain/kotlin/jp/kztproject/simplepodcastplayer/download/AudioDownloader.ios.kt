package jp.kztproject.simplepodcastplayer.download

import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSOutputStream
import platform.Foundation.NSUserDomainMask

class DownloadDataCreationException(message: String) : IllegalStateException(message)

actual class AudioDownloader {
    private val httpClient = HttpClient()

    @OptIn(ExperimentalForeignApi::class)
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

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun downloadAudio(url: String, episodeId: String): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0f))

        try {
            val downloadDir = getDownloadDirectory()
            val fileName = episodeId.replace(Regex("[^a-zA-Z0-9]"), "_") + ".mp3"
            val filePath = "$downloadDir/$fileName"

            httpClient.prepareGet(url).execute { response ->
                val channel = response.bodyAsChannel()
                val contentLength = response.contentLength() ?: 0L

                val outputStream = NSOutputStream.outputStreamToFileAtPath(filePath, append = false)
                    ?: throw DownloadDataCreationException("Failed to create output stream for path: $filePath")
                outputStream.open()

                try {
                    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                    var totalBytesRead = 0L

                    while (true) {
                        val bytesRead = channel.readAvailable(buffer)
                        if (bytesRead == -1) break

                        buffer.usePinned { pinned ->
                            outputStream.write(pinned.addressOf(0), bytesRead.toULong())
                        }

                        totalBytesRead += bytesRead
                        if (contentLength > 0) {
                            emit(DownloadState.Downloading(totalBytesRead.toFloat() / contentLength.toFloat()))
                        }
                    }
                } finally {
                    outputStream.close()
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

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun deleteDownload(episodeId: String): Boolean = getLocalFilePath(episodeId)?.let { filePath ->
        NSFileManager.defaultManager.removeItemAtPath(filePath, null)
    } ?: false

    actual fun isDownloaded(episodeId: String): Boolean = getLocalFilePath(episodeId) != null

    private companion object {
        const val DOWNLOAD_BUFFER_SIZE = 8192
    }
}
