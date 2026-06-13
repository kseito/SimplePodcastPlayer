package jp.kztproject.simplepodcastplayer

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import coil3.ColorImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.test.FakeImageLoaderEngine
import com.airbnb.android.showkase.models.Showkase
import com.airbnb.android.showkase.models.ShowkaseBrowserComponent
import com.github.takahirom.roborazzi.captureRoboImage
import jp.kztproject.simplepodcastplayer.showcase.getMetadata
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Showkase が収集した全 Preview コンポーネントを Roborazzi で一括キャプチャするスクリーンショットテスト。
 *
 * - `recordRoborazzi*` タスク: 参照画像を `screenshots/` に生成
 * - `compareRoborazzi*` タスク: 参照画像と比較し差分画像（`*_compare.png`）を生成
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [35])
class ShowkaseParameterizedTest(private val testCase: TestCase) {
    @get:Rule
    val composeRule = createComposeRule()

    @OptIn(DelicateCoilApi::class)
    @Before
    fun setUpImageLoader() {
        // 各テストごとに Coil のシングルトンをリセットしてからフェイク ImageLoader を設定する。
        // これにより、Preview が個別に setSingletonImageLoaderFactory を呼んでも衝突せず、
        // ネットワークへアクセスせず常に同じプレースホルダ画像で決定的に描画される。
        SingletonImageLoader.reset()
        SingletonImageLoader.setSafe { context ->
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

    @Test
    fun captureShowkaseComponent() {
        val component = testCase.showkaseBrowserComponent

        composeRule.setContent {
            component.component()
        }

        composeRule
            .onRoot()
            .captureRoboImage(
                filePath = "screenshots/${component.componentName}_${component.group}.png".replace(" ", "_"),
            )
    }

    companion object {
        class TestCase(val showkaseBrowserComponent: ShowkaseBrowserComponent) {
            override fun toString() = showkaseBrowserComponent.componentKey
        }

        @ParameterizedRobolectricTestRunner.Parameters(name = "[{index}] {0}")
        @JvmStatic
        fun components(): Iterable<Array<*>> = Showkase.getMetadata().componentList.map {
            arrayOf(TestCase(it))
        }
    }
}
