package jp.kztproject.simplepodcastplayer

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform