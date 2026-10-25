"""Atomic rolling source checkpoint; never includes user media or private keys."""
from pathlib import Path
import subprocess, tarfile, zipfile, io, json, hashlib, tempfile, shutil, argparse, datetime
w=Path(__file__).resolve().parent.parent
r=w/'Qingzhuan-next'; out=w/'deliverables'; out.mkdir(exist_ok=True)
p=argparse.ArgumentParser();p.add_argument('stage');p.add_argument('--checks',default='not built in this checkpoint');a=p.parse_args()
assert not subprocess.check_output(['git','status','--porcelain'],cwd=r,text=True).strip(), 'Commit all tracked work first'
commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=r,text=True).strip()
branch=subprocess.check_output(['git','branch','--show-current'],cwd=r,text=True).strip()
with tempfile.TemporaryDirectory(prefix='qz-153-checkpoint-',dir=out) as temporary:
 stage=Path(temporary)
 with tarfile.open(fileobj=io.BytesIO(subprocess.check_output(['git','archive','HEAD'],cwd=r))) as t:t.extractall(stage,filter='data')
 hand=stage/'handoff';hand.mkdir(exist_ok=True)
 subprocess.run(['git','bundle','create',str(hand/'Qingzhuan-history.git.bundle'),'--all'],cwd=r,check=True)
 subprocess.run(['git','bundle','verify',str(hand/'Qingzhuan-history.git.bundle')],cwd=r,check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
 record={'delivery_kind':'work-in-progress' if a.stage!='release' else 'partial-device-validation-pending','stage':a.stage,'git_commit':commit,'branch':branch,'utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'checks':a.checks,'no_emulator':True,'device_validation':'not performed','private_key_included':False,'read_next':['docs/DEVELOPMENT_1_5_3.md','docs/superpowers/plans/2026-10-25-glass-polish-1.5.3.md'],'resume':'Extract source; clone full Git bundle if history needed. Restore separately saved qingzhuan-preview.keystore outside project. Open android/ or use build.py. For release checkpoints, implementation is complete and only the explicitly pending device acceptance/feedback remains; do not repeat completed tasks.'}
 (hand/'RESUME.json').write_text(json.dumps(record,ensure_ascii=False,indent=2)+'\n')
 logs=hand/'logs';logs.mkdir()
 for log in (w/'tooling').glob('*153*.log'):shutil.copyfile(log,logs/log.name)
 boot=hand/'environment-bootstrap';boot.mkdir()
 for name in ['restore_toolchain.py','run_gradle.py','checkpoint_153.py','package_identity_153.py']:
  if (w/'tooling'/name).is_file():shutil.copyfile(w/'tooling'/name,boot/name)
 if a.stage=='release':
  for name in ['APK-build-153.json','Qingzhuan-1.5.3-signing.txt']:
   if (out/name).is_file():shutil.copyfile(out/name,hand/name)
 target=out/'Qingzhuan-1.5.3-source.zip';tmp=out/'Qingzhuan-1.5.3-source.tmp'
 with zipfile.ZipFile(tmp,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
  for f in sorted(stage.rglob('*')):
   if f.is_file():
    assert f.suffix not in ['.apk','.keystore','.jks'],str(f)
    z.write(f,f.relative_to(stage))
 with zipfile.ZipFile(tmp) as z:assert z.testzip() is None
 tmp.replace(target)
 record.update(path=str(target),bytes=target.stat().st_size,sha256=hashlib.sha256(target.read_bytes()).hexdigest())
 (out/'checkpoint-153.json').write_text(json.dumps(record,ensure_ascii=False,indent=2)+'\n')
 print(json.dumps(record,ensure_ascii=False,indent=2))
