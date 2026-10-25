/** Text reveal derived from copy.tsx's .34 positional ramp and 26 blur,
 * GPL-3.0-only, modified for Chinese graphemes and same-canvas optical composition. */
import React from 'react';
import {Blur,Group,Paint,Paragraph,Skia,TextAlign} from '@shopify/react-native-skia';
import {Easing,useDerivedValue,useSharedValue,withDelay,withTiming,type SharedValue} from 'react-native-reanimated';
import {segment} from './motionMath';
export function CanvasText({text,x,y,width,size=14,color,center=false,weight=400}:{text:string;x:number;y:number;width:number;size?:number;color:string;center?:boolean;weight?:number}){
 const paragraph=React.useMemo(()=>{const p=Skia.ParagraphBuilder.Make({textAlign:center?TextAlign.Center:TextAlign.Left}).pushStyle({color:Skia.Color(color),fontFamilies:['sans-serif'],fontSize:size,fontStyle:{weight}}).addText(text).pop().build();p.layout(width);return p;},[text,width,size,color,center,weight]);
 return <Paragraph paragraph={paragraph} x={x} y={y} width={width}/>;
}
function Glyph({text,index,count,x,y,width,size,color,progress}:{text:string;index:number;count:number;x:number;y:number;width:number;size:number;color:string;progress:SharedValue<number>}){
 const alpha=useDerivedValue(()=>{const front=-.34+progress.value*1.68;return Math.max(0,Math.min(1,(front-(index+.5)/count)/.34+.5));});
 const blur=useDerivedValue(()=>(1-alpha.value)*26);
 const transform=useDerivedValue(()=>[{translateY:(1-alpha.value)*9},{scaleY:.84+.16*alpha.value}]);
 return <Group transform={transform} origin={{x:x+width/2,y:y+size/2}} layer={<Paint opacity={alpha}><Blur blur={blur}/></Paint>}><CanvasText text={text} x={x} y={y} width={width} size={size} color={color} center/></Group>;
}
export function RevealText({text,x,y,width,color,size=15,delay=0,reduced=false}:{text:string;x:number;y:number;width:number;color:string;size?:number;delay?:number;reduced?:boolean}){
 const parts=React.useMemo(()=>segment(text),[text]);const progress=useSharedValue(reduced?1:0);
 React.useEffect(()=>{progress.value=reduced?1:0;progress.value=reduced?1:withDelay(delay,withTiming(1,{duration:560,easing:Easing.bezier(.16,.42,.40,1)}));},[text,reduced,delay]);
 const cell=Math.min(size*1.1,width/Math.max(1,parts.length));const start=x+(width-cell*parts.length)/2;
 return <Group>{parts.map((s,i)=><Glyph key={`${s}-${i}`} text={s} index={i} count={parts.length} x={start+i*cell} y={y} width={cell+2} size={size} color={color} progress={progress}/>)}</Group>;
}
