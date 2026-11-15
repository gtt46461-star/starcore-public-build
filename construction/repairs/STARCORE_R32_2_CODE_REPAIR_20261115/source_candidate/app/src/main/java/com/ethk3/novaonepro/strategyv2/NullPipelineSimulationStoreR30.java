package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Android adapter for the portable synthetic Q08 engine, using the existing cache and single writer. */
public final class NullPipelineSimulationStoreR30 {
    private NullPipelineSimulationStoreR30() {}
    public static NullPipelineSimulationR30 load(SQLiteDatabase ro,String source) {
        try(Cursor c=ro.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id='r30' AND key=?",new String[]{"q08-state:"+source})){
            if(!c.moveToFirst())return new NullPipelineSimulationR30(source);
            NullPipelineSimulationR30 state=NullPipelineSimulationR30.restore(c.getString(0));
            if(!source.equals(state.sourceCommit()))throw new IllegalStateException("R30_Q08_SOURCE_MISMATCH");return state;
        }
    }
    public static void checkpoint(ResearchWriteQueueR27.Batch batch,NullPipelineSimulationR30 state,long at) {
        String checkpoint=state.snapshot(),summary=JsonR27.write(state.summary());
        batch.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('r30',?,?,?)","q08-state:"+state.sourceCommit(),checkpoint,at);
        batch.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('r30','q08',?,?)",summary,at);
    }
}
