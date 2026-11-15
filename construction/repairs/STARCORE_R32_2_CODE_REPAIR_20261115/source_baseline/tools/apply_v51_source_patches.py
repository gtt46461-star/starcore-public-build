#!/usr/bin/env python3
"""Generate direct Draw adapter and splice typed V51 entrypoints.

The generated Java contains no reflection. Every discovery decision is written to
reports/V51_PATCH_DISCOVERY.json so CI failures are actionable.
"""
from __future__ import annotations
import json,re,traceback
from pathlib import Path
import javalang

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/com/ethk3/novaonepro'
REPORTS=ROOT/'reports'
DISCOVERY={}


def nested_class(tree,name):
    for _,node in tree.filter(javalang.tree.ClassDeclaration):
        if node.name==name:return node
    raise RuntimeError('CLASS_NOT_FOUND_'+name)


def fields_of(node):
    out={}
    for field in node.fields:
        t=getattr(field.type,'name','')
        type_dims=len(getattr(field.type,'dimensions',[]) or [])
        for d in field.declarators:
            dims=type_dims+len(getattr(d,'dimensions',[]) or [])
            out[d.name]=(t,dims)
    return out


def methods_of(node):
    return {m.name:m for m in node.methods}


def generate_draw_adapter():
    source=(JAVA/'Models.java').read_text('utf-8')
    tree=javalang.parse.parse(source);draw=nested_class(tree,'Draw');fields=fields_of(draw);methods=methods_of(draw)
    DISCOVERY['draw_fields']={k:list(v) for k,v in sorted(fields.items())}
    DISCOVERY['draw_methods']=sorted(methods)
    if 'issue' not in fields:raise RuntimeError('DRAW_ISSUE_FIELD_MISSING')
    time='draw.openTime' if 'openTime' in fields else ('draw.fetchTime' if 'fetchTime' in fields else '0L')
    aliases=('numbers','openNumbers','openNumber','nums','values','resultNumbers','numberList','codes','codeArray','result')
    array_name=next((x for x in aliases if fields.get(x)==('int',1)),None)
    if array_name:
        permutation='V51Math.copy(draw.%s)'%array_name;mode='int_array:'+array_name
    elif all(('n%d'%i) in fields for i in range(1,11)):
        permutation='new int[]{'+','.join('draw.n%d'%i for i in range(1,11))+'}';mode='n1_n10'
    elif all(('p%d'%i) in fields for i in range(1,11)):
        permutation='new int[]{'+','.join('draw.p%d'%i for i in range(1,11))+'}';mode='p1_p10'
    else:
        getter=next((x for x in ('getNumbers','numbers','getOpenNumbers','toArray') if x in methods and len(methods[x].parameters)==0),None)
        indexed=next((x for x in ('numberAt','getNumber','getNumberAt','at') if x in methods and len(methods[x].parameters)==1),None)
        if getter:
            permutation='V51Math.copy(draw.%s())'%getter;mode='array_getter:'+getter
        elif indexed:
            permutation='new int[]{'+','.join('draw.%s(%d)'%(indexed,i) for i in range(1,11))+'}';mode='indexed_getter:'+indexed
        else:
            raise RuntimeError('DRAW_PERMUTATION_ACCESSOR_NOT_FOUND:'+','.join(sorted(fields)))
    DISCOVERY['draw_adapter_mode']=mode
    text='''package com.ethk3.novaonepro;\n\n/** Generated from Models.Draw by tools/apply_v51_source_patches.py. */\npublic final class V51DrawAdapter {\n    private V51DrawAdapter() { }\n    public static V51Models.DrawFrame convert(Models.Draw draw) {\n        if (draw == null) return null;\n        int[] permutation = %s;\n        if (!V51Math.validPermutation(permutation)) return null;\n        return new V51Models.DrawFrame(draw.issue, permutation, %s);\n    }\n}\n'''%(permutation,time)
    (JAVA/'V51DrawAdapter.java').write_text(text,encoding='utf-8')


def inject_before_last_brace(path:Path,marker:str,method:str):
    text=path.read_text('utf-8')
    if marker in text:return
    pos=text.rfind('}')
    if pos<0:raise RuntimeError('NO_CLASS_CLOSE_'+path.name)
    text=text[:pos]+'\n    '+marker+'\n'+method+'\n'+text[pos:]
    path.write_text(text,encoding='utf-8')


