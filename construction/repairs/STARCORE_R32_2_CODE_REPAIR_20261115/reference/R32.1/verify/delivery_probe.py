"""Host-only negative cases for the packaging patch and exact-upgrade identity contracts."""
import argparse
import importlib.util
from pathlib import Path

from verify_overlay_032 import PACKAGE, CODE, APK_SHA, SIGNER, check_transition


def load_module(path):
    spec = importlib.util.spec_from_file_location("candidate_packager", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main(source):
    packager = load_module(source / "tools/r31/package_release.py")
    checked = 0
    matrix = [("PASS", "PASS", "PASS"), ("PASS", "FAIL", "FAIL"), ("PASS", "NOT_RUN", "NOT_RUN"),
              ("FAIL", "PASS", "FAIL"), ("NOT_RUN", "PASS", "NOT_RUN")]
    for contracts, baseline, expected in matrix:
        layers = packager._layers({"stages":{"contracts":{"status":contracts}, "baseline":{"status":baseline}}})
        if layers["L2_ENGINEERING_HOST_AND_UNIT"]["status"] != expected:
            raise ValueError("local engineering status mismatch: " + contracts + "/" + baseline)
        checked += 1
    installed = {"package":PACKAGE, "versionCode":CODE, "sha256":APK_SHA, "certificate":SIGNER}
    candidate = dict(installed, versionCode=CODE+1, sha256="a"*64)
    check_transition(installed, candidate)
    checked += 1
    for old, new in [(dict(installed,versionCode=577311025),candidate),
                     (dict(installed,sha256="b"*64),candidate),
                     (installed,dict(candidate,versionCode=CODE)),
                     (installed,dict(candidate,certificate="c"*64)),
                     (installed,dict(candidate,package="other.product"))]:
        try:
            check_transition(old,new)
        except ValueError:
            checked += 1
        else:
            raise ValueError("mismatched upgrade pair accepted")
    if packager.FIELD_032["apkSha256"] != installed["sha256"] or packager.FIELD_032["signerCertSha256"] != SIGNER:
        raise ValueError("packager and verified 032 reference differ")
    print("PASS", checked+1, "host packaging and version-identity cases; Android installation requires adb evidence")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, required=True)
    main(parser.parse_args().repo)
