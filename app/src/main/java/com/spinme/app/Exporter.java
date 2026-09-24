package com.spinme.app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.*;
import android.media.*;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.*;
import java.nio.ByteBuffer;

public final class Exporter {
    public interface Progress { void onProgress(String message); }

    private static Bitmap render(SpinView.Snapshot s, long offsetMs, int w, int h) {
        Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(out);
        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        Bitmap src=s.bitmap;
        Movie movie=null;
        if (s.gifData!=null) movie=Movie.decodeByteArray(s.gifData,0,s.gifData.length);
        int sw=movie!=null?movie.width():(src!=null?src.getWidth():0);
        int sh=movie!=null?movie.height():(src!=null?src.getHeight():0);
        if (sw<=0||sh<=0) return out;
        float base=Math.min(w/(float)sw,h/(float)sh)*s.scale;
        float dw=sw*base, dh=sh*base;
        float left=(w-dw)*.5f, top=(h-dh)*.5f;
        float angle=angleAtOffset(s,offsetMs);
        c.save();
        c.rotate(angle,s.pivotX*w,s.pivotY*h);
        if(movie!=null){
            int pos=SpinView.mapSourcePosition(s.sourceElapsedMs+offsetMs,s.durationMs,s.mode);
            movie.setTime(pos);
            c.save(); c.translate(left,top); c.scale(base,base); movie.draw(c,0,0); c.restore();
        } else if(src!=null){
            Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
            c.drawBitmap(src,null,new RectF(left,top,left+dw,top+dh),p);
        }
        c.restore();
        return out;
    }

