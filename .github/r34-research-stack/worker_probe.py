"""Capture only method names and counts on a disposable Android guest."""
import collections,re,subprocess,time
def sample_worker_frames(f,record,save):
    counters=collections.Counter()
    phase=collections.Counter()
    for _ in range(8):
        pid=f.adb('shell','pidof',f.PKG).decode().strip()
        if not pid.isdecimal():phase['NO_PID']+=1;continue
        try:
            p=subprocess.run(['adb','shell','debuggerd','-j',pid],capture_output=True,timeout=18)
        except subprocess.TimeoutExpired:
            phase['TRACE_TIMEOUT']+=1;continue
        found=False
        data=(p.stdout+b'\n'+p.stderr).decode(errors='replace')
        if '"main"' in data:phase['JAVA_TRACE']+=1
        for part in re.split(r'\n(?=")',data):
            header=part.partition('\n')[0]
            if not header.startswith('"starcore-r30-prepared-r27-evolution'):continue
            found=True
            methods=re.findall(r'at com\.ethk3\.novaonepro\.(?:strategyv2\.)?([A-Za-z0-9_$]+\.[A-Za-z0-9_$]+)\(',part)
            if 'CooperativeContinuationR26.awaitPermit' in methods and 'Object.wait(' in part[:1800]:phase['YIELDED']+=1
            elif methods:
                phase['SAMPLED']+=1
                counters[' / '.join(methods[:7])]+=1
        if not found:phase['OWNER_ABSENT']+=1
        time.sleep(3)
    record.setdefault('workerSamples',[]).append({'phaseCounts':dict(phase),'frames':dict(counters)})
    save()
