package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.sql.DriverManager;
import java.util.*;

public final class RetainedFormalCopiesR29HostTest {
    static SQLiteDatabase open(Path p)throws Exception{return new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+p));}
    static boolean authority(Path rp,Path root,boolean missing)throws Exception{
        try(SQLiteDatabase reader=open(rp)){
            reader.execSQL("ATTACH DATABASE ? AS realtime",new Object[]{root.toString()});for(String source:RetainedFormalCopiesR29.SOURCES)reader.execSQL("CREATE TEMP VIEW "+source+" AS SELECT * FROM realtime."+source);reader.execSQL("PRAGMA query_only=1");
            if(missing){try{RetainedFormalProductionFixtureR29.n(reader,"SELECT count(*) FROM r974_exact_real_receipt");return false;}catch(RuntimeException expected){return String.valueOf(expected.getMessage()).contains("no such table");}}
            boolean rootOnly=RetainedFormalProductionFixtureR29.n(reader,"SELECT count(*) FROM r974_exact_real_receipt")==0&&RetainedFormalProductionFixtureR29.n(reader,"SELECT count(*) FROM research_exact_probability_f05")==0;
            boolean archiveRejected=false;try{RetainedFormalCopiesR29.isolate(reader);}catch(IllegalStateException expected){archiveRejected=String.valueOf(expected.getMessage()).contains("PHYSICAL_RP_WRITER");}return rootOnly&&archiveRejected;
        }
    }
    public static void main(String[]args)throws Exception{
        Path dir=Files.createTempDirectory("r29-retained-formal-");Map<String,Object> result=new LinkedHashMap<>();
        try(SQLiteDatabase root=open(dir.resolve("root.db"));SQLiteDatabase rp=open(dir.resolve("rp.db"));SQLiteDatabase conflict=open(dir.resolve("conflict.db"));SQLiteDatabase missing=open(dir.resolve("missing-root.db"))){
            CriticalInputProductionTriggerFixtureR29.exercise(root,rp);RetainedFormalProductionFixtureR29.seed(root,rp);
            result.put("rootAuthorityViewShadowsNonemptyOldRpPhysicalCopy",authority(dir.resolve("rp.db"),dir.resolve("root.db"),false));result.put("missingRootAuthorityFailsClosedDespiteNonemptyOldRpCopy",authority(dir.resolve("rp.db"),dir.resolve("missing-root.db"),true));
            result.putAll(RetainedFormalProductionFixtureR29.exercise(root,rp));result.put("retainedNameConflictRollsBackBothFormalTablesAndAudit",RetainedFormalProductionFixtureR29.conflictRollsBack(root,conflict));result.put("rootAuthorityViewStillWinsAfterArchival",authority(dir.resolve("rp.db"),dir.resolve("root.db"),false));
            for(Map.Entry<String,Object> e:result.entrySet()){if(!Boolean.TRUE.equals(e.getValue()))throw new AssertionError(e.getKey());System.out.println("R29_RETAINED_FORMAL|"+e.getKey()+"|OK");}
            result.put("scope","HOST_REAL_SQLITE_PRODUCTION_FORMAL_DDL_NOT_NATURAL_OR_ANDROID");result.put("result","PASS_WITH_STATED_HOST_SCOPE");Path out=Paths.get(args[0]);Files.createDirectories(out.toAbsolutePath().getParent());Files.write(out,(JsonR27.write(result)+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));System.out.println("R29_RETAINED_FORMAL|ALL_PASS|NONEMPTY_OLD_FULL_COPIES_RETAINED_NOT_NEW_ROOT_REAL");
        }finally{try(java.util.stream.Stream<Path> walk=Files.walk(dir)){walk.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}}
    }
}
