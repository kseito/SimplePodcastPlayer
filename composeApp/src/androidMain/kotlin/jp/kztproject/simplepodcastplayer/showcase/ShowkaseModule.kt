package jp.kztproject.simplepodcastplayer.showcase

import com.airbnb.android.showkase.annotation.ShowkaseRoot
import com.airbnb.android.showkase.annotation.ShowkaseRootModule

/**
 * Showkase のルートモジュール。
 * これを起点に、アプリ内の `@ShowkaseComposable` が付与された Preview を自動収集する。
 * 収集結果は Roborazzi のスクリーンショットテスト（VRT）で一括キャプチャされる。
 */
@ShowkaseRoot
class ShowkaseModule : ShowkaseRootModule
