"""Optional JVM UI test runtime; normal APK builds continue using JDK17."""
from pathlib import Path
import urllib.request,shutil,tarfile
r=Path(__file__).resolve().parent
folder=r/'zulu21.46.19-ca-jdk21.0.9-linux_x64'
if not folder.is_dir():
 p=r/'jdk21.tar.gz'
 with urllib.request.urlopen('https://cdn.azul.com/zulu/bin/'+folder.name+'.tar.gz',timeout=60) as src,p.open('wb') as dest:shutil.copyfileobj(src,dest,1024*1024)
 with tarfile.open(p) as t:t.extractall(r,filter='data')
 p.unlink()
print(folder/'bin/java')
