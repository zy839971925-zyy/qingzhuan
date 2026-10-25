const assert=require('node:assert/strict');const fs=require('node:fs');
const path='../app/src/main/assets/motion.js';
assert(fs.existsSync(require('node:path').join(__dirname,path)),'a real reversible gesture controller must exist');
const {Gesture,geometry,graphemes}=require(path);let starts=[],g=new Gesture(id=>starts.push(id));
assert.equal(g.down(10,20),false,'no gesture before target is chosen');g.arm('image','png');
for(const p of [.1,.3,.5,.7,.9]){g.down(1,500);g.move(1,500-400*p,400);assert(Math.abs(g.p-p)<1e-6);g.move(1,500,400);assert.equal(g.p,0);assert.equal(g.up(1),false);assert.equal(starts.length,0);}
g.down(1,500);g.move(1,124,400);assert.equal(g.up(1),true);assert.equal(starts.length,1);assert.equal(g.up(1),false);assert.equal(g.down(1,500),false);const id=starts[0];
assert.equal(g.event({id:'stale',seq:9,status:'complete'}),false);g.event({id,seq:4,status:'running'});assert.equal(g.event({id,seq:3,status:'complete'}),false);g.cancel();assert.equal(g.phase,'cancelling');g.event({id,seq:5,status:'cancelled'});assert.equal(g.phase,'armed');assert.equal(g.p,0);
g.down(3,500);g.move(3,100,400);g.interrupt();assert.equal(g.up(3),false);assert.equal(starts.length,1);
g.down(4,500);g.move(4,100,400);g.down(5,500);assert.equal(g.up(4),false,'multitouch interrupts');
const geom=geometry(402,874,0),sphere=geometry(402,874,1);assert.equal(geom.r,245);assert.equal(geom.y,874);assert.equal(sphere.r,44);assert.equal(sphere.y,874*.469);assert.deepEqual(graphemes('轻转👨‍👩‍👧‍👦'),['轻','转','👨‍👩‍👧‍👦']);
let reloaded=new Gesture(()=>{});reloaded.restore({id:'live',seq:2,status:'running',kind:'convert'});assert.equal(reloaded.p,1);assert.equal(reloaded.down(1,500),false);reloaded.event({id:'live',seq:3,status:'complete'});assert.equal(reloaded.phase,'success');assert.equal(reloaded.p,0);
console.log('PASS reversible gesture, threshold, dedupe, stale events, cancel ACK, recovery, geometry and graphemes');
