# VolNotch — Fire Max 11 メディア音量微調整アプリ 実装計画

## Context（なぜ作るか）
Amazon Fire Max 11 (第3世代 / Fire OS 8.3.3.8 = Android 11 / API 30) では、ハードウェア音量ボタンでメディア音量が **4 刻み**でしか変わらず、「1」に設定できない。これを解決するため、`AudioManager.setStreamVolume()` を使って **1 刻み**で音量インデックスを設定できる単一 Activity の Kotlin アプリを作り、サイドロードする。

リポジトリは**完全に空**（コミットなし・`.git` のみ）の新規プロジェクト。ゼロからプロジェクト一式を生成する。

**確定スコープ（v1）:**
- 必須: 現在音量の表示 / SeekBar で 0〜max を 1 刻み設定 / ±1 ボタン / 「1 にする」ボタン
- 任意（採用）: **音量の自動追従**（ContentObserver）、**複数ストリーム切替**（Spinner）
- 任意（不採用）: 小音量の注意表示 / FLAG_SHOW_UI（`setStreamVolume` の flag は `0` を使用）

## ツールチェーン（Web で検証済み・2026-07 現行）
| 項目 | 値 | 備考 |
|---|---|---|
| AGP | **9.1.1** | Gradle 9.1.0 / JDK 17 / build-tools 36 を要求。**Kotlin ビルトイン内蔵（KGP 2.2.10 同梱）** |
| Gradle | **9.1.0** | Wrapper で固定（`gradle-9.1.0-bin.zip`） |
| JDK | **17** | AGP 9.1 の最小/既定 |
| build-tools | **36.0.0** | AGP 9.1 の最小/既定 |
| compileSdk | **36** | platform は `android-36` を導入 |
| minSdk / targetSdk | **30 / 30** | Fire OS 8 = Android 11 = API 30 |
| cmdline-tools | **14742923** | 現行公式ビルド番号 |

**AGP 9 の重要点（検証済み）:**
- `org.jetbrains.kotlin.android` は**適用しない**（不要かつ新 DSL と非互換）。プラグインは `com.android.application` のみ。
- `namespace` は build.gradle で**必須**。Manifest に `package` 属性は書かない。

**設計方針: 外部依存ゼロ。** UI は framework View（`Activity` + `Theme.Material.Light` + `SeekBar`/`Button`/`TextView`/`Spinner`）のみで構成し、AndroidX / Material / Google Play 依存ライブラリを一切使わない（Fire OS に Google Play Services が無い制約とも整合）。ビュー参照は `findViewById` を使用（viewBinding 設定不要）。

## プロジェクト構成（生成するファイル）
```
VolNotch/
├── settings.gradle.kts            # pluginManagement(google/mavenCentral/portal), include(":app")
├── build.gradle.kts               # plugins { id("com.android.application") version "9.1.1" apply false }
├── gradle.properties              # org.gradle.jvmargs, android.useAndroidX 不要(=依存ゼロ)
├── gradlew / gradlew.bat          # Gradle Wrapper 起動スクリプト
├── gradle/wrapper/
│   ├── gradle-wrapper.properties  # distributionUrl = gradle-9.1.0-bin.zip
│   └── gradle-wrapper.jar         # ← 実装時に Gradle 9.1 で生成（バイナリ）
├── app/
│   ├── build.gradle.kts           # namespace/applicationId=com.volnotch, compileSdk36, min/target30
│   ├── proguard-rules.pro         # 空でよい（release 未使用）
│   └── src/main/
│       ├── AndroidManifest.xml    # MODIFY_AUDIO_SETTINGS, LAUNCHER Activity(exported=true)
│       ├── java/com/volnotch/MainActivity.kt
│       └── res/
│           ├── layout/activity_main.xml
│           ├── values/strings.xml     # アプリ名・ラベル文言
│           ├── values/themes.xml      # @android:style/Theme.Material.Light.NoActionBar ベース
│           ├── drawable/ic_launcher_foreground.xml   # ベクター（PNG 不要）
│           ├── values/ic_launcher_background.xml     # 背景色
│           └── mipmap-anydpi-v26/ic_launcher.xml     # adaptive icon（minSdk30≥26 なので PNG フォールバック不要）
└── README.md
```

## 主要ファイルの実装内容

### `AndroidManifest.xml`
- `<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />`
- `application`（icon/label/theme）内に `MainActivity` を `android:exported="true"` + `MAIN`/`LAUNCHER` インテントフィルタで宣言。`package` 属性は書かない（namespace で解決）。

