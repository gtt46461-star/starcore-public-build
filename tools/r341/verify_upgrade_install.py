"""Capture and verify an authorized, data-preserving Android cover install."""
import argparse
import json
from pathlib import Path
import re
import subprocess
import sys
from verify_delivery_identity import package


def adb(serial,*args,timeout=45):
    cmd=["adb"]+(["-s",serial] if serial else [])+[str(arg) for arg in args]
    result=subprocess.run(cmd,capture_output=True,text=True,timeout=timeout,check=True)
    return result.stdout.strip()


def attached_device(serial):
    lines=adb(None,"devices","-l").splitlines()[1:]
    return any(line.split()[:2]==[serial,"device"] for line in lines)


def capture(serial,out,aapt,apksigner):
    if adb(serial,"shell","getprop","ro.kernel.qemu")=="1":
        raise ValueError("PHYSICAL_DEVICE_RECEIPT_CANNOT_USE_EMULATOR")
    dump=adb(serial,"shell","dumpsys","package",package.PACKAGE)
    width=re.search(r"\bversionCode=(\d+)\b",dump)
    uid=re.search(r"\b(?:userId|appId)=(\d+)\b",dump)
    paths=adb(serial,"shell","pm","path",package.PACKAGE).splitlines()
    base=[line[8:] for line in paths if line.startswith("package:") and line.endswith("/base.apk")]
    if not width or not uid or len(base)!=1:
        raise ValueError("BASE_APK_OR_INSTALLED_UID_UNAVAILABLE")
    out.mkdir(parents=True,exist_ok=False)
    (out/"dumpsys-package.txt").write_text(dump+"\n")
    target=out/"base.apk"
    adb(serial,"pull",base[0],target,timeout=180)
    identity,signature,badging=package.inspect(target,aapt,apksigner)
    if identity["package"]!=package.PACKAGE or identity["versionCode"]!=int(width[1]):
        raise ValueError("PULLED_APK_PACKAGE_OR_VERSION_DIFFERS_FROM_DEVICE")
    (out/"apksigner.log").write_text(signature)
    (out/"badging.log").write_text(badging)
    model=adb(serial,"shell","getprop","ro.product.model")
    return {"apk":identity,"uid":int(uid[1]),"model":model,"serial":serial,"path":base[0]}


def upgrade(serial,candidate,out,aapt,apksigner):
    baseline=capture(serial,out/"before",aapt,apksigner)
    release,_,_=package.inspect(candidate,aapt,apksigner)
    if release["package"]!=package.PACKAGE or release["versionCode"]<=baseline["apk"]["versionCode"]:
        raise ValueError("SAME_PACKAGE_STRICTLY_HIGHER_VERSION_REQUIRED")
    result=adb(serial,"install","-r",candidate,timeout=300)
    (out/"adb-install-r.log").write_text(result+"\n")
    if result.strip()!="Success":
        raise ValueError("ADB_COVER_INSTALL_UNSUCCESSFUL")
    installed=capture(serial,out/"after",aapt,apksigner)
    if installed["uid"]!=baseline["uid"] or installed["apk"]!=release:
        raise ValueError("COVER_INSTALL_IDENTITY_MISMATCH")
    return {"status":"COVER_INSTALL_BYTES_UID_VERIFIED","before":baseline,"after":installed,
            "protectedDataReadback":"PENDING_PRODUCTION_ORIGINALS"}


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--serial",required=True)
    for name in ("out","aapt","apksigner"):
        p.add_argument("--"+name,type=Path,required=True)
    p.add_argument("--install",type=Path,help="Exact originally signed APK approved for this device")
    a=p.parse_args()
    try:
        if not attached_device(a.serial):
            print(json.dumps({"status":"NOT_RUN","reason":"AUTHORIZED_PHYSICAL_DEVICE_NOT_ATTACHED"}))
            return 2
        result=upgrade(a.serial,a.install,a.out,a.aapt,a.apksigner) if a.install else capture(a.serial,a.out,a.aapt,a.apksigner)
        print(json.dumps(result,indent=2))
        return 0
    except (OSError,ValueError,subprocess.SubprocessError) as error:
        print(json.dumps({"status":"COVER_CHECK_FAILED","reason":str(error)}))
        return 1


if __name__=="__main__":
    sys.exit(main())
