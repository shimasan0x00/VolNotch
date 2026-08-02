# Amazon Appstore listing text (English / US)

English (US) is the **fallback locale**: it is shown in every marketplace that has no translation,
and it is what the Amazon review team reads. Fill this in English, and add the Japanese text from
`listing-ja.md` as a separate Japanese locale.

Limits: short description 2,000 bytes / long description 4,000 characters / 3–5 feature bullets.

> The app interface is Japanese-only, so that is stated explicitly in both the short and long
> descriptions. Distribution is Japan-only, but this locale can still surface as a fallback.

## Display Title

```
VolNotch
```

## Short Description

```
A small volume helper for tablets whose media volume steps are too coarse for that "one notch quieter" moment. Set the volume index directly in single steps, and attenuate the entire output in decibels to get quieter than the lowest step. No third-party libraries, no network access, no data collection. Note: the app interface is in Japanese only.
```

## Product Feature Bullets

```
Set the volume index in single steps with a slider or the minus and plus buttons
Attenuate the whole audio output in decibels to go quieter than the lowest single step
Switch between the Media, Ring, Notification, Alarm and System streams
Attenuation is kept by an ongoing notification, so it survives leaving the app, and one tap clears it
No third-party libraries, no network access, no data collection
```

## Long Description

```
VolNotch is a small helper app for turning your tablet's media volume down more finely than the hardware steps allow.

Note: the app interface is in Japanese only.

WHY IT EXISTS
Many tablets expose only about a dozen to 25 internal volume steps for media. On a 25-step device, one step is worth roughly 4 percent, and the hardware volume buttons cannot move in anything smaller. Late at night or in a quiet room, even the lowest step of 1 can still be too loud.

WHAT IT DOES
Direct volume index control
The current value of the selected stream is shown large as "current / maximum", and you can set it in single steps with the slider or the minus and plus buttons. A "set volume to 1" button jumps straight to the minimum. The maximum index is read from the device for each stream, so it is never hard-coded and adapts to the hardware.

Whole-output fine adjustment in decibels
For when an index of 1 is still too loud, the entire audio output can be attenuated in decibel steps. "Back to 0 dB" restores it at any time.

Stream switching
Media, Ring, Notification, Alarm and System can be selected at the top of the screen.

Automatic display sync
When the volume changes from the hardware buttons or another app, the display follows automatically.

Persistent attenuation, and how to stop it
While attenuation is active, an ongoing notification reading "VolNotch overall attenuation: -X dB" stays in the shade, and the attenuation is deliberately kept while you use other apps and after you leave VolNotch. This is intended behaviour, not a stuck volume. Stop it at any time with "Clear" on the notification or "Back to 0 dB" inside the app.

PRIVACY
The app uses no third-party libraries and performs no network communication. It does not collect or transmit any data, personal or otherwise.

REQUIREMENTS AND LIMITATIONS
Runs on tablets with Android 9 (API 28) or later.
The whole-output fine adjustment depends on the device's audio processing support. On a device that does not support it, only that feature is disabled and the card shows a message saying so; volume index control keeps working.

IMPORTANT - PLEASE READ
This is a personal project provided AS IS under the MIT license. You use it entirely at your own risk. The author and distributor accept no liability for any damage arising from its use or misuse, including device malfunction, data loss, health effects such as hearing damage from high volume, and any other direct or indirect loss. Because the app manipulates volume and audio output, unexpected loud output is possible depending on the device and environment. Take particular care when using earphones. If you do not agree to this, please do not install or use the app.
```

## Keywords (optional)

```
volume,volume control,fine volume,quiet,low volume,media volume,volume limiter,attenuation
```
