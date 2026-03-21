package jp.kztproject.simplepodcastplayer.screen

import androidx.compose.runtime.Composable
import coil3.ColorImage
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.test.FakeImageLoaderEngine

@Composable
internal fun SetFakeImageLoaderForPreview() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(
                    FakeImageLoaderEngine.Builder()
                        .default(ColorImage(0xFF00A3AF.toInt()))
                        .build(),
                )
            }
            .build()
    }
}
