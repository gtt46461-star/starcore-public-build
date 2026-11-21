import importlib.util
from pathlib import Path
p=Path('tools/r31/run_android_joint_protocols.py').resolve()
spec=importlib.util.spec_from_file_location('joint',p);joint=importlib.util.module_from_spec(spec);spec.loader.exec_module(joint)
name,count,classes=joint.SUITES[0]
assert name=='original-runtime-storage-owner' and count==10
joint.SUITES=[(name,count,[c.replace('R31OwnerFenceProtocolInstrumentedTest','R335OwnerFenceCompatibilityInstrumentedTest') for c in classes])]
if __name__=='__main__':raise SystemExit(joint.main())
