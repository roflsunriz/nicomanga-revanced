# 実機検証記録

## 2026-08-25 AQUOS R8 pro

- 端末: SH-R80P（AQUOS R8 pro）
- OS: Android 16 / API 36
- ABI: arm64-v8a
- 対象: Nicomanga 5.0.0（versionCode 532）
- 方法: 元XAPKをAPKEditor 1.4.9で単一APK化し、ReVanced CLI 6.0.0でRVPを適用した。公式版を保持するため検証用別パッケージとしてインストールした。
- 検証用別パッケージだけは内部DB・スキーマを採取できるようdebuggableにし、通常パッチではdebuggable属性を変更しない。

### 成功

- RVPの読込、リソース処理、12個のDEX再構築、align、署名が成功した。
- 最終RVPを1.0.30、1.0.82、2.0.3、2.0.7、3.0.3、4.0.1、5.0.0へ新規に適用し、7世代すべて成功した。
- v2／v3 APK署名を検証した。
- 公式 `com.lovehug` を削除せず検証APKを並行インストールできた。
- 新アーキテクチャを維持した安定版で起動し、FATAL EXCEPTIONがないことを確認した。
- AppLovin、TradPlus、Unity Ads、Google Mobile Ads、Audience Network、Vungleの初期化ログが出ないことを確認した。
- TradPlus `initSdk`／`loadAd`／`showAd`、AppLovin全画面広告、Google Mobile Ads初期化が空メソッドになったことを逆コンパイルで確認した。
- Fabric画面でログイン不要の4等分タブ（Home／List／読書履歴／設定）が表示され、Listと読書履歴がIndexedDBへ永続化されることを確認した。
- 詳細画面の「ビュー」直下に専用スペースを確保して「リストに追加」を表示し、「ビュー」および「概要」と重ならないことを確認した。
- 読者で章1のページ10／49を保存し、読書履歴に進捗バーと0%（完読章0／全章）が表示されることを確認した。
- 読書履歴からマンガIDで詳細を先読みし、章ストア初期化後にReaderへ遷移してCH 1とページ10相当へ復帰することを確認した。
- 「現在開発中です」は既定でセクション全体と余白が消え、設定で再表示できることを確認した。
- ログイン利用へ切り替えると代替List／読書履歴／List追加ボタンが消え、元アプリのタブだけになることを確認した。
- v0.1.0の公開`patches.json`をReVanced Manager 2.6.0へURL追加し、「Nicomanga ReVanced v0.1.0・2個のパッチ」として認識されることを確認した。
- ManagerでNicomanga 5.0.0の単一APKと既定の1パッチを選択し、12 DEX再構築、リソースコンパイル、align、署名、`result.apk`生成が成功した。
- Manager検証後、端末の入力APK、検証用別パッケージ、スクリーンショットを削除し、公式`com.lovehug`が残っていることを確認した。
- ログイン不要モードの設定タブから `nicomanga://setting` へ遷移し、HomeではなくNicomanga本来の設定画面が開くことを確認した。
- 本来の設定画面末尾に同じカード・区切り・行構成の「Nicomanga ReVanced」が表示され、そこからダイアログではなく同系統の全画面設定を開閉できることを確認した。
- 全画面設定でログイン不要／ログイン利用を切り替え、代替4タブと元アプリの3タブが即時に切り替わることを確認した。
- 「現在開発中です」の表示時は縦スクロールでき、非表示時はカードが消えて空白領域へ縦スクロールできないことをUI階層の座標比較で確認した。
- 非表示時もHomeの横方向の漫画棚を操作でき、再起動後に設定値、Home内容、棚の表示が復元されることを確認した。
- v0.1.1 RVP（SHA-256 `8887a7b261dbf5a9c26f37b7b3d0808d9ecf1d62cd7ba787d64a7b0dedc0c94c`）を7世代へ新規適用し、全件成功した。v5.0.0成果物はv2／v3署名を検証した。
- OSV-Scanner 2.4.0で2個のGradle lockfile、計34パッケージを検査し、既知脆弱性がないことを確認した。

### 検出して修正した問題

- Manifestのnamespace非対応DOMで広告宣言が残ったため、`android:name`通常属性へのフォールバックを追加した。
- 広告query actionだけを削除して空intentが残ったため、親intentごと削除するよう修正した。
- Unity Adsの名前空間が `com.unity3d.services` だったため、ManifestとDEX双方の遮断対象へ追加した。
- 旧世代MainActivityにonCreate実装がなかったため、全世代共通のMainApplication lifecycleフックへ変更した。
- React Native 5.0.0は起動直後にはFabric Surfaceしか持たず、UIを無効化していた。Surface生成後の実View、OkHttp応答、Fabricイベントを組み合わせる遅延統合へ変更した。
- TradPlus初期化遮断後に残存イベント保存がnull参照を起こしたため、広告SDK内のRunnableと保存・送信・追跡処理も無処理化した。
- OkHttpの正常なキャンセル例外が反射呼出しでラップされていたため、元の`IOException`をそのまま伝播するよう修正した。
- Readerのドット列はFabric仮想ノードだったため、章APIの`content`数とFabricタッチ座標から現在ページを算出するよう変更した。
- 履歴復帰のタイトル検索が長い題名で不安定だったため、Hermes解析で確認した`mangaId`／`chapter`を使う二段階内部ルートへ変更した。
- Fabricでは合成したタッチイベントがタブ選択として処理されず、設定タブがHomeのままになったため、Nicomangaの内部URIへ明示的に遷移するよう変更した。
- 開発中カードの子Viewを非表示にしてもFabric親コンテナのスクロール範囲が残ったため、親レイアウトを縮め、非表示中の縦スクロールを先頭へ固定した。