def patch_gate():
    gate=JAVA/'DecisionGateEngine.java'
    inject_before_last_brace(gate,'// V51_APEX_TYPED_ENTRY', '''    public V51Models.AuthorizedDecision decideV51(DbStore store, V51CompiledActionContract compiled) {
        return V51DecisionGateCore.decide(store, compiled);
    }''')
    inject_before_last_brace(gate,'// V51_APEX_LEGACY_VIEW_ADAPTER', '''    /** Compatibility view only; no V51 probability, rank or lane is recalculated. */
    public Models.Decision toLegacyDecisionV51(V51RuntimeResult runtime) {
        Models.Decision out = new Models.Decision();
        if (runtime == null || runtime.authorized == null || runtime.authorized.selected == null
                || runtime.authorized.selected.action == null || runtime.authorized.selected.context == null) {
            out.decision = AppConfig.DECISION_HOLD; out.action = AppConfig.ACTION_HOLD;
            out.blockCode = "V51_RUNTIME_RESULT_INCOMPLETE"; out.finalDecisionRejectCode = out.blockCode;
            out.reasonSummary = "V51 typed runtime result incomplete"; out.reasonText = out.reasonSummary; return out;
        }
        V51Models.AuthorizedDecision authorized = runtime.authorized;
        V51Models.SelectedActionEnvelope envelope = authorized.selected;
        V51Models.PositionAction selected = envelope.action;
        out.baseIssue=envelope.context.baseIssue;out.targetIssue=envelope.context.targetIssue;out.createdAt=envelope.context.createdAt;
        out.decision=authorized.action.name();out.action=authorized.action.name();out.position=selected.position;
        out.n1=selected.n1;out.n2=selected.n2;out.n3=selected.n3;out.k3Mask=AppConfig.buildK3Mask(out.n1,out.n2,out.n3);
        out.rawP=selected.rawActionP;out.modelP=selected.rankScore;out.rawRankScore=selected.rankScore;
        out.calibratedP=envelope.selectedHitP;out.finalP=envelope.selectedHitP;out.honestProbability=envelope.selectedHitP;out.calibratedHitProbability=envelope.selectedHitP;
        out.dynamicGate=0.0d;out.edge=envelope.selectedHitP;out.v46EventProbability=envelope.selectedHitP;
        out.v46ActionLcb=Math.max(0.0d,envelope.selectedHitP-envelope.uncertainty*0.08d);out.v46ProbabilityUncertainty=envelope.uncertainty;
        out.v46ExpectedValue=AppConfig.ODDS*envelope.selectedHitP-AppConfig.BET_COST;out.v46FinalRank=selected.rank;out.v46FormalCandidateCount=10;
        out.v46FinalContractHash=envelope.contractHash;out.v46ContractHash=envelope.contractHash;out.v46Lane=envelope.lane.name();
        out.laneCandidate=envelope.lane.name();out.betDomain=envelope.lane.name();out.finalDecisionBetDomain=envelope.lane.name();
        out.stateKey=envelope.domain==null?"":envelope.domain.domainId;
        out.trendLife=runtime.states!=null&&selected.position<=runtime.states.length&&runtime.states[selected.position-1]!=null?runtime.states[selected.position-1].runLength:0.0d;
        out.driftRisk=runtime.states!=null&&selected.position<=runtime.states.length&&runtime.states[selected.position-1]!=null?runtime.states[selected.position-1].changePointRisk:1.0d;
        out.buildId=V51BuildManifest.BUILD_ID;out.cohortId=V51BuildManifest.COHORT_ID;out.schemaVersion=V51BuildManifest.DATABASE_SCHEMA;
        out.featureSchemaVersion=V51BuildManifest.FEATURE_SCHEMA;out.rankingVersion=V51ActionRankerArtifact.ARTIFACT_ID;
        out.calibrationVersion=V51SelectedCalibrationArtifact.ARTIFACT_ID;out.gatePolicyVersion=V51PolicySurfaceArtifact.ARTIFACT_ID;
        out.pipelineVersion=V51BuildManifest.VERSION_NAME;out.v43StrategyVersion=V51BuildManifest.VERSION_NAME;out.contractDecisionId=authorized.decisionId;
        out.candidateHash=runtime.contract==null?"":runtime.contract.checksum;out.snapshotMaxIssue=envelope.context.historyMaxIssue;
        out.trainedUntilIssue=V51NumberModelArtifact.TRAINING_PERIOD_END;out.contractValid=1;out.contractStatus="V51_TYPED_CONTRACT_VALID";
        out.authorizationId=authorized.authorizationHash;out.authorizationValid=authorized.action==V51Enums.DecisionAction.BET?1:0;
        out.authorizedMode=authorized.action.name();out.authorizationRejectCode=authorized.rejectCode.name();out.authorizedAt=envelope.context.createdAt;
        out.authorizationProbability=envelope.selectedHitP;out.authorizationGate=0.0d;out.authorizationEdge=envelope.selectedHitP;out.dataOk=runtime.success?1:0;
        out.settled=0;out.result=AppConfig.RESULT_WAIT;out.probabilityMode="V51_SELECTED_HIT_P";out.gateMode="V51_POLICY_SURFACE_ARTIFACT";
        out.blockCode=authorized.rejectCode==V51Enums.RejectCode.NONE?"":authorized.rejectCode.name();out.finalDecisionRejectCode=out.blockCode;
        out.reasonSummary="V51排列约束Rank1｜lane="+envelope.lane.name()+"｜predictability="+(envelope.predictability==null?0.0d:envelope.predictability.probability)+"｜falsifier="+envelope.falsifierScore;
        out.reasonText=out.reasonSummary;out.preDrawReasonFingerprint=envelope.contractHash;out.preDrawReasonText=out.reasonSummary;
        out.learningSummary="V51 1/10/100/permutation full-information ledger";
        out.selfDiagnosisText="release="+V51BuildManifest.releaseState()+"|rankArtifact="+V51ActionRankerArtifact.ARTIFACT_ID+"|calibrationArtifact="+V51SelectedCalibrationArtifact.ARTIFACT_ID+"|policyArtifact="+V51PolicySurfaceArtifact.ARTIFACT_ID;
        out.statScope=authorized.action==V51Enums.DecisionAction.BET?AppConfig.STAT_SCOPE_REAL_TRADE:(authorized.action==V51Enums.DecisionAction.SHADOW?AppConfig.STAT_SCOPE_SHADOW:AppConfig.STAT_SCOPE_HOLD);
        out.realtimeCostMs=runtime.elapsedMs;return out;
    }''')
    inject_before_last_brace(JAVA/'DecisionAuthorizationEngine.java','// V51_APEX_AUTH_ENTRY', '''    public V51Models.AuthorizedDecision authorizeV51(V51CompiledActionContract compiled, V51Enums.DecisionAction action, V51Enums.RejectCode reject) {
        return V51AuthorizationCore.authorize(compiled, action, reject);
    }

    public static boolean verifyV51(V51Models.AuthorizedDecision decision, V51CompiledActionContract compiled) {
        return V51AuthorizationCore.verify(decision, compiled);
    }''')


