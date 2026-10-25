package cn.qingzhuan.app;
import java.util.*;import org.json.*;
/** Host-authoritative task state and monotonic callback sequence; visual p never enters here. */
public final class TaskLedger {
    private String id="",kind="",status="idle",message="";private long seq=0;private int value=-1;
    private JSONArray files=new JSONArray();private final LinkedHashSet<String> seen=new LinkedHashSet<>();
    public TaskLedger(){}
    public TaskLedger(JSONObject saved){
        JSONObject t=saved.optJSONObject("task");if(t==null)t=new JSONObject();id=t.optString("id");kind=t.optString("kind");seq=t.optLong("seq");status=t.optString("status","idle");message=t.optString("message");value=t.optInt("value",-1);JSONArray a=t.optJSONArray("files");if(a!=null)files=a;
        a=saved.optJSONArray("seen");if(a!=null)for(int i=0;i<a.length();i++)seen.add(a.optString(i));if(!id.isEmpty())seen.add(id);
        if(active()){status="interrupted";message="上次任务因应用进程结束而中断，请重新转换";value=-1;seq++;}
    }
    public synchronized boolean active(){return status.equals("running")||status.equals("cancelling");}
    public synchronized boolean begin(String newId,String operation){
        if(active()||newId==null||!newId.matches("[A-Za-z0-9_-]{1,100}")||seen.contains(newId))return false;
        id=newId;kind=operation;status="running";message="准备处理";value=-1;files=new JSONArray();seq++;seen.add(id);while(seen.size()>128)seen.remove(seen.iterator().next());return true;
    }
    public synchronized void progress(String task,String text,int v){if(!id.equals(task)||!active())return;if(!status.equals("cancelling"))message=text;value=Math.max(-1,Math.min(100,v));seq++;}
    public synchronized boolean requestCancel(String task){if(!id.equals(task)||!active())return false;status="cancelling";message="正在取消，等待引擎清理";seq++;return true;}
    public synchronized void finish(String task,String result,String text,JSONArray outputs){if(!id.equals(task)||!active())return;status=result;message=text;value=result.equals("complete")?100:-1;files=outputs==null?new JSONArray():outputs;seq++;}
    public synchronized void failCommit(String task,String reason){if(id.equals(task)){status="error";message=reason;value=-1;files=new JSONArray();seq++;}}
    public synchronized JSONObject snapshot(){return MainJson.obj("id",id,"seq",seq,"kind",kind,"status",status,"message",message,"value",value,"files",files);}
    public synchronized JSONObject checkpoint(){return MainJson.obj("task",snapshot(),"seen",new JSONArray(seen));}
}
final class MainJson {
    static JSONObject obj(Object... pairs){JSONObject o=new JSONObject();try{for(int i=0;i<pairs.length;i+=2)o.put((String)pairs[i],pairs[i+1]);}catch(JSONException e){throw new IllegalArgumentException(e);}return o;}
}
