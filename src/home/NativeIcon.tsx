import React from 'react';
import {Canvas} from '@shopify/react-native-skia';
import {CanvasIcon,type IconName} from './CanvasIcon';
export function NativeIcon({name,color,size=22}:{name:IconName;color:string;size?:number}){return <Canvas pointerEvents="none" style={{width:size,height:size}}><CanvasIcon name={name} x={0} y={0} size={size} color={color}/></Canvas>;}
