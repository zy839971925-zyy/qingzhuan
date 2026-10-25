const {test}=require('node:test'),a=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),React=require('react'),Renderer=require('react-test-renderer');
global.IS_REACT_ACT_ENVIRONMENT=true;
async function mount(outcome='complete'){
 let props,calls=0;const listeners={};
 const input={id:'file-1',file:'photo.png',name:'photo.png',size:12,kind:'image',tools:['image','pack','hash']};
 const state={protocol:2,revision:1,inputs:[input],outputs:[],busy:false,task:{id:'',seq:0,kind:'',status:'idle',message:'',value:-1}};
 const ui={dark:false,theme:'system',reduced:false,powerSave:false};
 const native={initialUi:ui,ready(){},ui:async()=>ui,sync:async()=>state,haptic(){},start(id){calls++;Object.assign(state,{revision:2,task:{id,seq:2,kind:'convert',status:outcome,message:'done',value:100,files:outcome==='complete'?[{id:'out',name:'photo.jpg',size:10}]:[]}});listeners.QingzhuanState(JSON.stringify(state));}};
 const subscription={addEventListener:()=>({remove(){}})};
 const rn={...Object.fromEntries(['View','Text','TextInput','Pressable','ScrollView','Modal'].map(k=>[k,k])),StyleSheet:{create:x=>x},ToastAndroid:{show(){},LONG:1},AccessibilityInfo:subscription,AppState:subscription,Appearance:{...subscription,addChangeListener:()=>({remove(){}}),getColorScheme:()=> 'light'},BackHandler:subscription,Alert:{alert(){}}};
 const modules={react:React,'react-native':rn,'react-native-gesture-handler':{GestureHandlerRootView:'Root'},'react-native-safe-area-context':{SafeAreaProvider:'SafeAreaProvider',SafeAreaView:'SafeAreaView'},'./src/native/bridge':{native,events:{addListener:(name,fn)=>{listeners[name]=fn;return{remove(){}};}}},'./src/home/GlassHome':{GlassHome:p=>{props=p;return null;}}};
 function load(file){const exports={};vm.runInNewContext(ts.transpileModule(fs.readFileSync(file,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020,jsx:ts.JsxEmit.React,esModuleInterop:true}}).outputText,{exports,require:name=>modules[name]||load(name.replace('./','')+'.ts'),console,Date,Math,Intl});return exports;}
 const App=load('App.tsx').default;let root;await Renderer.act(async()=>{root=Renderer.create(React.createElement(App));});
 await Renderer.act(async()=>props.onChooseTarget('jpg'));
 return{get props(){return props;},get calls(){return calls;},root};
}
test('actual App holds instant backend completion until ring/check acknowledgement',async()=>{const h=await mount();await Renderer.act(async()=>{h.props.onSubmit();h.props.onSubmit();});a.equal(h.calls,1);a.equal(h.props.convertBusy,true);a.equal(h.props.feedbackFinished,true);a.equal(h.props.success.length,0);await Renderer.act(async()=>h.props.onFeedbackDone());a.equal(h.props.convertBusy,false);a.equal(h.props.success[0].name,'photo.jpg');await Renderer.act(async()=>h.root.unmount());});
test('actual App releases presentation immediately on native failure without success',async()=>{const h=await mount('error');await Renderer.act(async()=>h.props.onSubmit());a.equal(h.props.convertBusy,false);a.equal(h.props.success.length,0);await Renderer.act(async()=>h.root.unmount());});
test('old ring acknowledgement cannot finish a subsequent task',async()=>{const h=await mount();await Renderer.act(async()=>h.props.onSubmit());const oldAck=h.props.onFeedbackDone;await Renderer.act(async()=>oldAck());await Renderer.act(async()=>h.props.onAgain());await Renderer.act(async()=>h.props.onChooseTarget('png'));await Renderer.act(async()=>h.props.onSubmit());a.equal(h.props.convertBusy,true);await Renderer.act(async()=>oldAck());a.equal(h.props.convertBusy,true);a.equal(h.props.success.length,0);await Renderer.act(async()=>h.props.onFeedbackDone());a.equal(h.props.convertBusy,false);await Renderer.act(async()=>h.root.unmount());});
