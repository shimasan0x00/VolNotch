# ビルド手順（sudo / apt 版）

WSL2 上で `sudo` が使える環境向けの、環境構築 → ビルドまでのコピペ手順。
`sudo` が使えない場合は [README.md](README.md) の「JDK 17 を用意 → B.ポータブル JDK」を参照。

検証済みバージョン: AGP 9.1.1 / Gradle 9.3.1 / JDK 17 / build-tools 36.0.0 / compileSdk 36 / min・targetSdk 30。
（AGP 9.1.1 は Gradle 9.3.1 以上が必須。）

---

## 1. JDK 17 と補助ツール（apt）
```bash
sudo apt update && sudo apt install -y openjdk-17-jdk unzip wget
java -version    # 17.x が出れば OK
# 必要なら JAVA_HOME も設定（apt の既定パス）
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

## 2. Android cmdline-tools を Google 公式 zip から取得
```bash
mkdir -p ~/android-sdk/cmdline-tools && cd ~/android-sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-14742923_latest.zip
unzip -q commandlinetools-linux-*.zip
mv cmdline-tools latest       # bin/ が latest/ 直下に来る構成にする（重要）
```

## 3. 環境変数（`~/.bashrc` に追記して永続化）
```bash
export ANDROID_HOME=$HOME/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
```

## 4. SDK パッケージ導入
```bash
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"
```

> Gradle Wrapper（`gradlew` / `gradle-wrapper.jar`）は同梱済み。以前の `gradle wrapper` 生成手順は不要。

## 5. SDK の場所を Gradle に伝える + ビルド
```bash
cd /home/shimasan0x00/products/VolNotch
echo "sdk.dir=$HOME/android-sdk" > local.properties
./gradlew assembleDebug   # 初回は gradlew が Gradle 9.3.1 を自動取得
# 生成物: app/build/outputs/apk/debug/app-debug.apk
```

---

## サイドロード（Fire Max 11）
1. `app/build/outputs/apk/debug/app-debug.apk` をクラウド / USB メモリ等で Fire へ転送。
2. Fire 側「設定 > セキュリティとプライバシー > 不明ソースからのアプリ」を許可。
3. ファイルマネージャで APK をタップしてインストール。

adb を使う場合の代替手段は [README.md](README.md) を参照。
