# PodcastSearchScreen 仕様

## 機能要件

- **キーワード検索**: 入力したキーワードで Apple Podcasts API を呼び出し検索
- **検索結果表示**: ポッドキャスト名・説明・カバー画像を含む一覧表示
- **詳細への導線**: 検索結果アイテムをタップで詳細画面へ遷移

## 画面構成要素

- **検索バー**: テキスト入力フィールド + 検索実行ボタン
- **検索結果一覧**: LazyColumn
  - 各アイテム: サムネイル、タイトル、著者、説明
- **空状態**: 検索前または結果ゼロ時のメッセージ
- **ローディング**: API 呼び出し中は CircularProgressIndicator を表示
- **エラー表示**: API エラー時にメッセージを表示

## 画面遷移

| 操作 | 遷移先 |
|---|---|
| 検索結果アイテムタップ | PodcastDetailScreen（`detail` ルート、Podcast オブジェクトを渡す） |
| 戻るボタン | PodcastListScreen |

## 外部 API

**Apple Podcasts Search API**

- エンドポイント: `https://itunes.apple.com/search`
- パラメータ: `term={keyword}&media=podcast&entity=podcast`
- レスポンス: `Podcast` オブジェクトのリスト（trackId, trackName, artistName, artworkUrl100, feedUrl 等）

## 状態管理

```kotlin
data class PodcastSearchUiState(
    val searchResults: List<Podcast> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
```
