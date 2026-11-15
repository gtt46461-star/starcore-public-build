package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.util.*;

/** Bounded, read-only matrix chart DTOs. Never creates evidence or changes authorization.
 * Every payload is read through the sole CP4 codec/hash verifier. Each checkpoint has
 * its own disclosed throughIssue/version; targetExclusive is a strict PIT upper bound. */
public final class MatrixVisualDataR82 {
    private MatrixVisualDataR82(){}
    public static final class Cell {
        public final String metric;public final int index;public final double value;
        Cell(String metric,int index,double value){this.metric=metric;this.index=index;this.value=value;}
    }
    public static final class Point {
        public final long ordinal;public final boolean observed,missing,hit;public final double frozenProbability;
        Point(MatrixProjectionCP12.Sample row,boolean probability){ordinal=row.ordinal;observed=row.has(MatrixProjectionCP12.OBSERVED);missing=row.has(MatrixProjectionCP12.MISSING);hit=row.has(MatrixProjectionCP12.HIT_A);frozenProbability=observed&&!missing&&probability?row.x:Double.NaN;}
    }
    public static final class Snapshot {
        public final String matrixId,type,partition,throughIssue,payloadHash,manifestHash,status;
        public final long version,support;public final int window;
        public final List<Cell> cells;public final List<Point> settledTimeline;
        Snapshot(MatrixSnapshotBusR952.CheckpointCP4 cp,MatrixProjectionCP12.State state){
            matrixId=cp.matrixId;type=cp.matrixType;partition=cp.partitionKey;throughIssue=cp.throughIssue;payloadHash=cp.payloadHash;manifestHash=cp.featureManifestHash;version=cp.version;support=cp.sampleCount;window=cp.windowSize;
            ArrayList<Cell> values=new ArrayList<>();for(Map.Entry<String,double[]> metric:MatrixProjectionCP12.metrics(state).entrySet())for(int i=0;i<metric.getValue().length;i++)values.add(new Cell(metric.getKey(),i,metric.getValue()[i]));
            if(values.size()>4096)throw new IllegalStateException("MATRIX_VISUAL_CELL_BOUND");cells=Collections.unmodifiableList(values);
            ArrayList<Point> points=new ArrayList<>();List<MatrixProjectionCP12.Sample> rows=state.samplesCopy();boolean probability=state.spec.type==MatrixProjectionCP12.Type.CandidateWindowMatrix||state.spec.type==MatrixProjectionCP12.Type.CandidateErrorMatrix||state.spec.type==MatrixProjectionCP12.Type.ConfidenceMatrix||state.spec.type==MatrixProjectionCP12.Type.ResidualMatrix;
            for(int i=Math.max(0,rows.size()-300);i<rows.size();i++)points.add(new Point(rows.get(i),probability));settledTimeline=Collections.unmodifiableList(points);
            status=cp.sampleCount==0?"NO_SAMPLES":"CHECKPOINT_VERIFIED";
        }
    }
    public static final class Page {
        public final List<Snapshot> snapshots;public final String nextMatrixId;public final boolean hasMore;
        Page(List<Snapshot> snapshots,String nextMatrixId,boolean hasMore){this.snapshots=Collections.unmodifiableList(new ArrayList<>(snapshots));this.nextMatrixId=nextMatrixId;this.hasMore=hasMore;}
    }
    public static Page readPage(SQLiteDatabase db,String targetExclusive,String afterMatrixId,int limit){return readPage(db,targetExclusive,afterMatrixId,limit,null,null);}
    public static Page readPage(SQLiteDatabase db,String targetExclusive,String afterMatrixId,int limit,CancellationSignal cancellation){return readPage(db,targetExclusive,afterMatrixId,limit,null,cancellation);}
    public static Page readPage(SQLiteDatabase db,String targetExclusive,String afterMatrixId,int limit,String typeFilter,CancellationSignal cancellation){
        if(db==null||!TargetIssueResolver.isValid(targetExclusive)||limit<1||limit>4||afterMatrixId==null||!afterMatrixId.isEmpty()&&!afterMatrixId.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("MATRIX_VISUAL_PAGE_INPUT");
        if(typeFilter!=null&&!typeFilter.isEmpty())MatrixProjectionCP12.Type.valueOf(typeFilter);if(cancellation!=null)cancellation.throwIfCanceled();
        boolean filter=typeFilter!=null&&!typeFilter.isEmpty();String sql="SELECT matrix_id,feature_manifest_hash FROM research_matrix_checkpoint_v2 WHERE matrix_id>? AND through_issue<?"+(filter?" AND matrix_type=?":"")+" ORDER BY matrix_id LIMIT ?";
        String[] args=filter?new String[]{afterMatrixId,targetExclusive,typeFilter,Integer.toString(limit+1)}:new String[]{afterMatrixId,targetExclusive,Integer.toString(limit+1)};
        ArrayList<String[]> ids=new ArrayList<>();try(Cursor c=db.rawQuery(sql,args,cancellation)){while(c.moveToNext()){if(cancellation!=null)cancellation.throwIfCanceled();ids.add(new String[]{c.getString(0),c.getString(1)});}}
        boolean more=ids.size()>limit;ArrayList<Snapshot> snapshots=new ArrayList<>();String last=afterMatrixId;
        for(int i=0;i<Math.min(limit,ids.size());i++){if(cancellation!=null)cancellation.throwIfCanceled();String[] id=ids.get(i);last=id[0];MatrixSnapshotBusR952.CheckpointCP4 cp=MatrixSnapshotBusR952.readAtCP4(db,id[0],id[1],targetExclusive,cancellation);
            if(cp==null)continue; // concurrent advancement past the strict target is not a valid historical sample.
            if(!MatrixProjectionCP12.SCHEMA_HASH.equals(cp.schemaHash))throw new IllegalStateException("MATRIX_VISUAL_SCHEMA");
            MatrixProjectionCP12.Spec spec=new MatrixProjectionCP12.Spec(MatrixProjectionCP12.Type.valueOf(cp.matrixType),cp.partitionKey,cp.windowSize,cp.featureManifestHash);MatrixProjectionCP12.State state=MatrixProjectionCP12.decode(spec,cp.payload());
            if(state.samples()!=cp.sampleCount||state.throughOrdinal()!=MatrixSourceRouterCP12.issueOrdinal(cp.throughIssue)&&spec.type!=MatrixProjectionCP12.Type.LatencyMatrix&&spec.type!=MatrixProjectionCP12.Type.FailureMatrix)throw new IllegalStateException("MATRIX_VISUAL_CHECKPOINT_SUPPORT");
            snapshots.add(new Snapshot(cp,state));
        }
        return new Page(snapshots,last,more);
    }
    public static final class TransitionEdge {
        public final int from,to,regime;public final double count;
        TransitionEdge(int from,int to,int regime,double count){this.from=from;this.to=to;this.regime=regime;this.count=count;}
    }
    public static List<TransitionEdge> transitionCounts(MatrixProjectionCP12.State state){
        if(state==null||state.spec.type!=MatrixProjectionCP12.Type.TransitionMatrix&&state.spec.type!=MatrixProjectionCP12.Type.ConditionalTransitionMatrix)throw new IllegalArgumentException("MATRIX_VISUAL_TRANSITION_TYPE");
        double[] counts=state.sufficientStatistics();ArrayList<TransitionEdge> edges=new ArrayList<>();for(int r=0;r<(state.spec.type==MatrixProjectionCP12.Type.TransitionMatrix?1:5);r++)for(int a=0;a<10;a++)for(int b=0;b<10;b++)if(counts[r*100+a*10+b]>0)edges.add(new TransitionEdge(a,b,r,counts[r*100+a*10+b]));return Collections.unmodifiableList(edges);
    }
}