    public static Uri exportPng(Context ctx, SpinView.Snapshot s, int size) throws Exception {
        Bitmap b=render(s,0,size,size);
        ContentValues cv=new ContentValues();
        cv.put(MediaStore.Images.Media.DISPLAY_NAME,"SpinMe-"+System.currentTimeMillis()+".png");
        cv.put(MediaStore.Images.Media.MIME_TYPE,"image/png");
        cv.put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+"/SpinMe");
        Uri uri=ctx.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cv);
        if(uri==null) throw new IOException("Unable to create PNG destination");
        try(OutputStream os=ctx.getContentResolver().openOutputStream(uri)){
            if(os==null||!b.compress(Bitmap.CompressFormat.PNG,100,os)) throw new IOException("PNG encode failed");
        } finally { b.recycle(); }
        return uri;
    }

    public static Uri exportGif(Context ctx, SpinView.Snapshot s, int size, int durationMs, int fps, Progress progress) throws Exception {
        int gifFps=Math.max(1,Math.min(100,fps));
        ContentValues cv=new ContentValues();
        cv.put(MediaStore.Images.Media.DISPLAY_NAME,"SpinMe-"+System.currentTimeMillis()+".gif");
        cv.put(MediaStore.Images.Media.MIME_TYPE,"image/gif");
        cv.put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+"/SpinMe");
        Uri uri=ctx.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cv);
        if(uri==null) throw new IOException("Unable to create GIF destination");
        int frames=Math.max(1,(int)Math.ceil(durationMs*gifFps/1000.0));
        try(OutputStream os=ctx.getContentResolver().openOutputStream(uri); GifEncoder enc=new GifEncoder(os,size,size,gifFps)){
            for(int i=0;i<frames;i++){
                long t=Math.round(i*1000.0/gifFps);
                Bitmap b=render(s,t,size,size);
                enc.addFrame(b); b.recycle();
                if(progress!=null && (i%Math.max(1,frames/10)==0)) progress.onProgress("GIF "+(i+1)+"/"+frames);
            }
        }
        return uri;
    }

    public static Uri exportMp4(Context ctx, SpinView.Snapshot s, int size, int durationMs, int fps, Progress progress) throws Exception {
        int videoFps=Math.max(1,Math.min(240,fps));
        File tmp=new File(ctx.getCacheDir(),"spinme-"+System.nanoTime()+".mp4");
        encodeMp4(tmp,s,size,durationMs,videoFps,progress);
        ContentValues cv=new ContentValues();
        cv.put(MediaStore.Video.Media.DISPLAY_NAME,"SpinMe-"+System.currentTimeMillis()+".mp4");
        cv.put(MediaStore.Video.Media.MIME_TYPE,"video/mp4");
        cv.put(MediaStore.Video.Media.RELATIVE_PATH,Environment.DIRECTORY_MOVIES+"/SpinMe");
        Uri uri=ctx.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,cv);
        if(uri==null) throw new IOException("Unable to create MP4 destination");
        try(InputStream in=new FileInputStream(tmp); OutputStream out=ctx.getContentResolver().openOutputStream(uri)){
            if(out==null) throw new IOException("Unable to open MP4 destination");
            byte[] buf=new byte[128*1024]; int n; while((n=in.read(buf))>=0) out.write(buf,0,n);
        } finally { tmp.delete(); }
        return uri;
    }

    private static void encodeMp4(File file, SpinView.Snapshot s, int size, int durationMs, int fps, Progress progress) throws Exception {
        final String MIME="video/avc";
        MediaCodec codec=MediaCodec.createEncoderByType(MIME);
        int color=chooseColor(codec.getCodecInfo().getCapabilitiesForType(MIME).colorFormats);
        MediaFormat fmt=MediaFormat.createVideoFormat(MIME,size,size);
        fmt.setInteger(MediaFormat.KEY_COLOR_FORMAT,color);
        fmt.setInteger(MediaFormat.KEY_BIT_RATE,Math.max(2_000_000,Math.min(20_000_000,size*size*fps/2)));
        fmt.setInteger(MediaFormat.KEY_FRAME_RATE,fps);
        fmt.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1);
        codec.configure(fmt,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        codec.start();
        MediaMuxer muxer=new MediaMuxer(file.getAbsolutePath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
        int track=-1; boolean muxStarted=false;
        int frames=Math.max(1,(int)Math.ceil(durationMs*fps/1000.0));
        try {
            for(int i=0;i<frames;i++){
                int input=codec.dequeueInputBuffer(10_000);
                if(input<0){ drain(codec,muxer,info,new int[]{track},new boolean[]{muxStarted},false); i--; continue; }
                ByteBuffer ib=codec.getInputBuffer(input); if(ib==null) throw new IOException("No encoder input buffer");
                long tMs=Math.round(i*1000.0/fps);
                Bitmap b=render(s,tMs,size,size);
                byte[] yuv=toYuv420(b,color); b.recycle();
                ib.clear(); ib.put(yuv);
                long pts=i*1_000_000L/fps;
                codec.queueInputBuffer(input,0,yuv.length,pts,0);
                State st=drainState(codec,muxer,info,track,muxStarted,false); track=st.track; muxStarted=st.started;
                if(progress!=null && (i%Math.max(1,frames/10)==0)) progress.onProgress("MP4 "+(i+1)+"/"+frames);
            }
            int input;
            do { input=codec.dequeueInputBuffer(10_000); } while(input<0);
            codec.queueInputBuffer(input,0,0,frames*1_000_000L/fps,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            boolean eos=false;
            while(!eos){
                State st=drainState(codec,muxer,info,track,muxStarted,true); track=st.track; muxStarted=st.started; eos=st.eos;
            }
        } finally {
            try { codec.stop(); } catch(Exception ignored) {}
            codec.release();
            if(muxStarted) { try { muxer.stop(); } catch(Exception ignored) {} }
            muxer.release();
        }
    }

    private static final class State { int track; boolean started,eos; State(int t,boolean s,boolean e){track=t;started=s;eos=e;} }
    private static State drainState(MediaCodec codec, MediaMuxer muxer, MediaCodec.BufferInfo info, int track, boolean started, boolean waitForEos) {
        boolean eos=false;
        for(;;){
            int out=codec.dequeueOutputBuffer(info, waitForEos?10_000:0);
            if(out==MediaCodec.INFO_TRY_AGAIN_LATER) break;
            if(out==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){ if(started) throw new IllegalStateException("format changed twice"); track=muxer.addTrack(codec.getOutputFormat()); muxer.start(); started=true; continue; }
            if(out>=0){
                ByteBuffer ob=codec.getOutputBuffer(out);
                if(ob!=null && info.size>0 && started){ ob.position(info.offset); ob.limit(info.offset+info.size); muxer.writeSampleData(track,ob,info); }
                eos=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
                codec.releaseOutputBuffer(out,false);
                if(eos) break;
            }
        }
        return new State(track,started,eos);
    }
    private static void drain(MediaCodec c, MediaMuxer m, MediaCodec.BufferInfo i, int[] t, boolean[] s, boolean e){ State st=drainState(c,m,i,t[0],s[0],e);t[0]=st.track;s[0]=st.started; }

    private static int chooseColor(int[] formats){
        int flex=MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;
        int planar=MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
        int semi=MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
        for(int f:formats) if(f==flex) return f;
        for(int f:formats) if(f==planar) return f;
        for(int f:formats) if(f==semi) return f;
        throw new IllegalStateException("No YUV420 AVC input format");
    }

    private static byte[] toYuv420(Bitmap b,int color){
        int w=b.getWidth(),h=b.getHeight(); int[] argb=new int[w*h]; b.getPixels(argb,0,w,0,0,w,h);
        int frame=w*h; byte[] out=new byte[frame+frame/2];
        int yPos=0,uPos=frame,vPos=frame+frame/4,uvPos=frame;
        boolean semi=color==MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
        for(int j=0;j<h;j++) for(int i=0;i<w;i++){
            int c=argb[j*w+i],r=(c>>16)&255,g=(c>>8)&255,bl=c&255;
            int y=((66*r+129*g+25*bl+128)>>8)+16;
            int u=((-38*r-74*g+112*bl+128)>>8)+128;
            int v=((112*r-94*g-18*bl+128)>>8)+128;
            out[yPos++]=(byte)clamp(y);
            if((j&1)==0&&(i&1)==0){
                if(semi){ out[uvPos++]=(byte)clamp(u); out[uvPos++]=(byte)clamp(v); }
                else { out[uPos++]=(byte)clamp(u); out[vPos++]=(byte)clamp(v); }
            }
        }
        return out;
    }
    private static float angleAtOffset(SpinView.Snapshot s,long offsetMs){
        double elapsedSeconds=Math.max(0L,offsetMs)/1000.0;
        if(!s.ramping||s.rampDurationMs<=0L){
            return normalize((float)(s.angleDeg+s.direction*s.rpm*6.0*elapsedSeconds));
        }

        double duration=Math.max(0.001,s.rampDurationMs/1000.0);
        double startProgress=Math.max(0.0,Math.min(duration,s.rampElapsedMs/1000.0));
        double endProgress=startProgress+elapsedSeconds;
        double rampEnd=Math.min(endProgress,duration);

        double rpmSeconds=0.0;
        if(rampEnd>startProgress){
            rpmSeconds+=s.rpm*((rampEnd*rampEnd)-(startProgress*startProgress))/(2.0*duration);
        }
        rpmSeconds+=s.rpm*Math.max(0.0,endProgress-duration);

        return normalize((float)(s.angleDeg+s.direction*6.0*rpmSeconds));
    }

    private static int clamp(int v){return v<0?0:Math.min(255,v);}    
    private static float normalize(float d){float v=d%360f;return v<0?v+360f:v;}
}
