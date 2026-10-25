package cn.qingzhuan.app;

import java.io.*;
import java.nio.charset.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/** Offline formats; intentionally independent of Android for repeatable JVM tests. */
public final class Core {
    public static final long MAX_INPUT=512L*1024*1024, MAX_EXPANDED=1024L*1024*1024;
    public interface Check { void check() throws IOException; }
    public static final Check NONE=()->{};
    public static String ext(String n){int p=n.lastIndexOf('.');return p<0?"":n.substring(p+1).toLowerCase(Locale.ROOT);}
    public static String stem(String n){int p=n.lastIndexOf('.');return clean(p>0?n.substring(0,p):n);}
    public static String clean(String s){s=s.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]","_").trim();return s.length()==0?"文件":s.substring(0,Math.min(s.length(),120));}
    public static long copy(InputStream in,OutputStream out,long limit,Check check)throws IOException{
        byte[] b=new byte[65536];long total=0;int n;
        while((n=in.read(b))!=-1){check.check();total+=n;if(total>limit)throw new IOException("文件超过安全处理上限");out.write(b,0,n);}return total;
    }
    public static byte[] bytes(File f,long max)throws IOException{try(InputStream in=new FileInputStream(f);ByteArrayOutputStream out=new ByteArrayOutputStream()){copy(in,out,max,NONE);return out.toByteArray();}}
    public static String text(File f,String enc)throws IOException{
        byte[] b=bytes(f,16L*1024*1024);String cs=enc;
        int skip=0;
        if(b.length>=3&&(b[0]&255)==239&&(b[1]&255)==187&&(b[2]&255)==191){cs="UTF-8";skip=3;}
        else if(b.length>=2&&(b[0]&255)==255&&(b[1]&255)==254){cs="UTF-16LE";skip=2;}
        else if(b.length>=2&&(b[0]&255)==254&&(b[1]&255)==255){cs="UTF-16BE";skip=2;}
        else if(cs.equals("auto")){
            try{Charset.forName("UTF-8").newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(b));cs="UTF-8";}
            catch(CharacterCodingException e){cs="GB18030";}
        }
        return new String(b,skip,b.length-skip,Charset.forName(cs));
    }
    public static void write(File f,String s)throws IOException{try(OutputStream o=new FileOutputStream(f)){o.write(s.getBytes("UTF-8"));}}
    public static List<List<String>> csv(String s,char sep)throws IOException{
        if(s.startsWith("\uFEFF"))s=s.substring(1);
        List<List<String>> rows=new ArrayList<>();List<String> row=new ArrayList<>();StringBuilder v=new StringBuilder();
        boolean quoted=false,closed=false,atStart=true;
        for(int i=0;i<s.length();i++){
            char c=s.charAt(i);
            if(quoted){if(c=='"'){if(i+1<s.length()&&s.charAt(i+1)=='"'){v.append('"');i++;}else{quoted=false;closed=true;}}else v.append(c);}
            else if(c=='"'&&atStart){quoted=true;atStart=false;}
            else if(c==sep){row.add(v.toString());v.setLength(0);closed=false;atStart=true;}
            else if(c=='\r'||c=='\n'){
                if(c=='\r'&&i+1<s.length()&&s.charAt(i+1)=='\n')i++;
                row.add(v.toString());rows.add(row);row=new ArrayList<>();v.setLength(0);closed=false;atStart=true;
                if(rows.size()>100000)throw new IOException("表格超过十万行");
            }else{if(closed)throw new IOException("分隔文本的引号格式有误");v.append(c);atStart=false;}
        }
        if(quoted)throw new IOException("分隔文本的引号未闭合");
        if(v.length()>0||!row.isEmpty()||closed){row.add(v.toString());rows.add(row);}return rows;
    }
    public static String csvOut(List<List<String>> rows,char sep){StringBuilder s=new StringBuilder();for(List<String> r:rows){for(int i=0;i<r.size();i++){if(i>0)s.append(sep);String v=r.get(i);if(v.indexOf(sep)>=0||v.contains("\n")||v.contains("\r")||v.contains("\""))s.append('"').append(v.replace("\"","\"\"")).append('"');else s.append(v);}s.append("\r\n");}return s.toString();}
    public static String xml(String s){StringBuilder b=new StringBuilder();for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c=='&')b.append("&amp;");else if(c=='<')b.append("&lt;");else if(c=='>')b.append("&gt;");else if(c=='\"')b.append("&quot;");else if(c=='\'')b.append("&apos;");else if(c>=32||c=='\n'||c=='\r'||c=='\t')b.append(c);}return b.toString();}
    public static Document document(byte[] b)throws Exception{
        String head=new String(b,"UTF-8");if(head.toUpperCase(Locale.ROOT).contains("<!DOCTYPE")||head.toUpperCase(Locale.ROOT).contains("<!ENTITY")||head.indexOf('\0')>=0)throw new IOException("文档包含不允许的 XML 声明");
        DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);f.setExpandEntityReferences(false);return f.newDocumentBuilder().parse(new ByteArrayInputStream(b));
    }
    public static byte[] entry(ZipFile z,String path)throws IOException{ZipEntry e=z.getEntry(path);if(e==null)throw new IOException("文档缺少 "+path);try(InputStream in=z.getInputStream(e);ByteArrayOutputStream o=new ByteArrayOutputStream()){copy(in,o,32L*1024*1024,NONE);return o.toByteArray();}}
    public static String docx(File f)throws Exception{
        try(ZipFile z=new ZipFile(f)){Document d=document(entry(z,"word/document.xml"));StringBuilder s=new StringBuilder();NodeList p=d.getElementsByTagNameNS("*","p");for(int i=0;i<p.getLength();i++){walkText(p.item(i),s);s.append('\n');}return s.toString();}
    }
    private static void walkText(Node n,StringBuilder s){String k=n.getLocalName();if("t".equals(k))s.append(n.getTextContent());else if("tab".equals(k))s.append('\t');else if("br".equals(k)||"cr".equals(k))s.append('\n');else for(Node c=n.getFirstChild();c!=null;c=c.getNextSibling())walkText(c,s);}
    public static void zipEntry(ZipOutputStream z,String path,String s)throws IOException{z.putNextEntry(new ZipEntry(path));z.write(s.getBytes("UTF-8"));z.closeEntry();}
    public static void docxOut(File f,String text)throws IOException{
        try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(f))){
            zipEntry(z,"[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>");
            zipEntry(z,"_rels/.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/></Relationships>");
            StringBuilder s=new StringBuilder("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>");
            for(String p:text.split("\\R",-1))s.append("<w:p><w:r><w:t xml:space=\"preserve\">").append(xml(p)).append("</w:t></w:r></w:p>");
            s.append("<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/><w:pgMar w:top=\"1134\" w:bottom=\"1134\" w:left=\"1134\" w:right=\"1134\"/></w:sectPr></w:body></w:document>");zipEntry(z,"word/document.xml",s.toString());
        }
    }
    public static String col(int c){StringBuilder s=new StringBuilder();while(c>0){c--;s.insert(0,(char)('A'+c%26));c/=26;}return s.toString();}
    public static int colIndex(String ref){int n=0;for(int i=0;i<ref.length();i++){char c=ref.charAt(i);if(c>='A'&&c<='Z')n=n*26+c-'A'+1;else if(c>='a'&&c<='z')n=n*26+c-'a'+1;else break;}return n-1;}
    public static void xlsxOut(File f,List<List<String>> rows)throws IOException{
        try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(f))){
            zipEntry(z,"[Content_Types].xml","<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
            zipEntry(z,"_rels/.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            zipEntry(z,"xl/workbook.xml","<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"轻转\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            zipEntry(z,"xl/_rels/workbook.xml.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
            z.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));Writer w=new OutputStreamWriter(z,"UTF-8");w.write("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
            for(int r=0;r<rows.size();r++){w.write("<row r=\""+(r+1)+"\">");List<String> row=rows.get(r);if(row.size()>16384)throw new IOException("表格列数过多");for(int c=0;c<row.size();c++){String v=row.get(c);if(v.length()>32767)throw new IOException("单元格文字过长");w.write("<c r=\""+col(c+1)+(r+1)+"\" t=\"inlineStr\"><is><t xml:space=\"preserve\">"+xml(v)+"</t></is></c>");}w.write("</row>");}w.write("</sheetData></worksheet>");w.flush();z.closeEntry();
        }
    }
    public static List<List<String>> xlsx(File f)throws Exception{
        try(ZipFile z=new ZipFile(f)){
            List<String> shared=new ArrayList<>();if(z.getEntry("xl/sharedStrings.xml")!=null){Document d=document(entry(z,"xl/sharedStrings.xml"));NodeList ss=d.getElementsByTagNameNS("*","si");for(int i=0;i<ss.getLength();i++){StringBuilder b=new StringBuilder();NodeList ts=((Element)ss.item(i)).getElementsByTagNameNS("*","t");for(int j=0;j<ts.getLength();j++)b.append(ts.item(j).getTextContent());shared.add(b.toString());}}
            Document wb=document(entry(z,"xl/workbook.xml"));NodeList sheets=wb.getElementsByTagNameNS("*","sheet");if(sheets.getLength()==0)throw new IOException("表格没有工作表");Element sh=(Element)sheets.item(0);String id=sh.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships","id");
            Document rel=document(entry(z,"xl/_rels/workbook.xml.rels"));String target=null;NodeList links=rel.getElementsByTagNameNS("*","Relationship");for(int i=0;i<links.getLength();i++){Element e=(Element)links.item(i);if(e.getAttribute("Id").equals(id))target=e.getAttribute("Target");}
            if(target==null||target.contains("..")||target.contains(":"))throw new IOException("不支持的工作表路径");String p=target.startsWith("/")?target.substring(1):"xl/"+target;
            Document d=document(entry(z,p));NodeList rs=d.getElementsByTagNameNS("*","row");List<List<String>> rows=new ArrayList<>();int cells=0;
            for(int i=0;i<rs.getLength();i++){
                Element row=(Element)rs.item(i);int rn=row.hasAttribute("r")?Integer.parseInt(row.getAttribute("r")):rows.size()+1;if(rn<1||rn>100000||rn<=rows.size())throw new IOException("表格行号不受支持");while(rows.size()<rn-1)rows.add(new ArrayList<>());
                List<String> out=new ArrayList<>();NodeList cs=row.getElementsByTagNameNS("*","c");
                for(int j=0;j<cs.getLength();j++){
                    if(++cells>500000)throw new IOException("表格单元格过多");Element c=(Element)cs.item(j);int ix=c.hasAttribute("r")?colIndex(c.getAttribute("r")):out.size();if(ix<0||ix>=16384||ix<out.size())throw new IOException("表格列号不受支持");while(out.size()<ix)out.add("");
                    String type=c.getAttribute("t"),v="";if(type.equals("inlineStr")){NodeList ts=c.getElementsByTagNameNS("*","t");for(int k=0;k<ts.getLength();k++)v+=ts.item(k).getTextContent();}else{NodeList vs=c.getElementsByTagNameNS("*","v");if(vs.getLength()>0)v=vs.item(0).getTextContent();if(type.equals("s")&&!v.isEmpty()){int si=Integer.parseInt(v);if(si<0||si>=shared.size())throw new IOException("表格共享字符串有误");v=shared.get(si);}else if(type.equals("b"))v=v.equals("1")?"TRUE":"FALSE";}out.add(v);
                }rows.add(out);
            }return rows;
        }
    }
    public static List<Integer> pages(String spec,int total)throws IOException{
        LinkedHashSet<Integer> a=new LinkedHashSet<>();if(spec.trim().isEmpty()){for(int i=0;i<total;i++)a.add(i);return new ArrayList<>(a);}
        for(String part:spec.replace('，',',').split(",",-1)){
            String[] b=part.trim().split("-",-1);try{int start=Integer.parseInt(b[0].trim()),end=b.length==2?Integer.parseInt(b[1].trim()):start;if(b.length>2||start<1||end<start||end>total)throw new Exception();for(int i=start;i<=end;i++)a.add(i-1);}catch(Exception e){throw new IOException("页码有误，请使用 1-3,5 且不超出文档范围");}
        }return new ArrayList<>(a);
    }
    public static File safe(File root,String path)throws IOException{
        if(path.indexOf('\0')>=0||path.startsWith("/")||path.startsWith("\\")||path.matches("^[a-zA-Z]:.*"))throw new IOException("压缩包包含危险路径");
        File f=new File(root,path.replace('\\','/'));if(!f.getCanonicalPath().startsWith(root.getCanonicalPath()+File.separator))throw new IOException("压缩包路径越界");return f;
    }
    public static String hash(File f,String algorithm,Check ck)throws Exception{MessageDigest d=MessageDigest.getInstance(algorithm);try(InputStream in=new FileInputStream(f)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){ck.check();d.update(b,0,n);}}StringBuilder s=new StringBuilder();for(byte b:d.digest())s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
}
