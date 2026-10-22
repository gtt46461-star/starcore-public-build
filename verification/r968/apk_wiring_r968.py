"""Check the final signed DEX uses this checkout's generated semantics and formal-plan guards."""
from pathlib import Path
import json,re,sys
root=Path(__file__).resolve().parents[2]
smali=Path(sys.argv[1])/'com/ethk3/novaonepro';checks=0
manifest=json.loads((root/'app/build/generated/r967/semantic_manifest.json').read_text())
def text(name):return (smali/(name+'.smali')).read_text()
def method(name,member):
    match=re.search(r'^\.method [^\n]*\b'+re.escape(member)+r'\([^\n]*\n.*?^\.end method',text(name),re.M|re.S)
    assert match,'METHOD_MISSING:'+name+'.'+member
    return match.group()
def check(value,name):
    global checks
    assert value,name
    checks+=1;print('PASS '+name)
for name in ['ResearchSemanticsR967','ResearchSemanticsR967$Builder','ResearchSemanticsGeneratedR967','ResearchGatePlanR967','ResearchGatePlanR967$Plan','ResearchGatePlanR967$Policy']:
    check((smali/(name+'.smali')).is_file(),'GENERATED_OR_PLAN_CLASS:'+name)
check(manifest['training_rule_sha256'] in text('ResearchSemanticsGeneratedR967'),'GENERATED_TRAINING_HASH_MATCHES_FINAL_DEX')
check(manifest['training_rule_sha256'] in text('TrainingCodeIdentityR964'),'LEGACY_DEV_OWNER_WRAPPER_HAS_GENERATED_RULE')
bootstrap='\n'.join(f.read_text() for f in smali.glob('ResearchDynamicBootstrapR922*.smali'))
check(manifest['training_rule_sha256'] in bootstrap,'ACTUAL_DEV_RESUME_PATH_USES_CURRENT_RULE')
check('ResearchGatePlanR967;->registerFormalExperiment(' in method('FixedResearchWindowR932','persistWindow'),'ACTUAL_WINDOW_REGISTRATION_CALLS_PLAN_OWNER')
check('ResearchGatePlanR967;->beforeEvaluation(' in method('FixedWindowStatisticsR932','evaluate'),'ACTUAL_STATISTICS_REVALIDATE_PLAN')
pub='\n'.join(f.read_text() for f in smali.glob('FixedWindowCertificatePublisherR932*.smali'))
check('ResearchGatePlanR967;->forWindow(' in pub and 'IMMUTABLE_EXPERIMENT_PLAN_MISMATCH' in pub,'ACTUAL_PUBLISHER_RECHECKS_PLAN_HASH')
check('ResearchMultiplicityR932;->registerLook(' in method('ResearchGatePlanR967','registerFormalExperiment'),'PLAN_USES_EXISTING_PAID_ALPHA_OWNER')
check('ResearchSemanticsR967$Builder;->digest(' in method('ResearchGatePlanR967','hash'),'TYPED_IDENTITY_ACTUALLY_HASHES_REGISTERED_PLAN')
check('R967_FORMAL_EARLIER_CALIBRATION_REQUIRED' in text('ResearchGatePlanR967'),'FORMAL_GATE_REQUIRES_PRIOR_CALIBRATION')
finite_source=text('FixedResearchWindowR932')
finite_wired='Ljava/lang/Double;->isFinite(D)Z' in finite_source
for owner,member in re.findall(r'invoke-static[^\n]*, L(com/ethk3/novaonepro/[^;]+);->([^\s(]+)\(D\)Z',finite_source):
    target=Path(sys.argv[1])/(owner+'.smali')
    if not target.is_file():continue
    backport=re.search(r'^\.method [^\n]* '+re.escape(member)+r'\(D\)Z\n.*?^\.end method',target.read_text(),re.M|re.S)
    if backport:
        b=backport.group()
        finite_wired|=('Ljava/lang/Double;->isInfinite(D)Z' in b and 'Ljava/lang/Double;->isNaN(D)Z' in b
                       and b.count('if-nez')==2 and 'const/4 p0, 0x1' in b and 'const/4 p0, 0x0' in b)
check(finite_wired,'UNKNOWN_SCORE_GUARD_USES_DIRECT_OR_VERIFIED_D8_ISFINITE_BODY')
check('R968_EVALUATION_DENOMINATOR_MISMATCH' in method('FixedWindowStatisticsR932','evaluate'),'STATISTICS_REJECT_CALLER_DENOMINATOR_DRIFT')
check('r967_plan_no_replace' in method('ResearchGatePlanR967','schema'),'REPLACE_BYPASS_GUARD_INCLUDED')
check('formal_plan_revision' in method('ResearchGatePlanR967','forWindow'),'LOST_PLAN_CANNOT_DOWNGRADE_NEW_WINDOW_TO_LEGACY')
check('POLICY_FIELDS' in text('ResearchGatePlanR967') and 'POLICY_TYPE' in text('ResearchGatePlanR967$Policy'),'STRICT_POLICY_INTERPRETER_IN_DEX')
check('research_experiment_plan_r967' in method('SchemaDomainCoordinatorR926','researchContract'),'REAL_SCHEMA_OWNER_REQUIRES_PLAN_TABLE')
check('immutable_formal_plans' in text('ResearchFunnelR967'),'PLAN_COUNT_IN_VISIBLE_READ_ONLY_FUNNEL')
print('R968_FINAL_DEX_WIRING=PASS|checks='+str(checks)+'|ANDROID_EXECUTION=NOT_RUN|FULL_91_PLAN=NOT_COMPLETE')
