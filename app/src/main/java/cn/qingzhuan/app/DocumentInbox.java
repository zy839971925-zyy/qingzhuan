package cn.qingzhuan.app;
import java.io.*;import org.json.*;
/** Host-owned durable inbox: document results survive ReactContext initialization. */
public final class DocumentInbox {
 private final SessionStore store;
 public DocumentInbox(File file){store=new SessionStore(file);}
 public synchronized void put(int request,int result,JSONArray uris,int flags)throws IOException{store.write(MainJson.obj("request",request,"result",result,"uris",uris,"flags",flags));}
 public synchronized JSONObject take()throws IOException{JSONObject data=store.read();if(!data.has("request"))return null;store.write(new JSONObject());return data;}
}
