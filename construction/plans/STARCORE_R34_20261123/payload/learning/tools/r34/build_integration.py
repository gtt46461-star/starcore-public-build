#!/usr/bin/env python3
"""Recreate exact 095 review postimages; not a cumulative branch overwrite tool."""
import pathlib,hashlib,json,difflib,argparse
BASE=pathlib.Path(__file__).resolve().parents[2]
parser=argparse.ArgumentParser();parser.add_argument('--source-root',required=True,type=pathlib.Path)
ROOT=parser.parse_args().source_root
J='app/src/main/java/com/ethk3/novaonepro/'
changes=[]
def patch(name,edits):
 path=J+name;before=(ROOT/path).read_text();after=before
 for old,new in edits:
  if after.count(old)!=1:raise ValueError((name,old[:80],after.count(old)))
  after=after.replace(old,new)
 target=BASE/'integration_postimages'/path;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(after)
 changes.append({'path':path,'before_sha256':hashlib.sha256(before.encode()).hexdigest(),'after_sha256':hashlib.sha256(after.encode()).hexdigest(),'anchors':len(edits),'status':'integration_delta_for_095_cumulative_context_required'})
 return ''.join(difflib.unified_diff(before.splitlines(True),after.splitlines(True),fromfile='a/'+path,tofile='b/'+path))
parts=[]
parts.append(patch('LifecycleSchemaR31.java',[
 ('import com.ethk3.novaonepro.strategyv2.DiagnosisSchemaR335;','import com.ethk3.novaonepro.strategyv2.DiagnosisSchemaR335;\nimport com.ethk3.novaonepro.strategyv2.ColdRebirthSchemaR34;'),
 ('if(!(DiagnosisSchemaR335.declares(name)||','if(!(ColdRebirthSchemaR34.declares(name)||DiagnosisSchemaR335.declares(name)||'),
 ('Collections.addAll(sql,DiagnosisSchemaR335.DDL);','Collections.addAll(sql,DiagnosisSchemaR335.DDL);\n        Collections.addAll(sql,ColdRebirthSchemaR34.DDL);')]))
parts.append(patch('strategyv2/RuntimeCoordinatorR26.java',[
 ('add(db,"warm-candidates","P2");','add(db,"warm-candidates","P2");add(db,ColdRebirthR34.TASK,"P2");'),
 ('case "warm-candidates":return TaskContinuationR26.step(store,c,budget,seq+1);','case "warm-candidates":return TaskContinuationR26.step(store,c,budget,seq+1);\n            case "cold-rebirth-r34":return TaskContinuationR26.step(store,c,budget,seq+1);'),
 ('else if(workPending==0&&entry.unit!=TaskProgressR30R1.Unit.TIME_TICK','else if(id.equals(ColdRebirthR34.TASK))workPending=ColdRebirthR34.pendingParentsForHealth(db);\n                        else if(workPending==0&&entry.unit!=TaskProgressR30R1.Unit.TIME_TICK'),
 ('}else sourceFailure=entry==null?"UNREGISTERED_TASK_ID":"NO_CURRENT_TYPED_PROGRESS_OR_WORK_UNIT_MISMATCH";', '}else if(entry!=null&&id.equals(ColdRebirthR34.TASK)){workPending=ColdRebirthR34.pendingParentsForHealth(db);sourceFailure=workPending==null?"COLD_REBIRTH_SCHEMA_UNAVAILABLE":null;}\n                else sourceFailure=entry==null?"UNREGISTERED_TASK_ID":"NO_CURRENT_TYPED_PROGRESS_OR_WORK_UNIT_MISMATCH";')]))
parts.append(patch('strategyv2/TaskContinuationR26.java',[
 ('if (task.equals("warm-candidates")) return WarmCandidateR26.step(store, () -> context.claim, token);','if (task.equals("warm-candidates")) return WarmCandidateR26.step(store, () -> context.claim, token);\n        if (task.equals(ColdRebirthR34.TASK)) return ColdRebirthR34.step(store, () -> context.claim, token);')]))
parts.append(patch('strategyv2/ModelReplaySU2.java',[
 ('public static OnlineModelsSU2.Model build(String kind, Map<String, String> p) {','public static OnlineModelsSU2.Model build(String kind, Map<String, String> p) {\n        // R34 lineage is hashed in the policy; strict model factories receive numerical fields only.\n        if(ColdRebirthContractR34.hasColdParameters(p))return build(kind,ColdRebirthContractR34.stripColdParameters(p));')]))
