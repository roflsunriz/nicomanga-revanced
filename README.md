# Nicomanga ReVanced

Nicomanga（`com.lovehug`）向けのReVanced Patchesです。Patcher v22形式のRVPとしてビルドし、Nicomangaの特定バージョン番号を固定せず適用します。

## 実装状況

| 項目 | 状態 |
|---|---|
| ReVanced Patcher v22対応RVP | 実装・ビルド確認済み |
| 1.0.30〜5.0.0の7世代への適用 | CLI適用確認済み |
| AppLovin、Google Mobile Ads、Meta Audience Network、TradPlus、Vungle、Unity Ads等の自動初期化・読込・表示遮断 | 実装・5.0.0実機確認済み |
| 広告Activity／Provider／Service／広告識別子権限のManifest除去 | 実装・確認済み |
| ログイン不要モード、List、Reading History、IndexedDB | 実装・5.0.0 Fabric実機確認済み |
| Reading Historyから章・ページへ復帰 | 実装・5.0.0で章1／ページ10への復帰確認済み |
| ログイン状態共通のレスポンシブHomeグリッド | 実装・5.0.0 Fabric実機確認済み |
| Nicomanga設定へ統合したReVanced設定画面 | 実装・5.0.0 Fabric実機確認済み |
| 「現在開発中です」の描画前非表示、スクロール領域除去、設定切替 | 実装・5.0.0 Fabric実機確認済み |
| ReVanced ManagerでのURL取込と実適用 | v0.1.3公開・v0.1.0をManager 2.6.0で実機適用確認済み |

## ビルド

前提:

- JDK 17以上
- Android SDK
- GitHub CLIで認証済みであること

PowerShellから次を実行します。

```powershell
.\scripts\build.ps1
```

成果物は `patches/build/libs/patches-<version>.rvp` に生成されます。認証トークンはファイルへ保存せず、ビルドプロセスの環境変数だけへ渡します。

## 適用

ReVanced Manager／CLIが扱えるのは単一APKです。XAPKをそのまま入力しないでください。

1. Anti Split MまたはAPKEditorでXAPKを単一APK化します。
2. ReVanced Managerでは「ストレージから選択」で単一APKを指定します。
3. `Nicomanga ReVanced` パッチを選択して適用します。

`検証用パッケージ名を使用` は既定で無効です。既存のNicomangaを残したまま並行インストールする開発・検証時だけ有効にしてください。

リリース後、Managerへ追加するJSONは次の固定URLです。

```text
https://github.com/roflsunriz/nicomanga-revanced/releases/latest/download/patches.json
```

## セキュリティ

パッチは広告SDKのManifest自動初期化コンポーネントを削除し、既知広告SDK名前空間内の初期化・読込・表示メソッドを実行前に終了させます。広告ビューが作られた旧世代では、拡張側でも該当ビューを `GONE` かつ0×0へ縮小します。

NicomangaのAPK／XAPK、逆コンパイル結果、署名鍵は配布物へ含めません。

## ライセンス

[GNU General Public License v3.0](LICENSE)
