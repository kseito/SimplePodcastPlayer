package jp.kztproject.simplepodcastplayer.download

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create

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

            // Download the file to memory
            val bytes = httpClient.get(url).bodyAsBytes()

            emit(DownloadState.Downloading(0.5f))

            // Write bytes to file using NSFileManager
            bytes.usePinned { pinned ->
                val data = NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
                if (data != null) {
                    NSFileManager.defaultManager.createFileAtPath(
                        path = filePath,
                        contents = data,
                        attributes = null,
                    )
                } else {
                    throw DownloadDataCreationException("Failed to create NSData from downloaded bytes")
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
}
