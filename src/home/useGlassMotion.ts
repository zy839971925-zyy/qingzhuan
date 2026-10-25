/** Derived from Appllama's liquid-glass-screen.tsx, GPL-3.0-only.
 * Modified 2026-10-25 for native file conversion: strict release gate, task lock,
 * interruption handling, lifecycle and shared scene transitions. No Expo dependency. */
import React from 'react';
import {Gesture} from 'react-native-gesture-handler';
import {Skia} from '@shopify/react-native-skia';
import {cancelAnimation,Extrapolation,interpolate,useDerivedValue,useFrameCallback,useSharedValue,withSpring,withTiming} from 'react-native-reanimated';
import {scheduleOnRN} from 'react-native-worklets';
import {SPRING,REDUCED_SPRING,rubber,shouldCommit} from './motionMath';

function clampProgress(p:number){'worklet';return Math.max(0,Math.min(1,p));}

type Props={width:number;height:number;resetVersion:number;armed:boolean;interactive:boolean;busy:boolean;active:boolean;reduced:boolean;dark:boolean;onSubmit:()=>void};
export function useGlassMotion({width,height,resetVersion,armed,interactive,busy,active,reduced,dark,onSubmit}:Props){
 const sx=width>height*1.2?height/874:width/402,sy=height/874,CX=width/2,CY0=height,CY1=height*.469,R0=245*sx,R1=44*sx,RF=32*sx,TRAVEL=CY0-CY1;
 const p=useSharedValue(busy?1:0),p0=useSharedValue(0),cxOff=useSharedValue(0),cx0=useSharedValue(0),locked=useSharedValue(busy?1:0);
 const visibility=useSharedValue(armed||busy?1:0);
 const touchOn=useSharedValue(0),glow=useSharedValue(0),dirX=useSharedValue(0),dirY=useSharedValue(-1),sloshX=useSharedValue(0),sloshY=useSharedValue(0);
 const prevCy=useSharedValue(CY0),prevCx=useSharedValue(0),prevV=useSharedValue(0),fvx=useSharedValue(0),fvy=useSharedValue(0),warm=useSharedValue(0);
 const cy=useDerivedValue(()=>interpolate(p.value,[0,1],[CY0,CY1])+(1-visibility.value)*R0*1.35*(1-clampProgress(p.value)));const cx=useDerivedValue(()=>CX+cxOff.value);
 const radius=useDerivedValue(()=>p.value<=1?R0+(R1-R0)*p.value:RF+(R1-RF)*Math.exp(-(p.value-1)*TRAVEL/(40*sy)));
 const spring=reduced?REDUCED_SPRING:SPRING;
 const animateHome=React.useCallback(()=>{touchOn.value=0;cxOff.value=reduced?0:withSpring(0,spring);p.value=reduced?0:withSpring(0,spring);},[cxOff,p,reduced,spring,touchOn]);
 React.useEffect(()=>{locked.value=busy?1:0;if(busy){touchOn.value=0;p.value=reduced?1:withSpring(1,spring);cxOff.value=withSpring(0,spring);}else animateHome();},[busy]); // one transition per native task boundary; progress does not restart the spring
 React.useEffect(()=>{if(!busy){locked.value=0;animateHome();}},[resetVersion]);
 React.useEffect(()=>{touchOn.value=0;warm.value=0;cancelAnimation(p);cancelAnimation(cxOff);locked.value=busy?1:0;p.value=busy?1:0;cxOff.value=0;prevCy.value=busy?CY1:CY0;prevCx.value=0;prevV.value=0;fvx.value=0;fvy.value=0;},[width,height,active,reduced]);
 useFrameCallback(info=>{
  if(!active||(!armed&&!busy))return;
  const dt=Math.max(1,Math.min(48,info.timeSincePreviousFrame??16))/1000;
  if(warm.value<4){warm.value+=1;prevCy.value=cy.value;prevCx.value=cxOff.value;return;}
  const orbVy=(cy.value-prevCy.value)/dt,orbVx=(cxOff.value-prevCx.value)/dt;prevCy.value=cy.value;prevCx.value=cxOff.value;
  const orbA=(orbVy-prevV.value)/dt;prevV.value=orbVy;
  const mx=fvx.value+(touchOn.value>0?0:orbVx),my=orbVy+fvy.value*.7,sp=Math.hypot(mx,my);let goal=0;
  if(touchOn.value>0){goal=Math.min(1,sp/430);if(sp>45){const tx=mx/sp*.42,ty=(my/sp>.55?1:-1)*Math.sqrt(1-tx*tx);dirX.value+=(tx-dirX.value)*.16;dirY.value+=(ty-dirY.value)*.16;}}
  else{const slowing=orbVy*orbA<0?Math.abs(orbA):0;goal=Math.min(1,slowing/12000);if(goal>.05){dirX.value+=(0-dirX.value)*.2;dirY.value+=(-1-dirY.value)*.2;}}
  glow.value+=(goal-glow.value)*(goal>glow.value?.22:.12);
  sloshX.value+=(Math.max(-1,Math.min(1,mx/430))-sloshX.value)*.12;sloshY.value+=(Math.max(-1,Math.min(1,my/430))-sloshY.value)*.12;
  const decay=Math.pow(.02,dt);fvx.value*=decay;fvy.value*=decay;
 });
 const orbBox=useDerivedValue(()=>{const R=radius.value*1.36;return{x:cx.value-R,y:cy.value-R,width:R*2,height:R*2};});
 const orbClip=useDerivedValue(()=>{const R=radius.value;return Skia.RRectXY(Skia.XYWHRect(cx.value-R,cy.value-R,R*2,R*2),R,R);});
 const lensUniforms=useDerivedValue(()=>{const R=radius.value;return{c:[cx.value,cy.value],r:R,amount:interpolate(R,[R1,R0],[.62,.42],Extrapolation.CLAMP),bezel:interpolate(R,[R1,R0],[.42,.25],Extrapolation.CLAMP),disp:interpolate(R,[R1,R0*.6],[.12,dark?0:.05],Extrapolation.CLAMP),slosh:[sloshX.value*R*.10,sloshY.value*R*.10]};});
 const uniforms=useDerivedValue(()=>{const n=Math.max(1e-4,Math.hypot(dirX.value,dirY.value)),R=radius.value;return{c:[cx.value,cy.value],r:R,glow:reduced?0:glow.value,dv:[dirX.value/n,dirY.value/n],small:interpolate(R,[R1*1.05,R1*1.7],[1,0],Extrapolation.CLAMP),caus:interpolate(R,[100*sx,190*sx],[0,1],Extrapolation.CLAMP),night:dark?1:0};});
 const sceneFade=useDerivedValue(()=>interpolate(p.value,[.08,.62],[1,0],Extrapolation.CLAMP));
 const sceneBlur=useDerivedValue(()=>reduced?0:interpolate(p.value,[.05,.60],[0,13],Extrapolation.CLAMP));
 const sceneTransform=useDerivedValue(()=>[{translateY:-Math.max(0,Math.min(1,p.value))*110*sy}]);
 const hintFade=useDerivedValue(()=>interpolate(p.value,[.06,.30],[1,0],Extrapolation.CLAMP));
 const hintBlur=useDerivedValue(()=>reduced?0:interpolate(p.value,[.04,.28],[0,12],Extrapolation.CLAMP));
 React.useEffect(()=>{visibility.value=reduced?(armed||busy?1:0):withTiming(armed||busy?1:0,{duration:360});if(!armed&&!busy)animateHome();},[armed,busy,reduced]);
 const accessibleSubmit=React.useCallback(()=>{
  if(!interactive||busy||locked.value)return;
  locked.value=1;touchOn.value=0;cxOff.value=withSpring(0,spring);
  if(reduced){p.value=1;onSubmit();}
  else p.value=withSpring(1,spring,finished=>{if(finished&&p.value>=.92)scheduleOnRN(onSubmit);});
 },[interactive,busy,reduced,onSubmit]);
 const pan=Gesture.Pan().enabled(interactive&&!busy&&active).maxPointers(1)
 .onTouchesDown((e,manager)=>{if(e.allTouches.length!==1){manager.fail();return;}const t=e.allTouches[0];if(Math.hypot(t.x-cx.value,t.y-cy.value)>radius.value+8||locked.value)manager.fail();}).activeOffsetY([-3,3]).activeOffsetX([-6,6])
 .onBegin(e=>{if(locked.value)return;cancelAnimation(p);cancelAnimation(cxOff);touchOn.value=1;p0.value=p.value;cx0.value=cxOff.value;})
 .onUpdate(e=>{if(locked.value)return;fvx.value=e.velocityX;fvy.value=e.velocityY;p.value=rubber(p0.value-e.translationY/TRAVEL);const lim=150*sx;cxOff.value=lim*Math.tanh((cx0.value+e.translationX)/lim);})
 .onEnd((e,success)=>{if(locked.value)return;touchOn.value=0;const vy=-e.velocityY/TRAVEL;
  if(shouldCommit(p.value,success,locked.value>0,cxOff.value,TRAVEL*(1-p.value),R1*1.75)){locked.value=1;p.value=reduced?1:withSpring(1,{...spring,velocity:vy});cxOff.value=withSpring(0,{...spring,velocity:e.velocityX});scheduleOnRN(onSubmit);}
  else{p.value=reduced?0:withSpring(0,{...spring,velocity:vy});cxOff.value=withSpring(0,{...spring,velocity:e.velocityX});}
 })
 .onFinalize((_e,success)=>{touchOn.value=0;if(!success&&locked.value===0){p.value=reduced?0:withSpring(0,spring);cxOff.value=withSpring(0,spring);}});
 return{accessibleSubmit,p,cx,cy,radius,pan,sceneFade,sceneBlur,sceneTransform,hintFade,hintBlur,orbBox,orbClip,lensUniforms,uniforms,visibility,locked,animateHome};
}
