package jp.kztproject.simplepodcastplayer

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun mainViewController(): UIViewController = ComposeUIViewController {
    App()
}
