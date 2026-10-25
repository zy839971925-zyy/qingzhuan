#!/usr/bin/env python3
"""Offline Java/SDK CLI build. Debug signing only by default; release keys are external."""
import os,pathlib,subprocess,zipfile,shutil,argparse
R=pathlib.Path(__file__).resolve().parent
p=argparse.ArgumentParser();p.add_argument('--compile-only',action='store_true');p.add_argument('--source-root',type=pathlib.Path,default=R);a=p.parse_args();SRC=a.source_root.resolve()
J=pathlib.Path(os.environ.get('JAVA_HOME','/usr/lib/jvm/java-17-openjdk-amd64'))
SDK=pathlib.Path(os.environ.get('ANDROID_SDK_ROOT',str(R.parent/'tooling/sdk')))
BT=next((x for x in [SDK/'android-16',SDK/'build-tools/36.0.0',SDK/'android-15',SDK/'build-tools/35.0.0'] if (x/'aapt2').exists()),SDK/'build-tools/36.0.0')
ANDROID=next((x for x in [SDK/'android-36/android.jar',SDK/'platforms/android-36/android.jar',SDK/'android-35/android.jar',SDK/'platforms/android-35/android.jar'] if x.exists()),SDK/'platforms/android-36/android.jar')
os.environ['LD_LIBRARY_PATH']=os.pathsep.join([str(J/'lib'),str(J/'lib/server'),str(BT/'lib64'),os.environ.get('LD_LIBRARY_PATH','')])
B=R/('build-baseline' if SRC!=R else 'build');B.mkdir(exist_ok=True)
for folder in ['classes','dex']:
 shutil.rmtree(B/folder,ignore_errors=True);(B/folder).mkdir()
libs=sorted((SRC/'libs').glob('*.jar'));java=J/'bin/java'
def run(args):
 print('Running',pathlib.Path(str(args[0])).name,flush=True);subprocess.run(list(map(str,args)),check=True,cwd=SRC)
if not ANDROID.exists() or not (BT/'aapt2').exists():raise SystemExit('Android SDK missing: set ANDROID_SDK_ROOT (platform 36 + build tools 36.0.0).')
run([java,'-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-source','8','-target','8','-cp',os.pathsep.join(map(str,[ANDROID]+libs)),'-d',B/'classes']+sorted((SRC/'app/src/main/java').rglob('*.java')))
if a.compile_only:raise SystemExit(0)
# Stage assets; never mutate source HTML during a build.
staging=B/'assets';shutil.rmtree(staging,ignore_errors=True);shutil.copytree(SRC/'app/src/main/assets',staging)
run([BT/'aapt2','compile','--dir',SRC/'app/src/main/res','-o',B/'resources.zip'])
run([BT/'aapt2','link','-o',B/'base.apk','--manifest',SRC/'app/src/main/AndroidManifest.xml','-I',ANDROID,'--min-sdk-version','26','--auto-add-overlay','-A',staging,B/'resources.zip'])
with zipfile.ZipFile(B/'code.jar','w') as z:
 for f in (B/'classes').rglob('*.class'):z.write(f,f.relative_to(B/'classes'))
run([java,'-Xmx2g','-cp',BT/'lib/d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',ANDROID,'--output',B/'dex',B/'code.jar']+libs)
shutil.copy(B/'base.apk',B/'unsigned.apk')
with zipfile.ZipFile(B/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
 for f in (B/'dex').glob('*.dex'):z.write(f,f.name)
run([BT/'zipalign','-f','4',B/'unsigned.apk',B/'aligned.apk'])
key=pathlib.Path(os.environ.get('QINGZHUAN_DEBUG_KEYSTORE',str(R.parent/'private-signing/debug.keystore')));key.parent.mkdir(exist_ok=True)
if not key.exists():
 run([J/'bin/keytool','-genkeypair','-noprompt','-keystore',key,'-alias','androiddebugkey','-storepass','android','-keypass','android','-dname','CN=Android Debug,O=Android,C=US','-keyalg','RSA','-keysize','2048','-validity','10000'])
apk=R/'Qingzhuan-1.2.0-debug.apk'
run([java,'-jar',BT/'lib/apksigner.jar','sign','--ks',key,'--ks-key-alias','androiddebugkey','--ks-pass','pass:android','--key-pass','pass:android','--out',apk,B/'aligned.apk'])
run([java,'-jar',BT/'lib/apksigner.jar','verify','--verbose',apk])
run([BT/'aapt2','dump','badging',apk]);print('DEBUG APK:',apk)
