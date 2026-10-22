package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
import java.util.Arrays;
public final class R947MolecularToolWorldHarness {
  private static void check(boolean x,String code){ if(!x) throw new AssertionError(code); System.out.println(code+"=PASS"); }
  public static void main(String[] args)throws Exception{
    SQLiteDatabase db=new SQLiteDatabase(args[0]);
    try{
      db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY,dirty INTEGER)");
      db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+" VALUES('202609130700',0)");
      AtomicToolWorldR943.schema(db);
      double[] counts=new double[]{48,48,48,48,48,18,20,22,24,26};
      byte[] packed=AtomicToolWorldR943.pack(counts);
      int[] indices=new int[]{0,20,40,60,80,100,120,140,160,180};
      for(int idx:indices){
        String atom=GenesisSchemaRegistryR915.atoms().get(idx).id;
        db.execSQL("INSERT OR REPLACE INTO "+AtomicToolWorldR943.QUALITY+" VALUES(?,?,240,0,?,72.0,26.0,0,0,2,2,'RESEARCH_PARENT','202609130699')",
          new Object[]{idx,atom,packed});
      }
      int[] fallback=new int[]{0,20,40,60,80,100,120,140,160,180,200,220};
      MolecularToolWorldR947.Plan plan;
      db.beginTransaction();try{plan=MolecularToolWorldR947.creationPlan(db,fallback,123456789L);db.setTransactionSuccessful();}finally{db.endTransaction();}
      check(plan.proposalHash.length()==64,"MOLECULE_PROPOSAL_HASHED");
      check(plan.atomIndices.length>=GenesisAssetGenomeR915.MIN_ATOMS,"MOLECULE_PLAN_MIN_ATOMS");
      check(plan.operator.length()>0,"MOLECULE_OPERATOR_BOUND");
      for(int idx:plan.atomIndices) check(GenesisSchemaRegistryR915.atoms().get(idx).productionScoreEligible(),"MOLECULE_PARENT_PRODUCTION_ATOM_"+idx);
      check(plan.trace().contains("MOLECULE="),"MOLECULE_TRACE_TO_GENESIS");
      System.out.println("PLAN="+Arrays.toString(plan.atomIndices)+"|"+plan.trace());
      System.out.println("R947_MOLECULAR_TOOL_WORLD_PASS");
    }finally{db.shutdown();}
  }
}
