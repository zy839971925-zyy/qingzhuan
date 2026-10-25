"""Inspect the delivered binary; this is not an Android installation/run test."""
import argparse, hashlib, json, os, pathlib, re, struct, subprocess, zipfile, zlib

p = argparse.ArgumentParser()
p.add_argument('apk', type=pathlib.Path)
p.add_argument('--build-tools', required=True, type=pathlib.Path)
a = p.parse_args()
apk = a.apk.resolve()
def run(*args):
    return subprocess.check_output(list(map(str, args)), text=True, stderr=subprocess.STDOUT)
java = pathlib.Path(os.environ['JAVA_HOME']) / 'bin/java'
signature = run(java, '-jar', a.build_tools / 'lib/apksigner.jar', 'verify', '--verbose', '--print-certs', apk)
alignment = run(a.build_tools / 'zipalign', '-c', '-P', '16', '4', apk)
badging = run(a.build_tools / 'aapt2', 'dump', 'badging', apk)
permissions = run(a.build_tools / 'aapt2', 'dump', 'permissions', apk)
manifest = run(a.build_tools / 'aapt2', 'dump', 'xmltree', apk, '--file', 'AndroidManifest.xml')
resources = run(a.build_tools / 'aapt2', 'dump', 'resources', apk)
assert "package: name='cn.qingzhuan.app'" in badging
assert re.search(r"(?:minSdkVersion|sdkVersion):'26'", badging) and "targetSdkVersion:'36'" in badging
assert "launchable-activity: name='cn.qingzhuan.app.StartupActivity'" in badging
assert 'android.permission.INTERNET' not in permissions
assert 'ReactAppTheme' in resources
with zipfile.ZipFile(apk) as z:
    names = z.namelist()
    assert z.testzip() is None
    dex_reports = []
    app_classes = set()
    for name in names:
        if re.fullmatch(r'classes\d*\.dex', name):
            data = z.read(name)
            assert data[:4] == b'dex\n' and struct.unpack_from('<I', data, 32)[0] == len(data)
            assert hashlib.sha1(data[32:]).digest() == data[12:32]
            assert zlib.adler32(data[12:]) == struct.unpack_from('<I', data, 8)[0]
            string_count, string_offset, type_count, type_offset = struct.unpack_from('<IIII', data, 56)
            strings = []
            for i in range(string_count):
                pos = struct.unpack_from('<I', data, string_offset+i*4)[0]
                while data[pos] & 128: pos += 1
                pos += 1
                strings.append(data[pos:data.index(b'\0', pos)].decode('utf-8', errors='replace'))
            types = [strings[struct.unpack_from('<I', data, type_offset+i*4)[0]] for i in range(type_count)]
            class_count, class_offset = struct.unpack_from('<II', data, 96)
            app_classes.update(types[struct.unpack_from('<I', data, class_offset+i*32)[0]] for i in range(class_count))
            dex_reports.append({'file': name, 'bytes': len(data), 'header_integrity': 'passed'})
    for name in ['QingzhuanApplication', 'NativeHomeActivity', 'StartupActivity', 'StartupDiagnostics', 'StartupReportStore', 'QingzhuanModule', 'AppSession', 'Engine', 'OutputProvider']:
        assert f'Lcn/qingzhuan/app/{name};' in app_classes, f'missing startup/engine class: {name}'
    bundle = z.read('assets/index.android.bundle')
    assert len(bundle) > 100_000
    # Hermes bytecode magic, little endian.
    assert bundle[:8] == bytes.fromhex('c61fbc03c103191f')
    assert 'assets/licenses.txt' in names
    assert any(x.startswith('assets/com/tom_roush/pdfbox/') for x in names)
    assert not any(x.lower().endswith(('.keystore', '.jks')) for x in names)
    assert not any('gsap' in x.lower() or x == 'assets/index.html' for x in names)
    abis = sorted({x.split('/')[1] for x in names if x.startswith('lib/')})
    assert abis and all(abi in ['arm64-v8a', 'x86_64'] for abi in abis)
    elf = []
    for name in names:
        if name.startswith('lib/') and name.endswith('.so'):
            assert z.getinfo(name).compress_type == zipfile.ZIP_STORED, f'{name}: expected mmap-ready uncompressed library'
            data = z.read(name)
            assert data[:5] == b'\x7fELF\x02', f'{name}: expected ELF64'
            offset = struct.unpack_from('<Q', data, 32)[0]
            entry_size, count = struct.unpack_from('<HH', data, 54)
            section_offset = struct.unpack_from('<Q', data, 40)[0]
            section_size, section_count = struct.unpack_from('<HH', data, 58)
            assert offset+entry_size*count <= len(data) and section_offset+section_size*section_count <= len(data), f'{name}: truncated ELF tables'
            alignments = []
            for i in range(count):
                header = struct.unpack_from('<IIQQQQQQ', data, offset+i*entry_size)
                assert header[2]+header[5] <= len(data), f'{name}: truncated ELF segment'
                if header[0] == 1:  # PT_LOAD
                    assert header[7] >= 16384 and (header[2]-header[3]) % 16384 == 0, f'{name}: ELF segment lacks 16K alignment'
                    alignments.append(header[7])
            assert alignments
            elf.append({'library': name, 'load_alignments': alignments})
    for abi in abis:
        for lib in ['libreactnative.so', 'libhermesvm.so', 'librnskia.so', 'libreanimated.so', 'libworklets.so']:
            assert f'lib/{abi}/{lib}' in names, f'{abi} missing {lib}'
report = {'apk': apk.name, 'size_bytes': apk.stat().st_size,
          'sha256': hashlib.sha256(apk.read_bytes()).hexdigest(), 'abis': abis,
          'offline_hermes_bundle_bytes': len(bundle), 'dex_integrity': dex_reports, 'signature': signature,
          'alignment_16k': 'passed', 'elf_16k_segments': elf, 'badging': badging, 'permissions': permissions,
          'android_runtime': 'NOT TESTED', 'fps': 'NOT MEASURED'}
apk.with_suffix('.verification.json').write_text(json.dumps(report, ensure_ascii=False, indent=2))
apk.with_suffix('.manifest.txt').write_text(manifest)
print(json.dumps(report, ensure_ascii=False, indent=2))
