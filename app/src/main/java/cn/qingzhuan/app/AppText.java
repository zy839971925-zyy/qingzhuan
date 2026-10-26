package cn.qingzhuan.app;

import android.app.LocaleManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import org.json.*;
import java.util.*;
import java.util.regex.*;
import java.nio.charset.StandardCharsets;

/** Localizes presentation only. Filenames, document content and session/engine data stay untouched. */
public final class AppText {
    private static volatile TextCatalog catalog;
    public static String mode(Context c){
        if(Build.VERSION.SDK_INT>=33){LocaleList locales=c.getSystemService(LocaleManager.class).getApplicationLocales();return locales.isEmpty()?"system":(locales.get(0).getLanguage().equals("zh")?"zh":"en");}
        return c.getSharedPreferences("appearance",0).getString("language","system");
    }
    public static boolean english(Context c){
        String mode=mode(c);
        return mode.equals("en")||mode.equals("system")&&!Resources.getSystem().getConfiguration().getLocales().get(0).getLanguage().equals("zh");
    }
    public static Context localizedContext(Context c){
        android.content.res.Configuration config=new android.content.res.Configuration(c.getResources().getConfiguration());
        config.setLocale(Locale.forLanguageTag(english(c)?"en":"zh"));return c.createConfigurationContext(config);
    }
    public static void setLanguage(Context c,String value){
        if(!Arrays.asList("system","zh","en").contains(value))return;
        c.getSharedPreferences("appearance",0).edit().putString("language",value).apply();
        if(Build.VERSION.SDK_INT>=33)c.getSystemService(LocaleManager.class).setApplicationLocales(value.equals("system")?LocaleList.getEmptyLocaleList():LocaleList.forLanguageTags(value));
    }
    public static synchronized void load(Context c){
        if(catalog!=null)return;
        try(java.io.InputStream in=c.getAssets().open("strings_en.json")){
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
            while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);
            JSONObject json=new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
            Map<String,String> entries=new HashMap<>();JSONObject exact=json.getJSONObject("exact");
            Iterator<String> keys=exact.keys();while(keys.hasNext()){String key=keys.next();entries.put(key,exact.getString(key));}
            List<String[]> rules=new ArrayList<>();JSONArray data=json.getJSONArray("rules");
            for(int i=0;i<data.length();i++){JSONObject rule=data.getJSONObject(i);rules.add(new String[]{rule.getString("re"),rule.getString("en"),rule.optBoolean("cause",false)?"cause":""});}
            catalog=new TextCatalog(entries,rules);
        }catch(Exception error){catalog=new TextCatalog(Collections.emptyMap(),Collections.emptyList());}
    }

    public static String text(Context c,String value){load(c);return translate(value,english(c));}
    public static String translate(String value,boolean english){
        if(value==null)return "";
        if(!english||value.isEmpty()||catalog==null)return value;
        return catalog.translate(value);
    }
}
