package cn.qingzhuan.app;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;

/** Reads declared embedded video ranges only. Does not scan arbitrary bytes for a fake video. */
public final class MotionPhoto {
    public static final class Segment {
        public final long offset,length;
        Segment(long offset,long length){this.offset=offset;this.length=length;}
    }
    public static Segment find(File file)throws IOException {
        long size=file.length();if(size<32)return null;
        try(RandomAccessFile in=new RandomAccessFile(file,"r")){
            int first=in.readUnsignedShort();in.seek(4);int box=in.readInt();
            if(first!=0xffd8&&box!=0x66747970)return null;
            in.seek(0);
            byte[] head=new byte[(int)Math.min(size,2L*1024*1024)];in.readFully(head);
            String xmp=new String(head,StandardCharsets.ISO_8859_1);
            Matcher old=Pattern.compile("(?:MicroVideoOffset|MotionPhotoVideoOffset)\\s*=\\s*[\"']([0-9]{1,12})[\"']").matcher(xmp);
            while(old.find()){long length=Long.parseLong(old.group(1));Segment s=valid(in,size,size-length,length);if(s!=null)return s;}
            Matcher item=Pattern.compile("<[^<>]{0,4096}Semantic\\s*=\\s*[\"']MotionPhoto[\"'][^<>]{0,4096}>").matcher(xmp);
            while(item.find()){
                Matcher length=Pattern.compile("(?:[A-Za-z0-9_]+:)?Length\\s*=\\s*[\"']([0-9]{1,12})[\"']").matcher(item.group());
                if(length.find()){long n=Long.parseLong(length.group(1));Segment s=valid(in,size,size-n,n);if(s!=null)return s;}
            }
            // ISO-BMFF HEIC/AVIF: a top-level mpvd carries the movie bytes.
            long offset=0;int count=0;
            while(offset+8<=size&&count++<10000){
                in.seek(offset);long n=Integer.toUnsignedLong(in.readInt());int type=in.readInt();long header=8;
                if(n==1){if(offset+16>size)break;n=in.readLong();header=16;}
                if(n==0)n=size-offset;
                if(n<header||n>size-offset)break;
                if(type==0x6d707664){Segment s=valid(in,size,offset+header,n-header);if(s!=null)return s;}
                offset+=n;
            }
        }return null;
    }
    private static Segment valid(RandomAccessFile in,long size,long offset,long length)throws IOException{
        if(offset<=0||length<16||offset>size||length>size-offset)return null;
        in.seek(offset);long first=Integer.toUnsignedLong(in.readInt());int type=in.readInt();
        return type==0x66747970&&first>=8&&first<=length?new Segment(offset,length):null;
    }
    public static File extract(File source,File scratch,Core.Check check)throws IOException{
        Segment s=find(source);if(s==null)return source;
        scratch.mkdirs();File target=File.createTempFile("motion-",".mp4",scratch);boolean ok=false;
        try(RandomAccessFile in=new RandomAccessFile(source,"r");OutputStream out=new FileOutputStream(target)){
            in.seek(s.offset);long left=s.length;byte[] buffer=new byte[65536];
            while(left>0){check.check();int n=in.read(buffer,0,(int)Math.min(left,buffer.length));if(n<0)throw new EOFException("动态照片的视频不完整");out.write(buffer,0,n);left-=n;}
            ok=true;return target;
        }finally{if(!ok)target.delete();}
    }
}
