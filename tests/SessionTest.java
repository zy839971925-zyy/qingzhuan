package cn.qingzhuan.app;
import java.io.*;import java.util.*;import org.json.*;
public class SessionTest {
 static int n;static void yes(boolean x,String s){n++;if(!x)throw new AssertionError(s);}
 static File file(File root,String name,byte[] data)throws Exception{File f=new File(root,name);try(FileOutputStream o=new FileOutputStream(f)){o.write(data);}return f;}
 public static void main(String[] args)throws Exception{
  File dir=new File("build/session-tests");dir.mkdirs();
  File fake=file(dir,"photo.png","%PDF-1.7\n".getBytes("UTF-8"));FileProbe.Result p=FileProbe.inspect(fake);yes(p.kind.equals("pdf"),"PDF content wins over image extension");yes(p.tools.contains("pdfimage")&&!p.tools.contains("image"),"fake extension cannot advertise image conversion");
  File unknown=file(dir,"without-extension","hello 中文".getBytes("UTF-8"));yes(FileProbe.inspect(unknown).tools.contains("document"),"unknown extension text is recognized");
  File binary=file(dir,"lie.csv",new byte[]{0,1,2,3,0});yes(!FileProbe.inspect(binary).tools.contains("table"),"binary CSV cannot advertise table");
  File png=new File("tests/fixtures/image.png");yes(FileProbe.inspect(png).kind.equals("image"),"PNG signature recognized");
  File zip=new File(dir,"pretend.docx");try(java.util.zip.ZipOutputStream o=new java.util.zip.ZipOutputStream(new FileOutputStream(zip))){Core.zipEntry(o,"hello.txt","not a docx");}yes(FileProbe.inspect(zip).kind.equals("archive"),"ZIP entry inspection rejects fake DOCX");
  File rar5=file(dir,"new.rar",new byte[]{82,97,114,33,26,7,1,0});yes(!FileProbe.inspect(rar5).tools.contains("unpack"),"unsupported RAR5 not offered");
  SessionStore store=new SessionStore(new File(dir,"session.json"));store.write(new JSONObject().put("inputs",new JSONArray().put("retained")));yes(store.read().getJSONArray("inputs").getString(0).equals("retained"),"durable checkpoint readback");
  store.write(new JSONObject().put("inputs",new JSONArray().put("second")));yes(store.read().getJSONArray("inputs").getString(0).equals("second"),"atomic checkpoint replace");
  TaskLedger l=new TaskLedger();yes(l.begin("one","convert"),"first task starts");yes(!l.begin("two","convert"),"concurrent submission rejected");l.progress("one","processing",-1);JSONObject running=l.snapshot();yes(!running.has("percent")&&running.getInt("value")==-1,"unknown progress stays indeterminate");long seq=running.getLong("seq");l.requestCancel("one");yes(l.snapshot().getString("status").equals("cancelling"),"cancel request is not terminal ACK");l.finish("one","cancelled","已取消",new JSONArray());yes(l.snapshot().getLong("seq")>seq,"task sequence increases");yes(!l.begin("one","convert"),"completed task ID cannot be replayed");yes(l.begin("two","convert"),"fresh ID starts");l.progress("one","stale",99);yes(!l.snapshot().getString("message").equals("stale"),"stale native progress ignored");
  TaskLedger restored=new TaskLedger(l.checkpoint());yes(restored.snapshot().getString("status").equals("interrupted"),"process death recovers interrupted, never fake running");yes(!restored.begin("two","convert"),"dedupe IDs survive restore");
  TaskLedger committed=new TaskLedger();committed.begin("committed","convert");committed.finish("committed","complete","done",new JSONArray().put("out"));yes(!committed.requestCancel("committed"),"late cancellation cannot revoke committed result");yes(new TaskLedger(committed.checkpoint()).snapshot().getString("status").equals("complete"),"committed task survives process death");
  File cache=new File(dir,"cache");cache.mkdirs();File pending=file(cache,"轻转结果-pending.zip",new byte[]{1});yes(!RecoveryPolicy.cleanCache(cache,pending,pending.getCanonicalPath()),"pending ZIP survives process recovery");yes(RecoveryPolicy.cleanCache(cache,pending,""),"unreferenced export cache can be cleaned");
  DocumentInbox inbox=new DocumentInbox(new File(dir,"inbox.json"));inbox.put(11,-1,new JSONArray().put("content://documents/new-target"),3);JSONObject recovered=new DocumentInbox(new File(dir,"inbox.json")).take();yes(recovered.getJSONArray("uris").getString(0).equals("content://documents/new-target"),"picker result survives cold React runtime");yes(inbox.take()==null,"document result consumed at most once");
  System.out.println("PASS session/probe "+n+" assertions");
 }
}
