package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** E04G acceptance for descendant zeroing and display/runtime contribution consistency. */
public final class ContributionQuarantineHarnessR932 {
    static int checks;
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;System.out.println(name+"=PASS");}
    static int[] atoms(){
        ArrayList<Integer> out=new ArrayList<Integer>();
        for(int g=0;g<9&&out.size()<8;g++)for(int i=g*20;i<(g+1)*20;i++)
            if(GenesisSchemaRegistryR915.atoms().get(i).productionScoreEligible()){out.add(Integer.valueOf(i));break;}
        int[] a=new int[out.size()];for(int i=0;i<a.length;i++)a[i]=out.get(i).intValue();return a;
    }
    static GenesisAssetGenomeR915 genome(){return new GenesisAssetGenomeR915("AF01",1,"","E04G_CHILD","202609100001",GenesisAssetGenomeR915.OutputType.GRAPH_FIELD,atoms(),AlgorithmGenomeR908.seed(4047L,1,"E04G"));}
    static void quarantine(SQLiteDatabase db,int index,String issue){
        GenesisSchemaRegistryR915.AtomSpec atom=GenesisSchemaRegistryR915.atoms().get(index);
        db.execSQL("INSERT OR REPLACE INTO "+AtomCaptureR932.QUALITY+" VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{atom.id,40,40,.5,1.0,100L,10L,issue,"QUARANTINED","E04G_FIXTURE"});
    }
    static boolean contains(int[] x,int v){for(int a:x)if(a==v)return true;return false;}
    static V51Models.PositionAction action(String checksum,int rank){return new V51Models.PositionAction(1,1,2,3,4,.3,.4,.01,.1,.2,.8,new double[96],"fp",rank,checksum);}
    static boolean blocked(DbStore store,GenesisAssetGenomeR915 g){
        return GenesisMoleculeTraceR932.blockedByQuarantine(g,true,AtomCaptureR932.quarantined(store));
    }
    public static void main(String[] args)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(args[0]);try{
            DbStore store=new DbStore(db);AtomCaptureR932.schema(db);
            GenesisAssetGenomeR915 g=genome();
            check(!blocked(store,g),"NO_QUARANTINE_ALLOWS_DEPENDENCY_GATE");

            int explicit=g.atomIndices[0];quarantine(db,explicit,"202609100100");
            check(blocked(store,g),"EXPLICIT_PARENT_QUARANTINE_BLOCKS_EXISTING_DESCENDANT_NEXT_CALL");

            db.execSQL("DELETE FROM "+AtomCaptureR932.QUALITY);
            int fixed=-1;for(int d:GenesisAtomDependencyR932.dependencies(g,true))if(!contains(g.atomIndices,d)){fixed=d;break;}
            check(fixed>=0,"FIXED_OR_SHAPED_DEPENDENCY_EXISTS_OUTSIDE_EXPLICIT_GENOME");
            quarantine(db,fixed,"202609100101");
            check(blocked(store,g),"FIXED_OR_SHAPED_PARENT_QUARANTINE_ALSO_BLOCKS_DESCENDANT");

            V51Models.PositionAction[] original=new V51Models.PositionAction[K3ActionSpaceEngine.TOTAL_ACTIONS];
            original[0]=action("ORIGINAL_RANK1",1);
            CertifiedResearchMixerR915.Result zero=new CertifiedResearchMixerR915.Result(false,original,original[0],"ATOM_QUARANTINED_DESCENDANT_ZERO",null,0.0d,"");
            CertifiedResearchMixerR915.assertZeroContributionInvariant(zero,original);
            check(!zero.applied&&zero.influenceWeight==0.0d&&zero.actions==original&&zero.rankOne==original[0]&&zero.contributionHash.length()==0,"ZERO_DISPLAY_EQUALS_ZERO_RUNTIME_CONTRIBUTION_AND_UNCHANGED_RANK1");

            boolean hiddenRankChange=false;
            V51Models.PositionAction[] changed=original.clone();changed[0]=action("CHANGED_RANK1",1);
            CertifiedResearchMixerR915.Result forged=new CertifiedResearchMixerR915.Result(false,changed,changed[0],"FORGED_ZERO",null,0.0d,"");
            try{CertifiedResearchMixerR915.assertZeroContributionInvariant(forged,original);}catch(IllegalStateException expected){hiddenRankChange=true;}
            check(hiddenRankChange,"NEGATIVE_DISPLAY_ZERO_CANNOT_HIDE_CHANGED_RANKING");

            boolean hiddenApplied=false;
            CertifiedResearchMixerR915.Result forgedApplied=new CertifiedResearchMixerR915.Result(true,original,original[0],"FORGED_APPLIED_ZERO",null,0.0d,"x");
            try{CertifiedResearchMixerR915.assertZeroContributionInvariant(forgedApplied,original);}catch(IllegalStateException expected){hiddenApplied=true;}
            check(hiddenApplied,"NEGATIVE_ZERO_WEIGHT_CANNOT_CLAIM_APPLIED_CONTRIBUTION");
            System.out.println("R932_E04G_DESCENDANT_ZERO_PASS|checks="+checks+"|scope=PRODUCTION_TYPED_MOLECULE_GUARD_REAL_SQLITE_HOST_NO_FUTURE_EFFECT");
        }finally{db.shutdown();}
    }
}
