package cn.qingzhuan.app;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.webkit.*;
import android.widget.FrameLayout;
import android.graphics.Color;
import org.json.*;
import java.io.*;
import java.util.*;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

/** Thin WebView/system UI host. All file work belongs to the application session. */
public class MainActivity extends Activity {
    WebView web;FrameLayout root;AppSession session;boolean ready,foreground,receiverRegistered,destroyed;
    File pendingSave;Intent shared;
    final AppSession.Listener observer=s->runOnUiThread(()->{if(foreground&&ready&&!destroyed)dispatch("state",s);});
    final BroadcastReceiver powerReceiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){lifecycle();}};
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);PDFBoxResourceLoader.init(getApplicationContext());session=AppSession.get(this);
        root=new FrameLayout(this);web=new WebView(this);root.addView(web,new FrameLayout.LayoutParams(-1,-1));setContentView(root);
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(true);s.setAllowContentAccess(false);s.setAllowFileAccessFromFileURLs(false);s.setAllowUniversalAccessFromFileURLs(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setSupportZoom(false);s.setTextZoom(100);if(Build.VERSION.SDK_INT>=29)s.setForceDark(WebSettings.FORCE_DARK_OFF);if(Build.VERSION.SDK_INT>=33)s.setAlgorithmicDarkeningAllowed(false);
        web.addJavascriptInterface(new Bridge(),"Native");web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return true;}
            @Override public void onPageFinished(WebView v,String url){if(destroyed||!url.equals("file:///android_asset/index.html"))return;ready=true;theme();lifecycle();session.attach(observer);if(shared!=null){Intent i=shared;shared=null;receive(i);}}
            @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail detail){if(Build.VERSION.SDK_INT>=26){recreate();return true;}return false;}
        });
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());android.graphics.Insets ime=insets.getInsets(WindowInsets.Type.ime());v.setPadding(i.left,i.top,i.right,Math.max(i.bottom,ime.bottom));}
            else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });root.requestApplyInsets();theme();
        if(saved!=null){String path=saved.getString("pendingSave");if(path!=null)try{File f=new File(path).getCanonicalFile();String p=f.getPath();if(f.isFile()&&(p.startsWith(getFilesDir().getCanonicalPath()+"/results/")||p.startsWith(getCacheDir().getCanonicalPath()+"/轻转结果-")))pendingSave=f;}catch(IOException ignored){}}
        if(saved==null&&(Intent.ACTION_SEND.equals(getIntent().getAction())||Intent.ACTION_SEND_MULTIPLE.equals(getIntent().getAction())))shared=getIntent();
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->back());
        web.loadUrl("file:///android_asset/index.html");
    }
    boolean dark(){String p=getSharedPreferences("appearance",0).getString("theme","system");return p.equals("dark")||(p.equals("system")&&(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);}
    void theme(){
        boolean d=dark();int color=Color.parseColor(d?"#101821":"#F5F4EF");getWindow().getDecorView().setBackgroundColor(color);if(root!=null)root.setBackgroundColor(color);if(web!=null)web.setBackgroundColor(color);
        int flags=View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;if(!d)flags|=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;getWindow().getDecorView().setSystemUiVisibility(flags);
        getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(Color.TRANSPARENT);if(Build.VERSION.SDK_INT>=29)getWindow().setNavigationBarContrastEnforced(false);
        if(ready)web.evaluateJavascript("window.setNativeTheme&&window.setNativeTheme("+d+")",null);
    }
    @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);theme();lifecycle();}
    void dispatch(String type,JSONObject data){if(ready&&web!=null&&!destroyed)web.evaluateJavascript("window.onNative&&window.onNative("+JSONObject.quote(type)+","+data+")",null);}
    void failure(Throwable t){runOnUiThread(()->dispatch("toast",MainJson.obj("message",AppSession.message(t))));}
    void lifecycle(){if(!ready||destroyed)return;PowerManager p=(PowerManager)getSystemService(POWER_SERVICE);boolean saver=p!=null&&p.isPowerSaveMode(),reduced=false;try{reduced=Settings.Global.getFloat(getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0;}catch(SecurityException ignored){}web.evaluateJavascript("window.setAppActive&&window.setAppActive("+foreground+","+saver+","+reduced+")",null);}
    @Override protected void onStart(){super.onStart();if(!receiverRegistered){IntentFilter f=new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);if(Build.VERSION.SDK_INT>=33)registerReceiver(powerReceiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(powerReceiver,f);receiverRegistered=true;}}
    @Override protected void onResume(){super.onResume();foreground=true;if(web!=null)web.onResume();theme();lifecycle();if(ready)session.attach(observer);}
    @Override protected void onPause(){foreground=false;lifecycle();if(web!=null)web.onPause();super.onPause();}
    @Override protected void onStop(){if(receiverRegistered){unregisterReceiver(powerReceiver);receiverRegistered=false;}super.onStop();}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);if(ready)receive(i);else shared=i;}
    void receive(Intent i){ArrayList<Uri> uris=new ArrayList<>();if(Intent.ACTION_SEND.equals(i.getAction())){Uri u=i.getParcelableExtra(Intent.EXTRA_STREAM);if(u!=null)uris.add(u);}else if(Intent.ACTION_SEND_MULTIPLE.equals(i.getAction())){ArrayList<Uri> a=i.getParcelableArrayListExtra(Intent.EXTRA_STREAM);if(a!=null)uris.addAll(a);}try{session.importUris(uris);}catch(Exception e){failure(e);}}
    @Override public void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==11){File f=pendingSave;pendingSave=null;if(f==null)return;if(result!=RESULT_OK||data==null||data.getData()==null){session.cleanExport(f);return;}try{session.saveTo(f,data.getData());}catch(Exception e){session.cleanExport(f);failure(e);}return;}
        if(request==10&&result==RESULT_OK&&data!=null){ArrayList<Uri> a=new ArrayList<>();if(data.getClipData()!=null)for(int j=0;j<data.getClipData().getItemCount();j++)a.add(data.getClipData().getItemAt(j).getUri());else if(data.getData()!=null)a.add(data.getData());try{session.importUris(a);}catch(Exception e){failure(e);}}
    }
    void export(File f){runOnUiThread(()->{if(destroyed){session.cleanExport(f);return;}if(pendingSave!=null){session.cleanExport(f);failure(new IOException("请先完成当前保存操作"));return;}try{pendingSave=f;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(OutputProvider.mime(f.getName()));i.putExtra(Intent.EXTRA_TITLE,f.getName());startActivityForResult(i,11);}catch(Exception e){pendingSave=null;session.cleanExport(f);failure(e);}});}
    final class Bridge {
        @JavascriptInterface public String getTheme(){return dark()?"dark":"light";}
        @JavascriptInterface public void setTheme(String value){if(!Arrays.asList("system","light","dark").contains(value))return;getSharedPreferences("appearance",0).edit().putString("theme",value).apply();runOnUiThread(()->theme());}
        @JavascriptInterface public void sync(){runOnUiThread(()->{theme();lifecycle();dispatch("state",session.snapshot());});}
        @JavascriptInterface public void pick(){runOnUiThread(()->{if(session.snapshot().optBoolean("busy"))return;try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);startActivityForResult(i,10);}catch(Exception e){failure(e);}});}
        @JavascriptInterface public void haptic(){runOnUiThread(()->{PowerManager p=(PowerManager)getSystemService(POWER_SERVICE);if(foreground&&p!=null&&!p.isPowerSaveMode())web.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);});}
        @JavascriptInterface public void startTask(String id,String tool,String opts,String ids){try{session.convert(id,tool,opts,ids);}catch(Exception e){failure(e);sync();}}
        @JavascriptInterface public void start(String tool,String opts,String ids){startTask(UUID.randomUUID().toString(),tool,opts,ids);}
        @JavascriptInterface public void cancelTask(String id){session.cancel(id);}
        @JavascriptInterface public void cancel(){session.cancel(session.snapshot().optJSONObject("task").optString("id"));}
        @JavascriptInterface public void removeInput(String id){try{session.removeInput(id);}catch(Exception e){failure(e);}}
        @JavascriptInterface public void clearInputs(){try{session.clearInputs();}catch(Exception e){failure(e);}}
        @JavascriptInterface public void save(String id){try{if(!session.snapshot().optBoolean("busy"))export(session.output(id));}catch(Exception e){failure(e);}}
        @JavascriptInterface public void saveAll(String ids){try{session.saveAll(ids,f->export(f));}catch(Exception e){failure(e);}}
        @JavascriptInterface public void deleteAll(){try{session.deleteAll();}catch(Exception e){failure(e);}}
        @JavascriptInterface public void open(String id){useOutput(id,false);}
        @JavascriptInterface public void share(String id){useOutput(id,true);}
        @JavascriptInterface public void demo(){failure(new IOException("请从系统文件选择器添加真实文件"));}
        void useOutput(String id,boolean share){runOnUiThread(()->{if(session.snapshot().optBoolean("busy"))return;try{File f=session.output(id);Uri u=Uri.parse("content://cn.qingzhuan.app.files/"+Uri.encode(id,"/"));Intent i=new Intent(share?Intent.ACTION_SEND:Intent.ACTION_VIEW);if(share){i.setType(OutputProvider.mime(f.getName()));i.putExtra(Intent.EXTRA_STREAM,u);}else i.setDataAndType(u,OutputProvider.mime(f.getName()));i.setClipData(ClipData.newRawUri("轻转文件",u));i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(share?Intent.createChooser(i,"分享文件"):i);}catch(ActivityNotFoundException e){failure(new IOException("没有可打开此格式的应用，可先导出或分享"));}catch(Exception e){failure(e);}});}
    }
    void back(){if(web==null)return;web.evaluateJavascript("window.handleBack&&window.handleBack()",v->{if(!"true".equals(v)){if(session.snapshot().optBoolean("busy"))failure(new IOException("正在处理，可取消或等待任务"));else finish();}});}
    @Override public void onBackPressed(){back();}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);if(pendingSave!=null)b.putString("pendingSave",pendingSave.getPath());}
    @Override protected void onDestroy(){destroyed=true;ready=false;session.detach(observer);if(web!=null){web.removeJavascriptInterface("Native");root.removeView(web);web.destroy();web=null;}super.onDestroy();}
}
