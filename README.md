# VolNotch

Amazon **Fire Max 11 (第3世代 / Fire OS 8.3.3.8 = Android 11 / API 30)** 向けの、メディア音量を **1 刻み**で調整する補助アプリ。

ハードウェアボタンではメディア音量が 4 刻みでしか変わらず「1」に設定できない問題を、`AudioManager.setStreamVolume()` を直接呼ぶことで解決する。Google Play Services に依存しないため、サイドロードで使える。

## 機能
### 粗調整（ストリームインデックス）
- 選択中ストリームの現在音量 `現在値 / 最大値` を表示
- SeekBar で 0〜max を **1 刻み**で設定
- 「−1」「+1」ボタンで 1 ずつ増減
- 「音量を 1 にする」ワンタップ設定
- **自動追従**: 物理ボタン・他アプリでの音量変更を UI へ即時反映（`ContentObserver`）
- **ストリーム切替**: メディア / 着信音 / 通知 / アラーム / システムを Spinner で選択

### 全体の微調整（他アプリ含む・dB）
`setStreamVolume` の分解能は端末依存のインデックス（Fire Max 11 では最大 25 = 最小刻み 1/25 ≒ 4%）が下限で、これより細かくはできません。そこで **出力全体に減衰エフェクト（`DynamicsProcessing`）をグローバル出力(session 0)へ適用**し、**index=1 よりさらに小さく・dB 単位で細かく**絞れるようにしています。

- スライダー / 「−1 dB」「+1 dB」/ 「0 dB に戻す」で 0〜−40 dB を調整（負ほど静か）
- 0 dB のときはサービスを停止して素通し
- 設定値は保存され、次回起動時に復元・再適用される
- **持続性**: 減衰は前面サービス（[AttenuationService](app/src/main/java/com/volnotch/AttenuationService.kt)）が保持するため、**他アプリ使用中や VolNotch を閉じた後も維持**される。適用中は通知「VolNotch 全体減衰: −X dB」が常駐し、通知の「解除」または本体の「0 dB に戻す」で停止できる。
- **⚠ 端末依存**: グローバル出力への効果適用が許可されるかは端末次第。使用不可の場合は画面に「全体微調整: この端末では使用できません」と表示され、微調整 UI は無効化される（Fire Max 11 実機では利用可能・他アプリにも効くことを確認済み）。

## プロジェクト仕様（検証済み: 2026-07 時点）
| 項目 | 値 |
|---|---|
| 言語 | Kotlin（AGP 9 のビルトイン Kotlin。`org.jetbrains.kotlin.android` は不要） |
| AGP / Gradle | 9.1.1 / 9.3.1（AGP 9.1.1 は Gradle 9.3.1 以上が必須） |
| JDK | 17 |
| build-tools | 36.0.0 |
| compileSdk | 36（platform は `android-36`） |
| minSdk / targetSdk | 30 / 30 |
| 外部依存 | なし（framework View のみ。AndroidX / Material ライブラリ不使用） |
| namespace / applicationId | `com.volnotch` |

---

## ビルド手順（WSL2 / CLI）

> Gradle Wrapper（`gradlew` / `gradle/wrapper/gradle-wrapper.jar`）はリポジトリに同梱済み。
> SDK さえ用意すれば `./gradlew assembleDebug` だけでビルドできる（`./gradlew` が Gradle 9.3.1 を自動取得する）。

### 1. JDK 17 を用意

**A. `sudo` が使える場合（apt）**
```bash
sudo apt update && sudo apt install -y openjdk-17-jdk unzip wget
```

**B. `sudo` が使えない場合（ポータブル JDK を $HOME に展開）**
```bash
mkdir -p ~/opt && cd ~/opt
curl -fL -o jdk17.tar.gz \
  "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
tar -xzf jdk17.tar.gz
# 展開されたディレクトリ名（例: jdk-17.0.x+y）を JAVA_HOME に設定
export JAVA_HOME="$HOME/opt/$(ls ~/opt | grep -E '^jdk-17' | head -1)"
export PATH="$JAVA_HOME/bin:$PATH"
java -version   # 17.x が出れば OK
```

### 2. Android cmdline-tools を Google 公式 zip から取得
```bash
mkdir -p ~/android-sdk/cmdline-tools && cd ~/android-sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-14742923_latest.zip
unzip commandlinetools-linux-*.zip
mv cmdline-tools latest      # bin/ が latest/ 直下に来る構成にする（重要）
```

### 3. 環境変数（`~/.bashrc` に追記して永続化）
```bash
export ANDROID_HOME=$HOME/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
# JDK B ルートを使った場合は JAVA_HOME / PATH も併せて追記する
```

### 4. SDK パッケージ導入
```bash
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"
```

> Wrapper は同梱済みのため、以前必要だった `gradle wrapper` による生成手順は不要になった。

### 5. SDK の場所を Gradle に伝える（どちらか一方）
```bash
# 方法A: local.properties を作る（ANDROID_HOME を export 済みなら不要な場合が多い）
echo "sdk.dir=$HOME/android-sdk" > /home/shimasan0x00/products/VolNotch/local.properties
# 方法B: ANDROID_HOME を export しておく（手順3で対応済み）
```

### 6. ビルド
```bash
cd /home/shimasan0x00/products/VolNotch
./gradlew assembleDebug
# 生成物: app/build/outputs/apk/debug/app-debug.apk
```

---

## Fire Max 11 へインストール（サイドロード）

WSL2 は USB を直接認識しないため、`adb` で直結できない点に注意。

### 推奨: adb 不要ルート（自分用アプリならこれで十分）
1. `app-debug.apk` をクラウド / USB メモリ等で Fire へ転送。
2. Fire 側「設定 > セキュリティとプライバシー > 不明ソースからのアプリ」を許可。
3. ファイルマネージャで APK をタップしてインストール。

### 代替: adb を使いたい場合
- **usbipd-win**: Windows 側で USB を WSL2 へパススルー（`device is busy` で詰まる報告あり）。
- **Windows 側の adb**: APK を `/mnt/c/...` にコピーし Windows の `adb install` で流す。
- **adb over TCP/IP**: Fire を同一ネットワークに置きワイヤレス接続。

---

## 使い方
### 粗調整
1. アプリを起動すると、選択中ストリーム（既定=メディア）の現在音量が表示される。
2. Spinner で対象ストリームを切り替えられる。
3. SeekBar を 1 刻みで動かす / 「−1」「+1」 / 「音量を 1 にする」で調整。
4. 物理ボタンや他アプリで音量が変わっても UI が自動で追従する。

### 全体の微調整（さらに小さく）
5. 画面下部の「全体の微調整」スライダー / 「−1 dB」「+1 dB」 / 「0 dB に戻す」で、出力全体を dB 単位で減衰させる（index=1 でも大きいときに使う）。
6. 減衰中は通知「VolNotch 全体減衰: −X dB」が常駐し、他アプリ使用中や本アプリを閉じても維持される。通知の「解除」または「0 dB に戻す」で停止。

> メモ: Fire Max 11 のメディア最大インデックスは 25 のため、「1」でもそれなりに音が出る。より静かにしたいときは「全体の微調整」を併用する。
> 着信音 / 通知を 0 にする操作は、端末により通知ポリシー(DND)アクセスが必要で無視されることがある。
