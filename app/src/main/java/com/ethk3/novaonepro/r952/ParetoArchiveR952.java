package com.ethk3.novaonepro.r952;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.V51ChecksumEngine;

public final class ParetoArchiveR952 {
    private ParetoArchiveR952(){}
    public static void put(SQLiteDatabase db,String candidate,double quality,double calibration,double stability,double diversity,double latency,double memory,int evidenceN){if(evidenceN<0||!finite(quality)||!finite(calibration)||!finite(stability)||!finite(diversity)||!finite(latency)||!finite(memory))throw new IllegalArgumentException("R952_PARETO_INPUT");String hash=V51ChecksumEngine.sha256("PARETO1|"+candidate+'|'+quality+'|'+calibration+'|'+stability+'|'+diversity+'|'+latency+'|'+memory+'|'+evidenceN);db.execSQL("INSERT OR REPLACE INTO r952_pareto(candidate_id,quality,calibration,stability,diversity,latency,memory,evidence_n,score_hash,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{candidate,Double.valueOf(quality),Double.valueOf(calibration),Double.valueOf(stability),Double.valueOf(diversity),Double.valueOf(latency),Double.valueOf(memory),Integer.valueOf(evidenceN),hash,Long.valueOf(System.currentTimeMillis())});}
    private static boolean finite(double v){return !Double.isNaN(v)&&!Double.isInfinite(v);}
}
