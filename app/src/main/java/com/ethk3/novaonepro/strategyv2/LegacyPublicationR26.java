package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.util.*;
import java.util.function.Supplier;

/** Prepared private strategies publish only after their frozen generation is verified and committed. */
public final class LegacyPublicationR26 {
    private LegacyPublicationR26() {}
    public static void prepare(DbStore store,String campaign,String source,String family,String text,long[] ordinals,ModelReplaySU2.Library library)throws IOException {
        String policy=LegacyReplayStateR26.key(text),abi=ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256;
        try(Cursor c=store.readable().rawQuery("SELECT canonical_text,state,state_hash FROM r26_prepared_policy WHERE policy_hash=? AND numerical_abi=? AND snapshot_hash=?",new String[]{policy,abi,source})){
            if(c.moveToFirst()){PolicyRuntimeR26.decode(c.getString(0),CheckpointArtifactsR26.resolve(store,c.getBlob(1)),c.getString(2));return;}
        }
        LegacyReplayStateR26 bootstrap=LegacyBootstrapR26.build(text,ordinals,library);byte[] bytes=bootstrap.encode();String hash=LiveStateR26.hash(bytes),checkpoint=HashingSU1.fields("R26_PREPARED_POLICY_V1",policy,source,hash);
        try(CheckpointArtifactsR26.Prepared file=CheckpointArtifactsR26.prepareLease(store,bytes)){
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_PREPARED_POLICY_COMMIT",db->{PolicyIdentityLedgerR26.record(db,text,policy);db.execSQL("INSERT OR IGNORE INTO r26_prepared_policy VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{policy,campaign,source,family,text,file.reference(),hash,checkpoint,ordinals[0],ordinals[ordinals.length-1],abi,"READY",System.currentTimeMillis()});CheckpointManifestR26.record(db,checkpoint,policy,source,"",bootstrap.through(),file.reference());return 0;});
        }
    }
    public static long publishOne(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claims,BudgetTokenSU1 budget)throws IOException {
        final long now=System.currentTimeMillis();String[] candidate=null;byte[] reference=null;long genesis=0,through=0;
        try(Cursor c=store.readable().rawQuery("SELECT p.policy_hash,p.campaign_id,p.family_id,p.canonical_text,p.state,p.state_hash,p.checkpoint_hash,p.genesis_ordinal,p.through_ordinal FROM r26_prepared_policy p JOIN r26_legacy_campaign c ON c.campaign_id=p.campaign_id WHERE p.status='READY' AND c.status='DONE' AND p.numerical_abi=? AND NOT EXISTS(SELECT 1 FROM r26_live_model l WHERE l.policy_hash=p.policy_hash) AND NOT EXISTS(SELECT 1 FROM r26_live_model l WHERE l.family_id=p.family_id AND l.status='ACTIVE') ORDER BY p.prepared_at DESC,p.family_id,p.policy_hash LIMIT 1",new String[]{ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256})){
            if(c.moveToFirst()){candidate=new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(5),c.getString(6)};reference=c.getBlob(4);genesis=c.getLong(7);through=c.getLong(8);}
        }
        if(candidate==null)return 0;budget.checkpointOrYield();final String[] c=candidate;final byte[] state=reference;final long first=genesis,last=through;
        PolicyRuntimeR26.decode(c[3],CheckpointArtifactsR26.resolve(store,state),c[4]);if(!CurrentPolicyR26.matches(c[3],c[0]))throw new IOException("R26_PREPARED_POLICY_IDENTITY");
        return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_PRIVATE_LEGACY_PUBLICATION",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());long count=0,legacy=0,birth=last+1;
            try(Cursor n=db.rawQuery("SELECT count(*),sum(family_id LIKE 'L:%') FROM r26_live_model WHERE status='ACTIVE'",null)){if(n.moveToFirst()){count=n.getLong(0);legacy=n.getLong(1);}}if(count>=32||legacy>=8)return 0L;
            try(Cursor draw=db.rawQuery("SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(draw.moveToFirst())birth=Math.max(birth,TargetIssueResolver.sequenceOrdinalSU1(draw.getString(0))+1);}
            if(birth-first>5000)return 0L;
            try(Cursor existing=db.rawQuery("SELECT 1 FROM r26_live_model WHERE status='ACTIVE' AND family_id=?",new String[]{c[2]})){if(existing.moveToFirst())return 0L;}
            try(Cursor prepared=db.rawQuery("SELECT 1 FROM r26_prepared_policy p JOIN r26_legacy_campaign g ON g.campaign_id=p.campaign_id WHERE p.policy_hash=? AND p.checkpoint_hash=? AND g.status='DONE'",new String[]{c[0],c[5]})){if(!prepared.moveToFirst())throw new IllegalStateException("R26_PREPARED_PUBLICATION_CAS");}
            db.execSQL("INSERT INTO r26_live_model(policy_hash,canonical_text,state,state_hash,checkpoint_hash,parent_hash,through_ordinal,genesis_ordinal,born_ordinal,updated_at,family_id) VALUES(?,?,?,?,?,'',?,?,?,?,?)",new Object[]{c[0],c[3],state,c[4],c[5],last,first,birth,now,c[2]});
            if(c[3].startsWith("SU2|"))ModelStoreSU2.saveGenome(db,new ModelEvolutionSU2.Ranked(ModelGenomeSU2.parse(c[3]),"R26_LEGACY_PRIVATE","","Complete private source-bound bootstrap; independent future qualification required"),c[1],last,0,0,"RESEARCH",now);
            ModelStoreSU2.joinLeague(db,"SU2:"+c[0],"SU2",c[2]+"·独立未来影子",c[0],birth);CertificateSU2.preregister(db,c[0],birth);db.execSQL("UPDATE r26_prepared_policy SET status='PUBLISHED' WHERE policy_hash=?",new Object[]{c[0]});return 1L;});
    }
}
