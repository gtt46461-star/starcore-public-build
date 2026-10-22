#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def rewrite(path, old, new, count=1):
    p = ROOT / path
    text = p.read_text(encoding='utf-8')
    found = text.count(old)
    if found != count:
        raise SystemExit(f'{path}: expected {count} occurrences, found {found}: {old[:120]}')
    p.write_text(text.replace(old, new, count), encoding='utf-8')
    print(f'REWRITE_OK {path} occurrences={count}')

def require(path, needle):
    text=(ROOT/path).read_text(encoding='utf-8')
    if needle not in text:
        raise SystemExit(f'{path}: required concurrent closeout invariant missing: {needle[:120]}')
    print(f'REQUIRE_OK {path}: {needle[:80]}')

# C08 was closed concurrently with a stronger implementation: lifecycle positive states now require
# the exact shared production authorizer and PAUSED has precedence over stale ACTIVE certificates.
# Verify that invariant instead of overwriting it with the earlier, weaker one-line guard.
require('app/src/main/java/com/ethk3/novaonepro/ResearchLifecycleR932.java',
        'store!=null&&next!=State.PAUSED&&next!=State.QUALITY_PRUNED&&next!=State.QUARANTINED')
require('app/src/main/java/com/ethk3/novaonepro/ResearchLifecycleR932.java',
        'ResearchPlatformStoreR929.authorizeCurrent(store,CertificateBundleServiceR929.Consumer.valueOf(consumer)')

# C05: retire a stale scheduling champion before selecting/replacing incumbents.
rewrite(
    'app/src/main/java/com/ethk3/novaonepro/ResearchCohortR932.java',
    '            // First incumbent requires a published, independently supported certificate. A later\n'
    '            // challenger cannot replace it by sorting training scores or cumulative hit rates.\n',
    '            // Scheduling champions are reversible projections, not statistical-family deletion.\n'
    '            // A revoked/closed/paused incumbent must not block the family forever.\n'
    '            db.execSQL("DELETE FROM "+CHAMPION+" WHERE NOT EXISTS(SELECT 1 FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" c WHERE c.certificate_hash="+CHAMPION+".certificate_hash AND c.state=\'ACTIVE\' AND c.all_gates_pass=1 AND c.maximum_weight>0 AND c.structural_hash="+CHAMPION+".structural_hash AND (CASE c.consumer WHEN \'INVENTED_ALGORITHM\' THEN \'algorithm:\' WHEN \'GENESIS_ASSET\' THEN \'genesis:\' ELSE \'formula:\' END)||c.candidate_id="+CHAMPION+".candidate_key AND NOT EXISTS(SELECT 1 FROM "+ResearchPlatformStoreR929.TABLE_REVOCATION+" rv WHERE rv.certificate_hash=c.certificate_hash)) OR EXISTS(SELECT 1 FROM "+ResearchLifecycleR932.TABLE+" life WHERE life.candidate_key="+CHAMPION+".candidate_key AND life.state IN (\'QUALITY_PRUNED\',\'QUARANTINED\',\'PAUSED\'))");\n'
    '            // First incumbent requires a published, independently supported certificate. A later\n'
    '            // challenger cannot replace it by sorting training scores or cumulative hit rates.\n'
)

