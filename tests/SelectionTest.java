package cn.qingzhuan.app;
import org.json.*;import java.io.*;
public class SelectionTest {
 static int n;static void yes(boolean b){n++;if(!b)throw new AssertionError("selection "+n);}
 static void bad(JSONArray rows,String ids)throws Exception{try{ResultSelection.choose(rows,ids);throw new AssertionError("must reject "+ids);}catch(IOException expected){n++;}}
 public static void main(String[] a)throws Exception{JSONArray rows=new JSONArray("[{\"id\":\"a/out.png\",\"name\":\"a.png\"},{\"id\":\"b/out.png\",\"name\":\"b.png\"}]");JSONArray chosen=ResultSelection.choose(rows,"[\"b/out.png\"]");yes(chosen.length()==1&&chosen.getJSONObject(0).getString("name").equals("b.png"));JSONArray kept=ResultSelection.without(rows,chosen);yes(kept.length()==1&&kept.getJSONObject(0).getString("id").equals("a/out.png"));yes(rows.length()==2);bad(rows,"[]");bad(rows,"[\"a/out.png\",\"a/out.png\"]");bad(rows,"[\"../session-v2.json\"]");bad(rows,"[\"missing\"]");System.out.println("PASS selection "+n+" assertions");}
}
