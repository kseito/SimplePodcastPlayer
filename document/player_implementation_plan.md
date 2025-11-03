# PlayerScreen 実装計画

## 概要

要件定義書に基づき、プラットフォーム固有のUIを持つ完全機能のPlayerScreenを実装します。

- **Android**: Jetpack Compose + ExoPlayer + MediaSession
- **iOS**: SwiftUI + AVPlayer + Control Center
- **共通部分**: ViewModel、状態管理、ビジネスロジック
- **データ永続化**: Room Database（再生位置、再生履歴）

## 実装方針

### 選択した実装アプローチ
1. **UI層**: プラットフォーム固有（Android: androidMain、iOS: SwiftUI）
2. **スコープ**: フル実装（バックグラウンド再生、システム統合含む）
3. **データベース**: Room統合（再生位置の永続化）
4. **ナビゲーション**: モーダル/独立Activity方式

## Phase 1: データベース層（Room）

### 1.1 依存関係の追加
**ファイル**: `composeApp/build.gradle.kts`

```kotlin
// Room dependencies
implementation(libs.room.runtime)
implementation(libs.room.ktx)
ksp(libs.room.compiler)
```

**ファイル**: `gradle/libs.versions.toml`
- Room のバージョン定義を追加

### 1.2 データベースエンティティの作成

**ファイル**: `composeApp/src/commonMain/kotlin/jp/kztproject/simplepodcastplayer/data/database/entity/PodcastEntity.kt`
```kotlin
@Entity(tableName = "podcasts")
data class PodcastEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val author: String,
    val subscribed: Boolean,
    val subscribedAt: Long
)
```

**ファイル**: `composeApp/src/commonMain/kotlin/jp/kztproject/simplepodcastplayer/data/database/entity/EpisodeEntity.kt`
```kotlin
@Entity(tableName = "episodes")
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val podcastId: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val duration: Long,
    val publishedAt: String,
    val listened: Boolean,
    val lastPlaybackPosition: Long = 0L
)
```

**ファイル**: `composeApp/src/commonMain/kotlin/jp/kztproject/simplepodcastplayer/data/database/entity/PlayHistoryEntity.kt`
```kotlin
@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val episodeId: String,
    val playedAt: Long,
    val position: Long,
    val completed: Boolean
)
```

### 1.3 DAOの作成

**ファイル**: `composeApp/src/commonMain/kotlin/jp/kztproject/simplepodcastplayer/data/database/dao/PodcastDao.kt`
- insert, delete, getAll, getById, updateSubscription メソッド

**ファイル**: `composeApp/src/commonMain/kotlin/jp/kztproject/simplepodcastplayer/data/database/dao/EpisodeDao.kt`
- insert, update, getById, getByPodcastId, updateListenedStatus, updatePlaybackPosition メソッド

**ファイル**: `composeApp/src/commonMain/kotlin/jp/kztproject/simplepodcastplayer/data/database/dao/PlayHistoryDao.kt`
- insert, getByEpisodeId, getRecent メソッド

### 1.4 データベースクラスの作成

**ファイル**: `composeApp/src/commonMain/kotlin/jp/kztproject/simplepodcastplayer/data/database/AppDatabase.kt`
```kotlin
@Database(
    entities = [PodcastEntity::class, EpisodeEntity::class, PlayHistoryEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun playHistoryDao(): PlayHistoryDao
}
```

### 1.5 Repositoryの作成

**ファイル**: `composeApp/src/commonMain/kotlin/jp/kztproject/simplepodcastplayer/data/repository/PlaybackRepository.kt`
- 再生位置の保存・取得
- 再生履歴の記録
- エピソードの聴取状態更新

## Phase 2: 共通層（commonMain）

### 2.1 PlayerUiState定義

**ファイル**: `composeApp/src/commonMain/kotlin/jp/kztproject/simplepodcastplayer/screen/PlayerUiState.kt`
```kotlin
data class PlayerUiState(
    val episode: Episode?,
    val podcast: Podcast?,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isLoading: Boolean = false,
    val error: String? = null,
    val bufferedPosition: Long = 0L
)
```

