from pathlib import Path
import sys
from apk_wiring_r964 import method
root=Path(sys.argv[1])/'com/ethk3/novaonepro'
for receiver in ('InternalWatchdogReceiverR815','BootReceiver'):
    body=method((root/(receiver+'.smali')).read_text(),'onReceive')
    assert 'goAsync' in body and 'RecoveryDispatchR965;->submit' in body
    assert 'ServiceHeartbeatStore;' not in body and 'RecoveryCoordinatorR815;->request' not in body
recovery=(root/'RecoveryDispatchR965.smali').read_text()
assert 'ArrayBlockingQueue' in recovery and 'ScheduledThreadPoolExecutor' in recovery
creation=method((root/'DirectedResearchCreationR932.smali').read_text(),'create')
assert 'ResearchFailureEvolutionR965;->choose' in creation and 'ResearchFailureEvolutionR965$Plan;->create' in creation
lab='\n'.join(p.read_text() for p in root.glob('AlgorithmInventionLabR908*.smali'))
assert 'ResearchPruningR965;->register' in lab and 'ResearchPruningR965;->observe' in lab
assert 'r965_evolution_mask_' in (root/'ResearchEvolutionHeartbeatR947.smali').read_text()
assert 'r965_ablation_source' in (root/'ModuleAblationR932.smali').read_text()
assert 'ResearchFailureEvolutionR965;->report' in (root/'r952/FullbloodDiagnosticsProviderR952.smali').read_text()
print('R965_FINAL_DEX_WIRING=PASS|RECEIVERS_ASYNC_BOUNDED|FAILURE_DIRECTED_PROGRAM|REAL_SETTLEMENT_PRUNING|INDEPENDENT_EVOLUTION_STAGES|SOURCE_BOUND_ABLATION_QUEUE|TRUTHFUL_DIAGNOSTICS')
