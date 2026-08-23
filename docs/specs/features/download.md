# エピソードダウンロード機能 仕様

エピソードの音声ファイルを端末に保存し、オフライン再生を可能にする機能。
画面としては `PodcastDetailScreen` のエピソード一覧から操作する（[podcast-detail.md](podcast-detail.md) 参照）。

## 機能要件

- **ダウンロード**: エピソードの音声ファイルを端末ローカルストレージに保存
- **進捗表示**: ダウンロード中は進捗をリアルタイムに表示
- **ダウンロード削除**: 保存済みファイルの削除（エピソード単位 / 一括）
- **オフライン再生**: ダウンロード済みエピソードはローカルファイルから再生（[player.md](player.md) 参照）
- **状態の永続化**: ダウンロード状態を DB に保存し、アプリ再起動後も保持

## UI（PodcastDetailScreen のエピソード行）

`DownloadButton` コンポーザブルが状態に応じて表示を切り替える。

| 状態 | 表示 | タップ時の動作 |
|---|---|---|
| 未ダウンロード | ダウンロードアイコン（`Icons.Default.Download`、primary 色） | ダウンロード開始 |
| ダウンロード中（`progress > 0f`） | 確定的 `CircularProgressIndicator`（24dp） | なし |
| ダウンロード中（`progress == 0f`） | 不確定 `CircularProgressIndicator`（24dp） | なし |
| ダウンロード済み | 削除アイコン（`Icons.Default.Delete`、error 色） | ダウンロード削除 |

進捗不明（開始直後、または `Content-Length` が取得できず進捗が `0f` のまま更新されない場合）は
不確定インジケーターとして表示される。

## 一括削除

不要になった音声ファイルをまとめて削除する導線。自動削除は行わず、**必ずユーザーの明示的な操作を起点**とする。

### 1. 購読解除時の削除（PodcastDetailScreen）

購読解除は「もう不要」という明示的な意思表示のため、確認の上でそのポッドキャストのダウンロードを全件削除する。

| 状況 | 挙動 |
|---|---|
| ダウンロード済みが 1 件以上 | 確認ダイアログ「N downloaded episode(s) will also be deleted.」を表示。[Unsubscribe] で削除 + 購読解除、[Cancel] で何もしない |
| ダウンロード済みが 0 件 | ダイアログを出さず即座に購読解除 |

- 確認前は購読状態・ファイルとも一切変更しない（`PodcastDetailUiState.unsubscribeConfirmDownloadCount` が null 以外の間はダイアログ表示中）
- 削除するのはファイルとダウンロード状態のみ。**エピソードのレコードは残す**ため、再購読時に再生位置と聴取済みフラグが復活する
- 購読解除済みポッドキャストのエピソードは `InProgressEpisodesScreen` に表示しない（`subscribed` で除外）

### 2. 聴き終わったエピソードの一括削除（PodcastListScreen）

ヘッダーのゴミ箱アイコン（`Icons.Default.Delete`）から、聴取済み（`listened = true`）かつダウンロード済みのエピソードを購読横断でまとめて削除する。

| 状況 | ダイアログ内容 | ボタン |
|---|---|---|
| 対象が 1 件以上 | 「Delete the downloaded audio of N listened episode(s)?」 | [Delete] / [Cancel] |
| 対象が 0 件 | 「There are no listened downloads to delete.」 | [OK] のみ |

- 削除完了後は Snackbar で「Deleted N download(s)」を表示（失敗時は「Failed to delete downloads」）
- 実行中は `isCleaningUp = true` でゴミ箱ボタンを無効化
- 聴取途中のエピソードは対象外。聴き直す可能性を考慮し、自動削除・猶予期間つき削除は採用しない

## 状態管理

### DownloadState（commonMain）

```kotlin
sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()  // 0.0f〜1.0f
    data object Completed : DownloadState()
    data class Failed(val error: String) : DownloadState()
}
```

- `PodcastDetailUiState.downloadStates: Map<String, DownloadState>` でエピソードIDごとに進捗を保持
- ダウンロード開始時に `Downloading(0f)` をセットし、以降 Flow で受け取った状態で更新
- `Completed` 受信時にエピソードの `isDownloaded` を true に更新
- `Failed` 受信時・例外発生時は `error = "Download failed"` をセット

## クラス構成

