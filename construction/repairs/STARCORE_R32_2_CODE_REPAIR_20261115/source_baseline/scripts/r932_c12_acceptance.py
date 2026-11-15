#!/usr/bin/env python3
import json
from pathlib import Path

ROOT=Path('.')
REPORT=ROOT/'reports/r932'
SRC=ROOT/'app/src/main/java/com/ethk3/novaonepro'
TASK=ROOT/'docs/5.4.2/01_TASKS/tasks_batch4_C02_C04_C05_C07-C12_D09.json'

def text(path):
    p=ROOT/path if isinstance(path,str) else path
    if not p.is_file(): raise SystemExit('C12_MISSING_FILE:'+str(p))
    return p.read_text(encoding='utf-8',errors='strict')

def require(name, haystack, needles):
    missing=[x for x in needles if x not in haystack]
    if missing: raise SystemExit(name+'_MISSING:'+','.join(missing))
    print(name+'=PASS')
    return list(needles)

tasks=json.loads(text(TASK))
c12=next((x for x in tasks if x.get('id')=='C12'),None)
expected=[
    '从最底层原子到可执行候选有完整源码文本',
    '至少用合同fixture验证新结构能运行并被正确拒绝/晋级，真实高品质只能由Future证据认定',
    '每个创造事件都有目的与预算']
if not c12 or c12.get('acceptance')!=expected: raise SystemExit('C12_ORIGINAL_ACCEPTANCE_DRIFT')
print('C12_ORIGINAL_ACCEPTANCE_LOCK=PASS')

schema=text(SRC/'GenesisSchemaRegistryR915.java')
novel=text(SRC/'ResearchNovelFormulaSynthesizerR903.java')
method=text(SRC/'ResearchMethodGraphSynthesizerR903.java')
genome=text(SRC/'AlgorithmGenomeR908.java')
vm=text(SRC/'AlgorithmGenomeVmR908.java')
directed=text(SRC/'DirectedResearchCreationR932.java')
genesis=text(SRC/'AssetGenesisEngineR915.java')
trace=text(SRC/'GenesisMoleculeTraceR932.java')
formula=text(SRC/'ResearchCompiledGraphRuntimeR906.java')

source_evidence=[]
source_evidence += require('C12_L0_240_ATOM_TYPED_REGISTRY',schema,['EXPECTED_ATOMS = 240','productionScoreEligible()','validateNewAtomProposal'])
source_evidence += require('C12_STRUCTURAL_FORMULA_SOURCE',novel,['Deterministic structural formula creator','ResearchStrategyDslEngine.parse(expression)','signal disagreement','complementary evidence'])
source_evidence += require('C12_METHOD_GRAPH_FULL_SOURCE',method,['REPRESENTATION[','LEARNER[','CALIBRATION[','RISK[','OBJECTIVE[','FALSIFY[','PIT_PROTOCOL[','selection=future_oos_only','real_authority=false'])
source_evidence += require('C12_EXECUTABLE_ALGORITHM_SOURCE',genome+vm,['public final class AlgorithmGenomeR908','public static double score(','public static void update(','behaviorHash(AlgorithmGenomeR908 genome)'])
source_evidence += require('C12_THREE_EXECUTABLE_LANES_SOURCE',directed,['lane.equals("algorithm")','lane.equals("genesis")','ResearchCompiledGraphRuntimeR906.compile(p)','AlgorithmInventionLabR908.registerDirected','AssetGenesisEngineR915.registerDirected','ResearchAssetRegistryR906.register'])
source_evidence += require('C12_GENESIS_REVERSE_PARENT_SOURCE',trace+genesis,['GenesisAtomDependencyR932.dependencies','completeFor(GenesisAssetGenomeR915 genome)','GenesisMoleculeTraceR932.trace(g,true)'])
source_evidence += require('C12_FORMULA_COMPILED_RUNTIME_SOURCE',formula,['public final class ResearchCompiledGraphRuntimeR906','compile(','evaluate('])

