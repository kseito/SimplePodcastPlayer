package jp.kztproject.simplepodcastplayer.download

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class AudioDownloader(private val context: Context) : IAudioDownloader {
    private val httpClient = HttpClient()

    private fun getDownloadDirectory(): File {
        val downloadDir = File(context.filesDir, "podcast_downloads")
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        return downloadDir
    }

    override suspend fun downloadAudio(url: String, episodeId: String): Flow<DownloadState> = channelFlow {
        send(DownloadState.Downloading(0f))

        val downloadDir = getDownloadDirectory()
        val file = File(downloadDir, fileNameOf(episodeId))
        val partFile = File(downloadDir, "${fileNameOf(episodeId)}$PART_SUFFIX")

        try {
            httpClient.prepareGet(url).execute { response ->
                val channel = response.bodyAsChannel()
                val contentLength = response.contentLength() ?: 0L

                partFile.outputStream().use { output ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
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
            }

            // The presence of the final file is what marks an episode as downloaded, so it may
            // only appear once the whole body is on disk. Until then the bytes live under the
            // temporary name, where an interrupted download leaves nothing that looks complete.
            if (!partFile.renameTo(file)) {
                throw DownloadDataCreationException("Failed to publish downloaded file: ${file.name}")
            }

            send(DownloadState.Completed)
        } catch (e: CancellationException) {
            partFile.delete()
            throw e
        } catch (e: Exception) {
            partFile.delete()
            send(DownloadState.Failed(e.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getAudioFilePath(episodeId: String): String? {
        val file = File(getDownloadDirectory(), fileNameOf(episodeId))
        return if (file.exists()) file.absolutePath else null
    }

    override suspend fun deleteAudioFile(episodeId: String): Boolean = withContext(Dispatchers.IO) {
        val filePath = getAudioFilePath(episodeId)
        filePath?.let { File(it).delete() } ?: false
    }

    override fun isDownloaded(episodeId: String): Boolean = getAudioFilePath(episodeId) != null

    override suspend fun deleteIncompleteDownloads(): Int = withContext(Dispatchers.IO) {
        getDownloadDirectory()
            .listFiles { file -> file.name.endsWith(PART_SUFFIX) }
            .orEmpty()
            .count { it.delete() }
    }

    private fun fileNameOf(episodeId: String): String = "${episodeId.replace("[^a-zA-Z0-9]".toRegex(), "_")}.mp3"

    private companion object {
        const val DOWNLOAD_BUFFER_SIZE = 8192
        const val PART_SUFFIX = ".part"
    }
}
