#!/usr/bin/env python3
"""Verify the configured CI emulator heap in a real instrumentation process before research.

Some API29 boots start zygote with its 16 MiB fallback before qemu heap properties
arrive. Repair only that emulator configuration, once; keep the product memory guard.
The original and final observations remain in an engineering environment receipt.
"""
from pathlib import Path
import json,re,subprocess,time

EXPECTED=512<<20
TARGET="com.starcore.research.r13"
RUNNER=TARGET+".test/com.ethk3.novaonepro.strategyv2.Su1AndroidTestRunner"

def adb(*args,check=True,timeout=30):
    return subprocess.run(["adb",*args],text=True,capture_output=True,check=check,timeout=timeout).stdout.strip()

def probe():
    adb("shell","am","force-stop",TARGET)
    text=adb("shell","am","instrument","-w","-e","r26HeapProbeOnly","true",RUNNER,timeout=60)
    found=re.search(r"^INSTRUMENTATION_RESULT: r26HeapEnvironment=(\{[^\n]+\})",text,re.M)
    if not found:raise RuntimeError("No source-bound runner heap probe receipt: "+text[-1000:])
    result=json.loads(found.group(1))
    result["dalvikHeapProperty"]=adb("shell","getprop","dalvik.vm.heapsize")
    result["qemuHeapProperty"]=adb("shell","getprop","qemu.dalvik.vm.heapsize")
    return result

def main():
    receipt={"scope":"CI_EMULATOR_CONFIGURATION_ONLY; NOT_USER_PHONE_OR_RESEARCH_ACCEPTANCE",
             "expectedHeapBytes":EXPECTED,"repairAttempted":False,"result":"FAIL"}
    try:
        initial=probe();receipt["initial"]=initial
        if initial["maxMemoryBytes"]!=EXPECTED:
            if adb("shell","getprop","ro.kernel.qemu")!="1":
                raise RuntimeError("Refusing emulator configuration repair on a physical device")
            receipt["repairAttempted"]=True
            adb("root");adb("wait-for-device")
            if adb("shell","id","-u")!="0":raise RuntimeError("Emulator root required to restore configured heap")
            adb("shell","setprop","dalvik.vm.heapsize","512m")
            adb("shell","setprop","dalvik.vm.heapgrowthlimit","256m")
            adb("shell","stop");adb("shell","start")
            deadline=time.monotonic()+90
            while time.monotonic()<deadline:
                ready=adb("shell","service","check","package",check=False)
                if "found" in ready and "not found" not in ready:
                    time.sleep(2);break
                time.sleep(1)
            else:raise RuntimeError("Emulator framework did not restart")
        final=probe();receipt["final"]=final
        if final["maxMemoryBytes"]!=EXPECTED or not final["largeHeapFlag"]:
            raise RuntimeError("Actual instrumentation heap does not match the configured 512 MiB environment")
        receipt["result"]="PASS_CONFIGURED_HEAP_ONLY"
    except Exception as failure:
        receipt["error"]=str(failure);raise
    finally:
        Path("r26-emulator-environment.json").write_text(json.dumps(receipt,indent=2)+"\n")
        print("R26_EMULATOR_ENVIRONMENT="+json.dumps(receipt,sort_keys=True),flush=True)

if __name__=="__main__":main()
