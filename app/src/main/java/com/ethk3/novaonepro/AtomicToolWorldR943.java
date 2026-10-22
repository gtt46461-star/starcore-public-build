package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.util.*;

/** Atomic observations -> prequential feature tools -> neural group learner -> creation parents.
 * These are research inputs. Feature utility does not issue a production certificate. */
public final class AtomicToolWorldR943 {
    public static final String CONTEXT="atomic_tool_context_r943",QUALITY="atomic_tool_quality_r943",MODEL="atomic_tool_network_r943";
    public static final String VERSION="R943_ATOMIC_TOOLS_165_MASKED_GROUP7_V1";
    private AtomicToolWorldR943(){}
    static void schema(SQLiteDatabase db){
        if(TransactionSchemaCacheR941.ready(db,"AtomicToolWorldR943"))return;
        db.execSQL("CREATE TABLE IF NOT EXISTS "+CONTEXT+"(target_issue TEXT PRIMARY KEY,base_issue TEXT NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,features BLOB NOT NULL,probabilities BLOB NOT NULL,network_probability REAL NOT NULL,ablation BLOB NOT NULL,network_hash TEXT NOT NULL,trained_until TEXT NOT NULL,record_hash TEXT NOT NULL,hit INTEGER NOT NULL DEFAULT -1,settled_at INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r943_atomic_pending ON "+CONTEXT+"(hit,target_issue)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+QUALITY+"(atom_index INTEGER PRIMARY KEY,atom_id TEXT NOT NULL,n INTEGER NOT NULL,missing INTEGER NOT NULL,counts BLOB NOT NULL,gain_sum REAL NOT NULL,gain_sq REAL NOT NULL,block_n INTEGER NOT NULL,block_gain REAL NOT NULL,positive_blocks INTEGER NOT NULL,blocks INTEGER NOT NULL,state TEXT NOT NULL,last_issue TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+MODEL+"(owner TEXT PRIMARY KEY,artifact BLOB NOT NULL,artifact_hash TEXT NOT NULL,trained_until TEXT NOT NULL,updates INTEGER NOT NULL,loss REAL NOT NULL,gain_sum REAL NOT NULL,ablation_gain BLOB NOT NULL)");
        TransactionSchemaCacheR941.mark(db,"AtomicToolWorldR943");
    }
    static final class Quality {
        int index,n,missing,blockN,positiveBlocks,blocks;double gain,gainSq,blockGain;double[] counts=new double[10];String state="OBSERVING",last="";
        double probability(double x){int b=Math.min(4,Math.max(0,(int)(x*5)));return (counts[b+5]+3)/(counts[b]+10);}
        double baseline(){double n=0,h=0;for(int i=0;i<5;i++){n+=counts[i];h+=counts[i+5];}return(h+3)/(n+10);}
        double lower(){return n<2?Double.NEGATIVE_INFINITY:gain/n-1.96*Math.sqrt(Math.max(0,gainSq/n-Math.pow(gain/n,2))/n);}
        void observe(double value,int hit,double delta){
            n++;gain+=delta;gainSq+=delta*delta;blockN++;blockGain+=delta;
            int bin=Math.min(4,Math.max(0,(int)(value*5)));counts[bin]++;counts[bin+5]+=hit;
            if(blockN==120){blocks++;if(blockGain>0)positiveBlocks++;blockN=0;blockGain=0;}
            double missingRate=missing/(double)Math.max(1,n+missing);
            state=missingRate>.10?"MASKED_MISSING":n>=240&&lower()>0&&blocks>=2&&positiveBlocks==blocks?"RESEARCH_PARENT":n>=240&&gain/n+1.96*Math.sqrt(Math.max(0,gainSq/n-Math.pow(gain/n,2))/n)<0?"RETIRED_NEGATIVE_UTILITY":"OBSERVING";
        }
    }
    static Map<Integer,Quality> qualities(SQLiteDatabase db){
        Map<Integer,Quality> out=new HashMap<Integer,Quality>();
        try(Cursor c=db.rawQuery("SELECT atom_index,n,missing,counts,gain_sum,gain_sq,block_n,block_gain,positive_blocks,blocks,state,last_issue FROM "+QUALITY,null)){
            while(c.moveToNext()){Quality q=new Quality();q.index=c.getInt(0);q.n=c.getInt(1);q.missing=c.getInt(2);q.counts=unpack(c.getBlob(3),10);q.gain=c.getDouble(4);q.gainSq=c.getDouble(5);q.blockN=c.getInt(6);q.blockGain=c.getDouble(7);q.positiveBlocks=c.getInt(8);q.blocks=c.getInt(9);q.state=c.getString(10);q.last=c.getString(11);out.put(q.index,q);}
        }
        return out;
    }
    static final class NetworkState {
        ResearchTinyDeepLearnerR909.Network network=ResearchTinyDeepLearnerR909.Network.initial();String hash=V51ChecksumEngine.sha256(VERSION+"|GENESIS"),until="";double gain;double[] ablation=new double[7];
    }
    static NetworkState network(SQLiteDatabase db){
        NetworkState s=new NetworkState();
        try(Cursor c=db.rawQuery("SELECT artifact,artifact_hash,trained_until,gain_sum,ablation_gain FROM "+MODEL+" WHERE owner=?",new String[]{VERSION})){
            if(c.moveToFirst()){
                byte[] bytes=c.getBlob(0);if(!ResearchArtifactCodecR811.verify(bytes,c.getString(1)))throw new IllegalStateException("ATOMIC_NETWORK_HASH");
                ResearchArtifactCodecR811.Artifact a=ResearchArtifactCodecR811.decode(bytes);if(!VERSION.equals(a.type)||!a.trainedUntilIssue.equals(c.getString(2)))throw new IllegalStateException("ATOMIC_NETWORK_IDENTITY");
                s.network=ResearchTinyDeepLearnerR909.Network.from(a);s.hash=c.getString(1);s.until=c.getString(2);s.gain=c.getDouble(3);s.ablation=unpack(c.getBlob(4),7);
            }
        }
        return s;
    }
    /** Runs in the same predraw context transaction; retries cannot replace an earlier model. */
    static void freeze(SQLiteDatabase db,GenesisAssetVmR915.PredrawContext context){
        if(context.observation==null)return;if(!db.inTransaction())throw new IllegalStateException("ATOMIC_FREEZE_TRANSACTION");schema(db);
        try(Cursor c=db.rawQuery("SELECT base_issue,position,n1,n2,n3 FROM "+CONTEXT+" WHERE target_issue=?",new String[]{context.targetIssue})){
            if(c.moveToFirst())return; // first independently captured context owns this issue
        }
        if(!TargetIssueResolver.exact(context.baseIssue,context.targetIssue).valid||drawn(db,context.targetIssue))throw new IllegalStateException("ATOMIC_FREEZE_POSTDRAW");
        NetworkState network=network(db);if(network.until.compareTo(context.targetIssue)>=0)throw new IllegalStateException("ATOMIC_MODEL_FUTURE_LEAK");
        Map<Integer,Quality> quality=qualities(db);double[] values=new double[240],probabilities=new double[480];Arrays.fill(values,-1);
        for(int i=0;i<240;i++){
            if(!GenesisSchemaRegistryR915.atoms().get(i).productionScoreEligible()||!context.observation.present[i])continue;
            values[i]=context.atoms[i];Quality q=quality.get(i);if(q==null)q=new Quality();
            probabilities[i]=q.probability(values[i]);probabilities[240+i]=q.baseline();
        }
        double[] x=groupFeatures(values),ablated=new double[7];double p=network.network.predict(x);
        for(int i=0;i<7;i++){double[] off=x.clone();off[i]=.5;ablated[i]=network.network.predict(off);}
        String record=recordHash(context.targetIssue,context.baseIssue,context.position,context.n1,context.n2,context.n3,values,probabilities,p,ablated,network.hash,network.until);
        db.execSQL("INSERT INTO "+CONTEXT+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,-1,0)",new Object[]{context.targetIssue,context.baseIssue,context.position,context.n1,context.n2,context.n3,pack(values),pack(probabilities),p,pack(ablated),network.hash,network.until,record});
    }
    /** Numerical preparation happens outside the writer; existing Atomic owner commits one issue. */
    private static final class PreparedLearningF03 {
        final String issue,contextHash,oldQualityHash,oldNetworkHash,oldNetworkUntil,status;
        final int hit,actual;
        final List<Object[]> qualityRows;
        final Object[] networkRow;
        PreparedLearningF03(String issue,String contextHash,String oldQualityHash,
                String oldNetworkHash,String oldNetworkUntil,int hit,int actual,List<Object[]> qualityRows,
                Object[] networkRow,String status){
            this.issue=issue;this.contextHash=contextHash;this.oldQualityHash=oldQualityHash;
            this.oldNetworkHash=oldNetworkHash;this.oldNetworkUntil=oldNetworkUntil;this.hit=hit;this.actual=actual;
            this.qualityRows=Collections.unmodifiableList(qualityRows);this.networkRow=networkRow;this.status=status;
        }
    }
    private static String qualityHashF03(Map<Integer,Quality> quality){
        StringBuilder s=new StringBuilder("ATOMIC_QUALITY_CAS_F03_V1");
        for(Map.Entry<Integer,Quality> e:new TreeMap<Integer,Quality>(quality).entrySet()){
            Quality q=e.getValue();s.append('|').append(e.getKey()).append('|').append(q.n).append('|').append(q.missing)
                .append('|').append(Arrays.toString(q.counts)).append('|').append(Double.toHexString(q.gain))
                .append('|').append(Double.toHexString(q.gainSq)).append('|').append(q.blockN)
                .append('|').append(Double.toHexString(q.blockGain)).append('|').append(q.positiveBlocks)
                .append('|').append(q.blocks).append('|').append(q.state).append('|').append(q.last);
        }
        return V51ChecksumEngine.sha256(s.toString());
    }
    private static PreparedLearningF03 prepareLearningF03(SQLiteDatabase db){
        if(db.inTransaction())throw new IllegalStateException("ATOMIC_PREPARE_IN_WRITE");
        String issue,base,modelHash,trainedUntil,record;int position,n1,n2,n3;
        double p;double[] values,probs,ablated;
        try(Cursor c=db.rawQuery("SELECT target_issue,position,n1,n2,n3,features,probabilities,network_probability,ablation,base_issue,network_hash,trained_until,record_hash FROM "+CONTEXT+" WHERE hit=-1 AND EXISTS(SELECT 1 FROM "+DrawDependencyInputCP12.SOURCE+" d WHERE d.issue=target_issue AND d.dirty=0) ORDER BY target_issue LIMIT 1",null)){
            if(!c.moveToFirst())return null;issue=c.getString(0);position=c.getInt(1);n1=c.getInt(2);n2=c.getInt(3);n3=c.getInt(4);
            values=unpack(c.getBlob(5),240);probs=unpack(c.getBlob(6),480);p=c.getDouble(7);ablated=unpack(c.getBlob(8),7);
            base=c.getString(9);modelHash=c.getString(10);trainedUntil=c.getString(11);record=c.getString(12);
        }
        if(!TargetIssueResolver.exact(base,issue).valid||trainedUntil.compareTo(issue)>=0
                ||!recordHash(issue,base,position,n1,n2,n3,values,probs,p,ablated,modelHash,trainedUntil).equals(record))
            throw new IllegalStateException("ATOMIC_FROZEN_INPUT_HASH_OR_PIT");
        if(position<1||position>10||!DecisionGateEngine.b12K3(n1,n2,n3)||!Double.isFinite(p)||p<0||p>1)
            throw new IllegalStateException("ATOMIC_FROZEN_ACTION_OR_P");
        int actual;
        try(Cursor draw=db.rawQuery("SELECT n"+position+" FROM "+DrawDependencyInputCP12.SOURCE+" WHERE issue=? AND dirty=0",new String[]{issue})){
            if(!draw.moveToFirst())return null;actual=draw.getInt(0);
        }
        if(actual<1||actual>10)throw new IllegalStateException("ATOMIC_EXACT_DRAW_INVALID");
        int hit=actual==n1||actual==n2||actual==n3?1:0;
        Map<Integer,Quality> quality=qualities(db);String qualityBefore=qualityHashF03(quality);
        NetworkState network=network(db);String networkBefore=network.hash,networkUntil=network.until;
        if(!networkUntil.isEmpty()&&networkUntil.compareTo(issue)>=0)throw new IllegalStateException("ATOMIC_NETWORK_LABEL_ORDER");
        int learned=0;List<Object[]> rows=new ArrayList<Object[]>();
        for(int i=0;i<240;i++){
            ResearchWorkBudgetR927.checkpoint();if(!GenesisSchemaRegistryR915.atoms().get(i).productionScoreEligible())continue;
            Quality q=quality.get(i);if(q==null){q=new Quality();q.index=i;}
            if(!q.last.isEmpty()&&q.last.compareTo(issue)>=0)throw new IllegalStateException("ATOMIC_LABEL_ORDER");
            if(values[i]<0){q.missing++;if(q.missing>24&&q.missing/(double)Math.max(1,q.n+q.missing)>.10)q.state="MASKED_MISSING";}
            else{
                if(values[i]>1||!Double.isFinite(probs[i])||probs[i]<0||probs[i]>1
                        ||!Double.isFinite(probs[240+i])||probs[240+i]<0||probs[240+i]>1)
                    throw new IllegalStateException("ATOMIC_FEATURE_OR_PROBABILITY_RANGE");
                q.observe(values[i],hit,Math.pow(probs[240+i]-hit,2)-Math.pow(probs[i]-hit,2));learned++;
            }
            q.last=issue;
            rows.add(new Object[]{i,GenesisSchemaRegistryR915.atoms().get(i).id,q.n,q.missing,pack(q.counts),q.gain,q.gainSq,q.blockN,q.blockGain,q.positiveBlocks,q.blocks,q.state,issue});
        }
        Object[] networkRow=null;String status="MASKED_ALL_ATOMS_NO_NETWORK_UPDATE|issue="+issue;
        if(learned>0){
            ResearchWorkBudgetR927.checkpoint();double loss=network.network.update(groupFeatures(values),hit);
            if(!Double.isFinite(loss))throw new IllegalStateException("ATOMIC_TRAIN_LOSS_NONFINITE");
            network.gain+=Math.pow(.3-hit,2)-Math.pow(p-hit,2);
            for(int i=0;i<7;i++)network.ablation[i]+=Math.pow(ablated[i]-hit,2)-Math.pow(p-hit,2);
            ResearchArtifactCodecR811.Artifact a=network.network.toArtifact(issue),artifact=new ResearchArtifactCodecR811.Artifact(VERSION,issue,a.fields);
            networkRow=new Object[]{VERSION,ResearchArtifactCodecR811.encode(artifact),ResearchArtifactCodecR811.hash(artifact),issue,network.network.updates,loss,network.gain,pack(network.ablation)};
            status="LEARNED|issue="+issue+"|atoms="+learned+"|networkUpdates="+network.network.updates+"|neuralInput=7_MASKED_ATOM_GROUPS|productionAuthority=0";
        }
        return new PreparedLearningF03(issue,record,qualityBefore,networkBefore,networkUntil,hit,actual,rows,networkRow,status);
    }
    private static String commitLearningF03(SQLiteDatabase db,PreparedLearningF03 prepared){
        if(!db.inTransaction())throw new IllegalStateException("ATOMIC_COMMIT_WITHOUT_TRANSACTION");
        try(Cursor context=db.rawQuery("SELECT record_hash,hit,position FROM "+CONTEXT+" WHERE target_issue=?",new String[]{prepared.issue})){
            if(!context.moveToFirst()||!prepared.contextHash.equals(context.getString(0))||context.getInt(1)!=-1)return "YIELD_ATOMIC_CAS";
            int position=context.getInt(2);
            try(Cursor draw=db.rawQuery("SELECT d.n"+position+",a.n1,a.n2,a.n3 FROM "+DrawDependencyInputCP12.SOURCE+" d JOIN "+CONTEXT+" a ON a.target_issue=d.issue WHERE d.issue=? AND d.dirty=0",new String[]{prepared.issue})){
                if(!draw.moveToFirst())return "YIELD_ATOMIC_DRAW_CHANGED";
                int actual=draw.getInt(0),hit=actual==draw.getInt(1)||actual==draw.getInt(2)||actual==draw.getInt(3)?1:0;
                if(actual<1||actual>10||actual!=prepared.actual||hit!=prepared.hit)return "YIELD_ATOMIC_DRAW_CHANGED";
            }
        }
        NetworkState current=network(db);
        if(!prepared.oldNetworkHash.equals(current.hash)||!prepared.oldNetworkUntil.equals(current.until)
                ||!prepared.oldQualityHash.equals(qualityHashF03(qualities(db))))return "YIELD_ATOMIC_CAS";
        StringBuilder slots=new StringBuilder();ArrayList<Object> args=new ArrayList<Object>();
        for(Object[] row:prepared.qualityRows){
            if(slots.length()>0)slots.append(',');slots.append("(?,?,?,?,?,?,?,?,?,?,?,?,?)");Collections.addAll(args,row);
            if(args.size()>=48*13){db.execSQL("INSERT OR REPLACE INTO "+QUALITY+" VALUES"+slots,args.toArray());slots.setLength(0);args.clear();}
        }
        if(!args.isEmpty())db.execSQL("INSERT OR REPLACE INTO "+QUALITY+" VALUES"+slots,args.toArray());
        if(prepared.networkRow!=null)db.execSQL("INSERT OR REPLACE INTO "+MODEL+" VALUES(?,?,?,?,?,?,?,?)",prepared.networkRow);
        db.execSQL("UPDATE "+CONTEXT+" SET hit=?,settled_at=? WHERE target_issue=? AND hit=-1 AND record_hash=?",new Object[]{prepared.hit,System.currentTimeMillis(),prepared.issue,prepared.contextHash});
        try(Cursor changed=db.rawQuery("SELECT changes()",null)){if(!changed.moveToFirst()||changed.getInt(0)!=1)throw new IllegalStateException("ATOMIC_CONTEXT_CAS");}
        return prepared.status;
    }
    public static String maintain(final DbStore store){
        if(store==null)return "WAIT_STORE";
        if(RealtimePressureGateR811.hasActiveCriticalWork())return "YIELD_P0";
        final PreparedLearningF03 prepared=prepareLearningF03(store.readable());
        if(prepared==null)return "WAIT_NEW_FROZEN_ATOM_LABEL";
        if(RealtimePressureGateR811.hasActiveCriticalWork())return "YIELD_P0";
        final Throwable[] failure={null};
        String result=store.coordinatorR36().runAtomicWrite("R943_ATOMIC_RESEARCH_LEARNING",new DatabaseCoordinator.WriteCallable<String>(){
            public String call(SQLiteDatabase db)throws Exception{try{return commitLearningF03(db,prepared);}catch(Exception error){failure[0]=error;throw error;}}
        },"ATOMIC_LEARNING_TRANSACTION_FAILED");
        if("ATOMIC_LEARNING_TRANSACTION_FAILED".equals(result)&&failure[0]!=null){
            Throwable root=failure[0];Set<Throwable> seen=Collections.newSetFromMap(new IdentityHashMap<Throwable,Boolean>());
            while(root.getCause()!=null&&seen.add(root)&&!seen.contains(root.getCause()))root=root.getCause();
            String message=root.getClass().getSimpleName()+":"+String.valueOf(root.getMessage());
            result+="|phase=COMMIT|cause="+(message.length()>384?message.substring(0,384):message);
        }
        store.setState("r943_atomic_world_training",result);return result;
    }
    static double[] groupFeatures(double[] values){
        double[] sum=new double[7];int[] count=new int[7];
        for(int i=0;i<values.length;i++)if(values[i]>=0&&GenesisSchemaRegistryR915.atoms().get(i).productionScoreEligible()){int group=toolGroup(i);sum[group]+=values[i];count[group]++;}
        for(int i=0;i<7;i++)sum[i]=count[i]==0?.5:sum[i]/count[i];return sum;
    }
    /** Evidence-ranked, diverse raw materials for a newly hashed executable asset. */
    static int[] creationAtoms(SQLiteDatabase db,int[] fallback){
        schema(db);final Map<Integer,Quality> all=qualities(db);final NetworkState net=network(db);
        Set<Integer> quarantined=AtomCaptureR932.quarantined(db);
        List<Integer> eligible=new ArrayList<Integer>();for(Quality q:all.values())if(q.state.equals("RESEARCH_PARENT")&&!quarantined.contains(q.index))eligible.add(q.index);
        Collections.sort(eligible,new Comparator<Integer>(){public int compare(Integer a,Integer b){
            double ga=all.get(a).lower()+Math.max(0,net.ablation[toolGroup(a)])/Math.max(1,net.network.updates),gb=all.get(b).lower()+Math.max(0,net.ablation[toolGroup(b)])/Math.max(1,net.network.updates);int order=Double.compare(gb,ga);return order==0?Integer.compare(a,b):order;}});
        List<Integer> chosen=new ArrayList<Integer>();Set<Integer> groups=new HashSet<Integer>();
        for(int i:eligible)if(groups.add(i/20))chosen.add(i);
        if(groups.size()<3)return fallback;
        for(int i:eligible)if(!chosen.contains(i)&&chosen.size()<12)chosen.add(i);
        if(chosen.size()<8)return fallback;while(chosen.size()>12)chosen.remove(chosen.size()-1);
        int[] out=new int[chosen.size()];for(int i=0;i<out.length;i++)out[i]=chosen.get(i);Arrays.sort(out);return out;
    }
    public static String report(DbStore store,boolean detail){
        StringBuilder out=new StringBuilder("原子世界：165项可评分原子→分箱工具→7组神经网络→资产原料选择。\n");
        try{SQLiteDatabase db=store.readable();
            out.append(store.getState("r943_atomic_world_training","等待首次开奖前原子冻结")).append('\n');
            try(Cursor c=db.rawQuery("SELECT state,COUNT(*),MIN(n),MAX(n) FROM "+QUALITY+" GROUP BY state",null)){while(c.moveToNext())out.append(c.getString(0)).append("：").append(c.getInt(1)).append("项；样本范围 ").append(c.getInt(2)).append("–").append(c.getInt(3)).append('\n');}
            if(detail)for(Quality q:qualities(db).values())out.append(GenesisSchemaRegistryR915.atoms().get(q.index).id).append(" | ").append(q.state).append(" | N=").append(q.n).append(" | missing=").append(q.missing).append(" | BrierGain=").append(q.n==0?"未知":q.gain/q.n).append(" | lower=").append(q.lower()).append(" | positive120Blocks=").append(q.positiveBlocks).append('/').append(q.blocks).append('\n');
            out.append("特征增益仅用于研究选材；新资产仍须独立未来样本与同一证书门。原子缺失、负增益和隔离状态均保留原因。");
        }catch(Exception e){out.append("状态暂不可读：").append(e.getClass().getSimpleName()).append(':').append(e.getMessage());}
        return out.toString();
    }
    private static boolean drawn(SQLiteDatabase db,String target){try(Cursor c=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" WHERE issue>=? LIMIT 1",new String[]{target})){return c.moveToFirst();}}
    // Context/coordinates, time, graph, operators, statistics, memory, uncertainty/action.
    private static int toolGroup(int atom){int g=atom/20;return g<=1?0:g<=6?g-1:6;}
    private static String recordHash(String issue,String base,int position,int a,int b,int c,double[] values,double[] probabilities,double p,double[] ablation,String network,String until){
        return V51ChecksumEngine.sha256(VERSION+'|'+issue+'|'+base+'|'+position+'|'+a+'|'+b+'|'+c+'|'+network+'|'+until+'|'+Arrays.toString(values)+'|'+Arrays.toString(probabilities)+'|'+Double.toHexString(p)+'|'+Arrays.toString(ablation));
    }
    static byte[] pack(double[] x){try{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);for(double v:x){if(!Double.isFinite(v))throw new IllegalArgumentException("ATOMIC_NONFINITE");d.writeDouble(v);}d.close();return b.toByteArray();}catch(IOException e){throw new IllegalStateException(e);}}
    static double[] unpack(byte[] b,int n){if(b==null||b.length!=n*8)throw new IllegalArgumentException("ATOMIC_VECTOR_SHAPE");try{DataInputStream in=new DataInputStream(new ByteArrayInputStream(b));double[] x=new double[n];for(int i=0;i<n;i++){x[i]=in.readDouble();if(!Double.isFinite(x[i]))throw new IllegalArgumentException("ATOMIC_VECTOR_NONFINITE");}return x;}catch(IOException e){throw new IllegalStateException(e);}}

