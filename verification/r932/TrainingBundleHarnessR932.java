package com.ethk3.novaonepro;
import java.nio.file.*;
import java.io.*;
import java.lang.reflect.Field;
import android.database.sqlite.SQLiteDatabase;

/** Tests the Python-trained package against the unmodified phone import and parameter owners. */
public final class TrainingBundleHarnessR932 {
    public static void main(String[] args)throws Exception {
        if(args.length==1){Files.write(Paths.get(args[0]),RuntimeModelContractR932.json().getBytes("UTF-8"));return;}
        Field root=ModelBundleRuntimeR932.class.getDeclaredField("filesRoot");root.setAccessible(true);root.set(null,Files.createTempDirectory("r932-trained-import-").toFile());
        ModelBundleRuntimeR932.trustKey(Files.readAllBytes(Paths.get(args[1])));
        SQLiteDatabase db=new SQLiteDatabase(args[2]);try {
            DbStore store=new DbStore(db);String hash;
            try(InputStream in=new FileInputStream(args[0])){hash=ModelBundleRuntimeR932.importArchive(store,in);}
            try(ModelBundleRuntimeR932.Scope capture=ModelBundleRuntimeR932.open(store,args.length>3?args[3]:"202609100200",System.currentTimeMillis())) {
                if(!hash.equals(ModelBundleRuntimeR932.current().hash))throw new AssertionError("TRAINED_PACKAGE_NOT_ACTIVE");
                if(ModelBundleRuntimeR932.number().TRAINED_MODEL==null||ModelBundleRuntimeR932.gate().TRAINED_MODEL==null||ModelBundleRuntimeR932.ranker().TRAINED_MODEL==null||ModelBundleRuntimeR932.calibration().TRAINED_MODEL==null)throw new AssertionError("TRAINED_COMPONENT_NOT_LOADED");
                if(ModelBundleRuntimeR932.number().PROVEN||ModelBundleRuntimeR932.calibration().PROVEN||ModelBundleRuntimeR932.policy().MODEL_GATE_PASSED)throw new AssertionError("SYNTHETIC_TRAINING_MUST_NOT_CLAIM_EFFECT_PROOF");
                if(args.length>3&&ModelBundleRuntimeR932.predictability().TRAINED_MODEL==null)throw new AssertionError("FROZEN_SELECTOR_TRUST_MODEL_NOT_LOADED");
                if(!Double.isFinite(ModelBundleRuntimeR932.ranker().TRAINED_MODEL.score(new double[96],.3)))throw new AssertionError("TRAINED_SCORE");
            }
            System.out.println("R932_TRAINING_TO_SIGNED_PHONE_LOADER_PASS|scope=SYNTHETIC_TRAINING_HOST_SQLITE_NOT_ANDROID_DEVICE");
        }finally{db.shutdown();}
    }
}