def patch_db():
    path=JAVA/'DbHelper.java';text=path.read_text('utf-8')
    needle='        requireV50Migration(db);';replacement=needle+'\n        V51Repository.ensure(db);'
    if 'V51Repository.ensure(db);' not in text:
        count=text.count(needle);DISCOVERY['db_migration_needle_count']=count
        if count==0:raise RuntimeError('V50_MIGRATION_NEEDLE_NOT_FOUND')
        text=text.replace(needle,replacement)
    path.write_text(text,encoding='utf-8')


def normalize_args(raw):
    return ' '.join(raw.replace('\n',' ').replace('\r',' ').split())


def patch_autofetch():
    path=JAVA/'AutoFetchService.java';text=path.read_text('utf-8')
    route='''        if (V51BuildManifest.ACTIVE_RUNTIME) {
            return buildV51ApexDecision(clean, baseIssue, targetIssue, totalStart);
        }
'''
    v50_route='        if (AppConfig.ENABLE_V50_PRIME) {'
    if route not in text:
        if v50_route not in text:raise RuntimeError('V50_ROUTE_NEEDLE_NOT_FOUND')
        text=text.replace(v50_route,route+v50_route,1)
    text=text.replace('        // V51_APEX_SIDECAR_CALL\n        V51AutoFetchBridge.runShadow(store, clean, analysisGeneration);\n','')
    text=text.replace('        V51AutoFetchBridge.runShadow(store, clean, analysisGeneration);\n','')
    if 'private Models.Decision buildV51ApexDecision' not in text:
        marker='    private Models.Decision buildV50PrimeDecision('
        at=text.find(marker)
        if at<0:raise RuntimeError('V50_METHOD_NEEDLE_NOT_FOUND')
        method='''    /**
     * Sole active V51 route. It never invokes V50/V49/V47 prediction, rescue,
     * frequency debt or post-gate mutation. Unproven artifacts produce SHADOW.
     */
    private Models.Decision buildV51ApexDecision(
            ArrayList<Models.Draw> clean, String baseIssue,
            String targetIssue, long totalStart) {
        setAnalysisState("RUNNING", 24,
                "V51 APEX：PIT排列特征→状态→专家Gate→Sinkhorn→10动作→Rank→校准→可预测性");
        long started = System.currentTimeMillis();
        V51RuntimeResult runtime = V51AutoFetchBridge.run(
                store, clean, analysisGeneration);
        recordAnalysisCost("v51_typed_runtime", started);
        Models.Decision decision = decisionGateEngine.toLegacyDecisionV51(runtime);
        if (decision.baseIssue == null || decision.baseIssue.length() == 0) decision.baseIssue = baseIssue;
        if (decision.targetIssue == null || decision.targetIssue.length() == 0) decision.targetIssue = targetIssue;
        if (runtime == null || !runtime.success) {
            decision.decision = AppConfig.DECISION_HOLD; decision.action = AppConfig.ACTION_HOLD;
            decision.statScope = AppConfig.STAT_SCOPE_HOLD; decision.isRealtimeReal = 0; decision.authorizationValid = 0;
            decision.blockCode = runtime == null ? "V51_RUNTIME_NULL" : runtime.status;
            decision.finalDecisionRejectCode = decision.blockCode;
            decision.reasonSummary = "V51保护性HOLD：" + decision.blockCode; decision.reasonText = decision.reasonSummary;
        }
        try {
            store.setState("active_runtime", "V51_ACTIVE_RUNTIME");
            store.setState("legacy_runtime", "LEGACY_READ_ONLY_OBSERVER");
            store.setState("v51_runtime_status", runtime == null ? "NULL" : runtime.status);
            store.setState("v51_runtime_checksum", runtime == null ? "" : runtime.checksum);
            store.setState("v51_runtime_cost_ms", runtime == null ? "0" : String.valueOf(runtime.elapsedMs));
            store.setState("v51_release_state", V51BuildManifest.releaseState());
            store.setState("v51_model_gate", V51BuildManifest.MODEL_PROVEN ? "PASSED" : "NOT_PROVEN");
        } catch (Throwable ignore) { }
        recordAnalysisCost("build_decision_total_v51", totalStart);
        return decision;
    }

'''
        text=text[:at]+method+text[at:]
    path.write_text(text,encoding='utf-8')
    DISCOVERY['autofetch_active_runtime']='V51' if 'V51BuildManifest.ACTIVE_RUNTIME' in text else 'MISSING'
    DISCOVERY['autofetch_v51_route_count']=text.count('buildV51ApexDecision(')
    DISCOVERY['autofetch_v51_bridge_count']=text.count('V51AutoFetchBridge.run(')
    if DISCOVERY['autofetch_active_runtime']!='V51':raise RuntimeError('V51_ACTIVE_RUNTIME_ROUTE_MISSING')
    if DISCOVERY['autofetch_v51_route_count']<2:raise RuntimeError('V51_ROUTE_METHOD_OR_CALL_MISSING')
    if DISCOVERY['autofetch_v51_bridge_count']!=1:raise RuntimeError('V51_ACTIVE_BRIDGE_COUNT_'+str(DISCOVERY['autofetch_v51_bridge_count']))
    active_start=text.index('private Models.Decision buildV51ApexDecision')
    active_end=text.find('private Models.Decision buildV50PrimeDecision',active_start)
    if active_end<0:active_end=len(text)
    active=text[active_start:active_end]
    forbidden=('V50RuntimeOrchestrator','buildV50PrimeDecision(','FREQUENCY_FLOOR','needSupply')
    bad=[token for token in forbidden if token in active];DISCOVERY['autofetch_v51_forbidden']=bad
    if bad:raise RuntimeError('V51_ACTIVE_ROUTE_LEGACY_REFERENCE:'+','.join(bad))


def main():
    REPORTS.mkdir(parents=True,exist_ok=True)
    steps=(('draw_adapter',generate_draw_adapter),('gate',patch_gate),('db',patch_db),('autofetch',patch_autofetch))
    try:
        for name,fn in steps:
            fn();DISCOVERY[name]='OK';print('V51_PATCH_STEP_OK',name)
        DISCOVERY['status']='SUCCESS'
    except Exception as exc:
        DISCOVERY['status']='FAILURE';DISCOVERY['error']=repr(exc);DISCOVERY['traceback']=traceback.format_exc()
        (REPORTS/'V51_PATCH_DISCOVERY.json').write_text(json.dumps(DISCOVERY,ensure_ascii=False,indent=2),encoding='utf-8')
        print('V51_PATCH_FAILURE',repr(exc));raise
    (REPORTS/'V51_PATCH_DISCOVERY.json').write_text(json.dumps(DISCOVERY,ensure_ascii=False,indent=2),encoding='utf-8')
    print('V51 source patches generated and applied')

if __name__=='__main__':main()
