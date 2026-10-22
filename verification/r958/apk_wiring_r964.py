from pathlib import Path
import sys,re
root=Path(sys.argv[1])/'com/ethk3/novaonepro'
service=(root/'AutoFetchService.smali').read_text()
def method(source,name):
 for m in re.finditer(r'^\.method [^\n]*\b'+re.escape(name)+r'\([^\n]*\n.*?^\.end method',source,re.M|re.S):return m.group()
 raise AssertionError('METHOD_MISSING:'+name)
watchdog=method(service,'runAnalysisWatchdogOnce')
assert 'Landroid/os/Looper;->myLooper' in watchdog and 'Landroid/os/Looper;->getMainLooper' in watchdog and 'postCountdownWorkR964' in watchdog
post=method(service,'postCountdownWorkR964')
assert 'Landroid/os/Handler;->post' in post and 'AtomicBoolean;->compareAndSet' in post
listeners=[]
for p in root.glob('AutoFetchService$*.smali'):
 s=p.read_text()
 if '.implements Lcom/ethk3/novaonepro/CountdownClock$TickListener;' in s:
  tick=method(s,'onTick');assert 'postCountdownWorkR964' in tick
  assert all(x not in tick for x in ('DbStore;','runAnalysisWatchdogOnce','refreshNotification','ensureRuntimeLoopsAlive'))
  listeners.append(p.name)
assert len(listeners)==1,listeners
creation=(root/'DirectedResearchCreationR932.smali').read_text()
assert 'ResearchFusionR964;->choose' in creation and 'ResearchFusionR964$Plan;->create' in creation
lab=(root/'AlgorithmInventionLabR908.smali').read_text()
assert 'DevelopmentScanR964' in lab and 'DEV_FULL1200_PENDING' in lab
molecule=(root/'MolecularToolWorldR947.smali').read_text()
assert 'OBSERVED_ATOM_ADMISSION' in molecule and 'WorldModelRepositoryR952;->putAtom' in molecule
evolution_owner=(root/'r952/EvolutionControllerR952.smali').read_text()
evolution_delegate=(root/'r952/EvolutionControllerR952$1.smali').read_text()
assert 'EvolutionControllerR952$1;' in evolution_owner
# R967 moved the same mutation owner body into a transaction-reusable method. Check the
# real call chain and real proposal call, not one historical anonymous-class number.
if 'evolveInTransaction' in evolution_owner:
 assert 'EvolutionControllerR952;->evolveInTransaction' in evolution_delegate
 assert 'MutationPolicyR952;->proposeNovel' in method(evolution_owner,'evolveInTransaction')
else:
 assert 'MutationPolicyR952;->proposeNovel' in evolution_delegate
assert 'EvolutionControllerR952;->evolveOne' in method((root/'r952/ResearchDirectorR952.smali').read_text(),'pulse')
print('R964_FINAL_DEX_WIRING=PASS|UI_COUNTDOWN_ASYNC_COALESCED|WATCHDOG_MAIN_GUARD|ACTUAL_FUSION|RESUMABLE_TRAINING|ATOM_ADMISSION|NOVEL_EVOLUTION')
