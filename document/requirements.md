# SimplePodcastPlayer 要件定義書

## プロジェクト概要

SimplePodcastPlayerは、興味のあるポッドキャストを検索・購読・再生できるKotlin Multiplatformアプリケーションです。
Android・iOS両プラットフォームに対応し、Compose Multiplatformを使用して共通UIで開発します。

## 機能要件

### 1. ポッドキャスト検索・発見
- **キーワード検索**: ユーザーが入力したキーワードでポッドキャストを検索
- **検索結果表示**: ポッドキャスト名、説明、カバー画像を含む一覧表示
- **検索履歴**: 過去の検索キーワードの保存・表示（オプション）

### 2. 音声再生機能
- **バックグラウンド再生**: アプリがバックグラウンドにあっても音声再生を継続
- **スキップ機能**: 15秒前進・後退機能
- **フルスクリーンプレイヤー**: 専用のプレイヤー画面での再生制御
- **再生制御**: 再生・一時停止・停止
- **プログレスバー**: 再生位置の表示と手動調整

### 3. ライブラリ管理
- **お気に入り登録（購読）**: 気に入ったポッドキャストの購読機能
- **購読一覧**: 購読中のポッドキャスト一覧表示
- **再生履歴**: 過去に再生したエピソードの履歴管理
- **聴取状況管理**: エピソードの聴いた/聴いていない状態の管理
- **エピソード一覧**: 各ポッドキャストのエピソード一覧表示

### 4. 除外機能
- **ダウンロード機能**: 実装しない（ストリーミング再生のみ）

## 技術要件

### プラットフォーム対応
- **Android**: API Level 34以上 (Android 14以上)
  - 対象バージョン: Android 14, 15, 16
- **iOS**: iOS 16以上
  - 対象バージョン: iOS 16, 17, 18
- **共通UI**: Compose Multiplatform使用

### 外部API
- **Apple Podcasts API**: ポッドキャスト検索とメタデータ取得
  - 検索エンドポイント
  - ポッドキャスト詳細情報取得
  - エピソード情報取得

### データ保存
- **Room Database**: ローカルデータベース
  - 購読情報
  - 再生履歴
  - エピソード聴取状況
  - 検索履歴（オプション）

### 音声再生エンジン
- **Android**: ExoPlayer
  - MediaSession対応
  - 通知領域での再生制御
  - バックグラウンド再生
- **iOS**: AVPlayer
  - MediaPlayer Framework連携
  - Control Center対応
  - バックグラウンド再生

## アーキテクチャ設計

### 全体アーキテクチャ
- **MVVM パターン**: UI層とビジネスロジック層の分離
- **Repository パターン**: データアクセス層の抽象化
- **Dependency Injection**: 依存関係の管理

### モジュール構成
```
commonMain/
├── data/
│   ├── api/          # Apple Podcasts API
│   ├── database/     # Room Database
│   └── repository/   # Repository実装
├── domain/
│   ├── model/        # データモデル
│   ├── repository/   # Repository抽象化
│   └── usecase/      # ビジネスロジック
├── presentation/
│   ├── screen/       # 各画面
│   ├── viewmodel/    # ViewModel
│   └── component/    # 共通UIコンポーネント
└── util/             # ユーティリティ
```

## 画面設計

### 画面一覧
1. **PodcastListScreen** (既存拡張): お気に入り一覧
2. **PodcastSearchScreen** (既存拡張): ポッドキャスト検索
3. **PodcastDetailScreen** (新規): エピソード一覧・購読管理
4. **PlayerScreen** (新規): フルスクリーン音声プレイヤー
5. **HistoryScreen** (新規): 再生履歴表示

### PodcastDetailScreen 詳細仕様

#### 画面構成要素
- **ヘッダー部分**
  - 戻るボタン（←）
  - 画面タイトル「Podcast Detail」
- **ポッドキャスト情報部分**
  - サムネイル画像（200dp × 200dp）
    - `bestArtworkUrl()`を使用して最高品質の画像を表示
    - 画像がない場合は🎧アイコンを表示
  - タイトル（podcast.trackName）
    - Typography: headlineSmall
    - 最大2行、省略記号で切り詰め
  - 著者（podcast.artistName）
    - Typography: bodyLarge
    - 最大1行、省略記号で切り詰め
    - Color: onSurfaceVariant
  - 購読ボタン
    - 購読済みの場合：「購読中」（無効化状態）
    - 未購読の場合：「購読する」（アクティブ状態）
- **エピソード一覧部分**
  - セクションタイトル「Episodes」
  - エピソードリスト（LazyColumn）
    - エピソードタイトル
    - 公開日
    - 再生時間
    - 再生ボタン

