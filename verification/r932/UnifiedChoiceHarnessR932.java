package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

public final class UnifiedChoiceHarnessR932 {
    static void check(boolean pass,String name){if(!pass)throw new AssertionError(name);System.out.println(name+"=PASS");}
    public static void main(String[] args)throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(args[0]);DbStore store=new DbStore(db);
        try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(null,"200",System.currentTimeMillis())){
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY)");
            ResearchPlatformStoreR929.createSchema(db);String h=V51ChecksumEngine.sha256("SYNTHETIC_TRIAL"),bundle=ModelBundleRuntimeR932.current().hash,schema=V51ActionMetaFeatureEngine.featureManifestHash(),code=SourceManifestR815.SOURCE_TREE_SHA256;
            db.execSQL("INSERT INTO "+ResearchPlatformStoreR929.TABLE_BUNDLE+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{bundle,"fixture","Binance1-PK10",schema,code,h,"TEST","RSA",1,6,100,"100","101","ACTIVE","SYNTHETIC_ONLY",1,1,"",h});
            db.execSQL("INSERT INTO "+ResearchPlatformStoreR929.TABLE_TRIAL+"(trial_id,identity_hash,stage,status,dataset_hash,schema_hash,source_hash,hyper_hash,family_id,structural_hash,split_hash,seed,registered_at,finished_at,record_hash) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{h,h,"EVALUATE","COMPLETED",h,schema,code,h,"FIXTURE_FAMILY",h,h,1,1,1,h});
            List<CertificateBundleServiceR929.Certificate> certs=new ArrayList<>();int i=0;
            for(CertificateBundleServiceR929.Consumer consumer:CertificateBundleServiceR929.Consumer.values()){
                CertificateBundleServiceR929.CandidateKey key=new CertificateBundleServiceR929.CandidateKey(consumer,"FIXTURE","FIXTURE_FAMILY","candidate_"+i,h);
                CertificateBundleServiceR929.Evaluation evaluation=CertificateBundleServiceR929.evaluate(UnifiedCertificateHarnessR932.input(360,2));
                CertificateBundleServiceR929.Certificate cert=new CertificateBundleServiceR929.Certificate(key,evaluation.level,CertificateBundleServiceR929.CertificateState.ACTIVE,bundle,schema,AtomCaptureR932.graphHash(store),CertificateDependenciesR932.modelHash(),CertificateDependenciesR932.component("POLICY"),CertificateDependenciesR932.component("CALIBRATION"),DomainRouterR929.installedSchemaHash(),h,CertificateDependenciesR932.selectionHash(),code,"101","300",2+i++,.05,evaluation.gates,"SYNTHETIC_ONLY");
                if(!ResearchPlatformStoreR929.putCertificate(store,cert,h))throw new AssertionError("FIXTURE_CERT");certs.add(cert);
            }
            UnifiedResearchChoiceR932.freeze(store,"200");int selected=0;
            for(CertificateBundleServiceR929.Consumer consumer:CertificateBundleServiceR929.Consumer.values())if(UnifiedResearchChoiceR932.selected(store,"200",consumer)!=null)selected++;
            check(selected==1,"THREE_VALID_CONSUMERS_EXACTLY_ONE_FROZEN_WINNER");
            CertificateBundleServiceR929.Certificate first=certs.get(0);
            UnifiedResearchChoiceR932.freeze(store,"200");check(UnifiedResearchChoiceR932.selected(store,"200",first.candidate.consumer).id.equals(first.candidate.candidateId),"SAME_ISSUE_RETRY_PRESERVES_EARLIER_CERTIFICATE_ORDER");
            ResearchPlatformStoreR929.revoke(store,first.certificateHash,"FIXTURE_DEPENDENCY_CHANGED");
            UnifiedResearchChoiceR932.freeze(store,"200");selected=0;
            for(CertificateBundleServiceR929.Consumer consumer:CertificateBundleServiceR929.Consumer.values())if(UnifiedResearchChoiceR932.selected(store,"200",consumer)!=null)selected++;
            check(selected==0,"REVOKED_WINNER_CANNOT_RESELECT_ANOTHER_CONSUMER_SAME_ISSUE");
            try(ModelBundleRuntimeR932.Scope next=ModelBundleRuntimeR932.open(null,"201",System.currentTimeMillis())){
                UnifiedResearchChoiceR932.freeze(store,"201");selected=0;for(CertificateBundleServiceR929.Consumer consumer:CertificateBundleServiceR929.Consumer.values())if(UnifiedResearchChoiceR932.selected(store,"201",consumer)!=null)selected++;
                check(selected==1,"NEXT_ISSUE_MAY_USE_NEXT_VALID_FROZEN_CERTIFICATE");
            }
            String report=UnifiedResearchChoiceR932.report(store);check(report.contains("observed=")&&report.contains("required=")&&report.contains("source="),"UI_EXPLAINS_MEASURED_GATE_THRESHOLD_AND_EVIDENCE_SOURCE");
            System.out.println("R932_UNIFIED_CHOICE_PASS|scope=SYNTHETIC_CERTIFICATES_REAL_SQLITE_NOT_EFFECT");
        }finally{db.shutdown();}
    }
}
