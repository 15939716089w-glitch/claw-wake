package com.clawwake.app;

import android.service.voice.VoiceInteractionSession;
import android.content.Context;
import android.os.Bundle;

public final class ClawVoiceSession extends VoiceInteractionSession {
    public ClawVoiceSession(Context context) { super(context); }
    @Override public void onShow(Bundle args, int showFlags) {
        super.onShow(args, showFlags);
        setContentView(new ClawWakeView(getContext()));
    }
}
