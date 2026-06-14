package jp.kztproject.simplepodcastplayer.previewtester

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import org.jetbrains.compose.resources.PreviewContextConfigurationEffect

/**
 * Robolectric 上で Compose Multiplatform Resources（`Res.drawable.*` や `stringResource` 等）を
 * 解決できるようにする。プレビュー扱いにするため `LocalInspectionMode` を true にし、
 * `PreviewContextConfigurationEffect()` でリソースコンテキストを初期化する。
 */
@Composable
fun ProvideAndroidContextToComposeResource() {
    CompositionLocalProvider(LocalInspectionMode provides true) {
        PreviewContextConfigurationEffect()
    }
}
