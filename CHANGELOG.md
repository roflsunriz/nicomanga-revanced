# 変更履歴

このプロジェクトの重要な変更はこのファイルに記録します。

書式は [Keep a Changelog](https://keepachangelog.com/ja/1.1.0/) に基づきます。

## [Unreleased]

### Changed

- GitHub ActionsのNode.js 24移行とビルド来歴証明の互換性を維持するため、Build／Releaseの`actions/checkout`をv7、Releaseの`actions/attest-build-provenance`をv4へ更新した。
- Release本文と変更履歴の不一致を防ぐため、タグと同じバージョンのCHANGELOG節だけを公開する生成処理へ統一した。

## [0.1.3] - 2026-08-25

### Added

- Home APIが返すNEW／TOP／UPDATE各25件を正規化・キャッシュし、表紙、章、タイトル、著者、ジャンルを表示するReVanced Homeを追加した。

### Changed

- 本棚下の黒い空白をなくすため、ログイン不要・ログイン利用の両モードでHomeを画面幅に応じて列数が変わるスクロール可能なグリッドへ統一した。
- Homeの漫画カードと検索をNicomangaの内部IDルートおよび既存検索導線へ接続し、ログイン状態にかかわらず同じ操作体験にした。
- 「現在開発中です」の表示設定を、Home全体の固定高を増やさないコンパクトな案内カードとして共通Homeへ統合した。

### Fixed

- 横画面でネイティブタブ検出が不安定でも、ログイン不要モードの4タブがHome上に維持されるよう表示条件を修正した。

## [0.1.2] - 2026-08-25

### Fixed

- 開発中セクションを非表示にしてもFabricのスクロール領域が残る問題を、描画前にセクションとScrollViewを漫画棚の末尾まで縮める方式へ変更して修正した。
- 非表示中の縦操作を常に先頭へ戻していたスクロール監視を廃止し、通常のタッチ操作が不自然に引き戻される問題を修正した。
- Activity生成直後から初回描画前に非表示を適用し、自動回転や再レイアウト時に開発中セクションが一瞬表示される問題を修正した。

## [0.1.1] - 2026-08-25

### Changed

- 浮遊ボタンとAndroid標準ダイアログだったNicomanga ReVanced設定を、Nicomangaの設定カードと同じ外観の入口およびアプリ内全画面設定へ変更した。

### Fixed

- ログイン不要モードの設定タブがFabric画面で反応せず、Homeのままになる問題を内部設定ルートへの遷移に変更して修正した。
- Nicomanga 5.0.0の設定画面で見出しとタブ名がHomeになる表示を、設定として表示するよう修正した。
- 「現在開発中です」を非表示にしてもReact Nativeのスクロール領域が残り、空白へ縦スクロールできる問題を修正した。

## [0.1.0] - 2026-08-25

### Added

- Nicomangaへバージョン番号を固定せず適用できるよう、ReVanced Patcher v22対応のRVP基盤と全世代共通のApplication lifecycleフックを追加した。
- 既存アプリデータを消さず実機検証できるよう、既定無効の並行インストール用パッケージ名パッチを追加した。
- ログイン不要のList／Reading Historyを端末内へ保存するため、スキーマ版と破損時復旧を備えたIndexedDB WebView基盤を追加した。
- 日本語を含む主要11言語とアラビア語／ウルドゥー語のRTL表示に対応する翻訳基盤を追加した。
- RVPとManager用`patches.json`を同じリリースへ公開するGitHub Actionsワークフローを追加した。
- React Native Fabric世代へ、ログイン不要の4等分タブ、IndexedDB List／Reading History、マンガIDベースの章・ページ復帰を追加した。
- 詳細画面の「ビュー」直下へ専用余白付きの「リストに追加」ボタンを追加した。
- Homeの「現在開発中です」を余白ごと既定非表示にし、Nicomanga ReVanced設定から再表示できるようにした。

### Fixed

- Fabric生成前のView判定によりv5.0.0で拡張UIが無効になる問題を修正した。
- 広告初期化遮断後のTradPlus残存イベントと、OkHttpキャンセル例外の反射ラップによるクラッシュを修正した。
- 長いタイトルの検索に依存した履歴復帰を、`mangaId`／`chapter`内部ルートへ変更した。

### Security

- ビルド・パッチ処理で既知脆弱性を含む旧依存候補を使わないよう、ReVanced Patcher 22.0.1、ReVanced Gradle plugin 1.0.0-dev.11、smali 3.0.9へ更新した。
- 広告SDKの通信と全画面広告生成を開始前に止めるため、AppLovin、Google Mobile Ads、Meta Audience Network、TradPlus、Vungle、Unity Ads等の初期化・読込・表示入口を無効化した。
- 広告SDKの自動起動と広告識別子利用を防ぐため、広告Activity／Provider／Service／Startup initializer／関連権限をManifestから削除した。
