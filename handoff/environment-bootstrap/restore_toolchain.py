from pathlib import Path
import urllib.request,xml.etree.ElementTree as ET,zipfile,tarfile,os,hashlib,concurrent.futures,shutil
r=Path(__file__).resolve().parent

def download(url,path):
 print('Downloading',path.name,flush=True)
 with urllib.request.urlopen(url,timeout=60) as src,path.open('wb') as dest:shutil.copyfileobj(src,dest,1024*1024)
 return path

def extract_zip(path,dest):
 with zipfile.ZipFile(path) as z:
  for item in z.infolist():
   z.extract(item,dest)
   mode=(item.external_attr>>16)&0o777
   if mode and not item.is_dir():(dest/item.filename).chmod(mode)

def gradle():
 p=download('https://downloads.gradle.org/distributions/gradle-8.13-bin.zip',r/'gradle.zip')
 assert hashlib.sha256(p.read_bytes()).hexdigest()=='20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78'
 extract_zip(p,r);p.unlink();return 'Gradle ready'
def jdk():
 p=download('https://cdn.azul.com/zulu/bin/zulu17.62.17-ca-jdk17.0.17-linux_x64.tar.gz',r/'jdk.tar.gz')
 with tarfile.open(p) as t:t.extractall(r,filter='data')
 p.unlink();return 'JDK ready'
def sdk():
 xml=urllib.request.urlopen('https://dl.google.com/android/repository/repository2-3.xml',timeout=60).read();root=ET.fromstring(xml)
 sdk=r/'sdk';(sdk/'licenses').mkdir(parents=True,exist_ok=True)
 for lic in root.findall('license'):
  if lic.text:(sdk/'licenses'/lic.attrib['id']).write_text(hashlib.sha1(lic.text.strip().encode()).hexdigest()+'\n')
 for package in ['platforms;android-36','build-tools;36.0.0']:
  node=next(x for x in root.findall('remotePackage') if x.attrib.get('path')==package)
  archives=node.findall('archives/archive');arc=next(x for x in archives if x.findtext('host-os') in ['linux',None])
  path=download('https://dl.google.com/android/repository/'+arc.findtext('complete/url'),r/(package.split(';')[0]+'.zip'))
  stage=r/('unpack-'+package.split(';')[0]);stage.mkdir();extract_zip(path,stage)
  first=next(stage.iterdir());target=sdk.joinpath(*package.split(';'));target.parent.mkdir(exist_ok=True)
  first.rename(target);stage.rmdir();path.unlink()
 return 'SDK ready'
with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
 for future in concurrent.futures.as_completed([pool.submit(gradle),pool.submit(jdk),pool.submit(sdk)]):print(future.result(),flush=True)
