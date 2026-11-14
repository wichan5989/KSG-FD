package com.example.feedbackkiller.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.*;
import java.util.Locale;

/** Four-channel control surface. Audio processing remains the original mono lab engine. */
public final class MainView {
  public interface Callbacks { void onBypassChanged(boolean bypass); void onStartStopClicked(); }
  public static final int NOTCH_COUNT=6;
  private final ScrollView root;
  private final TextView status;
  private final Button start;
  private final TextView[] notches=new TextView[NOTCH_COUNT];
  private final Context ctx;
  private final int bg=0xff0c101b, panel=0xff1a2030, cyan=0xff23d9ee, purple=0xffbb58ed, muted=0xffaab4ca;
  private boolean monitor=true, auto=false;
  private final boolean[] voice={false,false,false,false};
  private final String[] inputs={"None","None","None","None"}, outputs={"None","None","None","None"};
  private final String[] presets={"Default","Speech","Monitor","Custom"};
  private String preset="Default";
  private TextView label(String s,int size,int color){ TextView t=new TextView(ctx);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t; }
  private GradientDrawable shape(int fill,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(20);d.setStroke(2,stroke);return d;}
  private LinearLayout col(){LinearLayout l=new LinearLayout(ctx);l.setOrientation(1);return l;}
  private Button button(String text){Button b=new Button(ctx);b.setText(text);b.setAllCaps(false);b.setTextColor(-1);b.setBackground(shape(panel,0xff46516d));return b;}
  private void gap(LinearLayout p,int h){View v=new View(ctx);p.addView(v,new LinearLayout.LayoutParams(1,h));}
  private void choose(String title,String[] options,java.util.function.Consumer<String> cb){new AlertDialog.Builder(ctx).setTitle(title).setItems(options,(d,w)->cb.accept(options[w])).setNegativeButton("Cancel",null).show();}
  public MainView(Context context,Callbacks callbacks){
    ctx=context;root=new ScrollView(ctx);root.setFillViewport(true);root.setBackgroundColor(bg);
    LinearLayout page=col();page.setPadding(16,20,16,24);root.addView(page);
    TextView brand=label("◉  Kai Soundgood Ai",28,cyan);brand.setTypeface(null,Typeface.BOLD);page.addView(brand);
    page.addView(label("AI Feedback Control  |  4CH interface  |  v1.0 preview",12,muted));gap(page,14);
    LinearLayout actions=new LinearLayout(ctx);page.addView(actions);
    Button save=button("SAVE");actions.addView(save,new LinearLayout.LayoutParams(0,55,1));save.setOnClickListener(v->choose("Save preset",presets,s->{preset=s;Toast.makeText(ctx,"Preset selected: "+s,Toast.LENGTH_SHORT).show();}));
    Button settings=button("SETTINGS");actions.addView(settings,new LinearLayout.LayoutParams(0,55,1));settings.setOnClickListener(v->settings());
    Button routing=button("ROUTING");actions.addView(routing,new LinearLayout.LayoutParams(0,55,1));routing.setOnClickListener(v->routing());
    gap(page,12);
    Switch mode=new Switch(ctx);mode.setText("MONITOR / RTA ONLY     |     USB PROCESSING (LAB)");mode.setTextColor(-1);mode.setChecked(false);page.addView(mode);
    mode.setOnCheckedChangeListener((v,checked)->{monitor=!checked;Toast.makeText(ctx,monitor?"Monitor mode: analysis only":"USB processing: LAB / system default devices",Toast.LENGTH_LONG).show();});
    Switch autoSwitch=new Switch(ctx);autoSwitch.setText("AUTO FEEDBACK (OFF by default)");autoSwitch.setTextColor(purple);autoSwitch.setChecked(false);page.addView(autoSwitch);
    autoSwitch.setOnCheckedChangeListener((v,checked)->{auto=checked; if(checked){autoSwitch.setChecked(false);new AlertDialog.Builder(ctx).setTitle("Safety lock").setMessage("Automatic feedback suppression is not validated for live PA. Remains OFF. No M32 EQ writes are implemented.").setPositiveButton("OK",null).show();}});
    Switch bypass=new Switch(ctx);bypass.setText("SAFE BYPASS (ON by default)");bypass.setTextColor(-1);bypass.setChecked(true);page.addView(bypass);bypass.setOnCheckedChangeListener((v,c)->callbacks.onBypassChanged(c));
    gap(page,14);page.addView(label("CHANNELS",16,cyan));
    for(int row=0;row<2;row++){
      LinearLayout line=new LinearLayout(ctx);page.addView(line);
      for(int j=0;j<2;j++){
        int i=row*2+j;LinearLayout tile=col();tile.setPadding(12,12,12,12);tile.setBackground(shape(panel,i<2?cyan:0xff45506b));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(3,5,3,5);line.addView(tile,lp);
        TextView name=label((i+1)+"   MIC "+(i+1),18,-1);name.setTypeface(null,Typeface.BOLD);tile.addView(name);
        Button ai=button("AI  •  "+(i<2?"NO INPUT":"OFF"));tile.addView(ai);ai.setOnClickListener(v->new AlertDialog.Builder(ctx).setTitle("Channel "+(i+1)).setMessage("AI analysis preview. Audio input must be routed and validated before processing.").setPositiveButton("OK",null).show());
        TextView meter=label("IN  ▯▯▯▯▯▯     OUT  ▯▯▯▯▯▯",12,cyan);tile.addView(meter);
        TextView rta=label("RTA  •  NO INPUT\nSet input in ROUTING",12,muted);rta.setPadding(5,15,5,15);tile.addView(rta);
        Switch only=new Switch(ctx);only.setText("VOICE ONLY");only.setTextSize(11);only.setTextColor(purple);only.setChecked(false);tile.addView(only);only.setOnCheckedChangeListener((v,c)->{voice[i]=c;Toast.makeText(ctx,"Voice Only UI preview — no voice isolation DSP yet",Toast.LENGTH_LONG).show();});
      }
    }
    gap(page,12);status=label("STOPPED  •  SAFE BYPASS",14,cyan);page.addView(status);
    start=button("START AUDIO TEST (HEADPHONES ONLY)");page.addView(start);start.setOnClickListener(v->callbacks.onStartStopClicked());
    page.addView(label("Prototype: one mono DSP path, not four independent audio engines. No automatic M32 EQ writes. Never connect unvalidated processing to live PA.",12,muted));
    gap(page,12);page.addView(label("DETECTED NOTCHES (MONO LAB)",14,cyan));
    for(int i=0;i<NOTCH_COUNT;i++){notches[i]=label("— Hz",13,muted);page.addView(notches[i]);}
  }
  private void routing(){
    LinearLayout l=col();l.setPadding(18,8,18,8);
    TextView info=label("Routing selection preview. Physical USB channel mapping is not yet implemented; audio engine uses Android default I/O.",13,muted);l.addView(info);
    String[] devices={"None","Android default microphone","USB audio (requires implementation)"};
    for(int i=0;i<4;i++){final int ch=i;Button b=button("CH "+(i+1)+"  IN: "+inputs[i]+"  OUT: "+outputs[i]);l.addView(b);b.setOnClickListener(v->choose("Input CH "+(ch+1),devices,s->{inputs[ch]=s;b.setText("CH "+(ch+1)+"  IN: "+s+"  OUT: "+outputs[ch]);}));}
    new AlertDialog.Builder(ctx).setTitle("ROUTING  |  4 CH").setView(l).setPositiveButton("Close",null).show();
  }
  private void settings(){
    LinearLayout l=col();l.setPadding(20,10,20,10);
    l.addView(label("Preset: "+preset+"\nSample rate: 48 kHz\nDSP: Mono lab engine\nAuto Feedback: OFF (safety locked)\nM32 EQ write: DISABLED\nUSB routing: preview only",15,muted));
    Button p=button("SELECT PRESET");l.addView(p);p.setOnClickListener(v->choose("Presets",presets,s->{preset=s;Toast.makeText(ctx,s+" selected (UI only)",Toast.LENGTH_SHORT).show();}));
    new AlertDialog.Builder(ctx).setTitle("SETTINGS").setView(l).setPositiveButton("Close",null).show();
  }
  public View root(){return root;}
  public void setStatus(String s){status.setText(s);}
  public void setRunning(boolean running){start.setText(running?"STOP AUDIO TEST":"START AUDIO TEST (HEADPHONES ONLY)");}
  public void setNotches(float[] hz){for(int i=0;i<NOTCH_COUNT;i++)notches[i].setText(hz[i]>0?String.format(Locale.US,"NOTCH %d: %.0f Hz",i+1,hz[i]):"NOTCH "+(i+1)+": — Hz");}
}