#### 画面遷移
- **遷移元**: PodcastSearchScreen の PodcastItem をクリック
- **遷移パラメータ**: Podcast オブジェクト
- **遷移先**: 
  - 戻るボタン → PodcastSearchScreen
  - エピソード再生ボタン → PlayerScreen（将来実装）

#### 状態管理
- 購読状態の管理
- エピソード一覧の読み込み状態
- 購読処理の実行状態

### PlayerScreen 詳細仕様

#### 技術的アプローチ
PlayerScreenは、iOS/Androidそれぞれのネイティブコンポーネントを使用して実装します。
- **共通部分**: ViewModel、状態管理、ビジネスロジックは `commonMain` で共通化
- **UI層**: プラットフォーム固有のネイティブUIコンポーネントを使用
  - Android: Jetpack Compose + ExoPlayer
  - iOS: SwiftUI/UIKit + AVPlayer

#### 画面構成要素

##### 共通UI要素（両プラットフォーム共通）
- **ヘッダー部分**
  - 閉じるボタン（×または下向き矢印）
  - 画面タイトル「Now Playing」

- **エピソード情報部分**
  - エピソードアートワーク（300dp × 300dp）
    - ポッドキャストのカバー画像を表示
    - 画像がない場合はデフォルトアイコン表示
  - エピソードタイトル
    - Typography: headlineMedium（Android）/ Title（iOS）
    - 最大2行、省略記号で切り詰め
  - ポッドキャスト名
    - Typography: bodyLarge（Android）/ Headline（iOS）
    - 最大1行、省略記号で切り詰め
    - 半透明表示
  - エピソード説明
    - Typography: bodyMedium（Android）/ Body（iOS）
    - 最大3行、省略記号で切り詰め
    - 半透明表示
    - スクロール可能（オプション）

- **再生コントロール部分**
  - プログレスバー
    - 現在の再生位置表示（00:00形式）
    - トータル時間表示（00:00形式）
    - スライダーで再生位置の手動調整
  - メインコントロールボタン（中央に配置）
    - 15秒戻るボタン
    - 再生/一時停止ボタン（大きめ、アイコン切り替え）
    - 15秒進むボタン
  - 追加コントロール
    - 再生速度調整ボタン（0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x）（オプション）

#### プラットフォーム別実装

##### Android実装
- **使用コンポーネント**
  - Jetpack Compose: UI構築
  - ExoPlayer: 音声再生エンジン
  - Media3: MediaSession対応
  - Material3: デザインコンポーネント

- **画面実装場所**
  - `composeApp/src/androidMain/kotlin/.../screen/PlayerScreen.kt`
  - Composable関数として実装

- **プレイヤーサービス**
  - `PlaybackService` (Foreground Service)
  - MediaSessionServiceを継承
  - 通知領域での再生制御対応

- **プラットフォーム固有機能**
  - ロック画面での再生制御

##### iOS実装
- **使用コンポーネント**
  - SwiftUI: UI構築（推奨）
  - AVPlayer: 音声再生エンジン
  - AVFoundation: メディア管理
  - MediaPlayer Framework: Control Center連携

- **画面実装場所**
  - `iosApp/iosApp/Screen/PlayerView.swift`
  - SwiftUI Viewとして実装

- **プレイヤー管理**
  - `AudioPlayerManager` (Singleton)
  - AVPlayerを管理
  - バックグラウンド再生対応
  - Remote Command Center連携

- **プラットフォーム固有機能**
  - Control Centerでの再生制御
  - ロック画面での再生制御

#### 状態管理（commonMain）

##### PlayerViewModel
`composeApp/src/commonMain/kotlin/.../viewmodel/PlayerViewModel.kt`

```kotlin
data class PlayerUiState(
    val episode: Episode?,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isLoading: Boolean = false,
    val error: String? = null
)

interface PlayerViewModel {
    val uiState: StateFlow<PlayerUiState>

    fun play()
    fun pause()
    fun seekTo(position: Long)
    fun skipForward(seconds: Int = 15)
    fun skipBackward(seconds: Int = 15)
    fun setPlaybackSpeed(speed: Float)
    fun loadEpisode(episode: Episode)
    fun release()
}
```

##### プラットフォーム別実装
- **Android**: `androidMain` で `PlayerViewModel` を実装し、ExoPlayerと連携
- **iOS**: iosAppプロジェクトで `PlayerViewModel` プロトコルに準拠したクラスを実装し、AVPlayerと連携

#### 画面遷移
- **遷移元**:
  - PodcastDetailScreen のエピソード再生ボタン
  - ミニプレイヤー（他の画面の下部に表示、将来実装）
- **遷移パラメータ**: Episode オブジェクト
- **遷移先**:
  - 閉じるボタン → 前の画面に戻る（ナビゲーションスタックをpop）

