/** Geometry and rubber-band behavior derived from Appllama Liquid Glass Screens.
 * GPL-3.0-only. Modified 2026-10-25: strict release gate replaces velocity submission. */
export const SPRING={damping:15,stiffness:120,mass:1.05};
export const REDUCED_SPRING={damping:30,stiffness:160,mass:1};
export const COMMIT_THRESHOLD=.92;
export function rubber(raw:number):number{'worklet';return raw<0?raw*.25:raw>1?1+(raw-1)*.25:raw;}
export function shouldCommit(p:number,released:boolean,busy:boolean,dx=0,dy=0,centralRadius=Infinity):boolean{'worklet';return released&&!busy&&Number.isFinite(p)&&p>=COMMIT_THRESHOLD&&Math.hypot(dx,dy)<=centralRadius;}
export function geometry(w:number,h:number,p:number){'worklet';const sx=w/402,sy=h/874,R0=245*sx,R1=44*sx,RF=32*sx,travel=h*(1-.469);return{r:p<=1?R0+(R1-R0)*p:RF+(R1-RF)*Math.exp(-(p-1)*travel/(40*sy)),y:h+(h*.469-h)*p,travel};}
export function segment(s:string):string[]{const S=(Intl as unknown as {Segmenter?:new(locale:string,options:object)=>{segment:(v:string)=>Iterable<{segment:string}>}}).Segmenter;return S?Array.from(new S('zh',{granularity:'grapheme'}).segment(s),x=>x.segment):Array.from(s);}
export function acceptSnapshot(previous:{revision:number;task?:{seq:number}},next:{revision:number;task?:{seq:number}}):boolean{return next.revision>=previous.revision&&(next.task?.seq??0)>=(previous.task?.seq??0);}
