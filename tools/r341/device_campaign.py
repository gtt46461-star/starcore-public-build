"""Capture raw device time anchors or validate the completed sealed device receipt."""
import argparse
import json
from pathlib import Path
import sys
from verify_integrated_chain import verify_level
from verify_upgrade_install import adb, attached_device


def sample(serial,output,apk_sha256):
    if not attached_device(serial):
        return {"status":"NOT_RUN","reason":"AUTHORIZED_PHYSICAL_DEVICE_NOT_ATTACHED","exitCode":2}
    if adb(serial,"shell","getprop","ro.kernel.qemu")=="1":
        return {"status":"NOT_RUN","reason":"PHYSICAL_DEVICE_REQUIRED","exitCode":2}
    paths=adb(serial,"shell","pm","path","com.starcore.research.r13").splitlines()
    base=[value[8:] for value in paths if value.startswith("package:") and value.endswith("/base.apk")]
    if len(base)!=1 or adb(serial,"shell","sha256sum",base[0]).split()[0]!=apk_sha256:
        raise ValueError("CAMPAIGN_APK_BYTES_CHANGED")
    output.mkdir(parents=True,exist_ok=False)
    capture={
        "wallEpochSeconds":adb(serial,"shell","date","+%s"),
        "uptimeOriginal":adb(serial,"shell","cat","/proc/uptime"),
        "bootId":adb(serial,"shell","cat","/proc/sys/kernel/random/boot_id"),
        "model":adb(serial,"shell","getprop","ro.product.model"),
        "sdk":adb(serial,"shell","getprop","ro.build.version.sdk"),
        "apkSha256":apk_sha256,"serial":serial,"status":"TIME_ANCHOR_CAPTURED","exitCode":0,
    }
    raw=adb(serial,"shell","dumpsys","package","com.starcore.research.r13")
    (output/"dumpsys-package.txt").write_text(raw+"\n")
    (output/"time-anchor.json").write_text(json.dumps(capture,indent=2)+"\n")
    return capture


def main():
    p=argparse.ArgumentParser(description=__doc__)
    subs=p.add_subparsers(dest="command",required=True)
    capture=subs.add_parser("sample")
    capture.add_argument("--serial",required=True)
    capture.add_argument("--apk-sha256",required=True)
    capture.add_argument("--out",type=Path,required=True)
    finish=subs.add_parser("finish")
    for name in ("plan","repo","receipt","originals"):
        finish.add_argument("--"+name,type=Path,required=True)
    a=p.parse_args()
    try:
        result=sample(a.serial,a.out,a.apk_sha256) if a.command=="sample" else verify_level(a.plan,a.repo,a.receipt,a.originals,"DEVICE")
    except (OSError,ValueError) as error:
        result={"status":"CAMPAIGN_EVIDENCE_INCOMPLETE","reason":str(error),"exitCode":2}
    print(json.dumps(result,indent=2))
    return result["exitCode"]


if __name__=="__main__":
    sys.exit(main())
