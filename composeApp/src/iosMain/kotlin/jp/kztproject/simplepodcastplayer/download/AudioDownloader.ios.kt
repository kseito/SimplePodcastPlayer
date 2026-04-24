package jp.kztproject.simplepodcastplayer.download

import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

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

                val fileManager = NSFileManager.defaultManager
                val fileCreated = fileManager.createFileAtPath(
                    path = filePath,
                    contents = null,
                    attributes = null,
                )
                if (!fileCreated && !fileManager.fileExistsAtPath(filePath)) {
                    throw DownloadDataCreationException("Failed to create file for path: $filePath")
                }

                val fileHandle = fopen(filePath, "wb")
                    ?: throw DownloadDataCreationException("Failed to open file for path: $filePath")

                try {
                    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                    var totalBytesRead = 0L

                    while (true) {
                        val bytesRead = channel.readAvailable(buffer)
                        if (bytesRead == -1) break

                        val bytesWritten =
                            buffer.usePinned { pinned ->
                                fwrite(
                                    pinned.addressOf(0),
                                    1.convert(),
                                    bytesRead.convert(),
                                    fileHandle,
                                )
                            }
                        if (bytesWritten.toInt() != bytesRead) {
                            throw DownloadDataCreationException("Failed to write downloaded chunk to file")
                        }

                        totalBytesRead += bytesRead
                        if (contentLength > 0) {
                            emit(DownloadState.Downloading(totalBytesRead.toFloat() / contentLength.toFloat()))
                        }
                    }
                } finally {
                    fclose(fileHandle)
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
