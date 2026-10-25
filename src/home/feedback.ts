/** Visual acknowledgement is independent of actual task progress; no fake percentages. */
export type FeedbackState={elapsed:number;fill:number;check:number;done:boolean};
export function stepFeedback(s:FeedbackState,frameMs:number,real:number,finished:boolean):FeedbackState {
 'worklet';
 const dt=Math.max(0,Math.min(48,frameMs)),elapsed=s.elapsed+dt;
 const entrance=1-Math.pow(1-Math.min(1,elapsed/1200),2.4);
 const target=finished?entrance:real>=0?Math.min(entrance,Math.max(0,Math.min(1,real/100))):Math.min(.9,.9*(1-Math.exp(-elapsed/2200)));
 let fill=s.fill+(Math.max(s.fill,target)-s.fill)*(1-Math.exp(-dt/95));
 if(finished&&elapsed>=1200&&fill>.996)fill=1;
 const check=finished&&fill===1?s.check+dt:0;
 return{elapsed,fill,check,done:check>=420};
}
