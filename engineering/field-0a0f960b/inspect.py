from collections import Counter
from contextlib import ExitStack
from pathlib import Path
import hashlib
import io
import json
import re
import sys
import zipfile


def emit(label, obj):
    print(label, json.dumps(obj, ensure_ascii=False, sort_keys=True), flush=True)


def selected(obj, keys):
    return {key: obj.get(key) for key in keys if key in obj}


def status_table(row):
    counts = {}
    for state in row.get("states", []):
        value = str(state.get("value"))
        col = state.get("column")
        if col in {"status","state","kind","stage","terminal_code","failure_class"} and re.fullmatch(r"[A-Z0-9_:/|.-]{1,100}", value):
            counts.setdefault(col, {})[value] = state.get("rows")
    return {"table": row["table"], "rows": row["rows"], "groups": counts}


path=Path(sys.argv[1])
with ExitStack() as stack:
    archive=stack.enter_context(zipfile.ZipFile(path))
    if "DIAGNOSTIC_SCOPE.json" not in archive.namelist():
        matches=[item for item in archive.infolist() if item.filename.rsplit("/",1)[-1]=="STARCORE_FULL_REPORT.zip"]
        assert len(matches)==1 and matches[0].file_size <= 160_000_000
        archive=stack.enter_context(zipfile.ZipFile(io.BytesIO(archive.read(matches[0]))))
    members=archive.infolist()
    assert len(members)==len({m.filename for m in members})
    assert sum(m.file_size for m in members)<=750_000_000
    assert archive.testzip() is None
    def doc(name):
        return json.loads(archive.read(name))
    src=doc("source-manifest.json")
    sem=doc("R31_SEMANTIC_RECEIPT.json")
    scope=doc("DIAGNOSTIC_SCOPE.json")
    r26=doc("R26_REPORT.json")
    r30=doc("R30_REPORT.json")
    assert src["sourceCommit"]==scope["sourceCommit"]==sem["sourceCommit"]
    assert src["snapshotSha256"]==scope["snapshotSha256"]==sem["snapshotSha256"]
    assert hashlib.sha256(archive.read("R30_REPORT.json")).hexdigest()==sem["reportSha256"]
    emit("REPORT_IDENTITY",selected(src,["sourceCommit","snapshotSha256","snapshotBytes","apkSha256","versionCode","appVersionCode","reportProfile"]))
    emit("CHECK_OUTCOMES",Counter((str(x.get("executionState",x.get("execution_state","UNSPECIFIED")))) for x in sem["checks"].values()) if isinstance(sem["checks"],dict) else list(selected(x,["id","execution_state","executionState"]) for x in sem["checks"] if isinstance(x,dict)))
    tables={t["table"]:t for t in scope["tables"]}
    table_groups={
        "CREATE":r"family_creation|ablation|creation|genome|r952_candidate$|research_action_v2|r27_(generation|policy|lineage)$|r975_evolution_review$",
        "TRAIN":r"training_|r952_artifact|r952_learning_cursor|r27_atomnet_epoch|r26_training_step|research_rolling_(feature|version|event|head)|research_task_r967",
        "CAPTURE":r"selected_evidence|deferred_evidence|settlement_task|canonical|v57_shadow_observation|analysis_request",
        "QUALIFICATION":r"research_certificate|calibration|r958_frozen_predraw|r967_frozen_evaluation|fixed_window|mainline_contribution",
        "CHART":r"chart_cache|r30r1_(input_head|chart|research_applied)|r27_brier_pair",
    }
    for tag, pattern in table_groups.items():
        rows=[status_table(t) for name,t in tables.items() if re.search(pattern,name)]
        emit(tag, rows)
    emit("R30_CHAPTERS",[selected(x,["sectionId","status","reason","evidenceLevel"]) for x in r30["chapters"]])
    emit("R30_METRICS",[selected(x,["id","metricId","kind","value","status","sampleCount","denominator","missingReason","evidenceLevel"]) for x in r30["metricResults"]])
    emit("R30_TABLES",[selected(x,["id","totalMatchingRows","status","shown","sourceVersionStatus","readOnly"]) for x in r30["researchTables"]])
    idx=r30.get("objectIndex",{})
    if isinstance(idx,dict):
        for k,value in idx.items():
            if re.search(r"chart|research",k,re.I):
                if isinstance(value,dict):
                    emit("R30_INDEX",{"key":k,"summary":selected(value,["totalRows","totalObjects","status","inlineByteBudgetExceeded","displayIsFullDenominator","availableRows","exportedRows","fullOriginal"]),"keys":list(value)})
                elif isinstance(value,list):
                    emit("R30_INDEX",{"key":k,"count":len(value)})
    diag=doc("UPGRADE_DIAGNOSIS.json")
    findings=diag.get("findings",diag.get("diagnosis",{}).get("findings",[]))
    if isinstance(findings,list):
        emit("UPGRADE_FINDINGS",[selected(x,["id","status","priority","severity","action","conclusion"]) for x in findings])
    assess=r26.get("strategyAssessment",{})
    emit("R26_STRATEGY",selected(assess,["totalPolicies","exportedPolicies","omittedPolicies","evolution"]))
    for x in r26["sections"]:
        if x.get("sectionId") in {"TRAINING","PERFORMANCE","RECOVERY","BACKGROUND","CONTINUOUS_RUNTIME","R27_03_CONTINUOUS_RUNTIME","R29_12_ACCEPTANCE_GAPS"}:
            emit("R26_TRACK",selected(x,["sectionId","elapsedMs","availableRows","exportedRows","status","reason"]))

    emit("ACTUAL_MEMBER_SIZES", [[m.filename,m.file_size,m.compress_size] for m in members])
    emit("CAPTURE_ROOTS", {"scope":sorted(scope), "identity":sorted(src), "r26":sorted(r26), "r30":sorted(r30)})
    focused = re.compile(r"(research_(training|task_checkpoint|development_receipt|working_storage|family|atom_catalog_import|molecule_import|rolling|action|feedback|formal|fixed|evolution|teacher|long)|r952_(candidate|artifact|learning|research_outbox|matrix)|r26_(task$|capability$|candidate$|checkpoint|training_step|evolution|live_model)|r27_(policy$|generation$|task_error$|lineage$)|v57_(settlement_task|deferred_evidence|analysis_request|research_task|research_lane_cursor|draw_canonical)|r967_frozen|runtime_owner|schema_migration|r33_research)")
    def safe_reason(value):
        if not isinstance(value,str) or not value:
            return None
        if re.fullmatch(r"[A-Z][A-Z0-9_:.|-]{0,140}",value):
            return value
        codes = re.findall(r"(?:reason|category|phase|cause|state|operation|status|commitOutcome)=([A-Z][A-Z0-9_]{2,120})",value)
        known = re.findall(r"\b(?:WAIT[A-Z0-9_]+|YIELD[A-Z0-9_]+|R[0-9]+_[A-Z][A-Z0-9_]{3,105}|[A-Z][A-Z0-9_]+_MISSING|[A-Z][A-Z0-9_]+_FAILED)\b",value)
        return "|".join(dict.fromkeys((codes+known)[:12])) or "OTHER_TEXT"
    numeric = {"lane","cursor","next_index","next_row","rows","train_n","validation_n","future_n","optimizer_steps","attempt_count","next_sequence","fence","steps","last_progress","updated_at","created_at","last_committed_at","last_progress_at","lease_until","min_ready","next_elapsed_ms","actual","model_updates","numeric_updates","generation","ready_count","window_end","max_ready_target"}
    enums = {"kind","status","state","operation","phase","failure_class","terminal_code","terminal_kind","last_error","reason","outcome","blocked_reason","wait_detail","wait_kind","last_reason","last_outcome","dependency","result_state","proof_error","training_abi","family","role","problem_type","source_kind","task_kind","error_class","message"}
    issues = {"desired_issue","served_issue","target_issue","last_issue","cutoff_issue","through_issue","first_issue","validation_last","source_issue","completed_input_ordinal","work_cursor","known_latest"}
    ids = {"task_id","candidate_id","version_id","policy_hash","protocol_hash","receipt_hash","rule_hash","artifact_hash","authority_ref","sourceCommit","parent_id"}
    def allowed(key,value):
        if value is None or isinstance(value,bool):
            return value
        if key in issues:
            text = str(value)
            if re.fullmatch(r"[0-9]{1,15}",text): return value
            return safe_reason(value)
        if key in numeric:
            if isinstance(value,(int,float)): return value
            if isinstance(value,str) and re.fullmatch(r"[A-Z_]+[|][0-9]+",value): return value
            if isinstance(value,str):
                match=re.match(r"^([A-Z_]+)\|([0-9]+)\|",value)
                if match: return match.group(1)+"|"+match.group(2)
            return None
        if key in ids:
            if isinstance(value,str) and (re.fullmatch(r"[0-9a-f]{12,64}",value)
                or re.fullmatch(r"(?:R952-C-|lane:|r26-|r27-)[a-zA-Z0-9_:-]{1,72}",value)):
                return value[:28]
            return None
        if key in enums: return safe_reason(value)
        return None
    def brief(row):
        return {k:allowed(k,v) for k,v in row.items()
                if k in numeric|enums|issues|ids and allowed(k,v) is not None}
    for name,row in sorted(tables.items()):
        if focused.search(name):
            entry=status_table(row)
            # Report high-cardinality dimensions as counts, without ever logging raw witness text.
            entry["groups"]={col:dict(Counter(values).most_common(24))
                             for col,values in entry["groups"].items()}
            emit("FIELD_TABLE",entry)
    recent={x["table"]:x for x in scope.get("recentEvidence",[]) if "table" in x}
    detailed={"research_family_cursor_cp12","research_family_creation_cp12","research_action_v2","research_training_recovery_r343","research_rolling_version_r343","research_rolling_head_r343","research_rolling_offer_r343","research_rolling_frozen_publication_r343","research_rolling_publication_reject_r343","research_training_feature_cursor_cp12","research_development_receipt_v2","r952_candidate","r26_task","v57_research_lane_cursor_r967","v57_schema_migration_journal_r926","v57_analysis_request_r36","v57_deferred_evidence_job","v57_settlement_task_r36"}
    for name,row in sorted(recent.items()):
        if not focused.search(name): continue
        rows=[r for r in row.get("rows",[]) if isinstance(r,dict)]
        counts={key:dict(Counter(reason for r in rows
                       if (reason:=safe_reason(r.get(key))) is not None).most_common(24))
                for key in enums if any(key in r for r in rows)}
        emit("FIELD_ROWS",{"table":name,"total":row.get("totalRows",len(rows)),"n":len(rows),
                           "columns":sorted(set().union(*(r.keys() for r in rows))),"groups":counts})
        if name=="v57_research_task_r967":
            for kind in ["TRAIN_CANDIDATE","FREEZE_CANDIDATE","R343_ROLLING_TRAIN","R343_ROLLING_FREEZE","RESEARCH_ACTION","POST_TRAIN"]:
                work=[r for r in rows if r.get("kind")==kind]
                emit("FIELD_WORK_BY_KIND",{"kind":kind,"rows":len(work),
                         "states":Counter(r.get("status","") for r in work),
                         "detail":[brief(r) for r in work[:15]]})
        elif name in detailed:
            limit=22 if name not in {"v57_deferred_evidence_job","v57_settlement_task_r36","v57_analysis_request_r36"} else 5
            emit("FIELD_WITNESSES",{"table":name,"rows":[brief(r) for r in rows[:limit]]})
    for key,val in r26.items():
        if key in {"facts","strategyAssessment","r27Summary","committedResearchService","captureFactsError","strategyAssessmentError","asOfIssue"}:
            emit("R26_ROOT_DETAIL",{"key":key,"keys":sorted(val) if isinstance(val,dict) else [],"type":type(val).__name__})
            if key=="facts" and isinstance(val,dict):
                for component in ("tasks","taskLivenessR30","trainingStepSummary","neverProgressed"):
                    datum=val.get(component)
                    if isinstance(datum,list):
                        emit("R26_OWNER",{"key":component,"count":len(datum),"rows":[brief(r) for r in datum[:64] if isinstance(r,dict)]})
                    elif isinstance(datum,dict):
                        emit("R26_OWNER",{"key":component,"keys":sorted(datum),"numeric":{k:v for k,v in datum.items() if isinstance(v,(int,bool,float))}})
            if key=="strategyAssessment" and isinstance(val,dict):
                emit("R26_ASSESSMENT",{"count":val.get("totalPolicies"),"evolution":val.get("evolution"),"policies":[brief(r) for r in val.get("policies",[]) if isinstance(r,dict)]})
    for m in members:
        if not re.fullmatch(r"catalog-\d+\.jsonl",m.filename): continue
        for raw in archive.open(m):
            row=json.loads(raw)
            if row.get("kind")!="check_result": continue
            ident=row.get("id","")
            if not re.fullmatch(r"RSEC-[0-9]{3}",str(ident)): continue
            emit("FIELD_CHECK",{"id":ident,**{k:safe_reason(row.get(k)) for k in
                 ("execution_state","outcome","domain_outcome","health","status","reason") if k in row},
                 "evidenceBytes":len(str(row.get("evidence","")).encode())})

    dimensions = re.compile(r"(train|task|claim|learner|direct|recover|continu|checkpoint|molecule|action|post|loop|roll|create|evolu|settle|analysis|finish|stage|blocked|wait|reason|status|sample|health|queue|elapsed|duration|compat|storage|ready|fault|hold|budget|lane|issue|source|watermark|time|owner|write|code|result|commit)",re.I)
    exact_codes=re.compile(r"[A-Za-z][A-Za-z0-9_.:$-]+Exception|SQLITE_[A-Z0-9_]+|REPORT_[A-Z0-9_]+|(?:RESEARCH|R343|R952|WAIT|YIELD|CKPT|TRAIN|SEED|V51|R955|F01)_[A-Z0-9_]{2,90}")
    def context_value(value,depth=0):
        if depth>5: return "DEPTH_LIMIT"
        if isinstance(value,dict):
            return {k:context_value(v,depth+1) for k,v in value.items() if dimensions.search(k)}
        if isinstance(value,list):
            return [context_value(v,depth+1) for v in value[:70]]
        if value is None or isinstance(value,(int,float,bool)): return value
        if not isinstance(value,str): return None
        if re.fullmatch(r"[0-9a-f]{10,64}|(?:CP12-FAMILY-|R952-C-)[0-9a-f]+",value): return value
        if re.fullmatch(r"[A-Z0-9_./:-]{1,120}",value):return value
        tokens=exact_codes.findall(value)
        result=safe_reason(value)
        if result and result!="OTHER_TEXT": tokens.append(result)
        for field, n in re.findall(r"([a-zA-Z][a-zA-Z0-9_]{0,40})=([0-9]{1,16})",value):
            if dimensions.search(field): tokens.append(field+"="+n)
        if "no such table" in value:
            match=re.search(r"no such table: ([A-Za-z0-9_]+)",value)
            if match: tokens.append("NO_SUCH_TABLE:"+match.group(1))
        if "no such column" in value:
            match=re.search(r"no such column: ([A-Za-z0-9_.]+)",value)
            if match: tokens.append("NO_SUCH_COLUMN:"+match.group(1))
        return "|".join(dict.fromkeys(tokens)) or "OTHER_TEXT"
    scheduler=scope.get("researchSchedulerState",[])
    for item in scheduler:
        if isinstance(item,dict):
            key=item.get("key",item.get("state_key",item.get("name","")))
            if re.match(r"(r343_|r65_|r912_|r908_|r975_|r962_|r971_|r980_|cp1[12]_)",key):
                emit("SCHEDULER_VALUE",{"key":key,"fields":sorted(item), "updatedAt":item.get("updatedAt"), "status":item.get("status"),"values":{k:context_value(item[k]) for k in ("value","state_value","text","display","preview","typedValue","valuePreview","utf8","detail","reason","typed_value") if k in item}})
    for name in ("research_molecule_seed_v2","research_training_input_v2","v57_research_lane_cursor_r967","v57_runtime_event_r926","v57_runtime_event_scope_r341"):
        datum=recent.get(name,{})
        rows=[r for r in datum.get("rows",[]) if isinstance(r,dict)]
        emit("EXTRA_TABLE",{"name":name,"rows":len(rows),"groups":Counter(str(r.get("status",r.get("state",""))) for r in rows),"columns":sorted(set().union(*(r.keys() for r in rows)))})
    emit("FIELD_SCHEDULER_STATE",context_value(scheduler))
    emit("CAPTURE_IDENTITY_AUDIT",context_value({k:v for k,v in src.items() if k in ("reportSourcePreparation","verifiedInstallationR341","mainWriterFaultsR342","r341SqlObservationScope","r26CheckpointAuditR32","r26CheckpointByteValidation","generatedRuleSha256","runtimeObservedAt")}))
    emit("PHYSICAL_R30_HEALTH",context_value(r30.get("sourceHealthR341",{})))
    emit("PHYSICAL_R30_ACCEPTANCE",context_value(r30.get("evidenceAssessmentR33",{})))
    for name in ("research_molecule_import_checkpoint_cp12","research_molecule_import_cp12","research_atom_catalog_import_cp12",
                 "research_training_recovery_r343","research_training_protocol_v2","research_training_input_v2","research_training_feature_cursor_cp12",
                 "v57_research_task_r967","research_family_cursor_cp12","research_rolling_event_r343"):
        p=recent.get(name,{})
        rows=[r for r in p.get("rows",[]) if isinstance(r,dict)]
        if name=="v57_research_task_r967":
            rows=[r for r in rows if r.get("kind") in ("TRAIN_CANDIDATE","FREEZE_CANDIDATE","R343_ROLLING_TRAIN","R343_ROLLING_FREEZE")]
        safe_keys={"task_id","candidate_id","stream_seq","input_hash","authority_ref","protocol_hash","failed_task_id","successor_task_id","source_hash","ast_hash","id","current","target_issue","state","status","kind","source_issue","cutoff_issue","cursor","created_at","updated_at","lease_until","available_at","attempt_count","last_error","last_reason","next_row","scan_row","material_row","base_row","epoch","generation","next_variant","family","train_n","validation_last","row_count"}
        emit("TARGET_LEDGER",{"table":name,"rows":[{k:context_value(v) for k,v in row.items() if k in safe_keys} for row in rows[:30]]})
    safe_task_ids={"modern-research-continuity","cold-rebirth-r34","warm-candidates","live-model-training","feedback","r27-chart-cache","r27-evolution","r27-replay-dataset","r27-live","lifecycle-schema","schema","r27-atomnet","r27-eval","r27-health"}
    for name in ("r26_task","v57_research_lane_cursor_r967","research_family_creation_cp12","r952_candidate","v57_deferred_evidence_job"):
        rows=[r for r in recent.get(name,{}).get("rows",[]) if isinstance(r,dict)]
        if name=="r26_task":
            rows=[r for r in rows if r.get("task_id") in safe_task_ids]
        elif name=="v57_research_lane_cursor_r967":
            rows=[r for r in rows if str(r.get("lane")) in {"0","1","6","9","13","17","18"}]
        elif name=="v57_deferred_evidence_job":
            emit("DEFERRED_REASON_COUNTS",Counter(context_value(r.get("last_error","")) for r in rows).most_common(16))
            rows=[]
        for row in rows:
            if name=="r952_candidate" and row.get("candidate_id") not in {"CP12-FAMILY-ebfcc38e32ef9155cf4cbae7","CP12-FAMILY-872002fb10d1f206e96eae54"}: continue
            fields={k:context_value(v) for k,v in row.items() if k in {"task_id","candidate_id","status","state","last_error","last_outcome","last_served","last_progress","updated_at","lane","wait_kind","wait_detail","wait_since","blocked_reason","cursor","desired_issue","served_issue","active_task_id","steps","fence","family","creation_kind","reason","next_variant","created_issue"}}
            if row.get("task_id") in safe_task_ids: fields["task_id"]=row["task_id"]
            emit("OWNER_STATUS",{"table":name,"fields":fields})
    for component in r26.get("sections",[]):
        ident=component.get("sectionId")
        metrics=component.get("metrics",[])
        if ident in ("CONTINUOUS_RUNTIME","BACKGROUND","FAILURES","EVOLUTION","TRAINING","FREEZE") and isinstance(metrics,list):
            clean=[{k:context_value(v) for k,v in r.items() if k in {"task_id","status","lane","cursor","steps","last_progress","blocked_reason","message","recorded_at","error_class","dev_n","val_n","policy_hash","attempts","structuralCandidates"}} for r in metrics if isinstance(r,dict)]
            emit("PHYSICAL_R26_SECTION",{"id":ident,"available":component.get("availableRows"),"metrics":clean})
            if ident=="CONTINUOUS_RUNTIME":
                for row in metrics:
                    if row.get("task_id") in safe_task_ids:
                        reason=row.get("blocked_reason","")
                        emit("R26_TASK_OWNER",{"task":row["task_id"],"state":row.get("status"),"steps":row.get("steps"),"cursor":row.get("cursor"),"lastProgress":row.get("last_progress"),"reason":context_value(reason),
                          "reasonTokens":re.findall(r"\b[A-Z][A-Z0-9_]{3,70}\b",reason)[:12] if isinstance(reason,str) else []})
    training_inputs=set(["2bac10f86871fbee7b0c7eae5111b48499f7b1fe6714fe0f122e8efb720cf004","735ed22d0d458606994911222372c46d6021cc6c7fa40e84351102316e4f5aa7","642b4f9808a50c5ccfdbe348bbc7b9a70f8a37dab11a300ae8515582a30bb622","129c749b58ccfe9884070607d83b7ec51e2f742481495c0d24d2050c7b012f01"])
    for row in (json.loads(line) for line in archive.open("FILE_STATE.jsonl")):
        leaf=str(row.get("path","")).rsplit("/",1)[-1]
        if leaf[:64] in training_inputs:
            emit("ORIGINAL_TRAINING_INPUT",{"sha256InName":leaf[:64],"suffix":leaf[64:],"state":row.get("state"),"bytes":row.get("bytes",row.get("size"))})
    for item in scope.get("tables",[]):
        if str(item.get("name",item.get("table",""))).startswith("r34_cold"):
            emit("COLD_LEDGER",{"table":item.get("name",item.get("table")),"rows":item.get("rows",item.get("rowCount")),"fields":sorted(item)})
    for m in members:
        if not re.fullmatch(r"catalog-\d+\.jsonl", m.filename): continue
        for raw in archive.open(m):
            row=json.loads(raw)
            if row.get("kind")=="check_result" and row.get("id") in {"RSEC-068","RSEC-069","RSEC-071","RSEC-073"}:
                evidence=str(row.get("evidence",""))
                candidates=[line for line in evidence.splitlines() if re.search(r"(HARD_BLOCK|WATCH|LOCKED|^[^=]{2,64}=[^=]{1,140}$|owner|ablation|binding|缺|拦截|不一致|风险)",line,re.I)]
                safe_rows=[]
                for line in candidates[:120] if row["id"]=="RSEC-071" else candidates[:35]:
                    if re.search(r"(https?://|/(data|storage|sdcard)/|[A-Z0-9_]*KEY=|token=|bearer)",line,re.I): continue
                    if len(line)>265: continue
                    safe_rows.append(line)
                emit("AUDIT_FINDING",{"id":row["id"],"lines":safe_rows})
            if row.get("kind")=="check_result" and row.get("execution_state")!="EXECUTED":
                emit("FIELD_CHECK_CAUSE",{"id":row.get("id"),"reasons":context_value({k:v for k,v in row.items() if k in ("reason","failure","error","exception","errorChain","details","source")}),
                     "frames":re.findall(r"\bcom\.ethk3\.novaonepro\.[A-Za-z0-9_$.]+\([A-Za-z0-9_]+\.java:[0-9]+\)",str(row.get("error_detail","")))[:36],
                     "errorCodes":list(dict.fromkeys(exact_codes.findall(str(row.get("error_detail","")))))[:12],
                     "detailKeys":list(row["error_detail"]) if isinstance(row.get("error_detail"),dict) else []})
