package jp.kztproject.simplepodcastplayer

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.di.appModule
import jp.kztproject.simplepodcastplayer.screen.PlayerScreen
import jp.kztproject.simplepodcastplayer.screen.PodcastDetailScreen
import jp.kztproject.simplepodcastplayer.screen.PodcastListScreen
import jp.kztproject.simplepodcastplayer.screen.PodcastListViewModel
import jp.kztproject.simplepodcastplayer.screen.PodcastSearchScreen
import jp.kztproject.simplepodcastplayer.screen.rememberPlayerViewModel
import jp.kztproject.simplepodcastplayer.data.repository.PodcastRepository
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
@Preview
fun App() {
    KoinApplication(application = {
        modules(appModule)
    }) {
        MaterialTheme {
            val navController = rememberNavController()
            val selectedPodcast = remember { mutableStateOf<Podcast?>(null) }
            val selectedPodcastId = remember { mutableStateOf<Long?>(null) }
            val selectedEpisode = remember { mutableStateOf<Episode?>(null) }
            val selectedEpisodePodcast = remember { mutableStateOf<Podcast?>(null) }

            NavHost(navController = navController, startDestination = "list") {
                composable("list") {
                    PodcastListScreen(
                        onNavigateToSearch = { navController.navigate("search") },
                        onPodcastClick = { podcastId ->
                            selectedPodcastId.value = podcastId
                            navController.navigate("list_detail")
                        },
                    )
                }
                composable("search") {
                    PodcastSearchScreen(
                        onNavigateToList = { navController.navigate("list") },
                        onNavigateToDetail = { podcast ->
                            selectedPodcast.value = podcast
                            navController.navigate("detail")
                        },
                    )
                }
                composable("detail") {
                    selectedPodcast.value?.let { podcast ->
                        PodcastDetailScreen(
                            podcast = podcast,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToPlayer = { episode, podcastData ->
                                selectedEpisode.value = episode
                                selectedEpisodePodcast.value = podcastData
                                navController.navigate("player")
                            },
                        )
                    }
                }
                composable("list_detail") {
                    // This screen is accessed from PodcastListScreen
                    // We need to get the podcast from the database using selectedPodcastId
                    selectedPodcastId.value?.let { podcastId ->
                        // Get the podcast from the view model
                        val podcastRepository: PodcastRepository = koinInject()
                        val listViewModel: PodcastListViewModel = viewModel { PodcastListViewModel(podcastRepository) }
                        listViewModel.getPodcastById(podcastId)?.let { podcast ->
                            PodcastDetailScreen(
                                podcast = podcast,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToPlayer = { episode, podcastData ->
                                    selectedEpisode.value = episode
                                    selectedEpisodePodcast.value = podcastData
                                    navController.navigate("player")
                                },
                            )
                        }
                    }
                }
                composable("player") {
                    selectedEpisode.value?.let { episode ->
                        selectedEpisodePodcast.value?.let { podcast ->
                            val viewModel = rememberPlayerViewModel(episode, podcast)
                            PlayerScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}
