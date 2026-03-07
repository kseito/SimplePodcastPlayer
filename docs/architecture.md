# アーキテクチャ・実装パターン

## ディレクトリ構造

```
composeApp/src/
├── commonMain/kotlin/jp/kztproject/simplepodcastplayer/
│   ├── App.kt                     # エントリポイント、KoinApplication + NavHost
│   ├── Platform.kt                # expect/actual 宣言
│   ├── data/
│   │   ├── PodcastModels.kt       # Podcast / Episode データクラス
│   │   ├── RssModels.kt           # RSS フィードモデル
│   │   ├── AppleSearchApiClient.kt
│   │   ├── IAppleSearchApiClient.kt
│   │   ├── RssService.kt
│   │   ├── IRssService.kt
│   │   ├── database/
│   │   │   ├── AppDatabase.kt     # Room DB 定義
│   │   │   ├── DatabaseBuilder.kt # expect 宣言
│   │   │   ├── dao/               # PodcastDao / EpisodeDao / PlayHistoryDao
│   │   │   └── entity/            # PodcastEntity / EpisodeEntity / PlayHistoryEntity
│   │   └── repository/
│   │       ├── PodcastRepository.kt
│   │       ├── IPodcastRepository.kt
│   │       ├── PlaybackRepository.kt
│   │       ├── DownloadRepository.kt（expect）
│   │       └── IDownloadRepository.kt
│   ├── di/
│   │   └── AppModule.kt           # Koin モジュール定義
│   ├── player/
│   │   └── AudioPlayer.kt         # expect 宣言
│   ├── download/
│   │   ├── AudioDownloader.kt     # expect 宣言
│   │   └── DownloadState.kt
│   ├── screen/
│   │   ├── PodcastListScreen.kt
│   │   ├── PodcastListViewModel.kt
│   │   ├── PodcastSearchScreen.kt
│   │   ├── PodcastSearchViewModel.kt
│   │   ├── PodcastDetailScreen.kt
│   │   ├── PodcastDetailViewModel.kt
│   │   ├── PlayerScreen.kt
│   │   ├── PlayerViewModel.kt     # interface + BasePlayerViewModel
│   │   ├── PlayerUiState.kt
│   │   └── RememberPlayerViewModel.kt  # expect 宣言
│   ├── ui/
│   │   └── HtmlText.kt            # expect 宣言
│   └── util/
│       ├── RssParser.kt
│       └── EpisodeUtils.kt
├── androidMain/kotlin/jp/kztproject/simplepodcastplayer/
│   ├── MainActivity.kt
│   ├── AndroidPlatform.kt
│   ├── activity/PlayerActivity.kt
│   ├── service/PlaybackService.kt
│   ├── data/database/DatabaseBuilder.android.kt
│   ├── data/repository/DownloadRepository.android.kt
│   ├── player/AudioPlayer.android.kt
│   ├── download/AudioDownloader.android.kt
│   ├── screen/
│   │   ├── PlayerViewModelImpl.kt
│   │   └── RememberPlayerViewModel.android.kt
│   └── ui/HtmlText.android.kt
└── iosMain/kotlin/jp/kztproject/simplepodcastplayer/
    ├── MainViewController.kt
    ├── IOSPlatform.kt
    ├── data/database/DatabaseBuilder.ios.kt
    ├── data/repository/DownloadRepository.ios.kt
    ├── player/AudioPlayer.ios.kt
    ├── download/AudioDownloader.ios.kt
    ├── screen/
    │   ├── PlayerViewModelImpl.ios.kt
    │   └── RememberPlayerViewModel.ios.kt
    └── ui/HtmlText.ios.kt
```

## expect/actual パターン

プラットフォーム固有実装が必要なクラスには expect/actual パターンを使用する。

### ファイル命名規則

| ファイル | 役割 |
|---|---|
| `Foo.kt` (commonMain) | `expect` 宣言または共通インターフェース |
| `Foo.android.kt` (androidMain) | Android `actual` 実装 |
| `Foo.ios.kt` (iosMain) | iOS `actual` 実装 |

