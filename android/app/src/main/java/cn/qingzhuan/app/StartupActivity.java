package cn.qingzhuan.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.widget.*;

/** Plain framework Activity: can show an error even when RN/Skia cannot initialize. */
public final class StartupActivity extends Activity {
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  if(StartupDiagnostics.hasPreviousFailure()||"cn.qingzhuan.STARTUP_ERROR".equals(getIntent().getAction())){showReport();return;}
  StartupDiagnostics.phase("NativeHomeActivity / React 初始化");
  try{startActivity(new Intent(getIntent()).setClass(this,NativeHomeActivity.class));finish();}
  catch(Exception error){StartupDiagnostics.capture("启动 React Activity",error);showReport();}
 }
 private void showReport(){
  final String report=StartupDiagnostics.report();
  boolean dark=(getResources().getConfiguration().uiMode&48)==32;int bg=dark?Color.rgb(23,27,25):Color.rgb(247,248,243),fg=dark?Color.WHITE:Color.rgb(41,53,47);
  getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
  LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(20),dp(28),dp(20),dp(20));page.setBackgroundColor(bg);
  TextView title=new TextView(this);title.setText("轻转未能完成启动");title.setTextSize(22);title.setTextColor(fg);page.addView(title);
  TextView note=new TextView(this);note.setText("这是本机错误报告，不代表问题已修复。请复制报告发给开发者定位。原文件和转换结果不会被此页面清理，报告不会自动上传。");note.setTextSize(14);note.setTextColor(fg);note.setPadding(0,dp(12),0,dp(12));page.addView(note);
  Button copy=new Button(this);copy.setText("复制错误报告");copy.setOnClickListener(v->{((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("轻转启动错误",report));Toast.makeText(this,"错误报告已复制",Toast.LENGTH_LONG).show();});page.addView(copy);
  ScrollView scroll=new ScrollView(this);TextView text=new TextView(this);text.setText(report);text.setTextColor(fg);text.setTextSize(12);text.setTextIsSelectable(true);scroll.addView(text);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  Button retry=new Button(this);retry.setText("清除诊断并退出，下次打开重试");retry.setOnClickListener(v->{StartupDiagnostics.clearForRetry();finishAffinity();new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->android.os.Process.killProcess(android.os.Process.myPid()),200);});page.addView(retry);
  Button close=new Button(this);close.setText("关闭");close.setOnClickListener(v->finishAffinity());page.addView(close);setContentView(page);
 }
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
