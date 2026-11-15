package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.CheckpointGcStoreR31;
import com.ethk3.novaonepro.strategyv2.HypothesisStoreR31;
import com.ethk3.novaonepro.strategyv2.KeepMigrationStoreR31;
import com.ethk3.novaonepro.strategyv2.KeepMismatchReferenceR31;
import com.ethk3.novaonepro.strategyv2.KeepArchiveRetirementR31;
import com.ethk3.novaonepro.strategyv2.GapReferenceR31;
import com.ethk3.novaonepro.strategyv2.StartupCheckpointIdentityR31;
import com.ethk3.novaonepro.strategyv2.MainlineFreezeProofR31;
import com.ethk3.novaonepro.strategyv2.ImprovementStoreR31;
import java.util.*;
import java.util.regex.*;

/** The existing migration writer installs one lifecycle object per transaction.
 * Business producers/readers never call this installer. Every completed receipt is
 * checked against the real object again; a missing object rearms its own unit,
 * while an unknown definition is refused without deleting or restamping it. */
public final class LifecycleSchemaR31 {
    private LifecycleSchemaR31() {}
    public static final String DOMAIN="RESEARCH_R31_LIFECYCLE";
    private static final Pattern DECLARATION=Pattern.compile(
            "(?is)^\\s*CREATE\\s+(?:UNIQUE\\s+)?(TABLE|INDEX|TRIGGER)\\s+IF\\s+NOT\\s+EXISTS\\s+[\\\"`\\[]?([A-Za-z_][A-Za-z0-9_]*)");
    private static final int MAX_OBJECTS=256;
    public static final class Result {
        public final boolean ready,committedUnit;
        public final String object;
        Result(boolean ready,boolean committed,String object){this.ready=ready;committedUnit=committed;this.object=object;}
    }
    static final class Definition {
        final String kind,name,sql,unit;
        Definition(String sql){
            Matcher m=DECLARATION.matcher(sql);
            if(!m.find())throw new IllegalArgumentException("R31_LIFECYCLE_UNDECLARED_DDL");
            kind=m.group(1).toLowerCase(Locale.ROOT);name=m.group(2);this.sql=sql;
            if(!(name.startsWith("r31_startup_checkpoint_")||name.startsWith("r31_gap_")||name.startsWith("idx_r31_gap_")||name.startsWith("r31_keep_")||name.startsWith("r31_gc_")||name.startsWith("r31_hypothesis_")||name.startsWith("idx_r31_keep_")||name.startsWith("idx_r31_gc_")||name.startsWith("idx_r31_hypothesis_")||name.startsWith("r31_mainline_")||name.startsWith("idx_r31_mainline_")||name.startsWith("r31_improvement_")||name.startsWith("idx_r31_improvement_")))
                throw new IllegalArgumentException("R31_LIFECYCLE_FOREIGN_OBJECT");
            unit="OBJECT:"+name+":"+V51ChecksumEngine.sha256(canonical(sql));
        }
        boolean present(SQLiteDatabase db){
            try(Cursor c=db.rawQuery("SELECT type,sql FROM sqlite_master WHERE name=?",new String[]{name})){
                if(!c.moveToFirst())return false;
                if(!kind.equals(c.getString(0))||!canonical(sql).equals(canonical(c.getString(1))))
                    throw new DatabaseCoordinator.SchemaPendingR31(DOMAIN,"UNKNOWN_DEFINITION:"+name);
                return true;
            }
        }
    }
    /** Only unquoted SQL layout is ignored. Quoted identifiers and literal bytes
     * remain exact, including doubled quotes and words such as IF NOT EXISTS. */
    static String canonical(String sql){
        if(sql==null)return "";
        String value=sql.trim();if(value.endsWith(";"))value=value.substring(0,value.length()-1);
        StringBuilder out=new StringBuilder();char quote=0;
        for(int i=0;i<value.length();i++){
            char c=value.charAt(i);
            if(quote!=0){out.append(c);if(c==quote){if(i+1<value.length()&&value.charAt(i+1)==quote)out.append(value.charAt(++i));else quote=0;}}
            else if(c=='\''||c=='\"'||c=='`'||c=='['){quote=c=='['?']':c;out.append(c);}
            else if(!Character.isWhitespace(c))out.append(Character.toLowerCase(c));
        }
        return out.toString().replaceFirst("^(create(?:unique)?(?:table|index|trigger))ifnotexists","$1");
    }
    private static List<Definition> definitions(SQLiteDatabase db){
        List<String> sql=new ArrayList<>();Collections.addAll(sql,KeepMigrationStoreR31.DDL);
        Collections.addAll(sql,KeepMismatchReferenceR31.DDL);
        Collections.addAll(sql,KeepArchiveRetirementR31.DDL);Collections.addAll(sql,GapReferenceR31.DDL);Collections.addAll(sql,MainlineFreezeProofR31.DDL);
        Collections.addAll(sql,CheckpointGcStoreR31.DDL);Collections.addAll(sql,HypothesisStoreR31.ddlR31());
        Collections.addAll(sql,CheckpointGcStoreR31.referenceGuards(db));
        Collections.addAll(sql,KeepMismatchReferenceR31.cutoverDefinitionsR31(db));
        Collections.addAll(sql,KeepArchiveRetirementR31.guardDefinitionsR31(db));
        Collections.addAll(sql,GapReferenceR31.guardDefinitionsR31(db));
        if(startupSourcePresentR31(db))Collections.addAll(sql,StartupCheckpointIdentityR31.ddlR31());
        Collections.addAll(sql,ImprovementStoreR31.DDL);Collections.addAll(sql,ImprovementStoreR31.sourceGuardsR31(db));
        return definitions(sql.toArray(new String[0]));
    }
    private static boolean startupSourcePresentR31(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='r29_model_checkpoint'",null)){return c.moveToFirst();}}
    static List<Definition> definitions(String[] sql){
        if(sql.length==0||sql.length>MAX_OBJECTS)throw new IllegalArgumentException("R31_LIFECYCLE_OBJECT_BOUND");
        List<Definition> out=new ArrayList<>();Set<String> names=new HashSet<>();
        for(String statement:sql){Definition d=new Definition(statement);if(!names.add(d.name))throw new IllegalArgumentException("R31_LIFECYCLE_DUPLICATE_OBJECT");out.add(d);}
        return out;
    }
    private static void requireDeclaredAttachments(SQLiteDatabase db,List<Definition> all){
        Set<String> declared=new HashSet<>();for(Definition d:all)declared.add(d.name);
        int seen=0;
        for(Definition table:all)if(table.kind.equals("table")){
            try(Cursor c=db.rawQuery("SELECT name,type,sql FROM sqlite_master WHERE tbl_name=?",new String[]{table.name})){
                while(c.moveToNext()){
                    if(++seen>MAX_OBJECTS*4)throw new DatabaseCoordinator.SchemaPendingR31(DOMAIN,"ATTACHMENT_BOUND");
                    String name=c.getString(0);
                    if(declared.contains(name))continue;
                    if("index".equals(c.getString(1))&&name.startsWith("sqlite_autoindex_")&&c.isNull(2))continue;
                    throw new DatabaseCoordinator.SchemaPendingR31(DOMAIN,"UNKNOWN_ATTACHMENT:"+table.name);
                }
            }
        }
    }
    private static SchemaDomainCoordinatorR926.Contract contract(List<Definition> definitions){
        String[] names=new String[definitions.size()];for(int i=0;i<names.length;i++)names[i]=definitions.get(i).name;
        return new SchemaDomainCoordinatorR926.Contract(DOMAIN,1,names);
    }
    private static boolean infrastructure(SQLiteDatabase db){
        try(Cursor c=db.rawQuery("SELECT count(*) FROM sqlite_master WHERE (type='table' AND name IN ('v57_schema_domain_r926','v57_schema_migration_journal_r926')) OR (type='index' AND name='idx_r926_migration_status')",null)){
            return c.moveToFirst()&&c.getInt(0)==3;
        }
    }
    /** Pure read-side dependency probe; absent/corrupt storage grants no authority. */
    public static boolean ready(SQLiteDatabase db){
        if(!infrastructure(db))return false;
        try{
            List<Definition> all=definitions(db);requireDeclaredAttachments(db,all);for(Definition d:all)if(!d.present(db))return false;
            HypothesisStoreR31.requireSchemaR31(db);
            if(startupSourcePresentR31(db)&&!StartupCheckpointIdentityR31.ready(db))return false;
            if(!ImprovementStoreR31.ready(db))return false;
            return SchemaDomainCoordinatorR926.matches(db,contract(all));
        }catch(DatabaseCoordinator.SchemaPendingR31 dependency){return false;}
    }
    /** Called only by the explicit lifecycle-schema MIGRATION task. */
    public static Result advanceWithinMigration(SQLiteDatabase db)throws Exception{
        if(!db.inTransaction())throw new IllegalStateException("R31_LIFECYCLE_MIGRATION_WRITER_REQUIRED");
        if(ready(db)){
            if(KeepMismatchReferenceR31.advanceCutoverWithinMigrationR31(db))return new Result(true,true,"KEEP_MISMATCH_REFERENCE_CUTOVER");
            if(KeepArchiveRetirementR31.advanceWithinMigrationR31(db))return new Result(true,true,"KEEP_CAS_DUPLICATE_RETIREMENT_CUTOVER");
            if(GapReferenceR31.advanceCutoverWithinMigrationR31(db))return new Result(true,true,"KEEP_GAP_REFERENCE_CUTOVER");
            return new Result(true,false,"COMPLETE");
        }
        return advance(db,definitions(db),tx->{HypothesisStoreR31.seedQuotaWithinMigrationR31(tx);if(startupSourcePresentR31(tx))StartupCheckpointIdentityR31.seedWithinMigrationR31(tx);ImprovementStoreR31.seedWithinMigrationR31(tx);});
    }
    interface Initializer {void run(SQLiteDatabase db);}
    static Result advance(SQLiteDatabase db,List<Definition> all,Initializer initialize)throws Exception{
        if(!db.inTransaction())throw new IllegalStateException("R31_LIFECYCLE_MIGRATION_WRITER_REQUIRED");
        if(!infrastructure(db))throw new DatabaseCoordinator.SchemaPendingR31(DOMAIN,"CRITICAL_JOURNAL_PREREQUISITE");
        SchemaDomainCoordinatorR926.Contract contract=contract(all);
        // Validate every existing object before touching a receipt. An unknown
        // object never becomes accepted merely because an earlier name is absent.
        requireDeclaredAttachments(db,all);
        Definition missing=null;for(Definition d:all)if(!d.present(db)&&missing==null)missing=d;
        if(missing!=null){
            final Definition unit=missing;
            if(SchemaDomainCoordinatorR926.stepComplete(db,contract,unit.unit))
                SchemaDomainCoordinatorR926.invalidateStepR31(db,contract,unit.unit,"R31_LIFECYCLE_MISSING_POSTCONDITION");
            SchemaDomainCoordinatorR926.runStep(db,contract,unit.unit,tx->{
                String critical=SchemaDomainCoordinatorR926.fingerprint(tx,SchemaDomainCoordinatorR926.criticalContract());
                if(SchemaDomainCoordinatorR926.isR31MainlineChoiceLookupIndex(unit.sql))SchemaDomainCoordinatorR926.installR31MainlineChoiceLookupIndex(tx,unit.sql);
                else if(SchemaDomainCoordinatorR926.isR31PublicationAttachment(tx,unit.sql))SchemaDomainCoordinatorR926.installR31PublicationAttachment(tx,unit.sql);
                else tx.execSQL(unit.sql);
                if(!unit.present(tx))throw new IllegalStateException("R31_LIFECYCLE_DDL_POSTCONDITION");
                if(!critical.equals(SchemaDomainCoordinatorR926.fingerprint(tx,SchemaDomainCoordinatorR926.criticalContract())))
                    throw new IllegalStateException("R31_LIFECYCLE_CHANGED_CRITICAL_SCHEMA");
            });
            SchemaDomainCoordinatorR926.markState(db,contract,"MIGRATING",unit.name,"");
            return new Result(false,true,unit.name);
        }
        // Existing objects without a unit receipt still need an authenticated
        // no-DDL receipt. Never stamp the whole domain from mere class presence.
        for(Definition d:all)if(!SchemaDomainCoordinatorR926.stepComplete(db,contract,d.unit)){
            SchemaDomainCoordinatorR926.runStep(db,contract,d.unit,tx->{if(!d.present(tx))throw new IllegalStateException("R31_LIFECYCLE_OBJECT_DISAPPEARED");});
            return new Result(false,true,d.name);
        }
        initialize.run(db);
        for(Definition d:all)if(!d.present(db))throw new IllegalStateException("R31_LIFECYCLE_FINAL_POSTCONDITION");
        if(!SchemaDomainCoordinatorR926.matches(db,contract))SchemaDomainCoordinatorR926.verifyAndStamp(db,contract);
        return new Result(true,false,"COMPLETE");
    }
}