# C05: avoid repeatedly examining the same first 8 certificates; already registered pairs and
# the incumbent/same-structure rows are excluded, so each maintenance slice advances fairly.
old_query = '''            try(Cursor c=db.rawQuery("SELECT f.candidate_key,f.structural_hash,c.candidate_id,c.consumer,c.structural_hash,c.evidence_hash FROM "+CHAMPION+" f JOIN "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" c ON c.family_id=f.family WHERE c.state='ACTIVE' AND c.maximum_weight>0 AND c.all_gates_pass=1 ORDER BY c.issued_at,c.candidate_key LIMIT 8",null)){\n                while(c.moveToNext()){\n                    String prefix=c.getString(3).equals("INVENTED_ALGORITHM")?"algorithm":c.getString(3).equals("GENESIS_ASSET")?"genesis":"formula",key=prefix+':'+c.getString(2);\n                    if(!key.equals(c.getString(0))&&!c.getString(1).equals(c.getString(4)))PairedResearchChallengeR932.request(db,"FAMILY_REPLACEMENT",c.getString(0),key,c.getString(1),c.getString(4),c.getString(5));\n                }\n            }\n'''
new_query = '''            try(Cursor c=db.rawQuery("SELECT f.candidate_key,f.structural_hash,c.candidate_id,c.consumer,c.structural_hash,c.evidence_hash FROM "+CHAMPION+" f JOIN "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" c ON c.family_id=f.family WHERE c.state='ACTIVE' AND c.maximum_weight>0 AND c.all_gates_pass=1 AND NOT EXISTS(SELECT 1 FROM "+ResearchPlatformStoreR929.TABLE_REVOCATION+" rv WHERE rv.certificate_hash=c.certificate_hash) AND ((CASE c.consumer WHEN 'INVENTED_ALGORITHM' THEN 'algorithm:' WHEN 'GENESIS_ASSET' THEN 'genesis:' ELSE 'formula:' END)||c.candidate_id)<>f.candidate_key AND c.structural_hash<>f.structural_hash AND NOT EXISTS(SELECT 1 FROM "+PairedResearchChallengeR932.TABLE+" p WHERE p.purpose='FAMILY_REPLACEMENT' AND p.parent_key=f.candidate_key AND p.child_key=((CASE c.consumer WHEN 'INVENTED_ALGORITHM' THEN 'algorithm:' WHEN 'GENESIS_ASSET' THEN 'genesis:' ELSE 'formula:' END)||c.candidate_id)) ORDER BY c.issued_at,c.candidate_key LIMIT 8",null)){\n                while(c.moveToNext()){\n                    String prefix=c.getString(3).equals("INVENTED_ALGORITHM")?"algorithm":c.getString(3).equals("GENESIS_ASSET")?"genesis":"formula",key=prefix+':'+c.getString(2);\n                    PairedResearchChallengeR932.request(db,"FAMILY_REPLACEMENT",c.getString(0),key,c.getString(1),c.getString(4),c.getString(5));\n                }\n            }\n'''
rewrite('app/src/main/java/com/ethk3/novaonepro/ResearchCohortR932.java', old_query, new_query)

