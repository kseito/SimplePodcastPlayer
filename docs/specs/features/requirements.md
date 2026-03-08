# SimplePodcastPlayer 要件定義

## プロジェクト概要

SimplePodcastPlayerは、興味のあるポッドキャストを検索・購読・再生できる Kotlin Multiplatform アプリケーション。
Android・iOS 両プラットフォームに対応し、Compose Multiplatform を使用して共通 UI で開発する。

## 機能仕様

各画面の詳細仕様は以下を参照。

| ドキュメント | 内容 |
|---|---|
| [podcast-list.md](podcast-list.md) | 購読一覧画面（PodcastListScreen） |
| [podcast-search.md](podcast-search.md) | ポッドキャスト検索画面（PodcastSearchScreen） |
| [podcast-detail.md](podcast-detail.md) | ポッドキャスト詳細・エピソード一覧画面（PodcastDetailScreen） |
| [player.md](player.md) | 音声プレイヤー画面（PlayerScreen） |

## 対応プラットフォーム

- **Android**: API Level 34 以上（Android 14, 15, 16）
- **iOS**: iOS 16 以上（iOS 16, 17, 18）
- **共通 UI**: Compose Multiplatform

## ナビゲーション構成

```
PodcastListScreen ←→ PodcastSearchScreen
       ↓                    ↓
 (list_detail)           (detail)
       └──── PodcastDetailScreen ────┘
                    ↓
              PlayerScreen
```

## 注意事項

- Apple Podcasts API の利用規約を遵守すること
- API リクエスト数の上限に注意すること
- 通信は HTTPS のみ使用すること

---

**作成日**: 2025-08-03 / **最終更新**: 2025-10-29 / **バージョン**: 1.2
