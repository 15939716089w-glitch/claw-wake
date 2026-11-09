package com.clawwake.app;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** Claw Wake sci-fi launcher surface. Gateway transport is intentionally marked as pending. */
public final class ClawWakeView extends LinearLayout {
    private final TextView status;
    private final TextView transcript;
    private final OrbView orb;
    private Runnable voiceAction;
    private java.util.function.Consumer<String> speakAction;
    public ClawWakeView(Context c) {
        super(c); setOrientation(VERTICAL); setPadding(dp(22),dp(28),dp(22),dp(20)); setBackgroundColor(0xFF07121B);
        TextView brand=text("◈  CLAW WAKE",18,0xFFB7FFF2,true); addView(brand,new LayoutParams(-1,dp(34)));
        TextView sub=text("PERSONAL AI  /  LOCAL GATEWAY",10,0xFF7795A5,true); addView(sub,new LayoutParams(-1,dp(22)));
        status=text("●  INITIALIZING",12,0xFF65DCC7,true); LayoutParams st=new LayoutParams(-1,dp(30)); st.topMargin=dp(8); addView(status,st);
        orb=new OrbView(c); LayoutParams op=new LayoutParams(-1,0,1f); op.topMargin=dp(8); op.bottomMargin=dp(8); addView(orb,op);
        TextView title=text("我在，随时为你服务。",22,Color.WHITE,true); title.setGravity(Gravity.CENTER); addView(title,new LayoutParams(-1,dp(42)));
        TextView tagline=text("YOUR CLAW. YOUR RULES.",10,0xFF8298A8,true); tagline.setGravity(Gravity.CENTER); addView(tagline,new LayoutParams(-1,dp(24)));
        transcript=text("语音识别结果会显示在这里。",14,0xFFD6E5EC,false); transcript.setGravity(Gravity.CENTER); transcript.setPadding(dp(8),dp(8),dp(8),dp(8)); addView(transcript,new LayoutParams(-1,dp(56)));
        LinearLayout actions=new LinearLayout(c); actions.setGravity(Gravity.CENTER); actions.setOrientation(HORIZONTAL);
        Button mic=new Button(c); mic.setText("🎙  开始说话"); mic.setTextColor(Color.WHITE); mic.setBackground(bg(0xFF087F78,dp(28))); mic.setOnClickListener(v->{ if(voiceAction!=null)voiceAction.run(); });
        actions.addView(mic,new LayoutParams(0,dp(54),1)); addView(actions,new LayoutParams(-1,dp(58)));
        TextView note=text("唤醒方式：Vivo 熄屏长按音量减键",11,0xFF718696,false); note.setGravity(Gravity.CENTER); LayoutParams np=new LayoutParams(-1,dp(30)); np.topMargin=dp(10); addView(note,np);
        TextView disclaimer=text("当前版本：界面 + 系统语音识别原型；Gateway 对话尚未接入",10,0xFFFFC77A,false); disclaimer.setGravity(Gravity.CENTER); addView(disclaimer,new LayoutParams(-1,dp(30)));
    }
    public void setVoiceAction(Runnable r){voiceAction=r;}
    public void setSpeakAction(java.util.function.Consumer<String> a){speakAction=a;}
    public void setStatus(String s){status.setText("●  "+s);}
    public void setLevel(float level){orb.setLevel(level);}
    public void setRecognizedText(String s){transcript.setText("你："+s);}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(getContext());t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,1);return t;}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);d.setStroke(dp(1),0xFF287B80);return d;}
    private final class OrbView extends View {
        private final Paint p=new Paint(3); private float level=0;
        OrbView(Context c){super(c);}
        void setLevel(float l){level=Math.max(0,Math.min(10,l));invalidate();}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(getWidth(),getHeight())*.25f;float pulse=1+level*.012f;
            p.setShader(new RadialGradient(x,y,r*2.1f,new int[]{0xAA1AE7D1,0x5532A7FF,0x00101820},null,Shader.TileMode.CLAMP));c.drawCircle(x,y,r*2.1f,p);p.setShader(null);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.2f);p.setColor(0xFF65E8D5);c.drawCircle(x,y,r*pulse,p);p.setStrokeWidth(1);p.setColor(0x997BCFFF);c.drawCircle(x,y,r*1.22f,p);c.drawCircle(x,y,r*1.42f,p);
            p.setStyle(Paint.Style.FILL);p.setColor(0xFFB7FFF2);c.drawCircle(x,y,r*.12f,p);p.setTextSize(dp(11));p.setTextAlign(Paint.Align.CENTER);p.setColor(0xFF8FB6C4);c.drawText("CLAW CORE",x,y+r*1.8f,p);
        }
    }
}
