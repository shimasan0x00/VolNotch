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
- [ ] **Target Audience & Content Rating:**
      - 対象年齢は **13 歳以上／一般向け**を選び、**子ども向け（child-directed）とは申告しない**
        ⚠️ 13 歳未満を含む年齢層を選ぶと Amazon の **Child-Directed App Policy** が適用され、
        追加の要件・審査が発生する。本アプリは子ども向けに作られた端末ツールではないため該当しない
      - コンテンツ質問票（暴力・薬物・ヌード・ギャンブル等）はすべて「なし」で回答
- [ ] **User Data Privacy:** 「ユーザーデータを収集または第三者に送信するか」→ **No**
      （`INTERNET` 権限なし・外部 SDK なしを確認済み。この場合プライバシーポリシー URL は不要）

> **プライバシー質問票は全アプリで必須。** 未完了・未解決の指摘があると新規公開も更新もできない。

## 3. Appstore Details

### ロケールは 2 つ入れる

**英語（米国）は「翻訳が無いマーケットプレイスすべてで表示されるフォールバック」であり、
Amazon の審査担当者が読む欄でもある。ここに日本語を入れてはいけない。**

| ロケール | 原本 | 役割 |
|---|---|---|
| **英語（米国）** | `store/listing-en.md` | 必須。フォールバック。審査担当者が読む |
| **日本語** を追加 | `store/listing-ja.md` | 日本のユーザーに表示される |

英語版には「UI は日本語のみ」である旨と、**常駐通知による減衰の維持が意図した仕様である**旨を
明記してある（審査担当者に「音量が戻らない不具合」と誤解されないため）。

### 文字数（検証済み）

| 項目 | 英語（米国） | 日本語 | 上限 |
|---|---|---|---|
| 表示名 | `VolNotch` | `VolNotch` | — |
| 短い説明 | 348 バイト | 424 バイト | 2,000 バイト |
| 長い説明 | 2,699 文字 | 1,152 文字 | 4,000 文字 |
| 機能ハイライト | 5 行 | 5 行 | 3〜5 行 |
| キーワード | 任意 | 任意 | — |

- [ ] **カテゴリ: `Utilities`**（サブカテゴリは `All-in-One Tools`。必須でなければ未指定でも可）
      Amazon はカテゴリに厳格な要件を課しておらず、主にブラウズツリーと
      Similar / Related / Recommended の推薦に使われる。`Utilities` 配下には
      `Battery Savers` / `Task & App Managers` / `Wi-Fi Analyzers` など端末調整ツールが並び、
      本アプリと同じ層。`Music & Audio` は音楽プレイヤー・配信アプリの枠なので採らない
- [ ] **価格: 無料**
- [ ] 大アイコン `store/icon_512.png`（512×512 PNG 透過）
- [ ] 小アイコン `store/icon_114.png`（114×114 PNG 透過）
- [ ] スクリーンショット 4 枚（この順で並べる）
      - `store/screenshot_01_landscape_light.png`（1920×1200・ライト・メディア音量 3/25）
      - `store/screenshot_02_landscape_dark.png`（1920×1200・ダーク・全体減衰 −6 dB 動作中）
      - `store/screenshot_03_landscape_system.png`（1920×1200・ライト・システム音量 3/**7**＝メディア以外のストリーム。
        この端末ではシステムだけ最大値が 7 で他は 25 のため、最大インデックスを端末・ストリームごとに
        自動取得していることが伝わる）
      - `store/screenshot_04_portrait_light.png`（1200×1920・ライト・縦向き）
- [ ] プロモ画像 `store/promo_1024x500.png`（1024×500・PNG）
      任意項目だが用意済み。Amazon のガイダンスは「スクリーンショットではなくアプリの
      ブランディングを見せるもの」なので、アイコン＋ワードマーク＋タグラインに、
      端末の粗い目盛りの 1 区間の中へ VolNotch が細かい目盛りを入れる図を添えている。
      配色は `values-night/colors.xml` のトークンをそのまま使用（製品と同じ顔にするため）。

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

# プロモ画像（原本: store/promo.svg。日本語の描画に Noto Sans CJK JP が必要）
rsvg-convert -w 1024 -h 500 -o store/promo_1024x500.png store/promo.svg

# スクリーンショット（原本: captures/store_*.png。captures/ は Git 管理外）
convert captures/store_light_landscape.png -resize 1920x1200 \
  -background '#F6F9F8' -gravity center -extent 1920x1200 \
  store/screenshot_01_landscape_light.png
convert captures/store_dark_landscape.png -resize 1920x1200 \
  -background '#0F1614' -gravity center -extent 1920x1200 \
  store/screenshot_02_landscape_dark.png
convert captures/store_light_system.png -resize 1920x1200 \
  -background '#F6F9F8' -gravity center -extent 1920x1200 \
  store/screenshot_03_landscape_system.png
convert captures/store_light_portrait.png -resize 1200x1920 \
  -background '#F6F9F8' -gravity center -extent 1200x1920 \
  store/screenshot_04_portrait_light.png
```

Fire Max 11 の画面は 2000×1200 で Amazon の許容サイズに無いため、長辺に合わせて縮小したうえで
アプリの背景色（ライト `#F6F9F8` / ダーク `#0F1614`）で余白を埋めて規定サイズにしている。
