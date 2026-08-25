# 変更履歴

このプロジェクトの重要な変更はこのファイルに記録します。

書式は [Keep a Changelog](https://keepachangelog.com/ja/1.1.0/) に基づきます。

## [Unreleased]

### Added

- Nicomangaへバージョン番号を固定せず適用できるよう、ReVanced Patcher v22対応のRVP基盤と全世代共通のApplication lifecycleフックを追加した。
- 既存アプリデータを消さず実機検証できるよう、既定無効の並行インストール用パッケージ名パッチを追加した。
- ログイン不要のList／Reading Historyを端末内へ保存するため、スキーマ版と破損時復旧を備えたIndexedDB WebView基盤を追加した。
- 日本語を含む主要11言語とアラビア語／ウルドゥー語のRTL表示に対応する翻訳基盤を追加した。
- RVPとManager用`patches.json`を同じリリースへ公開するGitHub Actionsワークフローを追加した。

### Security

- 広告SDKの通信と全画面広告生成を開始前に止めるため、AppLovin、Google Mobile Ads、Meta Audience Network、TradPlus、Vungle、Unity Ads等の初期化・読込・表示入口を無効化した。
- 広告SDKの自動起動と広告識別子利用を防ぐため、広告Activity／Provider／Service／Startup initializer／関連権限をManifestから削除した。
