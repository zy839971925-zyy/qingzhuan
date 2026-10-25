package cn.qingzhuan.app;
import com.facebook.react.*;import com.facebook.react.bridge.*;import com.facebook.react.uimanager.*;import java.util.*;
public final class QingzhuanPackage implements ReactPackage {
 @Override public List<NativeModule> createNativeModules(ReactApplicationContext c){return Arrays.asList(new QingzhuanModule(c));}
 @Override public List<ViewManager> createViewManagers(ReactApplicationContext c){return Collections.emptyList();}
}
