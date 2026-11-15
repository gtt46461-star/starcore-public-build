package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Called once in the settlement transaction; arm rewards are research allocation data only. */
public final class DirectorLiveFeedbackSU1 {
    private DirectorLiveFeedbackSU1() {}
    public static void settled(SQLiteDatabase db,long sequence){
        if(!db.inTransaction())throw new IllegalStateException("SU1_DIRECTOR_OWNER");
        try(Cursor c=db.rawQuery("SELECT p.arm,count(*),sum(s.hit) FROM su1_live_shadow s JOIN(SELECT strategy_id,min(arm) arm FROM su1_research_pool GROUP BY strategy_id)p ON p.strategy_id=s.strategy_id WHERE s.sequence_ordinal=? AND s.hit IS NOT NULL GROUP BY p.arm",new String[]{String.valueOf(sequence)})){
            while(c.moveToNext()){String arm=c.getString(0);int n=c.getInt(1),k=c.getInt(2);if(StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_live_arm_stats WHERE arm=?",arm))db.execSQL("UPDATE su1_live_arm_stats SET actions=actions+?,hits=hits+?,through_sequence=max(through_sequence,?) WHERE arm=?",new Object[]{n,k,sequence,arm});else StrategyRepositorySU1.row(db,"su1_live_arm_stats","arm",arm,"actions",n,"hits",k,"through_sequence",sequence);}
        }
    }
    public static void load(SQLiteDatabase db,ResearchDirectorPolicySU1 director){try(Cursor c=db.rawQuery("SELECT arm,actions,hits FROM su1_live_arm_stats ORDER BY arm",null)){while(c.moveToNext())director.live(c.getString(0),c.getLong(1),c.getLong(2));}}
}
