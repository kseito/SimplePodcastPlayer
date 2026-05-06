package jp.kztproject.simplepodcastplayer

import androidx.compose.ui.window.ComposeUIViewController
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import platform.UIKit.UIViewController

fun mainViewController(): UIViewController {
    Napier.base(DebugAntilog())
    return ComposeUIViewController {
        App()
    }
}
