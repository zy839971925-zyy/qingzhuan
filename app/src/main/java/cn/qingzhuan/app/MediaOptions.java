package cn.qingzhuan.app;

import java.io.IOException;
import org.json.JSONObject;

/** Strict units and finite bounds shared by native GIF/transcode paths; JSON cannot bypass UI. */
public final class MediaOptions {
    public final long startMs,endMs;
    public final int edge,fps,quality,bitrate;
    public final boolean mute,loop;
    public MediaOptions(JSONObject json,long duration,boolean gif)throws IOException{
        double start=number(json,"clipStart",0),end=number(json,"clipEnd",0);
        if(start<0||end<0||start>86400||end>86400)throw new IOException("截取时间需要在0–86400秒之间");
        startMs=Math.round(start*1000);
        long requested=end==0?(gif?Math.min(duration,startMs+10000):duration):Math.round(end*1000);
        endMs=Math.min(duration,requested);
        if(duration<=0||startMs>=duration||endMs<=startMs)throw new IOException("截取结束时间需要大于开始时间，且在视频时长内");
        if(gif&&endMs-startMs>60000)throw new IOException("GIF最多60秒，请缩短截取范围");
        edge=integer(json,gif?"gifEdge":"videoEdge",gif?480:1080,gif?128:144,gif?960:3840);
        fps=integer(json,gif?"gifFps":"videoFps",gif?12:30,gif?5:1,gif?30:60);
        quality=integer(json,"gifQuality",80,1,100);
        int kbps=integer(json,"videoBitrate",4000,100,80000);bitrate=kbps*1000;
        mute=json.optBoolean("mute",false)||"true".equals(json.optString("mute"));
        loop=!"false".equals(json.optString("gifLoop","true"));
    }
    private static double number(JSONObject j,String key,double fallback)throws IOException{
        String s=j.optString(key,"").trim();if(s.isEmpty())return fallback;
        try{double value=Double.parseDouble(s);if(!Double.isFinite(value))throw new NumberFormatException();return value;}catch(NumberFormatException e){throw new IOException("参数不是有效数字："+key);}
    }
    private static int integer(JSONObject j,String key,int fallback,int min,int max)throws IOException{
        double n=number(j,key,fallback);if(n<min||n>max||n!=Math.rint(n))throw new IOException("参数"+key+"需要在"+min+"–"+max+"之间");return(int)n;
    }
}
