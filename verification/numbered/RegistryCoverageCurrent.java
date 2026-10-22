package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

/** Exports every actual compiled Rule and verifies coverage after real runtime initialization. */
public final class RegistryCoverageCurrent {
    public static void main(String[] args)throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);
        try {
            db.execSQL("PRAGMA foreign_keys=ON");
            if(args.length>3 && "runtime-diagnosis".equals(args[3]))HostProductR974.store(db).ensureDiagnosisLoopTablesNow();
            JSONArray rules=new JSONArray();Set<String> registered=new TreeSet<>();
            for(RetentionTableRegistryCP12.Rule rule:RetentionTableRegistryCP12.ALL_INSTALLED){
                if(!registered.add(rule.table))throw new AssertionError("duplicate rule "+rule.table);
                if(RetentionTableRegistryCP12.require(rule.table)!=rule)throw new AssertionError("lookup identity "+rule.table);
                String[] bound=rule.args("202601010001",1);
                try(Cursor ignored=db.rawQuery("SELECT rowid FROM \""+rule.table+"\" WHERE "+rule.eligibleSql+" ORDER BY rowid LIMIT 128",bound)){
                    JSONObject item=new JSONObject();item.put("table",rule.table);item.put("policy",rule.policy);
                    item.put("issue_column",rule.issueColumn);item.put("time_column",rule.timeColumn);
                    item.put("eligible_sql",rule.eligibleSql);item.put("bound_args",new JSONArray(Arrays.asList(bound)));
                    item.put("archive_and_prune",rule.archiveAndPrune());item.put("predicate_executed",true);rules.put(item);
                }
            }
            Set<String> actual=new TreeSet<>();
            try(Cursor c=db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name<>'android_metadata'",null)){while(c.moveToNext())actual.add(c.getString(0));}
            Set<String> missing=new TreeSet<>(registered);missing.removeAll(actual);
            Set<String> extra=new TreeSet<>(actual);extra.removeAll(registered);
            JSONObject receipt=new JSONObject();receipt.put("registry_hash",RetentionTableRegistryCP12.INSTALLED_HASH);
            receipt.put("original_registry_hash",RetentionTableRegistryCP12.HASH);
            receipt.put("original_rule_count",RetentionTableRegistryCP12.ALL.size());
            receipt.put("all_actual_compiled_rules",rules);receipt.put("unregistered_business_tables",new JSONArray(extra));
            receipt.put("missing_business_tables",new JSONArray(missing));receipt.put("actual_business_table_count",actual.size());
            receipt.put("status",extra.isEmpty()&&missing.isEmpty()?"PASS":"FAIL");
            receipt.put("scope","Real current Java Rule lookup/args and each exact SQL predicate on host SQLite. Runtime-diagnosis mode invokes actual DbStore initialization; no schema mirror or fixed-result cursor. Framework android_metadata excluded explicitly; no customer row values exported.");
            Files.writeString(Path.of(args[2]),receipt.toString(2)+'\n');
            if(!extra.isEmpty()||!missing.isEmpty())throw new AssertionError("RETENTION_REGISTRY_COVERAGE extra="+extra+" missing="+missing);
            System.out.println("PASS|current compiled rules="+rules.length()+"|all predicates executed|actual business table coverage");
        }finally{db.shutdown();}
    }
}
