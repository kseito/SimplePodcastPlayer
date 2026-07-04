# エピソードダウンロード機能 仕様

エピソードの音声ファイルを端末に保存し、オフライン再生を可能にする機能。
画面としては `PodcastDetailScreen` のエピソード一覧から操作する（[podcast-detail.md](podcast-detail.md) 参照）。

## 機能要件

- **ダウンロード**: エピソードの音声ファイルを端末ローカルストレージに保存
- **進捗表示**: ダウンロード中は進捗をリアルタイムに表示
- **ダウンロード削除**: 保存済みファイルの削除
- **オフライン再生**: ダウンロード済みエピソードはローカルファイルから再生（[player.md](player.md) 参照）
- **状態の永続化**: ダウンロード状態を DB に保存し、アプリ再起動後も保持

## UI（PodcastDetailScreen のエピソード行）

`DownloadButton` コンポーザブルが状態に応じて表示を切り替える。

| 状態 | 表示 | タップ時の動作 |
|---|---|---|
| 未ダウンロード | ダウンロードアイコン（`Icons.Default.Download`、primary 色） | ダウンロード開始 |
| ダウンロード中（進捗あり） | 確定的 `CircularProgressIndicator`（24dp） | なし |
| ダウンロード中（進捗不明） | 不確定 `CircularProgressIndicator`（24dp） | なし |
| ダウンロード済み | 削除アイコン（`Icons.Default.Delete`、error 色） | ダウンロード削除 |

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

```
PodcastDetailViewModel ──→ IDownloadRepository（interface, commonMain）
                                   ↑
                            DownloadRepository（expect/actual）
                                   │ 委譲
                            AudioDownloader（expect/actual）
                                   │
                            EpisodeDao.updateDownloadStatus()
```

| クラス | 役割 |
|---|---|
| `IDownloadRepository` | リポジトリのインターフェース。テスト時は `FakeDownloadRepository` に差し替え |
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
- 進捗: `Content-Length` が取得できた場合のみ `Downloading(progress)` を送出
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
- `PodcastDetailViewModelTest` でダウンロード開始・完了・失敗・削除の状態遷移を検証
