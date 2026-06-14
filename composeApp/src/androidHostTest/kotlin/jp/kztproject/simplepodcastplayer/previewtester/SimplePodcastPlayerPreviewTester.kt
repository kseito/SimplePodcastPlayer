package jp.kztproject.simplepodcastplayer.previewtester

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.github.takahirom.roborazzi.ComposePreviewTester
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziActivity
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.registerRoborazziActivityToRobolectricIfNeeded
import jp.kztproject.simplepodcastplayer.rule.CoilRule
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import sergio.sastre.composable.preview.scanner.jvm.JvmAnnotationInfo
import sergio.sastre.composable.preview.scanner.jvm.JvmAnnotationScanner

// Compose Multiplatform の @Preview。androidx の @Preview とは別物なので、
// この FQN を指定して JVM バイトコードから走査する。
private const val COMPOSE_PREVIEW_ANNOTATION = "org.jetbrains.compose.ui.tooling.preview.Preview"

/**
 * ComposablePreviewScanner で収集した CMP `@Preview` を 1 件ずつ Robolectric 上で描画し、
 * Roborazzi でスクリーンショットを撮る Tester。Roborazzi の
 * `generateComposePreviewRobolectricTests` から自動生成されたテストが本クラスを呼び出す。
 */
@Suppress("UNUSED")
@OptIn(ExperimentalRoborazziApi::class)
class SimplePodcastPlayerPreviewTester :
    ComposePreviewTester<ComposePreviewTester.TestParameter.JUnit4TestParameter<JvmAnnotationInfo>> {
    override fun test(
        testParameter: ComposePreviewTester.TestParameter.JUnit4TestParameter<JvmAnnotationInfo>,
    ) {
        val preview = testParameter.preview
        val screenshotNameSuffix = preview.previewIndex?.let { "_$it" }.orEmpty()
        testParameter.composeTestRule.setContent {
            ProvideAndroidContextToComposeResource()
            preview()
        }
        testParameter.composeTestRule
            .onRoot()
            .captureRoboImage("screenshots/${preview.methodName}$screenshotNameSuffix.png")
    }

    override fun testParameters(): List<ComposePreviewTester.TestParameter.JUnit4TestParameter<JvmAnnotationInfo>> {
        val options = options()
        val scanResult =
            JvmAnnotationScanner(COMPOSE_PREVIEW_ANNOTATION)
                .scanPackageTrees(*options.scanOptions.packages.toTypedArray())
                .let {
                    if (options.scanOptions.includePrivatePreviews) it.includePrivatePreviews() else it
                }
        return scanResult
            .getPreviews()
            .map {
                ComposePreviewTester.TestParameter.JUnit4TestParameter(
                    composeTestRuleFactory =
                        (options.testLifecycleOptions as ComposePreviewTester.Options.JUnit4TestLifecycleOptions).composeRuleFactory,
                    preview = it,
                )
            }
    }

    override fun options(): ComposePreviewTester.Options =
        super.options().copy(
            testLifecycleOptions =
                ComposePreviewTester.Options.JUnit4TestLifecycleOptions(
                    composeRuleFactory = {
                        @Suppress("UNCHECKED_CAST")
                        createAndroidComposeRule<RoborazziActivity>() as
                            AndroidComposeTestRule<ActivityScenarioRule<out androidx.activity.ComponentActivity>, *>
                    },
                    testRuleFactory = { composeTestRule ->
                        RuleChain.outerRule(
                            object : TestWatcher() {
                                override fun starting(description: Description?) {
                                    super.starting(description)
                                    registerRoborazziActivityToRobolectricIfNeeded()
                                }
                            },
                        )
                            .around(composeTestRule)
                            .around(CoilRule())
                    },
                ),
        )
}
