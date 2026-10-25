import React from 'react';
import {Circle,Group,Paint,Path,Skia} from '@shopify/react-native-skia';
import {useDerivedValue,useFrameCallback,useSharedValue,type SharedValue} from 'react-native-reanimated';
import {scheduleOnRN} from 'react-native-worklets';
import {stepFeedback} from './feedback';
export function RingFeedback({cx,cy,radius,p,active,finished,real,dark,onDone}:{cx:SharedValue<number>;cy:SharedValue<number>;radius:SharedValue<number>;p:SharedValue<number>;active:boolean;finished:boolean;real:number;dark:boolean;onDone:()=>void}){
 const elapsed=useSharedValue(0),fill=useSharedValue(0),check=useSharedValue(0),sent=useSharedValue(false);
 useFrameCallback(frame=>{if(!active||p.value<.94||sent.value)return;const next=stepFeedback({elapsed:elapsed.value,fill:fill.value,check:check.value,done:false},frame.timeSincePreviousFrame??16,real,finished);elapsed.value=next.elapsed;fill.value=next.fill;check.value=next.check;if(next.done){sent.value=true;scheduleOnRN(onDone);}});
 const arc=useDerivedValue(()=>{const r=radius.value*.56;const path=Skia.Path.Make();path.addArc(Skia.XYWHRect(cx.value-r,cy.value-r,2*r,2*r),-90,Math.max(2,fill.value*359.99));return path;});
 const hand=useDerivedValue(()=>{const a=-Math.PI/2+(elapsed.value%1000)/1000*Math.PI*2,r=radius.value*.32;const path=Skia.Path.Make();path.moveTo(cx.value,cy.value);path.lineTo(cx.value+Math.cos(a)*r,cy.value+Math.sin(a)*r);return path;});
 const tick=useDerivedValue(()=>{const r=radius.value,path=Skia.Path.Make();path.moveTo(cx.value-r*.25,cy.value);path.lineTo(cx.value-r*.055,cy.value+r*.19);path.lineTo(cx.value+r*.29,cy.value-r*.23);return path;});
 const ringRadius=useDerivedValue(()=>radius.value*.56),handAlpha=useDerivedValue(()=>1-Math.min(1,check.value/130)),checkAlpha=useDerivedValue(()=>Math.min(1,check.value/100)),drawCheck=useDerivedValue(()=>Math.min(1,check.value/220));
 const color=dark?'#F5F5F5':'#161616';
 return <><Group layer={<Paint opacity={handAlpha}/>}><Circle cx={cx} cy={cy} r={ringRadius} style="stroke" strokeWidth={2} color={dark?'#FFFFFF24':'#00000016'}/><Path path={arc} style="stroke" strokeWidth={2.5} strokeCap="round" color={color}/><Path path={hand} style="stroke" strokeWidth={1.6} strokeCap="round" color={color}/><Circle cx={cx} cy={cy} r={2} color={color}/></Group><Group layer={<Paint opacity={checkAlpha}/>}><Path path={tick} style="stroke" strokeWidth={3} strokeCap="round" strokeJoin="round" color={dark?'#6BD995':'#23864C'} end={drawCheck}/></Group></>;
}
