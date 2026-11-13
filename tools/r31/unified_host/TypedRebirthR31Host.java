package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import com.ethk3.novaonepro.BuildConfig;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

/** Actual ABI_REBIRTH producer compatibility; synthetic prior ABI only. No
 * historical certificate/qualification is fabricated or transferred to child. */
public final class TypedRebirthR31Host {
    static int assertions;static void need(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
    static long n(TypedFreezeRuntimeR31Fixture f,String sql,String... args){try(Cursor c=f.db.rawQuery(sql,args)){if(!c.moveToFirst())throw new AssertionError("REBIRTH_SCALAR");return c.getLong(0);}}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");try(TypedFreezeRuntimeR31Fixture f=new TypedFreezeRuntimeR31Fixture(Path.of(args[0]))){
            Field workerField=TypedFreezeRuntimeR31Fixture.class.getDeclaredField("worker");workerField.setAccessible(true);ResearchPoolR27.Worker worker=(ResearchPoolR27.Worker)workerField.get(f);
            PolicySpecR27 spec;try(Cursor q=f.db.rawQuery("SELECT spec_json FROM r27_live_shadow WHERE policy_hash=?",new String[]{f.policy})){need(q.moveToFirst(),"ORIGINAL_SPEC_EXISTS");spec=PolicySpecR27.parse(q.getString(0));}
            try(java.io.InputStream input=Files.newInputStream(Path.of("app/src/main/assets/r27/golden_512.bin"))){f.runtime.golden=SemanticAbiR27.readGolden(input,true);}
            f.runtime.goldenCache=new FeatureCacheR27(f.runtime.golden,64);f.runtime.operatorAbi=OperatorGoldenR27.fingerprints(f.runtime.goldenCache);f.runtime.operatorAbiHash=SemanticAbiR27.operatorSetHash(f.runtime.operatorAbi);
            SemanticAbiR27.Fingerprint actual=SemanticAbiR27.fingerprint(spec,1,f.runtime.goldenCache);int[] changed=actual.probRow.clone();changed[0]^=1;
            SemanticAbiR27.Fingerprint prior=new SemanticAbiR27.Fingerprint(StatsR27.sha256("HOST_SYNTHETIC_PRIOR_ABI"),changed,actual.selRow,actual.stateRow,actual.issues);
            long originalIndex=n(f,"SELECT global_index FROM r27_eprocess WHERE policy_hash=?",f.policy),originalN=n(f,"SELECT n FROM r27_eprocess WHERE policy_hash=?",f.policy);
            f.transaction(()->{
                f.db.execSQL("DELETE FROM r27_semantic_fingerprint WHERE policy_hash=?",new Object[]{f.policy});
                f.db.execSQL("INSERT INTO r27_semantic_fingerprint(policy_hash,build,fingerprint,operator_abi,rows_blob,computed_at) VALUES(?,'HOST_SYNTHETIC_PREVIOUS_BUILD',?,'HOST_PREVIOUS_OPERATOR',?,?)",new Object[]{f.policy,prior.digest,prior.rowsBlob(),System.currentTimeMillis()-1000});
            });
            f.owned(()->LiveShadowsR27.semanticCheck(f.runtime,worker));
            String child=StatsR27.sha256(f.policy+"|ABI_REBIRTH|"+BuildConfig.SOURCE_COMMIT_R26);
            Map<String,Object> bound=MainlineModelBindingR31.capture(f.db,child,spec,1,f.runtime.semanticAbi(spec),null);
            need("BOUND".equals(bound.get("state")),"ACTUAL_REBIRTH_EXECUTABLE_REMAINS_BINDABLE");need("VERIFIED_ABI_REBIRTH".equals(bound.get("experimentIdentity")),"ORIGINAL_LINEAGE_AND_CHANGED_ABI_RECEIPT_AUTHENTICATED");need(spec.policyHash().equals(bound.get("executablePolicyHash"))&&!child.equals(spec.policyHash()),"EXPERIMENT_AND_EXECUTABLE_IDENTITIES_SEPARATE");need(MainlineModelBindingR31.stillCommitted(f.db,bound),"REBIRTH_BINDING_REAL_READER_ACCEPTS_ORIGINAL_PRODUCER");
            need(n(f,"SELECT global_index FROM r27_eprocess WHERE policy_hash=?",child)>originalIndex&&n(f,"SELECT n FROM r27_eprocess WHERE policy_hash=?",child)==0,"NEW_PERMANENT_ALPHA_INDEX_AND_EMPTY_FUTURE_NOT_INHERITED");need(n(f,"SELECT n FROM r27_eprocess WHERE policy_hash=?",f.policy)==originalN,"ORIGINAL_EXPOSURE_PRESERVED");
            String unknown=StatsR27.sha256("HOST_UNKNOWN_EXPERIMENT_ID");f.transaction(()->StoreR27.insertShadow(f.db,spec,1,"HOST_UNKNOWN_ID",f.runtime.semanticAbi(spec),unknown,spec.policyHash()));
            need("WAIT_EXECUTABLE_IDENTITY".equals(MainlineModelBindingR31.capture(f.db,unknown,spec,1,f.runtime.semanticAbi(spec),null).get("state")),"ARBITRARY_ID_NOT_ADMITTED_AS_REBIRTH");
            f.owned(()->LiveShadowsR27.certify(f.runtime,worker,f.targetOrdinal-1));
            need(n(f,"SELECT count(*) FROM r27_certificate WHERE policy_hash=? AND status='NOT_ISSUED'",unknown)==1,"INVALID_ID_WAITS_WITHOUT_POISONING_REAL_CERTIFIER_BATCH");need(n(f,"SELECT count(*) FROM r27_certificate WHERE policy_hash=? AND status='ACTIVE'",child)==0,"REBIRTH_WITHOUT_NEW_FUTURE_NEVER_CERTIFIED");
        }System.out.println("TYPED_REBIRTH_R31_HOST_PASS assertions="+assertions+" actualSemanticCheck=true actualGoldenAbi=true originalExposurePreserved=true unknownIdentity=WAIT naturalQuality=NOT_PROVEN");
    }
}
