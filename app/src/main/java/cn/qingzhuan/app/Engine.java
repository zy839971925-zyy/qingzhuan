package cn.qingzhuan.app;

import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.*;
import android.media.*;
import android.os.ParcelFileDescriptor;
import android.text.*;
import org.json.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.*;
import java.util.concurrent.atomic.AtomicBoolean;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.io.MemoryUsageSetting;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.archivers.sevenz.*;
import org.apache.commons.compress.compressors.bzip2.*;
import org.apache.commons.compress.compressors.xz.*;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import com.github.junrar.Archive;

public final class Engine {
    static final class ManagedPdf extends PdfDocument implements AutoCloseable {}
    public interface Progress {void set(String text,int value);}
    final Context ctx;final File root;final JSONObject options;final AtomicBoolean cancelled;final Progress progress;
    final Core.Check check;long expanded=0;int entries=0;
    public Engine(Context c,File root,JSONObject options,AtomicBoolean cancel,Progress p){this.ctx=c;this.root=root;this.options=options;cancelled=cancel;progress=p;check=()->{if(cancelled.get()||Thread.currentThread().isInterrupted())throw new IOException("已取消");};}
    File out(String name)throws IOException {File f=new File(root,Core.clean(name));String stem=Core.stem(name),ext=Core.ext(name);int k=2;while(f.exists())f=new File(root,stem+" ("+k+++ ")"+(ext.isEmpty()?"":"."+ext));return f;}
    String opt(String k,String d){return options.optString(k,d);}
    public void run(String tool,List<File> inputs)throws Exception{
        try{runInternal(tool,inputs);}finally{AppSessionCleanup.remove(new File(root,".scratch"));}
    }
    void runInternal(String tool,List<File> inputs)throws Exception{
        if(inputs.isEmpty())throw new IOException("请先选择文件");root.mkdirs();String target=opt("target","");
        if(tool.equals("imagepdf")){imagesPdf(inputs);return;}
        if(tool.equals("merge")){merge(inputs);return;}
        if(tool.equals("pack")){pack(inputs,target);return;}
        for(int i=0;i<inputs.size();i++){
            check.check();File f=inputs.get(i);progress.set("正在处理 "+f.getName(),(int)(i*100f/inputs.size()));String base=Core.stem(f.getName());
            switch(tool){
                case "image":try{Bitmap b=image(f);try{saveBitmap(b,out(base+"."+target),target);}finally{b.recycle();}}catch(OutOfMemoryError e){throw new IOException("图片过大，请选择更小的输出尺寸");}break;
                case "pdfimage":pdfImages(f,target);break;
                case "split":split(f);break;
                case "document":document(f,target);break;
                case "table":table(f,target);break;
                case "unpack":unpack(f,new File(root,base));break;
                case "audio":{File source=MotionPhoto.extract(f,new File(root,".scratch"),check);if(target.equals("wav"))wav(source,out(base+".wav"));else m4a(source,out(base+".m4a"));break;}
                case "gif":case "video":case "videoframe":case "motionvideo":{
                    File scratch=new File(root,".scratch");File source=MotionPhoto.extract(f,scratch,check);final int batchIndex=i;
                    Progress mediaProgress=(message,value)->progress.set(message,value<0?-1:(batchIndex*100+Math.min(99,value))/inputs.size());
                    if(tool.equals("gif"))NativeMediaEngine.gif(source,out(base+".gif"),options,check,mediaProgress);
                    else if(tool.equals("videoframe"))NativeMediaEngine.screenshot(source,out(base+"-截图."+target),options,check);
                    else NativeMediaEngine.transcode(ctx,source,out(base+"-"+(tool.equals("motionvideo")?"motion":target)+".mp4"),tool.equals("motionvideo")?"h264":target,options,check,mediaProgress);
                    break;
                }
                case "hash":Core.write(out(base+"-校验.txt"),"文件："+f.getName()+"\nSHA-256："+Core.hash(f,"SHA-256",check)+"\nMD5："+Core.hash(f,"MD5",check)+"\n");break;
                default:throw new IOException("不支持的操作");
            }
        }check.check();progress.set("处理完成",100);
    }
    Bitmap image(File f)throws Exception{
        BitmapFactory.Options bo=new BitmapFactory.Options();bo.inJustDecodeBounds=true;BitmapFactory.decodeFile(f.getPath(),bo);
        if(bo.outWidth<1||bo.outHeight<1)throw new IOException("无法解码图片："+f.getName()+"。HEIC/AVIF 等格式取决于系统支持；GIF 只转换首帧。");
        int edge=options.optInt("edge",4096);if(edge<=0||edge>4096)edge=4096;int sample=1;while(Math.max(bo.outWidth,bo.outHeight)/sample>edge*1.4)sample*=2;
        bo=new BitmapFactory.Options();bo.inSampleSize=sample;Bitmap b=BitmapFactory.decodeFile(f.getPath(),bo);if(b==null)throw new IOException("图片解码失败");
        Matrix m=new Matrix();try{ExifInterface exif=new ExifInterface(f.getPath());int rot=exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,1);switch(rot){case 2:m.setScale(-1,1);break;case 3:m.setRotate(180);break;case 4:m.setScale(1,-1);break;case 5:m.setRotate(90);m.postScale(-1,1);break;case 6:m.setRotate(90);break;case 7:m.setRotate(270);m.postScale(-1,1);break;case 8:m.setRotate(270);break;}}catch(IOException ignored){}
        if(!m.isIdentity()){Bitmap r=Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),m,true);if(r!=b)b.recycle();b=r;}
        if(Math.max(b.getWidth(),b.getHeight())>edge){float ratio=edge/(float)Math.max(b.getWidth(),b.getHeight());Bitmap r=Bitmap.createScaledBitmap(b,Math.max(1,Math.round(b.getWidth()*ratio)),Math.max(1,Math.round(b.getHeight()*ratio)),true);if(r!=b)b.recycle();b=r;}return b;
    }
    void saveBitmap(Bitmap b,File f,String target)throws IOException{
        Bitmap actual=b;if(target.equals("jpg")&&b.hasAlpha()){actual=Bitmap.createBitmap(b.getWidth(),b.getHeight(),Bitmap.Config.ARGB_8888);Canvas c=new Canvas(actual);c.drawColor(Color.WHITE);c.drawBitmap(b,0,0,null);}
        Bitmap.CompressFormat fmt=target.equals("png")?Bitmap.CompressFormat.PNG:target.equals("webp")?Bitmap.CompressFormat.WEBP:Bitmap.CompressFormat.JPEG;
        try(OutputStream o=new FileOutputStream(f)){if(!actual.compress(fmt,Math.max(20,Math.min(100,options.optInt("quality",88))),o))throw new IOException("图片编码失败");}finally{if(actual!=b)actual.recycle();}
    }
    void imagesPdf(List<File> fs)throws Exception{
        try(ManagedPdf pdf=new ManagedPdf()){
            for(int i=0;i<fs.size();i++){check.check();progress.set("正在排版 "+fs.get(i).getName(),i*100/fs.size());Bitmap b=image(fs.get(i));try{boolean landscape=b.getWidth()>b.getHeight();int w=landscape?842:595,h=landscape?595:842;PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(w,h,i+1).create());Canvas c=page.getCanvas();c.drawColor(Color.WHITE);float scale=Math.min((w-40f)/b.getWidth(),(h-40f)/b.getHeight());float bw=b.getWidth()*scale,bh=b.getHeight()*scale;c.drawBitmap(b,null,new RectF((w-bw)/2,(h-bh)/2,(w+bw)/2,(h+bh)/2),new Paint(Paint.FILTER_BITMAP_FLAG));pdf.finishPage(page);}finally{b.recycle();}}
            try(OutputStream o=new FileOutputStream(out("图片合集.pdf"))){pdf.writeTo(o);}
        }
    }
    MemoryUsageSetting memory(){File cache=new File(root,".scratch");cache.mkdirs();return MemoryUsageSetting.setupTempFileOnly().setTempDir(cache);}
    PDDocument load(File f)throws IOException{PDDocument d=PDDocument.load(f,opt("password",""),memory());if(!d.getCurrentAccessPermission().canExtractContent()){d.close();throw new IOException("PDF 不允许提取内容");}if(d.getNumberOfPages()>1000){d.close();throw new IOException("PDF 超过一千页处理上限");}return d;}
    void merge(List<File> files)throws Exception{
        List<PDDocument> sources=new ArrayList<>();
        try(PDDocument dest=new PDDocument(memory())){
            int count=0;for(File f:files){check.check();progress.set("合并 "+f.getName(),count*100/files.size());PDDocument src=load(f);sources.add(src);if(dest.getNumberOfPages()+src.getNumberOfPages()>1000)throw new IOException("合并结果页数过多");for(int i=0;i<src.getNumberOfPages();i++){check.check();com.tom_roush.pdfbox.pdmodel.PDPage page=dest.importPage(src.getPage(i));page.setResources(src.getPage(i).getResources());}count++;}dest.save(out("合并文档.pdf"));
        }finally{for(PDDocument d:sources)try{d.close();}catch(IOException ignored){}}
    }
    void split(File f)throws Exception{
        try(PDDocument src=load(f)){for(int i:Core.pages(opt("pages",""),src.getNumberOfPages())){check.check();progress.set("正在拆分第 "+(i+1)+" 页",i*100/Math.max(1,src.getNumberOfPages()));try(PDDocument dest=new PDDocument()){com.tom_roush.pdfbox.pdmodel.PDPage page=dest.importPage(src.getPage(i));page.setResources(src.getPage(i).getResources());dest.save(out(Core.stem(f.getName())+"-p"+(i+1)+".pdf"));}}}
    }
    void pdfImages(File f,String target)throws Exception{
        // Native renderer handles PDF rasterization; password-protected files are first decrypted locally.
        File source=f,temp=null;
        try{
        if(!opt("password","").isEmpty()){File scratch=new File(root,".scratch");scratch.mkdirs();temp=File.createTempFile("pdf-",".pdf",scratch);try(PDDocument d=load(f)){d.setAllSecurityToBeRemoved(true);d.save(temp);}source=temp;}
        try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(source,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer r=new PdfRenderer(fd)){
            if(r.getPageCount()>1000)throw new IOException("PDF 超过一千页处理上限");for(int i:Core.pages(opt("pages",""),r.getPageCount())){check.check();try(PdfRenderer.Page p=r.openPage(i)){progress.set("渲染第 "+(i+1)+" 页",i*100/Math.max(1,r.getPageCount()));int edge=options.optInt("edge",2048);edge=Math.max(800,Math.min(4096,edge));float s=edge/(float)Math.max(p.getWidth(),p.getHeight());Bitmap b=Bitmap.createBitmap(Math.max(1,Math.round(p.getWidth()*s)),Math.max(1,Math.round(p.getHeight()*s)),Bitmap.Config.ARGB_8888);try{b.eraseColor(Color.WHITE);p.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_PRINT);saveBitmap(b,out(Core.stem(f.getName())+"-p"+(i+1)+"."+target),target);}finally{b.recycle();}}}
        }
        }finally{if(temp!=null)temp.delete();}
    }
    void document(File f,String target)throws Exception{
        String ext=Core.ext(f.getName()),s;
        switch(ext){case "pdf":try(PDDocument d=load(f)){PDFTextStripper ts=new PDFTextStripper();ts.setSortByPosition(true);s=ts.getText(d);if(s.trim().isEmpty())throw new IOException("没有可提取的文字：扫描件需要 OCR，本版尚未提供");}break;
        case "docx":s=Core.docx(f);break;
        case "html":case "htm":s=Html.fromHtml(Core.text(f,opt("encoding","auto")),Html.FROM_HTML_MODE_LEGACY).toString();break;
        case "txt":case "md":case "csv":case "json":case "log":s=Core.text(f,opt("encoding","auto"));break;
        default:throw new IOException("文档支持 TXT、MD、HTML、DOCX 和文字型 PDF；不支持旧版 DOC 或 PPT");}
        check.check();File dest=out(Core.stem(f.getName())+"."+target);
        switch(target){case "txt":Core.write(dest,s);break;case "html":Core.write(dest,"<!doctype html><html lang=\"zh-CN\"><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width\"><title>"+Core.xml(Core.stem(f.getName()))+"</title><style>body{max-width:48rem;margin:3rem auto;padding:0 1.5rem;font:18px/1.8 system-ui;color:#222}pre{white-space:pre-wrap;word-break:break-word;font:inherit}</style><pre>"+Core.xml(s)+"</pre></html>");break;case "docx":Core.docxOut(dest,s);break;case "pdf":textPdf(s,dest);break;default:throw new IOException("输出格式有误");}
    }
    void textPdf(String text,File dest)throws Exception{
        if(text.length()>500000)throw new IOException("文字过长，请分段转换");TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);paint.setTextSize(12);paint.setColor(Color.rgb(28,35,48));
        StaticLayout layout=StaticLayout.Builder.obtain(text,0,text.length(),paint,499).setLineSpacing(4,1).setIncludePad(false).build();
        try(ManagedPdf pdf=new ManagedPdf()){
            int start=0,n=1;while(start<layout.getLineCount()){check.check();int end=start;int top=layout.getLineTop(start);while(end<layout.getLineCount()&&layout.getLineBottom(end)-top<=746)end++;if(end==start)end++;PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,n++).create());Canvas c=page.getCanvas();c.drawColor(Color.WHITE);c.save();c.clipRect(48,48,547,48+layout.getLineBottom(end-1)-top);c.translate(48,48-top);layout.draw(c);c.restore();pdf.finishPage(page);start=end;if(n>1000)throw new IOException("生成文档页数过多");}try(OutputStream o=new FileOutputStream(dest)){pdf.writeTo(o);}
        }
    }
    void table(File f,String target)throws Exception{
        String ext=Core.ext(f.getName());List<List<String>> rows;
        if(ext.equals("xlsx"))rows=Core.xlsx(f);
        else if(ext.equals("csv")||ext.equals("tsv"))rows=Core.csv(Core.text(f,opt("encoding","auto")),ext.equals("tsv")?'\t':',');
        else if(ext.equals("json")){
            JSONArray a=new JSONArray(Core.text(f,opt("encoding","auto")));if(a.length()>100000)throw new IOException("表格超过十万行");rows=new ArrayList<>();
            if(a.length()>0&&a.get(0) instanceof JSONObject){LinkedHashSet<String> keys=new LinkedHashSet<>();for(int i=0;i<a.length();i++){Object ob=a.get(i);if(!(ob instanceof JSONObject))throw new IOException("JSON 数组内的元素类型应保持一致");Iterator<String> it=((JSONObject)ob).keys();while(it.hasNext())keys.add(it.next());}List<String> header=new ArrayList<>(keys);rows.add(header);for(int i=0;i<a.length();i++){JSONObject ob=a.getJSONObject(i);List<String> row=new ArrayList<>();for(String key:header)row.add(ob.isNull(key)?"":String.valueOf(ob.get(key)));rows.add(row);}}
            else for(int i=0;i<a.length();i++){JSONArray r=a.getJSONArray(i);List<String> row=new ArrayList<>();for(int j=0;j<r.length();j++)row.add(r.isNull(j)?"":String.valueOf(r.get(j)));rows.add(row);}
        }else throw new IOException("表格支持 CSV、TSV、JSON 和 XLSX；不支持旧版 XLS");
        check.check();File dest=out(Core.stem(f.getName())+"."+target);
        switch(target){case "xlsx":Core.xlsxOut(dest,rows);break;case "csv":case "tsv":Core.write(dest,"\uFEFF"+Core.csvOut(rows,target.equals("tsv")?'\t':','));break;case "json":JSONArray a=new JSONArray();for(List<String> row:rows)a.put(new JSONArray(row));Core.write(dest,a.toString(2));break;default:throw new IOException("表格输出格式有误");}
    }
    String archiveName(File file,Set<String> used){String n=file.getName(),candidate=n;int i=2;while(!used.add(candidate.toLowerCase(Locale.ROOT))){String e=Core.ext(n);candidate=Core.stem(n)+" ("+i+++")"+(e.isEmpty()?"":"."+e);}return candidate;}
    void pack(List<File> files,String target)throws Exception{
        Set<String> used=new HashSet<>();
        File dest=out("文件合集."+target);if(target.equals("zip")){
            final long total=files.stream().mapToLong(File::length).sum();final long[] copied={0};
            try(net.lingala.zip4j.ZipFile z=new net.lingala.zip4j.ZipFile(dest,opt("password","").toCharArray())){
                for(File file:files){check.check();ZipParameters p=new ZipParameters();p.setFileNameInZip(archiveName(file,used));p.setEntrySize(file.length());
                    if(!opt("password","").isEmpty()){p.setEncryptFiles(true);p.setEncryptionMethod(EncryptionMethod.AES);}
                    try(InputStream in=new FilterInputStream(new FileInputStream(file)){
                        void counted(int n)throws IOException{if(n>0){copied[0]+=n;progress.set("压缩 "+file.getName(),total==0?0:(int)Math.min(99,copied[0]*100/total));}check.check();}
                        @Override public int read(byte[] b,int off,int len)throws IOException{check.check();int n=super.read(b,off,len);counted(n);return n;}
                        @Override public int read()throws IOException{check.check();int n=super.read();counted(n<0?0:1);return n;}
                    }){z.addStream(in,p);}check.check();
                }
            }
        }else if(target.equals("tar.gz")){
            try(TarArchiveOutputStream tar=new TarArchiveOutputStream(new GZIPOutputStream(new FileOutputStream(dest)),"UTF-8")){tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);for(File f:files){check.check();TarArchiveEntry e=new TarArchiveEntry(f,archiveName(f,used));tar.putArchiveEntry(e);try(InputStream in=new FileInputStream(f)){Core.copy(in,tar,Core.MAX_INPUT,check);}tar.closeArchiveEntry();}}
        }else throw new IOException("压缩格式有误");
    }
    File archivePath(File dir,String name,boolean directory)throws IOException{
        check.check();if(++entries>2000)throw new IOException("解压条目超过两千个");if(directory&&(name.isEmpty()||name.equals(".")||name.matches("(\\./)+")))return null;File p=Core.safe(dir,name);if(directory){if(!p.mkdirs()&&!p.isDirectory())throw new IOException("无法创建文件夹");return null;}if(p.exists())throw new IOException("压缩包存在重复路径，为避免覆盖已停止");if(!p.getParentFile().mkdirs()&&!p.getParentFile().isDirectory())throw new IOException("无法创建解压目录");return p;
    }
    void extracted(InputStream in,File dest)throws IOException{try(OutputStream o=new FileOutputStream(dest)){expanded+=Core.copy(in,o,Core.MAX_EXPANDED-expanded,check);}}
    void unpack(File f,File dir)throws Exception{
        if(dir.exists())throw new IOException("压缩包名称重复，请分次解压");dir.mkdirs();String n=f.getName().toLowerCase(Locale.ROOT);
        if(n.endsWith(".zip")){
            try(net.lingala.zip4j.ZipFile z=new net.lingala.zip4j.ZipFile(f,opt("password","").toCharArray())){if(opt("zipEncoding","auto").equals("GB18030"))z.setCharset(java.nio.charset.Charset.forName("GB18030"));for(FileHeader h:z.getFileHeaders()){File p=archivePath(dir,h.getFileName(),h.isDirectory());if(p!=null)try(InputStream in=z.getInputStream(h)){extracted(in,p);}}}
        }else if(n.endsWith(".7z")){
            try(SevenZFile z=SevenZFile.builder().setFile(f).setPassword(opt("password","")).setMaxMemoryLimitKb(65536).get()){SevenZArchiveEntry e;while((e=z.getNextEntry())!=null){File p=archivePath(dir,e.getName()==null?"未命名文件":e.getName(),e.isDirectory());if(p!=null){try(OutputStream o=new FileOutputStream(p)){byte[] b=new byte[65536];int count;while((count=z.read(b))!=-1){check.check();expanded+=count;if(expanded>Core.MAX_EXPANDED)throw new IOException("解压总量超过安全上限");o.write(b,0,count);}}}}}
        }else if(n.endsWith(".rar")){
            try(Archive a=new Archive(f,opt("password",""))){for(com.github.junrar.rarfile.FileHeader h:a.getFileHeaders()){if(h.isSplitAfter()||h.isSplitBefore())throw new IOException("暂不支持分卷 RAR");String name=h.isUnicode()?h.getFileNameW():h.getFileNameString();File p=archivePath(dir,name,h.isDirectory());if(p!=null)try(InputStream in=a.getInputStream(h)){extracted(in,p);}}}catch(com.github.junrar.exception.UnsupportedRarV5Exception e){throw new IOException("本版支持 RAR4，不支持 RAR5 或分卷 RAR");}
        }else{
            InputStream raw=new BufferedInputStream(new FileInputStream(f));try{InputStream in=raw;boolean tar=n.endsWith(".tar")||n.endsWith(".tar.gz")||n.endsWith(".tgz")||n.endsWith(".tar.bz2")||n.endsWith(".tar.xz");if(n.endsWith(".gz")||n.endsWith(".tgz"))in=new GZIPInputStream(raw);else if(n.endsWith(".bz2"))in=new BZip2CompressorInputStream(raw);else if(n.endsWith(".xz"))in=new XZCompressorInputStream(raw,true,65536);else if(!n.endsWith(".tar"))throw new IOException("不支持此压缩格式");
                try(InputStream decoded=in){if(tar){try(TarArchiveInputStream t=new TarArchiveInputStream(decoded,"UTF-8")){TarArchiveEntry e;while((e=t.getNextTarEntry())!=null){if(!e.isDirectory()&&!e.isFile())throw new IOException("为防止路径跳转，不解压链接和设备条目");File p=archivePath(dir,e.getName(),e.isDirectory());if(p!=null)extracted(t,p);}}}else{File p=archivePath(dir,Core.stem(f.getName()),false);extracted(decoded,p);}}
            }finally{raw.close();}
        }
    }
    int audioTrack(MediaExtractor e)throws IOException{for(int i=0;i<e.getTrackCount();i++){MediaFormat f=e.getTrackFormat(i);String mime=f.getString(MediaFormat.KEY_MIME);if(mime!=null&&mime.startsWith("audio/")){e.selectTrack(i);return i;}}throw new IOException("没有找到音轨");}
    void m4a(File f,File dest)throws Exception{
        MediaExtractor e=new MediaExtractor();MediaMuxer m=null;boolean started=false;try{e.setDataSource(f.getPath());int track=audioTrack(e);MediaFormat format=e.getTrackFormat(track);if(!"audio/mp4a-latm".equals(format.getString(MediaFormat.KEY_MIME)))throw new IOException("直接提取 M4A 仅支持 AAC 音轨。其他音轨可选择转 WAV。");m=new MediaMuxer(dest.getPath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);int outTrack=m.addTrack(format);m.start();started=true;
            int max=format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)?format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE):1048576;ByteBuffer buffer=ByteBuffer.allocate(Math.max(1048576,Math.min(max,8*1048576)));MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();long origin=-1;
            while(true){check.check();int n=e.readSampleData(buffer,0);if(n<0)break;if(n>buffer.capacity())throw new IOException("音轨采样数据过大");if(origin<0)origin=e.getSampleTime();info.set(0,n,Math.max(0,e.getSampleTime()-origin),e.getSampleFlags()&MediaCodec.BUFFER_FLAG_KEY_FRAME);m.writeSampleData(outTrack,buffer,info);e.advance();buffer.clear();}
        }finally{e.release();if(m!=null){try{if(started)m.stop();}finally{m.release();}}}
    }
    void wav(File f,File dest)throws Exception{
        MediaExtractor e=new MediaExtractor();MediaCodec codec=null;boolean started=false;long written=0;int rate=44100,channels=2;try(RandomAccessFile out=new RandomAccessFile(dest,"rw")){
            out.write(new byte[44]);e.setDataSource(f.getPath());int track=audioTrack(e);MediaFormat input=e.getTrackFormat(track);codec=MediaCodec.createDecoderByType(input.getString(MediaFormat.KEY_MIME));codec.configure(input,null,null,0);codec.start();started=true;boolean sent=false,done=false;MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();long last=System.currentTimeMillis();
            while(!done){check.check();if(System.currentTimeMillis()-last>20000)throw new IOException("音频解码器长时间没有响应");
                if(!sent){int ix=codec.dequeueInputBuffer(10000);if(ix>=0){ByteBuffer b=codec.getInputBuffer(ix);b.clear();int n=e.readSampleData(b,0);if(n<0){codec.queueInputBuffer(ix,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);sent=true;}else{codec.queueInputBuffer(ix,0,n,e.getSampleTime(),0);e.advance();}}}
                int ix=codec.dequeueOutputBuffer(info,10000);if(ix==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){MediaFormat fmt=codec.getOutputFormat();rate=fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE);channels=fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT);if(fmt.containsKey(MediaFormat.KEY_PCM_ENCODING)&&fmt.getInteger(MediaFormat.KEY_PCM_ENCODING)!=AudioFormat.ENCODING_PCM_16BIT)throw new IOException("此解码器未输出 16 位 PCM");last=System.currentTimeMillis();}
                else if(ix>=0){ByteBuffer b=codec.getOutputBuffer(ix);if(info.size>0){b.position(info.offset);b.limit(info.offset+info.size);byte[] data=new byte[info.size];b.get(data);written+=data.length;if(written>Core.MAX_EXPANDED)throw new IOException("解码后文件超过安全上限");out.write(data);}done=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;codec.releaseOutputBuffer(ix,false);last=System.currentTimeMillis();}
            }
            out.seek(0);ByteBuffer header=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);header.put("RIFF".getBytes("US-ASCII")).putInt((int)written+36).put("WAVEfmt ".getBytes("US-ASCII")).putInt(16).putShort((short)1).putShort((short)channels).putInt(rate).putInt(rate*channels*2).putShort((short)(channels*2)).putShort((short)16).put("data".getBytes("US-ASCII")).putInt((int)written);out.write(header.array());
        }finally{e.release();if(codec!=null){try{if(started)codec.stop();}finally{codec.release();}}}
    }
}

/** Engine cleanup independent of Android host, also exercised by the JVM suite. */
final class AppSessionCleanup {
    static void remove(File f){File[] fs=f.listFiles();if(fs!=null)for(File c:fs)remove(c);f.delete();}
}
