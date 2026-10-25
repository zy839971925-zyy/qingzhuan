import React from 'react';
import {Group,RoundedRect} from '@shopify/react-native-skia';
import {CanvasIcon,type IconName} from './CanvasIcon';
import {CanvasText} from './CanvasText';
import {theme} from './theme';
export function fileGlyph(name:string,kind?:string):IconName{const ext=name.split('.').pop()?.toLowerCase();if(kind==='image'||['png','jpg','jpeg','webp','heic','gif','avif'].includes(ext||''))return 'file-image';if(kind==='archive'||['zip','gz','rar','7z','tar'].includes(ext||''))return 'file-archive';if(kind==='media'||['wav','mp3','m4a','aac','mp4'].includes(ext||''))return 'file-audio';return 'file-text';}
export function FileEmblem({name,kind,x,y,size=88,dark}:{name:string;kind?:string;x:number;y:number;size?:number;dark:boolean}){const c=theme(dark);return <Group><RoundedRect x={x} y={y+3} width={size} height={size} r={size*.23} color={c.soft}/><RoundedRect x={x+3} y={y} width={size-6} height={size-6} r={size*.21} color={c.paper}/><CanvasIcon name={fileGlyph(name,kind)} x={x+size*.23} y={y+size*.12} size={size*.54} color={c.accent}/><CanvasText text={(name.split('.').pop()||'FILE').toUpperCase().slice(0,7)} x={x+4} y={y+size*.69} width={size-8} size={Math.min(11,size*.13)} color={c.accent} center weight={600}/></Group>;}
