/* Original gesture state machine; no reference-repository implementation is embedded. */
(function(root){'use strict';
const config=Object.freeze({threshold:.92,r0:245,r1:44,center:.469,stiffness:160,damping:24,formatStagger:.08});
const clamp=v=>Math.max(0,Math.min(1,v));
function geometry(w,h,p){const s=Math.min(w,520)/402;return{r:(config.r0+(config.r1-config.r0)*clamp(p))*s,y:h+(h*config.center-h)*clamp(p),travel:h*(1-config.center)};}
function graphemes(s){return typeof Intl.Segmenter==='function'?Array.from(new Intl.Segmenter('zh',{granularity:'grapheme'}).segment(s),x=>x.segment):Array.from(s);}
function uid(){return globalThis.crypto?.randomUUID?.()||('task-'+Date.now().toString(36)+'-'+Math.random().toString(36).slice(2));}
class Gesture{
 constructor(submit){this.submit=submit;this.p=0;this.phase='idle';this.pointer=null;this.tool='';this.target='';this.id='';this.seq=-1;}
 get busy(){return ['running','cancelling'].includes(this.phase);}
 arm(tool,target){if(this.busy)return false;this.interrupt();this.tool=tool;this.target=target;this.phase=target?'armed':'idle';return true;}
 down(pointer,y){if(this.pointer!==null){this.interrupt();return false;}if(this.busy||!this.target)return false;this.pointer=pointer;this.start=y;this.origin=this.p;this.phase='dragging';return true;}
 move(pointer,y,travel){if(pointer!==this.pointer)return false;this.p=clamp(this.origin+(this.start-y)/Math.max(1,travel));return true;}
 up(pointer){if(this.pointer!==pointer)return false;this.pointer=null;if(this.p>=config.threshold)return this.commit();this.p=0;this.phase='armed';return false;}
 interrupt(){if(this.phase==='dragging'){this.pointer=null;this.p=0;this.phase=this.target?'armed':'idle';}}
 commit(){if(this.busy||!this.target)return false;this.pointer=null;this.p=1;this.phase='running';this.id=uid();this.seq=-1;this.submit(this.id);return true;}
 cancel(){if(this.busy)this.phase='cancelling';}
 reject(){this.pointer=null;this.p=0;this.phase=this.target?'armed':'idle';this.id='';this.seq=-1;}
 event(t){if(!t||t.id!==this.id||t.seq<=this.seq)return false;this.seq=t.seq;
  if(t.status==='running'||t.status==='cancelling'){this.phase=t.status;this.p=1;}
  else if(['complete','error','cancelled','interrupted'].includes(t.status)){this.p=0;this.phase=t.status==='complete'?'success':this.target?'armed':'idle';}
  return true;
 }
 restore(t){if(!t||!t.id)return false;this.id=t.id;this.seq=-1;return this.event(t);}
}
const api={Gesture,geometry,graphemes,config,clamp};if(typeof module==='object'&&module.exports)module.exports=api;else root.QMotion=api;
})(typeof window!=='undefined'?window:globalThis);
