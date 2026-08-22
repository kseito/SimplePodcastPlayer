package jp.kztproject.simplepodcastplayer.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
internal data object PodcastListRoute : NavKey

@Serializable
internal data object PodcastSearchRoute : NavKey

@Serializable
internal data class PodcastDetailRoute(val podcast: Podcast) : NavKey

@Serializable
internal data class SubscribedPodcastDetailRoute(val podcastId: Long) : NavKey

@Serializable
internal data class PlayerRoute(val episode: Episode, val podcast: Podcast) : NavKey

@Serializable
internal data object InProgressEpisodesRoute : NavKey

// 非JVMプラットフォーム(iOS)ではリフレクションベースのシリアライズが使えないため、
// バックスタック永続化に使う NavKey のシリアライザを明示的に登録する
internal val navBackStackConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(PodcastListRoute::class, PodcastListRoute.serializer())
            subclass(PodcastSearchRoute::class, PodcastSearchRoute.serializer())
            subclass(PodcastDetailRoute::class, PodcastDetailRoute.serializer())
            subclass(SubscribedPodcastDetailRoute::class, SubscribedPodcastDetailRoute.serializer())
            subclass(PlayerRoute::class, PlayerRoute.serializer())
            subclass(InProgressEpisodesRoute::class, InProgressEpisodesRoute.serializer())
        }
    }
}
