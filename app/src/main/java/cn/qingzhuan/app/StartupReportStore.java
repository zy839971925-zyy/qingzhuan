package cn.qingzhuan.app;

import java.io.*;
import java.nio.charset.StandardCharsets;

/** Bounded startup-only report; never reads or alters conversion inputs/results. */
public final class StartupReportStore {
 private static final int LIMIT=131072;
 private final File root,report,pending;
 public StartupReportStore(File root){this.root=root;report=new File(root,"startup-error.txt");pending=new File(root,"startup-pending");}
 private void save(File target,String text)throws IOException{
  if(!root.isDirectory()&&!root.mkdirs())throw new IOException("Cannot create diagnostics directory");
  File tmp=new File(root,target.getName()+".tmp");
  try(FileOutputStream out=new FileOutputStream(tmp)){out.write(text.getBytes(StandardCharsets.UTF_8));out.getFD().sync();}
  if(!tmp.renameTo(target))throw new IOException("Cannot commit startup report");
 }
 public synchronized void markPending()throws IOException{save(pending,Long.toString(System.currentTimeMillis()));}
 public synchronized boolean pending(){return pending.isFile();}
 public synchronized boolean shouldBlock(boolean startedThisProcess)throws IOException{String saved=read();if(saved==null)return false;
  // 1.3 created this synthetic exception from a pending marker, including normal SwipeUpClean.
  // Keep the report for diagnosis but never treat absence of a captured exception as a crash gate.
  return !(saved.contains("\n阶段：上次启动未完成\n")&&saved.contains("\njava.lang.IllegalStateException: 上次进程在启动完成前结束，未捕获 Java 异常")&&!saved.contains("\nCaused by:"));}
 public synchronized boolean interrupted(boolean startedThisProcess){return !startedThisProcess&&pending();}
 public synchronized void clearPending(){pending.delete();}
 public synchronized String read()throws IOException{
  if(!report.isFile())return null;
  try(Reader in=new InputStreamReader(new FileInputStream(report),StandardCharsets.UTF_8)){StringBuilder s=new StringBuilder();char[] b=new char[2048];int n;while(s.length()<LIMIT&&(n=in.read(b,0,Math.min(b.length,LIMIT-s.length())))!=-1)s.append(b,0,n);return s.toString();}
 }
 public synchronized void write(String phase,Throwable error,String metadata)throws IOException{
  StringWriter detail=new StringWriter();if(error!=null)error.printStackTrace(new PrintWriter(detail));
  String text="轻转启动诊断\n"+metadata+"\n阶段："+phase+"\n时间："+new java.util.Date()+"\n\n"+detail;
  if(text.length()>LIMIT)text=text.substring(0,LIMIT-32)+"\n[报告已截断]";
  save(report,text);
 }
 public synchronized void clear(){report.delete();pending.delete();new File(root,"startup-error.txt.tmp").delete();new File(root,"startup-pending.tmp").delete();}
}
