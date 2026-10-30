"""Small host regression check of the actual production sizing policy. No Android emulator."""
from pathlib import Path
import subprocess,tempfile,os,shutil
root=Path(__file__).resolve().parents[1]
jdk=Path(os.environ.get('JAVA_HOME',root.parent/'tooling/zulu17.62.17-ca-jdk17.0.17-linux_x64'))/'bin'
source='''import cn.qingzhuan.app.ImageTransformPolicy;
public class ImagePolicySmoke {
 static int checks=0;
 static void expect(ImageTransformPolicy p,int w,int h,int q){if(p.width!=w||p.height!=h||p.quality!=q)throw new AssertionError(p.width+"x"+p.height+" q"+p.quality);checks++;}
 static ImageTransformPolicy plan(int w,int h,boolean a,String size,int cw,int ch,boolean lock,int q){return ImageTransformPolicy.resolve(w,h,a,size,cw,ch,lock,q);}
 public static void main(String[]args){
  expect(plan(6000,4000,false,"720p",1,1,false,20),6000,4000,100);
  expect(plan(7680,4320,true,"4k",0,0,true,96),3840,2160,96);
  expect(plan(4320,7680,true,"1080p",0,0,true,90),1080,1920,90);
  expect(plan(4000,3000,true,"720p",0,0,true,100),960,720,100);
  expect(plan(320,240,true,"4k",0,0,true,100),320,240,100);
  expect(plan(6000,4000,true,"custom",960,540,true,80),810,540,80);
  expect(plan(6000,4000,true,"custom",960,540,false,85),960,540,85);
  ImageTransformPolicy p=plan(7680,4320,true,"4k",0,0,true,100);
  if(p.sampleSize(7680,4320,false)!=2)throw new AssertionError("sample");checks++;
  ImageTransformPolicy portrait=plan(4320,7680,true,"4k",0,0,true,100);
  if(portrait.sampleSize(7680,4320,true)!=2)throw new AssertionError("Exif sample");checks++;
  try{plan(6000,4000,true,"custom",0,540,true,100);throw new AssertionError("zero accepted");}catch(IllegalArgumentException ok){checks++;}
  try{plan(32768,32768,false,"original",0,0,true,100);throw new AssertionError("pixel limit");}catch(IllegalArgumentException ok){checks++;}
  try{plan(100,100,true,"original",0,0,true,101);throw new AssertionError("quality");}catch(IllegalArgumentException ok){checks++;}
  System.out.println(checks+" image geometry/default/Exif/range checks passed");
 }
}'''
with tempfile.TemporaryDirectory() as tmp:
 d=Path(tmp);(d/'ImagePolicySmoke.java').write_text(source)
 subprocess.run([str(jdk/'javac'),'-d',tmp,str(root/'app/src/main/java/cn/qingzhuan/app/ImageTransformPolicy.java'),str(d/'ImagePolicySmoke.java')],check=True)
 subprocess.run([str(jdk/'java'),'-cp',tmp,'ImagePolicySmoke'],check=True)
