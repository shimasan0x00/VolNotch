# Testing Instructions（Developer Console のテスト手順欄）

任意項目だが**必ず入れる**。本アプリは以下 3 点で審査担当者に誤解されやすい。

1. 減衰がアプリ終了後も維持される → 「音量が戻らない不具合」に見える
2. UI が日本語のみ → どのボタンが何をするか分からない
3. 審査端末が `DynamicsProcessing` 非対応だと「使用できません」表示になる → 機能不全に見える

以下をそのまま貼る（英語。審査担当者が読むため）。

```
No account, login, or in-app purchase is required. The app works fully offline.

THE INTERFACE IS IN JAPANESE. Control reference:
- メディア / 着信音 / 通知 / アラーム / システム = stream selector (Media / Ring / Notification / Alarm / System)
- メディア音量 = volume of the selected stream, shown as "current / maximum"
- 「-」 and 「+」 = decrease / increase the volume index by one step
- 「音量を 1 にする」 = set the volume index to 1
- 全体の微調整 = whole-output attenuation in decibels
- 「0 dB に戻す」 = reset the attenuation to 0 dB
- 「解除」 on the notification = clear the attenuation

HOW TO TEST
1. Launch the app. The current media volume is shown large as "current / maximum", for example 6 / 25.
2. Tap 「+」 or 「-」, or drag the slider. The device volume moves by one index step. Pressing the hardware volume buttons also updates the display automatically.
3. Use the selector at the top to switch streams. The card title and the maximum value change to match the selected stream. On this hardware the System stream has a different maximum (7) from the others (25); the app reads the maximum from the device rather than hard-coding it.
4. In the 全体の微調整 card, tap 「-」 a few times. The audio output is attenuated in 1 dB steps and an ongoing notification appears.

EXPECTED BEHAVIOUR, NOT A DEFECT
The attenuation is intentionally kept while you use other apps and after you leave VolNotch. This is the core feature of the app: it lets the output stay quieter than the lowest volume index the hardware allows. It is not a stuck volume. Clear it at any time with 「0 dB に戻す」 inside the app, or 「解除」 on the ongoing notification. The attenuation returns to 0 dB and the notification disappears.

IF THE WHOLE-OUTPUT ADJUSTMENT IS NOT SUPPORTED ON YOUR TEST DEVICE
That feature uses the Android DynamicsProcessing audio effect (API 28+). On a device that does not provide it, the card is disabled and shows 「この端末では全体の微調整を使用できません」 ("whole-output fine adjustment is not available on this device"). This is a deliberate fallback, not a crash, and the volume index controls keep working normally. Verified working on Fire Max 11 (Fire OS 8).

PERMISSIONS
MODIFY_AUDIO_SETTINGS and FOREGROUND_SERVICE only. No network permission, no third-party libraries, no data collection.
```
