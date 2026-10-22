package com.ethk3.novaonepro;
import com.ethk3.novaonepro.r952.*;import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.io.File;
public final class Resume{
 static String cursor(SQLiteDatabase d){try(Cursor c=d.rawQuery("SELECT material_row,material_offset,generation,state FROM research_molecule_import_checkpoint_cp12",null)){c.moveToFirst();return c.getString(0)+":"+c.getString(1)+":"+c.getString(2)+":"+c.getString(3);}}
 public static void main(String[] a)throws Exception{try(SQLiteDatabase d=new SQLiteDatabase(a[0],a[1])){DbStore s=HostProductR974.store(d);HostProductR974.set(s,"researchAppCP12",new android.content.Context(new File(a[2]),new File("app/src/main/assets")));RealtimePressureGateR811.resetForTests();
 for(int i=0;i<20;i++){String before=cursor(d),result="";ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(250);long start=System.nanoTime();try{result=ModernSeedFactoryCP12.maintain(s,System.nanoTime()+250000000L);}catch(ResearchWorkBudgetR927.Yield y){result=y.getMessage();}finally{ResearchWorkBudgetR927.leave(scope);}System.out.println(i+"|before="+before+"|after="+cursor(d)+"|result="+result+"|ms="+(System.nanoTime()-start)/1000000);}
 }}
}
