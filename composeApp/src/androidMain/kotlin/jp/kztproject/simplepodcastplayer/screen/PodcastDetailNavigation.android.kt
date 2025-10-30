package jp.kztproject.simplepodcastplayer.screen

import android.content.Intent
import jp.kztproject.simplepodcastplayer.activity.PlayerActivity
import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast

// Global holder for the current Activity context
// This will be set by PlayerActivity when it starts
object PlayerNavigator {
    var currentContext: android.content.Context? = null
}

actual fun navigateToPlayer(
    episode: Episode,
    podcast: Podcast,
) {
    val context = PlayerNavigator.currentContext ?: return
    val intent = PlayerActivity.createIntent(context, episode, podcast)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
