/** Same-canvas native home. Original Appllama optical shaders, GPL-3.0-only. */
import React from 'react';
import {Pressable,StyleSheet,Text,useWindowDimensions,View} from 'react-native';
import {GestureDetector} from 'react-native-gesture-handler';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import {BackdropFilter,Blur,Canvas,Circle,DashPathEffect,Group,Paint,Rect,RoundedRect,RuntimeShader,Shader,Skia} from '@shopify/react-native-skia';
import {Easing,interpolate,useDerivedValue,useSharedValue,withDelay,withTiming} from 'react-native-reanimated';
import {glassEffect} from '../liquid-glass/glass';
import {CanvasText,RevealText} from './CanvasText';
import {kyantLensEffect} from '../liquid-glass/kyantLens';
import {FileEmblem} from './FileEmblem';
import {UploadArtwork} from './UploadArtwork';
import {CanvasIcon} from './CanvasIcon';
import {RingFeedback} from './RingFeedback';
import {useGlassMotion} from './useGlassMotion';
import {size} from './catalog';
import {homeLayout} from './layout';
import {theme} from './theme';
import {segment} from './motionMath';
import type {InputFile,OutputFile,Snapshot,Tool} from '../native/types';

function shortName(text:string,width:number,size:number){const parts=segment(text),max=Math.max(8,Math.floor(width/size));return parts.length>max?parts.slice(0,max-1).join('')+'…':text;}
type Props={state:Snapshot;tool:Tool|null;target:string|null;formats:string[];selectedFiles:InputFile[];success:OutputFile[];busy:boolean;convertBusy:boolean;submitting:boolean;feedbackFinished:boolean;feedbackTask:string;dark:boolean;reduced:boolean;active:boolean;onFeedbackDone:()=>void;onMenu:()=>void;onPick:()=>void;onChooseTarget:(f:string)=>void;onSubmit:()=>void;onCancel:()=>void;onFiles:()=>void;onTools:()=>void;onOptions:()=>void;onHistory:()=>void;onSettings:()=>void;onSave:()=>void;onAgain:()=>void};
function Tile({f,index,x,y,width,chosen,dark,reduced}:{f:string;index:number;x:number;y:number;width:number;chosen:boolean;dark:boolean;reduced:boolean}){
 const c=theme(dark),show=useSharedValue(reduced?1:0);React.useEffect(()=>{show.value=reduced?1:0;show.value=reduced?1:withDelay(index*80,withTiming(1,{duration:520,easing:Easing.bezier(.23,1,.32,1)}));},[f,index,reduced]);
 const transform=useDerivedValue(()=>[{perspective:600},{translateY:(1-show.value)*12},{rotateX:(1-show.value)*-.24}]),blur=useDerivedValue(()=>(1-show.value)*8);
 return <Group transform={transform} origin={{x:x+width/2,y:y+33}} layer={<Paint opacity={show}><Blur blur={blur}/></Paint>}><RoundedRect x={x} y={y} width={width} height={62} r={15} color={chosen?c.accent:c.paper}/><RoundedRect x={x} y={y} width={width} height={62} r={15} style="stroke" strokeWidth={1} color={chosen?c.accent:c.line}/><RevealText text={f?f.toUpperCase():'全部解压'} x={x+5} y={y+20} width={width-(chosen?23:10)} color={chosen?c.paper:c.text} size={14} reduced={reduced} delay={index*80}/>{chosen&&<CanvasIcon name="check" x={x+width-24} y={y+24} size={14} color={c.paper}/>}</Group>;
}
export function GlassHome(props:Props){
 const {state,tool,target,formats,selectedFiles,success,busy,convertBusy,submitting,feedbackFinished,feedbackTask,dark,reduced,active}=props;
 const {width,height}=useWindowDimensions(),insets=useSafeAreaInsets(),sx=width>height*1.2?height/874:width/402,c=theme(dark),hasFiles=state.inputs.length>0,hasSuccess=success.length>0;
 const armed=target!==null&&!!tool&&selectedFiles.length>0&&!hasSuccess;
 const m=useGlassMotion({width,height,resetVersion:state.task.seq,armed:armed||hasSuccess,interactive:armed&&!busy,busy:convertBusy,active,reduced,dark,onSubmit:props.onSubmit});
 // Only individual SharedValues may enter worklet closures; m also contains PanGesture.
 const {cx:orbCx,cy:orbCy,radius:orbRadius,p:orbProgress,hintFade,visibility}=m;
 const f=useSharedValue(hasFiles?1:0);React.useEffect(()=>{f.value=reduced?(hasFiles?1:0):withTiming(hasFiles?1:0,{duration:540});},[hasFiles,reduced]);
 const {landscape,fileY,formatY,formatX,formatWidth,margin,cardWidth,emptyX,emptyY,emptyHeight,successX,successY,tileW}=homeLayout(width,height,insets.top);
 const fileCenter=landscape?margin+cardWidth/2:width/2;
 const uploadRect=useDerivedValue(()=>Skia.RRectXY(Skia.XYWHRect(interpolate(f.value,[0,1],[emptyX,fileCenter-34]),interpolate(f.value,[0,1],[emptyY,fileY]),interpolate(f.value,[0,1],[cardWidth,68]),interpolate(f.value,[0,1],[emptyHeight,76])),22,22));
 const emptyAlpha=useDerivedValue(()=>1-f.value),fileAlpha=useDerivedValue(()=>f.value),fileTransform=useDerivedValue(()=>[{translateY:(1-f.value)*(emptyY+emptyHeight*.36-fileY)},{scale:1+(1-f.value)*.18}]);
 const kyantUniforms=useDerivedValue(()=>({c:[orbCx.value,orbCy.value],r:orbRadius.value,height:orbRadius.value*.32,amount:orbRadius.value*.42,dispersion:.38,slosh:[0,0]}));
 const hintOpacity=useDerivedValue(()=>hintFade.value*visibility.value);
 const optionY=formatY+Math.ceil(formats.length/3)*76+10,headerY=insets.top+12,hintY=landscape?fileY+170:height-245*sx-54;
 const doc=selectedFiles[0]||state.inputs[0],docName=doc?.name||'选择文件',name=shortName(docName,cardWidth,16);
 const original=(doc?.file||docName).split('.').pop()?.toUpperCase()||doc?.kind||'';
 const fileIcon=doc?.kind==='image'?'file-image':doc?.kind==='archive'?'file-archive':doc?.kind==='media'?'file-audio':'file-text';
 const progress=state.task.value;
 const hit=(label:string,onPress:()=>void,x:number,y:number,w:number,h:number,disabled=false)=><Pressable key={label} accessibilityLabel={label} accessibilityRole="button" accessibilityState={{disabled}} disabled={disabled} onPress={onPress} style={{position:'absolute',left:x,top:y,width:w,height:h}}><Text style={styles.accessibleText}>{label}</Text></Pressable>;
 return <GestureDetector gesture={m.pan}><View style={{flex:1,backgroundColor:c.bg}}><Canvas pointerEvents="none" style={StyleSheet.absoluteFill}>
  <Rect x={0} y={0} width={width} height={height} color={c.paper}/>
  {/* Restrained neutral backdrop lines become visibly refracted under the native lens. */}
  {(armed||convertBusy||hasSuccess)&&<Group opacity={dark?.065:.04}>{[0,1,2].map(i=><RoundedRect key={i} x={width*.14+i*14} y={height*.40+i*44} width={width*.72-i*28} height={1} r={.5} color={c.text}/>)}</Group>}
  <Group transform={m.sceneTransform} layer={<Paint opacity={m.sceneFade}><Blur blur={m.sceneBlur}/></Paint>}>
   {!hasSuccess&&<><Group layer={<Paint opacity={emptyAlpha}/>}><RoundedRect rect={uploadRect} color={c.line} style="stroke" strokeWidth={1.2}><DashPathEffect intervals={[5,7]}/></RoundedRect><UploadArtwork cx={emptyX+cardWidth/2} cy={emptyY+emptyHeight*.41} dark={dark} reduced={reduced} active={active&&!hasFiles}/><CanvasText text="选择需要转换的文件" x={emptyX+12} y={emptyY+emptyHeight*.68} width={cardWidth-24} size={15} color={c.muted} center/></Group>
   {hasFiles&&<Group layer={<Paint opacity={fileAlpha}/>}><Group transform={fileTransform} origin={{x:fileCenter,y:fileY+38}}><FileEmblem name={docName} kind={doc?.kind} x={fileCenter-40} y={fileY-10} size={80} dark={dark}/></Group><CanvasText text={name} x={landscape?margin:28} y={fileY+79} width={cardWidth} size={16} color={c.text} center/><CanvasText text={`${original}  ·  ${size(selectedFiles.reduce((n,f)=>n+f.size,0))}${selectedFiles.length>1?`  ·  ${selectedFiles.length} 份`:''}`} x={landscape?margin:28} y={fileY+108} width={cardWidth} size={12} color={c.muted} center/></Group>}
   {hasFiles&&tool&&<><CanvasText text="转换为" x={formatX} y={formatY-28} width={110} size={12} color={c.muted}/><CanvasIcon name="chevron-down" x={formatX+formatWidth-20} y={formatY-29} size={16} color={c.muted}/>{formats.map((value,i)=><Tile key={`${tool.id}:${value}`} f={value} index={i} x={formatX+(i%3)*(tileW+9)} y={formatY+Math.floor(i/3)*76} width={tileW} chosen={value===target} dark={dark} reduced={reduced}/>)}<CanvasIcon name="settings-2" x={formatX+8} y={optionY+8} size={21} color={c.muted}/><CanvasIcon name="plus" x={formatX+formatWidth-32} y={optionY+8} size={22} color={c.muted}/></>}
   </>}
   {hasSuccess&&<><FileEmblem name={success[0].name} x={successX+cardWidth/2-48} y={successY-108} size={96} dark={dark}/><CanvasIcon name="check" x={successX+cardWidth/2-41} y={successY+4} size={17} color={c.success}/><CanvasText text="转换完成" x={successX+cardWidth/2-18} y={successY+4} width={100} size={13} color={c.success}/><RevealText text={shortName(success[0].name,cardWidth,18)} x={successX} y={successY+40} width={cardWidth} size={18} color={c.text} reduced={reduced}/><CanvasText text={`${size(success.reduce((n,f)=>n+f.size,0))}${success.length>1?` · ${success.length} 份文件`:''}`} x={successX} y={successY+74} width={cardWidth} size={12} color={c.muted} center/>{[-38,38].map((dx,i)=><Group key={i}><Circle cx={successX+cardWidth/2+dx} cy={successY+136} r={26} color={c.card}/><CanvasIcon name={i?'plus':'download'} x={successX+cardWidth/2+dx-11} y={successY+125} size={22} color={c.accent}/></Group>)}</>}

  </Group>
  {!convertBusy&&!hasSuccess&&<Group layer={<Paint opacity={hintOpacity}><Blur blur={m.hintBlur}/></Paint>}><CanvasText text={hasSuccess?'文件已就绪':'按住下方玻璃罩'} x={landscape?margin:20} y={hintY} width={landscape?cardWidth:width-40} size={14} color={c.text} center/>{!hasSuccess&&<CanvasText text="向上滑动以转换" x={landscape?margin:20} y={hintY+25} width={landscape?cardWidth:width-40} size={12} color={c.muted} center/>}</Group>}
  {(armed||convertBusy||hasSuccess)&&<Group><Group clip={m.orbClip}><BackdropFilter filter={<RuntimeShader source={kyantLensEffect!} uniforms={kyantUniforms}/>}/></Group><Rect rect={m.orbBox}><Shader source={glassEffect} uniforms={m.uniforms}/></Rect></Group>}
  {convertBusy&&<RingFeedback key={feedbackTask||state.task.id} cx={orbCx} cy={orbCy} radius={orbRadius} p={orbProgress} active={active} finished={feedbackFinished} real={progress} dark={dark} onDone={props.onFeedbackDone}/>}
  {busy&&<><CanvasText text={state.task.status==='cancelling'?'正在取消':convertBusy?(feedbackFinished?'已完成':submitting?'正在准备':'正在转换'):({import:'正在导入',export:'正在保存',bundle:'正在打包',cleanup:'正在清理'} as Record<string,string>)[state.task.kind]||'正在处理'} x={28} y={height*.59} width={width-56} size={14} color={c.text} center/>{!feedbackFinished&&<CanvasText text={convertBusy?(progress>=0?`${progress}%`:''):state.task.message} x={28} y={height*.59+29} width={width-56} size={12} color={c.muted} center/>}</>}
  <CanvasIcon name="ellipsis" x={width-57} y={headerY+12} size={24} color={c.muted}/>
 </Canvas>
 {hit('更多操作',props.onMenu,width-72,headerY,48,48,busy)}
 <View pointerEvents={!busy&&active?'box-none':'none'} accessibilityElementsHidden={busy} importantForAccessibility={busy?'no-hide-descendants':'auto'} style={StyleSheet.absoluteFill}>
  {!hasFiles?hit('选择需要转换的文件',props.onPick,emptyX,emptyY,cardWidth,emptyHeight):!hasSuccess&&hit(`管理文件 ${docName}`,props.onFiles,landscape?margin:28,fileY,cardWidth,138)}
  {hasFiles&&!hasSuccess&&tool&&<>{hit(`切换工具，当前${tool.title}`,props.onTools,formatX+formatWidth-110,formatY-40,110,44)}{formats.map((value,i)=>hit(`转换为 ${value||'全部解压'}${value===target?'，已选':''}`,()=>props.onChooseTarget(value),formatX+(i%3)*(tileW+9),formatY+Math.floor(i/3)*76,tileW,62))}{hit('转换选项',props.onOptions,formatX,optionY,48,48)}{hit('添加文件',props.onPick,formatX+formatWidth-48,optionY,48,48)}</>}
  {hasSuccess&&<>{hit('保存转换结果',props.onSave,successX+cardWidth/2-64,successY+110,52,52)}{hit('继续转换下一个文件',props.onAgain,successX+cardWidth/2+12,successY+110,52,52)}</>}
 </View>
 {armed&&!busy&&<Pressable accessibilityRole="button" accessibilityLabel="直接开始转换，也可按住玻璃罩向上滑动" onPress={m.accessibleSubmit} style={{position:'absolute',bottom:Math.max(insets.bottom+8,18),left:width*.3,right:width*.3,minHeight:44,alignItems:'center',justifyContent:'center'}}><Text style={styles.accessibleText}>直接开始转换</Text></Pressable>}
 {busy&&!feedbackFinished&&<Pressable accessibilityRole="button" accessibilityLabel="取消当前任务" disabled={state.task.status==='cancelling'||submitting} onPress={props.onCancel} style={{position:'absolute',top:Math.min(height*.59+70,height-insets.bottom-56),left:width*.3,right:width*.3,minHeight:48,alignItems:'center',justifyContent:'center'}}><Text style={{color:c.muted,fontSize:12}}>{state.task.status==='cancelling'?'等待取消确认':'取消'}</Text></Pressable>}
 </View></GestureDetector>;
}
const styles=StyleSheet.create({accessibleText:{opacity:0,fontSize:1}});