### 2.2 PlayerViewModel共通インターフェース

**ファイル**: `composeApp/src/commonMain/kotlin/jp/kztproject/simplepodcastplayer/screen/PlayerViewModel.kt`
```kotlin
interface PlayerViewModel {
    val uiState: StateFlow<PlayerUiState>

    fun play()
    fun pause()
    fun seekTo(position: Long)
    fun skipForward(seconds: Int = 15)
    fun skipBackward(seconds: Int = 15)
    fun setPlaybackSpeed(speed: Float)
    fun loadEpisode(episode: Episode, podcast: Podcast)
    fun release()
}
```

### 2.3 Expect/Actual宣言

**ファイル**: `composeApp/src/commonMain/kotlin/jp/kztproject/simplepodcastplayer/player/AudioPlayer.kt`
```kotlin
expect class AudioPlayer {
    fun play()
    fun pause()
    fun seekTo(position: Long)
    fun setPlaybackSpeed(speed: Float)
    fun loadUrl(url: String)
    fun release()
    fun getCurrentPosition(): Long
    fun getDuration(): Long
    fun isPlaying(): Boolean
}
```

## Phase 3: Android実装

### 3.1 依存関係の追加

**ファイル**: `composeApp/build.gradle.kts`
```kotlin
// ExoPlayer (Media3)
implementation(libs.androidx.media3.exoplayer)
implementation(libs.androidx.media3.session)
implementation(libs.androidx.media3.ui)
```

### 3.2 PlaybackServiceの作成

**ファイル**: `composeApp/src/androidMain/kotlin/jp/kztproject/simplepodcastplayer/service/PlaybackService.kt`
- MediaSessionService を継承
- ExoPlayer の初期化
- MediaSession の設定
- Foreground Service として実行
- 通知の作成と更新
- 再生状態の監視

### 3.3 PlayerScreenの作成

**ファイル**: `composeApp/src/androidMain/kotlin/jp/kztproject/simplepodcastplayer/screen/PlayerScreen.kt`

**UI構成要素**:
- ヘッダー（閉じるボタン、"Now Playing"タイトル）
- エピソードアートワーク（300dp × 300dp）
- エピソードタイトル（headlineMedium、最大2行）
- ポッドキャスト名（bodyLarge、半透明、最大1行）
- エピソード説明（bodyMedium、半透明、最大3行）
- プログレスバー（Slider、現在位置/合計時間表示）
- コントロールボタン（15秒戻る、再生/一時停止、15秒進む）
- 再生速度調整ボタン（オプション）

### 3.4 PlayerViewModelの実装

**ファイル**: `composeApp/src/androidMain/kotlin/jp/kztproject/simplepodcastplayer/screen/PlayerViewModelImpl.kt`
- PlayerViewModel インターフェースを実装
- ExoPlayer との統合
- PlaybackService とのバインディング
- 再生位置の定期的な保存（PlaybackRepository 使用）
- 95%以上再生で listened = true に更新

### 3.5 AudioPlayerの実装

**ファイル**: `composeApp/src/androidMain/kotlin/jp/kztproject/simplepodcastplayer/player/AudioPlayer.kt`
- actual class AudioPlayer
- ExoPlayer のラッパー

### 3.6 PlayerActivityの作成

**ファイル**: `composeApp/src/androidMain/kotlin/jp/kztproject/simplepodcastplayer/activity/PlayerActivity.kt`
- ComponentActivity を継承
- Intent から Episode を受け取る
- PlayerScreen を表示
- フルスクリーンモーダル設定

### 3.7 AndroidManifest.xmlの更新

**ファイル**: `composeApp/src/androidMain/AndroidManifest.xml`
```xml
<!-- Permissions -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

<!-- Service -->
<service
    android:name=".service.PlaybackService"
    android:foregroundServiceType="mediaPlayback"
    android:exported="false">
    <intent-filter>
        <action android:name="androidx.media3.session.MediaSessionService" />
    </intent-filter>
</service>

<!-- Activity -->
<activity
    android:name=".activity.PlayerActivity"
    android:theme="@style/Theme.App"
    android:exported="false" />
```

