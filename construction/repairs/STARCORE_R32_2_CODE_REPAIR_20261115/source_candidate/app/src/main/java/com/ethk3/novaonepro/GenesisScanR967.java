package com.ethk3.novaonepro;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Arithmetic-only candidate patch. No DB, label, certificate or production authority.
 * A Surface owns defensive copies of all context values and is retained across slices.
 * Its cache is process-local; loss means recompute before draw, never retroactive proof.
 */
final class GenesisScanR967 {
    private static final int ACTIONS=1200, MAX_SURFACES=2, MAX_SCANS=96;
    private static final Map<DbStore,LinkedHashMap<String,Surface>> CACHE=
            new WeakHashMap<DbStore,LinkedHashMap<String,Surface>>();
    interface ContextFactory { GenesisAssetVmR915.PredrawContext build(int index); }
    static final class Surface {
        final String identity,base,target,input,history;
        final int historyRows;
        private final V51Models.PositionAction[] actions;
        private final GenesisAssetVmR915.PredrawContext[] contexts;
        private final LinkedHashMap<String,GenesisScanR967> scans=
                new LinkedHashMap<String,GenesisScanR967>();
        private int built;
        private boolean retired;
        private Surface(String identity,String base,String target,String input,String history,
                int historyRows,V51Models.PositionAction[] actions) {
            this.identity=identity;this.base=base;this.target=target;this.input=input;
            this.history=history;this.historyRows=historyRows;
            this.actions=actions.clone();this.contexts=new GenesisAssetVmR915.PredrawContext[ACTIONS];
        }
        synchronized boolean prepare(ContextFactory factory,long deadlineMono) {
            active();
            if(factory==null)throw new IllegalArgumentException("R967_CONTEXT_FACTORY_REQUIRED");
            while(built<ACTIONS) {
                ResearchWorkBudgetR927.checkpoint();
                if(expired(deadlineMono))return false;
                GenesisAssetVmR915.PredrawContext c=factory.build(built);
                if(c==null)throw new IllegalStateException("R967_CONTEXT_BUILD_NULL:"+built);
                V51Models.PositionAction a=actions[built];
                if(!base.equals(c.baseIssue)||!target.equals(c.targetIssue)
                        ||!input.equals(c.surfaceHash)||!history.equals(c.historyHash)
                        ||historyRows!=c.historyRows||a.position!=c.position||a.n1!=c.n1
                        ||a.n2!=c.n2||a.n3!=c.n3)
                    throw new IllegalStateException("R967_CONTEXT_IDENTITY_MISMATCH:"+built);
                // Publish one immutable arithmetic checkpoint before moving its cursor.
                contexts[built]=copy(c);built++;
            }
            return true;
        }
        synchronized int builtCount(){return built;}
        synchronized GenesisAssetVmR915.PredrawContext first(){
            active();if(built!=ACTIONS)throw new IllegalStateException("R967_SURFACE_NOT_COMPLETE");
            return copy(contexts[0]);
        }
        synchronized GenesisScanR967 scan(GenesisAssetGenomeR915 genome,
                GenesisAssetVmR915.State state,boolean proxy){
            active();if(built!=ACTIONS)throw new IllegalStateException("R967_SURFACE_NOT_COMPLETE");
            String key=genome.assetId+'|'+proxy;
            GenesisScanR967 s=scans.get(key);
            if(s==null||!s.matches(genome,state)){
                if(s==null&&scans.size()>=MAX_SCANS)scans.remove(scans.keySet().iterator().next());
                s=new GenesisScanR967(this,genome,state,proxy);scans.put(key,s);
            }
            return s;
        }
        synchronized void release(String id,boolean proxy){scans.remove(id+'|'+proxy);}
        synchronized void retire(){retired=true;scans.clear();}
        private void active(){if(retired)throw new IllegalStateException("R967_SURFACE_RETIRED");}
    }
    static Surface acquireSurface(DbStore store,String source,String base,String target,
            String input,String history,int historyRows,long timingInput,
            V51Models.PositionAction[] actions) {
        if(store==null||source==null||!source.matches("[A-Z_]{1,32}")
                ||!TargetIssueResolver.exact(base,target).valid||historyRows<0||timingInput<0)
            throw new IllegalArgumentException("R967_SURFACE_CONTRACT");
        hash(input);hash(history);K3ActionSpaceEngine.validateOrThrow(actions);
        StringBuilder ordered=new StringBuilder();
        for(V51Models.PositionAction a:actions)
            ordered.append(K3ActionSpaceEngine.globalOrdinal(a.position,a.n1,a.n2,a.n3)).append(',');
        // All variable text below is validated delimiter-free. Surface data itself is source-bound.
        String identity=V51ChecksumEngine.sha256("R967_SURFACE_V1|"+source+'|'+base+'|'+target+'|'
                +input+'|'+history+'|'+historyRows+'|'+timingInput+'|'+ordered);
        synchronized(CACHE){
            LinkedHashMap<String,Surface> map=CACHE.get(store);
            if(map==null){map=new LinkedHashMap<String,Surface>();CACHE.put(store,map);}
            // Only the active target is retained. LIVE and SNAPSHOT may coexist without thrashing.
            java.util.Iterator<Map.Entry<String,Surface>> it=map.entrySet().iterator();
            while(it.hasNext()){Surface v=it.next().getValue();if(!v.target.equals(target)){v.retire();it.remove();}}
            Surface s=map.get(identity);
            if(s==null){
                if(map.size()>=MAX_SURFACES){String first=map.keySet().iterator().next();map.remove(first).retire();}
                s=new Surface(identity,base,target,input,history,historyRows,actions);map.put(identity,s);
            }
            return s;
        }
    }
    static void releaseTarget(DbStore store,String target){
        synchronized(CACHE){LinkedHashMap<String,Surface> map=CACHE.get(store);if(map==null)return;
            java.util.Iterator<Surface> it=map.values().iterator();
            while(it.hasNext()){Surface s=it.next();if(s.target.equals(target)){s.retire();it.remove();}}
            if(map.isEmpty())CACHE.remove(store);
        }
    }
    private final Surface surface;
    final String programBefore,stateBefore;
    final GenesisAssetGenomeR915 replayGenome;
    final GenesisAssetVmR915.State replayState;
    final CandidateExecutionAuditR932.Builder audit;
    final int stride;
    int cursor,best=-1;
    double bestScore=-1;
    private GenesisAssetVmR915.PredrawContext bestContext;
    GenesisAssetVmR915.AssetValue bestValue;
    private GenesisScanR967(Surface surface,GenesisAssetGenomeR915 genome,
            GenesisAssetVmR915.State state,boolean proxy){
        this.surface=surface;this.stride=proxy?10:1;
        programBefore=genome.canonical();stateBefore=state.encode();
        replayGenome=GenesisAssetGenomeR915.parse(programBefore);
        replayState=GenesisAssetVmR915.State.decode(stateBefore);
        audit=proxy?null:new CandidateExecutionAuditR932.Builder();
    }
    private boolean matches(GenesisAssetGenomeR915 genome,GenesisAssetVmR915.State state){
        return programBefore.equals(genome.canonical())&&stateBefore.equals(state.encode());
    }
    synchronized boolean advance(GenesisAssetGenomeR915 genome,GenesisAssetVmR915.State state,long deadlineMono){
        synchronized(surface){
            surface.active();
            if(!matches(genome,state))throw new IllegalStateException("R967_SCAN_STATE_DRIFT");
            for(int i=cursor;i<ACTIONS;i+=stride){
                ResearchWorkBudgetR927.checkpoint();if(expired(deadlineMono))return false;
                V51Models.PositionAction action=surface.actions[i];
                GenesisAssetVmR915.PredrawContext context=surface.contexts[i];
                GenesisAssetVmR915.AssetValue value=GenesisAssetVmR915.evaluateCompact(genome,state,context);
                double quality=value.primary*(.65d+.35d*value.stability)-.12d*value.risk;
                if(Double.isNaN(quality)||Double.isInfinite(quality))throw new IllegalStateException("R967_NONFINITE_SCORE");
                if(audit!=null){
                    GenesisAssetVmR915.AssetValue replay=GenesisAssetVmR915.evaluateCompact(replayGenome,replayState,context);
                    audit.add(K3ActionSpaceEngine.globalOrdinal(action.position,action.n1,action.n2,action.n3),quality,
                            replay.primary*(.65d+.35d*replay.stability)-.12d*replay.risk,i);
                }
                if(best<0||quality>bestScore||(quality==bestScore&&i<best)){
                    best=i;bestScore=quality;bestContext=context;bestValue=value;
                }
                cursor=i+stride;
            }
            return true;
        }
    }
    synchronized GenesisAssetVmR915.PredrawContext selectedContext(){
        synchronized(surface){surface.active();
            if(cursor!=ACTIONS||bestContext==null)throw new IllegalStateException("R967_SCAN_INCOMPLETE");
            return copy(bestContext);
        }
    }
    private static boolean expired(long deadlineMono){
        return TimeSourceR811.monoNowMs()>=deadlineMono||RealtimePressureGateR811.hasActiveCriticalWork();
    }
    private static void hash(String value){if(value==null||!value.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R967_HASH_REQUIRED");}
    private static GenesisAssetVmR915.PredrawContext copy(GenesisAssetVmR915.PredrawContext c){
        AtomCaptureR932.Values o=c.observation,cloned=null;
        if(o!=null){
            int n=o.values.length;
            if(n!=c.atoms.length||o.present.length!=n||o.costNs.length!=n||o.missingReasons.length!=n)
                throw new IllegalArgumentException("R967_OBSERVATION_SHAPE");
            cloned=new AtomCaptureR932.Values(n,o.maxSourceIssue);
            System.arraycopy(o.values,0,cloned.values,0,n);System.arraycopy(o.present,0,cloned.present,0,n);
            System.arraycopy(o.costNs,0,cloned.costNs,0,n);System.arraycopy(o.missingReasons,0,cloned.missingReasons,0,n);
        }
        return new GenesisAssetVmR915.PredrawContext(c.baseIssue,c.targetIssue,c.historyHash,c.surfaceHash,
                c.historyRows,c.position,c.n1,c.n2,c.n3,c.actionOrdinal,c.atoms,cloned);
    }
}
