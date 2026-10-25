package cn.qingzhuan.app;

import android.content.Context;
import android.graphics.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import androidx.media3.common.*;
import androidx.media3.effect.Presentation;
import androidx.media3.transformer.*;
import com.squareup.gifencoder.*;
import org.json.JSONObject;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Native decode, GIF89a encode and Media3/MediaCodec MP4 transcode. No shell, FFmpeg or server. */
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public final class NativeMediaEngine {
    public static String mime(String codec){switch(codec){case "h264":return "video/avc";case "hevc":return "video/hevc";case "av1":return "video/av01";default:return "";}}
    public static List<String> encoders(){
        List<String> result=new ArrayList<>();
        try{for(String codec:Arrays.asList("h264","hevc","av1")){String mime=mime(codec);boolean found=false;for(MediaCodecInfo info:new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos())if(info.isEncoder())for(String type:info.getSupportedTypes())if(mime.equalsIgnoreCase(type)){found=true;break;}if(found)result.add(codec);}}catch(RuntimeException ignored){}
        return result;
    }
    static long duration(MediaMetadataRetriever r)throws IOException{try{long n=Long.parseLong(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));if(n>0)return n;}catch(Exception ignored){}throw new IOException("无法读取视频时长");}
    static int dimension(MediaMetadataRetriever r,int key)throws IOException{try{return Math.max(1,Integer.parseInt(r.extractMetadata(key)));}catch(Exception e){throw new IOException("无法读取视频尺寸");}}
    static Bitmap frame(MediaMetadataRetriever r,long us,int width,int height)throws IOException{
        Bitmap b=Build.VERSION.SDK_INT>=27?r.getScaledFrameAtTime(us,MediaMetadataRetriever.OPTION_CLOSEST,width,height):r.getFrameAtTime(us,MediaMetadataRetriever.OPTION_CLOSEST);
        if(b==null)throw new IOException("手机未能解码这一帧，请更换视频或截取范围");
        if(b.getWidth()!=width||b.getHeight()!=height){Bitmap scaled=Bitmap.createScaledBitmap(b,width,height,true);if(scaled!=b)b.recycle();b=scaled;}return b;
    }
    public static void gif(File input,File output,JSONObject json,Core.Check check,Engine.Progress progress)throws Exception{
        MediaMetadataRetriever r=new MediaMetadataRetriever();try{
            r.setDataSource(input.getPath());MediaOptions o=new MediaOptions(json,duration(r),true);
            int sw=dimension(r,MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH),sh=dimension(r,MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String rotation=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);if("90".equals(rotation)||"270".equals(rotation)){int swap=sw;sw=sh;sh=swap;}
            float scale=Math.min(1f,o.edge/(float)Math.max(sw,sh));int w=Math.max(1,Math.round(sw*scale)),h=Math.max(1,Math.round(sh*scale));
            int count=(int)Math.ceil((o.endMs-o.startMs)*o.fps/1000.0);
            if((long)count*w*h>200_000_000L)throw new IOException("这组GIF参数处理量过大，请降低尺寸、帧率或时长");
            try(GifWriter writer=new GifWriter(output,w,h,o.fps,o.quality,o.loop)){
                int[] pixels=new int[w*h];
                for(int i=0;i<count;i++){
                    check.check();long us=(o.startMs+Math.min(o.endMs-o.startMs-1,Math.round(i*1000.0/o.fps)))*1000;
                    Bitmap bitmap=frame(r,us,w,h);try{bitmap.getPixels(pixels,0,w,0,0,w,h);}finally{bitmap.recycle();}
                    writer.addFrame(pixels);progress.set("生成GIF "+(i+1)+" / "+count,Math.min(99,(i+1)*100/count));
                    if(output.length()>128L*1024*1024)throw new IOException("GIF超过128MiB，请降低尺寸或帧率");
                }
                check.check();writer.finish(check);
            }
        }finally{r.release();}
    }
    public static void screenshot(File input,File output,JSONObject json,Core.Check check)throws Exception{
        MediaMetadataRetriever r=new MediaMetadataRetriever();try{
            r.setDataSource(input.getPath());long duration=duration(r);MediaOptions o=new MediaOptions(json,duration,false);
            int w=dimension(r,MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH),h=dimension(r,MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);String rotation=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);if("90".equals(rotation)||"270".equals(rotation)){int swap=w;w=h;h=swap;}float scale=Math.min(1f,o.edge/(float)Math.max(w,h));
            check.check();Bitmap b=frame(r,o.startMs*1000,Math.max(1,Math.round(w*scale)),Math.max(1,Math.round(h*scale)));
            try(OutputStream out=new FileOutputStream(output)){if(!b.compress(output.getName().endsWith(".png")?Bitmap.CompressFormat.PNG:Bitmap.CompressFormat.JPEG,90,out))throw new IOException("无法写入截图");}finally{b.recycle();}
        }finally{r.release();}
    }
    public static void transcode(Context context,File input,File output,String codec,JSONObject json,Core.Check check,Engine.Progress progress)throws Exception{
        if(!encoders().contains(codec))throw new IOException("这台手机没有"+codec.toUpperCase(Locale.ROOT)+"编码器；不会替换成其他编码");
        MediaMetadataRetriever metadata=new MediaMetadataRetriever();final MediaOptions options;final int width,height;
        try{metadata.setDataSource(input.getPath());options=new MediaOptions(json,duration(metadata),false);int w=dimension(metadata,MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH),h=dimension(metadata,MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);String rotation=metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);if("90".equals(rotation)||"270".equals(rotation)){int swap=w;w=h;h=swap;}float scale=Math.min(1f,options.edge/(float)Math.max(w,h));width=Math.max(2,Math.round(w*scale)/2*2);height=Math.max(2,Math.round(h*scale)/2*2);}finally{metadata.release();}
        HandlerThread thread=new HandlerThread("Qingzhuan-media-export");thread.start();Handler handler=new Handler(thread.getLooper());
        CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> error=new AtomicReference<>();AtomicReference<Transformer> reference=new AtomicReference<>();AtomicBoolean closing=new AtomicBoolean();
        handler.post(()->{try{
            VideoEncoderSettings settings=new VideoEncoderSettings.Builder().setBitrate(options.bitrate).build();
            Transformer transformer=new Transformer.Builder(context).setLooper(thread.getLooper()).setVideoMimeType(mime(codec))
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .setEncoderFactory(new DefaultEncoderFactory.Builder(context).setEnableFallback(false).setRequestedVideoEncoderSettings(settings).build())
                .addListener(new Transformer.Listener(){@Override public void onCompleted(Composition c,ExportResult result){done.countDown();}@Override public void onError(Composition c,ExportResult result,ExportException e){error.set(e);done.countDown();}}).build();
            reference.set(transformer);
            MediaItem media=new MediaItem.Builder().setUri(Uri.fromFile(input)).setClippingConfiguration(new MediaItem.ClippingConfiguration.Builder().setStartPositionMs(options.startMs).setEndPositionMs(options.endMs).build()).build();
            EditedMediaItem item=new EditedMediaItem.Builder(media).setRemoveAudio(options.mute).setFrameRate(options.fps)
                .setEffects(new Effects(Collections.emptyList(),Collections.singletonList(Presentation.createForWidthAndHeight(width,height,Presentation.LAYOUT_SCALE_TO_FIT)))).build();
            transformer.start(item,output.getPath());
            handler.post(new Runnable(){@Override public void run(){if(closing.get()||done.getCount()==0)return;try{ProgressHolder p=new ProgressHolder();if(transformer.getProgress(p)==Transformer.PROGRESS_STATE_AVAILABLE)progress.set("视频编码 "+p.progress+"%",Math.min(99,p.progress));else progress.set("正在处理视频",-1);}catch(RuntimeException e){error.set(e);done.countDown();}if(done.getCount()>0)handler.postDelayed(this,300);}});
        }catch(Throwable e){error.set(e);done.countDown();}});
        try{
            while(!done.await(250,TimeUnit.MILLISECONDS))check.check();
            check.check();if(error.get()!=null)throw new IOException("视频编码失败："+AppSession.message(error.get()),error.get());
            if(!output.isFile()||output.length()==0)throw new IOException("编码器没有生成可用的视频");
            MediaExtractor verify=new MediaExtractor();try{verify.setDataSource(output.getPath());boolean found=false;for(int i=0;i<verify.getTrackCount();i++)if(mime(codec).equals(verify.getTrackFormat(i).getString(MediaFormat.KEY_MIME)))found=true;if(!found)throw new IOException("输出实际编码与所选编码不一致");}finally{verify.release();}
        }finally{
            closing.set(true);CountDownLatch released=new CountDownLatch(1);handler.post(()->{try{Transformer t=reference.get();if(t!=null)t.cancel();}finally{released.countDown();}});
            // Do not let AppSession remove the partial path while native codecs still own it.
            boolean interrupted=false;boolean freed=false;long deadline=android.os.SystemClock.elapsedRealtime()+10000;
            try{while(!freed&&android.os.SystemClock.elapsedRealtime()<deadline)try{freed=released.await(1,TimeUnit.SECONDS);}catch(InterruptedException e){interrupted=true;}}
            finally{thread.quitSafely();if(interrupted)Thread.currentThread().interrupt();}
            if(!freed)throw new IOException("视频编码器未能及时释放资源，任务已取消");
        }
    }
}
