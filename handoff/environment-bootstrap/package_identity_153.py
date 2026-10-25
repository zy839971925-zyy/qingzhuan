from pathlib import Path
import subprocess,os,hashlib,json,zipfile,shutil,re,io
from PIL import Image
w=Path(__file__).resolve().parent.parent;r=w/'Qingzhuan-next';d=w/'deliverables';sdk=w/'tooling/sdk/build-tools/36.0.0'
apk=d/'Qingzhuan-1.5.3-glass.apk'
shutil.copyfile(r/'android/app/build/outputs/apk/release/app-release.apk',apk)
env=dict(os.environ);env['JAVA_HOME']=str(w/'tooling/zulu17.62.17-ca-jdk17.0.17-linux_x64');env['PATH']=env['JAVA_HOME']+'/bin:'+env['PATH']
sign=subprocess.check_output([str(sdk/'apksigner'),'verify','--verbose','--print-certs',str(apk)],env=env,text=True)
cert='f253392041358c9edff32e76590aaa8de02725b1806183ae6ab6ba3eb8173286';assert cert in sign
(d/'Qingzhuan-1.5.3-signing.txt').write_text(sign)
subprocess.run([str(sdk/'zipalign'),'-c','-P','16','4',str(apk)],env=env,check=True)
badging=subprocess.check_output([str(sdk/'aapt'),'dump','badging',str(apk)],env=env,text=True)
assert "package: name='cn.qingzhuan.app.preview' versionCode='11' versionName='1.5.3-glass'" in badging
assert "application-label:'轻转预览 1.5.3'" in badging
assert 'android.permission.INTERNET' not in badging
manifest=subprocess.check_output([str(sdk/'aapt'),'dump','xmltree',str(apk),'AndroidManifest.xml'],env=env,text=True)
for cls in ['NativePreviewActivity','ConversionService']:
 part=manifest.split('cn.qingzhuan.app.'+cls,1)[1].split('E: ',1)[0]
 assert 'android:exported' in part and '0x0' in part
resources=subprocess.check_output([str(sdk/'aapt'),'dump','--values','resources',str(apk)],env=env,text=True)
with zipfile.ZipFile(apk) as z:
 assert z.testzip() is None
 dex=b''.join(z.read(x) for x in z.namelist() if x.endswith('.dex'))
 for cls in ['NativeMediaEngine','GifWriter','MotionPhoto','ConversionService','NativePreviewActivity','GlassSheetHostKt','ElasticPreviewMotion','BackgroundCatalog']:
  assert ('Lcn/qingzhuan/app/'+cls+';').encode() in dex,cls
 assert b'1.5.3' in z.read('assets/licenses.txt')
 for name in ['mist','dune','iris']:
  for suffix in ['', '_thumb']:
   entry=re.search(r'resource 0x[0-9a-f]+ [^\n]*:drawable/wallpaper_'+name+suffix+r':[^\n]*\n\s+\(string8\) "([^"]+)"',resources)
   assert entry,(name,suffix)
   with Image.open(io.BytesIO(z.read(entry.group(1)))) as image:
    assert image.size==((135,240) if suffix else (720,1280));image.verify()
record={'version':'1.5.3-glass','versionCode':11,'applicationId':'cn.qingzhuan.app.preview','label':'轻转预览 1.5.3','bytes':apk.stat().st_size,'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'certificateSha256':cert,'signatureVerified':True,'zipalign16KB':True,'internetPermission':False,'privatePreviewAndService':True,'nativeMediaAndGlassClassesInDex':True,'presetImages':6,'buildLog':'build-153-final.log','buildSourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=r,text=True).strip(),'minimalChecks':'Kotlin compile + fresh read-only source review/fixes + clean release build + package/signature/resource identity; no broad regression suite','deviceValidation':'not performed','emulator':False}
(d/'APK-build-153.json').write_text(json.dumps(record,ensure_ascii=False,indent=2)+'\n')
with zipfile.ZipFile(d/'Qingzhuan-1.5.3-install.zip','w',zipfile.ZIP_DEFLATED) as z:z.write(apk,apk.name)
print(json.dumps(record,ensure_ascii=False,indent=2))
