# Claw Wake — Android prototype 0.2.0

## What changed in this revision
- Removed the VoiceInteractionService takeover path. Vivo does not expose this app as a selectable long-press power assistant on the user's device.
- Designed the launcher as the target for Vivo's built-in shortcut: screen off → long-press Volume Down → launch Claw Wake.
- Added Android SpeechRecognizer microphone permission flow and Chinese speech-to-text prototype.
- Added a native sci-fi orb interface and explicit status text.

## Important limitations
This is still a prototype, **not a finished OpenClaw client and not a verified APK**.
- Gateway WebSocket handshake, device identity/signature, pairing approval, chat streaming, and secure token storage are NOT implemented.
- The speech recognizer currently displays recognized text locally; it does not send that text to OpenClaw yet.
- Text-to-speech plumbing is initialized but no assistant response exists until Gateway chat is integrated.
- Vivo shortcut behavior must be tested on the target phone after installing a signed APK.

## Build
Open the `claw-wake` folder in Android Studio (AGP 8.7.3, compileSdk 35). Sync Gradle, then Build > Build APK(s). Requires Android SDK 35 and internet access to resolve Gradle dependencies.

## Vivo setup after installation
In Vivo Settings → Shortcuts & accessibility → Quick launch (wording varies), assign the screen-off long-press Volume Down shortcut to **Claw Wake**. This uses Vivo's supported app-launch shortcut; the app does not intercept hardware volume keys itself.

## Security
Never paste a Gateway token into source code or send it in chat. The eventual implementation must use the official OpenClaw Gateway protocol, device identity and pairing flow, and Android Keystore-backed encrypted storage.
