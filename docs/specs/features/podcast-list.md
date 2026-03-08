# PodcastListScreen 仕様

## 機能要件

- **購読一覧**: 購読中のポッドキャストをカード形式で一覧表示
- **検索への導線**: 検索画面へのナビゲーションボタン
- **詳細への導線**: ポッドキャストをタップで詳細画面へ遷移

## 画面構成要素

- **ヘッダー**
  - タイトル「My Podcasts」
  - 検索ボタン（右上）
- **一覧エリア**
  - 購読中ポッドキャストの LazyColumn
  - 各アイテム: サムネイル（80dp）、タイトル（最大2行）、著者（最大1行）、説明（最大2行）
- **空状態**: 購読なし時に「No subscribed podcasts」メッセージを表示
- **ローディング**: 初期読み込み中は CircularProgressIndicator を表示

## 画面遷移

| 操作 | 遷移先 |
|---|---|
| 検索ボタンタップ | PodcastSearchScreen（`search` ルート） |
| ポッドキャストアイテムタップ | PodcastDetailScreen（`list_detail` ルート、podcastId を渡す） |

## 状態管理

```kotlin
data class PodcastListUiState(
    val subscribedPodcasts: List<PodcastEntity> = emptyList(),
    val isLoading: Boolean = true,
)
```

## データベース

**Podcast テーブル**

| カラム | 型 | 説明 |
|---|---|---|
| id | Long (PK) | 内部ID |
| name | String | ポッドキャスト名 |
| artistName | String | 著者名 |
| description | String | 説明 |
| imageUrl | String? | アートワークURL |
| feedUrl | String | RSSフィードURL |
| subscribed | Boolean | 購読状態 |
| subscribedAt | Long | 購読日時（UnixTime） |