### 3.8 PodcastDetailScreenの更新

**ファイル**: `composeApp/src/commonMain/kotlin/jp/kztproject/simplepodcastplayer/screen/PodcastDetailScreen.kt`
- `playEpisode()` の実装を expect 関数に変更
- Android: PlayerActivity を起動する actual 実装

## Phase 4: iOS実装

### 4.1 AudioPlayerManager.swiftの作成

**ファイル**: `iosApp/iosApp/Manager/AudioPlayerManager.swift`
- Singleton パターン
- AVPlayer の管理
- Remote Command Center の設定
- Now Playing Info の更新
- バックグラウンド再生対応
- 再生状態の監視（KVO）

### 4.2 PlayerView.swiftの作成

**ファイル**: `iosApp/iosApp/Screen/PlayerView.swift`

**UI構成要素**（Android と同様）:
- ヘッダー（閉じるボタン、"Now Playing"タイトル）
- エピソードアートワーク（300pt × 300pt）
- エピソードタイトル（.title、最大2行）
- ポッドキャスト名（.headline、半透明、最大1行）
- エピソード説明（.body、半透明、最大3行）
- プログレスバー（Slider、現在位置/合計時間表示）
- コントロールボタン（15秒戻る、再生/一時停止、15秒進む）
- 再生速度調整ボタン（オプション）

### 4.3 PlayerViewModel.swiftの作成

**ファイル**: `iosApp/iosApp/ViewModel/PlayerViewModel.swift`
- ObservableObject に準拠
- AudioPlayerManager との統合
- 共通インターフェースに準拠したメソッド実装
- 再生位置の定期的な保存（PlaybackRepository 使用）
- 95%以上再生で listened = true に更新

### 4.4 AudioPlayerの実装（iOS）

**ファイル**: `composeApp/src/iosMain/kotlin/jp/kztproject/simplepodcastplayer/player/AudioPlayer.kt`
- actual class AudioPlayer
- Swift の AudioPlayerManager を呼び出すブリッジ

### 4.5 Info.plistの更新

**ファイル**: `iosApp/iosApp/Info.plist`
```xml
<key>UIBackgroundModes</key>
<array>
    <string>audio</string>
</array>
```

### 4.6 PodcastDetailScreenの更新（iOS）

**ファイル**: `composeApp/src/iosMain/kotlin/jp/kztproject/simplepodcastplayer/screen/PodcastDetailScreen.kt`
- `playEpisode()` の actual 実装
- PlayerView をモーダル表示

## Phase 5: 統合とテスト

### 5.1 ナビゲーションの統合
- PodcastDetailScreen から PlayerScreen への遷移を実装
- Episode と Podcast オブジェクトの引き渡し

### 5.2 テスト項目

#### 基本再生機能
- [ ] エピソードの読み込みと再生
- [ ] 再生/一時停止の切り替え
- [ ] プログレスバーでのシーク
- [ ] 15秒スキップ（前進/後退）
- [ ] 再生速度変更（0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x）

#### バックグラウンド再生
- [ ] アプリをバックグラウンドにしても再生継続
- [ ] Android: 通知領域での再生制御
- [ ] iOS: Control Center での再生制御
- [ ] ロック画面での再生制御

#### データ永続化
- [ ] 再生位置の自動保存
- [ ] アプリ再起動後の再生位置復元
- [ ] 95%以上再生で listened = true 更新
- [ ] 再生履歴の記録

#### エラーハンドリング
- [ ] ネットワークエラー時のエラー表示
- [ ] 無効な音声URLの処理
- [ ] ストリーム読み込み失敗時のリトライ

#### UI/UX
- [ ] 画面遷移（モーダル表示）
- [ ] 閉じるボタン/スワイプでの画面終了
- [ ] 再生中にPlayerScreenを閉じても再生継続
- [ ] UI要素のレスポンシブ対応

### 5.3 パフォーマンステスト
- [ ] 長時間再生の安定性
- [ ] メモリリークのチェック
- [ ] バッテリー消費の確認

## ファイル構成まとめ

