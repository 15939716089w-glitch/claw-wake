# OpenClaw Gateway integration plan

Target: Claw Wake app-only voice assistant. Do not register a system VoiceInteractionService.

## Current state
- Android SpeechRecognizer captures Chinese speech locally.
- Android TextToSpeech is initialized.
- Recognized text is not yet sent to OpenClaw Gateway.
- Gateway protocol, device identity, pairing, chat streaming, and secure token storage remain unimplemented.

## Required implementation
1. Connect to the same-phone Gateway at `ws://127.0.0.1:18789` using OkHttp WebSocket, after verifying loopback reachability from the Android app.
2. Implement the OpenClaw Gateway v4 challenge/connect handshake. Wait for `connect.challenge`, use its nonce, and send a correctly signed Ed25519 device identity payload. Never hardcode or commit a Gateway token or private key.
3. Persist the device identity and auth material using Android Keystore-backed encryption. Support pairing-required responses and show the request ID needed for CLI approval.
4. Send recognized text using `chat.send` with a session key and idempotency key. Handle chat delta/final/error events and filter by run/session.
5. Display assistant text and speak the final response through Android TTS. Report connection, pairing, and request errors clearly in the UI.
6. Add tests for handshake payload construction, event parsing, and error handling; build and verify a debug APK.

## Constraints
- Keep all changes on `feature/gateway-chat`; do not modify `main`.
- Keep this as an in-app voice assistant, not a system-level assistant takeover.
- Do not log or expose tokens, private keys, or other credentials.
- Do not claim completion until the app builds and the Gateway conversation is tested on the target Vivo device.