parts.append(patch('strategyv2/ResearchTaskRegistryR30R1.java',[
 ('ADDITIONAL=Collections.unmodifiableMap(m);','add(m,"cold-rebirth-r34",TaskProgressR30R1.Unit.POLICY,false,"immutable zero-evidence child births; not numeric training or quality");ADDITIONAL=Collections.unmodifiableMap(m);')]))
old_delete='DELETE FROM r26_foundry WHERE campaign_id IN (SELECT f.campaign_id FROM r26_foundry f WHERE f.status<>\'RUNNING\' AND NOT EXISTS(SELECT 1 FROM r26_candidate c WHERE c.campaign_id=f.campaign_id) AND f.campaign_id NOT IN (SELECT campaign_id FROM r26_foundry ORDER BY created_at DESC LIMIT 2) ORDER BY f.created_at,f.campaign_id LIMIT 2)'
parts.append(patch('strategyv2/FoundryCampaignR26.java',[
 ('private static void pruneFinishedR31(DbStore store,RuntimeCoordinatorR26.Claim claim){','private static void pruneFinishedR31(DbStore store,RuntimeCoordinatorR26.Claim claim){\n        ColdRebirthSchemaR34.require(store.readable());'),
 ('String old="campaign_id IN (SELECT campaign_id FROM r26_foundry WHERE status<>\'RUNNING\' ORDER BY created_at DESC LIMIT -1 OFFSET 2)";','String old="campaign_id IN (SELECT campaign_id FROM r26_foundry WHERE status<>\'RUNNING\' ORDER BY created_at DESC LIMIT -1 OFFSET 2) AND NOT "+ColdRebirthContractR34.protectZeroCandidateSql("r26_candidate");'),
 ('db.execSQL("'+old_delete+'");','db.execSQL(ColdRebirthContractR34.pruneSourceSql());')]))
parts.append(patch('strategyv2/WarmCandidateR26.java',[
 ('long version=scalar(read,"PRAGMA schema_version");\n            String learned=', 'long version=scalar(read,"PRAGMA schema_version");\n            ColdRebirthSchemaR34.require(read);\n            String learned='),
 ('String action="CASE WHEN c.state IS NULL','String action="CASE WHEN "+ColdRebirthContractR34.protectZeroCandidateSql("c")+" THEN 0 WHEN c.state IS NULL'),
 ('if(action==3)db.execSQL("DELETE FROM r26_candidate"+conditions+" AND (state IS NULL OR length(state)=0 OR cursor=0 AND instr(canonical_text,\'_warm_kernel=\')=0 OR EXISTS(SELECT 1 FROM r26_live_model l WHERE l.policy_hash=r26_candidate.policy_hash))",args);','if(action==3)db.execSQL("DELETE FROM r26_candidate"+conditions+" AND (state IS NULL OR length(state)=0 OR cursor=0 AND instr(canonical_text,\'_warm_kernel=\')=0 OR EXISTS(SELECT 1 FROM r26_live_model l WHERE l.policy_hash=r26_candidate.policy_hash)) AND NOT "+ColdRebirthContractR34.protectZeroCandidateSql("r26_candidate"),args);')]))
parts.append(patch('strategyv2/RetentionR26.java',[
 ('ExpertContinuationR26.preserveExpired(db);','ColdRebirthSchemaR34.require(db);\n        ExpertContinuationR26.preserveExpired(db);'),
 ('db.execSQL("DELETE FROM r26_expert_experiment WHERE rowid IN (SELECT rowid FROM r26_expert_experiment WHERE campaign_id NOT IN (SELECT campaign_id FROM r26_foundry) AND (length(state)=0 OR cursor=0 AND checkpoint_hash=\'\') LIMIT "+SLICE_R30+")");','db.execSQL(ColdRebirthContractR34.pruneExpiredExpertsSql(SLICE_R30));')]))
(BASE/'patches/B06_cold_rebirth_integration.patch').write_text(''.join(parts))
for file in sorted((BASE/J/'strategyv2').glob('ColdRebirth*R34.java')):
 changes.append({'path':str(file.relative_to(BASE)),'sha256':hashlib.sha256(file.read_bytes()).hexdigest(),'status':'new_complete_production_source'})
(BASE/'B06_FILE_MANIFEST.json').write_text(json.dumps({'source_commit':'273641e838e0b8dd842b42a67a674e184942a30a','objects':changes},ensure_ascii=False,indent=2)+'\n')
print(json.dumps({'postimages':len(parts),'new_classes':3}))
