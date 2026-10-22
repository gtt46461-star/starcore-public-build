import os,subprocess as s,time,json,pathlib,signal,hashlib
W=pathlib.Path('/workspace/scratch/15eb00f8c0a7');O=W/'audit_work/r80_api36_runtime';SDK=pathlib.Path(os.environ['ANDROID_HOME']);AVD=pathlib.Path(os.environ['ANDROID_AVD_HOME']);name='STARCORE_POOL_API36_5586';port='5586';os.environ['ADB_SERVER_PORT']='5086';os.environ['ANDROID_ADB_SERVER_PORT']='5086';os.environ['HOME']=str(W/'toolchain_restore/android-user');
a=AVD/(name+'.avd');a.mkdir(exist_ok=True);s.run(['cp','--sparse=always',str(AVD/'STARCORE_API36.avd/userdata-qemu.img'),str(a/'userdata-qemu.img')],check=True);cfg=(AVD/'STARCORE_API36.avd/config.ini').read_text().replace('STARCORE_API36',name).replace('6442450944','1073741824').replace('hw.cpu.ncore = 2','hw.cpu.ncore = 1');(a/'config.ini').write_text(cfg);(AVD/(name+'.ini')).write_text('avd.ini.encoding=UTF-8\npath='+str(a)+'\ntarget=android-36\n')
ADB=[str(W/'toolchain_restore/bin/adb'),'-P','5086','-s','emulator-'+port];children=[];rec={'scope':'API36_SOFTWARE_SYNTHETIC_ANDROID_TESTS','commands':[]}
def run(args,n,t=30):
 start=time.monotonic()
 try:
  with (O/(n+'.log')).open('wb') as output:r=s.run(args,stdout=output,stderr=s.STDOUT,timeout=t)
  code=r.returncode;out=(O/(n+'.log')).read_bytes()
 except s.TimeoutExpired as e:code=124;out=(O/(n+'.log')).read_bytes()
 (O/(n+'.log')).write_bytes(out);rec['commands'].append({'argv':args,'exit':code,'seconds':round(time.monotonic()-start,2),'log':n+'.log'});(O/'receipt.json').write_text(json.dumps(rec,indent=2));return code,out.decode(errors='replace')
def adb(args,n,t=30):return run(ADB+args,n,t)
try:
 proxy=(W/'toolchain_restore/block_emulator_proxy.py').read_text().replace('18080','18088').replace("toolchain_restore/EMULATOR_PROXY_STATUS.json","audit_work/r80_api36_runtime/proxy_status.json")
 (O/'proxy.py').write_text(proxy);children.append(s.Popen(['python3',str(O/'proxy.py')],stdout=(O/'proxy.log').open('w'),stderr=s.STDOUT,start_new_session=True))
 emu=[str(SDK/'emulator/emulator'),'-avd',name,'-read-only','-port',port,'-no-window','-gpu','swiftshader','-no-audio','-no-boot-anim','-accel','off','-cores','1','-memory','2560','-no-snapshot','-no-metrics','-dns-server','127.0.0.1','-http-proxy','http://127.0.0.1:18088'];rec['emulator_argv']=emu;children.append(s.Popen(emu,stdout=(O/'emulator_private.log').open('w'),stderr=s.STDOUT,start_new_session=True));start=time.monotonic()
 while time.monotonic()-start<1200:
  code,out=adb(['shell','getprop','sys.boot_completed'],'boot',15)
  print('boot',round(time.monotonic()-start),code,out.strip(),flush=True)
  if code==0 and out.strip()=='1':break
  if children[-1].poll() is not None:raise RuntimeError('emulator exited')
  time.sleep(15)
 else:raise RuntimeError('boot deadline')
 for args,n in [(['shell','svc','wifi','disable'],'wifi_disable'),(['shell','svc','data','disable'],'data_disable')]:adb(args,n,90)
 time.sleep(5)
 _,c=adb(['shell','dumpsys','connectivity'],'connectivity',60)
 for retry in range(6):
  if 'Active default network: none' in c:break
  adb(['shell','svc','wifi','disable'],'wifi_retry_'+str(retry),60);adb(['shell','svc','data','disable'],'data_retry_'+str(retry),60);time.sleep(5)
  _,c=adb(['shell','dumpsys','connectivity'],'connectivity_retry_'+str(retry),60)
 assert 'Active default network: none' in c
 _,q=adb(['shell','getprop','ro.kernel.qemu'],'qemu');assert q.strip()=='1'
 adb(['shell','ip','route','show','table','all'],'routes');(O/'READY').write_text('API36 emulator5586 isolated no active default network; reject proxy and loopback DNS\n');print('READY',flush=True)
 children.append(s.Popen(ADB+['logcat','-v','threadtime'],stdout=(O/'live_logcat_private.log').open('w'),stderr=s.STDOUT,start_new_session=True))
 idle=time.monotonic();done=set()
 while not (O/'STOP').exists():
  jobs=sorted(O.glob('command_*.json'))
  for job in jobs:
   if job.name in done:continue
   spec=json.loads(job.read_text());name=spec['name'];args=spec['args']
   if args[:3]==['shell','am','instrument']:
    _,c=adb(['shell','dumpsys','connectivity'],name+'_connectivity');assert 'Active default network: none' in c
   code,out=adb(args,name,spec.get('timeout',120));done.add(job.name);idle=time.monotonic()
   (O/(name+'.result.json')).write_text(json.dumps({'exit':code,'log':name+'.log'}));print(name,code,flush=True)
  if time.monotonic()-idle>1800:raise RuntimeError('controller idle deadline')
  time.sleep(2)

finally:
 for p in reversed(children):
  if p.poll() is None:
   os.killpg(p.pid,signal.SIGTERM)
   try:p.wait(20)
   except s.TimeoutExpired:os.killpg(p.pid,signal.SIGKILL)
 run([str(W/'toolchain_restore/bin/adb'),'-P','5086','kill-server'],'adb_stop')
