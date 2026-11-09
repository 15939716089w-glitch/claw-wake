package com.clawwake.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.Locale;

public final class MainActivity extends Activity {
    private ClawWakeView view;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private GatewayChatClient gateway;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        view = new ClawWakeView(this);
        setContentView(view);
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.SIMPLIFIED_CHINESE);
        });
        view.setVoiceAction(this::startListening);
        view.setConnectAction(token -> connectGateway(token));
        view.setStatus("界面已启动 · 请先连接本机 Gateway");
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

    private void startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 71); return;
        }
        if (gateway == null) { view.setStatus("请先粘贴 Gateway Token 并连接"); return; }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { view.setStatus("此设备暂不支持系统语音识别"); return; }
        if (recognizer != null) recognizer.destroy();
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(Bundle p) { view.setStatus("正在聆听…"); }
            public void onBeginningOfSpeech() { view.setStatus("听到你的声音了…"); }
            public void onRmsChanged(float r) { view.setLevel(r); }
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() { view.setStatus("正在识别中文语音…"); }
            public void onError(int e) { view.setStatus("语音识别未完成，请再试一次（代码 "+e+"）"); }
            public void onResults(Bundle b) {
                ArrayList<String> results = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (results != null && !results.isEmpty()) {
                    String recognized = results.get(0);
                    view.setRecognizedText(recognized);
                    if (gateway != null) gateway.sendMessage(recognized);
                }
            }
            public void onPartialResults(Bundle b) {}
            public void onEvent(int t, Bundle p) {}
        });
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizer.startListening(i);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 71 && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startListening();
        else view.setStatus("需要麦克风权限才能进行语音输入");
    }

    @Override protected void onDestroy() {
        if (recognizer != null) recognizer.destroy();
        if (gateway != null) gateway.close();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
