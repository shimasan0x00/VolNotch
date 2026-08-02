# Amazon Appstore 提出チェックリスト

Developer Console（https://developer.amazon.com/apps-and-games/console）での手作業。
**Upload Your App File / Target Your App / Appstore Details の 3 画面すべてが緑チェックになるまで提出できない。**

## 0. 事前

- [ ] Amazon 開発者アカウントを作成（登録無料。無料アプリなら税務・銀行情報の登録も不要）
- [ ] `versionCode` を確認。**更新提出のたびに +1 する**（初回は 1 のままでよい）
- [ ] release 署名 APK をビルドし、署名を検証する

```bash
./gradlew clean assembleRelease
"$ANDROID_HOME"/build-tools/36.0.0/apksigner verify --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

## 1. Upload Your App File

- [ ] `app/build/outputs/apk/release/app-release.apk` をアップロード
- [ ] **Allow Amazon to Apply DRM? → No**（無料アプリのため）
- [ ] zipalign エラーが出ないこと（AGP の release ビルドは自動で整列済み）

> Amazon はアップロード後に開発者署名を剥がし、開発者アカウント固有の Amazon 署名へ差し替える。
> このため Amazon 版と GitHub Releases 版は同じ端末に共存・上書きできない。

## 2. Target Your App

- [ ] **Supported Devices:** Fire タブレットのみを選択（Fire TV / Automotive は外す）
- [ ] **Availability:** 日本を含む配信国を選択
- [ ] **Target Audience & Content Rating:** 対象年齢を全年齢向けとし、コンテンツ質問票（暴力・薬物・ヌード・ギャンブル等）にすべて「なし」で回答
- [ ] **User Data Privacy:** 「ユーザーデータを収集または第三者に送信するか」→ **No**
      （`INTERNET` 権限なし・外部 SDK なしを確認済み。この場合プライバシーポリシー URL は不要）

> **プライバシー質問票は全アプリで必須。** 未完了・未解決の指摘があると新規公開も更新もできない。

## 3. Appstore Details

`store/listing-ja.md` から貼り付ける（文字数・バイト数は検証済み）。

| 項目 | 内容 | 実測 |
|---|---|---|
| 表示名 | `VolNotch` | — |
| 短い説明 | listing-ja.md より | 424 バイト / 上限 2,000 |
| 長い説明 | listing-ja.md より | 1,152 文字 / 上限 4,000 |
| 機能ハイライト | listing-ja.md より | 5 行 / 3〜5 行 |
| キーワード | listing-ja.md より | 任意項目 |

- [ ] **価格: 無料**
- [ ] 大アイコン `store/icon_512.png`（512×512 PNG 透過）
- [ ] 小アイコン `store/icon_114.png`（114×114 PNG 透過）
- [ ] スクリーンショット 4 枚（この順で並べる）
      - `store/screenshot_01_landscape_light.png`（1920×1200・ライト・メディア音量 3/25）
      - `store/screenshot_02_landscape_dark.png`（1920×1200・ダーク・全体減衰 −6 dB 動作中）
      - `store/screenshot_03_landscape_alarm.png`（1920×1200・ライト・アラーム音量 4/25＝メディア以外のストリーム）
      - `store/screenshot_04_portrait_light.png`（1200×1920・ライト・縦向き）
- [ ] プロモ画像（1024×500）は任意 — 初回は省略

## 4. 提出前の最終確認

- [ ] 実機（Fire Max 11 / Fire OS 8）で release APK の全機能を確認済み
- [ ] 「全体の微調整が使えません」の画面をスクリーンショットに含めていない
- [ ] 表示名に `Amazon` / `Fire` などの他社商標を含めていない
- [ ] 長い説明に「アプリを閉じても減衰が維持され、通知から解除できる」旨を明記している
      （審査担当者に「音量が戻らない不具合」と誤解されないため）

## 5. 提出後

- [ ] アップロード完了メールを確認（Amazon の publish サイクルは 30〜90 分ごと）
- [ ] 審査結果のメールを待つ
- [ ] リジェクト時は指摘箇所を直し、`versionCode` を +1 して再提出

## 既知の制約

- `targetSdk = 30` のため、配信対象は **Fire OS 7 / 8** の端末。Fire OS 14（最小 targetSdk 34）・
  Fire OS 16（同 36）の端末には配信されない。対象を広げる場合は targetSdk の引き上げと、
  それに伴う挙動確認（前面サービス種別の指定、通知権限など）が別途必要。
- `app/build.gradle.kts` の `lint` で `ExpiredTargetSdkVersion` を無効化している。これは
  「Google Play は API 33 以上を要求する」という Play 専用の要件で、Amazon には当てはまらない。

## 素材の再生成手順

アイコンとスクリーンショットは以下で作り直せる。

```bash
# アイコン（原本: store/icon.svg）
rsvg-convert -w 512 -h 512 -o store/icon_512.png store/icon.svg
rsvg-convert -w 114 -h 114 -o store/icon_114.png store/icon.svg

# スクリーンショット（原本: captures/store_*.png。captures/ は Git 管理外）
convert captures/store_light_landscape.png -resize 1920x1200 \
  -background '#F6F9F8' -gravity center -extent 1920x1200 \
  store/screenshot_01_landscape_light.png
convert captures/store_dark_landscape.png -resize 1920x1200 \
  -background '#0F1614' -gravity center -extent 1920x1200 \
  store/screenshot_02_landscape_dark.png
convert captures/store_light_alarm.png -resize 1920x1200 \
  -background '#F6F9F8' -gravity center -extent 1920x1200 \
  store/screenshot_03_landscape_alarm.png
convert captures/store_light_portrait.png -resize 1200x1920 \
  -background '#F6F9F8' -gravity center -extent 1200x1920 \
  store/screenshot_04_portrait_light.png
```

Fire Max 11 の画面は 2000×1200 で Amazon の許容サイズに無いため、長辺に合わせて縮小したうえで
アプリの背景色（ライト `#F6F9F8` / ダーク `#0F1614`）で余白を埋めて規定サイズにしている。
