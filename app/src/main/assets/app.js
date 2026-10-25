(function(){'use strict';
const $=s=>document.querySelector(s),esc=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])),icon=n=>`<svg aria-hidden="true"><use href="#${n}"/></svg>`;
const specs=[
{id:'image',name:'图片转换',formats:['jpg','png','webp'],desc:'透明图片转 JPG 时铺白，GIF 只取首帧。'},
{id:'imagepdf',name:'图片转 PDF',formats:['pdf'],desc:'按文件排列顺序成册，每张图片一页。'},
{id:'pdfimage',name:'PDF 转图片',formats:['jpg','png'],desc:'将选定页面导出为图片，扫描件也可渲染。'},
{id:'merge',name:'合并 PDF',formats:['pdf'],desc:'按文件顺序合并，数字签名不会保留。'},
{id:'split',name:'PDF 拆页',formats:['pdf'],desc:'将选定的每页保存为独立 PDF。'},
{id:'document',name:'文档文字转换',formats:['txt','html','docx','pdf'],desc:'仅提取和重建文字，不保留原版式、图片、批注及签名。扫描 PDF 需要 OCR，本版未提供。'},
{id:'table',name:'表格转换',formats:['xlsx','csv','tsv','json'],desc:'仅首个工作表。保留文字及公式缓存，不计算公式、不保留样式。'},
{id:'unpack',name:'解压文件',formats:[''],desc:'展开压缩内容，不支持链接、分卷或 RAR5。'},
{id:'audio',name:'音频提取',formats:['m4a','wav'],desc:'M4A 仅提取 AAC；WAV 由系统解码为 16 位 PCM，体积可能增大。'},
{id:'pack',name:'压缩打包',formats:['zip','tar.gz'],desc:'可选 ZIP AES 密码，或打包 TAR.GZ。'},
{id:'hash',name:'文件校验',formats:['txt'],desc:'生成含 SHA-256 与 MD5 的校验文本。'}];
const storage=(k,d)=>{try{return localStorage.getItem(k)??d;}catch{return d;}};
let prefs={theme:storage('theme','system'),reduce:storage('reduce','false')==='true',haptic:storage('haptic','true')!=='false',quality:storage('quality','auto')};
let lastNativeSeq=-1;
let state={revision:-1,inputs:[],outputs:[],busy:false,task:null},page='home',tool=null,target=null,selected=new Set(),order=[],active=true,saver=false,systemReduced=false,dark=document.documentElement.dataset.theme==='dark',lastTerminal='',fileKey='',submitTimer=0,toastTimer=0,dragX=0,visualP=0,frame=0,springFrame=0,gestureStartX=0,selectionRestored=false;
const motionQuery=matchMedia('(prefers-reduced-motion: reduce)'),themeQuery=matchMedia('(prefers-color-scheme: dark)');
const glass=new QGlass($('#glass-canvas'));const motion=new QMotion.Gesture(submit);
const reduced=()=>prefs.reduce||motionQuery.matches||systemReduced||saver||!active;
function store(k,v){try{localStorage.setItem(k,String(v));}catch{}}
function native(method,...args){if(window.Native&&typeof Native[method]==='function'){Native[method](...args);return true;}toast('文件操作需要安卓安装版');return false;}
function toast(message){$('#toast').textContent=message;$('#toast').hidden=false;clearTimeout(toastTimer);toastTimer=setTimeout(()=>{$('#toast').hidden=true;},4500);}
function feedback(){if(prefs.haptic&&!saver&&active)native('haptic');}
function bytes(n){return n<1024?n+' B':n<1048576?(n/1024).toFixed(1)+' KiB':(n/1048576).toFixed(1)+' MiB';}
function currentFiles(){const map=new Map(state.inputs.map(f=>[f.id,f]));return order.filter(id=>selected.has(id)&&map.has(id)).map(id=>map.get(id));}
function eligible(t,f){return !!f.tools?.includes(t.id);}
function saveSelection(){try{localStorage.setItem('selection',JSON.stringify({tool:tool?.id,target,ids:[...selected],order}));}catch{}}
function restoreSelection(){if(selectionRestored||!state.inputs.length)return;selectionRestored=true;try{const s=JSON.parse(storage('selection','null'));if(s){order=s.order||[];selected=new Set(s.ids||[]);tool=specs.find(t=>t.id===s.tool)||null;target=s.target;}}catch{}}
function options(){let v={target:target??''};for(const k of ['quality','edge','pages','password','encoding','zipEncoding']){const el=$('#opt-'+k);if(el)v[k]=['quality','edge'].includes(k)?Number(el.value):el.value;}if(tool?.id==='pack'&&target!=='zip')v.password='';return v;}
function submit(id){
 if(!tool||state.busy||!currentFiles().length){motion.reject();return;}
 const opts=options();if(tool.id==='merge'&&currentFiles().length<2){motion.reject();toast('合并至少需要两份 PDF');return;}
 saveSelection();$('#completion').hidden=true;document.body.classList.add('running');$('#operation-status').hidden=false;$('#operation-title').textContent='正在转换';$('#operation-message').textContent='等待本机引擎确认';$('#progress-number').textContent='';$('#task-progress').classList.add('indeterminate');$('#task-progress').removeAttribute('aria-valuenow');$('#cancel-button').disabled=false;$('#cancel-button').textContent='取消任务';showGlass();animate(1);
 if(!native('startTask',id,tool.id,JSON.stringify(opts),JSON.stringify(currentFiles().map(f=>f.id)))){motion.reject();resetAfterTask();return;}
 clearTimeout(submitTimer);submitTimer=setTimeout(()=>{if(motion.id===id&&motion.seq<0){native('sync');toast('正在核对任务状态');}},1800);
}
function chooseTool(id,restoring=false){
 tool=specs.find(t=>t.id===id);if(!tool)return;target=null;selected=new Set(state.inputs.filter(f=>eligible(tool,f)).map(f=>f.id));motion.arm(tool.id,'');paint(0);$('#completion').hidden=true;renderChoices(!restoring);saveSelection();showGlass();
}
function chooseFormat(f){if(state.busy||!tool)return;target=f;motion.arm(tool.id,f||'unpack');$('#formats').querySelectorAll('button').forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.format===f)));$('#completion').hidden=true;saveSelection();showGlass(true);feedback();}
function renderFiles(){
 const map=new Map(state.inputs.map(f=>[f.id,f]));order=order.filter(id=>map.has(id));for(const f of state.inputs)if(!order.includes(f.id))order.push(f.id);
 $('#file-stack').innerHTML=order.map((id,i)=>{const f=map.get(id),ok=!tool||eligible(tool,f);return `<div class="file-card"><span class="file-badge">${icon('file')}</span><div class="file-copy"><strong>${esc(f.name)}</strong><small>${bytes(f.size)} · ${esc(({image:'可解码图片',pdf:'PDF 文档',document:'文字文档',table:'表格',archive:'压缩文件',media:'音视频',unknown:'未识别格式',unsupported:'暂不支持的格式'})[f.kind]||'文件')}</small></div>${tool?`<input type="checkbox" data-select="${esc(id)}" aria-label="选择 ${esc(f.name)}" ${selected.has(id)?'checked':''} ${ok?'':'disabled'}>`:''}<div class="file-order"><button data-move="${esc(id)}" data-direction="-1" aria-label="前移 ${esc(f.name)}" ${i===0?'disabled':''}>↑</button><button data-move="${esc(id)}" data-direction="1" aria-label="后移 ${esc(f.name)}" ${i===order.length-1?'disabled':''}>↓</button></div><button class="remove-file" data-remove="${esc(id)}" aria-label="移除 ${esc(f.name)}">×</button></div>`;}).join('');
 for(const e of $('#file-stack').querySelectorAll('button,input'))if(state.busy)e.disabled=true;
}
function formatsFor(){if(tool.id!=='audio')return tool.formats;const fs=currentFiles();return tool.formats.filter(x=>fs.length&&fs.every(f=>f.audioTargets?.includes(x)));}
function animateWords(container){if(reduced()||!window.gsap)return;const els=container.querySelectorAll('.format');gsap.fromTo(els,{opacity:0,y:12,rotateX:-14,filter:'blur(4px)'},{opacity:1,y:0,rotateX:0,filter:'blur(0px)',duration:.45,stagger:QMotion.config.formatStagger,ease:'power2.out',clearProps:'all'});}
function renderChoices(entrance=false){
 const available=specs.filter(t=>state.inputs.some(f=>eligible(t,f)));if(!tool||!available.includes(tool)){tool=available[0]||null;target=null;selected=new Set(tool?state.inputs.filter(f=>eligible(tool,f)).map(f=>f.id):[]);}
 $('#choices').hidden=!tool||!state.inputs.length;if(!tool)return;
 $('#tool-select').innerHTML=available.map(t=>`<option value="${t.id}" ${t===tool?'selected':''}>${esc(t.name)}</option>`).join('');$('#tool-description').textContent=tool.desc;
 const formats=formatsFor();if(target!==null&&!formats.includes(target)){target=null;motion.arm(tool.id,'');}
 $('#formats').innerHTML=formats.map(f=>`<button class="format" data-format="${esc(f)}" aria-pressed="${f===target}">${esc(f?f.toUpperCase():'展开全部')}<small>${f==='txt'&&tool.id==='hash'?'SHA-256 / MD5':f==='pdf'&&tool.id==='merge'?'按序合并':tool.id==='unpack'?'保留原有内容':'本机处理'}</small></button>`).join('');
 $('#selection-note').textContent=`已选择 ${currentFiles().length} / ${state.inputs.length} 份文件${state.inputs.some(f=>!eligible(tool,f))?' · 不适用的文件已排除':''}`;
 renderOptions();renderFiles();if(entrance)animateWords($('#formats'));showGlass();
}
function renderOptions(){
 const id=tool.id;let html='';const field=(title,key,element)=>`<label class="field-label">${title}${element.replace('$ID','opt-'+key)}</label>`;
 if(['image','pdfimage'].includes(id)){html+=field('最长边','edge',`<select id="$ID"><option>1280</option><option ${id==='pdfimage'?'selected':''}>2048</option><option ${id==='image'?'selected':''}>4096</option></select>`);html+=field('输出画质','quality','<input id="$ID" type="range" min="20" max="100" value="88">');}
 if(['split','pdfimage'].includes(id))html+=field('页码范围','pages','<input id="$ID" placeholder="全部，或 1-3,5">');
 if(['document','table'].includes(id))html+=field('输入编码','encoding','<select id="$ID"><option value="auto">自动 UTF / GB18030</option><option>UTF-8</option><option>GB18030</option><option>UTF-16LE</option></select>');
 if(['document','merge','split','pdfimage','unpack','pack'].includes(id))html+=field(id==='pack'?'ZIP 密码（可选）':'文件密码（如有）','password','<input id="$ID" type="password" maxlength="200" autocomplete="new-password" placeholder="不需要时留空">');
 if(id==='unpack')html+=field('旧 ZIP 编码','zipEncoding','<select id="$ID"><option value="auto">标准 / UTF-8</option><option>GB18030</option></select>');
 const old={};for(const e of $('#option-fields').querySelectorAll('input,select'))old[e.id]=e.value;$('#option-fields').innerHTML=html;for(const e of $('#option-fields').querySelectorAll('input,select'))if(old[e.id]!==undefined)e.value=old[e.id];$('#options').hidden=!html;
}
function resultCard(f){return `<div class="result-card"><div class="result-header"><span class="file-badge">${icon('file')}</span><div class="file-copy"><strong>${esc(f.name)}</strong><small>${bytes(f.size)}</small></div></div><div class="result-actions"><button data-save="${esc(f.id)}">导出到手机 ↗</button><button data-open="${esc(f.id)}">预览</button><button data-share="${esc(f.id)}">分享</button></div></div>`;}
function renderHistory(){ $('#history-dot').hidden=!state.outputs.length;$('#history-list').innerHTML=state.outputs.length?state.outputs.map(resultCard).join(''):'<p class="subtle">还没有完成的文件。先选择一份文件吧。</p>';$('#export-all').disabled=!state.outputs.length||state.busy;$('#delete-results').disabled=!state.outputs.length||state.busy;}
function renderState(){
 restoreSelection();const nextKey=JSON.stringify(state.inputs.map(f=>[f.id,f.name,f.kind,f.tools,f.audioTargets]));const changed=fileKey!==nextKey;fileKey=nextKey;
 document.body.classList.toggle('with-files',!!state.inputs.length);$('#dropzone').hidden=!!state.inputs.length;$('#selected-area').hidden=!state.inputs.length;
 if(changed){for(const id of [...selected])if(!state.inputs.some(f=>f.id===id))selected.delete(id);if(!state.inputs.length){tool=null;target=null;selected.clear();if(!motion.busy)motion.arm('','');}renderChoices(true);renderFiles();if(target!==null&&tool&&!motion.busy)motion.arm(tool.id,target||'unpack');}
 $('#dropzone').disabled=state.busy;$('#add-button').disabled=state.busy;$('#clear-button').disabled=state.busy;$('#tool-select').disabled=state.busy;for(const el of $('#choices').querySelectorAll('button,input,select'))el.disabled=state.busy;
 renderHistory();renderFiles();showGlass();requestBake();
}
function acceptTask(t){
 if(!t?.id)return;
 const terminal=['complete','error','cancelled','interrupted'].includes(t.status);const converting=t.kind==='convert';
 if(t.id===motion.id){if(!motion.event(t))return;}
 else if(!terminal){if(converting)motion.restore(t);}
 else if(motion.busy&&t.id!==motion.id){if(!state.busy){motion.reject();resetAfterTask();}return;}
 if(!terminal){
  clearTimeout(submitTimer);document.body.classList.toggle('running',converting);document.body.classList.toggle('busy-utility',!converting);$('#operation-status').hidden=false;
  if(converting){showGlass();animate(1);}else{hideGlass();$('#scene-content').style.opacity='.16';$('#scene-content').inert=true;}
  $('#operation-title').textContent=t.status==='cancelling'?'正在取消':({convert:'正在转换',import:'正在导入',export:'正在导出',bundle:'正在打包',cleanup:'正在清理'})[t.kind]||'正在处理';$('#operation-message').textContent=t.message;
  $('#task-progress').classList.toggle('indeterminate',t.value<0);if(t.value<0){$('#task-progress').removeAttribute('aria-valuenow');$('#progress-number').textContent='处理中 · 进度暂不可量化';}else{$('#task-progress').setAttribute('aria-valuenow',t.value);$('#task-fill').style.transform=`scaleX(${t.value/100})`;$('#progress-number').textContent=t.value+'%';}
  $('#cancel-button').disabled=t.status==='cancelling';$('#cancel-button').textContent=t.status==='cancelling'?'等待取消确认…':'取消任务';
 }else{
  const key=t.id+':'+t.seq;if(lastTerminal===key)return;lastTerminal=key;clearTimeout(submitTimer);resetAfterTask();
  if(t.status==='complete'&&converting){$('#completion').hidden=false;$('#completed-files').innerHTML=(t.files||[]).map(resultCard).join('');$('#choices').hidden=true;feedback();if(!reduced()&&window.gsap)gsap.fromTo($('#completion'),{y:-25,opacity:0,filter:'blur(5px)'},{y:0,opacity:1,filter:'blur(0px)',duration:.65,ease:'power2.out',clearProps:'all'});}
  else if(t.status!=='complete')toast(t.message||'任务未完成');else if(t.kind==='export')toast('已保存到所选位置');
 }
}
function resetAfterTask(){document.body.classList.remove('running','busy-utility');$('#operation-status').hidden=true;$('#scene-content').inert=false;animate(0);showGlass();}
function showGlass(entrance=false){const show=page==='home'&&((target!==null&&tool&&currentFiles().length>0)||motion.busy);$('#glass-stage').hidden=!show;document.body.classList.toggle('with-target',!!show);$('#gesture-hint').hidden=motion.busy;$('#accessible-start').hidden=motion.busy;$('#glass').disabled=motion.busy||state.busy;$('#accessible-start').disabled=state.busy;if(show){resizeGeometry();if(entrance&&!reduced()&&window.gsap)gsap.fromTo($('#glass'),{opacity:0},{opacity:1,duration:.45,clearProps:'opacity'});}}
function hideGlass(){$('#glass-stage').hidden=true;document.body.classList.remove('with-target');}
function navigate(next){if(state.busy||motion.busy){toast('任务进行中，请等待或取消');return;}motion.interrupt();animate(0);page=next;$('#home').hidden=next!=='home';$('#history-page').hidden=next!=='history';$('#settings-page').hidden=next!=='settings';scrollTo(0,0);showGlass();requestBake();}
let geom={w:innerWidth,h:innerHeight,r:245,travel:400};
function resizeGeometry(){geom={w:innerWidth,h:innerHeight,...QMotion.geometry(innerWidth,innerHeight,0)};document.documentElement.style.setProperty('--dome',geom.r*2+'px');paint(visualP);}
function paint(p){visualP=QMotion.clamp(p);const g=QMotion.geometry(geom.w,geom.h,visualP);$('#glass').style.transform=`translate3d(${-geom.r+dragX}px,${g.y-geom.r}px,0) scale(${g.r/geom.r})`;
 const hide=Math.min(1,visualP*1.75);const scene=$('#scene-content');scene.style.transform=`translate3d(0,${-visualP*110}px,0)`;scene.style.opacity=String(1-hide);scene.inert=visualP>.08||state.busy;
 const blur=reduced()||glass.level==='compat'?0:Math.min(7,visualP*12);$('#file-stack').style.filter=blur?`blur(${blur}px)`:'';$('#formats').style.filter=blur?`blur(${blur}px)`:'';$('.intro').style.filter=blur?`blur(${Math.min(4,blur)}px)`:'';$('#gesture-hint').style.opacity=String(Math.max(0,1-visualP*4));$('#accessible-start').style.opacity=String(Math.max(0,1-visualP*4));$('#glass .glass-arrow').style.opacity=String(.6*(1-visualP));
 glass.draw(geom.w/2+dragX,g.y,g.r,visualP,dark);
}
function schedulePaint(){if(!frame)frame=requestAnimationFrame(()=>{frame=0;paint(motion.p);});}
function stopSpring(){if(springFrame)cancelAnimationFrame(springFrame);springFrame=0;}
function animate(to){stopSpring();if(reduced()||!active){dragX=0;paint(to);return;}let x=visualP,v=0,last=performance.now();const tick=now=>{const dt=Math.min(.032,(now-last)/1000);last=now;v+=(QMotion.config.stiffness*(to-x)-QMotion.config.damping*v)*dt;x+=v*dt;dragX*=.8;paint(x);if(Math.abs(x-to)<.0005&&Math.abs(v)<.005){paint(to);springFrame=0;}else springFrame=requestAnimationFrame(tick);};springFrame=requestAnimationFrame(tick);}
let bakeFrame=0;function requestBake(){if(!bakeFrame)bakeFrame=requestAnimationFrame(()=>{bakeFrame=0;glass.bake();paint(visualP);});}
const dome=$('#glass');
dome.addEventListener('pointerdown',e=>{if(e.button!==0&&e.pointerType==='mouse')return;if(!active||state.busy)return;if(!motion.down(e.pointerId,e.clientY)){animate(0);return;}stopSpring();motion.origin=visualP;gestureStartX=e.clientX;dragX=0;glass.bake();document.body.classList.add('gesturing');dome.setPointerCapture(e.pointerId);e.preventDefault();});
dome.addEventListener('pointermove',e=>{if(motion.move(e.pointerId,e.clientY,geom.travel)){dragX=65*Math.tanh((e.clientX-gestureStartX)/100);schedulePaint();}});
function endPointer(e,interrupted){if(motion.pointer!==e.pointerId)return;if(frame){cancelAnimationFrame(frame);frame=0;}if(interrupted){motion.interrupt();animate(0);}else{const submitted=motion.up(e.pointerId);animate(submitted?1:0);}document.body.classList.remove('gesturing');if(dome.hasPointerCapture?.(e.pointerId))dome.releasePointerCapture(e.pointerId);}
dome.addEventListener('pointerup',e=>endPointer(e,false));dome.addEventListener('pointercancel',e=>endPointer(e,true));dome.addEventListener('lostpointercapture',e=>endPointer(e,true));
document.addEventListener('pointerdown',e=>{if(motion.pointer!==null&&e.pointerId!==motion.pointer){motion.interrupt();document.body.classList.remove('gesturing');animate(0);}},true);
dome.addEventListener('keydown',e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();motion.commit();}});
$('#accessible-start').onclick=()=>motion.commit();
window.onNative=(type,data)=>{if(type==='toast'){toast(data.message);return;}if(type!=='state'||!data||data.revision<state.revision)return;if(data.task&&data.task.seq<lastNativeSeq)return;if(data.task)lastNativeSeq=Math.max(lastNativeSeq,data.task.seq);state=data;renderState();acceptTask(state.task);if(state.warning)toast(state.warning);};
window.setNativeTheme=d=>{dark=!!d;document.documentElement.dataset.theme=dark?'dark':'light';requestBake();};
function applyPreferences(){document.body.classList.toggle('reduce',reduced());document.body.classList.toggle('inactive',!active);glass.setLevel(reduced()?'compat':prefs.quality);requestBake();}
window.setAppActive=(a,power=false,reduce=false)=>{active=a;saver=power;systemReduced=reduce;if(!a){motion.interrupt();stopSpring();if(frame){cancelAnimationFrame(frame);frame=0;}if(bakeFrame){cancelAnimationFrame(bakeFrame);bakeFrame=0;}clearTimeout(toastTimer);$('#toast').hidden=true;document.body.classList.remove('gesturing');paint(motion.busy?1:0);}applyPreferences();};
// No periodic polling. Native resumes with an authoritative snapshot.
window.handleBack=()=>{for(const d of document.querySelectorAll('dialog[open]')){d.close();return true;}if(state.busy||motion.busy){toast('可用取消按钮终止当前任务');return true;}if(page!=='home'){navigate('home');return true;}if(motion.phase==='dragging'||visualP>.01){motion.interrupt();animate(0);return true;}if(target!==null){target=null;motion.arm(tool?.id||'','');renderChoices();showGlass();saveSelection();return true;}return false;};
$('#dropzone').onclick=$('#add-button').onclick=()=>native('pick');$('#clear-button').onclick=()=>native('clearInputs');$('#home-button').onclick=()=>navigate('home');$('#history-button').onclick=()=>navigate('history');$('#settings-button').onclick=()=>navigate('settings');$('#tool-select').onchange=e=>chooseTool(e.target.value);
$('#cancel-button').onclick=()=>{const id=state.task?.id||motion.id;if(motion.busy)motion.cancel();$('#cancel-button').disabled=true;$('#cancel-button').textContent='等待取消确认…';native('cancelTask',id);};
$('#again-button').onclick=()=>{$('#completion').hidden=true;renderChoices(true);showGlass();};$('#export-all').onclick=()=>native('saveAll',JSON.stringify(state.outputs.map(f=>f.id)));$('#delete-results').onclick=()=>$('#confirm-dialog').showModal();$('#keep-results').onclick=()=>$('#confirm-dialog').close();$('#confirm-delete').onclick=()=>{$('#confirm-dialog').close();native('deleteAll');};
document.addEventListener('click',e=>{const b=e.target.closest('button');if(!b||b.disabled)return;if(b.dataset.format!==undefined){chooseFormat(b.dataset.format);return;}for(const k of ['remove','save','open','share'])if(b.dataset[k]){native(k==='remove'?'removeInput':k,b.dataset[k]);return;}if(b.dataset.move){const i=order.indexOf(b.dataset.move),j=i+Number(b.dataset.direction);if(j>=0&&j<order.length){[order[i],order[j]]=[order[j],order[i]];renderFiles();saveSelection();requestBake();}}});
$('#file-stack').addEventListener('change',e=>{if(!e.target.dataset.select)return;const id=e.target.dataset.select;if(e.target.checked)selected.add(id);else selected.delete(id);target=null;motion.arm(tool.id,'');renderChoices();saveSelection();});
$('#theme-select').value=prefs.theme;$('#theme-select').onchange=e=>{prefs.theme=e.target.value;store('theme',prefs.theme);if(window.Native)native('setTheme',prefs.theme);else setNativeTheme(prefs.theme==='dark'||(prefs.theme==='system'&&themeQuery.matches));};$('#reduce-toggle').checked=prefs.reduce;$('#reduce-toggle').onchange=e=>{prefs.reduce=e.target.checked;store('reduce',prefs.reduce);motion.interrupt();paint(motion.busy?1:0);applyPreferences();};$('#haptic-toggle').checked=prefs.haptic;$('#haptic-toggle').onchange=e=>{prefs.haptic=e.target.checked;store('haptic',prefs.haptic);};$('#quality-select').value=prefs.quality;$('#quality-select').onchange=e=>{prefs.quality=e.target.value;store('quality',prefs.quality);applyPreferences();};
const onTheme=()=>{if(!window.Native&&prefs.theme==='system')setNativeTheme(themeQuery.matches);};themeQuery.addEventListener?.('change',onTheme);motionQuery.addEventListener?.('change',applyPreferences);
for(const name of ['dragenter','dragover'])$('#dropzone').addEventListener(name,e=>{e.preventDefault();$('#dropzone').classList.add('dragover');});$('#dropzone').addEventListener('dragleave',()=>$('#dropzone').classList.remove('dragover'));$('#dropzone').addEventListener('drop',e=>{e.preventDefault();$('#dropzone').classList.remove('dragover');toast('请在系统文件选择器中选择这些文件');native('pick');});
addEventListener('resize',()=>{motion.interrupt();stopSpring();document.body.classList.remove('gesturing');resizeGeometry();paint(motion.busy?1:0);requestBake();});addEventListener('scroll',requestBake,{passive:true});document.addEventListener('visibilitychange',()=>{if(document.hidden){motion.interrupt();stopSpring();paint(motion.busy?1:0);}});
$('#licenses-button').onclick=()=>{$('#license-dialog').showModal();$('#license-text').textContent=window.QINGZHUAN_LICENSES||'许可见源码包 THIRD_PARTY_NOTICES.txt 与 licenses/。';};$('#close-license').onclick=()=>$('#license-dialog').close();
resizeGeometry();applyPreferences();native('sync');
})();
