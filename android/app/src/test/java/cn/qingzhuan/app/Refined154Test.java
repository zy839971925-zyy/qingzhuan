package cn.qingzhuan.app;

import android.app.Application;
import android.graphics.*;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

/** Two narrow Android/JVM checks, without starting an Activity or an emulator. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
public class Refined154Test {
    @Test public void realImageEngineDoesNotApplyLegacyEdgeWhenAdvancedIsOff() throws Exception {
        Application app=RuntimeEnvironment.getApplication();File dir=new File(app.getCacheDir(),"refined-image");dir.mkdirs();
        File input=new File(dir,"original.png");Bitmap original=Bitmap.createBitmap(5000,20,Bitmap.Config.ARGB_8888);
        try(OutputStream out=new FileOutputStream(input)){assertTrue(original.compress(Bitmap.CompressFormat.PNG,100,out));}original.recycle();
        File output=new File(dir,"output");
        JSONObject options=new JSONObject().put("target","png").put("edge",2048).put("quality",20).put("imageAdvanced",false);
        Engine engine=new Engine(app,output,options,new AtomicBoolean(),(text,value)->{});
        assertEquals(100,engine.qualityFor(true));assertEquals(20,engine.qualityFor(false));
        JSONObject advanced=new JSONObject().put("imageAdvanced",true).put("imageQuality",20).put("quality",99);
        Engine other=new Engine(app,output,advanced,new AtomicBoolean(),(text,value)->{});
        assertEquals(20,other.qualityFor(true));assertEquals(99,other.qualityFor(false));
        engine.run("image",Collections.singletonList(input));
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeFile(new File(output,"original.png").getPath(),bounds);
        assertEquals(5000,bounds.outWidth);assertEquals(20,bounds.outHeight);assertTrue(input.isFile());
    }
    @Test public void manualLanguageUsesActualCatalogAndPreservesFileNamesOnAndroid28(){
        Application app=RuntimeEnvironment.getApplication();
        AppText.setLanguage(app,"en");assertTrue(AppText.english(app));
        assertEquals("Swipe to convert",AppText.text(app,"滑动以开始转换"));
        assertEquals("Save 设置.png",AppText.text(app,"保存 设置.png"));
        AppText.setLanguage(app,"zh");assertFalse(AppText.english(app));
        assertEquals("滑动以开始转换",AppText.text(app,"滑动以开始转换"));
    }
}
