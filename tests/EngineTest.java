package cn.qingzhuan.app;
import java.io.*;import java.util.*;import java.util.zip.*;import org.json.*;import org.apache.commons.compress.archivers.sevenz.*;
import java.util.concurrent.atomic.AtomicBoolean;
public class EngineTest {
    static int count=0;static File base=new File("build/test-data");
    static void yes(boolean b,String n){count++;if(!b)throw new AssertionError(n);}
    interface Run{void run()throws Exception;}
    static void rejects(Run r,String n)throws Exception{boolean failed=false;try{r.run();}catch(Exception e){failed=true;}yes(failed,n);}
    static Engine engine(String name,JSONObject opts){File d=new File(base,name);d.mkdirs();return new Engine(null,d,opts,new AtomicBoolean(),(m,v)->{});}
    static File f(String name,String text)throws Exception{File f=new File(base,name);Core.write(f,text);return f;}
    static List<List<String>> rows(String[][] a){List<List<String>> o=new ArrayList<>();for(String[] b:a)o.add(Arrays.asList(b));return o;}
    public static void main(String[] args)throws Exception{
        base.mkdirs();
        String[][][] samples={{{"名称","说明"},{"咖啡","燕麦奶,少冰"},{"引号","他说\"好\""},{"换行","第一行\n第二行"}},{{"","a",""},{"x"},{"",""}},{{"只有一格"}},{{"中文 😀 <>&\"'", "=SUM(A1:A2)","00123"}}};
        for(int i=0;i<samples.length;i++){List<List<String>> input=rows(samples[i]);yes(Core.csv(Core.csvOut(input,','),',').equals(input),"CSV roundtrip "+i);yes(Core.csv(Core.csvOut(input,'\t'),'\t').equals(input),"TSV roundtrip "+i);File x=new File(base,"sample"+i+".xlsx");Core.xlsxOut(x,input);yes(Core.xlsx(x).equals(input),"XLSX roundtrip "+i);}
        yes(Core.csv("",',').isEmpty(),"empty CSV");yes(Core.csv("\uFEFFa,b\r\nc,d\r\n",',').size()==2,"BOM CRLF");yes(Core.csv("a,b,",',').get(0).size()==3,"trailing field");yes(Core.csv("\"\"",',').get(0).get(0).equals(""),"quoted empty");
        rejects(()->Core.csv("\"not closed",','),"malformed quoted CSV");rejects(()->Core.csv("\"x\"junk",','),"CSV trailing junk");
        String text="中文与 emoji 😀\n<&>\"'\n末尾";File doc=new File(base,"document.docx");Core.docxOut(doc,text);yes(Core.docx(doc).equals(text+"\n"),"DOCX XML escaping / Chinese");
        File gb=new File(base,"gb.txt");try(OutputStream o=new FileOutputStream(gb)){o.write("你好，世界".getBytes("GB18030"));}yes(Core.text(gb,"auto").equals("你好，世界"),"GB18030 automatic");
        File utf=new File(base,"utf16.txt");try(OutputStream o=new FileOutputStream(utf)){o.write(new byte[]{(byte)255,(byte)254});o.write("中文".getBytes("UTF-16LE"));}yes(Core.text(utf,"auto").equals("中文"),"UTF16 BOM");
        yes(Core.pages("",5).equals(Arrays.asList(0,1,2,3,4)),"all pages");yes(Core.pages("1-3,3,5",5).equals(Arrays.asList(0,1,2,4)),"page union");yes(Core.pages("2，1",2).equals(Arrays.asList(1,0)),"Chinese comma and order");
        for(String s:new String[]{"0","6","3-1","1-6","1,,2","1-2-3","-1","1,"})rejects(()->Core.pages(s,5),"invalid pages "+s);
        for(String p:new String[]{"../escape","a/../../x","/root","C:\\x","\\root",".","x\0y"})rejects(()->Core.safe(base,p),"unsafe path "+p);
        yes(Core.safe(base,"中文/文件.txt").getCanonicalPath().startsWith(base.getCanonicalPath()+"/"),"normal archive path");
        rejects(()->Core.document("<!DOCTYPE x [<!ENTITY t SYSTEM 'file:///etc/passwd'>]><x>&t;</x>".getBytes("UTF-8")),"XML external entities rejected");
        File empty=f("empty","");yes(Core.hash(empty,"SHA-256",Core.NONE).equals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),"SHA empty");
        for(int i=1;i<20000;i++)yes(Core.colIndex(Core.col(i))==i-1,"column roundtrip "+i);
        rejects(()->Core.copy(new ByteArrayInputStream(new byte[10]),new ByteArrayOutputStream(),9,Core.NONE),"copy size cap");
        File csv=f("table.csv","姓名,编号,备注\r\n林,00123,\"你好,世界\"\r\n");
        Engine table=engine("table",new JSONObject());table.table(csv,"xlsx");yes(Core.xlsx(new File(table.root,"table.xlsx")).get(1).get(1).equals("00123"),"table string IDs survive");
        table.table(csv,"json");JSONArray a=new JSONArray(Core.text(new File(table.root,"table.json"),"UTF-8"));yes(a.getJSONArray(1).getString(2).equals("你好,世界"),"table JSON cells");
        File json=f("objects.json","[{\"姓名\":\"林\",\"值\":null},{\"姓名\":\"王\",\"新增\":\"yes\"}]");table.table(json,"csv");List<List<String>> tr=Core.csv(Core.text(new File(table.root,"objects.csv"),"UTF-8"),',');yes(tr.size()==3&&tr.get(0).contains("新增"),"object union columns");
        rejects(()->table.table(f("mixed.json","[{},[]]"),"csv"),"mixed JSON rejection");
        File chinese=f("中文.txt","你好，压缩包\n");File other=f("quotes.txt","a,b\n");
        Engine pack=engine("zip",new JSONObject().put("password","密钥-123"));pack.pack(Arrays.asList(chinese,other),"zip");
        Engine un=engine("unzip",new JSONObject().put("password","密钥-123"));un.unpack(new File(pack.root,"文件合集.zip"),new File(un.root,"expanded"));yes(Core.text(new File(un.root,"expanded/中文.txt"),"UTF-8").equals("你好，压缩包\n"),"AES ZIP Chinese roundtrip");
        rejects(()->engine("wrongzip",new JSONObject().put("password","wrong")).unpack(new File(pack.root,"文件合集.zip"),new File(base,"wrongzip/out")),"wrong ZIP password");
        Engine tar=engine("tar",new JSONObject());tar.pack(Arrays.asList(chinese,other),"tar.gz");Engine untar=engine("untar",new JSONObject());untar.unpack(new File(tar.root,"文件合集.tar.gz"),new File(untar.root,"out"));yes(Core.text(new File(untar.root,"out/中文.txt"),"UTF-8").equals("你好，压缩包\n"),"TAR.GZ roundtrip");
        File malicious=new File(base,"escape.zip");try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(malicious))){Core.zipEntry(z,"../escape.txt","bad");}rejects(()->engine("badzip",new JSONObject()).unpack(malicious,new File(base,"badzip/out")),"ZIP slip blocked");yes(!new File(base,"badzip/escape.txt").exists(),"no escaped file");
        File seven=new File(base,"sample.7z");try(SevenZOutputFile z=new SevenZOutputFile(seven)){SevenZArchiveEntry e=z.createArchiveEntry(chinese,"中文.txt");z.putArchiveEntry(e);z.write(Core.bytes(chinese,1000));z.closeArchiveEntry();}Engine uz=engine("seven",new JSONObject());uz.unpack(seven,new File(uz.root,"out"));yes(Core.text(new File(uz.root,"out/中文.txt"),"UTF-8").equals("你好，压缩包\n"),"7z roundtrip");
        File gz=new File(base,"中文.txt.gz");try(GZIPOutputStream z=new GZIPOutputStream(new FileOutputStream(gz))){z.write(Core.bytes(chinese,1000));}Engine ug=engine("gzip",new JSONObject());ug.unpack(gz,new File(ug.root,"out"));yes(Core.text(new File(ug.root,"out/中文.txt"),"UTF-8").equals("你好，压缩包\n"),"GZIP roundtrip");
        rejects(()->{Engine c=engine("cancel",new JSONObject());c.cancelled.set(true);c.pack(Arrays.asList(chinese),"tar.gz");},"cancellation");
        File collision=new File(base,"collision/中文.txt");collision.getParentFile().mkdirs();Core.write(collision,"second");Engine dup=engine("duplicatezip",new JSONObject());dup.pack(Arrays.asList(chinese,collision),"zip");Engine ud=engine("duplicated",new JSONObject());ud.unpack(new File(dup.root,"文件合集.zip"),new File(ud.root,"out"));yes(new File(ud.root,"out/中文.txt").exists()&&new File(ud.root,"out/中文 (2).txt").exists(),"duplicate filenames preserved");
        Engine pdf=engine("pdf",new JSONObject());pdf.merge(Arrays.asList(new File("tests/fixtures/doc1.pdf"),new File("tests/fixtures/doc2.pdf")));try(com.tom_roush.pdfbox.pdmodel.PDDocument merged=pdf.load(new File(pdf.root,"合并文档.pdf"))){yes(merged.getNumberOfPages()==4,"merged PDF pages");yes(merged.getPage(0).getResources().getXObjectNames().iterator().hasNext(),"PDF image resources retained");}
        Engine split=engine("splitpdf",new JSONObject().put("pages","2"));split.split(new File("tests/fixtures/doc1.pdf"));yes(new File(split.root,"doc1-p2.pdf").isFile(),"PDF split page");
        Engine rar=engine("rar",new JSONObject());rar.unpack(new File("tests/fixtures/rar4.rar"),new File(rar.root,"out"));yes(new File(rar.root,"out").listFiles().length>0,"RAR4 upstream fixture extraction");
        yes(engine("tarroot",new JSONObject()).archivePath(new File(base,"tarroot"),"./",true)==null,"normal TAR root entry accepted");
        UiEventGate gate=new UiEventGate(120);int accepted=0;for(int tick=0;tick<4000;tick++)if(gate.allow(tick%100,"page "+tick,tick))accepted++;yes(accepted==34,"progress frequency bounded without polling");yes(gate.allow(100,"done",4000),"terminal progress is never delayed");yes(!gate.allow(100,"done",5000),"duplicate terminal progress suppressed");gate.reset();yes(gate.allow(-1,"import",5001),"new task begins immediately after reset");
        File large=new File(base,"large.bin");byte[] noise=new byte[65536];new Random(48).nextBytes(noise);try(OutputStream o=new FileOutputStream(large)){for(int i=0;i<128;i++)o.write(noise);}
        AtomicBoolean stopZip=new AtomicBoolean();Engine interruptZip=new Engine(null,new File(base,"zipcancel"),new JSONObject(),stopZip,(m,v)->{if(v>0&&v<100)stopZip.set(true);});interruptZip.root.mkdirs();
        rejects(()->interruptZip.pack(Arrays.asList(large),"zip"),"cancel inside single ZIP input stream");yes(stopZip.get(),"ZIP exposes actual byte progress before file finishes");
        System.out.println("PASS "+count+" assertions");Core.write(new File(base,"result.json"),new JSONObject().put("assertions",count).put("status","passed").toString(2));
    }
}
