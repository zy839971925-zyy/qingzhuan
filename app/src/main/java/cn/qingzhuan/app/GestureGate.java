package cn.qingzhuan.app;
/** Same reversible linear path as liquid-glass-screens, all values in dp. */
public final class GestureGate {
 public static float radius(float width,float height,float p){float scale=width>height*1.2f?height/874:width/402;return (245-201*Math.max(0,Math.min(1,p)))*scale;}
 public static boolean commit(float progress,float dx,float dy,float radius){return Float.isFinite(progress)&&Float.isFinite(dx)&&Float.isFinite(dy)&&progress>=.92f&&Math.hypot(dx,dy)<=radius;}
}
