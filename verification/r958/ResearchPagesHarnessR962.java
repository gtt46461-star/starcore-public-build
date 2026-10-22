package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import com.ethk3.novaonepro.r952.*;
import java.util.*;

/** Production compound SELECT and immutable page cursor against SQLite; no Android UI claim. */
public final class ResearchPagesHarnessR962 {
    static int checks;
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
    public static void main(String[] args)throws Exception {
        final SQLiteDatabase db=new SQLiteDatabase(args[0]);
        try{
            DevelopmentTrainingHarnessR959.init(db);
            db.beginTransaction();try{
                DirectedResearchCreationR932.schema(db);AtomicToolWorldR943.schema(db);ResearchMainlineContributionAuditR922.ensureSchema(db);
                for(int i=0;i<55;i++)ResearchLifecycleR932.enrollR952(db,String.format(java.util.Locale.US,"r952:page-%03d",i),V51ChecksumEngine.sha256("page"+i),"202609200001");
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            DevelopmentTrainingHarnessR959.draw(db,"202609200001",new Random(962));
            ResearchPageRepositoryR952 repository=new ResearchPageRepositoryR952(new ResearchPageRepositoryR952.ReadPort(){
                public List<ResearchPageRepositoryR952.Row> read(CancellationSignal cancel){return ResearchPageRepositoryR952.readDatabase(db,cancel);}
            });
            long writes=DevelopmentTrainingHarnessR959.n(db,"SELECT total_changes()");
            ResearchPageRepositoryR952.PageResult first=repository.load("evolution","","","",30,new CancellationSignal());
            check(first.status.equals("READY")&&first.total==55&&first.rows.size()==30,"PRODUCTION_PAGE_SQL_READS_ALL_55_LIFECYCLE_ROWS");
            check(first.asOfIssue.equals("202609200001"),"PAGE_HAS_SQL_SNAPSHOT_DRAW_WATERMARK");
            db.execSQL("DELETE FROM "+ResearchLifecycleR932.TABLE+" WHERE candidate_key='r952:page-054'");
            ResearchPageRepositoryR952.PageResult second=repository.load("evolution",first.snapshotId,first.nextKey,"",30,new CancellationSignal());
            check(second.rows.size()==25&&second.total==55&&second.nextKey.isEmpty(),"PAGINATION_RETAINS_SNAPSHOT_DESPITE_RETENTION");
            Set<String> ids=new HashSet<String>();for(ResearchPageRepositoryR952.Row row:first.rows)ids.add(row.key);for(ResearchPageRepositoryR952.Row row:second.rows)ids.add(row.key);
            check(ids.size()==55,"KEYSET_PAGES_HAVE_NO_DUPLICATION_OR_OMISSION");
            for(String page:new String[]{"deep","creation","atoms"}){
                ResearchPageRepositoryR952.PageResult p=repository.load(page,first.snapshotId,"","",30,new CancellationSignal());
                check(p.status.equals("EMPTY")&&p.snapshotId.equals(first.snapshotId),"FOUR_PAGES_SHARE_SNAPSHOT_EMPTY_IS_NOT_PASS:"+page);
            }
            check(DevelopmentTrainingHarnessR959.n(db,"SELECT total_changes()")==writes+1,"PAGE_AND_PAGING_PERFORM_NO_BUSINESS_WRITES");
            boolean immutable=false;try{first.rows.clear();}catch(UnsupportedOperationException ok){immutable=true;}check(immutable,"RESULT_DTO_LIST_IS_IMMUTABLE");
            check(repository.load("evolution",first.snapshotId,"","page-054",30,new CancellationSignal()).rows.size()==1,"FILTER_USES_SAME_SNAPSHOT");
            CancellationSignal cancelled=new CancellationSignal();cancelled.cancel();
            check(repository.load("atoms","","","",30,cancelled).error.equals("QUERY_CANCELLED"),"CANCELLED_REQUEST_IS_ERROR_NOT_EMPTY");
            check(repository.load("bad","","","",30,new CancellationSignal()).error.equals("INVALID_PAGE_QUERY"),"UNKNOWN_PAGE_REJECTED");
            db.execSQL("ALTER TABLE r952_world_atom RENAME TO held_world_atom");
            check(repository.load("atoms","","","",30,new CancellationSignal()).status.equals("ERROR"),"MISSING_SCHEMA_NOT_REPORTED_AS_ZERO_RECORDS");
            db.execSQL("ALTER TABLE held_world_atom RENAME TO r952_world_atom");
            for(int i=0;i<5;i++)repository.load("atoms","","","",30,new CancellationSignal());
            check(repository.load("atoms",first.snapshotId,"","",30,new CancellationSignal()).error.equals("SNAPSHOT_EXPIRED"),"EVICTED_SNAPSHOT_REQUIRES_EXPLICIT_REFRESH");
            System.out.println("R962_RESEARCH_PAGES_PASS|checks="+checks+"|scope=REAL_SQLITE_READ_ONLY_NOT_ANDROID_UI");
        }finally{db.shutdown();}
    }
}
