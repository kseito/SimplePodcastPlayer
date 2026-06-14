@file:Suppress("OPT_IN_USAGE_ERROR", "OPT_IN_USAGE")

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.spotless)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.roborazzi)
}

kotlin {
    androidLibrary {
        namespace = "jp.kztproject.simplepodcastplayer.shared"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }

        withHostTestBuilder {}
            .configure {
                // Robolectric が Android リソース/マニフェストを読めるようにする（Roborazzi スクリーンショットテスト用）
                isIncludeAndroidResources = true
            }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.android)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.session)
            implementation(libs.androidx.media3.ui)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.coil.test)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.napier)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
            implementation(libs.koin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
        getByName("androidHostTest").dependencies {
            // Roborazzi + Robolectric によるスクリーンショット VRT（Android ホストテスト）
            // ComposablePreviewScanner が CMP の @Preview を直接走査し、
            // Roborazzi がプレビュー用 Robolectric テストを自動生成する。
            implementation(libs.junit)
            implementation(libs.robolectric)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.junit)
            implementation(libs.androidx.compose.ui.test.junit4)
            implementation(libs.androidx.compose.ui.test.manifest)
            implementation(libs.roborazzi)
            implementation(libs.roborazzi.compose)
            implementation(libs.roborazzi.rule)
            implementation(libs.roborazzi.preview.scanner.support)
            implementation(libs.composable.preview.scanner.android)
            implementation(libs.composable.preview.scanner.jvm)
            // CoilRule 用（commonMain の implementation 依存はテストへ伝播しないため明示）
            implementation(libs.coil.compose)
            implementation(libs.coil.test)
            // ProvideAndroidContextToComposeResource 用（Compose Resources を Robolectric 上で解決）
            implementation(compose.components.resources)
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

roborazzi {
    // 参照画像の出力先（artifact 経由でやり取りするためコミットはしない）
    outputDir.set(file("screenshots"))

    // CMP の @Preview を ComposablePreviewScanner で走査し、
    // プレビューごとの Robolectric スクリーンショットテストを自動生成する。
    generateComposePreviewRobolectricTests {
        enable = true
        // App() などのアプリ全体プレビューを除外し、画面プレビューのみを対象にする
        packages = listOf("jp.kztproject.simplepodcastplayer.screen")
        includePrivatePreviews = true
        // カスタム Tester 使用時は scanOptions を Tester 側で参照する必要がある
        useScanOptionParametersInTester = true
        testerQualifiedClassName =
            "jp.kztproject.simplepodcastplayer.previewtester.SimplePodcastPlayerPreviewTester"
        robolectricConfig =
            mapOf(
                "sdk" to "[35]",
                "qualifiers" to "RobolectricDeviceQualifiers.Pixel4a",
            )
    }
}

tasks.withType<Test>().configureEach {
    // Roborazzi のスクリーンショットをネイティブグラフィックスで描画する
    systemProperty("robolectric.graphicsMode", "NATIVE")
    systemProperty("robolectric.pixelCopyRenderMode", "hardware")
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint()
            .setEditorConfigPath("$rootDir/.editorconfig")
            .editorConfigOverride(
                mapOf(
                    "android" to "true",
                    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                ),
            )
        leadingTabsToSpaces(4)
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}
