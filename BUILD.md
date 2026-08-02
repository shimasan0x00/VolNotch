# ビルド手順（sudo / apt 版）

WSL2 上で `sudo` が使える環境向けの、環境構築 → ビルドまでのコピペ手順。
`sudo` が使えない場合は [README.md](README.md) の「JDK 17 を用意 → B.ポータブル JDK」を参照。

検証済みバージョン: AGP 9.1.1 / Gradle 9.3.1 / JDK 17 / build-tools 36.0.0 / compileSdk 36 / minSdk 28 / targetSdk 30。
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

---

## release 署名ビルド

Amazon Appstore 提出用・GitHub Releases 配布用の APK は release 署名でビルドする。

### 1. 署名鍵を作る（初回のみ。リポジトリ外に置くこと）

```bash
mkdir -p ~/keys
keytool -genkeypair -v \
  -keystore ~/keys/volnotch-release.jks \
  -storetype PKCS12 -alias volnotch \
  -keyalg RSA -keysize 4096 -validity 10950
```

> PKCS12 形式では store と key のパスワードを分けられない（keytool が `-keypass` を無視する）。
> 以降の設定で `keyPassword` を書く箇所には `storePassword` と同じ値を入れる。

### 2. 鍵情報を設定する

```bash
cp keystore.properties.example keystore.properties
# エディタで storeFile / storePassword / keyAlias / keyPassword を実値に書き換える
```

`keystore.properties` と `*.jks` は `.gitignore` 済み。

### 3. ビルドと署名検証

```bash
./gradlew clean assembleRelease
"$ANDROID_HOME"/build-tools/36.0.0/apksigner verify --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

`Signer #1 certificate DN:` に手順 1 で入力した名前が出れば成功。

`keystore.properties` が無い環境では release は**未署名**（`app-release-unsigned.apk`）でビルドされる。
CI では環境変数 `VOLNOTCH_STORE_FILE` / `VOLNOTCH_STORE_PASSWORD` / `VOLNOTCH_KEY_ALIAS` /
`VOLNOTCH_KEY_PASSWORD` から鍵情報を渡し、`apksigner verify` で署名の有無を機械的に検証する。

### 補足: lint の `ExpiredTargetSdkVersion` を無効化している

`assembleRelease` は `lintVitalRelease` を走らせ、`targetSdk = 30` に対して
「Google Play requires that apps target API level 33 or higher」を**致命的エラー**として出す。
これは Google Play 専用の要件で、本アプリの配信先（Amazon Appstore / サイドロード）には当てはまらない
（Amazon の Fire OS 8 向け最小 targetSdk は 30）。そのため `app/build.gradle.kts` の `lint` ブロックで
このチェックのみ無効化している。

> Amazon Appstore はアップロード後に開発者署名を剥がし、開発者アカウント固有の Amazon 署名に
> 差し替える。したがって Amazon 版と Releases 版は署名が異なり、同じ端末に共存・上書きできない。
