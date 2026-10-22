from pathlib import Path
import sys
from apk_wiring_r964 import method
r=Path(sys.argv[1])/'com/ethk3/novaonepro'
creation=method((r/'DirectedResearchCreationR932.smali').read_text(),'create')
assert 'ResearchPopulationR966;->normalize' in creation and 'ResearchPopulationR966;->committed' in creation
assert 'ELITE_IMPROVEMENT' in (r/'ResearchFusionR964$Plan.smali').read_text()
assert 'RESOURCE_RETIRED' in (r/'ResearchLifecycleR932$State.smali').read_text()
assert 'ResearchPopulationR966;->archiveReplaced' in (r/'PairedResearchChallengeR932.smali').read_text()
assert 'DEV_WAIT_POPULATION_CAPACITY' in (r/'ResearchDynamicBootstrapR922.smali').read_text()
assert 'MIN_ENTRY_HIT_RATE:D = 0.34' in (r/'CertificateBundleServiceR929.smali').read_text()
assert 'R966_WILSON_ECE_BRIER_HIT_RATE' in (r/'UnifiedResearchChoiceR932$1.smali').read_text()
assert 'PRUNED_RESOURCE_R966' in (r/'ResearchLifecycleR932.smali').read_text()
print('R966_FINAL_DEX_WIRING=PASS|CAPACITY_REPLACEMENT|ELITE_IMPROVEMENT|RESOURCE_RETIREMENT|PARENT_ARCHIVE|TRAINING_WAIT|ENTRY_34_PERCENT|QUALITY_ORDER')
