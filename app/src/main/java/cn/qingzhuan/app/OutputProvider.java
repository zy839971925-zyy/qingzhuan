package cn.qingzhuan.app;
import android.content.*;import android.net.Uri;import android.database.*;import android.os.*;import android.provider.OpenableColumns;import android.webkit.MimeTypeMap;import java.io.*;
/** Read-only, explicitly granted output URIs. No directory or arbitrary input access. */
public class OutputProvider extends ContentProvider {
    @Override public boolean onCreate(){return true;}
    File file(Uri u)throws FileNotFoundException{try{File f=Core.safe(new File(getContext().getFilesDir(),"results"),u.getPath().substring(1));if(!f.isFile())throw new IOException();return f;}catch(Exception e){throw new FileNotFoundException("文件不存在");}}
    public static String mime(String name){String ext=Core.ext(name);String m=MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);if(m!=null)return m;switch(ext){case "md":case "log":return "text/plain";case "json":return "application/json";case "csv":return "text/csv";case "tsv":return "text/tab-separated-values";case "7z":return "application/x-7z-compressed";default:return "application/octet-stream";}}
    @Override public String getType(Uri u){return mime(u.getLastPathSegment());}
    @Override public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException("只读文件");return ParcelFileDescriptor.open(file(u),ParcelFileDescriptor.MODE_READ_ONLY);}
    @Override public Cursor query(Uri u,String[] projection,String selection,String[] args,String sort){try{File f=file(u);String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor c=new MatrixCursor(columns);Object[] values=new Object[columns.length];for(int i=0;i<columns.length;i++){if(OpenableColumns.DISPLAY_NAME.equals(columns[i]))values[i]=f.getName();else if(OpenableColumns.SIZE.equals(columns[i]))values[i]=f.length();}c.addRow(values);return c;}catch(Exception e){return null;}}
    @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}@Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}@Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
