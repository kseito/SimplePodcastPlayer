# PlayerScreen 仕様

## 機能要件

- **音声再生**: エピソードのストリーミング再生
- **再生制御**: 再生・一時停止
- **スキップ**: 15秒前進・後退
- **プログレスバー**: 再生位置の表示と手動調整
- **バックグラウンド再生**: 画面を閉じても再生継続
- **再生位置の保存**: 再生位置を定期的にDBへ保存、次回起動時に再開
- **聴取状態の更新**: 95%以上再生で `listened = true` に更新

## 技術的アプローチ

- **共通**: ViewModel・状態管理・ビジネスロジックは `commonMain` で共通化
- **Android**: Jetpack Compose + ExoPlayer（Media3）
- **iOS**: SwiftUI/UIKit + AVPlayer（AVFoundation）

## 画面構成要素

- **ヘッダー**: 閉じるボタン（×または下向き矢印）、タイトル「Now Playing」
- **エピソード情報**
  - アートワーク（300dp × 300dp）: ポッドキャストカバー画像、なければデフォルトアイコン
  - エピソードタイトル: headlineMedium、最大2行
  - ポッドキャスト名: bodyLarge、最大1行、半透明
  - エピソード説明: bodyMedium、最大3行、半透明
- **再生コントロール**
  - プログレスバー: 現在位置（00:00形式）、総再生時間、スライダー
  - 15秒戻るボタン
  - 再生/一時停止ボタン（大きめ）
  - 15秒進むボタン
  - 再生速度ボタン（0.5x / 0.75x / 1.0x / 1.25x / 1.5x / 2.0x）（オプション）

## 画面遷移

| 操作 | 遷移先 |
|---|---|
| 閉じるボタン | 前画面（popBackStack）、再生は継続 |

**遷移元**: PodcastDetailScreen のエピソード再生ボタン（Episode + Podcast オブジェクトを渡す）

## プラットフォーム別実装

### Android

- UI: `commonMain/screen/PlayerScreen.kt`（Composable）
- ViewModel: `androidMain/screen/PlayerViewModelImpl.kt`（ExoPlayer と連携）
- サービス: `androidMain/service/PlaybackService.kt`（MediaSessionService を継承）
- バックグラウンド再生: Foreground Service + 通知領域での再生コントロール
- システム連携: MediaSession、ロック画面コントロール

### iOS

- UI: `iosApp/iosApp/Screen/PlayerView.swift`（SwiftUI）
- ViewModel: `iosMain/screen/PlayerViewModelImpl.ios.kt`（AVPlayer と連携）
- プレイヤー管理: `AudioPlayerManager`（Singleton）
- バックグラウンド再生: Background Modes（Audio）有効化
- システム連携: Now Playing Info Center、Remote Command Center、Control Center

## 状態管理

```kotlin
data class PlayerUiState(
    val episode: Episode? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isLoading: Boolean = false,
    val error: String? = null,
)
```

## データベース

**PlayHistory テーブル**

| カラム | 型 | 説明 |
|---|---|---|
| id | Long (PK) | 自動採番 |
| episodeId | String (FK) | エピソードID |
| playedAt | Long | 再生日時（UnixTime） |
| position | Long | 再生位置（ミリ秒） |
| completed | Boolean | 完了フラグ |

## エラーハンドリング

| エラー種別 | 表示内容 |
|---|---|
| ネットワークエラー | エラーメッセージ + リトライボタン |
| 再生エラー | エラーメッセージ + 前画面に戻るオプション |
| 無効なストリームURL | 「このエピソードは再生できません」メッセージ |
