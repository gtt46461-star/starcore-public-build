package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
import java.util.Random;
public final class ResearchPruningHarnessR965 {
 static int checks;static void check(boolean x,String label){if(!x)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
 public static void main(String[] args)throws Exception {
  SQLiteDatabase db=new SQLiteDatabase(args[0]);try {
   DbStore store=DevelopmentTrainingHarnessR959.init(db);String base="202609200001",first=TargetIssueResolver.next(base).targetIssue;
   DevelopmentTrainingHarnessR959.draw(db,base,new Random(965));AlgorithmGenomeR908 poor=AlgorithmGenomeR908.seed(96501,1,"FIXTURE"),healthy=AlgorithmGenomeR908.seed(96502,1,"FIXTURE"),protectedG=AlgorithmGenomeR908.seed(96503,1,"FIXTURE");
   db.beginTransaction();try {
    for(AlgorithmGenomeR908 g:new AlgorithmGenomeR908[]{poor,healthy,protectedG}){AlgorithmInventionLabR908.registerDirected(db,g,base,first,"PRUNING_TEST","SYNTHETIC_OUTCOMES");ResearchLifecycleR932.enroll(db,"algorithm:"+g.genomeId,g.structuralHash,first,"C");}
    ResearchPruningR965.observe(db,poor.genomeId,first,0,"synthetic-record",SourceManifestR815.SOURCE_TREE_SHA256);
    check(DevelopmentTrainingHarnessR959.n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key LIKE 'r965_research_pruning_%'")==0,"NO_RETROSPECTIVE_CONTRACT_AFTER_LABEL");
    for(AlgorithmGenomeR908 g:new AlgorithmGenomeR908[]{poor,healthy,protectedG})ResearchPruningR965.register(db,g.genomeId,first);
    db.execSQL("UPDATE "+ResearchLifecycleR932.TABLE+" SET state='SHADOW' WHERE candidate_key=?",new Object[]{"algorithm:"+protectedG.genomeId});
    String issue=first;
    for(int i=0;i<120;i++) {
     for(AlgorithmGenomeR908 g:new AlgorithmGenomeR908[]{poor,healthy,protectedG})ResearchPruningR965.observe(db,g.genomeId,issue,g==healthy?1:0,"synthetic-proof-"+i,SourceManifestR815.SOURCE_TREE_SHA256);
     ResearchPruningR965.observe(db,poor.genomeId,issue,0,"duplicate",SourceManifestR815.SOURCE_TREE_SHA256);
     if(i==118)check(DevelopmentTrainingHarnessR959.n(db,"SELECT COUNT(*) FROM "+ResearchLifecycleR932.TABLE+" WHERE state='QUALITY_PRUNED'")==0,"NO_SHORT_WINDOW_OR_DUPLICATE_COUNT_PRUNING");
     issue=TargetIssueResolver.next(issue).targetIssue;
    }
    check(DevelopmentTrainingHarnessR959.n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id='"+poor.genomeId+"' AND status='PRUNED_QUALITY_R932'")==1,"FAILED_FIXED_120_RESEARCH_PARENT_STOPS_SCHEDULING");
    check(DevelopmentTrainingHarnessR959.n(db,"SELECT COUNT(*) FROM "+ResearchLifecycleR932.TABLE+" WHERE candidate_key='algorithm:"+healthy.genomeId+"' AND state='NURSERY'")==1,"RETAINED_PARENT_NOT_PROMOTED_FROM_RESEARCH_TEST");
    check(DevelopmentTrainingHarnessR959.n(db,"SELECT COUNT(*) FROM "+ResearchLifecycleR932.TABLE+" WHERE candidate_key='algorithm:"+protectedG.genomeId+"' AND state='SHADOW'")==1,"RESEARCH_RETIREMENT_CANNOT_OVERRIDE_CERTIFIED_STAGE");
    check(DevelopmentTrainingHarnessR959.n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"RESEARCH_PRUNING_HAS_NO_CERTIFICATE_AUTHORITY");
    db.setTransactionSuccessful();
   }finally{db.endTransaction();}
   System.out.println("R965_RESEARCH_PRUNING_PASS|checks="+checks+"|SYNTHETIC_FIXED_ENDPOINT_CONTRACT_NOT_LIVE_EFFECT");
  }finally{db.shutdown();}
 }
}
