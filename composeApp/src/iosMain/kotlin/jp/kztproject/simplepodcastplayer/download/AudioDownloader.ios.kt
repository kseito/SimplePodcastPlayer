package jp.kztproject.simplepodcastplayer.download

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
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

    @OptIn(ExperimentalForeignApi::class, ExperimentalTime::class)
    actual suspend fun downloadAudio(url: String, episodeId: String): Flow<DownloadState> = channelFlow {
        send(DownloadState.Downloading(0f))

        try {
            val downloadDir = getDownloadDirectory()
            val fileName = episodeId.replace(Regex("[^a-zA-Z0-9]"), "_") + ".mp3"
            val filePath = "$downloadDir/$fileName"

            httpClient.prepareGet(url).execute { response ->
                val channel = response.bodyAsChannel()
                val contentLength = response.contentLength() ?: 0L
                Napier.d("Download started: contentLength=$contentLength")

                val fileHandle = fopen(filePath, "wb")
                    ?: throw DownloadDataCreationException("Failed to open file for path: $filePath")

                try {
                    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                    var totalBytesRead = 0L
                    var lastEmitMillis = Clock.System.now().toEpochMilliseconds()

                    while (true) {
                        val bytesRead = try {
                            channel.readAvailable(buffer)
                        } catch (e: Exception) {
                            Napier.d("readAvailable ended: ${e::class.simpleName} - ${e.message}")
                            break
                        }
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
                            val nowMillis = Clock.System.now().toEpochMilliseconds()
                            if (nowMillis - lastEmitMillis >= PROGRESS_EMIT_INTERVAL_MS) {
                                val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                                send(DownloadState.Downloading(progress))
                                lastEmitMillis = nowMillis
                            }
                        }
                    }
                } finally {
                    fclose(fileHandle)
                }
            }

            send(DownloadState.Completed)
        } catch (e: Exception) {
            Napier.e("Download failed: ${e::class.simpleName} - ${e.message}", e)
            send(DownloadState.Failed(e.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

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
        const val PROGRESS_EMIT_INTERVAL_MS = 100L
    }
}
