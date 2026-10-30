package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.FormalReceiptSchemaR29;
import com.ethk3.novaonepro.ReleaseManifestR926;
import java.util.*;

/** Real R974/F05 DDL and nonempty historical RP copies. Root's formal tables stay empty; no fake trades or
 * formal Root decisions are created. The archived synthetic RP rows never become new REAL evidence. */
public final class RetainedFormalProductionFixtureR29 {
    private RetainedFormalProductionFixtureR29(){}
    static void require(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    static long n(SQLiteDatabase db,String sql){return MigrationCopyR29.scalar(db,sql,null);}
    static final String REAL="r974_exact_real_receipt",PROB="research_exact_probability_f05";
    public static void seed(SQLiteDatabase root,SQLiteDatabase rp)throws Exception{
        CriticalInputProductionTriggerFixtureR29.tx(root,()->FormalReceiptSchemaR29.ensureRoot(root));
        CriticalInputProductionTriggerFixtureR29.tx(rp,()->{
            if(!ResearchRouteR29.isResearchDatabase(rp)){rp.execSQL(ResearchRouteR29.OWNER_DDL);rp.execSQL("INSERT INTO r29_research_owner VALUES(1,'READY','SYNTHETIC','SYNTHETIC',1,1)");}
            rp.execSQL(LegacyDomainUpgradeR29.DDL);rp.execSQL("INSERT OR IGNORE INTO r29_legacy_domain_owner VALUES(1,'FULL',1,?,1)",new Object[]{StatsR27.sha256("synthetic-old-FULL-domain")});
            for(String source:RetainedFormalCopiesR29.SOURCES)rp.execSQL(RetainedFormalCopiesR29.tableSql(root,source));
            String hash=StatsR27.sha256("synthetic-retained-only");
            rp.execSQL("INSERT INTO "+REAL+" VALUES('202610300001',71,?,?,?,?,?,?,?,1)",new Object[]{"legacy\u0000🚀",hash,hash,hash,hash,hash,hash});
            rp.execSQL("INSERT INTO "+REAL+" VALUES('202610300002',72,?,?,?,?,?,?,?,2)",new Object[]{"legacy-second",hash,hash,hash,hash,hash,hash});
            rp.execSQL("INSERT INTO "+PROB+" VALUES(?,'202610300001',73,?,?,?,?,?,?,.42,?,?,?,1,2)",new Object[]{ReleaseManifestR926.GAME_CODE,"legacy-f05",hash,hash,hash,hash,hash,hash,hash,"retained-auth"});
            for(String source:RetainedFormalCopiesR29.SOURCES)for(Object object:RetainedFormalCopiesR29.objects(root,source)){@SuppressWarnings("unchecked") List<Object> fields=(List<Object>)object;rp.execSQL((String)fields.get(2));}
        });
        // Imported old RP rows have no Root trades parent. The archive operation itself must work with normal
        // FK enforcement enabled; it never disables constraints or fabricates the missing Root parent.
        rp.execSQL("PRAGMA foreign_keys=ON");
    }
    public static Map<String,Object> exercise(SQLiteDatabase root,SQLiteDatabase rp)throws Exception{
        Map<String,Object> out=new LinkedHashMap<>();Map<String,String> ddl=new LinkedHashMap<>(),hashes=new LinkedHashMap<>();Map<String,Long> counts=new LinkedHashMap<>();Map<String,List<Object>> rootObjects=new LinkedHashMap<>();
        for(String source:RetainedFormalCopiesR29.SOURCES){String sql=RetainedFormalCopiesR29.tableSql(rp,source);ddl.put(source,sql);RetainedFormalCopiesR29.Content content=RetainedFormalCopiesR29.content(rp,MigrationCopyR29.table(rp,source,sql),Long.MAX_VALUE);hashes.put(source,content.hash);counts.put(source,content.count);rootObjects.put(source,RetainedFormalCopiesR29.objects(root,source));}
        // A surviving old cross-Root trigger must be fixed BEFORE ALTER's schema reparse.
        for(String sql:CriticalInputMirrorR29.misplacedRootTriggers(root).values())if(RetainedFormalCopiesR29.tableSql(rp,com.ethk3.novaonepro.DbHelper.TABLE_R35_OOS_LABEL)!=null)rp.execSQL(sql);
        CriticalInputProductionTriggerFixtureR29.tx(rp,()->RetainedFormalCopiesR29.isolate(rp));
        boolean kept=true,audited=true,gone=true,unchanged=true;
        for(String source:RetainedFormalCopiesR29.SOURCES){String retained=RetainedFormalCopiesR29.retainedName(source),sql=RetainedFormalCopiesR29.tableSql(rp,retained);RetainedFormalCopiesR29.Content after=RetainedFormalCopiesR29.content(rp,MigrationCopyR29.table(rp,retained,sql),Long.MAX_VALUE);kept&=after.count==counts.get(source)&&after.hash.equals(hashes.get(source));gone&=RetainedFormalCopiesR29.tableSql(rp,source)==null;
            try(Cursor c=rp.rawQuery("SELECT original_ddl,original_ddl_hash,row_count,typed_content_hash FROM main."+RetainedFormalCopiesR29.AUDIT+" WHERE source_name=?",new String[]{source})){audited&=c.moveToFirst()&&ddl.get(source).equals(c.getString(0))&&StatsR27.sha256(ddl.get(source)).equals(c.getString(1))&&counts.get(source)==c.getLong(2)&&hashes.get(source).equals(c.getString(3));}
            unchanged&=RetainedFormalCopiesR29.tableSql(root,source)!=null&&n(root,"SELECT count(*) FROM main."+source)==0&&rootObjects.get(source).equals(RetainedFormalCopiesR29.objects(root,source));
        }
        boolean immutable=false;try{rp.execSQL("DELETE FROM main."+RetainedFormalCopiesR29.retainedName(REAL));}catch(RuntimeException expected){immutable=String.valueOf(expected.getMessage()).contains("RETAINED_FORMAL_COPY_IMMUTABLE");}
        CriticalInputProductionTriggerFixtureR29.tx(rp,()->RetainedFormalCopiesR29.isolate(rp));
        boolean rootRefused=false;try{CriticalInputProductionTriggerFixtureR29.tx(root,()->RetainedFormalCopiesR29.isolate(root));}catch(IllegalStateException expected){rootRefused=String.valueOf(expected.getMessage()).contains("REQUIRES_RESEARCH_OWNER");}
        out.put("oldFullFormalRowsAndTypedHashesRetained",kept&&counts.get(REAL)==2&&counts.get(PROB)==1);out.put("retainedFormalOriginalDdlAndHashAudited",audited);out.put("oldFormalPhysicalNamesRemovedFromRp",gone);out.put("retainedFormalCopiesImmutableAndIdempotent",immutable&&n(rp,"SELECT count(*) FROM main."+RetainedFormalCopiesR29.AUDIT)==2);out.put("rootFormalSchemaRowsAndTriggersUnchanged",unchanged&&rootRefused);out.put("noFakeTradesCreatedForFormalArchival",RetainedFormalCopiesR29.tableSql(root,"trades")==null&&RetainedFormalCopiesR29.tableSql(rp,"trades")==null);
        for(Map.Entry<String,Object> value:out.entrySet())require(Boolean.TRUE.equals(value.getValue()),value.getKey());return out;
    }
    public static boolean conflictRollsBack(SQLiteDatabase root,SQLiteDatabase conflicting)throws Exception{
        seed(root,conflicting);conflicting.execSQL("CREATE TABLE "+RetainedFormalCopiesR29.retainedName(PROB)+"(unowned INTEGER)");boolean rejected=false;
        try{CriticalInputProductionTriggerFixtureR29.tx(conflicting,()->RetainedFormalCopiesR29.isolate(conflicting));}catch(IllegalStateException expected){rejected=String.valueOf(expected.getMessage()).contains("NAME_CONFLICT");}
        return rejected&&n(conflicting,"SELECT count(*) FROM main."+REAL)==2&&n(conflicting,"SELECT count(*) FROM main."+PROB)==1&&RetainedFormalCopiesR29.tableSql(conflicting,RetainedFormalCopiesR29.retainedName(REAL))==null&&RetainedFormalCopiesR29.tableSql(conflicting,RetainedFormalCopiesR29.AUDIT)==null;
    }
}
