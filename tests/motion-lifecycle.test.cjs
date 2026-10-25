// Runs the real hook's control flow with deterministic hook/gesture adapters.
// This verifies submission/cancellation logic, not the native UI thread or FPS.
const {test}=require('node:test'),a=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path'),ts=require('typescript');
const root=path.resolve(__dirname,'..');
function hook(busy=false,dark=false){
 const effects=[],handlers={};let submits=0,cancellations=0;
 const gesture=new Proxy({}, {get:(_,key)=>(...args)=>{if(key.startsWith('on'))handlers[key]=args[0];return gesture;}});
 const react={useCallback:f=>f,useEffect:f=>effects.push(f)};
 const animation={useSharedValue:v=>({value:v}),useDerivedValue:f=>({get value(){return f();}}),useFrameCallback:()=>{},cancelAnimation:()=>cancellations++,withSpring:v=>v,withTiming:v=>v,Extrapolation:{CLAMP:'clamp'},interpolate:(v,[a,b],[c,d],extra)=>{const t=(v-a)/(b-a);return c+(d-c)*(extra==='clamp'?Math.max(0,Math.min(1,t)):t);}};
 function load(file){const exports={};const code=ts.transpileModule(fs.readFileSync(path.join(root,file),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText;vm.runInNewContext(code,{exports,Math,Number,Intl,require:name=>{
  if(name==='react')return{default:react};if(name==='react-native-gesture-handler')return{Gesture:{Pan:()=>gesture}};
  if(name==='react-native-reanimated')return animation;if(name==='react-native-worklets')return{scheduleOnRN:f=>f()};
  if(name==='@shopify/react-native-skia')return{Skia:{RRectXY:(...v)=>v,XYWHRect:(...v)=>v}};
  if(name==='./motionMath')return load('src/home/motionMath.ts');throw Error(name);
 }});return exports;}
 const m=load('src/home/useGlassMotion.ts').useGlassMotion({width:402,height:874,resetVersion:0,armed:true,interactive:!busy,busy,active:true,reduced:false,dark,onSubmit:()=>submits++});
 for(const f of effects)f();
 return{m,effects,handlers,get submits(){return submits;},get cancellations(){return cancellations;}};
}
test('interrupting the accessible landing animation releases an idle task lock',()=>{const h=hook();h.m.accessibleSubmit();a.equal(h.m.locked.value,1);h.effects[2]();a.equal(h.m.p.value,0);a.equal(h.m.locked.value,0);a.equal(h.submits,0);a.ok(h.cancellations>=4);});
test('lifecycle reset keeps a real conversion locked at the center',()=>{const h=hook(true);h.effects[2]();a.equal(h.m.p.value,1);a.equal(h.m.locked.value,1);a.equal(h.submits,0);});
test('finger reversal updates the same progress immediately; early and cancelled release never submit',()=>{const h=hook(),g=h.handlers,travel=874*(1-.469);g.onBegin({});g.onUpdate({translationY:-travel*.7,translationX:0,velocityX:0,velocityY:-600});a.ok(Math.abs(h.m.p.value-.7)<1e-8);g.onUpdate({translationY:-travel*.3,translationX:0,velocityX:0,velocityY:600});a.ok(Math.abs(h.m.p.value-.3)<1e-8);g.onEnd({velocityX:0,velocityY:-9000},true);a.equal(h.m.p.value,0);a.equal(h.submits,0);g.onBegin({});g.onUpdate({translationY:-travel*.8,translationX:0,velocityX:0,velocityY:-600});g.onFinalize({},false);a.equal(h.m.p.value,0);a.equal(h.submits,0);});
test('central release submits once and lateral release returns home',()=>{const travel=874*(1-.469),h=hook(),g=h.handlers;g.onBegin({});g.onUpdate({translationY:-travel,translationX:500,velocityX:0,velocityY:-600});g.onEnd({velocityX:0,velocityY:-600},true);a.equal(h.submits,0);a.equal(h.m.p.value,0);g.onBegin({});g.onUpdate({translationY:-travel*.95,translationX:0,velocityX:0,velocityY:-600});g.onEnd({velocityX:0,velocityY:-600},true);g.onEnd({velocityX:0,velocityY:-600},true);a.equal(h.submits,1);});
test('real motion hook matches reference keyframe geometry and optical uniforms in both directions and themes',()=>{for(const dark of [false,true]){const h=hook(false,dark),frames=[0,.1,.3,.5,.7,.9,1];for(const p of [...frames,...frames.toReversed()]){h.m.p.value=p;a.ok(Math.abs(h.m.radius.value-(245-201*p))<1e-8);a.ok(Math.abs(h.m.cy.value-(874+(874*.469-874)*p))<1e-8);a.equal(h.m.cx.value,201);a.equal(h.m.uniforms.value.night,dark?1:0);const u=h.m.lensUniforms.value;a.ok(u.amount>=.42&&u.amount<=.62);a.ok(u.disp>=0&&u.disp<=.12);}h.m.p.value=0;a.ok(Math.abs(h.m.lensUniforms.value.disp-(dark?0:.05))<1e-8);h.m.p.value=1;a.equal(h.m.lensUniforms.value.disp,.12);}});
