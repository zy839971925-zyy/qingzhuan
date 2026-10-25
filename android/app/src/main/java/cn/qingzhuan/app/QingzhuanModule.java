package cn.qingzhuan.app;
import android.app.*;import android.content.*;import android.net.Uri;import android.os.*;import android.provider.Settings;import android.view.HapticFeedbackConstants;
import com.facebook.react.bridge.*;import com.facebook.react.modules.core.DeviceEventManagerModule;import org.json.*;import java.io.*;import java.util.*;

/** Keeps the original Java engine and provider; React Native only calls this boundary. */
public final class QingzhuanModule extends ReactContextBaseJavaModule implements ActivityEventListener,LifecycleEventListener {
 public static volatile QingzhuanModule current;
 final AppSession session;final ReactApplicationContext react;boolean foreground=true;int observers;File pendingSave;
 final AppSession.Listener observer=s->{if(foreground)send("QingzhuanState",s.toString());};
 QingzhuanModule(ReactApplicationContext c){super(c);react=c;session=AppSession.get(c);c.addActivityEventListener(this);c.addLifecycleEventListener(this);session.attach(observer);current=this;}
 @Override public String getName(){return "Qingzhuan";}
 void send(String event,String json){if(react.hasActiveReactInstance()&&observers>0)react.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(event,json);}
 void fail(Throwable e){send("QingzhuanNotice",MainJson.obj("message",AppSession.message(e)).toString());}
 @ReactMethod public void addListener(String event){observers++;}
 @ReactMethod public void removeListeners(double count){observers=Math.max(0,observers-(int)count);}
 @ReactMethod public void sync(Promise promise){UiThreadUtil.runOnUiThread(()->{Activity a=getCurrentActivity();if(a!=null){drainInbox(a);receive(a.getIntent());}promise.resolve(session.snapshot().toString());});}
 @Override public Map<String,Object> getConstants(){Map<String,Object> values=new HashMap<>();values.put("initialUi",uiJson().toString());return values;}
 JSONObject uiJson(){
  String pref=react.getSharedPreferences("appearance",0).getString("theme","system");boolean systemDark=(react.getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;
  PowerManager power=(PowerManager)react.getSystemService(Context.POWER_SERVICE);boolean reduced=false;try{reduced=Settings.Global.getFloat(react.getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0;}catch(SecurityException ignored){}
  return MainJson.obj("theme",pref,"dark",pref.equals("dark")||(pref.equals("system")&&systemDark),"reduced",reduced,"powerSave",power!=null&&power.isPowerSaveMode());
 }
 @ReactMethod public void getUi(Promise promise){promise.resolve(uiJson().toString());}
 @ReactMethod public void setTheme(String mode){if(!Arrays.asList("system","light","dark").contains(mode))return;react.getSharedPreferences("appearance",0).edit().putString("theme",mode).apply();UiThreadUtil.runOnUiThread(()->{Activity a=getCurrentActivity();if(a instanceof NativeHomeActivity)((NativeHomeActivity)a).applyTheme();});}
 @ReactMethod public void pick(){UiThreadUtil.runOnUiThread(()->{try{Activity a=getCurrentActivity();if(a==null||session.snapshot().optBoolean("busy"))return;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);a.startActivityForResult(i,10);}catch(Exception e){fail(e);}});}
 @ReactMethod public void startTask(String id,String tool,String options,String ids){try{session.convert(id,tool,options,ids);}catch(Exception e){fail(e);send("QingzhuanState",session.snapshot().toString());}}
 @ReactMethod public void cancelTask(String id){session.cancel(id);}
 @ReactMethod public void startupReady(){StartupDiagnostics.ready();}
 @ReactMethod public void removeInput(String id){try{session.removeInput(id);}catch(Exception e){fail(e);}}
 @ReactMethod public void clearInputs(){try{session.clearInputs();}catch(Exception e){fail(e);}}
 @ReactMethod public void deleteAll(){try{session.deleteAll();}catch(Exception e){fail(e);}}
 @ReactMethod public void save(String id){try{if(!session.snapshot().optBoolean("busy"))export(session.output(id));}catch(Exception e){fail(e);}}
 @ReactMethod public void deleteSelected(String ids){try{session.deleteSelected(ids);}catch(Exception e){fail(e);}}
 @ReactMethod public void removeSelected(String ids){try{session.removeSelected(ids);}catch(Exception e){fail(e);}}
 @ReactMethod public void saveSelected(String ids){UiThreadUtil.runOnUiThread(()->{try{if(session.snapshot().optBoolean("busy"))return;session.selectedOutputs(ids);Activity a=getCurrentActivity();if(a==null)return;android.content.SharedPreferences prefs=react.getSharedPreferences("pending-folder",0);if(prefs.contains("ids"))throw new IOException("请先完成当前文件夹选择");prefs.edit().putString("ids",ids).commit();Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);try{a.startActivityForResult(i,12);}catch(Exception e){prefs.edit().remove("ids").apply();throw e;}}catch(Exception e){fail(e);}});}
 @ReactMethod public void saveAll(String ids){try{session.saveAll(ids,this::export);}catch(Exception e){fail(e);}}
 @ReactMethod public void share(String id){useOutput(id,true);}
 @ReactMethod public void open(String id){useOutput(id,false);}
 @ReactMethod public void haptic(){UiThreadUtil.runOnUiThread(()->{Activity a=getCurrentActivity();PowerManager p=(PowerManager)react.getSystemService(Context.POWER_SERVICE);if(a!=null&&foreground&&p!=null&&!p.isPowerSaveMode())a.getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);});}
 @ReactMethod public void licenses(Promise p){try(InputStream in=react.getAssets().open("licenses.txt");ByteArrayOutputStream out=new ByteArrayOutputStream()){Core.copy(in,out,4L*1024*1024,Core.NONE);p.resolve(out.toString("UTF-8"));}catch(Exception e){p.reject("LICENSE",e);}}
 void useOutput(String id,boolean share){UiThreadUtil.runOnUiThread(()->{try{Activity a=getCurrentActivity();if(a==null||session.snapshot().optBoolean("busy"))return;File f=session.output(id);Uri u=Uri.parse("content://"+react.getPackageName()+".files/"+Uri.encode(id,"/"));Intent i=new Intent(share?Intent.ACTION_SEND:Intent.ACTION_VIEW);if(share){i.setType(OutputProvider.mime(f.getName()));i.putExtra(Intent.EXTRA_STREAM,u);}else i.setDataAndType(u,OutputProvider.mime(f.getName()));i.setClipData(ClipData.newRawUri("轻转文件",u));i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);a.startActivity(share?Intent.createChooser(i,"分享文件"):i);}catch(ActivityNotFoundException e){fail(new IOException("没有可打开此格式的应用，请导出或分享"));}catch(Exception e){fail(e);}});}
 void export(File f){UiThreadUtil.runOnUiThread(()->{try{Activity a=getCurrentActivity();if(a==null){session.cleanExport(f);return;}if(pendingSave!=null){session.cleanExport(f);throw new IOException("请先完成当前保存操作");}pendingSave=f;react.getSharedPreferences("pending-export",0).edit().putString("path",f.getPath()).apply();Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(OutputProvider.mime(f.getName()));i.putExtra(Intent.EXTRA_TITLE,f.getName());a.startActivityForResult(i,11);}catch(Exception e){session.cleanExport(f);pendingSave=null;fail(e);}});}
 public void drainInbox(Activity a){try{JSONObject item=new DocumentInbox(new File(react.getFilesDir(),"document-result.json")).take();if(item==null)return;Intent data=new Intent();JSONArray uris=item.optJSONArray("uris");if(uris!=null&&uris.length()>0){Uri first=Uri.parse(uris.getString(0));data.setData(first);ClipData clip=ClipData.newRawUri("轻转文件",first);for(int i=1;i<uris.length();i++)clip.addItem(new ClipData.Item(Uri.parse(uris.getString(i))));data.setClipData(clip);}data.setFlags(item.optInt("flags"));onActivityResult(a,item.getInt("request"),item.getInt("result"),data);}catch(Exception e){fail(e);}}
 @Override public void onActivityResult(Activity a,int request,int result,Intent data){
  if(request==10&&result==Activity.RESULT_OK&&data!=null){List<Uri> uris=new ArrayList<>();if(data.getClipData()!=null)for(int j=0;j<data.getClipData().getItemCount();j++)uris.add(data.getClipData().getItemAt(j).getUri());else if(data.getData()!=null)uris.add(data.getData());try{session.importUris(uris);}catch(Exception e){fail(e);}}
  if(request==12){android.content.SharedPreferences prefs=react.getSharedPreferences("pending-folder",0);String ids=prefs.getString("ids",null);prefs.edit().remove("ids").apply();if(result==Activity.RESULT_OK&&data!=null&&data.getData()!=null&&ids!=null)try{session.saveSelectedToFolder(ids,data.getData());}catch(Exception e){fail(e);}return;}
  if(request==11){File f=pendingSave;pendingSave=null;String restored=react.getSharedPreferences("pending-export",0).getString("path",null);react.getSharedPreferences("pending-export",0).edit().remove("path").apply();if(f==null&&restored!=null)try{File candidate=new File(restored).getCanonicalFile();if(candidate.isFile()&&(candidate.getPath().startsWith(session.resultRoot.getCanonicalPath()+"/")||candidate.getPath().startsWith(react.getCacheDir().getCanonicalPath()+"/轻转结果-")))f=candidate;}catch(IOException ignored){}
   if(f==null){if(result==Activity.RESULT_OK)fail(new IOException("导出临时文件已失效，请从已完成列表重新导出"));return;}if(result!=Activity.RESULT_OK||data==null||data.getData()==null){session.cleanExport(f);return;}try{session.saveTo(f,data.getData());}catch(Exception e){session.cleanExport(f);fail(e);}
  }
 }
 void receive(Intent i){if(i==null)return;List<Uri> a=new ArrayList<>();if(Intent.ACTION_SEND.equals(i.getAction())){Uri u=i.getParcelableExtra(Intent.EXTRA_STREAM);if(u!=null)a.add(u);}else if(Intent.ACTION_SEND_MULTIPLE.equals(i.getAction())){ArrayList<Uri> items=i.getParcelableArrayListExtra(Intent.EXTRA_STREAM);if(items!=null)a.addAll(items);}i.setAction(null);try{session.importUris(a);}catch(Exception e){fail(e);}}
 @Override public void onNewIntent(Intent i){receive(i);}
 @Override public void onHostResume(){foreground=true;session.attach(observer);Activity a=getCurrentActivity();if(a!=null){drainInbox(a);receive(a.getIntent());}}
 @Override public void onHostPause(){foreground=false;}
 @Override public void onHostDestroy(){}
 @Override public void invalidate(){if(current==this)current=null;session.detach(observer);react.removeActivityEventListener(this);react.removeLifecycleEventListener(this);super.invalidate();}
}
