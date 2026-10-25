package cn.qingzhuan.app;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;
import org.json.*;

/** Conservative content sniffing. Android decoders further narrow image/audio capability. */
public final class FileProbe {
    public static final class Result {
        public String kind="unknown",extension="";
        public final Set<String> tools=new LinkedHashSet<>(Arrays.asList("pack","hash"));
        Result as(String k,String ext,String... allowed){kind=k;extension=ext;tools.addAll(Arrays.asList(allowed));return this;}
    }
    static boolean at(byte[] b,int offset,int... sig){if(b.length<offset+sig.length)return false;for(int i=0;i<sig.length;i++)if((b[offset+i]&255)!=sig[i])return false;return true;}
    static byte[] head(InputStream in,int max)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[1024];int n;while(o.size()<max&&(n=in.read(b,0,Math.min(b.length,max-o.size())))>0)o.write(b,0,n);return o.toByteArray();}
    public static Result inspect(File f)throws IOException{
        Result r=new Result();byte[] b;try(InputStream in=new FileInputStream(f)){b=head(in,8192);}String e=Core.ext(f.getName());
        if(at(b,0,37,80,68,70,45))return r.as("pdf","pdf","pdfimage","merge","split","document");
        if(at(b,0,137,80,78,71,13,10,26,10))return r.as("image","png");
        if(at(b,0,255,216,255))return r.as("image","jpg");
        if(at(b,0,71,73,70,56))return r.as("image","gif");
        if(at(b,0,66,77))return r.as("image","bmp");
        if(at(b,0,82,73,70,70)&&at(b,8,87,69,66,80))return r.as("image","webp");
        if(at(b,0,80,75,3,4)||at(b,0,80,75,5,6)){
            try(ZipFile z=new ZipFile(f)){
                if(z.getEntry("[Content_Types].xml")!=null){
                    if(z.getEntry("word/document.xml")!=null)return r.as("document","docx","document");
                    if(z.getEntry("xl/workbook.xml")!=null)return r.as("table","xlsx","table");
                }
            }catch(ZipException ignored){}return r.as("archive","zip","unpack");
        }
        if(at(b,0,55,122,188,175,39,28))return r.as("archive","7z","unpack");
        if(at(b,0,82,97,114,33,26,7,0))return r.as("archive","rar","unpack");
        if(at(b,0,82,97,114,33,26,7,1))return r.as("unsupported","rar");
        if(at(b,0,31,139)){
            String x="gz";try(InputStream in=new GZIPInputStream(new FileInputStream(f))){if(at(head(in,512),257,117,115,116,97,114))x="tar.gz";}catch(IOException ignored){}
            return r.as("archive",x,"unpack");
        }
        if(at(b,0,66,90,104))return r.as("archive","bz2","unpack");
        if(at(b,0,253,55,122,88,90,0))return r.as("archive","xz","unpack");
        if(at(b,257,117,115,116,97,114))return r.as("archive","tar","unpack");
        if(at(b,4,102,116,121,112)||at(b,0,73,68,51)||at(b,0,102,76,97,67)||at(b,0,79,103,103,83)||at(b,0,82,73,70,70)||at(b,0,26,69,223,163)||at(b,0,255,251)||at(b,0,255,241)||at(b,0,255,249))return r.as("media",e);
        // UTF-16 BOM is textual even though its byte representation includes NULs.
        boolean bom16=at(b,0,255,254)||at(b,0,254,255),text=true;
        if(!bom16)for(byte v:b){int c=v&255;if(c==0||(c<32&&c!=9&&c!=10&&c!=13&&c!=12)){text=false;break;}}
        if(text){
            r.as("document",bom16?"txt":Arrays.asList("txt","md","html","htm","csv","json","log").contains(e)?e:"txt","document");
            if(f.length()<=16L*1024*1024){
                try{
                    if(e.equals("csv")||e.equals("tsv")){Core.csv(Core.text(f,"auto"),e.equals("tsv")?'\t':',');r.as("table",e,"table");if(e.equals("tsv"))r.tools.remove("document");}
                    else if(e.equals("json")){JSONArray a=new JSONArray(Core.text(f,"auto"));if(a.length()==0||a.opt(0) instanceof JSONObject||a.opt(0) instanceof JSONArray)r.as("table","json","table");}
                }catch(Exception ignored){}
            }
            return r;
        }return r;
    }
}
