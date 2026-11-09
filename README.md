# Claw Wake — Android voice assistant prototype 0.3.0

Claw Wake is an **in-app** Chinese voice assistant for an OpenClaw Gateway running on the same Android phone. It does not register itself as Android's system VoiceInteractionService.

## Implemented in this branch

- Native Android SpeechRecognizer input for Simplified Chinese.
- OkHttp WebSocket transport to `ws://127.0.0.1:18789`.
- OpenClaw Gateway protocol v4 challenge/connect handshake.
- Per-installation Ed25519 device identity; the raw private key and issued device token are stored in AndroidX EncryptedSharedPreferences (Android Keystore-backed).
- `chat.send` requests using the `agent:main:main` session.
- Assistant text display and Android TextToSpeech playback.
- Visible connection, pairing, and request status messages.

## Current limitations / verification status

This is an early integration, **not yet verified on the target Vivo V2203A**. The code has been committed, but a successful Android Gradle build and a live Gateway conversation have not yet been confirmed.

- The app currently assumes the Gateway is on the same phone at `127.0.0.1:18789`.
- On first connection, paste the Gateway auth token into the app. After a successful pairing/hello response, the issued device token is saved in encrypted preferences; subsequent connections can use the saved device token.
- If the Gateway requires pairing, approve the request from the Termux environment with `openclaw devices approve <requestId>`, using the request ID shown in the app.
- The app uses the `agent:main:main` session. Change this constant in `GatewayChatClient.java` if your intended agent/session key differs.
- Android system speech recognition availability and language packs depend on the device's speech services.
- The current WebSocket client is a first integration pass: reconnect/backoff, chat history, streaming edge cases, and broader Gateway error recovery still need device testing and refinement.

## Build

Open the repository root in Android Studio with Android SDK 35 and JDK 17, sync Gradle, then run **Build > Build APK(s)**. The app module uses AGP 8.7.3 and Gradle 8.9.

## Security

- Never commit or paste a Gateway token into source code.
- The Gateway token is accepted in the app UI and held in memory for the initial connection; the issued device token and Ed25519 identity are stored using Android Keystore-backed encrypted preferences.
- The manifest currently allows cleartext traffic because the local Gateway uses `ws://`; the client endpoint is hard-coded to loopback for this same-phone setup.
