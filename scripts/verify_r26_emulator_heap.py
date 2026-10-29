#!/usr/bin/env python3
"""Verify the configured CI emulator heap in a real instrumentation process before research.

Some API29 boots start zygote with its 16 MiB fallback before qemu heap properties
arrive. Repair only that emulator configuration, once; keep the product memory guard.
The original and final observations remain in an engineering environment receipt.
"""
from pathlib import Path
import json,re,subprocess,time,sys

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

def sufficient(result):
    return type(result.get("maxMemoryBytes")) is int and result["maxMemoryBytes"]>=EXPECTED and result.get("largeHeapFlag") is True

def wait_for_framework(previous_pid):
    deadline=time.monotonic()+90
    while time.monotonic()<deadline:
        completed=adb("shell","getprop","sys.boot_completed",check=False)=="1"
        pid=adb("shell","pidof","system_server",check=False)
        services=[adb("shell","service","check",name,check=False) for name in ("package","activity","battery")]
        user=adb("shell","am","get-current-user",check=False)
        if completed and pid and pid!=previous_pid and all("found" in s and "not found" not in s for s in services) and user.isdigit():return pid
        time.sleep(1)
    raise RuntimeError("Emulator framework did not finish restarting its required services")

def main():
    receipt={"scope":"CI_EMULATOR_CONFIGURATION_ONLY; NOT_USER_PHONE_OR_RESEARCH_ACCEPTANCE",
             "minimumConfiguredHeapBytes":EXPECTED,"heapRule":"AT_LEAST_CONFIGURED_MINIMUM; ACTUAL_HEAP_REPORTED_PER_RUN","repairAttempted":False,"result":"FAIL"}
    try:
        initial=probe();receipt["initial"]=initial
        if not sufficient(initial):
            if adb("shell","getprop","ro.kernel.qemu")!="1":
                raise RuntimeError("Refusing emulator configuration repair on a physical device")
            receipt["repairAttempted"]=True
            adb("root");adb("wait-for-device")
            if adb("shell","id","-u")!="0":raise RuntimeError("Emulator root required to restore configured heap")
            previous_pid=adb("shell","pidof","system_server")
            adb("shell","setprop","dalvik.vm.heapsize","512m")
            adb("shell","setprop","dalvik.vm.heapgrowthlimit","256m")
            adb("shell","setprop","sys.boot_completed","0")
            adb("shell","stop");adb("shell","start")
            receipt["previousSystemServerPid"]=previous_pid
            receipt["restoredSystemServerPid"]=wait_for_framework(previous_pid)
        final=probe();receipt["final"]=final
        if not sufficient(final):raise RuntimeError("Actual instrumentation heap is below the configured 512 MiB minimum or lacks the product large-heap flag")
        receipt["result"]="PASS_MINIMUM_HEAP_ONLY_WITH_ACTUAL_MEASUREMENT"
    except Exception as failure:
        receipt["error"]=str(failure);raise
    finally:
        Path("r26-emulator-environment.json").write_text(json.dumps(receipt,indent=2)+"\n")
        print("R26_EMULATOR_ENVIRONMENT="+json.dumps(receipt,sort_keys=True),flush=True)

def self_test():
    import unittest
    from unittest.mock import patch
    class Regression(unittest.TestCase):
        def invoke(self,initial,final,device="1"):
            calls=[];writes=[]
            def fake_adb(*args,**kwargs):
                calls.append(args)
                if args==("shell","getprop","ro.kernel.qemu"):return device
                if args==("shell","id","-u"):return "0"
                if args==("shell","pidof","system_server"):return "123"
                return ""
            with patch(__name__+".probe",side_effect=[initial,final]),patch(__name__+".adb",side_effect=fake_adb),patch(__name__+".wait_for_framework",return_value="124"),patch.object(Path,"write_text",lambda path,value:writes.append(json.loads(value))),patch("builtins.print"):
                main()
            return calls,writes[-1]
        def test_larger_576m_heap_never_restarts_framework(self):
            measured={"maxMemoryBytes":576<<20,"largeHeapFlag":True};calls,receipt=self.invoke(measured,measured)
            self.assertEqual([],calls);self.assertFalse(receipt["repairAttempted"]);self.assertEqual(576<<20,receipt["final"]["maxMemoryBytes"])
        def test_16m_fallback_repairs_only_emulator_and_resets_boot_marker(self):
            calls,receipt=self.invoke({"maxMemoryBytes":16<<20,"largeHeapFlag":True},{"maxMemoryBytes":512<<20,"largeHeapFlag":True})
            self.assertTrue(receipt["repairAttempted"]);self.assertIn(("shell","setprop","sys.boot_completed","0"),calls);self.assertLess(calls.index(("shell","stop")),calls.index(("shell","start")))
        def test_physical_device_is_not_reconfigured(self):
            with self.assertRaisesRegex(RuntimeError,"physical device"):self.invoke({"maxMemoryBytes":16<<20,"largeHeapFlag":True},{},device="0")
        def test_framework_wait_requires_new_pid_all_services_and_completed_boot(self):
            states=iter(["0","123","Service package: found","Service activity: not found","Service battery: found","", "1","124","Service package: found","Service activity: found","Service battery: found","0"])
            with patch(__name__+".adb",side_effect=lambda *args,**kwargs:next(states)),patch(__name__+".time.sleep") as slept:
                self.assertEqual("124",wait_for_framework("123"));self.assertEqual(1,slept.call_count)
    result=unittest.TextTestRunner().run(unittest.defaultTestLoader.loadTestsFromTestCase(Regression));return 0 if result.wasSuccessful() else 1

if __name__=="__main__":sys.exit(self_test() if "--self-test" in sys.argv else main())
