#!/usr/bin/env python3
"""Verify a Qingzhuan 1.5.5 stable APK without reading signing secrets."""
from pathlib import Path
import argparse, hashlib, os, re, subprocess, sys

EXPECTED_PACKAGE = "cn.qingzhuan.toolbox"
EXPECTED_VERSION_CODE = "2"
EXPECTED_VERSION_NAME = "1.5.5"
EXPECTED_CERT_SHA256 = "a077d92842d12b607fede22c6168bc0fff92dcbf9c515529f60d5361f4bd2198"
EXPECTED_SHA256 = "81ff97463de6c1222434a6fe72dee36d29c0408cc7ee413a7796678df7a78dff"

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("apk",nargs="?",default="Qingzhuan-Toolbox-1.5.5.apk")
    args=parser.parse_args()
    apk=Path(args.apk).resolve()
    if not apk.is_file(): raise SystemExit(f"APK not found: {apk}")
    sdk=Path(os.environ.get("ANDROID_HOME", os.environ.get("ANDROID_SDK_ROOT", "~/Android/Sdk"))).expanduser()
    build_tools=Path(os.environ.get("QZ_BUILD_TOOLS", str(sdk/"build-tools/36.0.0")))
    aapt=build_tools/"aapt"; signer=build_tools/"apksigner"; align=build_tools/"zipalign"
    for tool in (aapt,signer,align):
        if not tool.is_file(): raise SystemExit(f"Required Android Build Tool not found: {tool}")
    badging=subprocess.check_output([str(aapt),"dump","badging",str(apk)],text=True)
    match=re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'",badging)
    assert match and match.groups()==(EXPECTED_PACKAGE,EXPECTED_VERSION_CODE,EXPECTED_VERSION_NAME), "unexpected APK package/version"
    assert "android.permission.INTERNET" not in badging, "unexpected INTERNET permission"
    signature=subprocess.check_output([str(signer),"verify","--verbose","--print-certs",str(apk)],text=True)
    assert "Verified using v2 scheme (APK Signature Scheme v2): true" in signature, "APK v2 signature invalid"
    assert EXPECTED_CERT_SHA256 in signature, "unexpected signing certificate"
    subprocess.run([str(align),"-c","-P","16","4",str(apk)],check=True,stdout=subprocess.DEVNULL)
    digest=hashlib.sha256(apk.read_bytes()).hexdigest()
    assert digest==EXPECTED_SHA256, "APK SHA-256 differs from release record"
    print(f"Verified stable APK: {apk.name}")
    print(f"package={EXPECTED_PACKAGE} versionCode={EXPECTED_VERSION_CODE} versionName={EXPECTED_VERSION_NAME}")
    print(f"bytes={apk.stat().st_size} sha256={digest}")
    print(f"certificateSha256={EXPECTED_CERT_SHA256}; v2 signature valid; 16 KiB ZIP alignment valid; INTERNET permission absent")

if __name__=="__main__":
    try: main()
    except (AssertionError,subprocess.CalledProcessError) as exc: print(f"Verification failed: {exc}",file=sys.stderr); raise SystemExit(1)