### 代表例: DatabaseBuilder

```kotlin
// commonMain/DatabaseBuilder.kt
expect object DatabaseBuilder {
    fun build(): AppDatabase
}

// androidMain/DatabaseBuilder.android.kt
actual object DatabaseBuilder {
    private lateinit var appContext: Context
    fun init(context: Context) { appContext = context }
    actual fun build(): AppDatabase = Room.databaseBuilder(appContext, ...).build()
}

// iosMain/DatabaseBuilder.ios.kt
actual object DatabaseBuilder {
    actual fun build(): AppDatabase = Room.databaseBuilder(producePath("app.db"), ...).build()
}
```

## Koin DI 設定

### AppModule.kt の構造

```kotlin
val appModule = module {
    // Database（singleton）
    single { DatabaseBuilder.build() }
    single { get<AppDatabase>().podcastDao() }

    // Repository（singleton）
    single<IPodcastRepository> { PodcastRepository(get(), get()) }
    factory<IDownloadRepository> { DownloadRepositoryBuilder.build() }

    // Services（factory）
    factory<IAppleSearchApiClient> { AppleSearchApiClient() }

    // ViewModels
    viewModel { PodcastListViewModel(get()) }
    viewModel { params ->
        PodcastDetailViewModel(
            podcastRepository = get(),
            onNavigateToPlayer = params.get<(Episode, Podcast) -> Unit>(),
        )
    }
}
```

### 新規依存関係の追加手順

1. `AppModule.kt` に `single` / `factory` / `viewModel` ブロックを追加
2. ViewModel はコンストラクタ引数で受け取る（`get()` で解決）
3. プラットフォーム固有依存はビルダークラス経由で提供（例: `DownloadRepositoryBuilder`）

## データフロー

```
Screen
  └── koinViewModel() で ViewModel 取得
        └── StateFlow<UiState> を collectAsStateWithLifecycle()
              └── ViewModel が Repository を呼び出す
                    └── Repository が DAO / API Client を呼び出す
```

- ViewModel → `viewModelScope.launch` で非同期処理
- Repository → Flow または suspend 関数でデータ提供
- Screen → `collectAsStateWithLifecycle()` で状態を購読

## ナビゲーション定義

`App.kt` の `NavHost` でルートを管理する。

```kotlin
NavHost(navController = navController, startDestination = "list") {
    composable("list") { PodcastListScreen(...) }
    composable("search") { PodcastSearchScreen(...) }
    composable("detail") { /* selectedPodcast.value を参照 */ }
    composable("player") { /* selectedEpisode.value を参照 */ }
}
```

- ルート間のデータ受け渡しは `remember { mutableStateOf<T?>(null) }` で管理
- 画面遷移は `navController.navigate("route")` / `navController.popBackStack()`
- 新しい画面を追加する際は `composable("new_route") { ... }` ブロックを追加

## データベース設定

### AppDatabase

```kotlin
@Database(
    entities = [PodcastEntity::class, EpisodeEntity::class, PlayHistoryEntity::class],
    version = 3,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun playHistoryDao(): PlayHistoryDao
}
```

### 新しいテーブルを追加する手順

1. `entity/` に `@Entity` アノテーション付きデータクラスを作成
2. `dao/` に `@Dao` インターフェースを作成
3. `AppDatabase` の `entities` リストと `version` を更新
4. `AppDatabase` に `abstract fun newTableDao(): NewTableDao` を追加
5. `AppModule.kt` に `single { get<AppDatabase>().newTableDao() }` を追加
6. スキーマ移行が必要な場合は `Migration` を定義してビルダーに追加

## プラットフォーム初期化

### Android (MainActivity.kt)

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        DatabaseBuilder.init(this)           // Context が必要な初期化
        DownloadRepositoryBuilder.init(this)
        setContent { App() }
    }
}
```

Context を必要とするプラットフォーム固有クラスは `init(context)` パターンで初期化する。

### iOS (MainViewController.kt)

iOS は `DatabaseBuilder.build()` 時にファイルパスを直接指定するため、`init()` 呼び出しは不要。
