package com.ethk3.novaonepro.strategyv2;
import com.ethk3.novaonepro.*;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.*;
import java.nio.file.*;

/** The production StatFs API boundary with actual host filesystem capacity. */
public final class WarmCapacityHostR31 {
    static final class PathDb extends SQLiteDatabase {final String value;PathDb(String value){super(null);this.value=value;}public String getPath(){return value;}}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");Path file=Paths.get(args[0]);Files.deleteIfExists(file);try(WarmAbiHostR31.World w=new WarmAbiHostR31.World(file,"warm-candidates")){
            Method check=ModelPublicationR31.class.getDeclaredMethod("checkFreeFloorR31",DbStore.class);check.setAccessible(true);long linux=Files.getFileStore(file.getParent()).getUsableSpace(),androidTransport=new android.os.StatFs(file.getParent().toString()).getAvailableBytes();WarmAbiHostR31.need(linux>=268435456L&&androidTransport>=268435456L,"ACTUAL_LINUX_AVAILABLE_CAPACITY");check.invoke(null,w.store);
            for(String value:new String[]{null,"/","/workspace/.r31-absent-capacity-fixture/db"}){android.database.sqlite.SQLiteOpenHelper.hostDatabase=new PathDb(value);boolean refused=false;try{check.invoke(null,w.store);}catch(InvocationTargetException expected){refused=FailureDispositionR31.classify(expected.getCause()).disposition==FailureDispositionR31.Disposition.DEPENDENCY;}finally{android.database.sqlite.SQLiteOpenHelper.hostDatabase=w.db;}WarmAbiHostR31.need(refused,"UNKNOWN_CAPACITY_TYPED_REJECTION:"+value);}
            System.out.println("RESULT {\"scope\":\"ACTUAL_LINUX_FILESTORE_TRANSPORT_NOT_ANDROID_STORAGE_SLO\",\"linuxUsableBytes\":"+linux+",\"statFsTransportBytes\":"+androidTransport+",\"unchangedFloor\":268435456}");WarmAbiHostR31.setupWarm(w);SQLiteDatabase.scenario="NEW_STATFS_WARM_CAPACITY";WarmCandidateR26.step(w.store,()->w.claim,new BudgetTokenSU1(60000,10000000,()->false));WarmProtocolHostR31.invariant(w);System.out.println("PASS "+WarmAbiHostR31.assertions+" STATFS_TYPED_CAPACITY_AND_COMPLETE_WARM_PUBLISH");
        }
    }
}
