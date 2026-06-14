package jp.kztproject.simplepodcastplayer.rule

import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import coil3.ColorImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.test.FakeImageLoaderEngine
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * 各テスト前に Coil のシングルトン ImageLoader をフェイクへ差し替える JUnit ルール。
 * ネットワークアクセスを避け、常に同じプレースホルダ画像で決定的に描画する。
 * `setUnsafe` を使うため、テスト間でシングルトンが衝突しない。
 */
@OptIn(DelicateCoilApi::class)
class CoilRule : TestWatcher() {
    override fun starting(description: Description?) {
        super.starting(description)
        val engine =
            FakeImageLoaderEngine.Builder()
                .default(ColorImage(Color.BLUE))
                .build()
        val imageLoader =
            ImageLoader.Builder(ApplicationProvider.getApplicationContext())
                .components { add(engine) }
                .build()
        SingletonImageLoader.setUnsafe(imageLoader)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        // 同一 JVM で動く後続テストへフェイク ImageLoader が残留しないようリセットする
        SingletonImageLoader.reset()
    }
}
