# AGENTS.md

## 作業開始前の必須手順（最優先・例外なし）

1. エージェントは、調査、計画、コマンド実行、スキル利用、ファイル編集、コミット、プッシュを始める前に、必ずリポジトリ直下の `.\COMMON-AGENTS.md` を開き、先頭から末尾まで全文を読む。
2. `COMMON-AGENTS.md` はGit管理外のシンボリックリンクである。`git`や既定のignore設定が有効な`rg --files`の検索結果だけで、ファイルが存在しないと判断してはならない。PowerShellでは最初に次を実行する。

```powershell
Get-Content -Raw -LiteralPath .\COMMON-AGENTS.md
```

3. 読み取りに失敗した場合、出力が省略された場合、または末尾まで読めたことを確認できない場合は、一切の作業を開始せず、パスとシンボリックリンク先を確認して全文を再取得する。必要なら分割して末尾まで読む。
4. 全文を読了するまで、ローカル `AGENTS.md` だけを根拠に作業を続けてはならない。読了後は `COMMON-AGENTS.md` を最優先の指針とし、読了直後の最初の進捗報告で全文を読了したことを明示する。
   このファイルでは `nicomanga-revanced` 固有の補足だけを記載する。

## 目的
- Nicomanga(ID:com.lovehug)アプリ用のReVanced patchを作る
- ログインを必須ではなく、バイパス（ログイン不要）と選べるようにする
- バイパス時はアカウント必須の機能をタブからアイコンごと削除する、削除時は自動ストレッチして良い感じに幅を取るようにデザインする
- バイパス時は代替としてNicomanga ReVanced側で全チャプターの内の読了済みのチャプターのパーセンテージとプログレスバーを表示するReading Historyのタブを追加しIndexedDBに保存する
- バイパス時は代替としてNicomanga ReVanced側でListのタブを追加しIndexedDBに保存する。Listに追加したマンガを一覧表示する。
- バイパス時にマンガのチャプターリストの「ビュー」下部にList追加ボタンを設置する
- Nicomanga ReVanced Reading Historyで、マンガを押すとそのチャプターの読んだページ数まで自動で飛ぶようにする
- ログイン時にはバイパス時用代替機能(List, Reading History, Listボタン)は表示しない
- 広告SDKの通信を全遮断する
- 挿入される広告の高さと幅をゼロにし、広告の空欄を作らない
- 全画面を覆う広告は作成前から遮断し表示させない
- Home タブの「現在開発中です」セクションをデフォルトで非表示
- 設定タブに新設定「Nicomanga ReVanced」を作り「現在開発中です」の表示を切替可能にする

## 展開
- リリースワークフローを整備してリリースする
- リリースにはpatches.jsonを含ませる
- 実機で検証する
- 検証結果と対策を記録する
- Nicomangaアプリのバージョンに依存せず適用できることを確認する
- ReVanced Managerで実際に適用できることを確認する
- 実機検証では中間生成物のゴミを残さない

## Environment
- `nicomanga-apks`にあるファイルはapkではなくxapkである
- `nicomanga-apks`にあるxapkで複数世代の適用確認ができる
- XAPKはAnti Split Mで単一APK化できる(Anti Split MはAndroidアプリ)