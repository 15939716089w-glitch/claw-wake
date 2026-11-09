package com.clawwake.app;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

public final class ClawWakeView extends LinearLayout {
    private final TextView status;
    private final TextView transcript;
    private final TextView assistant;
    private final OrbView orb;
    private final EditText tokenInput;
    private Runnable voiceAction;
    private java.util.function.Consumer<String> connectAction;
    private Runnable importTokenAction;

    public ClawWakeView(Context c) {
        super(c); setOrientation(VERTICAL); setPadding(dp(22),dp(22),dp(22),dp(16)); setBackgroundColor(0xFF07121B);
        TextView brand=text("◈  CLAW WAKE",18,0xFFB7FFF2,true); addView(brand,new LayoutParams(-1,dp(34)));
        TextView sub=text("PERSONAL AI  /  OPENCLAW GATEWAY",10,0xFF7795A5,true); addView(sub,new LayoutParams(-1,dp(22)));
        status=text("●  等待连接 OpenClaw",12,0xFF65DCC7,true); LayoutParams st=new LayoutParams(-1,dp(30)); st.topMargin=dp(6); addView(status,st);
        orb=new OrbView(c); LayoutParams op=new LayoutParams(-1,0,1f); op.topMargin=dp(4); op.bottomMargin=dp(4); addView(orb,op);
        TextView title=text("我在，随时为你服务。",22,Color.WHITE,true); title.setGravity(Gravity.CENTER); addView(title,new LayoutParams(-1,dp(40)));
        transcript=text("你说的话会显示在这里。",14,0xFFD6E5EC,false); transcript.setGravity(Gravity.CENTER); transcript.setPadding(dp(8),dp(5),dp(8),dp(5)); addView(transcript,new LayoutParams(-1,dp(42)));
        assistant=text("OpenClaw 的回复会显示在这里。",14,0xFF8FE8D8,false); assistant.setGravity(Gravity.CENTER); assistant.setPadding(dp(8),dp(4),dp(8),dp(4)); addView(assistant,new LayoutParams(-1,dp(54)));
        tokenInput=new EditText(c); tokenInput.setSingleLine(true); tokenInput.setTextSize(13); tokenInput.setTextColor(Color.WHITE); tokenInput.setHintTextColor(0xFF718696); tokenInput.setHint("粘贴 OpenClaw Gateway Token"); tokenInput.setInputType(129); tokenInput.setPadding(dp(12),0,dp(12),0); tokenInput.setBackground(bg(0xFF10232E,dp(10))); addView(tokenInput,new LayoutParams(-1,dp(46)));
        Button importToken=new Button(c); importToken.setText("从文件导入 Token"); importToken.setTextColor(0xFFB7FFF2); importToken.setBackground(bg(0xFF10232E,dp(20))); importToken.setOnClickListener(v->{if(importTokenAction!=null)importTokenAction.run();}); LayoutParams ip=new LayoutParams(-1,dp(42)); ip.topMargin=dp(6); addView(importToken,ip);
        Button connect=new Button(c); connect.setText("连接本机 Gateway"); connect.setTextColor(Color.WHITE); connect.setBackground(bg(0xFF174F65,dp(24))); connect.setOnClickListener(v->{if(connectAction!=null)connectAction.accept(tokenInput.getText().toString().trim());}); LayoutParams cp=new LayoutParams(-1,dp(46)); cp.topMargin=dp(5); addView(connect,cp);
        Button mic=new Button(c); mic.setText("🎙  开始说话"); mic.setTextColor(Color.WHITE); mic.setBackground(bg(0xFF087F78,dp(26))); mic.setOnClickListener(v->{if(voiceAction!=null)voiceAction.run();}); LayoutParams mp=new LayoutParams(-1,dp(50)); mp.topMargin=dp(5); addView(mic,mp);
        TextView note=text("本机地址：ws://127.0.0.1:18789",10,0xFF8298A8,false); note.setGravity(Gravity.CENTER); LayoutParams np=new LayoutParams(-1,dp(22)); np.topMargin=dp(5); addView(note,np);
    }
    public void setVoiceAction(Runnable r){voiceAction=r;}
    public void setConnectAction(java.util.function.Consumer<String> a){connectAction=a;}
    public void setImportTokenAction(Runnable a){importTokenAction=a;}
    public void setToken(String token){tokenInput.setText(token); tokenInput.setSelection(tokenInput.length());}
    public void setStatus(String s){status.setText("●  "+s);}
    public void setLevel(float level){orb.setLevel(level);}
    public void setRecognizedText(String s){transcript.setText("你："+s);}
    public void setAssistantText(String s){assistant.setText("Claw："+s);}
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