### `MainActivity.kt`（`android.app.Activity` を継承）
- `audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager`
- **ストリーム定義**: `Pair<表示名, streamType>` のリスト
  - メディア=`STREAM_MUSIC`（既定選択）/ 着信音=`STREAM_RING` / 通知=`STREAM_NOTIFICATION` / アラーム=`STREAM_ALARM` / システム=`STREAM_SYSTEM`
  - `Spinner` に表示名を出し、選択中の streamType を `currentStream` に保持
- **`syncUiFromSystem()`**: `currentStream` の `getStreamMaxVolume` を `seekBar.max`、`getStreamVolume` を `seekBar.progress` に反映し、ラベルを「メディア音量: 3 / 15」形式に更新
- **`applyVolume(index)`**: `index` を `0..max` にクランプ → `audio.setStreamVolume(currentStream, index, 0)` → 直後に `getStreamVolume` を読み直して UI 同期（システム側クランプ/丸めに追従）
- **SeekBar**: `OnSeekBarChangeListener.onProgressChanged` で `if (fromUser) applyVolume(progress)`（プログラム更新は `fromUser=false` なので無限ループしない）
- **ボタン**: 「−1」/「+1」→ `applyVolume(getStreamVolume ± 1)`、「音量を 1 にする」→ `applyVolume(1)`
- **Spinner**: 選択変更で `currentStream` 更新 → `syncUiFromSystem()`
- **自動追従（ContentObserver）**:
  - `Handler(Looper.getMainLooper())` を渡した `ContentObserver` を用意し、`contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)`
  - `onChange` で `syncUiFromSystem()`（物理ボタン・他アプリ・本アプリ由来の変更すべてを UI へ反映）
  - **ライフサイクル**: `onStart` で登録 + 初回同期、`onStop` で解除、`onResume` でも念のため再同期

### `activity_main.xml`
縦 `LinearLayout`（padding あり）:
1. 見出し `TextView`（"VolNotch"）
2. ストリーム選択 `Spinner`
3. 音量ラベル `TextView`（"メディア音量: 3 / 15"）
4. `SeekBar`
5. 横 `LinearLayout`: 「−1」`Button` / 「+1」`Button`
6. 「音量を 1 にする」`Button`

## ビルド検証（この環境で実施）
1. `java -version` 確認 → 無ければ `sudo apt install -y openjdk-17-jdk unzip wget`
2. cmdline-tools を Google 公式 zip（`commandlinetools-linux-14742923_latest.zip`）から取得し `~/android-sdk/cmdline-tools/latest/` に配置、`ANDROID_HOME`/`PATH` 設定
3. `yes | sdkmanager --licenses` → `sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"`
4. **Gradle Wrapper 生成**: `gradle-9.1.0-bin.zip` を取得し、その `gradle` で `gradle wrapper --gradle-version 9.1.0 --distribution-type bin` を実行 → `gradlew` / `gradle-wrapper.jar` を確定
5. `./gradlew assembleDebug` → 成果物 `app/build/outputs/apk/debug/app-debug.apk` の生成を確認
   - ※ SDK/Gradle の数百 MB ダウンロードとネットワークが必要。`sudo` やネットワークが制限される場合はその旨を報告し、ソース + Wrapper + README は完成状態で残す

## README.md（build + sideload 手順）
要件定義の「WSL2 CLI セットアップ手順」「サイドロード手順」をそのまま README に転記・整形して収録:
- WSL2 セットアップ（apt で JDK17 → cmdline-tools 直取得 → sdkmanager でパッケージ導入 → `./gradlew assembleDebug`）
- 検証済みバージョン早見表
- サイドロード（推奨: adb 不要ルート＝クラウド/USB 転送 → 不明ソース許可 → APK タップ。代替: usbipd-win / Windows 側 adb / adb over TCP/IP）
- アプリの使い方（ストリーム選択・スライダー・±1・「1 にする」・自動追従）

## Verification（動作確認）
- **ビルド検証**: `./gradlew assembleDebug` が成功し `app-debug.apk` が生成されること（上記手順で本環境実行）
- **静的確認**: `./gradlew tasks` が通り、`namespace`/`compileSdk`/`min/target` が期待値であること
- **端末動作（ユーザー実施 / README 記載）**: Fire Max 11 にサイドロード後、
  1. 起動時にメディア音量の現在値/最大値が表示される
  2. SeekBar を 1 刻みで動かすと即時反映される
  3. ±1 ボタン・「1 にする」ボタンが機能する
  4. 物理ボタンや他アプリで音量を変えると UI が自動追従する
  5. Spinner でストリームを切り替えると対象が変わる
