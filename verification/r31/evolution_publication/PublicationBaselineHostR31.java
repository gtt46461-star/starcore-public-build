package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Controlled persistence fixtures use the actual production programs and codec.
 * They do not assert that a trained generation or a device run has occurred. */
public final class PublicationBaselineHostR31 {
    static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getLong(0);}}
    static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static List<ModelGenomeSU2> programs(){
        List<ModelGenomeSU2> out=new ArrayList<>();
        for(ModelGenomeSU2 g:ModelEvolutionSU2.centralPrograms(true,true))out.add(g.bindBirth(StatsR27.sha256("PUBLICATION_CONTROLLED_SOURCE"),3595680));
        for(String family:FamilyRegistryR26.EXECUTABLE)out.add(FamilyRegistryR26.genome(family).bindBirth(StatsR27.sha256("PUBLICATION_CONTROLLED_SOURCE"),3595680));
        for(ModelGenomeSU2 g:new ArrayList<>(out))for(StructuralProposalR26.Proposal proposal:StructuralProposalR26.mechanismChildren(g))out.add(proposal.genome);
        LinkedHashMap<String,ModelGenomeSU2> unique=new LinkedHashMap<>();for(ModelGenomeSU2 g:out)unique.put(g.key(),g);
        require(unique.size()>=36,"ACTUAL_PROGRAM_FIXTURE_LT_36");return new ArrayList<>(unique.values());
    }
    static void schema(SQLiteDatabase db){for(String sql:StrategySchemaSU2.DDL)db.execSQL(sql);for(String sql:RuntimeSchemaR26.DDL)db.execSQL(sql);}
    public static void main(String[] args)throws Exception {
        Class.forName("org.sqlite.JDBC");Path file=Paths.get(args[0]);Files.deleteIfExists(file);
        try(SQLiteDatabase db=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+file))){schema(db);List<ModelGenomeSU2> ps=programs();ModelEvolutionSU2.State state=new ModelEvolutionSU2.State();
            for(int i=0;i<36;i++){ModelEvolutionSU2.Ranked r=new ModelEvolutionSU2.Ranked(ps.get(i),"CONTROLLED_EXISTING_KERNEL",ps.get((i+1)%36).key(),"Persistence-only fixture; no quality claim");if(i<24)state.population.add(r);else state.hallOfFame.add(r);}
            state.effectiveGenerations=3;state.kindStats.put("HMM",new double[]{17,.125});state.opStats.put("MUTATE",new double[]{11,4});state.atomPrior.put(37,new double[]{7,.75});for(int i=0;i<5000;i++)state.priorSeen.add(StatsR27.sha256("ROLLING_HISTORY_"+i));
            byte[] expected=ModelStateCodecSU2.encode(state);SQLiteDatabase.scenario="BASELINE_LEGACY_POP24_HOF12";
            db.beginTransaction();try{for(int i=0;i<24;i++)ModelStoreSU2.saveGenome(db,state.population.get(i),"baseline",3595680,.1+i,.2+i,"RESEARCH",1234567);for(int i=0;i<12;i++)ModelStoreSU2.saveGenome(db,state.hallOfFame.get(i),"baseline",3595680,.3+i,.4+i,"HALL_OF_FAME",1234567);ModelStoreSU2.saveEvolution(db,"baseline",state,1234567);db.setTransactionSuccessful();}finally{db.endTransaction();}
            require(scalar(db,"SELECT count(*) FROM su2_model_genome")==36,"GENOMES_PRESERVED");require(scalar(db,"SELECT count(*) FROM su2_lineage")==36,"LINEAGE_PRESERVED");require(Arrays.equals(expected,ModelStateCodecSU2.encode(ModelStoreSU2.loadEvolution(db))),"CODEC_NUMERICAL_BYTES_ROUNDTRIP");
            Map<String,Object> frame=SQLiteDatabase.frames.get(SQLiteDatabase.frames.size()-1);long sql=((Number)frame.get("sqlCalls")).longValue();System.out.println("RESULT "+JsonR27.write(new LinkedHashMap<String,Object>(){{put("scope","ACTUAL_MODELSTORE_AND_CODEC_CONTROLLED_POP24_HOF12_NOT_TRAINING_NOT_ANDROID");put("sqlCalls",sql);put("population",state.population.size());put("hallOfFame",state.hallOfFame.size());put("rollingHistory",state.priorSeen.size());put("codecBytes",expected.length);put("baselineBudgetFailure",sql>64);}}));
            require(sql>200,"BASELINE_EXPECTED_MORE_THAN_200_ACTUAL_SQL");throw new AssertionError("BASELINE_FAIL_REPRODUCED_R31_PUBLICATION_BUDGET:"+sql);
        }
    }
}
