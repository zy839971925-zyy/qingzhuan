package cn.qingzhuan.app;
/** Visible-frame clock; backend success and a complete visual cycle are both required. */
public final class FeedbackClock {
 public double fill,elapsed,check;public boolean done;
 public void step(double frameMs,int real,boolean finished){if(done)return;double dt=Math.max(0,Math.min(48,frameMs));elapsed+=dt;double intro=1-Math.pow(1-Math.min(1,elapsed/1200),2.4);double target=finished?intro:real>=0?Math.min(.92,Math.max(0,real/100.0))*intro:Math.min(.9,.9*(1-Math.exp(-elapsed/2200)))*intro;fill+=(target-fill)*(1-Math.exp(-dt/95));if(finished&&elapsed>=1200&&fill>.996){fill=1;check+=dt;done=check>=420;}}
}
