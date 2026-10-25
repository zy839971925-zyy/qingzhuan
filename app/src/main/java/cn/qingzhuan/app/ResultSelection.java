package cn.qingzhuan.app;
import org.json.*;import java.util.*;import java.io.IOException;
/** Only manifest members may be exported/deleted; validates the entire request first. */
public final class ResultSelection {
 public static JSONArray choose(JSONArray rows,String json)throws IOException{
  try{JSONArray ids=new JSONArray(json),out=new JSONArray();if(ids.length()==0||ids.length()>rows.length())throw new IOException("请选择有效文件");Set<String> seen=new HashSet<>();Map<String,JSONObject> known=new HashMap<>();for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);known.put(row.getString("id"),row);}for(int i=0;i<ids.length();i++){String id=ids.getString(i);if(!seen.add(id)||!known.containsKey(id))throw new IOException("所选文件已变化，请重新选择");out.put(known.get(id));}return out;}catch(JSONException e){throw new IOException("文件选择无效",e);}
 }
 public static JSONArray without(JSONArray rows,JSONArray chosen)throws IOException{try{Set<String> ids=new HashSet<>();for(int i=0;i<chosen.length();i++)ids.add(chosen.getJSONObject(i).getString("id"));JSONArray kept=new JSONArray();for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);if(!ids.contains(row.getString("id")))kept.put(row);}return kept;}catch(JSONException e){throw new IOException(e);}}
}
