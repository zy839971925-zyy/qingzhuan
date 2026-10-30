#!/usr/bin/env python3
"""Build Qingzhuan. Release builds require the separately stored production signing key."""
from pathlib import Path
import argparse, os, subprocess, shutil, sys

root = Path(__file__).resolve().parent
parser = argparse.ArgumentParser()
parser.add_argument('--variant', choices=['debug', 'release'], default='release')
args = parser.parse_args()
env = dict(os.environ)
java = str(Path(env['JAVA_HOME']) / 'bin/java') if env.get('JAVA_HOME') else shutil.which('java')
if not java:
    raise SystemExit('Install JDK17+ and set JAVA_HOME.')
if args.variant == 'release':
    required = ['QINGZHUAN_RELEASE_KEYSTORE', 'QINGZHUAN_RELEASE_STORE_PASSWORD',
                'QINGZHUAN_RELEASE_KEY_ALIAS', 'QINGZHUAN_RELEASE_KEY_PASSWORD']
    missing = [name for name in required if not env.get(name)]
    if missing:
        raise SystemExit('Release build requires the production signing credentials in environment variables: ' + ', '.join(missing))
    key = Path(env['QINGZHUAN_RELEASE_KEYSTORE']).expanduser().resolve()
    if not key.is_file():
        raise SystemExit('The configured production signing keystore does not exist.')
    env['QINGZHUAN_RELEASE_KEYSTORE'] = str(key)
else:
    key = Path(env.get('QINGZHUAN_DEBUG_KEYSTORE', str(root.parent / 'private-signing/qingzhuan-preview.keystore'))).resolve()
    key.parent.mkdir(parents=True, exist_ok=True)
    if not key.exists():
        keytool = str(Path(env['JAVA_HOME']) / 'bin/keytool') if env.get('JAVA_HOME') else shutil.which('keytool')
        if not keytool:
            raise SystemExit('JDK keytool is required for a local development signing certificate.')
        subprocess.run([keytool, '-genkeypair', '-noprompt', '-keystore', str(key), '-alias', 'androiddebugkey',
                        '-storepass', 'android', '-keypass', 'android', '-dname', 'CN=Qingzhuan Development',
                        '-keyalg', 'RSA', '-keysize', '3072', '-validity', '10000'], check=True)
    env['QINGZHUAN_DEBUG_KEYSTORE'] = str(key)

subprocess.run(['sh', str(root / 'android/gradlew'), ':app:assemble' + args.variant.title(),
                '--console=plain', '--no-daemon'], cwd=root / 'android', env=env, check=True)
source = root / 'android/app/build/outputs/apk' / args.variant / ('app-' + args.variant + '.apk')
output = root / ('Qingzhuan-Toolbox-1.5.5.apk' if args.variant == 'release' else 'Qingzhuan-1.5.5-debug.apk')
shutil.copyfile(source, output)
print(('Stable production APK:' if args.variant == 'release' else 'Development APK:'), output)
