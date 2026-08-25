# 実機検証記録

## 2026-08-25 AQUOS R8 pro

- 端末: SH-R80P（AQUOS R8 pro）
- OS: Android 16 / API 36
- ABI: arm64-v8a
- 対象: Nicomanga 5.0.0（versionCode 532）
- 方法: 元XAPKをAPKEditor 1.4.9で単一APK化し、ReVanced CLI 6.0.0でRVPを適用した。公式版を保持するため検証用別パッケージとしてインストールした。

### 成功

- RVPの読込、リソース処理、12個のDEX再構築、align、署名が成功した。
- v2／v3 APK署名を検証した。
- 公式 `com.lovehug` を削除せず検証APKを並行インストールできた。
- 新アーキテクチャを維持した安定版で起動し、FATAL EXCEPTIONがないことを確認した。
- AppLovin、TradPlus、Unity Ads、Google Mobile Ads、Audience Network、Vungleの初期化ログが出ないことを確認した。
- TradPlus `initSdk`／`loadAd`／`showAd`、AppLovin全画面広告、Google Mobile Ads初期化が空メソッドになったことを逆コンパイルで確認した。

### 検出して修正した問題

- Manifestのnamespace非対応DOMで広告宣言が残ったため、`android:name`通常属性へのフォールバックを追加した。
- 広告query actionだけを削除して空intentが残ったため、親intentごと削除するよう修正した。
- Unity Adsの名前空間が `com.unity3d.services` だったため、ManifestとDEX双方の遮断対象へ追加した。
- 旧世代MainActivityにonCreate実装がなかったため、全世代共通のMainApplication lifecycleフックへ変更した。
- React Native 5.0.0はFabric Surfaceへ直接描画し、旧View階層統合を利用できなかった。Reanimated 4が新アーキテクチャ専用のためPaper強制は棄却し、Fabricでは未接続UI基盤を起動しないようにした。

### 未完了

- Fabric 5.0.0のログイン不要List／Reading History／設定UIは未接続。
- ReVanced Managerからの実適用は、最初のリリース公開後に検証する。
