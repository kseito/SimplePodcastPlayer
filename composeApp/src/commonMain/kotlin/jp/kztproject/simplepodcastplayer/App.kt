package jp.kztproject.simplepodcastplayer

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.screen.PodcastDetailScreen
import jp.kztproject.simplepodcastplayer.screen.PodcastListScreen
import jp.kztproject.simplepodcastplayer.screen.PodcastSearchScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        val selectedPodcast = remember { mutableStateOf<Podcast?>(null) }
        val selectedPodcastId = remember { mutableStateOf<Long?>(null) }

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
                    )
                }
            }
            composable("list_detail") {
                // This screen is accessed from PodcastListScreen
                // We need to get the podcast from the database using selectedPodcastId
                selectedPodcastId.value?.let { podcastId ->
                    // Create a temporary view model to get the podcast
                    val listViewModel =
                        androidx.lifecycle.viewmodel.compose.viewModel<
                            jp.kztproject.simplepodcastplayer.screen.PodcastListViewModel,
                            > {
                            jp.kztproject.simplepodcastplayer.screen.PodcastListViewModel()
                        }
                    listViewModel.getPodcastById(podcastId)?.let { podcast ->
                        PodcastDetailScreen(
                            podcast = podcast,
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
