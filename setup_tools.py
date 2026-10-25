#!/usr/bin/env python3
from pathlib import Path
import os,shutil
sdk=Path(os.environ.get('ANDROID_SDK_ROOT',os.environ.get('ANDROID_HOME',str(Path(__file__).resolve().parent.parent/'tooling/sdk'))))
checks={'JDK java':bool(shutil.which('java')) or (Path(os.environ.get('JAVA_HOME',''))/'bin/java').is_file(),'SDK platform36':(sdk/'platforms/android-36/android.jar').is_file(),'Build Tools36':(sdk/'build-tools/36.0.0/aapt2').is_file()}
for name,passed in checks.items():print(('OK ' if passed else 'MISSING ')+name)
print('APK build: JDK17+ (JDK21 recommended). Android15 JVM UI tests need JDK21. No Node/NDK/CMake required.')
raise SystemExit(0 if all(checks.values()) else 1)
