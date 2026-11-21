import os,re
from pathlib import Path
text=(Path(os.environ["RUNNER_TEMP"])/"report-originals"/"instrument-build.log").read_text(errors="replace")
# Emit finite Gradle reason codes and compiler source locations. Original signing files stay private.
print("GRADLE_RESULT",re.findall(r"(?:Task |Execution failed for task ')(:[a-zA-Z0-9_:]+)(?: FAILED|')",text)[-8:])
for token in ["Could not resolve","Could not download","Could not GET","Could not HEAD","429","502","503","PKIX","UnknownHostException","timeout","timed out","No space left","Permission denied","cannot find symbol","error:","R997_UNCOMMITTED_BUILD_INPUTS_RELEASE_BLOCKED","AAPT2","Gradle Wrapper","distributionUrl","zip END header not found","Could not install Gradle distribution","Could not find","corrupt","Unsupported class","OutOfMemory"]:
    if token.lower() in text.lower():print("GRADLE_REASON",token)
for path,line in re.findall(r"/([A-Za-z_0-9]+\.java):(\d+): error:",text):print("COMPILER_SOURCE",path,line)
print("PROJECT_GATES",sorted(set(re.findall(r"\bR[0-9]{2,4}_[A-Z0-9_]{3,96}\b",text)))[:16])
