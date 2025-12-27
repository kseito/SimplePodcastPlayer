# 単体テスト導入計画

## 概要
SimplePodcastPlayerに単体テストを導入する。優先度高（ユーティリティ）と優先度中（Repository/ViewModel）までカバー。

## 前提条件
- DIライブラリ（Koin）を導入してテスト可能な設計にリファクタリング
- テスト戦略: **Fakeパターン + Turbine**（KMP互換）

---

## Step 1: 依存関係の追加

### 変更ファイル
- `gradle/libs.versions.toml`
- `composeApp/build.gradle.kts`

### 追加する依存関係
```toml
# libs.versions.toml
[versions]
koin = "4.0.0"
kotlinx-coroutines-test = "1.8.1"
turbine = "1.1.0"

[libraries]
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }
koin-test = { module = "io.insert-koin:koin-test", version.ref = "koin" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines-test" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
```

---

## Step 2: Koin DI設定

### 新規ファイル
- `composeApp/src/commonMain/kotlin/jp/kztproject/simplepodcastplayer/di/AppModule.kt`

### 変更ファイル
- `composeApp/src/commonMain/kotlin/jp/kztproject/simplepodcastplayer/App.kt` - KoinApplication追加

### AppModule.kt の内容
```kotlin
val appModule = module {
    // Database
    single { DatabaseBuilder.build() }
    single { get<AppDatabase>().podcastDao() }
    single { get<AppDatabase>().episodeDao() }
    single { get<AppDatabase>().playHistoryDao() }

    // Repositories
    single { PodcastRepository(get(), get()) }
    single { PlaybackRepository(get(), get()) }
    factory { DownloadRepositoryBuilder.build() }

    // Services
    factory { RssService() }
    factory { AppleSearchApiClient() }

    // ViewModels
    viewModel { PodcastListViewModel(get()) }
    viewModel { PodcastSearchViewModel(get()) }
    viewModel { params -> PodcastDetailViewModel(get(), get(), get(), params.get()) }
}
```

---

## Step 3: Repository/ViewModelのリファクタリング

### 変更ファイル

#### PodcastRepository.kt
```kotlin
// Before
class PodcastRepository {
    private val database = DatabaseBuilder.build()
    private val podcastDao = database.podcastDao()
}

// After
class PodcastRepository(
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao
)
```

#### PodcastListViewModel.kt
```kotlin
// Before
class PodcastListViewModel : ViewModel() {
    private val podcastRepository = PodcastRepository()
}

// After
class PodcastListViewModel(
    private val podcastRepository: PodcastRepository
) : ViewModel()
```

#### PodcastSearchViewModel.kt
```kotlin
// Before
class PodcastSearchViewModel : ViewModel() {
    private val apiClient = AppleSearchApiClient()
}

// After
class PodcastSearchViewModel(
    private val apiClient: AppleSearchApiClient
) : ViewModel()
```

#### PodcastDetailViewModel.kt
```kotlin
// Before
class PodcastDetailViewModel : ViewModel() {
    private val rssService = RssService()
    private val podcastRepository = PodcastRepository()
    private val downloadRepository = DownloadRepositoryBuilder.build()
}

// After
class PodcastDetailViewModel(
    private val rssService: RssService,
    private val podcastRepository: PodcastRepository,
    private val downloadRepository: DownloadRepository,
    private val navigateToPlayer: (EpisodeDisplayModel) -> Unit
) : ViewModel()
```

#### PlaybackRepository.kt
```kotlin
// After
class PlaybackRepository(
    private val episodeDao: EpisodeDao,
    private val playHistoryDao: PlayHistoryDao
)
```

---

## Step 4: 優先度高のテスト作成

### 新規ファイル
- `composeApp/src/commonTest/kotlin/jp/kztproject/simplepodcastplayer/util/RssParserTest.kt`
- `composeApp/src/commonTest/kotlin/jp/kztproject/simplepodcastplayer/util/EpisodeUtilsTest.kt`

### RssParserTest.kt テストケース
| メソッド | テストケース |
|----------|-------------|
| `parseRssFeed()` | 正常RSS、空コンテンツ、不正XML、CDATA処理、HTMLエンティティ |
| `parseItunesDuration()` | HH:MM:SS、MM:SS、秒数のみ、不正値 |
| `formatRssDate()` | 標準フォーマット、カンマなし、不正フォーマット |
| `toEpisodes()` | 正常変換、空リスト、guidなし |

### EpisodeUtilsTest.kt テストケース
| メソッド | テストケース |
|----------|-------------|
| `formatDuration()` | 1時間以上、1時間未満、0秒、境界値 |
| `formatPublishedDate()` | ISO形式、不正形式、空文字 |
| `toDisplayModel()` | 正常変換、ダウンロード済みフラグ |

---

## Step 5: Fakeクラス作成

