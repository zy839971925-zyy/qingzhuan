import React from 'react';
import {Group,Path} from '@shopify/react-native-skia';
import {ICON_PATHS} from './iconPaths';
export type IconName=keyof typeof ICON_PATHS;
/** Explicit geometry: no SVG DOM parsing, CSS inheritance or silently absent SVG. */
export function CanvasIcon({name,x,y,size=24,color}:{name:IconName;x:number;y:number;size?:number;color:string}){
 return <Group transform={[{translateX:x},{translateY:y},{scale:size/24}]}>{ICON_PATHS[name].map((d,i)=><Path key={i} path={d} color={color} style="stroke" strokeWidth={1.65} strokeCap="round" strokeJoin="round"/>)}</Group>;
}
