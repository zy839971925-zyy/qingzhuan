package cn.qingzhuan.app;
import java.io.*;import java.nio.file.*;import org.json.*;
/** Small atomic durable checkpoints; no passwords or original content in metadata. */
public final class SessionStore {
    final File file;
    public SessionStore(File file){this.file=file;}
    public synchronized JSONObject read()throws IOException{
        if(!file.isFile())return new JSONObject();
        try{return new JSONObject(new String(Core.bytes(file,8L*1024*1024),"UTF-8"));}catch(JSONException e){throw new IOException("会话记录损坏；文件仍保留在应用内",e);}
    }
    public synchronized void write(JSONObject data)throws IOException{
        File parent=file.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("无法创建会话目录");
        File tmp=new File(parent,file.getName()+".tmp");
        try{
            try(FileOutputStream out=new FileOutputStream(tmp)){out.write(data.toString().getBytes("UTF-8"));out.getFD().sync();}
            try{Files.move(tmp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){Files.move(tmp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);}
        }finally{tmp.delete();}
    }
}
