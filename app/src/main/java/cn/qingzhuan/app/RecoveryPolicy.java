package cn.qingzhuan.app;
import java.io.*;
public final class RecoveryPolicy {
 public static boolean cleanCache(File root,File candidate,String pending)throws IOException{
  String path=candidate.getCanonicalPath(),base=root.getCanonicalPath()+File.separator;
  if(!path.startsWith(base))return false;
  if(path.equals(pending))return false;
  return candidate.getName().startsWith("pdf-")||candidate.getName().startsWith("轻转结果-");
 }
}
