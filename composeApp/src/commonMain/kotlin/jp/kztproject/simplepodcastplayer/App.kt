package jp.kztproject.simplepodcastplayer

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import jp.kztproject.simplepodcastplayer.screen.PodcastListScreen
import jp.kztproject.simplepodcastplayer.screen.PodcastSearchScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "list") {
            composable("list") {
                PodcastListScreen(onNavigateToSearch = { navController.navigate("search") })
            }
            composable("search") {
                PodcastSearchScreen(onNavigateToList = { navController.navigate("list") })
            }
        }
    }
}
