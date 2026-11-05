package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.Batch34SchemaR930;
import com.ethk3.novaonepro.ResearchMultiplicityR932;

/** One irrevocable reservation ledger across the legacy and SU1 writers, on their existing transaction. */
public final class AlphaLedgerSU1 {
    private AlphaLedgerSU1() {}
    private static boolean table(SQLiteDatabase db,String name){return StrategyRepositorySU1.exists(db,"SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",name);}
    private static void insert(SQLiteDatabase db,String id,String origin,double alpha){
        if(!db.inTransaction())throw new IllegalStateException("SU1_ALPHA_OWNER_TRANSACTION");
        try(Cursor c=db.rawQuery("SELECT origin,amount FROM su1_alpha_reservation WHERE reservation_id=?",new String[]{id})){
            if(c.moveToFirst()){if(!origin.equals(c.getString(0))||Math.abs(alpha-c.getDouble(1))>1e-15)throw new IllegalStateException("SU1_ALPHA_IDENTITY_CONFLICT");return;}
        }
        db.execSQL("INSERT INTO su1_alpha_reservation VALUES(?,?,?,?)",new Object[]{id,origin,alpha,System.currentTimeMillis()});
    }
    public static void importLegacy(SQLiteDatabase db){
        if(!table(db,"su1_alpha_reservation"))return;
        if(table(db,Batch34SchemaR930.TABLE_HYPOTHESIS))try(Cursor c=db.rawQuery("SELECT hypothesis_id,alpha FROM "+Batch34SchemaR930.TABLE_HYPOTHESIS+" ORDER BY reserved_at,hypothesis_id",null)){while(c.moveToNext())insert(db,"CP3:"+c.getString(0),"LEGACY_CP3",c.getDouble(1));}
        if(table(db,ResearchMultiplicityR932.HYPOTHESIS))try(Cursor c=db.rawQuery("SELECT hypothesis_hash,alpha FROM "+ResearchMultiplicityR932.HYPOTHESIS+" ORDER BY registered_issue,hypothesis_hash",null)){while(c.moveToNext())insert(db,"R932:"+c.getString(0),"LEGACY_R932",c.getDouble(1));}
        String legacy=HashingSU1.text("SU1_IMPORTED_LEGACY_CP3_R932_FAMILY_V1");
        if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_family_budget WHERE family_root_hash=?",legacy))db.execSQL("INSERT INTO su1_family_budget VALUES(?,?,1,?,?)",new Object[]{legacy,.025,HashingSU1.text("SU1_LEGACY_IMPORT_NO_NEW_QUALIFICATION"),System.currentTimeMillis()});
    }
    public static void reserveLegacy(SQLiteDatabase db,String hypothesis,double alpha,boolean cp3){
        if(!table(db,"su1_alpha_reservation"))return;
        importLegacy(db);insert(db,(cp3?"CP3:":"R932:")+hypothesis,cp3?"LEGACY_CP3":"LEGACY_R932",alpha);
    }
    public static double family(SQLiteDatabase db,String root){
        importLegacy(db);HashingSU1.sha(root);
        try(Cursor c=db.rawQuery("SELECT family_alpha FROM su1_family_budget WHERE family_root_hash=?",new String[]{root})){if(c.moveToFirst())return c.getDouble(0);}
        long ordinal=1;try(Cursor c=db.rawQuery("SELECT coalesce(max(family_ordinal),0)+1 FROM su1_family_budget",null)){c.moveToFirst();ordinal=c.getLong(0);}
        double alpha=.05/(double)ordinal/(ordinal+1d);
        db.execSQL("INSERT INTO su1_family_budget VALUES(?,?,?,?,?)",new Object[]{root,alpha,ordinal,HashingSU1.fields("SU1_FAMILY_REGISTRATION_V1",root,String.valueOf(ordinal)),System.currentTimeMillis()});return alpha;
    }
    public static void reserveClaim(SQLiteDatabase db,String claim,double familyAlpha,long ordinal){
        importLegacy(db);insert(db,"SU1:"+HashingSU1.sha(claim),"SU1",familyAlpha/(double)ordinal/(ordinal+1d));
    }
}
