package cn.qingzhuan.app;
import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import org.json.JSONObject;
public final class NativeMediaSmoke {
 static int checks;static void require(boolean v,String note){if(!v)throw new AssertionError(note);checks++;}
 public static void main(String[] args)throws Exception{
  File out=new File(args[0]);out.mkdirs();
  for(boolean loop:new boolean[]{false,true}){
   File gif=new File(out,loop?"loop.gif":"once.gif");
   try(GifWriter writer=new GifWriter(gif,32,24,12,85,loop)){
    int[] rgb=new int[32*24];for(int f=0;f<3;f++){for(int y=0;y<24;y++)for(int x=0;x<32;x++)rgb[y*32+x]=((x*8)&255)<<16|((y*10)&255)<<8|((x+y+f*20)*4&255);writer.addFrame(rgb);}writer.finish(Core.NONE);
   }
   byte[] bytes=Files.readAllBytes(gif.toPath());require(new String(bytes,0,6,StandardCharsets.US_ASCII).equals("GIF89a"),"GIF89a header");require((bytes[bytes.length-1]&255)==0x3b,"complete trailer");require(new String(bytes,StandardCharsets.ISO_8859_1).contains("NETSCAPE2.0")==loop,"loop toggle encoded");
  }
  byte[] movie={0,0,0,16,102,116,121,112,105,115,111,109,0,0,0,0};
  File jpeg=new File(out,"motion.jpg");try(OutputStream s=new FileOutputStream(jpeg)){s.write(new byte[]{(byte)255,(byte)216});s.write("<x GCamera:MicroVideoOffset=\"16\"/>".getBytes(StandardCharsets.US_ASCII));s.write(new byte[]{(byte)255,(byte)217});s.write(movie);}
  MotionPhoto.Segment segment=MotionPhoto.find(jpeg);require(segment!=null&&segment.length==16&&segment.offset==jpeg.length()-16,"declared JPEG motion range");
  File invalid=new File(out,"invalid.jpg");Files.write(invalid.toPath(),new byte[]{(byte)255,(byte)216,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});require(MotionPhoto.find(invalid)==null,"static photo is not a video");
  File heic=new File(out,"motion.heic");try(DataOutputStream s=new DataOutputStream(new FileOutputStream(heic))){s.write(movie);s.writeInt(24);s.writeInt(0x6d707664);s.write(movie);}segment=MotionPhoto.find(heic);require(segment!=null&&segment.offset==24&&segment.length==16,"HEIC mpvd range");
  MediaOptions options=new MediaOptions(new JSONObject().put("clipStart","1").put("clipEnd","3"),4000,true);require(options.startMs==1000&&options.endMs==3000,"clip seconds to milliseconds");
  try{new MediaOptions(new JSONObject().put("clipStart","3").put("clipEnd","1"),4000,true);throw new AssertionError("reverse clip accepted");}catch(IOException expected){checks++;}
  try{new MediaOptions(new JSONObject().put("gifFps","0"),4000,true);throw new AssertionError("invalid fps accepted");}catch(IOException expected){checks++;}
  System.out.println("PASS "+checks+" native media writer/range/option assertions; Android decoder/encoder not executed.");
 }
}
