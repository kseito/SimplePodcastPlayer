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
| ダウンロード済み | 削除アイコン（`Icons.Default.Delete`、error 色） | 音声ファイル削除 |

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
| 件数の取得に失敗 | 購読解除を中止し `error = "Failed to unsubscribe"` をセット。**失敗を 0 件として扱わない**（確認なしの削除になるため） |

- 確認前は購読状態・ファイルとも一切変更しない（`PodcastDetailUiState.unsubscribeConfirmAudioFileCount` が null 以外の間はダイアログ表示中）
- 削除するのはファイルとダウンロード状態のみ。**エピソードのレコードは残す**ため、再購読時に再生位置と聴取済みフラグが復活する
- 購読解除済みポッドキャストのエピソードは `InProgressEpisodesScreen` に表示しない（`subscribed` で除外）

### 2. 聴き終わったエピソードの一括削除（PodcastListScreen）

ヘッダーのゴミ箱アイコン（`Icons.Default.Delete`）から、聴取済み（`listened = true`）かつダウンロード済みのエピソードを購読横断でまとめて削除する。

| 状況 | ダイアログ内容 | ボタン |
|---|---|---|
| 対象が 1 件以上 | 「Delete the downloaded audio of N listened episode(s)?」 | [Delete] / [Cancel] |
| 対象が 0 件 | 「There are no listened downloads to delete.」 | [OK] のみ |
| 件数の取得に失敗 | ダイアログを出さず Snackbar「Failed to check downloads」 | － |

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
PodcastDetailViewModel ──┐
PodcastListViewModel  ───┴─→ IEpisodeAudioRepository（interface, commonMain）
BasePlayerViewModel   ──────→          ↑
                             EpisodeAudioRepository（commonMain の通常クラス）
                                       │ 削除対象の抽出・DB 更新はここに集約
                                       ├─→ EpisodeDao
                                       └─→ IAudioDownloader（プラットフォーム実装）
```

| クラス | 役割 |
|---|---|
| `IEpisodeAudioRepository` | エピソード音声ファイルの取得・参照・削除（単体 / 一括）のインターフェース |
| `EpisodeAudioRepository` | 実体。ファイル操作を `IAudioDownloader` に委譲し、`EpisodeDao` のダウンロード列と同期させる。プラットフォーム差分を持たないため commonMain の通常クラス |
| `EpisodeAudioRepositoryBuilder` | `expect object`。プラットフォームごとの `AudioDownloader` と `EpisodeDao` を組み立てて生成（Android は `Context` が必要） |
| `IAudioDownloader` | HTTP ダウンロードとファイル操作のインターフェース。テスト時は `FakeAudioDownloader` に差し替え |
| `AudioDownloader` | `IAudioDownloader` の Android / iOS 実装 |

名前について: `Download` を名詞として使うと「ダウンロード処理」と「保存済みの音声ファイル」のどちらを指すか曖昧になるため、
リポジトリ層は `AudioFile` を用いる。動詞としての `downloadEpisode` / `DownloadState` と、
DB カラムの `isDownloaded` / `downloadedAt` / `localFilePath` はそのまま。

### IAudioDownloader API

```kotlin
interface IAudioDownloader {
    suspend fun downloadAudio(url: String, episodeId: String): Flow<DownloadState>
    fun getAudioFilePath(episodeId: String): String?
    suspend fun deleteAudioFile(episodeId: String): Boolean
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

`EpisodeAudioRepository` が `DownloadState.Completed` を検知した時点で `EpisodeDao.updateDownloadStatus()` を呼び、Episode テーブルを更新する。

| カラム | ダウンロード完了時 | 削除時 |
|---|---|---|
| isDownloaded | true | false |
| localFilePath | ローカルファイルパス | null |
| downloadedAt | 現在時刻（UnixTime ミリ秒） | 0 |

## オフライン再生との連携

`BasePlayerViewModel` は再生開始時に `episodeAudioRepository.getAudioFilePath(episode.id)` を確認し、
ローカルファイルが存在すればそのパスを、なければ `episode.audioUrl`（ストリーミング）を再生する。

## エラー処理

- ネットワークエラー・ファイル書き込みエラーは `DownloadState.Failed(error)` として通知
- iOS はファイルオープン・書き込み失敗時に `DownloadDataCreationException` を送出し `Failed` に変換
- 削除失敗時は `PodcastDetailUiState.error = "Failed to delete download"` をセット

## テスト

- `FakeAudioDownloader`（commonTest の `fake` パッケージ）で `IAudioDownloader` のみを差し替え、
  リポジトリは本物の `EpisodeAudioRepository` を使う。DB への書き戻しを Fake 側で再現する必要がない
- `EpisodeAudioRepositoryTest` で DB 同期（ダウンロード完了 / 削除）と削除対象の抽出条件（ポッドキャスト単位 / 聴取済み）を検証
- `PodcastDetailViewModelTest` でダウンロード開始・完了・失敗・削除の状態遷移と、購読解除の確認 / 実行 / キャンセル / 件数取得失敗を検証
- `PodcastListViewModelTest` で聴取済み一括削除の件数集計・削除・キャンセル・件数取得失敗を検証
- 件数取得の失敗は `FakeEpisodeDao.setDownloadedEpisodeQueryError()` で再現する
