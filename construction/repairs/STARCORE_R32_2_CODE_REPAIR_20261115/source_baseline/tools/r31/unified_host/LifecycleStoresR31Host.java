package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.*;
import java.io.File;
import java.sql.*;
import java.util.*;

/** Real four-store declarations and dynamic CAS guards, using the production
 * lifecycle owner on native SQLite. Minimal CAS publisher tables are explicit
 * fixtures; this is not an Android service or end-to-end model training claim. */
public final class LifecycleStoresR31Host {
    static int assertions;
    static void need(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static class Db extends SQLiteDatabase {
        int ddl;Db(Connection c){super(c);}
        @Override public void execSQL(String sql,Object[] args){if(sql.matches("(?is)^\\s*(CREATE|ALTER|DROP)\\b.*"))ddl++;super.execSQL(sql,args);}
    }
    static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),"scalar");return c.getLong(0);}}
    static LifecycleSchemaR31.Result unit(Db db)throws Exception{
        db.ddl=0;db.beginTransaction();try{
            LifecycleSchemaR31.Result result=LifecycleSchemaR31.advanceWithinMigration(db);
            need(db.ddl<=1,"actual production declarations install at most one DDL per transaction");db.setTransactionSuccessful();return result;
        }finally{db.endTransaction();}
    }
    static int finish(Db db)throws Exception{for(int i=0;i<256;i++)if(unit(db).ready)return i+1;throw new AssertionError("actual declarations did not finish");}
    // Explicit schema-shape fixtures cover every declared producer column and
    // the full protected-name inventory. They do not stand in for real assets.
    static void allProducerShapes(Db db){
        Set<String> sources=new LinkedHashSet<>(ResearchRetentionR30.PROTECTED_TABLES);
        Collections.addAll(sources,"r26_live_model","r26_candidate","r26_warm_initialization","r26_expert_experiment","r26_legacy_runtime","r26_member_replay","r26_prepared_policy","r26_replay_output_chunk","r29_model_checkpoint","r29_legacy_model_checkpoint","r30_model_artifact","r30_atomnet_dataset");
        Set<String> tables=new LinkedHashSet<>();for(String source:sources){tables.add(source);tables.add("r30_keep_"+source);}tables.add("r26_checkpoint_artifact");
        for(String table:tables){
            LinkedHashMap<String,String> columns=new LinkedHashMap<>();columns.put("owner","TEXT");columns.put("__r30_id","INTEGER");columns.put("policy_hash","TEXT");columns.put("state","BLOB");
            for(String column:new String[]{"artifact_ref","artifact","weights","payload","artifact_hash","state_ref","weights_ref","dataset_ref","issue","decision_key","original_allow","explain_allow","direction","original_reason","first_blocker","recorded_at"})columns.put(column,"BLOB");
            for(int i=0;i<16;i++)columns.put("c"+i,"BLOB");
            db.execSQL("CREATE TABLE IF NOT EXISTS \""+table+"\"(shape_fixture INTEGER)");
            Set<String> existing=new HashSet<>();try(Cursor c=db.rawQuery("PRAGMA table_info(\""+table+"\")",null)){while(c.moveToNext())existing.add(c.getString(1));}
            for(Map.Entry<String,String> column:columns.entrySet())if(!existing.contains(column.getKey()))db.execSQL("ALTER TABLE \""+table+"\" ADD COLUMN \""+column.getKey()+"\" "+column.getValue());
        }
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");File path=new File(args[0]);
        try(Db db=new Db(DriverManager.getConnection("jdbc:sqlite:"+path))){
            SchemaDomainCoordinatorR926.ensureInfrastructure(db);
            db.execSQL(ModelArtifactR29.DDL);
            for(String ddl:StrategySchemaSU2.DDL)if(ddl.startsWith("CREATE TABLE IF NOT EXISTS su2_evolution_state("))db.execSQL(ddl);
            for(String ddl:SchemaR27.DDL)if(ddl.startsWith("CREATE TABLE IF NOT EXISTS r27_chart_cache("))db.execSQL(ddl);
            // Real source declarations are prerequisites of the declared narrow
            // indexes. Do not replace this path with a zero-table object count.
            for(String ddl:QualificationInboxR29.DDL)if(ddl.startsWith("CREATE TABLE IF NOT EXISTS r29_qual_conflict(")||ddl.startsWith("CREATE TABLE IF NOT EXISTS r29_qual_receipt("))db.execSQL(ddl);
            for(String ddl:ModelSurfaceStoreR30.DDL)if(ddl.startsWith("CREATE TABLE IF NOT EXISTS r30_model_call("))db.execSQL(ddl);
            for(String ddl:AtomNetPublicationR31.DDL)if(ddl.startsWith("CREATE TABLE IF NOT EXISTS r31_atomnet_publication("))db.execSQL(ddl);
            db.beginTransaction();try{SchemaDomainCoordinatorR926.migrateR31MainlineChoice(db);SchemaDomainCoordinatorR926.migrateR31QualificationReceipts(db);SchemaDomainCoordinatorR926.migrateR31AtomNetPublication(db);SchemaDomainCoordinatorR926.migrateR31ModelPublication(db);db.setTransactionSuccessful();}finally{db.endTransaction();}

            db.execSQL("CREATE TABLE protected_critical_fixture(id INTEGER PRIMARY KEY,value TEXT NOT NULL)");db.execSQL("INSERT INTO protected_critical_fixture VALUES(1,'ORIGINAL')");
            db.execSQL("CREATE TABLE r26_live_model(policy_hash TEXT PRIMARY KEY,state BLOB)");
            need(!LifecycleSchemaR31.ready(db),"real missing Stores are not ready");int units=0,critical=0;
            for(;units<256;units++){
                LifecycleSchemaR31.Result r=unit(db);
                try(Connection other=DriverManager.getConnection("jdbc:sqlite:"+path);Statement s=other.createStatement()){s.executeUpdate("INSERT INTO protected_critical_fixture VALUES("+(100+units)+",'BETWEEN_REAL_STORE_DDL_UNITS')");critical++;}
                if(r.ready)break;
            }
            need(units<256&&LifecycleSchemaR31.ready(db),"actual Store declarations and quota publish independently READY");
            HypothesisStoreR31.requireSchemaR31(db);need(CheckpointGcStoreR31.ready(db),"real GC storage exists");need(KeepMigrationStoreR31.ready(db),"real keep storage exists");
            SQLiteDatabase.trace.clear();need(LifecycleSchemaR31.ready(db),"business readiness reads all actual Store definitions");
            need(SQLiteDatabase.trace.stream().noneMatch(x->x.contains("|execSQL|")),"actual readiness performs zero DML/DDL");
            // The source did not exist at the first READY boundary. It must get
            // guards when it appears, and cannot reuse an early READY receipt.
            db.execSQL("CREATE TABLE r26_candidate(policy_hash TEXT PRIMARY KEY,state BLOB)");
            need(!LifecycleSchemaR31.ready(db),"later CAS source invalidates old dynamic contract");
            need(finish(db)>=3,"late source needs its two individually committed reference guards");
            String artifact=String.format(Locale.ROOT,"%064x",1);byte[] reference=("R26CAS1:"+artifact).getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            db.execSQL("INSERT INTO r31_gc_artifact VALUES(?,1,0,'DELETING',1,0)",new Object[]{artifact});
            for(String table:new String[]{"r26_live_model","r26_candidate"}){
                boolean denied=false;try{db.execSQL("INSERT INTO "+table+" VALUES('candidate',?)",new Object[]{reference});}catch(android.database.sqlite.SQLiteException expected){denied=true;}
                need(denied,"real publisher INSERT loses to a committed DELETING claim");
            }
            db.execSQL("UPDATE r31_gc_artifact SET state='LIVE' WHERE artifact_hash=?",new Object[]{artifact});
            db.execSQL("INSERT INTO r26_candidate VALUES('candidate',?)",new Object[]{reference});
            db.execSQL("UPDATE r31_gc_artifact SET state='DELETING' WHERE artifact_hash=?",new Object[]{artifact});
            boolean denied=false;try{db.execSQL("UPDATE r26_candidate SET state=? WHERE policy_hash='candidate'",new Object[]{reference});}catch(android.database.sqlite.SQLiteException expected){denied=true;}
            need(denied,"real publisher UPDATE also refuses DELETING identity");
            String name="r31_gc_ref_r26_candidate_state_insert";
            db.execSQL("DROP TRIGGER \""+name+"\"");need(!LifecycleSchemaR31.ready(db),"old READY missing a real publisher guard is not accepted");
            finish(db);need(LifecycleSchemaR31.ready(db),"owner repairs only the actual missing guard");
            need(scalar(db,"SELECT count(*) FROM protected_critical_fixture WHERE value='ORIGINAL'")==1,"all original critical fixture rows survive");
            need(critical>1,"critical native commits occurred between real declaration units");
            allProducerShapes(db);need(!LifecycleSchemaR31.ready(db),"later full producer/archive shapes rearm the exact dynamic contract");finish(db);
            need(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31AtomNetPublicationContract())&&SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31ModelPublicationContract()),"real publication domains remain MATCH after every declared index/GC guard delta");
            need(MainlineFreezeProofR31.ready(db),"actual typed freeze table/index/two immutable guards are installed");
            int generic=KeepArchiveRetirementR31.guardDefinitionsR31(db).length,gc=CheckpointGcStoreR31.referenceGuards(db).length;
            int gap=GapReferenceR31.guardDefinitionsR31(db).length;
            int objects=StartupCheckpointIdentityR31.ddlR31().length+GapReferenceR31.DDL.length+gap+KeepMigrationStoreR31.DDL.length+KeepMismatchReferenceR31.DDL.length+KeepArchiveRetirementR31.DDL.length+MainlineFreezeProofR31.DDL.length+CheckpointGcStoreR31.DDL.length+HypothesisStoreR31.ddlR31().length+gc+KeepMismatchReferenceR31.cutoverDefinitionsR31(db).length+generic;
            need(generic==57&&gc==86&&gap==3&&GapReferenceR31.DDL.length==5&&objects<=256,"actual full declared inventory stays under original 256 bound: "+objects);
            need(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31MainlineChoiceContract()),"exact lifecycle lookup index delta preserves authenticated P05 domain");
            db.execSQL("DROP INDEX idx_r31_mainline_choice_fence");need(!LifecycleSchemaR31.ready(db),"missing owned lookup index invalidates lifecycle READY");finish(db);need(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31MainlineChoiceContract()),"missing exact lookup index is recovered with authenticated prior presence state");
            db.execSQL("CREATE INDEX foreign_mainline_lookup ON r31_mainline_choice(status)");db.execSQL("DROP INDEX idx_r31_mainline_choice_fence");boolean unknownLookup=false;try{unit(db);}catch(DatabaseCoordinator.SchemaPendingR31 expected){unknownLookup=true;}need(unknownLookup,"known missing index cannot launder an unknown mainline attachment");need(scalar(db,"SELECT count(*) FROM sqlite_master WHERE name='idx_r31_mainline_choice_fence'")==0,"unknown attachment refusal rolls back known-index install");db.execSQL("DROP INDEX foreign_mainline_lookup");finish(db);need(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31MainlineChoiceContract()),"exact owner remains recoverable after unknown attachment removed");
            for(String source:new String[]{"r29_qual_conflict","r30_model_call"}){Set<String> owners=new TreeSet<>();for(java.lang.reflect.Method method:SchemaDomainCoordinatorR926.class.getDeclaredMethods())if(java.lang.reflect.Modifier.isPublic(method.getModifiers())&&java.lang.reflect.Modifier.isStatic(method.getModifiers())&&method.getParameterCount()==0&&method.getReturnType()==SchemaDomainCoordinatorR926.Contract.class){SchemaDomainCoordinatorR926.Contract c=(SchemaDomainCoordinatorR926.Contract)method.invoke(null);if(Arrays.asList(c.objects).contains(source))owners.add(c.domain);}need(owners.isEmpty(),"actual complete declared domain intersection is empty for "+source+":"+owners);}
            db.execSQL("DROP INDEX idx_r31_mainline_model_publication");finish(db);need(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31AtomNetPublicationContract()),"publication lookup missing-unit repair retains the authenticated actual domain");
            String stagedGuard="r31_gc_ref_r31_model_publication_item_c0_insert";db.execSQL("DROP TRIGGER \""+stagedGuard+"\"");finish(db);need(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31ModelPublicationContract()),"one of 32 publication reference guards recovers from the durable exact-core proof");
            db.execSQL("CREATE INDEX foreign_model_publication_attachment ON r31_model_publication_item(kind)");db.execSQL("DROP TRIGGER \""+stagedGuard+"\"");boolean unknownPublication=false;try{unit(db);}catch(DatabaseCoordinator.SchemaPendingR31 expected){unknownPublication=true;}need(unknownPublication,"unknown publication attachment cannot launder a known missing GC guard");need(scalar(db,"SELECT count(*) FROM sqlite_master WHERE name='"+stagedGuard+"'")==0,"unknown publication drift rolls back the attempted owned guard");db.execSQL("DROP INDEX foreign_model_publication_attachment");finish(db);need(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31ModelPublicationContract()),"publication exact-core remains recoverable without arbitrary restamping");
            String lateGuard=KeepArchiveRetirementR31.guardName("r26_gap_receipt");db.execSQL("DROP TRIGGER \""+lateGuard+"\"");need(!LifecycleSchemaR31.ready(db),"old READY cannot hide a missing generic archive guard");finish(db);
            db.execSQL("DROP TRIGGER r31_mainline_freeze_no_update");need(!LifecycleSchemaR31.ready(db),"old READY cannot hide a typed freeze guard loss");finish(db);
            db.execSQL("DROP TRIGGER r31_mainline_freeze_no_update");db.execSQL("CREATE TRIGGER r31_mainline_freeze_no_update BEFORE UPDATE ON r31_mainline_freeze BEGIN SELECT RAISE(ABORT,'different literal'); END");
            boolean drift=false;try{unit(db);}catch(DatabaseCoordinator.SchemaPendingR31 expected){drift=true;}need(drift,"same-name typed freeze drift is refused without an accepted receipt");
            System.out.println("LIFECYCLE_DECLARATION_BOUND objects="+objects+" limit=256 gapDefinitions="+(GapReferenceR31.DDL.length+gap)+" gcReferenceGuards="+gc+" genericArchiveGuards="+generic+" actualDeclarations=true schemaShapeFixtures=true");
            System.out.println("LIFECYCLE_STORES_NATIVE_PASS assertions="+assertions+" initialUnits="+(units+1)+" criticalCommits="+critical+" actualStoreDeclarations=true dynamicLateSource=true deletingInsertAndUpdateRefused=true scope=NATIVE_OWNER_STORE_DECLARATIONS_NOT_ANDROID_OR_FULL_GC");
        }
    }
}
