/* Event-driven BottomSheet interaction for the existing local WebView UI.
   Snap positions are measured once; dragging changes only transform/opacity.
   No idle frame loop. Touch scrolling remains native until a top-edge gesture is claimed. */
(function(global){
'use strict';
const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
function geometry(viewport,natural){
  const max=Math.max(1,Math.min(viewport-12,viewport*.94));
  const height=clamp(natural||max,Math.min(180,max),max);
  return {height,collapsed:Math.max(0,height-Math.max(Math.min(320,max),viewport*.62))};
}
function releaseSnap({start,offset,velocity,delta,collapsed,height,cancelled}){
  if(cancelled)return start;
  if(delta>0&&offset>collapsed+Math.max(72,height*.13))return 'hidden';
  if(velocity>.7&&delta>28&&(start==='collapsed'||offset>collapsed+24))return 'hidden';
  if(velocity<-.45&&delta<-18)return 'expanded';
  if(velocity>.45&&delta>18)return 'collapsed';
  if(Math.abs(delta)<10)return start;
  return offset<collapsed*.5?'expanded':'collapsed';
}
const rubber=value=>value<0?-Math.min(18,Math.sqrt(-value)*2):value;
class SheetHost {
  constructor({root,reduced=()=>false,onDismiss=()=>{},onFeedback=()=>{},onBlocked=()=>{}}){
    this.root=root;this.reduced=reduced;this.onDismiss=onDismiss;this.onFeedback=onFeedback;this.onBlocked=onBlocked;
    this.cleanups=[];this.timers=new Set();this.frame=0;this.resizeFrame=0;this.openFrame=0;this.gesture=null;this.stable='collapsed';this.phase='hidden';this.offset=0;this.token=0;
  }
  listen(el,type,fn,options){el.addEventListener(type,fn,options);this.cleanups.push(()=>el.removeEventListener(type,fn,options));}
  later(fn,ms){const id=setTimeout(()=>{this.timers.delete(id);fn();},ms);this.timers.add(id);return id;}
  clearJobs(){for(const id of this.timers)clearTimeout(id);this.timers.clear();for(const key of ['frame','resizeFrame','openFrame'])if(this[key]){cancelAnimationFrame(this[key]);this[key]=0;}}
  mount(content,{modal=false,locked=false,snap='collapsed',label='轻转提示',onDismiss}={}){
    const origin=this.shell&&this.shell.contains(document.activeElement)?this.returnFocus:document.activeElement;this.dispose();this.token++;this.modal=modal;this.locked=locked;this.onClosed=onDismiss||this.onDismiss;this.returnFocus=origin;
    this.root.innerHTML='';const shade=document.createElement('div');shade.className='scrim'+(modal?' modal':'');shade.id='scrim';
    const shell=document.createElement('section');shell.className='sheet sheet-shell';shell.setAttribute('role','dialog');shell.setAttribute('aria-modal','true');shell.setAttribute('aria-label',label);shell.tabIndex=-1;
    if(!modal){const grip=document.createElement('button');grip.className='sheet-grip';grip.type='button';grip.setAttribute('aria-label','展开面板');grip.innerHTML='<span class="handle"></span><span class="grip-label">上滑展开</span>';shell.appendChild(grip);this.grip=grip;}
    const body=document.createElement('div');body.className='sheet-content';body.innerHTML=content;shell.appendChild(body);
    const footer=Array.from(body.children).find(el=>el.classList.contains('sheet-footer'));if(footer)shell.appendChild(footer);
    shade.appendChild(shell);this.root.appendChild(shade);this.shade=shade;this.shell=shell;this.body=body;this.footer=footer;
    document.body.style.overflow='hidden';document.querySelectorAll('main,nav').forEach(el=>{el.setAttribute('aria-hidden','true');el.setAttribute('inert','');});
    this.stable=snap;this.phase=snap;this.measure();
    this.listen(shade,'click',e=>{if(e.target===shade){e.stopPropagation();this.requestClose();}});
    this.listen(shell,'click',e=>{if(performance.now()<(this.suppressUntil||0)){e.preventDefault();e.stopImmediatePropagation();}},true);
    this.listen(shell,'keydown',e=>this.key(e));
    this.listen(shell,'focusin',e=>{if(!modal&&e.target.matches('input:not([type=checkbox]):not([type=range]),textarea,select'))this.setSnap('expanded');});
    if(!modal){
      this.listen(this.grip,'click',()=>{this.setSnap(this.stable==='expanded'?'collapsed':'expanded');});
      this.listen(shell,'touchstart',e=>{if(e.touches.length===1)this.begin(e.touches[0],e.target,'touch');else this.cancelGesture();},{passive:true});
      this.listen(shell,'touchmove',e=>{if(e.touches.length!==1){this.cancelGesture();return;}this.move(e.touches[0],e);},{passive:false});
      this.listen(shell,'touchend',()=>this.end(false),{passive:true});this.listen(shell,'touchcancel',()=>this.end(true),{passive:true});
      this.listen(shell,'pointerdown',e=>{if(e.pointerType==='touch'||e.button!==0||!e.target.closest('.sheet-grip,.sheet-header'))return;this.begin(e,e.target,'pointer');});
      this.listen(document,'pointermove',e=>{if(this.gesture&&this.gesture.type==='pointer'&&e.pointerId===this.gesture.id)this.move(e,e);},{passive:false});
      this.listen(document,'pointerup',e=>{if(this.gesture&&this.gesture.type==='pointer'&&e.pointerId===this.gesture.id)this.end(false);});
      this.listen(document,'pointercancel',e=>{if(this.gesture&&this.gesture.type==='pointer'&&e.pointerId===this.gesture.id)this.end(true);});
      const resize=()=>{if(this.resizeFrame)return;this.resizeFrame=requestAnimationFrame(()=>{this.resizeFrame=0;this.cancelGesture();this.measure();this.setSnap(this.stable,false);});};
      this.listen(window,'resize',resize);if(window.visualViewport)this.listen(window.visualViewport,'resize',resize);
    }
    this.listen(document,'visibilitychange',()=>{if(document.hidden)this.suspend();});
    if(modal){this.phase='expanded';shell.dataset.state='expanded';}
    else{
      this.paint(this.metrics.height+24);this.phase='opening';shell.dataset.state='opening';
      if(this.reduced())this.setSnap(this.stable,false,false);else this.openFrame=requestAnimationFrame(()=>{this.openFrame=0;this.setSnap(this.stable,true,false);});
    }
    const focus=modal?(body.querySelector('[data-action="close"]')||body.querySelector('button')):this.grip;(focus||shell).focus({preventScroll:true});
  }
  viewport(){return Math.max(1,Math.min(window.innerHeight||844,window.visualViewport?window.visualViewport.height:Infinity));}
  measure(){if(!this.shell||this.modal)return;this.body.style.height='auto';this.gripHeight=this.grip.offsetHeight||44;this.footerHeight=this.footer?this.footer.offsetHeight:0;const natural=this.body.scrollHeight+this.gripHeight+this.footerHeight;this.metrics=geometry(this.viewport(),natural);this.shell.style.height=this.metrics.height+'px';this.shell.style.maxHeight=this.metrics.height+'px';this.sizeBody(this.stable==='expanded'?0:this.metrics.collapsed);this.updateGrip();}
  sizeBody(offset){if(!this.body||!this.metrics)return;this.body.style.flex='none';this.body.style.height=Math.max(0,this.metrics.height-this.gripHeight-this.footerHeight-clamp(offset,0,this.metrics.collapsed))+'px';}
  updateGrip(){if(!this.grip)return;const expandable=this.metrics.collapsed>12;this.grip.setAttribute('aria-expanded',String(this.stable==='expanded'||!expandable));this.grip.setAttribute('aria-label',expandable?(this.stable==='expanded'?'收起面板':'展开面板'):'面板已展开，可下滑关闭');this.grip.querySelector('.grip-label').textContent=expandable?(this.stable==='expanded'?'下滑收起':'上滑展开'):'下滑关闭';}
  paint(offset){if(!this.shell)return;this.offset=offset;this.shell.style.transform='translate3d(0,'+offset+'px,0)';if(this.footer)this.footer.style.transform='translate3d(0,'+(-clamp(offset,0,this.metrics.collapsed))+'px,0)';const fade=clamp(1-Math.max(0,offset-this.metrics.collapsed)/Math.max(1,this.metrics.height),0,1);this.shade.style.setProperty('--shade',String(fade));}
  setSnap(snap,animate=true,notify=true){
    if(!this.shell||this.modal||this.phase==='closing')return;if(snap==='hidden'){this.requestClose();return;}
    const previous=this.stable;this.stable=this.metrics.collapsed<=12?'expanded':snap;this.clearJobs();
    this.shell.classList.toggle('settling',animate&&!this.reduced());this.shell.style.willChange=animate&&!this.reduced()?'transform':'';if(this.footer)this.footer.style.willChange=this.shell.style.willChange;
    this.sizeBody(animate&&!this.reduced()?0:(this.stable==='expanded'?0:this.metrics.collapsed));this.phase=animate&&!this.reduced()?'settling':this.stable;this.shell.dataset.state=this.phase;this.shade.dataset.dragging='false';this.paint(this.stable==='expanded'?0:this.metrics.collapsed);this.updateGrip();
    if(previous!==this.stable&&notify)this.onFeedback();
    if(this.phase==='settling'){const token=this.token;this.later(()=>{if(this.token!==token||!this.shell)return;this.shell.classList.remove('settling');this.shell.style.willChange='';if(this.footer)this.footer.style.willChange='';this.sizeBody(this.stable==='expanded'?0:this.metrics.collapsed);this.phase=this.stable;this.shell.dataset.state=this.stable;},240);}
  }
  scrollAtTop(target){for(let n=target;n&&n!==this.shell;n=n.parentElement){if(n.scrollTop>0)return false;}return true;}
  begin(point,target,type){
    if(!this.shell||this.modal||this.locked||this.gesture||this.phase==='closing')return;
    if(target.closest('input,select,textarea,a,[contenteditable="true"]')||target.closest('button:not(.sheet-grip)'))return;
    this.gesture={type,id:point.pointerId,x:point.clientX,y:point.clientY,lastY:point.clientY,lastTime:performance.now(),velocity:0,start:this.stable,offset:this.stable==='expanded'?0:this.metrics.collapsed,owned:false,grip:!!target.closest('.sheet-grip,.sheet-header'),atTop:this.scrollAtTop(target),delta:0};
  }
  move(point,event){
    const g=this.gesture;if(!g)return;const dx=point.clientX-g.x,dy=point.clientY-g.y;
    if(!g.owned){if(Math.max(Math.abs(dx),Math.abs(dy))<8)return;if(Math.abs(dx)>Math.abs(dy)*1.15||(!g.grip&&!g.atTop)){this.gesture=null;return;}if(!g.grip&&dy<0&&g.start==='expanded'){this.gesture=null;return;}
      this.clearJobs();g.owned=true;this.phase='dragging';this.shell.dataset.state='dragging';this.shade.dataset.dragging='true';this.shell.classList.remove('settling');this.shell.style.willChange='transform';if(this.footer)this.footer.style.willChange='transform';this.sizeBody(0);
    }
    if(event.cancelable)event.preventDefault();const now=performance.now(),dt=Math.max(8,now-g.lastTime);g.velocity=.65*((point.clientY-g.lastY)/dt)+.35*g.velocity;g.lastY=point.clientY;g.lastTime=now;g.delta=dy;g.next=rubber(g.offset+dy);
    if(!this.frame)this.frame=requestAnimationFrame(()=>{this.frame=0;if(this.gesture===g)this.paint(g.next);});
  }
  end(cancelled){const g=this.gesture;if(!g)return;this.gesture=null;if(this.frame){cancelAnimationFrame(this.frame);this.frame=0;}if(!g.owned)return;this.suppressUntil=performance.now()+350;const offset=g.next===undefined?g.offset:g.next;this.paint(offset);const velocity=performance.now()-g.lastTime>120?0:g.velocity;const next=releaseSnap({start:g.start,offset,velocity,delta:g.delta,collapsed:this.metrics.collapsed,height:this.metrics.height,cancelled});if(next==='hidden')this.requestClose();else this.setSnap(next);}
  cancelGesture(){if(this.gesture)this.end(true);}
  requestClose({immediate=false}={}){
    if(!this.shell)return;if(this.phase==='closing'){if(immediate&&this.finishClose)this.finishClose();return;}if(this.locked){this.onBlocked();return;}
    this.gesture=null;this.clearJobs();const done=this.onClosed;
    const finish=()=>{this.dispose();this.root.innerHTML='';document.body.style.overflow='';document.querySelectorAll('main,nav').forEach(el=>{el.removeAttribute('aria-hidden');el.removeAttribute('inert');});if(this.returnFocus&&this.returnFocus.isConnected)this.returnFocus.focus({preventScroll:true});done();};
    this.finishClose=finish;if(immediate||this.reduced()||document.hidden){finish();return;}
    this.phase='closing';this.shell.dataset.state='closing';this.shade.classList.add('closing');this.shell.style.willChange='transform';if(!this.modal){this.shell.classList.add('settling');this.paint(this.metrics.height+24);}this.later(finish,210);
  }
  key(e){
    if(e.key==='Escape'){e.preventDefault();this.requestClose();return;}
    if(e.target===this.grip&&['ArrowUp','ArrowDown','Home','End'].includes(e.key)){e.preventDefault();this.setSnap(e.key==='ArrowUp'||e.key==='Home'?'expanded':'collapsed');return;}
    if(e.key==='Tab'){const items=Array.from(this.shell.querySelectorAll('button:not([disabled]),input:not([disabled]),select:not([disabled]),textarea:not([disabled]),a[href],[tabindex]')).filter(n=>n.tabIndex>=0&&!n.closest('.hidden,[hidden]'));if(!items.length){e.preventDefault();this.shell.focus();return;}const first=items[0],last=items[items.length-1];if(e.shiftKey&&(document.activeElement===first||document.activeElement===this.shell)){e.preventDefault();last.focus();}else if(!e.shiftKey&&document.activeElement===last){e.preventDefault();first.focus();}}
  }
  suspend(){if(!this.shell)return;this.cancelGesture();if(this.phase==='closing'){this.requestClose({immediate:true});return;}this.clearJobs();if(!this.modal)this.setSnap(this.stable,false,false);}
  snapshot(){return {phase:this.phase,snap:this.stable,offset:this.offset,metrics:this.metrics,framePending:!!(this.frame||this.resizeFrame||this.openFrame),timerCount:this.timers.size};}
  dispose(){this.clearJobs();for(const off of this.cleanups.splice(0))off();this.gesture=null;if(this.shell)this.shell.style.willChange='';if(this.footer)this.footer.style.willChange='';this.finishClose=null;this.shell=null;this.body=null;this.grip=null;this.footer=null;this.shade=null;this.phase='hidden';}
}
global.SheetMath={geometry,releaseSnap,rubber};global.SheetHost=SheetHost;
})(typeof window!=='undefined'?window:globalThis);