public static String reportSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store,boolean detail){
        com.ethk3.novaonepro.ReportTextCP12 out=new com.ethk3.novaonepro.ReportTextCP12(report, "原子世界：165项可评分原子→分箱工具→7组神经网络→资产原料选择。\n");
        try{SQLiteDatabase db=store.readable();
            out.append(store.getState("r943_atomic_world_training","等待首次开奖前原子冻结")).append('\n');
            try(Cursor c=report.select("SELECT state,COUNT(*),MIN(n),MAX(n) FROM "+QUALITY+" GROUP BY state",null)){while(c.moveToNext())out.append(c.getString(0)).append("：").append(c.getInt(1)).append("项；样本范围 ").append(c.getInt(2)).append("–").append(c.getInt(3)).append('\n');}
            if(detail)for(Quality q:qualitiesSnapshotCP4(report, db).values())out.append(GenesisSchemaRegistryR915.atoms().get(q.index).id).append(" | ").append(q.state).append(" | N=").append(q.n).append(" | missing=").append(q.missing).append(" | BrierGain=").append(q.n==0?"未知":q.gain/q.n).append(" | lower=").append(q.lower()).append(" | positive120Blocks=").append(q.positiveBlocks).append('/').append(q.blocks).append('\n');
            out.append("特征增益仅用于研究选材；新资产仍须独立未来样本与同一证书门。原子缺失、负增益和隔离状态均保留原因。");
        }catch(Exception e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}
        return out.toString();
    }

