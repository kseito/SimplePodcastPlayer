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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

class AudioDownloader : IAudioDownloader {
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
    override suspend fun downloadAudio(url: String, episodeId: String): Flow<DownloadState> = channelFlow {
        send(DownloadState.Downloading(0f))

        val downloadDir = getDownloadDirectory()
        val filePath = "$downloadDir/${fileNameOf(episodeId)}"
        val partPath = "$filePath$PART_SUFFIX"

        try {
            httpClient.prepareGet(url).execute { response ->
                val channel = response.bodyAsChannel()
                val contentLength = response.contentLength() ?: 0L
                Napier.d("Download started: contentLength=$contentLength")

                val fileHandle = fopen(partPath, "wb")
                    ?: throw DownloadDataCreationException("Failed to open file for path: $partPath")

                try {
                    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                    var totalBytesRead = 0L
                    var lastEmitMillis = Clock.System.now().toEpochMilliseconds()

                    while (true) {
                        val bytesRead = try {
                            channel.readAvailable(buffer)
                        } catch (e: Exception) {
                            Napier.d("readAvailable ended: ${e::class.simpleName} - ${e.message}")
                            -1
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

            // The presence of the final file is what marks an episode as downloaded, so it may
            // only appear once the whole body is on disk. Until then the bytes live under the
            // temporary name, where an interrupted download leaves nothing that looks complete.
            publishDownloadedFile(partPath, filePath)

            send(DownloadState.Completed)
        } catch (e: CancellationException) {
            NSFileManager.defaultManager.removeItemAtPath(partPath, null)
            throw e
        } catch (e: Exception) {
            NSFileManager.defaultManager.removeItemAtPath(partPath, null)
            Napier.e("Download failed: ${e::class.simpleName} - ${e.message}", e)
            send(DownloadState.Failed(e.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    @OptIn(ExperimentalForeignApi::class)
    private fun publishDownloadedFile(partPath: String, filePath: String) {
        val fileManager = NSFileManager.defaultManager
        // moveItemAtPath refuses to overwrite, so an older file has to go first.
        if (fileManager.fileExistsAtPath(filePath)) {
            fileManager.removeItemAtPath(filePath, null)
        }
        if (!fileManager.moveItemAtPath(partPath, toPath = filePath, error = null)) {
            throw DownloadDataCreationException("Failed to publish downloaded file: $filePath")
        }
    }

    override fun getAudioFilePath(episodeId: String): String? {
        val filePath = "${getDownloadDirectory()}/${fileNameOf(episodeId)}"

        return if (NSFileManager.defaultManager.fileExistsAtPath(filePath)) {
            filePath
        } else {
            null
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun deleteAudioFile(episodeId: String): Boolean = getAudioFilePath(episodeId)?.let { filePath ->
        NSFileManager.defaultManager.removeItemAtPath(filePath, null)
    } ?: false

    override fun isDownloaded(episodeId: String): Boolean = getAudioFilePath(episodeId) != null

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun deleteIncompleteDownloads(): Int = withContext(Dispatchers.IO) {
        val fileManager = NSFileManager.defaultManager
        val downloadDir = getDownloadDirectory()
        fileManager.contentsOfDirectoryAtPath(downloadDir, null)
            .orEmpty()
            .filterIsInstance<String>()
            .filter { it.endsWith(PART_SUFFIX) }
            .count { fileManager.removeItemAtPath("$downloadDir/$it", null) }
    }

    private fun fileNameOf(episodeId: String): String = episodeId.replace(Regex("[^a-zA-Z0-9]"), "_") + ".mp3"

    private companion object {
        const val DOWNLOAD_BUFFER_SIZE = 8192
        const val PROGRESS_EMIT_INTERVAL_MS = 100L
        const val PART_SUFFIX = ".part"
    }
}
