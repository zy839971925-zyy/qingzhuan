package cn.qingzhuan.app;
import java.util.*;
/** A displayed batch is processed as a whole; never silently omit unsupported members. */
public final class ToolRouting{
 public static Set<String> common(Collection<? extends Collection<String>> files){Set<String> result=new LinkedHashSet<>();boolean first=true;for(Collection<String> tools:files){if(first){result.addAll(tools);first=false;}else result.retainAll(tools);}return result;}
}