static Map<Integer,Quality> qualitiesSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db){
        Map<Integer,Quality> out=new HashMap<Integer,Quality>();
        try(Cursor c=report.select("SELECT atom_index,n,missing,counts,gain_sum,gain_sq,block_n,block_gain,positive_blocks,blocks,state,last_issue FROM "+QUALITY,null)){
            while(c.moveToNext()){Quality q=new Quality();q.index=c.getInt(0);q.n=c.getInt(1);q.missing=c.getInt(2);q.counts=unpackSnapshotCP4(report, c.getBlob(3),10);q.gain=c.getDouble(4);q.gainSq=c.getDouble(5);q.blockN=c.getInt(6);q.blockGain=c.getDouble(7);q.positiveBlocks=c.getInt(8);q.blocks=c.getInt(9);q.state=c.getString(10);q.last=c.getString(11);out.put(q.index,q);}
        }
        return out;
    }

static double[] unpackSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, byte[] b,int n){if(b==null||b.length!=n*8)throw new IllegalArgumentException("ATOMIC_VECTOR_SHAPE");try{DataInputStream in=new DataInputStream(new ByteArrayInputStream(b));double[] x=new double[n];for(int i=0;i<n;i++){x[i]=in.readDouble();if(!Double.isFinite(x[i]))throw new IllegalArgumentException("ATOMIC_VECTOR_NONFINITE");}return x;}catch(IOException e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}}
}
