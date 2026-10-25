package com.tom_roush.pdfbox.android;
import java.io.*;
/** JVM-only adapter: supplies the exact packaged PDFBox assets, without emulating Android graphics. */
public final class PDFBoxResourceLoader {
 public static void init(android.content.Context c){}
 public static InputStream getStream(String path)throws IOException{return new FileInputStream(new File("app/src/main/assets",path));}
 public static boolean isReady(){return true;}
}
