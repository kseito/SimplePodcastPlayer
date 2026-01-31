package jp.kztproject.simplepodcastplayer.di

import jp.kztproject.simplepodcastplayer.data.AppleSearchApiClient
import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.IAppleSearchApiClient
import jp.kztproject.simplepodcastplayer.data.IRssService
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.data.RssService
import jp.kztproject.simplepodcastplayer.data.database.DatabaseBuilder
import jp.kztproject.simplepodcastplayer.data.repository.DownloadRepositoryBuilder
import jp.kztproject.simplepodcastplayer.data.repository.IDownloadRepository
import jp.kztproject.simplepodcastplayer.data.repository.PlaybackRepository
import jp.kztproject.simplepodcastplayer.data.repository.PodcastRepository
import jp.kztproject.simplepodcastplayer.screen.PodcastDetailViewModel
import jp.kztproject.simplepodcastplayer.screen.PodcastListViewModel
import jp.kztproject.simplepodcastplayer.screen.PodcastSearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database
    single { DatabaseBuilder.build() }
    single { get<jp.kztproject.simplepodcastplayer.data.database.AppDatabase>().podcastDao() }
    single { get<jp.kztproject.simplepodcastplayer.data.database.AppDatabase>().episodeDao() }
    single { get<jp.kztproject.simplepodcastplayer.data.database.AppDatabase>().playHistoryDao() }

    // Repositories
    single { PodcastRepository(get(), get()) }
    single { PlaybackRepository(get(), get()) }
    factory<IDownloadRepository> { DownloadRepositoryBuilder.build() }

    // Services
    factory<IRssService> { RssService() }
    factory<IAppleSearchApiClient> { AppleSearchApiClient() }

    // ViewModels
    viewModel { PodcastListViewModel(get()) }
    viewModel { PodcastSearchViewModel(get()) }
    viewModel { params ->
        PodcastDetailViewModel(
            podcastRepository = get(),
            downloadRepository = get(),
            appleApiClient = get(),
            onNavigateToPlayer = params.get<(Episode, Podcast) -> Unit>(),
        )
    }
}
