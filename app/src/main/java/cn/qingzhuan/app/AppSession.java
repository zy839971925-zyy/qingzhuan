package cn.qingzhuan.app;

import android.content.*;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.*;
import android.net.Uri;
import android.provider.OpenableColumns;
import org.json.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Process-owned offline session. No Activity/WebView is retained by a worker. */
public final class AppSession {
    interface Listener {void changed(JSONObject snapshot);}
    private static AppSession instance;
    static synchronized AppSession get(Context c){if(instance==null)instance=new AppSession(c.getApplicationContext());return instance;}
    final Context context;final File inputRoot,resultRoot;
    private final SessionStore store;private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final AtomicBoolean cancelled=new AtomicBoolean();private final UiEventGate gate=new UiEventGate(120);
    private final LinkedHashMap<String,File> inputs=new LinkedHashMap<>();private final LinkedHashMap<String,JSONObject> info=new LinkedHashMap<>();
    private JSONArray outputs=new JSONArray();private TaskLedger tasks=new TaskLedger();private final java.util.concurrent.CopyOnWriteArraySet<Listener> listeners=new java.util.concurrent.CopyOnWriteArraySet<>();
    private String storageWarning="";private long revision=0,lastProgressCheckpoint=0;private JSONObject activeRequest=new JSONObject(),lastConversion=new JSONObject();
    private AppSession(Context c){
        context=c;inputRoot=new File(c.getFilesDir(),"inputs");resultRoot=new File(c.getFilesDir(),"results");inputRoot.mkdirs();resultRoot.mkdirs();store=new SessionStore(new File(c.getFilesDir(),"session-v2.json"));
        try{
            JSONObject saved=store.read();activeRequest=saved.optJSONObject("request");if(activeRequest==null)activeRequest=new JSONObject();lastConversion=saved.optJSONObject("lastConversion");if(lastConversion==null)lastConversion=new JSONObject();JSONObject oldTask=saved.optJSONObject("task");
            if(oldTask!=null&&Arrays.asList("running","cancelling").contains(oldTask.optString("status"))){String id=oldTask.optString("id");if(id.matches("[A-Za-z0-9_-]{1,100}"))remove(new File(resultRoot,id));}
            tasks=new TaskLedger(saved);revision=saved.optLong("revision");
            JSONArray history=saved.optJSONArray("outputs");if(history==null){try{history=new JSONArray(c.getSharedPreferences("MainActivity",0).getString("history","[]"));}catch(Exception ignored){history=new JSONArray();}}
            for(int i=0;i<history.length();i++){JSONObject f=history.optJSONObject(i);if(f!=null)try{if(output(f.optString("id")).isFile())outputs.put(f);}catch(IOException ignored){}}
            File oldInputs=new File(c.getCacheDir(),"inputs");File[] old=oldInputs.listFiles();if(old!=null)for(File d:old){File to=new File(inputRoot,d.getName());if(!to.exists())d.renameTo(to);}
            // Partial imports never enter the durable manifest; keep only committed IDs.
            JSONArray a=saved.optJSONArray("inputs");Set<String> keep=new HashSet<>();
            if(a!=null)for(int i=0;i<a.length();i++){JSONObject item=a.optJSONObject(i);if(item==null)continue;String id=item.optString("id");if(!id.matches("[A-Za-z0-9_-]{1,100}"))continue;File f=Core.safe(inputRoot,id+"/"+item.optString("file"));if(f.isFile()){keep.add(id);inputs.put(id,f);info.put(id,item);}}
            File[] dirs=inputRoot.listFiles();if(dirs!=null)for(File d:dirs)if(!keep.contains(d.getName())){
                if(saved.length()==0&&d.isDirectory()){File[] fs=d.listFiles();if(fs!=null&&fs.length==1&&fs[0].isFile()){inputs.put(d.getName(),fs[0]);info.put(d.getName(),MainJson.obj("id",d.getName(),"name",fs[0].getName(),"file",fs[0].getName(),"size",fs[0].length(),"kind","unknown","tools",new JSONArray(Arrays.asList("pack","hash"))));continue;}}
                remove(d);
            }
            String pendingExport=c.getSharedPreferences("pending-export",0).getString("path","");
            File[] cache=c.getCacheDir().listFiles();if(cache!=null)for(File f:cache)if(RecoveryPolicy.cleanCache(c.getCacheDir(),f,pendingExport))remove(f);
            checkpoint();
        }catch(Exception e){storageWarning="恢复记录失败："+message(e);}
        // Probe restored inputs off the UI thread, retaining metadata until done.
        worker.execute(()->{try{for(Map.Entry<String,File> e:copyInputs().entrySet()){JSONObject previous; synchronized(this){previous=info.get(e.getKey());}JSONObject data=inspect(e.getKey(),previous.optString("name",e.getValue().getName()),e.getValue());synchronized(this){info.put(e.getKey(),data);}}linkLivePairs();checkpoint();publish();}catch(Exception e){storageWarning=message(e);publish();}});
    }
    synchronized LinkedHashMap<String,File> copyInputs(){return new LinkedHashMap<>(inputs);}
    synchronized JSONObject snapshot(){
        JSONArray a=new JSONArray();for(JSONObject f:info.values())a.put(f);
        JSONObject s=MainJson.obj("protocol",2,"revision",revision,"inputs",a,"outputs",outputs,"busy",tasks.active(),"task",tasks.snapshot(),"warning",storageWarning,"request",activeRequest,"lastConversion",lastConversion,"pendingConversion",pendingConversion());
        try{return new JSONObject(s.toString());}catch(JSONException e){throw new IllegalStateException(e);}
    }
    void attach(Listener l){listeners.add(l);l.changed(snapshot());}void detach(Listener l){listeners.remove(l);}
    void publish(){JSONObject s;synchronized(this){revision++;s=snapshot();}for(Listener l:listeners)try{l.changed(s);}catch(RuntimeException ignored){}}
    synchronized boolean pendingConversion(){
        JSONObject current=tasks.snapshot();String id=activeRequest.optString("id");if(id.isEmpty()||id.equals(context.getSharedPreferences("conversion-presentation",0).getString("ack","")))return false;
        return id.equals(current.optString("id"))&&"convert".equals(current.optString("kind"))&&Arrays.asList("running","cancelling","complete","error","cancelled","interrupted").contains(current.optString("status"))
            ||id.equals(lastConversion.optString("id"))&&Arrays.asList("complete","error","cancelled","interrupted").contains(lastConversion.optString("status"));
    }
    void acknowledgeConversion(String id){context.getSharedPreferences("conversion-presentation",0).edit().putString("ack",id).apply();publish();}
    synchronized void checkpoint()throws IOException{
        JSONObject s=tasks.checkpoint();try{s.put("request",activeRequest).put("lastConversion",lastConversion);JSONArray a=new JSONArray();for(JSONObject f:info.values())a.put(f);s.put("inputs",a).put("outputs",outputs).put("revision",revision);store.write(s);}catch(JSONException e){throw new IOException(e);}
    }
    synchronized String begin(String id,String kind)throws IOException{return begin(id,kind,false);}
    synchronized String begin(String id,String kind,boolean media)throws IOException{
        if(!tasks.begin(id,kind))throw new IOException("任务已提交或另一项操作仍在运行");cancelled.set(false);gate.reset();
        try{checkpoint();ConversionService.start(context,id,media);}catch(Exception e){tasks.finish(id,"error",message(e),null);try{checkpoint();}catch(IOException ignored){}throw new IOException("无法启动后台转换任务："+message(e),e);}publish();return id;
    }
    void progress(String id,String text,int value){long now=android.os.SystemClock.uptimeMillis();if(!gate.allow(value,text,now))return;tasks.progress(id,text,value);if(now-lastProgressCheckpoint>=2000){lastProgressCheckpoint=now;try{checkpoint();}catch(IOException e){storageWarning=message(e);}}publish();}
    void finish(String id,Throwable failure,JSONArray files){
        String status=failure==null?"complete":cancelled.get()?"cancelled":"error";
        tasks.finish(id,status,failure==null?"处理完成":message(failure),files);
        JSONObject terminal=tasks.snapshot();if("convert".equals(terminal.optString("kind"))&&id.equals(terminal.optString("id")))lastConversion=terminal;
        try{checkpoint();}catch(IOException e){storageWarning="结果已保留，但状态保存失败："+message(e);}publish();
    }
    static String message(Throwable e){String s=e.getMessage();if(s==null||s.trim().isEmpty())s=e.getClass().getSimpleName();if(e instanceof OutOfMemoryError)return "手机内存不足，请减少文件或尺寸";if(s.toLowerCase(Locale.ROOT).contains("password"))return "密码错误或文件已加密，请检查密码";return s;}
    synchronized void backgroundUnavailable(String id,String reason){
        JSONObject task=tasks.snapshot();if(!id.equals(task.optString("id"))||!tasks.active())return;
        cancelled.set(true);tasks.finish(id,"error",reason,null);if("convert".equals(task.optString("kind")))lastConversion=tasks.snapshot();
        try{checkpoint();}catch(IOException e){storageWarning=message(e);}publish();
    }
    void check()throws IOException{if(cancelled.get()||Thread.currentThread().isInterrupted())throw new IOException("已取消");}
    synchronized void cancel(String id){if(tasks.requestCancel(id)){cancelled.set(true);publish();try{checkpoint();}catch(IOException e){storageWarning=message(e);}}}
    JSONObject inspect(String id,String display,File f)throws Exception{
        FileProbe.Result r=FileProbe.inspect(f);BitmapFactory.Options b=new BitmapFactory.Options();b.inJustDecodeBounds=true;BitmapFactory.decodeFile(f.getPath(),b);
        if(b.outWidth>0&&b.outHeight>0){r.kind="image";r.tools.clear();r.tools.addAll(Arrays.asList("image","imagepdf","pack","hash"));}
        JSONArray audio=new JSONArray();boolean hasVideo=false;long durationMs=0;MotionPhoto.Segment motion=MotionPhoto.find(f);
        if(r.kind.equals("media")||motion!=null||Arrays.asList("mp4","mov","mkv","webm","avi","m4v").contains(Core.ext(f.getName()))){
            MediaExtractor extractor=new MediaExtractor();try{if(motion!=null){try(FileInputStream stream=new FileInputStream(f)){extractor.setDataSource(stream.getFD(),motion.offset,motion.length);}}else extractor.setDataSource(f.getPath());for(int i=0;i<extractor.getTrackCount();i++){
                MediaFormat format=extractor.getTrackFormat(i);String mime=format.getString(MediaFormat.KEY_MIME);if(format.containsKey(MediaFormat.KEY_DURATION))durationMs=Math.max(durationMs,format.getLong(MediaFormat.KEY_DURATION)/1000);if(mime!=null&&mime.startsWith("video/")){hasVideo=true;r.tools.addAll(Arrays.asList("gif","video","videoframe"));if(motion!=null)r.tools.add("motionvideo");}if(mime!=null&&mime.startsWith("audio/")){
                    if("audio/mp4a-latm".equals(mime))audio.put("m4a");try{if(new MediaCodecList(MediaCodecList.ALL_CODECS).findDecoderForFormat(format)!=null)audio.put("wav");}catch(RuntimeException ignored){}
                }
            }}catch(IOException|RuntimeException ignored){}finally{extractor.release();}
            if(audio.length()>0)r.tools.add("audio");
        }
        // Engine's document/table/archive routing uses a suffix: normalize its private copy only.
        String stored=f.getName();if(!r.extension.isEmpty()&&!stored.toLowerCase(Locale.ROOT).endsWith("."+r.extension)&&!r.kind.equals("media")){
            File renamed=new File(f.getParentFile(),Core.stem(stored)+"."+r.extension);if(!f.equals(renamed)&&f.renameTo(renamed)){f=renamed;stored=f.getName();}
        }
        synchronized(this){inputs.put(id,f);}
        return MainJson.obj("id",id,"name",display,"file",stored,"size",f.length(),"kind",r.kind,"tools",new JSONArray(r.tools),"audioTargets",audio,"hasVideo",hasVideo,"durationMs",durationMs,"motionPhoto",motion!=null,"videoTargets",new JSONArray(hasVideo?NativeMediaEngine.encoders():Collections.emptyList()));
    }
    private synchronized void linkLivePairs()throws JSONException{
        for(JSONObject photo:info.values())if("image".equals(photo.optString("kind")))for(JSONObject video:info.values()){
            if(!video.optBoolean("hasVideo"))continue;
            if(Core.stem(photo.optString("name")).equalsIgnoreCase(Core.stem(video.optString("name")))){
                photo.put("liveVideoId",video.optString("id"));JSONArray tools=photo.optJSONArray("tools");for(String tool:Arrays.asList("gif","video","videoframe","motionvideo"))if(!has(tools,tool))tools.put(tool);
                photo.put("videoTargets",video.optJSONArray("videoTargets"));photo.put("durationMs",video.optLong("durationMs"));break;
            }
        }
    }
    void importUris(List<Uri> uris)throws IOException{
        if(uris.isEmpty())return;String id=begin(UUID.randomUUID().toString(),"import");
        worker.execute(()->{Throwable failure=null;List<String> added=new ArrayList<>();try{
            LinkedHashMap<String,File> previous=copyInputs();if(previous.size()+uris.size()>20)throw new IOException("一次最多二十个文件");long total=0;for(File f:previous.values())total+=f.length();
            for(Uri uri:uris){check();if(!"content".equals(uri.getScheme()))throw new IOException("只接受系统授权的内容文件");String name="文件";try(Cursor cur=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cur!=null&&cur.moveToFirst()&&cur.getString(0)!=null)name=cur.getString(0);}
                String key=UUID.randomUUID().toString();added.add(key);File d=new File(inputRoot,key);if(!d.mkdirs())throw new IOException("无法创建输入缓存");File f=new File(d,Core.clean(name));
                if(inputRoot.getUsableSpace()<Math.min(Core.MAX_INPUT,64L*1024*1024))throw new IOException("可用空间不足，请先导出并清理结果");
                progress(id,"正在导入 "+name,-1);try(InputStream in=context.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(f)){if(in==null)throw new IOException("无法读取文件");total+=Core.copy(in,out,Math.min(Core.MAX_INPUT,Core.MAX_EXPANDED-total),this::check);}
                JSONObject data=inspect(key,name,f);synchronized(this){info.put(key,data);}
            }check();linkLivePairs();checkpoint();
        }catch(Throwable e){failure=e;for(String key:added){synchronized(this){inputs.remove(key);info.remove(key);}remove(new File(inputRoot,key));}}finally{finish(id,failure,null);}});
    }
    void convert(String id,String tool,String options,String selection)throws IOException{
        begin(id,"convert",Arrays.asList("image","imagepdf","pdfimage","audio","gif","video","videoframe","motionvideo").contains(tool));
        try{activeRequest=MainJson.obj("id",id,"tool",tool,"options",new JSONObject(options),"selection",new JSONArray(selection));checkpoint();}catch(JSONException|IOException e){finish(id,e,null);throw new IOException(message(e),e);}
        worker.execute(()->{File dir=new File(resultRoot,id);Throwable failure=null;JSONArray result=new JSONArray();boolean committed=false;try{
            if(treeSize(resultRoot)>2L*1024*1024*1024)throw new IOException("应用内结果超过 2 GiB，请先导出并清理");
            JSONObject opts=new JSONObject(options);JSONArray ids=new JSONArray(selection);if(ids.length()==0||ids.length()>20)throw new IOException("请选择一至二十份文件");
            List<File> fs=new ArrayList<>();Set<String> unique=new HashSet<>();synchronized(this){for(int i=0;i<ids.length();i++){String key=ids.getString(i);if(!unique.add(key))throw new IOException("重复的输入文件");File f=inputs.get(key);JSONObject meta=info.get(key);if(f==null||meta==null||!has(meta.optJSONArray("tools"),tool))throw new IOException("所选文件不支持此工具，请重新选择");if(tool.equals("audio")&&!has(meta.optJSONArray("audioTargets"),opts.optString("target")))throw new IOException("手机没有对应音频解码能力");if(Arrays.asList("gif","video","videoframe","motionvideo").contains(tool)&&meta.has("liveVideoId")){File video=inputs.get(meta.optString("liveVideoId"));if(video==null)throw new IOException("Live Photo的视频已移除，请重新导入照片和视频");f=video;}if(!fs.contains(f))fs.add(f);}}
            validateTarget(tool,opts.optString("target"));if(tool.equals("merge")&&fs.size()<2)throw new IOException("合并需要至少两份 PDF");check();
            Engine engine=new Engine(context,dir,opts,cancelled,(m,v)->progress(id,m,v));engine.run(tool,fs);check();scan(dir,result);if(result.length()==0)throw new IOException("没有生成可导出的文件，压缩包可能为空");
            synchronized(this){check();JSONArray combined=new JSONArray();for(int i=0;i<result.length();i++)combined.put(result.get(i));for(int i=0;i<outputs.length();i++)combined.put(outputs.get(i));JSONArray before=outputs;outputs=combined;tasks.finish(id,"complete","处理完成",result);lastConversion=tasks.snapshot();try{checkpoint();committed=true;}catch(IOException e){outputs=before;tasks.failCommit(id,message(e));throw e;}}
        }catch(Throwable e){failure=e;}finally{if(!committed)remove(dir);finish(id,failure,committed?result:null);}});
    }
    static boolean has(JSONArray a,String s){if(a!=null)for(int i=0;i<a.length();i++)if(s.equals(a.optString(i)))return true;return false;}
    static void validateTarget(String tool,String target)throws IOException{
        String values;switch(tool){case "image":values="jpg,png,webp";break;case "imagepdf":case "merge":case "split":values="pdf";break;case "pdfimage":values="jpg,png";break;case "document":values="txt,html,docx,pdf";break;case "table":values="csv,tsv,json,xlsx";break;case "pack":values="zip,tar.gz";break;case "audio":values="m4a,wav";break;case "gif":values="gif";break;case "video":values="h264,hevc,av1";break;case "videoframe":values="jpg,png";break;case "motionvideo":values="mp4";break;case "hash":values="txt";break;case "unpack":values="";break;default:throw new IOException("不支持的工具");}if(!Arrays.asList(values.split(",",-1)).contains(target))throw new IOException("不支持的输出格式");
    }
    void removeInput(String key)throws IOException{ synchronized(this){if(tasks.active())throw new IOException("请等待当前任务结束");File f=inputs.remove(key);JSONObject m=info.remove(key);try{checkpoint();}catch(IOException e){if(f!=null){inputs.put(key,f);info.put(key,m);}throw e;}if(f!=null)remove(f.getParentFile());}publish();}
    void clearInputs()throws IOException{for(String key:copyInputs().keySet())removeInput(key);}
    synchronized File previewInput(String id)throws IOException{File f=inputs.get(id);if(f==null||!f.isFile())throw new IOException("输入文件不存在");return f;}
    synchronized File output(String id)throws IOException{File f=Core.safe(resultRoot,id);if(!f.isFile())throw new IOException("结果文件不存在，可能已被清理");return f;}
    void scan(File dir,JSONArray result)throws Exception{File[] fs=dir.listFiles();if(fs==null)return;Arrays.sort(fs,Comparator.comparing(File::getName));for(File f:fs){check();if(f.isDirectory())scan(f,result);else{String path=resultRoot.toURI().relativize(f.toURI()).getPath();result.put(MainJson.obj("id",path,"name",f.getName(),"path",path.substring(path.indexOf('/')+1),"size",f.length(),"time",System.currentTimeMillis()));}}}
    void saveTo(File f,Uri uri)throws IOException{
        String id=begin(UUID.randomUUID().toString(),"export");worker.execute(()->{Throwable failure=null;try(OutputStream out=context.getContentResolver().openOutputStream(uri,"wt");InputStream in=new FileInputStream(f)){if(out==null)throw new IOException("无法打开保存位置");Core.copy(in,out,Core.MAX_EXPANDED+64L*1024*1024,this::check);}catch(Throwable e){failure=e;}finally{cleanExport(f);finish(id,failure,null);}});
    }
    interface ExportReady{void ready(File f);}
    void saveAll(String ids,ExportReady callback)throws IOException{
        String id=begin(UUID.randomUUID().toString(),"bundle");worker.execute(()->{File target=new File(context.getCacheDir(),"轻转结果-"+UUID.randomUUID()+".zip");Throwable failure=null;try{
            JSONArray a=new JSONArray(ids);if(a.length()==0)throw new IOException("没有结果可导出");long total=0;try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(target))){for(int i=0;i<a.length();i++){check();File f=output(a.getString(i));out.putNextEntry(new ZipEntry(String.format(Locale.ROOT,"%03d/",i+1)+f.getName()));try(InputStream in=new FileInputStream(f)){total+=Core.copy(in,out,Core.MAX_EXPANDED-total,this::check);}out.closeEntry();}}check();
        }catch(Throwable e){failure=e;target.delete();}finally{finish(id,failure,null);}if(failure==null)callback.ready(target);});
    }
    void cleanExport(File f){if(f!=null&&f.getParentFile().equals(context.getCacheDir())&&f.getName().startsWith("轻转结果-"))f.delete();}
    void saveToGallery(String json,String remainingDocuments)throws IOException{
        final JSONArray chosen=selectedOutputs(json);
        for(int i=0;i<chosen.length();i++){String mime=OutputProvider.mime(chosen.optJSONObject(i).optString("name"));if(!mime.startsWith("image/")&&!mime.startsWith("video/"))throw new IOException("只有图片或视频可以保存到相册");}
        final String task=begin(UUID.randomUUID().toString(),"gallery");
        worker.execute(()->{Throwable failure=null;int saved=0;Uri partial=null;try{for(int i=0;i<chosen.length();i++){check();File source=output(chosen.getJSONObject(i).getString("id"));String mime=OutputProvider.mime(source.getName());boolean video=mime.startsWith("video/");android.content.ContentValues values=new android.content.ContentValues();values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,source.getName());values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,mime);if(android.os.Build.VERSION.SDK_INT>=29){values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,(video?android.os.Environment.DIRECTORY_MOVIES:android.os.Environment.DIRECTORY_PICTURES)+"/轻转");values.put(android.provider.MediaStore.MediaColumns.IS_PENDING,1);}partial=context.getContentResolver().insert(video?android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI:android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);if(partial==null)throw new IOException("无法创建相册文件");try(InputStream in=new FileInputStream(source);OutputStream out=context.getContentResolver().openOutputStream(partial,"w")){if(out==null)throw new IOException("相册无法写入");Core.copy(in,out,Core.MAX_EXPANDED,this::check);}check();if(android.os.Build.VERSION.SDK_INT>=29){android.content.ContentValues publish=new android.content.ContentValues();publish.put(android.provider.MediaStore.MediaColumns.IS_PENDING,0);if(context.getContentResolver().update(partial,publish,null,null)==0)throw new IOException("无法发布相册文件");}partial=null;saved++;progress(task,"已保存到相册 "+saved+" / "+chosen.length(),saved*100/chosen.length());}}
        catch(Throwable e){failure=new IOException("已保存到相册 "+saved+" / "+chosen.length()+" 份："+message(e),e);if(partial!=null)try{context.getContentResolver().delete(partial,null,null);}catch(Exception ignored){}}finally{if(failure==null&&remainingDocuments!=null&&!remainingDocuments.equals("[]"))context.getSharedPreferences("gallery-followup",0).edit().putString("ids",remainingDocuments).commit();finish(task,failure,null);}});
    }
    synchronized JSONArray selectedOutputs(String ids)throws IOException{return ResultSelection.choose(outputs,ids);}
    void deleteSelected(String ids)throws IOException{
        final JSONArray chosen=selectedOutputs(ids);final String task=begin(UUID.randomUUID().toString(),"cleanup");
        worker.execute(()->{Throwable failure=null;try{synchronized(this){JSONArray old=outputs;outputs=ResultSelection.without(outputs,chosen);try{checkpoint();}catch(IOException e){outputs=old;throw e;}}for(int i=0;i<chosen.length();i++){File f=Core.safe(resultRoot,chosen.getJSONObject(i).getString("id"));if(f.exists()&&!f.delete())throw new IOException("记录已移除，部分缓存未能删除");File parent=f.getParentFile();while(!parent.equals(resultRoot)&&parent.delete())parent=parent.getParentFile();}}catch(Throwable e){failure=e;}finally{finish(task,failure,null);}});
    }
    void removeSelected(String json)throws IOException{
        JSONArray rows=new JSONArray();synchronized(this){for(JSONObject row:info.values())rows.put(row);}final JSONArray chosen=ResultSelection.choose(rows,json);final String task=begin(UUID.randomUUID().toString(),"cleanup");
        worker.execute(()->{Throwable failure=null;try{synchronized(this){LinkedHashMap<String,File> oldFiles=new LinkedHashMap<>(inputs);LinkedHashMap<String,JSONObject> oldInfo=new LinkedHashMap<>(info);for(int i=0;i<chosen.length();i++){String id=chosen.getJSONObject(i).getString("id");inputs.remove(id);info.remove(id);}try{checkpoint();}catch(IOException e){inputs.clear();inputs.putAll(oldFiles);info.clear();info.putAll(oldInfo);throw e;}}for(int i=0;i<chosen.length();i++)remove(new File(inputRoot,chosen.getJSONObject(i).getString("id")));}catch(Throwable e){failure=e;}finally{finish(task,failure,null);}});
    }
    void saveSelectedToFolder(String json,Uri tree)throws IOException{
        final JSONArray chosen=selectedOutputs(json);final String task=begin(UUID.randomUUID().toString(),"export");
        worker.execute(()->{Throwable failure=null;int saved=0;Uri partial=null;try{Uri directory=android.provider.DocumentsContract.buildDocumentUriUsingTree(tree,android.provider.DocumentsContract.getTreeDocumentId(tree));for(int i=0;i<chosen.length();i++){check();File source=output(chosen.getJSONObject(i).getString("id"));partial=android.provider.DocumentsContract.createDocument(context.getContentResolver(),directory,OutputProvider.mime(source.getName()),source.getName());if(partial==null)throw new IOException("文件夹不支持创建文件");try(InputStream in=new FileInputStream(source);OutputStream out=context.getContentResolver().openOutputStream(partial,"wt")){if(out==null)throw new IOException("无法打开保存位置");Core.copy(in,out,Core.MAX_EXPANDED,this::check);}check();partial=null;saved++;progress(task,"已保存 "+saved+" / "+chosen.length(),saved*100/chosen.length());}}catch(Throwable e){failure=new IOException("已保存 "+saved+" / "+chosen.length()+" 份文件："+message(e),e);if(partial!=null)try{android.provider.DocumentsContract.deleteDocument(context.getContentResolver(),partial);}catch(Exception ignored){}}finally{finish(task,failure,null);}});
    }
    void deleteAll()throws IOException{
        String id=begin(UUID.randomUUID().toString(),"cleanup");worker.execute(()->{Throwable failure=null;try{synchronized(this){JSONArray old=outputs;outputs=new JSONArray();try{checkpoint();}catch(IOException e){outputs=old;throw e;}}remove(resultRoot);resultRoot.mkdirs();}catch(Throwable e){failure=e;}finally{finish(id,failure,null);}});
    }
    static void remove(File f){if(f==null)return;File[] fs=f.listFiles();if(fs!=null)for(File child:fs)remove(child);f.delete();}
    static long treeSize(File f){if(!f.isDirectory())return f.length();long n=0;File[] fs=f.listFiles();if(fs!=null)for(File c:fs)n+=treeSize(c);return n;}
}
