"""Resolve a matching JDK compiler/runtime for source-level smoke checks."""
from pathlib import Path
import os
import shutil


def jdk_bin(root: Path) -> Path:
    candidates = []
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        candidates.append(Path(java_home) / "bin")
    candidates.append(root.parent / "tooling/zulu17.62.17-ca-jdk17.0.17-linux_x64/bin")
    javac = shutil.which("javac")
    if javac:
        candidates.append(Path(javac).resolve().parent)
    for directory in candidates:
        if (directory / "javac").is_file() and (directory / "java").is_file():
            return directory
    raise SystemExit(
        "A JDK is required: set JAVA_HOME, install java/javac on PATH, "
        "or restore the bundled development JDK."
    )
