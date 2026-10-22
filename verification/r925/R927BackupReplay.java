package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;

/** Offline materializer runner. Caller must supply a disposable SQLite backup copy. */
public final class R927BackupReplay {
    public static void main(String[] args) throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);
        DbStore store=new DbStore(db);
        try {
            ArrayList<String> targets=new ArrayList<String>();
            Cursor c=db.rawQuery("SELECT target_issue FROM "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+" WHERE status<>'COMPLETED' ORDER BY target_issue",null);
            try {while(c.moveToNext())targets.add(c.getString(0));}finally{c.close();}
            for(String target:targets) {
                for(int slice=0;slice<1500;slice++) {
                    long start=System.nanoTime();
                    DeferredEvidenceWriterR36.WriteResult result=DeferredEvidenceWriterR36.writeOne(store,target,"R927_OFFLINE_COPY");
                    long elapsed=(System.nanoTime()-start)/1000000L;
                    System.out.println(target+"\t"+slice+"\t"+result.success+"\t"+result.status.replace('\t',' ').replace('\n',' ')+"\t"+elapsed);
                    if(!result.success||DeferredEvidenceWriterR36.COMPLETED.equals(result.status))break;
                }
            }
        } finally {db.shutdown();}
    }
}
