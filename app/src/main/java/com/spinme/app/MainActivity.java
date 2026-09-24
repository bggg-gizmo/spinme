package com.spinme.app;

import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int PICK_MEDIA=42;
    private SpinView spinView;
    private TextView status,rpmLabel,scaleLabel,angleLabel,pivotLabel,durationLabel,fpsLabel;
    private SeekBar rpmSeek,scaleSeek,angleSeek,pivotXSeek,pivotYSeek,durationSeek,fpsSeek;
    private Spinner modeSpinner,sizeSpinner;
    private Button pauseButton,directionButton,themeButton;
    private boolean clockwise=true,dark=true;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        buildUi();
    }

    private void buildUi(){
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(14),dp(12),dp(14),dp(24));
        scroll.addView(root,new ScrollView.LayoutParams(-1,-2));

        TextView title=text("SPINME",30,true); root.addView(title);
        TextView by=text("Background Gremlin Group",14,true); root.addView(by);
        TextView tag=text("Creating Unique Tools for Unique Individuals",13,false); root.addView(tag);
        addSpace(root,8);

        LinearLayout top=row();
        Button open=button("Import image / GIF"); open.setOnClickListener(v->pickMedia()); top.addView(open,weight());
        themeButton=button("Light"); themeButton.setOnClickListener(v->{dark=!dark; applyTheme(scroll);}); top.addView(themeButton,wrap());
        root.addView(top,matchWrap());

        spinView=new SpinView(this);
        spinView.setPivotListener((x,y)->runOnUiThread(()->{
            pivotXSeek.setProgress(Math.round(x*100)); pivotYSeek.setProgress(Math.round(y*100)); updatePivotLabel();
        }));
        root.addView(spinView,new LinearLayout.LayoutParams(-1,dp(420)));

        status=text("Import media to begin.",13,false); root.addView(status);

        addSection(root,"SOURCE PLAYBACK");
        modeSpinner=new Spinner(this);
        String[] modes={"Ping-pong","Loop","Once"};
        modeSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,modes));
        modeSpinner.setOnItemSelectedListener(new SimpleItemSelected(){public void selected(int p){ spinView.setPlaybackMode(p==1?SpinView.PlaybackMode.LOOP:p==2?SpinView.PlaybackMode.ONCE:SpinView.PlaybackMode.PING_PONG); }});
        root.addView(modeSpinner,matchWrap());

        addSection(root,"SPIN");
        rpmLabel=text("Speed: 60 RPM",14,true); root.addView(rpmLabel);
        rpmSeek=seek(3000,60,p->{spinView.setRpm(p);rpmLabel.setText("Speed: "+p+" RPM");}); root.addView(rpmSeek);
        LinearLayout presets=row();
        for(int v:new int[]{60,360,1000,3000}){ Button q=button(v==3000?"3k RPM":v+" RPM"); q.setOnClickListener(x->rpmSeek.setProgress(v)); presets.addView(q,weight()); }
        root.addView(presets,matchWrap());
        LinearLayout spinBtns=row();
        directionButton=button("Clockwise"); directionButton.setOnClickListener(v->{clockwise=!clockwise;spinView.setDirection(clockwise);directionButton.setText(clockwise?"Clockwise":"Counter-clockwise");}); spinBtns.addView(directionButton,weight());
        pauseButton=button("Pause spin"); pauseButton.setOnClickListener(v->{spinView.togglePause();pauseButton.setText(spinView.isSpinPaused()?"Resume spin":"Pause spin");}); spinBtns.addView(pauseButton,weight());
        root.addView(spinBtns,matchWrap());

        scaleLabel=text("Scale: 100%",14,true); root.addView(scaleLabel);
        scaleSeek=seek(290,90,p->{float s=(p+10)/100f;spinView.setScaleFactor(s);scaleLabel.setText("Scale: "+Math.round(s*100)+"%");}); root.addView(scaleSeek);
        angleLabel=text("Start angle: 0°",14,true); root.addView(angleLabel);
        angleSeek=seek(359,0,p->{spinView.setStartAngle(p);angleLabel.setText("Start angle: "+p+"°");}); root.addView(angleSeek);

        addSection(root,"PIVOT");
        pivotLabel=text("Pivot: 50%, 50%",14,true); root.addView(pivotLabel);
        pivotXSeek=seek(100,50,p->{spinView.setPivot(p/100f,pivotYSeek==null?.5f:pivotYSeek.getProgress()/100f);updatePivotLabel();}); root.addView(pivotXSeek);
        pivotYSeek=seek(100,50,p->{spinView.setPivot(pivotXSeek.getProgress()/100f,p/100f);updatePivotLabel();}); root.addView(pivotYSeek);
        TextView pivotHint=text("Drag directly on the preview to move the pivot.",12,false); root.addView(pivotHint);

        addSection(root,"EXPORT");
        LinearLayout sizeRow=row(); sizeRow.addView(text("Output size",14,true),weight());
        sizeSpinner=new Spinner(this); sizeSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"512 × 512","720 × 720","1080 × 1080","1440 × 1440"})); sizeRow.addView(sizeSpinner,wrap()); root.addView(sizeRow,matchWrap());
        durationLabel=text("Duration: 3.0 s",14,true); root.addView(durationLabel);
        durationSeek=seek(195,25,p->{float sec=.5f+p/10f;durationLabel.setText(String.format("Duration: %.1f s",sec));}); root.addView(durationSeek);
        fpsLabel=text("Output FPS: 30",14,true); root.addView(fpsLabel);
        fpsSeek=seek(239,29,p->{fpsLabel.setText("Output FPS: "+(p+1));}); root.addView(fpsSeek);
        LinearLayout exports=row();
        Button png=button("PNG"); png.setOnClickListener(v->export("PNG")); exports.addView(png,weight());
        Button gif=button("GIF"); gif.setOnClickListener(v->export("GIF")); exports.addView(gif,weight());
        Button mp4=button("MP4"); mp4.setOnClickListener(v->export("MP4")); exports.addView(mp4,weight());
        root.addView(exports,matchWrap());

        addSection(root,"ABOUT");
        root.addView(text("SpinMe v0.1.0\n© Background Gremlin Group\nCreating Unique Tools for Unique Individuals\n\nLocal-first processing. Source animation timing and spin timing are independent.",13,false));

        setContentView(scroll);
        applyTheme(scroll);
    }

    private void pickMedia(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("image/*"); startActivityForResult(i,PICK_MEDIA);
    }

    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);
        if(req!=PICK_MEDIA||result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        try { getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch(Exception ignored){}
        worker.execute(()->{
            try(InputStream in=getContentResolver().openInputStream(uri); ByteArrayOutputStream out=new ByteArrayOutputStream()){
                if(in==null) throw new IOException("Unable to open media");
                byte[] buf=new byte[64*1024]; int n; while((n=in.read(buf))>=0) out.write(buf,0,n);
                byte[] bytes=out.toByteArray();
                runOnUiThread(()->{spinView.setMedia(bytes);status.setText(spinView.isAnimated()?"Animated GIF loaded — source clock active.":"Image loaded.");});
            }catch(Exception e){ showError(e); }
        });
    }

    private void export(String type){
        if(!spinView.hasMedia()){Toast.makeText(this,"Import media first",Toast.LENGTH_SHORT).show();return;}
        final int size=new int[]{512,720,1080,1440}[sizeSpinner.getSelectedItemPosition()];
        final int duration=Math.round((.5f+durationSeek.getProgress()/10f)*1000);
        final int fps=fpsSeek.getProgress()+1;
        final SpinView.Snapshot snap=spinView.snapshot();
        status.setText("Exporting "+type+"…");
        worker.execute(()->{
            try{
                Uri uri;
                Exporter.Progress prog=m->runOnUiThread(()->status.setText(m));
                if(type.equals("PNG")) uri=Exporter.exportPng(this,snap,size);
                else if(type.equals("GIF")) uri=Exporter.exportGif(this,snap,size,duration,Math.min(100,fps),prog);
                else uri=Exporter.exportMp4(this,snap,size,duration,Math.min(240,fps),prog);
                runOnUiThread(()->status.setText(type+" saved: "+uri));
            }catch(Exception e){showError(e);} finally{snap.close();}
        });
    }

    private void showError(Exception e){runOnUiThread(()->{status.setText("Error: "+e.getMessage());Toast.makeText(this,"SpinMe: "+e.getMessage(),Toast.LENGTH_LONG).show();});}
    private int selectedSize(){return new int[]{512,720,1080,1440}[sizeSpinner.getSelectedItemPosition()];}
    private void updatePivotLabel(){ if(pivotXSeek!=null&&pivotYSeek!=null)pivotLabel.setText("Pivot: "+pivotXSeek.getProgress()+"%, "+pivotYSeek.getProgress()+"%"); }

    private void applyTheme(View root){
        int bg=dark?Color.rgb(9,9,9):Color.rgb(246,241,226);
        int fg=dark?Color.rgb(244,235,211):Color.rgb(45,38,27);
        int panel=dark?Color.rgb(24,24,24):Color.rgb(232,224,204);
        getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
        applyColors(root,bg,fg,panel);
        spinView.setBackgroundColor(dark?Color.rgb(14,14,14):Color.rgb(235,228,208));
        themeButton.setText(dark?"Light":"Dark");
    }
    private void applyColors(View v,int bg,int fg,int panel){
        if(v instanceof ScrollView || v instanceof LinearLayout) v.setBackgroundColor(bg);
        if(v instanceof TextView) ((TextView)v).setTextColor(fg);
        if(v instanceof Button){v.setBackgroundColor(panel);((Button)v).setTextColor(fg);}
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)applyColors(g.getChildAt(i),bg,fg,panel);}
    }

    private TextView text(String s,int sp,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setPadding(0,dp(3),0,dp(3));if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void addSection(LinearLayout root,String s){addSpace(root,12);TextView t=text(s,13,true);root.addView(t);}
    private void addSpace(LinearLayout root,int d){Space s=new Space(this);root.addView(s,new LinearLayout.LayoutParams(1,dp(d)));}
    private SeekBar seek(int max,int progress,final Seek cb){SeekBar s=new SeekBar(this);s.setMax(max);s.setProgress(progress);s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean f){cb.changed(p);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});return s;}
    private interface Seek{void changed(int p);}    
    private LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,-2,1f);}    
    private LinearLayout.LayoutParams wrap(){return new LinearLayout.LayoutParams(-2,-2);}    
    private LinearLayout.LayoutParams matchWrap(){return new LinearLayout.LayoutParams(-1,-2);}    
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}    
    private abstract static class SimpleItemSelected implements AdapterView.OnItemSelectedListener{public abstract void selected(int p);public void onItemSelected(AdapterView<?> p,View v,int pos,long id){selected(pos);}public void onNothingSelected(AdapterView<?> p){}}

    @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
