package cn.qingzhuan.app;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;
import org.json.*;

/** Owns task liveness/notification, not conversion state. AppSession remains the single task owner. */
public final class ConversionService extends Service {
    private static final String CHANNEL="conversion-progress";
    private static final int NOTIFICATION=152;
    private AppSession session;private PowerManager.WakeLock wake;
    private final Handler main=new Handler(Looper.getMainLooper());
    private String lastTask="";private boolean watching=false;
    private final AppSession.Listener observer=snapshot->main.post(()->refresh(snapshot));
    public static void start(Context context,String id,boolean media){
        Intent intent=new Intent(context,ConversionService.class).putExtra("task",id).putExtra("media",media);
        context.startForegroundService(intent);
    }
    @Override public void onCreate(){super.onCreate();NotificationChannel channel=new NotificationChannel(CHANNEL,AppText.text(this,"转换进度"),NotificationManager.IMPORTANCE_LOW);channel.setDescription(AppText.text(this,"正在进行的本地转换、导入和保存任务"));getSystemService(NotificationManager.class).createNotificationChannel(channel);}
    private Notification notification(String title,String text,int value,boolean active){
        Intent open=new Intent(this,StartupActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("showTask",lastTask);
        PendingIntent content=PendingIntent.getActivity(this,152,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.qz_notification).setContentTitle(AppText.text(this,title)).setContentText(AppText.text(this,text)).setContentIntent(content).setOnlyAlertOnce(true).setOngoing(active).setAutoCancel(!active).setCategory(Notification.CATEGORY_PROGRESS);
        if(active){b.setProgress(100,Math.max(0,value),value<0);Intent cancel=new Intent(this,ConversionService.class).setAction("cancel").putExtra("task",lastTask);b.addAction(new Notification.Action.Builder(null,AppText.text(this,"取消"),PendingIntent.getService(this,153,cancel,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).build());}
        return b.build();
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null){stopSelf(startId);return START_NOT_STICKY;}
        lastTask=intent.getStringExtra("task");if(lastTask==null)lastTask="";
        int type=Build.VERSION.SDK_INT>=35&&intent.getBooleanExtra("media",false)?ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING:ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
        Notification initial=notification("轻转正在处理","可切到后台，任务会继续",-1,true);
        try{if(Build.VERSION.SDK_INT>=29)startForeground(NOTIFICATION,initial,type);else startForeground(NOTIFICATION,initial);}
        catch(RuntimeException e){session=AppSession.get(this);session.backgroundUnavailable(lastTask,"系统未允许启动后台转换服务："+AppSession.message(e));stopSelf(startId);return START_NOT_STICKY;}
        session=AppSession.get(this);
        if("cancel".equals(intent.getAction()))session.cancel(lastTask);
        if(wake==null){wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Qingzhuan:conversion");wake.acquire(6L*60*60*1000);}
        if(!watching){watching=true;session.attach(observer);}else refresh(session.snapshot());
        return START_NOT_STICKY;
    }
    private void refresh(JSONObject previous){
        if(!watching)return;
        JSONObject snapshot=session.snapshot();
        JSONObject task=snapshot.optJSONObject("task");if(task==null)return;
        lastTask=task.optString("id");boolean busy=snapshot.optBoolean("busy");String status=task.optString("status");
        NotificationManager manager=getSystemService(NotificationManager.class);
        if(busy)manager.notify(NOTIFICATION,notification("轻转正在处理",task.optString("message"),task.optInt("value",-1),true));
        else{
            // Detach before stop; do not consume or reset the actual task/result in AppSession.
            watching=false;session.detach(observer);stopForeground(STOP_FOREGROUND_REMOVE);
            if(!lastTask.isEmpty())manager.notify(NOTIFICATION,notification(status.equals("complete")?"轻转处理完成":"轻转任务已结束",task.optString("message"),task.optInt("value",-1),false));
            stopSelf();
        }
    }
    @Override public void onTimeout(int startId,int foregroundServiceType){if(session!=null)session.backgroundUnavailable(lastTask,"系统后台处理时限已到，任务已中止，原文件保留");stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
    @Override public void onDestroy(){watching=false;if(session!=null)session.detach(observer);main.removeCallbacksAndMessages(null);if(wake!=null&&wake.isHeld())wake.release();wake=null;super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
