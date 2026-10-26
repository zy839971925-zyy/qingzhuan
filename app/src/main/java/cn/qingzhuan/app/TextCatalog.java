package cn.qingzhuan.app;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.*;

/** Pure, immutable catalog with precompiled patterns and a bounded presentation cache. */
public final class TextCatalog {
    private final Map<String,String> exact;
    private final List<Pattern> patterns=new ArrayList<>();
    private final List<String> templates=new ArrayList<>();
    private final List<Boolean> causes=new ArrayList<>();
    private final ConcurrentHashMap<String,String> cache=new ConcurrentHashMap<>();
    public TextCatalog(Map<String,String> entries,List<String[]> rules){
        exact=Collections.unmodifiableMap(new HashMap<>(entries));
        for(String[] rule:rules){patterns.add(Pattern.compile(rule[0]));templates.add(rule[1]);causes.add(rule.length>2&&rule[2].equals("cause"));}
    }
    public String translate(String value){return translate(value,0);}
    private String translate(String value,int depth){
        if(value==null)return "";
        if(depth>4)return value;
        String result=exact.get(value);if(result!=null)return result;
        result=cache.get(value);if(result!=null)return result;
        result=value;
        for(int i=0;i<patterns.size();i++){
            Matcher m=patterns.get(i).matcher(value);if(!m.matches())continue;
            result=templates.get(i);
            for(int g=m.groupCount();g>=1;g--){String part=m.group(g)==null?"":m.group(g);if(causes.get(i))part=translate(part,depth+1);result=result.replace("{"+g+"}",part);}
            break;
        }
        if(cache.size()>512)cache.clear();
        cache.put(value,result);return result;
    }
}
