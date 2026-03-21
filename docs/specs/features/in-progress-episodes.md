# InProgressEpisodesScreen 仕様

## 機能要件

- **途中再生一覧**: 途中まで聴いたエピソードを一覧で表示
- **進捗の可視化**: 各エピソードの再生進捗率（プログレスバー）と残り時間を表示
- **再生再開**: アイテムタップで PlayerScreen へ遷移し、途中から再開
- **自動更新**: エピソードを最後まで聴くと一覧から自動的に消える

## 「途中再生」の定義

`lastPlaybackPosition > 0 AND listened = false`

- `lastPlaybackPosition > 0`: 再生を開始している
- `listened = false`: 最後まで聴いていない（95%以上再生で `true` に更新される）

## エントリポイント

PodcastListScreen のヘッダー行直下に配置した「In Progress」OutlinedButton からアクセス。

## 画面構成要素

- **ヘッダー**
  - 戻るボタン（ArrowBack アイコン）
  - タイトル「In Progress」
- **一覧エリア**
  - 途中再生エピソードの LazyColumn（`lastPlaybackPosition` の降順）
  - 各アイテム:
    - アートワーク（64dp 正方形、角丸8dp）
    - エピソードタイトル（最大2行）
    - ポッドキャスト名（最大1行）
    - LinearProgressIndicator（再生進捗率）
    - 残り時間テキスト（「X:XX remaining」形式）
- **空状態**: 該当エピソードなし時に「No episodes in progress」を表示
- **ローディング**: 初期読み込み中は CircularProgressIndicator を表示

## 画面遷移

| 操作 | 遷移先 |
|---|---|
| 戻るボタンタップ | 前画面（popBackStack） |
| エピソードアイテムタップ | PlayerScreen（`player` ルート、Episode + Podcast を渡す） |

**遷移元**: PodcastListScreen の「In Progress」ボタン（`in_progress` ルート）

## 状態管理

### InProgressEpisodesUiState

```kotlin
data class InProgressEpisodesUiState(
    val isLoading: Boolean = true,
    val episodes: List<InProgressEpisodeUiItem> = emptyList(),
)
```

### InProgressEpisodeUiItem

```kotlin
data class InProgressEpisodeUiItem(
    val episode: Episode,           // PlayerScreen 遷移用
    val podcast: Podcast,           // PlayerScreen 遷移用
    val episodeTitle: String,
    val podcastName: String,
    val artworkUrl: String?,
    val progressPercent: Int,           // 0〜100
    val remainingTimeFormatted: String, // "X:XX" 形式
)
```

### 計算ロジック

- `progressPercent = (lastPlaybackPosition / 1000.0 / duration * 100).toInt().coerceIn(0, 100)`
- `remainingSeconds = (duration - lastPlaybackPosition / 1000).coerceAtLeast(0L)`
- **単位注意**: `duration` は秒、`lastPlaybackPosition` はミリ秒

## データベース

**Episodes テーブル（参照カラム）**

| カラム | 型 | 説明 |
|---|---|---|
| lastPlaybackPosition | Long | 再生位置（ミリ秒）|
| listened | Boolean | 聴取済みフラグ |
| duration | Long | エピソード総再生時間（秒） |
| podcastId | String | 紐づくポッドキャストのID |

**クエリ**

```sql
SELECT * FROM episodes
WHERE lastPlaybackPosition > 0 AND listened = 0
ORDER BY lastPlaybackPosition DESC
```

## 実装ファイル

| ファイル | 役割 |
|---|---|
| `commonMain/screen/InProgressEpisodesScreen.kt` | UI（Composable） |
| `commonMain/screen/InProgressEpisodesViewModel.kt` | ViewModel |
| `commonMain/screen/InProgressEpisodesUiState.kt` | UiState・UiItem データクラス |
| `commonMain/data/database/dao/EpisodeDao.kt` | `getInProgressEpisodes()` クエリ |
| `commonMain/data/repository/PlaybackRepository.kt` | `getInProgressEpisodes()` メソッド（`IPlaybackRepository` 経由） |
