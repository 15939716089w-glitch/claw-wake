package com.clawwake.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.WindowManager;
import java.util.Locale;

public final class MainActivity extends Activity {
    private ClawWakeView view;
    private TextToSpeech tts;
    private GatewayChatClient gateway;
    private static final int REQUEST_IMPORT_TOKEN = 72;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        view = new ClawWakeView(this);
        setContentView(view);
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.SIMPLIFIED_CHINESE);
        });
        view.setSendAction(this::sendTextMessage);
        view.setConnectAction(token -> connectGateway(token));
        view.setImportTokenAction(this::chooseTokenFile);
        view.setStatus("界面已启动 · 请先连接本机 Gateway");
    }

    private void chooseTokenFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, REQUEST_IMPORT_TOKEN);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMPORT_TOKEN || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalStateException("无法打开所选文件");
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[1024]; int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            String token = new String(out.toByteArray(), StandardCharsets.UTF_8).trim();
            if (token.isEmpty() || token.length() > 4096 || token.contains("\\n")) {
                view.setStatus("文件内容不是有效的单行 Gateway Token");
                return;
            }
            view.setToken(token);
            view.setStatus("Token 已从本机文件导入 · 请点击连接");
        } catch (Exception e) {
            view.setStatus("导入失败：" + e.getClass().getSimpleName());
        }
    }

    private void connectGateway(String token) {
        try {
            if (gateway != null) gateway.close();
            gateway = new GatewayChatClient(this,
                    message -> view.post(() -> view.setStatus(message)),
                    text -> view.post(() -> {
                        view.setAssistantText(text);
                        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "claw-reply");
                    }));
            gateway.connect(token);
        } catch (Exception e) {
            view.setStatus("安全存储初始化失败："+e.getClass().getSimpleName());
        }
    }

    private void sendTextMessage(String message) {
        if (gateway == null) {
            view.setStatus("请先导入 Gateway Token 并连接本机 Gateway");
            return;
        }
        view.setRecognizedText(message);
        gateway.sendMessage(message);
    }

    @Override protected void onDestroy() {
        if (gateway != null) gateway.close();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
