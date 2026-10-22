package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/** Immutable hypothesis registration; archived/paused candidates never leave the denominator. */
public final class ResearchFamilyRegistryR925 {
    private ResearchFamilyRegistryR925() {}
    public static void register(SQLiteDatabase db,AlgorithmGenomeR908 g,String origin){
        StringBuilder topology=new StringBuilder();
        for(AlgorithmGenomeR908.Insn op:g.scoreProgram)topology.append(op.op.name()).append(',');
        topology.append('|');for(AlgorithmGenomeR908.Insn op:g.updateProgram)topology.append(op.op.name()).append(',');
        String family=origin.equals("LEGACY_BACKFILL")?"LEGACY_UNREGISTERED_SINGLE_FAMILY":V51ChecksumEngine.sha256(topology.toString());
        db.execSQL("INSERT OR IGNORE INTO v57_hypothesis_r925 VALUES (?,?,?,?,?)",new Object[]{g.genomeId,family,g.structuralHash,origin,System.currentTimeMillis()});
    }
    public static void reconcile(final DbStore store){
        final ArrayList<Object[]> missing=new ArrayList<Object[]>();
        Cursor c=store.readable().rawQuery("SELECT g.genome_id,g.structural_hash FROM v57_algorithm_genome_r908 g LEFT JOIN v57_hypothesis_r925 h ON h.genome_id=g.genome_id WHERE h.genome_id IS NULL LIMIT 256",null);
        try{while(c.moveToNext())missing.add(new Object[]{c.getString(0),"LEGACY_UNREGISTERED_SINGLE_FAMILY",c.getString(1),"LEGACY_BACKFILL",System.currentTimeMillis()});}finally{c.close();}
        if(missing.isEmpty())return;
        store.coordinatorR36().runWrite("R925_HYPOTHESIS_BACKFILL",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){for(Object[] row:missing)db.execSQL("INSERT OR IGNORE INTO v57_hypothesis_r925 VALUES (?,?,?,?,?)",row);}});
    }
    public static double adjustedQ(DbStore store,String genome,int hit,int n){
        Cursor pending=store.readable().rawQuery("SELECT 1 FROM v57_algorithm_genome_r908 g LEFT JOIN v57_hypothesis_r925 h ON h.genome_id=g.genome_id WHERE h.genome_id IS NULL LIMIT 1",null);
        try{if(pending.moveToFirst())return 1;}finally{pending.close();}
        Cursor c=store.readable().rawQuery("SELECT family_id,COUNT(*) FROM v57_hypothesis_r925 GROUP BY family_id",null);
        LinkedHashMap<String,Integer> families=new LinkedHashMap<String,Integer>();
        try{while(c.moveToNext())families.put(c.getString(0),c.getInt(1));}finally{c.close();}
        c=store.readable().rawQuery("SELECT family_id FROM v57_hypothesis_r925 WHERE genome_id=?",new String[]{genome});
        String family="";try{if(c.moveToFirst())family=c.getString(0);}finally{c.close();}
        Integer members=families.get(family);if(members==null)return 1;
        // Equal alpha budget across families, then each preregistered hypothesis in the family.
        // This controls FWER conservatively, hence bounds FDR; it is not described as BH.
        return Math.min(1,AlgorithmEvidencePolicyR912.fdrQ(hit,n,1)*families.size()*members.intValue());
    }
    public static String report(DbStore store){return "HIERARCHICAL_FWER_BOUND_R925\n旧候选不能事后按表现挑族，全部进入旧注册族；新结构在注册时冻结家族。归档不删假设。\n"+RuntimeObservabilityR925.query(store,"SELECT family_id,registration_origin,COUNT(*) hypothesis_count FROM v57_hypothesis_r925 GROUP BY family_id,registration_origin",false);}

public static String reportSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store){return "HIERARCHICAL_FWER_BOUND_R925\n旧候选不能事后按表现挑族，全部进入旧注册族；新结构在注册时冻结家族。归档不删假设。\n"+RuntimeObservabilityR925.querySnapshotCP4(report, store,"SELECT family_id,registration_origin,COUNT(*) hypothesis_count FROM v57_hypothesis_r925 GROUP BY family_id,registration_origin",false);}
}