#### 遷移仕様
- **PodcastDetailScreen → PlayerScreen**
  - エピソードアイテムの再生ボタンをタップ
  - Episode オブジェクトをパラメータとして渡す
  - モーダル遷移（フルスクリーン）
- **PlayerScreen → 前画面**
  - 閉じるボタンまたはスワイプダウンジェスチャー
  - 再生は継続（バックグラウンド再生）

#### バックグラウンド再生仕様

##### Android
- Foreground Serviceとして実行
- 通知領域に再生コントロール表示
- 再生中は通知を表示し続ける
- MediaSessionでシステムメディアコントロールに統合

##### iOS
- Background Modes（Audio）を有効化
- Now Playing Info Centerに再生情報を設定
- Remote Command Centerでシステムコントロールに対応
- Control Centerとロック画面で再生制御可能

#### エラーハンドリング
- ネットワークエラー時
  - エラーメッセージ表示
  - リトライボタン表示
- 再生エラー時
  - エラーメッセージ表示
  - 前画面に戻るオプション
- ストリームURLが無効な場合
  - 「このエピソードは再生できません」メッセージ表示

#### 再生位置の保存
- エピソード再生中の位置を定期的にデータベースに保存
- アプリ再起動時に前回の再生位置から再開可能
- 再生完了時（95%以上再生）は `listened = true` に更新

### ナビゲーション構成
```
PodcastListScreen ←→ PodcastSearchScreen
       ↓                    ↓
PodcastDetailScreen ←→ PlayerScreen
       ↓
HistoryScreen
```

#### 詳細画面への遷移仕様
- **PodcastSearchScreen → PodcastDetailScreen**
  - PodcastItem に onClick ハンドラを追加
  - Podcast オブジェクトをパラメータとして渡す
  - ナビゲーション定義に "detail/{podcastId}" ルートを追加
- **PodcastDetailScreen → PodcastSearchScreen**
  - 戻るボタンまたはシステムバックボタンで前画面に戻る
  - ナビゲーションスタックから PodcastDetailScreen を削除

## データベース設計

### テーブル構成
```sql
-- 購読ポッドキャスト
Podcast {
  id: String (Primary Key)
  name: String
  description: String
  imageUrl: String
  author: String
  subscribed: Boolean
  subscribedAt: DateTime
}

-- エピソード
Episode {
  id: String (Primary Key)
  podcastId: String (Foreign Key)
  title: String
  description: String
  audioUrl: String
  duration: Long
  publishedAt: DateTime
  listened: Boolean
}

-- エピソード表示用モデル（PodcastDetailScreen用）
EpisodeDisplayModel {
  id: String
  title: String
  description: String
  publishedAt: String (フォーマット済み日付)
  duration: String (フォーマット済み時間 "12:34")
  audioUrl: String
  listened: Boolean
}

-- 再生履歴
PlayHistory {
  id: Long (Primary Key)
  episodeId: String (Foreign Key)
  playedAt: DateTime
  position: Long
  completed: Boolean
}
```

## 開発優先順位

### Phase 1: 基本検索・表示機能
1. Apple Podcasts API連携
2. 検索画面の実装
3. 検索結果表示

### Phase 2: データベース連携
1. Room Database設定
2. 購読機能実装
3. お気に入り一覧表示

### Phase 3: 音声再生機能
1. 基本的な音声再生
2. プレイヤー画面実装
3. バックグラウンド再生

### Phase 4: 履歴・詳細機能
1. 再生履歴機能
2. エピソード詳細画面
3. 聴取状況管理

## 注意事項

### Apple Podcasts API
- Apple Developer Program登録が必要
- API制限に注意（1日あたりのリクエスト数）
- 利用規約の遵守

### パフォーマンス考慮事項
- 画像の遅延読み込み
- エピソードリストのページング
- 検索結果のキャッシュ

### セキュリティ
- APIキーの安全な管理
- ユーザーデータの暗号化
- 通信のHTTPS化

---

**作成日**: 2025-08-03
**最終更新**: 2025-10-29
**バージョン**: 1.2

## 更新履歴
- **v1.2 (2025-10-29)**: PlayerScreen の詳細仕様を追加
  - 技術的アプローチの定義（iOS/Androidネイティブコンポーネント使用）
  - 画面構成要素の定義（共通UI要素）
  - プラットフォーム別実装仕様（Android: Compose + ExoPlayer, iOS: SwiftUI + AVPlayer）
  - 状態管理の定義（commonMainでのViewModel）
  - 画面遷移仕様の追加
  - バックグラウンド再生仕様の定義
  - エラーハンドリングの定義
  - 再生位置保存仕様の追加
- **v1.1 (2025-08-16)**: PodcastDetailScreen の詳細仕様を追加
  - 画面構成要素の定義
  - 画面遷移仕様の追加
  - エピソード表示用データモデルの定義
- **v1.0 (2025-08-03)**: 初版作成
