package cn.qingzhuan.app;

/** Pure geometry policy, independent of Android decoders and UI defaults. */
public final class ImageTransformPolicy {
    public final int width,height,quality;
    public final boolean advanced;
    private ImageTransformPolicy(int w,int h,int q,boolean a){width=w;height=h;quality=q;advanced=a;}
    public static ImageTransformPolicy resolve(int w,int h,boolean advanced,String preset,int customW,int customH,boolean aspect,int quality) {
        if(w<1||h<1)throw new IllegalArgumentException("无法读取图片尺寸");
        if(!advanced)return checked(w,h,100,false);
        if(quality<20||quality>100)throw new IllegalArgumentException("画质应在20–100之间");
        int bw,bh;
        switch(preset){
            case "original":return checked(w,h,quality,true);
            case "4k":bw=3840;bh=2160;break;
            case "1080p":bw=1920;bh=1080;break;
            case "720p":bw=1280;bh=720;break;
            case "custom":
                if(customW<1||customH<1||customW>32768||customH>32768)throw new IllegalArgumentException("自定义宽高应在1–32768之间");
                if(!aspect)return checked(customW,customH,quality,true);
                return fit(w,h,customW,customH,quality);
            default:throw new IllegalArgumentException("输出尺寸选项无效");
        }
        if(h>w){int t=bw;bw=bh;bh=t;}
        return fit(w,h,bw,bh,quality);
    }
    private static ImageTransformPolicy fit(int w,int h,int bw,int bh,int q){
        double ratio=Math.min(1d,Math.min((double)bw/w,(double)bh/h));
        return checked(Math.max(1,(int)Math.round(w*ratio)),Math.max(1,(int)Math.round(h*ratio)),q,true);
    }
    private static ImageTransformPolicy checked(int w,int h,int q,boolean a){
        if(w>32768||h>32768||(long)w*h>64L*1024*1024)throw new IllegalArgumentException("图片尺寸过大，请开启高级选项选择较小尺寸");
        return new ImageTransformPolicy(w,h,q,a);
    }
    public int sampleSize(int rawW,int rawH,boolean rotated){
        int dw=rotated?height:width,dh=rotated?width:height,s=1;
        while(rawW/(s*2)>=dw&&rawH/(s*2)>=dh&&s<16384)s*=2;
        return s;
    }
}
