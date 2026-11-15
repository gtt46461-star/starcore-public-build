"""Compile a candidate overlay against SDK 36 for host probes; never generates a release APK."""
import argparse
import hashlib
import re
import shutil
import subprocess
import tempfile
from pathlib import Path


PKG = "com.ethk3.novaonepro"
GENERATED = "app/build/generated/source/r967/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java"


def declare(root, package, name, body):
    path = root.joinpath(*package.split("."), name + ".java")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(f"package {package};\n{body}\n", encoding="utf-8")


def main(repo, sdk, output, expected=None):
    sdk_digest = hashlib.sha256(sdk.read_bytes()).hexdigest()
    if expected is not None and sdk_digest != expected:
        raise ValueError("SDK digest differs from the explicitly requested reference")
    if output.exists() and any(output.iterdir()):
        raise ValueError("Use an empty class output directory")
    output.mkdir(parents=True, exist_ok=True)
    java = sorted((repo / "app/src/main/java").rglob("*.java"))
    if not java:
        raise ValueError("No production Java sources")
    resources = {}
    for path in java:
        for kind, name in re.findall(r"(?<!android\.)\bR\.(\w+)\.(\w+)", path.read_text(encoding="utf-8")):
            resources.setdefault(kind, set()).add(name)
    with tempfile.TemporaryDirectory() as work:
        stubs = Path(work)
        build_fields = []
        for kind, fields, default in [
            ("int", "VERSION_CODE DB_USER_VERSION LEGACY_R906_SCHEMA_REVISION CRITICAL_DOMAIN_REVISION RESEARCH_DOMAIN_REVISION LEGACY_DOMAIN_REVISION EVENT_LEDGER_REVISION", "0"),
            ("String", "APPLICATION_ID VERSION_NAME R925_SCHEMA_REVISION DUOQU_API_KEY_A DUOQU_API_KEY_B RELEASE_MANIFEST_SHA256 SOURCE_COMMIT_R26 SOURCE_WORKTREE_SHA256_R31", '""'),
            ("boolean", "DEBUG", "false"),
        ]:
            for field in fields.split():
                build_fields.append(f"public static final {kind} {field}={default};")
        declare(stubs, PKG, "BuildConfig", "public class BuildConfig {" + "\n".join(build_fields) + "}")
        source_identity = " SOURCE_TREE_SHA256 GENERATED_AT_UTC RESOURCE_MANIFEST_SHA256 TOOLCHAIN_SHA256".split()
        declare(stubs, PKG, "BuildSourceIdentityCP12", "class BuildSourceIdentityCP12 {" +
                "\n".join('public static final String '+name+'="";' for name in source_identity) + "}")
        inner = []
        value = 1000
        for kind, names in sorted(resources.items()):
            group = []
            for name in sorted(names):
                value += 1
                group.append(f"public static final int {name}={value};")
            inner.append("public static class " + kind + "{" + "\n".join(group) + "}")
        declare(stubs, PKG, "R", "public class R {" + "\n".join(inner) + "}")
        for name in ("RequiresApi", "ChecksSdkIntAtLeast"):
            declare(stubs, "androidx.annotation", name, "public @interface " + name +
                    " {int value() default 1; int api() default 1;}")
        generated = repo / GENERATED
        if generated.exists():
            java.append(generated)
        else:
            dest = stubs / "ResearchSemanticsGeneratedR967.java"
            dest.write_bytes(subprocess.check_output(["git", "-C", str(repo), "show", "HEAD:" + GENERATED]))
        sources = java + sorted(stubs.rglob("*.java"))
        argfile = stubs / "sources.txt"
        argfile.write_text("".join('"' + str(p).replace('\\', '\\\\').replace('"','\\"') + '"\n' for p in sources))
        compiler = [shutil.which("javac")] if shutil.which("javac") else ["java", "-m", "jdk.compiler/com.sun.tools.javac.Main"]
        subprocess.run(compiler + ["-J-Xmx2g"] * int(bool(shutil.which("javac"))) +
                       ["-encoding", "UTF-8", "-source", "17", "-target", "17", "-proc:none", "-cp", str(sdk),
                        "-d", str(output), "@" + str(argfile)], check=True)
        print(f"HOST_TYPECHECK_OK production={len(list((repo / 'app/src/main/java').rglob('*.java')))} resourceIdentity=HOST_DECLARATIONS sdkSha256={sdk_digest}")


if __name__ == "__main__":
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument("--repo", type=Path, required=True)
    cli.add_argument("--android-jar", type=Path, required=True)
    cli.add_argument("--out", type=Path, required=True)
    cli.add_argument("--expected-sdk-sha256", help="Optional hash for exact reproduction of the recorded rehearsal")
    args = cli.parse_args()
    main(args.repo.resolve(), args.android_jar.resolve(), args.out.resolve(), args.expected_sdk_sha256)
