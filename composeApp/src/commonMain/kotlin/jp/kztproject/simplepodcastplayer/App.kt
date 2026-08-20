package jp.kztproject.simplepodcastplayer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import jp.kztproject.simplepodcastplayer.di.appModule
import jp.kztproject.simplepodcastplayer.navigation.InProgressEpisodesRoute
import jp.kztproject.simplepodcastplayer.navigation.PlayerRoute
import jp.kztproject.simplepodcastplayer.navigation.PodcastDetailRoute
import jp.kztproject.simplepodcastplayer.navigation.PodcastListRoute
import jp.kztproject.simplepodcastplayer.navigation.PodcastSearchRoute
import jp.kztproject.simplepodcastplayer.navigation.SubscribedPodcastDetailRoute
import jp.kztproject.simplepodcastplayer.navigation.navBackStackConfig
import jp.kztproject.simplepodcastplayer.screen.InProgressEpisodesScreen
import jp.kztproject.simplepodcastplayer.screen.PlayerScreen
import jp.kztproject.simplepodcastplayer.screen.PodcastDetailScreen
import jp.kztproject.simplepodcastplayer.screen.PodcastListScreen
import jp.kztproject.simplepodcastplayer.screen.PodcastSearchScreen
import jp.kztproject.simplepodcastplayer.screen.rememberPlayerViewModel
import jp.kztproject.simplepodcastplayer.ui.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
@Preview
fun App() {
    KoinApplication(
        configuration = koinConfiguration {
            modules(appModule)
        },
    ) {
        AppTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                val backStack = rememberNavBackStack(navBackStackConfig, PodcastListRoute)

                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    entryProvider = entryProvider {
                        entry<PodcastListRoute> {
                            PodcastListScreen(
                                onNavigateToSearch = { backStack.add(PodcastSearchRoute) },
                                onNavigateToInProgress = { backStack.add(InProgressEpisodesRoute) },
                                onPodcastClick = { podcastId ->
                                    backStack.add(SubscribedPodcastDetailRoute(podcastId))
                                },
                            )
                        }
                        entry<PodcastSearchRoute> {
                            PodcastSearchScreen(
                                onNavigateToList = { backStack.add(PodcastListRoute) },
                                onNavigateToDetail = { podcast ->
                                    backStack.add(PodcastDetailRoute(podcast))
                                },
                            )
                        }
                        entry<PodcastDetailRoute> { key ->
                            PodcastDetailScreen(
                                podcast = key.podcast,
                                onNavigateBack = { backStack.removeLastOrNull() },
                                onNavigateToPlayer = { episode, podcastData ->
                                    backStack.add(PlayerRoute(episode, podcastData))
                                },
                            )
                        }
                        entry<SubscribedPodcastDetailRoute> { key ->
                            PodcastDetailScreen(
                                podcastId = key.podcastId,
                                onNavigateBack = { backStack.removeLastOrNull() },
                                onNavigateToPlayer = { episode, podcastData ->
                                    backStack.add(PlayerRoute(episode, podcastData))
                                },
                            )
                        }
                        entry<PlayerRoute> { key ->
                            val viewModel = rememberPlayerViewModel(key.episode, key.podcast)
                            PlayerScreen(
                                viewModel = viewModel,
                                onNavigateBack = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<InProgressEpisodesRoute> {
                            InProgressEpisodesScreen(
                                onNavigateBack = { backStack.removeLastOrNull() },
                                onNavigateToPlayer = { episode, podcast ->
                                    backStack.add(PlayerRoute(episode, podcast))
                                },
                            )
                        }
                    },
                )
            }
        }
    }
}
