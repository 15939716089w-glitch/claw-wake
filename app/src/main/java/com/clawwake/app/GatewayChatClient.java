package com.clawwake.app;

import android.content.Context;
import android.util.Base64;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import okhttp3.*;
import okio.ByteString;
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator;
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Minimal OpenClaw Gateway v4 WebSocket client for the in-app voice assistant. */
public final class GatewayChatClient {
    private static final String URL = "ws://127.0.0.1:18789";
    private static final String CLIENT_ID = "openclaw-android";
    private static final String CLIENT_MODE = "ui";
    private static final String ROLE = "operator";
    private static final String SESSION_KEY = "agent:main:main";
    private static final String[] SCOPES = new String[]{"operator.read", "operator.write"};
    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS).readTimeout(0, TimeUnit.MILLISECONDS).build();
    private final android.content.SharedPreferences securePrefs;
    private final Consumer<String> status;
    private final Consumer<String> reply;
    private WebSocket socket;
    private String gatewayToken = "";
    private String deviceToken = "";
    private String connectRequestId;
    private String chatRequestId;
    private String activeRunId;
    private final StringBuilder responseBuffer = new StringBuilder();
    private boolean connected;

    public GatewayChatClient(Context context, Consumer<String> status, Consumer<String> reply) throws Exception {
        this.status = status; this.reply = reply;
        MasterKey masterKey = new MasterKey.Builder(context.getApplicationContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
        securePrefs = EncryptedSharedPreferences.create(context.getApplicationContext(),
                "claw_wake_secure", masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        deviceToken = securePrefs.getString("gateway.deviceToken", "");
    }

    public void connect(String token) {
        gatewayToken = token == null ? "" : token.trim();
        if (gatewayToken.isEmpty() && deviceToken.isEmpty()) {
            status.accept("请粘贴 Gateway Token 后再连接"); return;
        }
        if (socket != null) socket.close(1000, "reconnect");
        connected = false;
        status.accept("正在连接本机 Gateway…");
        Request request = new Request.Builder().url(URL).build();
        socket = http.newWebSocket(request, new WebSocketListener() {
            @Override public void onOpen(WebSocket webSocket, Response response) {
                status.accept("WebSocket 已连接，等待 Gateway 挑战…");
            }
            @Override public void onMessage(WebSocket webSocket, String text) {
                try { handleFrame(new JSONObject(text)); }
                catch (Exception e) { status.accept("Gateway 数据解析失败："+safeMessage(e)); }
            }
            @Override public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                connected = false;
                status.accept("Gateway 连接失败："+safeMessage(t)+"（确认 Termux Gateway 正在运行）");
            }
            @Override public void onClosed(WebSocket webSocket, int code, String reason) {
                connected = false; status.accept("Gateway 已断开："+reason);
            }
        });
    }

    private void handleFrame(JSONObject frame) throws Exception {
        String type = frame.optString("type", "");
        if ("event".equals(type)) {
            String event = frame.optString("event", "");
            JSONObject payload = frame.optJSONObject("payload");
            if ("connect.challenge".equals(event) && payload != null) {
                long challengeTs = payload.optLong("ts", -1L);
                sendConnect(payload.optString("nonce", ""), challengeTs);
            }
            else if ("chat".equals(event) && payload != null) handleChatEvent(payload);
            return;
        }
        if (!"res".equals(type)) return;
        String id = frame.optString("id", "");
        if (!frame.optBoolean("ok", false)) {
            JSONObject error = frame.optJSONObject("error");
            String message = error == null ? "Gateway 请求失败" : error.optString("message", "Gateway 请求失败");
            JSONObject details = error == null ? null : error.optJSONObject("details");
            String code = error == null ? "" : error.optString("code", "");
            if (id.equals(connectRequestId) && ("PAIRING_REQUIRED".equals(code) || message.toLowerCase().contains("pair"))) {
                String requestId = details == null ? "" : details.optString("requestId", "");
                status.accept(requestId.isEmpty()
                        ? "Gateway 要求设备配对，请在 Termux 执行 openclaw devices list 查看待审批请求"
                        : "需要设备配对，请在 Termux 执行：openclaw devices approve "+requestId);
            } else status.accept("Gateway 错误："+message);
            return;
        }
        JSONObject payload = frame.optJSONObject("payload");
        if (id.equals(connectRequestId) && payload != null) {
            JSONObject auth = payload.optJSONObject("auth");
            if (auth != null) {
                String issued = auth.optString("deviceToken", "");
                if (!issued.isEmpty()) {
                    deviceToken = issued;
                    securePrefs.edit().putString("gateway.deviceToken", issued).apply();
                }
            }
            connected = true;
            status.accept("已连接 OpenClaw · 可以开始说话");
        } else if (id.equals(chatRequestId)) {
            if (payload != null) activeRunId = payload.optString("runId", "");
            status.accept("消息已发送，等待 OpenClaw 回复…");
        }
    }

    private void sendConnect(String nonce, long challengeTs) throws Exception {
        if (nonce.isEmpty()) { status.accept("Gateway 未提供认证挑战"); return; }
        DeviceIdentity identity = loadOrCreateIdentity();
        // Current Gateway challenges carry the server timestamp; sign that exact value.
        // The local clock is only a compatibility fallback for older challenge-less peers.
        long signedAt = challengeTs >= 0 ? challengeTs : System.currentTimeMillis();
        String signatureToken = !deviceToken.isEmpty() ? deviceToken : gatewayToken;
        String scopes = String.join(",", SCOPES);
        String authPayload = String.join("|", "v3", identity.id, CLIENT_ID, CLIENT_MODE, ROLE,
                scopes, Long.toString(signedAt), signatureToken, nonce, "android", "phone");
        String signature = sign(authPayload, identity.privateKey);
        JSONObject client = new JSONObject();
        client.put("id", CLIENT_ID); client.put("displayName", "Claw Wake");
        client.put("version", "0.2.0"); client.put("platform", "android");
        client.put("deviceFamily", "phone"); client.put("mode", CLIENT_MODE);
        JSONObject device = new JSONObject();
        device.put("id", identity.id); device.put("publicKey", identity.publicKey);
        device.put("signature", signature); device.put("signedAt", signedAt); device.put("nonce", nonce);
        JSONObject auth = new JSONObject();
        if (!deviceToken.isEmpty()) auth.put("deviceToken", deviceToken);
        else auth.put("token", gatewayToken);
        JSONObject params = new JSONObject();
        params.put("minProtocol", 4); params.put("maxProtocol", 4);
        params.put("client", client); params.put("role", ROLE);
        params.put("scopes", new JSONArray(SCOPES)); params.put("device", device); params.put("auth", auth);
        params.put("locale", "zh-CN"); params.put("userAgent", "ClawWake/0.2.0 Android");
        connectRequestId = UUID.randomUUID().toString();
        sendRequest(connectRequestId, "connect", params);
        status.accept("正在验证设备身份…");
    }

    public void sendMessage(String message) {
        if (!connected || socket == null) { status.accept("尚未连接 Gateway，请先连接"); return; }
        try {
            responseBuffer.setLength(0); activeRunId = "";
            JSONObject params = new JSONObject();
            params.put("sessionKey", SESSION_KEY); params.put("message", message);
            params.put("idempotencyKey", UUID.randomUUID().toString());
            chatRequestId = UUID.randomUUID().toString();
            sendRequest(chatRequestId, "chat.send", params);
            status.accept("正在发送语音识别结果…");
        } catch (Exception e) { status.accept("发送失败："+safeMessage(e)); }
    }

    private void handleChatEvent(JSONObject payload) {
        String runId = payload.optString("runId", "");
        if (!activeRunId.isEmpty() && !runId.isEmpty() && !activeRunId.equals(runId)) return;
        JSONObject message = payload.optJSONObject("message");
        if (message != null && !"assistant".equals(message.optString("role", "assistant"))) return;
        String state = payload.optString("state", "");
        String text = message == null ? "" : extractText(message.opt("content"));
        if (text.isEmpty()) text = payload.optString("delta", "");
        if (!text.isEmpty()) {
            if ("delta".equals(state)) {
                responseBuffer.append(text);
                reply.accept(responseBuffer.toString());
            } else {
                responseBuffer.setLength(0); responseBuffer.append(text); reply.accept(text);
            }
        }
        if ("final".equals(state)) status.accept("OpenClaw 已回复");
        else if ("error".equals(state)) status.accept("OpenClaw 处理消息时发生错误");
    }

    private String extractText(Object content) {
        if (content instanceof String) return (String) content;
        if (content instanceof JSONArray) {
            StringBuilder out = new StringBuilder(); JSONArray parts = (JSONArray) content;
            for (int i=0;i<parts.length();i++) {
                JSONObject part = parts.optJSONObject(i);
                if (part != null && "text".equals(part.optString("type"))) out.append(part.optString("text", ""));
            }
            return out.toString();
        }
        return "";
    }

    private void sendRequest(String id, String method, JSONObject params) throws Exception {
        JSONObject frame = new JSONObject(); frame.put("type", "req"); frame.put("id", id);
        frame.put("method", method); frame.put("params", params);
        if (socket == null || !socket.send(frame.toString())) status.accept("无法发送 Gateway 请求");
    }

    private DeviceIdentity loadOrCreateIdentity() throws Exception {
        String id = securePrefs.getString("device.id", "");
        String pub = securePrefs.getString("device.public", "");
        String priv = securePrefs.getString("device.private", "");
        if (!id.isEmpty() && !pub.isEmpty() && !priv.isEmpty()) {
            return new DeviceIdentity(id, pub, Base64.decode(priv, Base64.NO_WRAP));
        }
        Ed25519KeyPairGenerator generator = new Ed25519KeyPairGenerator();
        generator.init(new Ed25519KeyGenerationParameters(new SecureRandom()));
        org.bouncycastle.crypto.AsymmetricCipherKeyPair pair = generator.generateKeyPair();
        byte[] publicRaw = ((Ed25519PublicKeyParameters) pair.getPublic()).getEncoded();
        byte[] privateRaw = ((Ed25519PrivateKeyParameters) pair.getPrivate()).getEncoded();
        String publicB64 = Base64.encodeToString(publicRaw, Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);
        String deviceId = hex(MessageDigest.getInstance("SHA-256").digest(publicRaw));
        securePrefs.edit().putString("device.id", deviceId).putString("device.public", publicB64)
                .putString("device.private", Base64.encodeToString(privateRaw, Base64.NO_WRAP)).apply();
        return new DeviceIdentity(deviceId, publicB64, privateRaw);
    }

    private String sign(String payload, byte[] privateRaw) {
        Ed25519PrivateKeyParameters key = new Ed25519PrivateKeyParameters(privateRaw, 0);
        Ed25519Signer signer = new Ed25519Signer(); signer.init(true, key);
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8); signer.update(bytes, 0, bytes.length);
        return Base64.encodeToString(signer.generateSignature(), Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);
    }

    private String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(); for (byte b:bytes) out.append(String.format("%02x", b)); return out.toString();
    }
    private String safeMessage(Throwable t) {
        String m=t.getMessage(); return m==null?t.getClass().getSimpleName():m.replaceAll("(?i)(token|authorization)\\s*[:=]\\s*[^\\s]+","$1=[redacted]");
    }
    public void close() {
        connected=false; if(socket!=null)socket.close(1000,"activity destroyed"); http.dispatcher().executorService().shutdown();
    }
    private static final class DeviceIdentity {
        final String id, publicKey; final byte[] privateKey;
        DeviceIdentity(String id,String publicKey,byte[] privateKey){this.id=id;this.publicKey=publicKey;this.privateKey=privateKey;}
    }
}