# C05/C08: family replacement is valid only while the preregistered parent is still the incumbent;
# replacement itself is an exact parent+structure CAS and requires the child's live unrevoked cert.
rewrite(
    'app/src/main/java/com/ethk3/novaonepro/PairedResearchChallengeR932.java',
    'import android.database.Cursor;\nimport android.database.sqlite.SQLiteDatabase;\n',
    'import android.content.ContentValues;\nimport android.database.Cursor;\nimport android.database.sqlite.SQLiteDatabase;\n'
)
rewrite(
    'app/src/main/java/com/ethk3/novaonepro/PairedResearchChallengeR932.java',
    '        String parent,child,first,last,frozen,purpose;\n'
    '        try(Cursor c=db.rawQuery("SELECT parent_key,child_key,first_issue,last_issue,contract_hash,purpose,state FROM "+TABLE+" WHERE challenge_id=?",new String[]{id})){\n'
    '            if(!c.moveToFirst()||!"OPEN".equals(c.getString(6)))return;parent=c.getString(0);child=c.getString(1);first=c.getString(2);last=c.getString(3);frozen=c.getString(4);purpose=c.getString(5);\n'
    '        }\n',
    '        String parent,child,parentStructure,childStructure,first,last,frozen,purpose;\n'
    '        try(Cursor c=db.rawQuery("SELECT parent_key,child_key,parent_structure,child_structure,first_issue,last_issue,contract_hash,purpose,state FROM "+TABLE+" WHERE challenge_id=?",new String[]{id})){\n'
    '            if(!c.moveToFirst()||!"OPEN".equals(c.getString(8)))return;parent=c.getString(0);child=c.getString(1);parentStructure=c.getString(2);childStructure=c.getString(3);first=c.getString(4);last=c.getString(5);frozen=c.getString(6);purpose=c.getString(7);\n'
    '        }\n'
)
rewrite(
    'app/src/main/java/com/ethk3/novaonepro/PairedResearchChallengeR932.java',
    '        String state=!frozen.equals(contract(db))?"PAUSED_DEPENDENCY":n!=PLANNED?"PAUSED_CAPTURE":ch/(double)n>=.4&&cb/n<=.21&&cb<=pb&&ece<=.04&&delta>0&&raw<=alpha&&maxCost<=250000000L?"PASSED":"FAILED";\n',
    '        String state=!frozen.equals(contract(db))?"PAUSED_DEPENDENCY":purpose.equals("FAMILY_REPLACEMENT")&&!incumbentStillCurrent(db,parent,parentStructure)?"PAUSED_DEPENDENCY":n!=PLANNED?"PAUSED_CAPTURE":ch/(double)n>=.4&&cb/n<=.21&&cb<=pb&&ece<=.04&&delta>0&&raw<=alpha&&maxCost<=250000000L?"PASSED":"FAILED";\n'
)
rewrite(
    'app/src/main/java/com/ethk3/novaonepro/PairedResearchChallengeR932.java',
    '            if(purpose.equals("FAMILY_REPLACEMENT")&&state.equals("PASSED"))replace(db,parent,child,hash,TargetIssueResolver.next(endpoint).targetIssue);\n',
    '            if(purpose.equals("FAMILY_REPLACEMENT")&&state.equals("PASSED"))replace(db,parent,child,parentStructure,childStructure,hash,TargetIssueResolver.next(endpoint).targetIssue);\n'
)
old_replace = '''    private static void replace(SQLiteDatabase db,String parent,String child,String proof,String next){\n        String consumer=child.startsWith("algorithm:")?"INVENTED_ALGORITHM":child.startsWith("genesis:")?"GENESIS_ASSET":"FORMULA_GRAPH";\n        try(Cursor c=db.rawQuery("SELECT family_id,structural_hash,certificate_hash FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" WHERE consumer=? AND candidate_id=? AND state='ACTIVE' AND all_gates_pass=1 AND maximum_weight>0 ORDER BY issued_at DESC LIMIT 1",new String[]{consumer,child.substring(child.indexOf(':')+1)})){\n            if(c.moveToFirst())db.execSQL("UPDATE "+ResearchCohortR932.CHAMPION+" SET candidate_key=?,structural_hash=?,certificate_hash=?,evidence_hash=?,selected_issue=? WHERE family=? AND candidate_key=?",new Object[]{child,c.getString(1),c.getString(2),proof,next,c.getString(0),parent});\n        }\n    }\n'''
new_replace = '''    static boolean incumbentStillCurrent(SQLiteDatabase db,String parent,String structure){\n        try(Cursor c=db.rawQuery("SELECT 1 FROM "+ResearchCohortR932.CHAMPION+" f JOIN "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" pc ON pc.certificate_hash=f.certificate_hash WHERE f.candidate_key=? AND f.structural_hash=? AND pc.state='ACTIVE' AND pc.all_gates_pass=1 AND pc.maximum_weight>0 AND NOT EXISTS(SELECT 1 FROM "+ResearchPlatformStoreR929.TABLE_REVOCATION+" rv WHERE rv.certificate_hash=pc.certificate_hash) LIMIT 1",new String[]{parent,structure})){return c.moveToFirst();}\n    }\n    static void replace(SQLiteDatabase db,String parent,String child,String parentStructure,String childStructure,String proof,String next){\n        String consumer=child.startsWith("algorithm:")?"INVENTED_ALGORITHM":child.startsWith("genesis:")?"GENESIS_ASSET":"FORMULA_GRAPH";\n        try(Cursor c=db.rawQuery("SELECT family_id,structural_hash,certificate_hash FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" WHERE consumer=? AND candidate_id=? AND structural_hash=? AND state='ACTIVE' AND all_gates_pass=1 AND maximum_weight>0 AND NOT EXISTS(SELECT 1 FROM "+ResearchPlatformStoreR929.TABLE_REVOCATION+" rv WHERE rv.certificate_hash="+ResearchPlatformStoreR929.TABLE_CERTIFICATE+".certificate_hash) ORDER BY issued_at DESC LIMIT 2",new String[]{consumer,child.substring(child.indexOf(':')+1),childStructure})){\n            if(!c.moveToFirst())throw new IllegalStateException("R932_REPLACEMENT_CHILD_CERTIFICATE_MISSING");\n            String family=c.getString(0),structure=c.getString(1),certificate=c.getString(2);\n            if(c.moveToNext())throw new IllegalStateException("R932_REPLACEMENT_CHILD_CERTIFICATE_AMBIGUOUS");\n            ContentValues values=new ContentValues();values.put("candidate_key",child);values.put("structural_hash",structure);values.put("certificate_hash",certificate);values.put("evidence_hash",proof);values.put("selected_issue",next);\n            int rows=db.update(ResearchCohortR932.CHAMPION,values,"family=? AND candidate_key=? AND structural_hash=?",new String[]{family,parent,parentStructure});\n            if(rows!=1)throw new IllegalStateException("R932_REPLACEMENT_INCUMBENT_CAS_FAILED");\n            try(Cursor verify=db.rawQuery("SELECT candidate_key,structural_hash,certificate_hash,evidence_hash,selected_issue FROM "+ResearchCohortR932.CHAMPION+" WHERE family=?",new String[]{family})){\n                if(!verify.moveToFirst()||!child.equals(verify.getString(0))||!childStructure.equals(verify.getString(1))||!certificate.equals(verify.getString(2))||!proof.equals(verify.getString(3))||!next.equals(verify.getString(4)))throw new IllegalStateException("R932_REPLACEMENT_CAS_VERIFY_FAILED");\n            }\n        }\n    }\n'''
rewrite('app/src/main/java/com/ethk3/novaonepro/PairedResearchChallengeR932.java', old_replace, new_replace)

print('R932_CLOSEOUT_REWRITE_PASS')
