package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbStore;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ResearchModuleRegistryR952 {
    private ResearchModuleRegistryR952() {}

    public static void reconcile(final DbStore store) {
        if (store == null) throw new IllegalArgumentException("R952_MODULE_STORE_NULL");
        final List<ResearchModuleDescriptorR952> modules = ResearchModuleLoaderR952.builtIns();
        Set<String> ids = new HashSet<String>();
        for (ResearchModuleDescriptorR952 d : modules)
            if (!ids.add(d.id)) throw new IllegalStateException("R952_DUPLICATE_MODULE:" + d.id);
        if (current(store.readable(), modules)) return;
        boolean committed=store.coordinatorForResearchR952().runAtomicWrite("R952_MODULE_RECONCILE",
                new com.ethk3.novaonepro.DatabaseCoordinator.WriteRunnable() {
            public void run(SQLiteDatabase db) {
                ResearchMigrationRegistryR952.ensure(db);
                long now = System.currentTimeMillis();
                for (ResearchModuleDescriptorR952 d : modules) {
                    db.execSQL("INSERT OR REPLACE INTO r952_module_registry"
                                    + "(module_id,revision,extension_point,owner,contract_hash,may_create_real,updated_at)"
                                    + " VALUES(?,?,?,?,?,?,?)",
                            new Object[] {d.id, d.revision, d.point.name(), d.owner,
                                    d.contractHash, Integer.valueOf(0), Long.valueOf(now)});
                }
            }
        });
        if (!committed) throw new IllegalStateException("R958_MODULE_RECONCILE_COMMIT_FAILED");
    }

    private static boolean current(SQLiteDatabase db,List<ResearchModuleDescriptorR952> modules) {
        try(Cursor table=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='r952_module_registry'",null)) {
            if(!table.moveToFirst())return false;
        }
        try (Cursor c=db.rawQuery("SELECT module_id,revision,extension_point,owner,contract_hash,may_create_real FROM r952_module_registry ORDER BY module_id",null)) {
            java.util.Map<String,ResearchModuleDescriptorR952> expected=new java.util.HashMap<String,ResearchModuleDescriptorR952>();
            for(ResearchModuleDescriptorR952 d:modules)expected.put(d.id,d);
            while(c.moveToNext()) {
                ResearchModuleDescriptorR952 d=expected.remove(c.getString(0));
                if(d==null||!String.valueOf(d.revision).equals(c.getString(1))||!d.point.name().equals(c.getString(2))
                        ||!d.owner.equals(c.getString(3))||!d.contractHash.equals(c.getString(4))||c.getInt(5)!=0)return false;
            }
            return expected.isEmpty();
        } catch(android.database.sqlite.SQLiteException notReady) { return false; }
    }

    public static boolean allResearchOnly(DbStore store) {
        if (store == null) return false;
        Cursor c = null;
        try {
            c = store.readable().rawQuery("SELECT COUNT(*) FROM r952_module_registry WHERE may_create_real<>0", null);
            return c.moveToFirst() && c.getInt(0) == 0;
        } catch (Throwable missing) { return false; }
        finally { if (c != null) c.close(); }
    }
}
