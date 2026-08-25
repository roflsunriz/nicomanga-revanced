# 更新手順

## 前提

- `COMMON-AGENTS.md` とリポジトリ固有の `AGENTS.md` を全文確認する。
- `git status --short --branch` で既存差分を確認する。
- 更新対象XAPKは `nicomanga-apks/` へ置き、Gitへ追加しない。
- JDK、Android SDK、GitHub CLI、ReVanced CLI v6以降を用意する。
- GitHub Actionsをセルフホストrunnerで再現する場合、Node.js 24 Actionに対応するActions Runner v2.327.1以降を使用する。リポジトリ標準の`ubuntu-latest`はGitHub管理runnerを使用する。

## 更新

1. XAPKの `manifest.json` からpackage、versionCode、versionName、split一覧を確認する。
2. ベースAPKの `com.lovehug.MainApplication.onCreate()` が存在することを確認する。
3. 広告SDKの追加・削除をManifestとDEX名前空間から確認し、`adClassPrefixes`／`adManifestPrefixes`を更新する。
4. `CHANGELOG.md` の `[Unreleased]` を意図ベースで更新する。
   リリース時は対象バージョン節へ日付付きで移し、`scripts/new-release-notes.ps1`でその節を抽出できることを確認する。
5. 次を実行してRVPをビルドする。

```powershell
.\scripts\build.ps1
```

依存関係を変更した場合は `--write-locks` でlockfileを更新し、OSV-Scannerで `patches/gradle.lockfile` と `extensions/extension/gradle.lockfile` を検査する。

6. ReVanced CLIで保存済みの全世代ベースAPKへ適用し、各ログに `"Nicomanga ReVanced" succeeded` があることを確認する。
7. 最新XAPKをAnti Split MまたはAPKEditorで単一APK化し、RVPを適用する。
8. 実機では最初に `検証用パッケージ名を使用` を有効化し、公式版を消さずに起動・画面・ログを確認する。
9. ReVanced ManagerではRVP単体ではなく、リリースの `patches.json` URLを登録し、「ストレージから選択」で単一APKを入力する。
10. Release本文はタグと同じバージョンのCHANGELOG節だけになっていることを確認する。

## 検証

- Gradleビルドが警告なしで成功する。
- Build／Releaseの`actions/checkout@v7`がGitHub管理runnerで成功し、Releaseの`actions/attest-build-provenance@v4`に必要な`id-token: write`／`attestations: write`権限とRVPを指す`subject-path`が維持されている。
- RVPが7世代すべてへエラーなしに適用される。
- Manifestに広告SDKコンポーネント、広告識別子権限、空のquery intentが残らない。
- `apksigner verify --verbose` がv2またはv3署名を検証する。
- 実機LogcatにFATAL EXCEPTIONと広告SDK初期化ログがない。
- ログイン不要モードのHome／List／読書履歴／設定がそれぞれ正しい画面へ遷移する。
- ログイン不要・ログイン利用の両方で共通Homeが表示され、HOME／NEW／TOP／UPDATEの各グリッド、漫画詳細、検索が操作できる。
- 縦画面では2列以上、横画面では画面幅に応じた列数へ変わり、本棚下に黒い空欄が残らない。
- Nicomanga設定内の統合カード、全画面ReVanced設定、戻る操作、モード切替が元アプリのUXから逸脱していない。
- 開発中セクションの表示時だけ縦スクロール領域が増え、非表示時は空白へスクロールできず、縦操作を先頭へ戻す不自然な動きもない。Homeの横棚は両状態で操作できる。
- 開発中セクションを非表示にしたまま縦横へ回転し、切替途中を含む全表示フレームでセクションが描画されない。
- 端末側の一時キャプチャと検証用パッケージを検証後に削除する。

## ロールバック

- コードは直前の日本語Conventional Commit単位でrevertする。
- 実機は検証用別パッケージだけをアンインストールし、公式 `com.lovehug` とそのデータには触れない。
- リリースに問題がある場合は削除やタグ付け替えをせず、修正版を新しいバージョンで公開する。
