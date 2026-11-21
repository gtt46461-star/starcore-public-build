package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.TreeMap;
import java.util.TreeSet;

public final class RawClosure {
    public static void main(String[] args) throws Exception {
        File root=new File(args[0]), snapshot=new File(root,"snapshot.db"), bodies=new File(root,"bodies"), scratch=new File(root,"verify");
        if (!scratch.mkdir()) throw new AssertionError("SCRATCH");
        TreeMap<String,Long> observed=new TreeMap<>();
        try(SQLiteDatabase db=SQLiteDatabase.openDatabase(snapshot.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            CheckpointClosureR324 closure=new CheckpointClosureR324(scratch,bodies,()->true,(file,growth)->{})) {
            try(Cursor check=db.rawQuery("PRAGMA quick_check",null)){if(!check.moveToFirst()||!"ok".equals(check.getString(0)))throw new AssertionError("DB_INTEGRITY");}
            TreeSet<String> roots=new TreeSet<>();CheckpointReferencesR26.forEachHashR324(db,roots::add);
            CheckpointClosureR324.Result counts=closure.traverse(sink->CheckpointReferencesR26.forEachHashR324(db,sink),(hash,size)->{
                if(observed.put(hash,size)!=null)throw new AssertionError("DUPLICATE_HASH");
            });
            if(counts.objects!=observed.size()||counts.objects!=Long.parseLong(args[1])||counts.bytes!=Long.parseLong(args[2]))throw new AssertionError("CLOSURE_COUNTS");
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            for(java.util.Map.Entry<String,Long> e:observed.entrySet())digest.update((e.getKey()+" "+e.getValue()+"\n").getBytes(StandardCharsets.US_ASCII));
            StringBuilder sha=new StringBuilder();for(byte v:digest.digest())sha.append(String.format(java.util.Locale.ROOT,"%02x",v&255));
            if(!sha.toString().equals(args[3]))throw new AssertionError("CLOSURE_SET");
            System.out.println("RAW_CLOSURE roots="+roots.size()+" objects="+counts.objects+" bytes="+counts.bytes+" collection="+sha+" sqlite=ok");
        }
    }
}