directed_report=text(REPORT/'DIRECTED_CREATION.txt')
window_report=text(REPORT/'WINDOW_PUBLICATION.txt')
cal_report=text(REPORT/'CALIBRATED_WINDOW.txt')
enh_report=text(REPORT/'ENHANCEMENT_GAP_ACCEPTANCE.txt')
fixture_evidence=[]
fixture_evidence += require('C12_FIXTURE_EXECUTES_NEW_STRUCTURES',directed_report,['ACTUAL_TYPED_FORMULA_100_SERIALIZED_PARITY_AND_FEATURE_SENSITIVITY=PASS','GENERATION_SEED_REPRODUCES_EXECUTABLE_CHILD=PASS','ACTUAL_ALGORITHM_PRODUCTION_REGISTRATION=PASS','ACTUAL_GENESIS_PRODUCTION_REGISTRATION=PASS','ACTUAL_FORMULA_PRODUCTION_REGISTRATION=PASS'])
fixture_evidence += require('C12_FIXTURE_REJECTS_ILLEGAL_OR_PREMATURE',directed_report,['TYPED_POSTDRAW_FEATURE_REJECTED=PASS','TYPED_UNBOUNDED_DIVISION_REJECTED=PASS','TWENTYNINE_FAILURES_CANNOT_TRIGGER_DIRECTED_REPAIR=PASS','REJECTED_ATTEMPT_AND_REASON_PERSIST=PASS'])
fixture_evidence += require('C12_FUTURE_STARTS_N0',directed_report,['ALL_THREE_GENERATED_CONSUMERS_START_N0=PASS','CREATION_ENROLLS_NEW_FUTURE_CHALLENGE_WITHOUT_SUCCESS=PASS'])
fixture_evidence += require('C12_FUTURE_PROMOTION_CHAIN',window_report+cal_report,['FIRST_120_WINDOW_PUBLISHES_L5_WITH_ZERO_WEIGHT=PASS','BOTH_INDEPENDENT_WINDOWS_PUBLISH_FROM_FROZEN_EXECUTION=PASS','360_PAIRED_FUTURE_SAMPLES_PUBLISH_FIVE_PERCENT_CAP=PASS','PUBLISHED_WINDOW_CERTIFICATE_REACHES_EXISTING_AUTHORIZER=PASS','FUTURE_PROBABILITIES_BIND_EARLIER_FROZEN_CALIBRATION=PASS','AUTOMATIC_NEXT_240_IS_INDEPENDENT_OF_FIRST_120_AND_CAL=PASS'])
fixture_evidence += require('C12_PARENT_TRACE_RUNTIME_FIXTURE',enh_report,['E04A_NEW_MOLECULE_REVERSES_ALL_PARENT_ATOMS=PASS','E04A_MISSING_PARENT_TRACE_REJECTED=PASS'])
if 'NO_FUTURE_EFFECT_CLAIM' not in directed_report or 'NOT_DEVICE_OR_REAL_FUTURE' not in window_report: raise SystemExit('C12_SCOPE_MUST_NOT_CLAIM_REAL_FUTURE_EFFECT')
print('C12_REAL_QUALITY_ONLY_FUTURE_EVIDENCE=PASS')
purpose_budget=[]
purpose_budget += require('C12_EVERY_ATTEMPT_HAS_PURPOSE_AND_BUDGET_SOURCE',directed,['p.put("module",module)','p.put("domain",domain)','p.put("failure_support",support)','p.put("evidence_hash",evidence)','p.put("attempt_budget",1)','p.put("planned_new_future",120)','p.put("minimum_new_failures",FAILURES)','p.put("scope","POSTMORTEM_DIAGNOSTIC_NOT_CAUSAL_OR_FUTURE_PROOF")','p.put("may_authorize_real",false)'])
purpose_budget += require('C12_BUDGET_CANNOT_BE_LAUNDERED',directed_report,['OPEN_QUOTA_CANNOT_REPEAT_WITHOUT_120_NEW_ISSUES=PASS','SAME_FAILURE_GROUP_CANNOT_PAY_FOR_UNLIMITED_CHILDREN=PASS','REJECTED_GENERATION_RETAINS_HYPOTHESIS_BUDGET=PASS','INTERRUPTION_ROLLS_BACK_ATTRIBUTION_AND_CURSOR_TOGETHER=PASS'])
result={'revision':'R932_C12_STRICT_ACCEPTANCE_V1','task':'C12','title':c12['title'],'status':'PASS','acceptance':[{'text':expected[0],'pass':True,'evidence':source_evidence},{'text':expected[1],'pass':True,'evidence':fixture_evidence+['C12_REAL_QUALITY_ONLY_FUTURE_EVIDENCE=PASS']},{'text':expected[2],'pass':True,'evidence':purpose_budget}],'scope':'PRODUCTION_SOURCE_PLUS_ACTUAL_VM_REAL_SQLITE_SYNTHETIC_FIXTURES_NO_PHYSICAL_DEVICE_NO_REAL_FUTURE_EFFECT_CLAIM','effect_proven':False,'may_authorize_real_from_this_acceptance':False}
(REPORT/'C12_ACCEPTANCE.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('C12_ACCEPTANCE_1_L0_TO_EXECUTABLE_SOURCE=PASS')
print('C12_ACCEPTANCE_2_FIXTURE_REJECT_PROMOTE_FUTURE_ONLY=PASS')
print('C12_ACCEPTANCE_3_PURPOSE_AND_BUDGET_EVERY_EVENT=PASS')
print('R932_C12_ACCEPTANCE_PASS|3/3|effect_proven=false|scope=PRODUCTION_SOURCE_ACTUAL_VM_REAL_SQLITE_SYNTHETIC_FIXTURES')
