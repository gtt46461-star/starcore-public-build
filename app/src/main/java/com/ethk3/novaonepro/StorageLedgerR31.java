package com.ethk3.novaonepro;

import java.io.*;
import java.nio.channels.*;
import java.util.*;

/** Fixed-size disk observation history: 288 five-minute points and 720 hourly summaries.
 * Measurements are intervals. The ledger records missing sources explicitly and grants no deletion rights.
 */
public final class StorageLedgerR31 {
    public static final int POINTS=288,HOURS=720;public static final long INTERVAL_MS=300000L;
    private static final int MAGIC=0x52333153;private static final Object JVM_LOCK=new Object();
    private final File root,directory;
    public StorageLedgerR31(File dataRoot)throws IOException{root=dataRoot.getCanonicalFile();directory=StorageBudgetR31.inside(root,"files/r31-storage-samples");if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("STORAGE_SAMPLE_DIRECTORY");}
    public static Map<String,Object> sampleIfDue(File files,String phase,String owner,boolean force)throws IOException{return new StorageLedgerR31(files.getCanonicalFile().getParentFile()).sample(phase,owner,force);}
    /** Call from a low-priority owner outside the business writer transaction. */
    public Map<String,Object> sample(String phase,String owner,boolean force)throws IOException{
        synchronized(JVM_LOCK){try(RandomAccessFile io=new RandomAccessFile(StorageBudgetR31.inside(directory,"sampling.lock"),"rw")){
            FileLock lease;try{lease=io.getChannel().tryLock();}catch(OverlappingFileLockException busy){return waitOwner();}if(lease==null)return waitOwner();
            try{long now=System.currentTimeMillis();Point prior=latestPoint();String boot=StorageBudgetR31.boot();long mono=System.nanoTime();
                if(!force&&prior!=null&&!boot.equals("UNKNOWN")&&boot.equals(prior.boot)&&mono>=prior.endNanos&&mono-prior.endNanos<INTERVAL_MS*1000000L)return prior.map("CACHED_NOT_NEW_SAMPLE");
                StorageCensusSU2.Census census=StorageCensusSU2.measure(root);long reserved=-1;String budgetState="UNKNOWN";
                try{Map<String,Object> budget=new StorageBudgetR31(root).snapshot();reserved=((Number)budget.get("unallocatedReservationBytes")).longValue();budgetState=String.valueOf(budget.get("state"));}
                catch(IOException unavailable){budgetState="UNKNOWN:"+unavailable.getClass().getSimpleName();}
                Point point=recordWithin(census,phase,owner,now,observedUsableBytesR31(),reserved,budgetState,"ACTUAL_RUNTIME");return point.map("RECORDED");
            }finally{lease.release();}
        }}
    }
    // This metric observes currently usable physical space; it grants no allocation.
    // StorageManager allocatable bytes can include caches that have not been released.
    @android.annotation.SuppressLint("UsableSpace")
    private long observedUsableBytesR31(){return root.getUsableSpace();}
    private static Map<String,Object> waitOwner(){Map<String,Object> out=new LinkedHashMap<>();out.put("status","WAITING_SAMPLE_OWNER");out.put("physicalBytes",null);return out;}
    /** Deterministic clock fixtures are labeled by clockRole; they do not represent elapsed handset time. */
    public void record(StorageCensusSU2.Census census,String phase,String owner,long wall,long free,long reserved,String budgetState,String clockRole)throws IOException{
        synchronized(JVM_LOCK){try(RandomAccessFile io=new RandomAccessFile(StorageBudgetR31.inside(directory,"sampling.lock"),"rw");FileLock lock=io.getChannel().lock()){recordWithin(census,phase,owner,wall,free,reserved,budgetState,clockRole);}}
    }
    private Point recordWithin(StorageCensusSU2.Census census,String phase,String owner,long wall,long free,long reserved,String budgetState,String clockRole)throws IOException{
        if(census==null||phase==null||owner==null||phase.length()>128||owner.length()>160||wall<0||free<0||reserved< -1||budgetState.length()>256||clockRole.length()>64)throw new IOException("STORAGE_SAMPLE_INPUT");
        Point prior=latestPoint();Point p=new Point();p.sequence=prior==null?1:Math.addExact(prior.sequence,1);p.wall=wall;p.startNanos=census.startedMonotonicNanos;p.endNanos=census.finishedMonotonicNanos;p.pid=StorageBudgetR31.pid();p.boot=census.bootId;p.phase=phase;p.owner=owner;p.role=clockRole;p.complete=!census.truncated;p.total=census.total;p.allocated=census.allocatedBytesKnown&&!census.truncated?census.allocatedFileAndDirectoryBytes:-1;p.free=free;p.reserved=reserved;p.budgetState=budgetState;p.incomplete=String.valueOf(census.incompleteReasons);
        for(String category:StorageCensusSU2.CATEGORIES)p.categories.put(category,census.bytes.get(category));
        byte[] bytes=p.bytes();StorageBudgetR31.atomicWrite(StorageBudgetR31.inside(directory,"point-"+(p.sequence%POINTS)+".bin"),bytes);StorageBudgetR31.atomicWrite(StorageBudgetR31.inside(directory,"latest.bin"),bytes);
        long hour=wall/3600000L;File slot=StorageBudgetR31.inside(directory,"hour-"+(hour%HOURS)+".bin");Hour h=slot.isFile()?Hour.parse(StorageBudgetR31.readBounded(slot,32768)):new Hour();if(h.hour!=hour){h=new Hour();h.hour=hour;}h.add(p);StorageBudgetR31.atomicWrite(slot,h.bytes());return p;
    }
    public Map<String,Object> latest()throws IOException{Point p=latestPoint();return p==null?waitOwner():p.map("RECORDED");}
    public List<Map<String,Object>> history(int maximum)throws IOException{
        if(maximum<1||maximum>POINTS)throw new IllegalArgumentException("STORAGE_HISTORY_BOUND");List<Point> points=new ArrayList<>();for(int i=0;i<POINTS;i++){File f=StorageBudgetR31.inside(directory,"point-"+i+".bin");if(f.isFile())points.add(Point.parse(StorageBudgetR31.readBounded(f,32768)));}points.sort((a,b)->Long.compare(b.sequence,a.sequence));List<Map<String,Object>> out=new ArrayList<>();for(int i=0;i<Math.min(maximum,points.size());i++)out.add(points.get(i).map("RECORDED"));return out;
    }
    private Point latestPoint()throws IOException{File f=StorageBudgetR31.inside(directory,"latest.bin");return f.isFile()?Point.parse(StorageBudgetR31.readBounded(f,32768)):null;}
    private static final class Point {
        long sequence,wall,startNanos,endNanos,pid,total,allocated,free,reserved;String boot,phase,owner,role,budgetState,incomplete;boolean complete;final Map<String,Long> categories=new LinkedHashMap<>();
        byte[] bytes()throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){out.writeInt(MAGIC);for(long x:new long[]{sequence,wall,startNanos,endNanos,pid,total,allocated,free,reserved})out.writeLong(x);for(String x:new String[]{boot,phase,owner,role,budgetState,incomplete})out.writeUTF(x);out.writeBoolean(complete);out.writeInt(categories.size());for(Map.Entry<String,Long> e:categories.entrySet()){out.writeUTF(e.getKey());out.writeLong(e.getValue());}}return bytes.toByteArray();}
        static Point parse(byte[] bytes)throws IOException{Point p=new Point();try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){if(in.readInt()!=MAGIC)throw new IOException("STORAGE_SAMPLE_VERSION");p.sequence=in.readLong();p.wall=in.readLong();p.startNanos=in.readLong();p.endNanos=in.readLong();p.pid=in.readLong();p.total=in.readLong();p.allocated=in.readLong();p.free=in.readLong();p.reserved=in.readLong();p.boot=in.readUTF();p.phase=in.readUTF();p.owner=in.readUTF();p.role=in.readUTF();p.budgetState=in.readUTF();p.incomplete=in.readUTF();p.complete=in.readBoolean();int n=in.readInt();if(n!=StorageCensusSU2.CATEGORIES.length)throw new IOException("STORAGE_SAMPLE_CATEGORY_VERSION");for(int i=0;i<n;i++){String category=in.readUTF();if(!Arrays.asList(StorageCensusSU2.CATEGORIES).contains(category)||p.categories.put(category,in.readLong())!=null)throw new IOException("STORAGE_SAMPLE_CATEGORY");}if(in.read()!=-1)throw new IOException("STORAGE_SAMPLE_TRAILING");}return p;}
        Map<String,Object> map(String state){Map<String,Object> out=new LinkedHashMap<>();out.put("status",state);out.put("sequence",sequence);out.put("wallMs",wall);out.put("startedMonotonicNanos",startNanos);out.put("finishedMonotonicNanos",endNanos);out.put("boot",boot);out.put("pid",pid);out.put("phase",phase);out.put("owner",owner);out.put("clockRole",role);out.put("complete",complete);out.put("incompleteReasons",incomplete);out.put("regularFileBytes",total);out.put("physicalMeasurementComplete",complete&&allocated>=0);out.put("freeDiskBytes",free);out.put("unallocatedReservationBytes",reserved<0?null:reserved);out.put("budgetState",budgetState);out.put("categories",new LinkedHashMap<>(categories));out.put("allocatedBlocksBytes",allocated<0?null:allocated);out.put("protectedBytes",null);out.put("reclaimableBytes",null);out.put("method","REGULAR_FILE_LENGTH_AND_INDEPENDENT_ANDROID_ST_BLOCKS; protected/reclaimable require reference-closure proof");out.put("measurementScope","INTERVAL_NOT_ATOMIC_SNAPSHOT; DISK_NOT_PROCESS_MEMORY");return out;}
    }
    private static final class Hour {
        long hour=-1,count,completeCount,firstWall,lastWall,min=Long.MAX_VALUE,max,lastPhysical,firstPhysical;
        void add(Point p){if(count==0){firstWall=p.wall;firstPhysical=p.total;}count++;if(p.complete)completeCount++;lastWall=p.wall;lastPhysical=p.total;min=Math.min(min,p.total);max=Math.max(max,p.total);}
        byte[] bytes()throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){out.writeInt(MAGIC+1);for(long x:new long[]{hour,count,completeCount,firstWall,lastWall,min,max,firstPhysical,lastPhysical})out.writeLong(x);}return bytes.toByteArray();}
        static Hour parse(byte[] bytes)throws IOException{Hour h=new Hour();try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){if(in.readInt()!=MAGIC+1)throw new IOException("STORAGE_HOUR_VERSION");h.hour=in.readLong();h.count=in.readLong();h.completeCount=in.readLong();h.firstWall=in.readLong();h.lastWall=in.readLong();h.min=in.readLong();h.max=in.readLong();h.firstPhysical=in.readLong();h.lastPhysical=in.readLong();if(in.read()!=-1)throw new IOException("STORAGE_HOUR_TRAILING");}return h;}
    }
}