```
SimplePodcastPlayer/
├── composeApp/
│   ├── build.gradle.kts (更新: Room, ExoPlayer依存関係)
│   └── src/
│       ├── commonMain/kotlin/jp/kztproject/simplepodcastplayer/
│       │   ├── data/
│       │   │   ├── database/
│       │   │   │   ├── entity/
│       │   │   │   │   ├── PodcastEntity.kt (新規)
│       │   │   │   │   ├── EpisodeEntity.kt (新規)
│       │   │   │   │   └── PlayHistoryEntity.kt (新規)
│       │   │   │   ├── dao/
│       │   │   │   │   ├── PodcastDao.kt (新規)
│       │   │   │   │   ├── EpisodeDao.kt (新規)
│       │   │   │   │   └── PlayHistoryDao.kt (新規)
│       │   │   │   └── AppDatabase.kt (新規)
│       │   │   └── repository/
│       │   │       └── PlaybackRepository.kt (新規)
│       │   ├── screen/
│       │   │   ├── PlayerUiState.kt (新規)
│       │   │   ├── PlayerViewModel.kt (新規: interface)
│       │   │   └── PodcastDetailScreen.kt (更新)
│       │   └── player/
│       │       └── AudioPlayer.kt (新規: expect class)
│       ├── androidMain/kotlin/jp/kztproject/simplepodcastplayer/
│       │   ├── activity/
│       │   │   └── PlayerActivity.kt (新規)
│       │   ├── screen/
│       │   │   ├── PlayerScreen.kt (新規)
│       │   │   └── PlayerViewModelImpl.kt (新規)
│       │   ├── service/
│       │   │   └── PlaybackService.kt (新規)
│       │   └── player/
│       │       └── AudioPlayer.kt (新規: actual class)
│       └── iosMain/kotlin/jp/kztproject/simplepodcastplayer/
│           └── player/
│               └── AudioPlayer.kt (新規: actual class)
├── iosApp/iosApp/
│   ├── Manager/
│   │   └── AudioPlayerManager.swift (新規)
│   ├── Screen/
│   │   └── PlayerView.swift (新規)
│   ├── ViewModel/
│   │   └── PlayerViewModel.swift (新規)
│   └── Info.plist (更新)
└── gradle/
    └── libs.versions.toml (更新: Room, Media3バージョン定義)
```

## 実装の注意点

### Android
- ExoPlayer は Media3 ライブラリを使用（最新の推奨方式）
- MediaSessionService でシステムメディアコントロールと統合
- Foreground Service として実行し、通知を常に表示
- サービスのライフサイクル管理に注意

### iOS
- AVAudioSession の設定が必要（カテゴリ: playback）
- Remote Command Center でシステムコントロールに対応
- Now Playing Info Center に再生情報を設定
- バックグラウンド実行の Capability 設定

### データベース
- Room は KMP で実験的サポート（設定に注意）
- マイグレーション戦略の定義
- データベースのシングルトンインスタンス管理

### 状態管理
- 再生状態は ViewModel で一元管理
- プラットフォーム固有の Player から状態を収集
- UI は StateFlow を購読して自動更新

## 想定される課題と対策

### 課題1: Room の KMP サポート
- **対策**: 最新の Room KMP ライブラリを使用。必要に応じてプラットフォーム固有実装も検討

### 課題2: iOS での Kotlin-Swift ブリッジ
- **対策**: expect/actual パターンで分離。Swift 側の実装を Objective-C 互換で公開

### 課題3: バックグラウンド再生の権限とライフサイクル
- **対策**: 適切なパーミッション設定と Foreground Service（Android）/ Background Modes（iOS）の実装

### 課題4: 再生位置の同期
- **対策**: 定期的な保存（例: 5秒ごと）+ onPause/onStop 時の保存

## タイムライン見積もり

- **Phase 1 (Database)**: 2-3時間
- **Phase 2 (Common)**: 1-2時間
- **Phase 3 (Android)**: 4-6時間
- **Phase 4 (iOS)**: 4-6時間
- **Phase 5 (Integration & Testing)**: 3-4時間

**合計**: 14-21時間

---

**作成日**: 2025-10-29
**バージョン**: 1.0