```text
PodcastDetailViewModel ──→ IDownloadRepository（interface, commonMain）
PodcastListViewModel  ──┐          ↑
                        │   DownloadRepository（expect/actual）
                        │          │ 委譲
                        │   AudioDownloader（expect/actual）
                        │          │
                        │   EpisodeDao.updateDownloadStatus()
                        │
                        └─→ IDownloadCleanupRepository（commonMain）
                                   │ 対象を EpisodeDao で抽出し
                                   └─→ IDownloadRepository.deleteDownload() を反復
```

| クラス | 役割 |
|---|---|
| `IDownloadRepository` | リポジトリのインターフェース。テスト時は `FakeDownloadRepository` に差し替え |
| `DownloadCleanupRepository` | 一括削除。削除条件を共通化するため commonMain の通常クラスとして実装し、ファイル削除は `IDownloadRepository` に委譲する |
| `DownloadRepository` | ダウンロード実行と DB 更新の統合。`expect class` で各プラットフォームに actual 実装 |
| `DownloadRepositoryBuilder` | `expect object`。プラットフォームごとの `DownloadRepository` 生成（Android は `Context` が必要） |
| `AudioDownloader` | `expect class`。HTTP ダウンロードとファイル操作の実体 |

### AudioDownloader API

```kotlin
expect class AudioDownloader {
    suspend fun downloadAudio(url: String, episodeId: String): Flow<DownloadState>
    fun getLocalFilePath(episodeId: String): String?
    suspend fun deleteDownload(episodeId: String): Boolean
    fun isDownloaded(episodeId: String): Boolean
}
```

## ダウンロード処理仕様（共通）

- HTTP クライアント: Ktor `HttpClient`（`prepareGet` + `bodyAsChannel` でストリーミング取得）
- バッファサイズ: 8192 バイト
- ファイル名: エピソードIDの英数字以外を `_` に置換し `.mp3` を付与（例: `abc-123` → `abc_123.mp3`）
- 保存ディレクトリ: `podcast_downloads/`（存在しなければ作成）
- 進捗: 開始時に必ず `Downloading(0f)` を送出。以降は `Content-Length` が取得できた場合のみ進捗値を更新送出（取得できない場合は `0f` のままとなり、UI では不確定表示になる）
- 例外発生時は `Failed(message)` を送出（Flow はエラー終了しない）
- 実行コンテキスト: `Dispatchers.IO`

### プラットフォーム別差分

| 項目 | Android | iOS |
|---|---|---|
| 保存先 | `context.filesDir/podcast_downloads/` | Documents ディレクトリ配下 `podcast_downloads/` |
| ファイル I/O | `java.io.File` | `NSFileManager` + POSIX（`fopen`/`fwrite`/`fclose`） |
| 進捗送出 | チャンク読み込みごと | 100ms 間隔でスロットリング |
| コンストラクタ | `AudioDownloader(context: Context)` | `AudioDownloader()` |

## データベース連携

`DownloadRepository` が `DownloadState.Completed` を検知した時点で `EpisodeDao.updateDownloadStatus()` を呼び、Episode テーブルを更新する。

| カラム | ダウンロード完了時 | 削除時 |
|---|---|---|
| isDownloaded | true | false |
| localFilePath | ローカルファイルパス | null |
| downloadedAt | 現在時刻（UnixTime ミリ秒） | 0 |

## オフライン再生との連携

`BasePlayerViewModel` は再生開始時に `downloadRepository.getLocalFilePath(episode.id)` を確認し、
ローカルファイルが存在すればそのパスを、なければ `episode.audioUrl`（ストリーミング）を再生する。

## エラー処理

- ネットワークエラー・ファイル書き込みエラーは `DownloadState.Failed(error)` として通知
- iOS はファイルオープン・書き込み失敗時に `DownloadDataCreationException` を送出し `Failed` に変換
- 削除失敗時は `PodcastDetailUiState.error = "Failed to delete download"` をセット

## テスト

- `FakeDownloadRepository`（commonTest の `fake` パッケージ）で `IDownloadRepository` を差し替え
  - コンストラクタに `EpisodeDao` を渡すと、本物と同じくダウンロード状態を DB へ書き戻す（DB 状態を検証するテスト向け）
- `PodcastDetailViewModelTest` でダウンロード開始・完了・失敗・削除の状態遷移と、購読解除の確認 / 実行 / キャンセルを検証
- `PodcastListViewModelTest` で聴取済み一括削除の件数集計・削除・キャンセルを検証
- `DownloadCleanupRepositoryTest` で削除対象の抽出条件（ポッドキャスト単位 / 聴取済み）を検証
