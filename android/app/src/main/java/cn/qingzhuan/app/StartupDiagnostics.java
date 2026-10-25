package cn.qingzhuan.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.Log;
import java.io.File;

/** Local diagnostics independent of React Native, Skia and AppCompat initialization. */
public final class StartupDiagnostics {
 private static Application app;
 private static StartupReportStore store;
 private static volatile String phase="Application";
 private static volatile boolean startedThisProcess;
 private static Thread.UncaughtExceptionHandler previous;
 public static synchronized void install(Application context){
  if(app!=null)return;app=context;store=new StartupReportStore(new File(context.getFilesDir(),"startup-diagnostics"));
  // A newly installed diagnostic/fix build must not be trapped by an older report.
  android.content.SharedPreferences p=app.getSharedPreferences("startup-diagnostics",0);
  if(p.getInt("version",-1)!=BuildConfig.VERSION_CODE){store.clear();p.edit().putInt("version",BuildConfig.VERSION_CODE).commit();}
  previous=Thread.getDefaultUncaughtExceptionHandler();
  Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
   capture(phase+" / "+thread.getName(),error);
   if(previous!=null)previous.uncaughtException(thread,error);else{android.os.Process.killProcess(android.os.Process.myPid());System.exit(10);}
  });
 }
 public static void phase(String name){phase=name;}
 static String metadata(){
  String text="版本："+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")\n设备："+Build.MANUFACTURER+" "+Build.MODEL+"\nAndroid："+Build.VERSION.RELEASE+" / API "+Build.VERSION.SDK_INT+"\nABI："+java.util.Arrays.toString(Build.SUPPORTED_ABIS);
  try{text+="\n内存页大小："+android.system.Os.sysconf(android.system.OsConstants._SC_PAGESIZE);}catch(Exception ignored){}
  if(Build.VERSION.SDK_INT>=30)try{
   ActivityManager manager=(ActivityManager)app.getSystemService(Context.ACTIVITY_SERVICE);
   java.util.List<ApplicationExitInfo> exits=manager.getHistoricalProcessExitReasons(app.getPackageName(),0,1);
   if(!exits.isEmpty()){ApplicationExitInfo e=exits.get(0);text+="\n上次进程退出：reason="+e.getReason()+", status="+e.getStatus()+", "+e.getDescription();if(e.getReason()==ApplicationExitInfo.REASON_CRASH_NATIVE||e.getReason()==ApplicationExitInfo.REASON_ANR){try(java.io.InputStream in=e.getTraceInputStream()){if(in!=null){byte[] bytes=new byte[49152];int count=in.read(bytes);if(count>0)text+="\n上次退出追踪：\n"+new String(bytes,0,count,java.nio.charset.StandardCharsets.UTF_8);}}}}
  }catch(Exception ignored){}
  return text;
 }
 public static void capture(String at,Throwable error){try{store.write(at,error,metadata());}catch(Throwable secondary){Log.e("QingzhuanStartup","Cannot save startup report",secondary);}}
 public static boolean hasPreviousFailure(){
  try{if(store.shouldBlock(startedThisProcess))return true;if(store.interrupted(startedThisProcess)){Log.w("QingzhuanStartup","Previous startup interrupted without captured exception; allow retry");store.clearPending();}}catch(Exception e){Log.e("QingzhuanStartup","Cannot read report",e);}
  return false;
 }
 public static void clearForRetry(){store.clear();}
 public static void begin(){startedThisProcess=true;try{store.markPending();}catch(Exception e){Log.e("QingzhuanStartup","Cannot mark startup",e);}}
 public static void ready(){new Handler(Looper.getMainLooper()).post(()->{try{if(!store.shouldBlock(true))store.clearPending();}catch(Exception ignored){}});phase("首页已挂载");}
 public static String report(){try{String s=store.read();return s==null?metadata()+"\n未取得 Java 错误报告。":s;}catch(Exception e){return "错误报告读取失败："+e;}}
 public static void hostFailure(Context context,Exception error){
  capture("ReactHost / JS 初始化",error);
  new Handler(Looper.getMainLooper()).post(()->{
   try{context.startActivity(new Intent(context,StartupActivity.class).setAction("cn.qingzhuan.STARTUP_ERROR").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));}catch(Exception e){Log.e("QingzhuanStartup","Cannot open native report",e);}
  });
 }
}
