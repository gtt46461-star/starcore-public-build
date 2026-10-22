#!/usr/bin/env python3
"""Upgrade LifecycleIntegrityHarness to prove the current production authorizer positively.

The harness must not fabricate a superficially ACTIVE certificate and call it valid. It registers
an ACTIVE runtime bundle plus a completed prior trial, freezes every dependency hash expected by
ResearchPlatformStoreR929.authorizeCurrent(), and invokes lifecycle review through its DbStore
entry so a positive state can only come from the shared production authorizer. Negative variants
then mutate exactly one dependency.
"""
from pathlib import Path

R = Path(__file__).resolve().parents[2]
p = R / 'verification/r932/LifecycleIntegrityHarnessR932.java'
s = p.read_text(encoding='utf-8')

def one(old, new):
    global s
    n=s.count(old)
    if n!=1:
        raise SystemExit('R932_LIFECYCLE_FIXTURE_ANCHOR_COUNT=%d:%s' % (n, old[:100]))
    s=s.replace(old,new,1)

one(
'''    private static String certificate(SQLiteDatabase db,String k){
        ContentValues v=new ContentValues();
        for(String f:new String[]{"candidate_key","namespace","family_id","candidate_id","structural_hash","evidence_hash","bundle_hash","schema_hash","atom_graph_hash","model_hash","policy_hash","calibration_hash","domain_hash","trial_hash","selection_hash","code_hash","gate_hash","reason","certificate_hash"})v.put(f,hash(k+'|'+f));
        v.put("consumer","INVENTED_ALGORITHM");v.put("candidate_id",k.substring(k.indexOf(':')+1));v.put("structural_hash",structure(k));
        v.put("bundle_hash",ModelBundleRuntimeR932.current().hash);v.put("schema_hash",V51ActionMetaFeatureEngine.featureManifestHash());
        v.put("atom_graph_hash",AtomCaptureR932.graphHash(db));v.put("selection_hash",CertificateDependenciesR932.selectionHash());v.put("code_hash",SourceManifestR815.SOURCE_TREE_SHA256);
        v.put("level","L6_CONTROLLED_INFLUENCE");v.put("state","ACTIVE");v.put("valid_from_issue","202609100001");v.put("valid_until_issue","");
        v.put("maximum_weight",.05);v.put("all_gates_pass",1);v.put("issued_at",1L);v.put("updated_at",1L);v.put("contract_revision","R932_REQUIRED_GATES_V1");
        db.insertWithOnConflict(ResearchPlatformStoreR929.TABLE_CERTIFICATE,null,v,0);
        return hash(k+"|certificate_hash");
    }
''',
'''    private static String certificate(SQLiteDatabase db,String k){
        String family="INTEGRITY_TEST_FAMILY",structural=structure(k),trial=hash(k+"|trial"),cert=hash(k+"|certificate_hash");
        db.execSQL("INSERT INTO "+ResearchPlatformStoreR929.TABLE_TRIAL+"(trial_id,identity_hash,stage,status,dataset_hash,schema_hash,source_hash,hyper_hash,family_id,structural_hash,split_hash,seed,registered_at,finished_at,record_hash) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                new Object[]{trial,trial,"EVALUATE","COMPLETED",hash(k+"|dataset"),V51ActionMetaFeatureEngine.featureManifestHash(),SourceManifestR815.SOURCE_TREE_SHA256,hash(k+"|hyper"),family,structural,hash(k+"|split"),1,1,2,trial});
        ContentValues v=new ContentValues();
        for(String f:new String[]{"candidate_key","namespace","family_id","candidate_id","structural_hash","evidence_hash","bundle_hash","schema_hash","atom_graph_hash","model_hash","policy_hash","calibration_hash","domain_hash","trial_hash","selection_hash","code_hash","gate_hash","reason","certificate_hash"})v.put(f,hash(k+'|'+f));
        v.put("consumer","INVENTED_ALGORITHM");v.put("candidate_id",k.substring(k.indexOf(':')+1));v.put("family_id",family);v.put("structural_hash",structural);
        v.put("bundle_hash",ModelBundleRuntimeR932.current().hash);v.put("schema_hash",V51ActionMetaFeatureEngine.featureManifestHash());
        v.put("atom_graph_hash",AtomCaptureR932.graphHash(db));v.put("model_hash",CertificateDependenciesR932.modelHash());
        v.put("policy_hash",CertificateDependenciesR932.component("POLICY"));v.put("calibration_hash",CertificateDependenciesR932.component("CALIBRATION"));
        v.put("domain_hash",DomainRouterR929.installedSchemaHash());v.put("trial_hash",trial);v.put("selection_hash",CertificateDependenciesR932.selectionHash());v.put("code_hash",SourceManifestR815.SOURCE_TREE_SHA256);
        v.put("certificate_hash",cert);v.put("level","L6_CONTROLLED_INFLUENCE");v.put("state","ACTIVE");v.put("valid_from_issue","202609100001");v.put("valid_until_issue","");
        v.put("maximum_weight",.05);v.put("all_gates_pass",1);v.put("issued_at",3L);v.put("updated_at",3L);v.put("contract_revision","R932_REQUIRED_GATES_V1");
        if(db.insertWithOnConflict(ResearchPlatformStoreR929.TABLE_CERTIFICATE,null,v,0)<0)throw new IllegalStateException("R932_FIXTURE_CERTIFICATE_INSERT");
        return cert;
    }
''')

one(
'''        SQLiteDatabase db=new SQLiteDatabase(args[0]);try{
            GlobalResearchScheduleR932.schema(db);FixedResearchWindowR932.schema(db);FixedWindowCertificatePublisherR932.schema(db);
''',
'''        SQLiteDatabase db=new SQLiteDatabase(args[0]);try(ModelBundleRuntimeR932.Scope request=ModelBundleRuntimeR932.open(null,"202609100121",System.currentTimeMillis())){
            DbStore store=new DbStore(db);
            GlobalResearchScheduleR932.schema(db);FixedResearchWindowR932.schema(db);FixedWindowCertificatePublisherR932.schema(db);
''')

one(
'''            db.beginTransaction();try{
                String key="algorithm:paused";enroll(db,key);certificate(db,key);String w=window(db,key);
''',
'''            db.beginTransaction();try{
                ResearchPlatformStoreR929.activateRuntimeBundle(db,ModelBundleRuntimeR932.current(),"202609100001",1);
                String key="algorithm:paused";enroll(db,key);certificate(db,key);String w=window(db,key);
''')

# The DB-only review entry is intentionally non-authorizing in current production. A positive
# certificate test must use the DbStore entry and therefore the exact shared authorizeCurrent path.
needle='ResearchLifecycleR932.review(db,'
count=s.count(needle)
if count < 4:
    raise SystemExit('R932_LIFECYCLE_FIXTURE_SHARED_REVIEW_COUNT=%d' % count)
s=s.replace(needle,'ResearchLifecycleR932.review(store,db,')

p.write_text(s,encoding='utf-8')
print('R932_LIFECYCLE_FIXTURE_SHARED_AUTHORITY_APPLIED reviews=%d' % count)

