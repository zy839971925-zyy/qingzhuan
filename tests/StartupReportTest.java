package cn.qingzhuan.app;
import java.io.*;
public class StartupReportTest {
 static int n;
 static void yes(boolean value){n++;if(!value)throw new AssertionError("startup assertion "+n);}
 public static void main(String[] args)throws Exception{
  File base=new File("build/startup-report-test");base.mkdirs();StartupReportStore s=new StartupReportStore(base);s.clear();
  yes(!s.pending()&&s.read()==null);s.markPending();yes(new StartupReportStore(base).pending());yes(s.interrupted(false));yes(!s.shouldBlock(false));yes(!s.interrupted(true));
  s.write("ReactHost",new IllegalStateException("真实错误：玻璃初始化"),"device-test");String report=s.read();yes(report.contains("真实错误：玻璃初始化"));yes(report.contains("IllegalStateException")&&report.contains("ReactHost")&&report.contains("device-test"));
  s.clearPending();yes(!s.pending()&&s.read()!=null);
  s.write("上次启动未完成",new IllegalStateException("上次进程在启动完成前结束，未捕获 Java 异常。此记录不能单独确定是原生崩溃还是进程被结束。"),"版本：1.3.0-monochrome (6)\n上次进程退出：reason=10, status=0, [FORCE STOP] SwipeUpClean");yes(!s.shouldBlock(false));yes(s.read()!=null);
  s.write("ReactHost",new IllegalStateException("真实错误，诊断文本包含上次启动未完成"),"test");yes(s.shouldBlock(false));s.write("large",new RuntimeException(new String(new char[200000]).replace('\0','x')),"test");yes(s.read().length()<=131072);
  File untouched=new File(base,"business-result.txt");try(FileOutputStream out=new FileOutputStream(untouched)){out.write(7);}s.clear();yes(s.read()==null&&!s.pending()&&untouched.isFile());
  System.out.println("PASS startup diagnostics "+n+" assertions");
 }
}
