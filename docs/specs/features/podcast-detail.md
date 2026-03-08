# PodcastDetailScreen 仕様

## 機能要件

- **購読管理**: ポッドキャストの購読・購読解除
- **エピソード一覧**: エピソードのリスト表示
- **エピソード再生**: 再生ボタンタップでプレイヤー画面へ遷移
- **エピソード更新**: 購読済みポッドキャストのエピソードを Apple API から最新化
- **エピソードダウンロード**: エピソードのオフライン再生用ダウンロード

## 画面構成要素

- **ヘッダー**
  - 戻るボタン（←）
  - タイトル「Podcast Detail」
- **ポッドキャスト情報**
  - サムネイル画像（200dp × 200dp）: `bestArtworkUrl()` で最高品質画像を表示、なければアイコン
  - タイトル（`podcast.trackName`）: Typography headlineSmall、最大2行
  - 著者（`podcast.artistName`）: Typography bodyLarge、最大1行、色 onSurfaceVariant
  - 購読ボタン: 購読済み→「購読中」（無効）/ 未購読→「購読する」（アクティブ）
- **エピソード一覧**
  - セクションタイトル「Episodes」
  - LazyColumn: タイトル、公開日、再生時間、再生ボタン、ダウンロードボタン

## 画面遷移

| 操作 | 遷移先 |
|---|---|
| 戻るボタン | 前画面（popBackStack） |
| エピソード再生ボタン | PlayerScreen（`player` ルート、Episode + Podcast を渡す） |

**遷移元パターン**

- `PodcastSearchScreen` → `detail` ルート: `Podcast` オブジェクトを渡す
- `PodcastListScreen` → `list_detail` ルート: `podcastId`（Long）を渡し VM で Podcast を解決

## 状態管理

```kotlin
data class PodcastDetailUiState(
    val podcast: Podcast? = null,
    val episodes: List<EpisodeDisplayModel> = emptyList(),
    val isSubscribed: Boolean = false,
    val isLoading: Boolean = true,
    val isSubscriptionLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)
```

## データベース

**Episode テーブル**

| カラム | 型 | 説明 |
|---|---|---|
| id | String (PK) | エピソードID（GUID） |
| podcastId | String (FK) | 親ポッドキャストID |
| title | String | エピソードタイトル |
| description | String | 説明 |
| audioUrl | String | 音声ファイルURL |
| duration | Long | 再生時間（ミリ秒） |
| publishedAt | Long | 公開日時（UnixTime） |
| listened | Boolean | 聴取済みフラグ |

**EpisodeDisplayModel**（表示用モデル、DB エンティティではない）

| フィールド | 型 | 説明 |
|---|---|---|
| id | String | エピソードID |
| title | String | タイトル |
| description | String | 説明 |
| publishedAt | String | フォーマット済み日付 |
| duration | String | フォーマット済み再生時間（例: "12:34"） |
| audioUrl | String | 音声URL |
| listened | Boolean | 聴取済みフラグ |
| isDownloaded | Boolean | ダウンロード済みフラグ |
