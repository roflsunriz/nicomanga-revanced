# 更新手順

## 前提

- `COMMON-AGENTS.md` とリポジトリ固有の `AGENTS.md` を全文確認する。
- `git status --short --branch` で既存差分を確認する。
- 更新対象XAPKは `nicomanga-apks/` へ置き、Gitへ追加しない。
- JDK、Android SDK、GitHub CLI、ReVanced CLI v6以降を用意する。

## 更新

1. XAPKの `manifest.json` からpackage、versionCode、versionName、split一覧を確認する。
2. ベースAPKの `com.lovehug.MainApplication.onCreate()` が存在することを確認する。
3. 広告SDKの追加・削除をManifestとDEX名前空間から確認し、`adClassPrefixes`／`adManifestPrefixes`を更新する。
4. `CHANGELOG.md` の `[Unreleased]` を意図ベースで更新する。
5. 次を実行してRVPをビルドする。

```powershell
.\scripts\build.ps1
```

6. ReVanced CLIで保存済みの全世代ベースAPKへ適用し、各ログに `"Nicomanga ReVanced" succeeded` があることを確認する。
7. 最新XAPKをAnti Split MまたはAPKEditorで単一APK化し、RVPを適用する。
8. 実機では最初に `検証用パッケージ名を使用` を有効化し、公式版を消さずに起動・画面・ログを確認する。
9. ReVanced ManagerではRVP単体ではなく、リリースの `patches.json` URLを登録し、「ストレージから選択」で単一APKを入力する。

## 検証

- Gradleビルドが警告なしで成功する。
- RVPが7世代すべてへエラーなしに適用される。
- Manifestに広告SDKコンポーネント、広告識別子権限、空のquery intentが残らない。
- `apksigner verify --verbose` がv2またはv3署名を検証する。
- 実機LogcatにFATAL EXCEPTIONと広告SDK初期化ログがない。
- 端末側の一時キャプチャと検証用パッケージを検証後に削除する。

## ロールバック

- コードは直前の日本語Conventional Commit単位でrevertする。
- 実機は検証用別パッケージだけをアンインストールし、公式 `com.lovehug` とそのデータには触れない。
- リリースに問題がある場合は削除やタグ付け替えをせず、修正版を新しいバージョンで公開する。
