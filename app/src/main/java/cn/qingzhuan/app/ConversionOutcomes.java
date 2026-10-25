package cn.qingzhuan.app;
import org.json.*;import java.util.*;
/** UI acknowledgement outlives the engine task. Other operations must not erase it. */
public final class ConversionOutcomes{
 private JSONObject latest;
 public synchronized void observe(JSONObject task){if(task!=null&&"convert".equals(task.optString("kind"))&&Arrays.asList("complete","error","cancelled","interrupted").contains(task.optString("status")))try{latest=new JSONObject(task.toString());}catch(JSONException e){throw new IllegalStateException(e);}}
 public synchronized JSONObject latest(){try{return latest==null?null:new JSONObject(latest.toString());}catch(JSONException e){throw new IllegalStateException(e);}}
}