### 新規ファイル
- `composeApp/src/commonTest/kotlin/jp/kztproject/simplepodcastplayer/fake/FakePodcastDao.kt`
- `composeApp/src/commonTest/kotlin/jp/kztproject/simplepodcastplayer/fake/FakeEpisodeDao.kt`
- `composeApp/src/commonTest/kotlin/jp/kztproject/simplepodcastplayer/fake/FakePlayHistoryDao.kt`
- `composeApp/src/commonTest/kotlin/jp/kztproject/simplepodcastplayer/fake/FakeAppleSearchApiClient.kt`
- `composeApp/src/commonTest/kotlin/jp/kztproject/simplepodcastplayer/fake/FakeRssService.kt`
- `composeApp/src/commonTest/kotlin/jp/kztproject/simplepodcastplayer/fake/FakeDownloadRepository.kt`
- `composeApp/src/commonTest/kotlin/jp/kztproject/simplepodcastplayer/fake/TestDataFactory.kt`

---

## Step 6: 優先度中のテスト作成

### 新規ファイル
- `composeApp/src/commonTest/kotlin/jp/kztproject/simplepodcastplayer/data/repository/PodcastRepositoryTest.kt`
- `composeApp/src/commonTest/kotlin/jp/kztproject/simplepodcastplayer/screen/PodcastListViewModelTest.kt`
- `composeApp/src/commonTest/kotlin/jp/kztproject/simplepodcastplayer/screen/PodcastSearchViewModelTest.kt`
- `composeApp/src/commonTest/kotlin/jp/kztproject/simplepodcastplayer/screen/PodcastDetailViewModelTest.kt`

### テストケース概要

#### PodcastRepositoryTest
- `subscribeToPodcast` - Podcast/Episode保存
- `unsubscribeFromPodcast` - 購読解除
- `isSubscribed` - 購読状態確認
- `getEpisodesByPodcastId` - エピソード取得

#### PodcastListViewModelTest
- 初期化時の購読済みPodcast読み込み
- UI状態の更新

#### PodcastSearchViewModelTest
- 検索クエリ更新
- 検索成功/エラー時の状態
- ローディング状態

#### PodcastDetailViewModelTest
- Podcast/エピソード読み込み
- 購読/購読解除
- ダウンロード処理

---

## 実装順序

1. **依存関係追加** - libs.versions.toml, build.gradle.kts
2. **DIモジュール作成** - AppModule.kt
3. **App.kt修正** - KoinApplication設定
4. **Repository リファクタリング** - PodcastRepository, PlaybackRepository
5. **ViewModel リファクタリング** - 各ViewModel
6. **Screen修正** - koinViewModelの使用
7. **ビルド確認** - `./gradlew build`
8. **優先度高テスト作成** - RssParserTest, EpisodeUtilsTest
9. **Fakeクラス作成** - 各Fakeクラス
10. **優先度中テスト作成** - Repository/ViewModelテスト
11. **全テスト実行** - `./gradlew test`

---

## 主要ファイル一覧

### 変更ファイル
| ファイル | 変更内容 |
|----------|----------|
| `gradle/libs.versions.toml` | Koin, coroutines-test, turbine追加 |
| `composeApp/build.gradle.kts` | 依存関係追加 |
| `App.kt` | KoinApplication設定 |
| `PodcastRepository.kt` | コンストラクタインジェクション |
| `PlaybackRepository.kt` | コンストラクタインジェクション |
| `PodcastListViewModel.kt` | コンストラクタインジェクション |
| `PodcastSearchViewModel.kt` | コンストラクタインジェクション |
| `PodcastDetailViewModel.kt` | コンストラクタインジェクション |
| `PodcastListScreen.kt` | koinViewModel使用 |
| `PodcastSearchScreen.kt` | koinViewModel使用 |
| `PodcastDetailScreen.kt` | koinViewModel使用 |

### 新規ファイル
| ファイル | 内容 |
|----------|------|
| `di/AppModule.kt` | Koin DIモジュール定義 |
| `util/RssParserTest.kt` | RssParserのテスト |
| `util/EpisodeUtilsTest.kt` | EpisodeUtilsのテスト |
| `fake/FakePodcastDao.kt` | PodcastDaoのFake実装 |
| `fake/FakeEpisodeDao.kt` | EpisodeDaoのFake実装 |
| `fake/FakePlayHistoryDao.kt` | PlayHistoryDaoのFake実装 |
| `fake/FakeAppleSearchApiClient.kt` | APIクライアントのFake |
| `fake/FakeRssService.kt` | RssServiceのFake |
| `fake/FakeDownloadRepository.kt` | DownloadRepositoryのFake |
| `fake/TestDataFactory.kt` | テストデータ生成ヘルパー |
| `repository/PodcastRepositoryTest.kt` | Repositoryテスト |
| `screen/PodcastListViewModelTest.kt` | ViewModelテスト |
| `screen/PodcastSearchViewModelTest.kt` | ViewModelテスト |
| `screen/PodcastDetailViewModelTest.kt` | ViewModelテスト |