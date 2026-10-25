package cn.qingzhuan.app;

/** Coalesces duplicate/high-frequency progress without a timer or polling loop. */
public final class UiEventGate {
    private final long intervalMs;private boolean seen;private long lastAt;private int lastValue;private String lastMessage;
    public UiEventGate(long intervalMs){if(intervalMs<0)throw new IllegalArgumentException("intervalMs");this.intervalMs=intervalMs;}
    public synchronized boolean allow(int value,String message,long now){
        if(seen&&value==lastValue&&message.equals(lastMessage))return false;
        if(seen&&value!=100&&now-lastAt<intervalMs)return false;
        seen=true;lastAt=now;lastValue=value;lastMessage=message;return true;
    }
    public synchronized void reset(){seen=false;lastMessage=null;}
}
