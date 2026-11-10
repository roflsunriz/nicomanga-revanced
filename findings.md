# 調査知見

- Manager登録用のraw URLは既定ブランチ直下の `patches.json` を読む。release workflowの最後で、最新リリースのJSONだけを同ブランチへ同期する。旧リリース再実行は更新対象外で、JSONの版と現在のblob SHAを検証するため、URLだけを変更してJSON配置・同期を省略しない。

- 保存済み入力はAPKではなくXAPK。複数世代のbaseへCLI適用を確認し、最新実機用はAnti Split MまたはAPKEditorで単一APK化してからpatchする。
- RVPは `buildAndroid` を通してAndroid `classes.dex` を含める。通常build成功だけではManager読込可能を意味しない。
- CLI exit codeや出力APKだけで成功扱いせず、全世代logの `"Nicomanga ReVanced" succeeded`、RVP内DEX、manifest、署名を確認する。
- 実機検証は別package IDを使って公式版とdataを残す。公式packageを消さず、検証後は一時captureと検証packageだけを削除する。
- 広告SDK namespace/manifestは新版ごとに再調査し、空欄、rotation途中、scroll範囲、ログイン有無の両modeを画面で確認する。
