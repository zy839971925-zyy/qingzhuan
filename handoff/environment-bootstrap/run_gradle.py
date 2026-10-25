import os,subprocess,sys
from pathlib import Path
from urllib.parse import urlparse
r=Path(__file__).resolve().parent;e=dict(os.environ);p=urlparse(e.get('HTTPS_PROXY',''))
if p.hostname:e['JAVA_TOOL_OPTIONS']=f'-Dhttp.proxyHost={p.hostname} -Dhttp.proxyPort={p.port} -Dhttps.proxyHost={p.hostname} -Dhttps.proxyPort={p.port}'
e['JAVA_TOOL_OPTIONS']=e.get('JAVA_TOOL_OPTIONS','')+' -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts'
e['GRADLE_USER_HOME']=str(r/'gradle-home');e['JAVA_HOME']=str(r/'zulu17.62.17-ca-jdk17.0.17-linux_x64')
raise SystemExit(subprocess.run([str(r/'gradle-8.13/bin/gradle'),'--no-daemon','--console=plain','-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts']+([f'-Dhttp.proxyHost={p.hostname}',f'-Dhttp.proxyPort={p.port}',f'-Dhttps.proxyHost={p.hostname}',f'-Dhttps.proxyPort={p.port}'] if p.hostname else [])+sys.argv[1:],cwd=r.parent/'Qingzhuan-next/android',env=e).returncode)
