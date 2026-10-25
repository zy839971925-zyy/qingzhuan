package cn.qingzhuan.app;

import com.squareup.gifencoder.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** Streaming GIF89a writer; no Android dependency, frames are never retained as a movie in RAM. */
public final class GifWriter implements AutoCloseable {
    private final File file;private final OutputStream stream;private final GifEncoder encoder;
    private final int width,height,fps,quality;private final boolean loop;private int frames;private boolean finished;
    public GifWriter(File file,int width,int height,int fps,int quality,boolean loop)throws IOException{
        if(width<1||height<1||width>960||height>960||fps<5||fps>30)throw new IOException("GIF尺寸或帧率超出范围");
        this.file=file;this.width=width;this.height=height;this.fps=fps;this.quality=quality;this.loop=loop;
        stream=new BufferedOutputStream(new FileOutputStream(file));
        try{encoder=new GifEncoder(stream,width,height,0);}catch(IOException|RuntimeException e){stream.close();throw e;}
    }
    public void addFrame(int[] pixels)throws IOException{
        if(finished||pixels.length!=width*height)throw new IOException("GIF帧尺寸有误");
        if(quality<70){int bits=quality<35?2:3;int step=1<<(8-bits);for(int p=0;p<pixels.length;p++){int rgb=pixels[p];pixels[p]=((rgb>>16&255)/step*step)<<16|((rgb>>8&255)/step*step)<<8|((rgb&255)/step*step);}}
        long delay=(Math.round((frames+1)*100.0/fps)-Math.round(frames*100.0/fps))*10;
        encoder.addImage(pixels,width,new ImageOptions().setDelay(Math.max(20,delay),TimeUnit.MILLISECONDS).setDisposalMethod(DisposalMethod.DO_NOT_DISPOSE));
        frames++;
    }
    public void finish(Core.Check check)throws IOException{
        if(finished)return;if(frames==0)throw new IOException("没有可编码的GIF帧");check.check();encoder.finishEncoding();stream.close();
        if(!loop)try(RandomAccessFile io=new RandomAccessFile(file,"rw")){
            byte[] header=new byte[32];io.readFully(header);
            if(header[13]!=0x21||(header[14]&255)!=0xff||header[15]!=11||!"NETSCAPE2.0".equals(new String(header,16,11,StandardCharsets.US_ASCII)))throw new IOException("GIF循环扩展结构有误");
            long source=32,target=13,size=io.length();byte[] buffer=new byte[65536];
            while(source<size){check.check();io.seek(source);int n=io.read(buffer,0,(int)Math.min(buffer.length,size-source));if(n<0)throw new EOFException();io.seek(target);io.write(buffer,0,n);source+=n;target+=n;}
            io.setLength(size-19);
        }
        finished=true;
    }
    @Override public void close()throws IOException{stream.close();}
}
