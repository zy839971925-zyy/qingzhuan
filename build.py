#!/usr/bin/env python3
"""Native Compose build. SDK36 + JDK17+; no Node, React Native or NDK setup."""
from pathlib import Path
import argparse,os,subprocess,shutil
root=Path(__file__).resolve().parent
parser=argparse.ArgumentParser();parser.add_argument('--variant',choices=['debug','release'],default='release');args=parser.parse_args()
env=dict(os.environ);java=str(Path(env['JAVA_HOME'])/'bin/java') if env.get('JAVA_HOME') else shutil.which('java')
if not java:raise SystemExit('Install JDK21 (Android Studio bundled JBR works) and set JAVA_HOME.')
key=Path(env.get('QINGZHUAN_DEBUG_KEYSTORE',str(root.parent/'private-signing/qingzhuan-preview.keystore'))).resolve();key.parent.mkdir(parents=True,exist_ok=True)
if not key.exists():
 keytool=str(Path(env['JAVA_HOME'])/'bin/keytool') if env.get('JAVA_HOME') else shutil.which('keytool')
 if not keytool:raise SystemExit('JDK keytool is required for a local development signing certificate.')
 subprocess.run([keytool,'-genkeypair','-noprompt','-keystore',str(key),'-alias','androiddebugkey','-storepass','android','-keypass','android','-dname','CN=Qingzhuan Preview Development','-keyalg','RSA','-keysize','3072','-validity','10000'],check=True)
env['QINGZHUAN_DEBUG_KEYSTORE']=str(key)
subprocess.run(['sh',str(root/'android/gradlew'),':app:assemble'+args.variant.title(),'--console=plain'],cwd=root/'android',env=env,check=True)
source=root/'android/app/build/outputs/apk'/args.variant/('app-'+args.variant+'.apk');output=root/'Qingzhuan-1.5.4-refined.apk';shutil.copyfile(source,output)
print('Native Compose test APK:',output)
print('Private development certificate, separate cn.qingzhuan.app.preview package; device verification still required.')
