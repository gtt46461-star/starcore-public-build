package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.TargetIssueResolver;
import com.ethk3.novaonepro.V51ChecksumEngine;
import java.io.*;
import java.security.*;
import java.util.*;
import com.ethk3.novaonepro.r952.CandidateCompilerR952.*;

/** Pure bounded development numerical kernel. Registry prepares; Director owns development commits.
 * Each step is a deterministic restart boundary; no wall clock, database or publication here.
 * Dataset consists of frozen PIT inputs. It cannot query a live feature or teacher provider.
 */
public strictfp final class DevelopmentTrainerCP9 {
    private DevelopmentTrainerCP9() {}
    public enum Phase { NORMALIZER, INITIALIZE, TRAIN, TREE_QUANTILES, TREE_SCAN, VALIDATE, DONE }
    public static final int MAX_CHECKPOINT=262144;
    private static final int MAX_TAPE=32768;
    private static final String ZERO=String.join("",Collections.nCopies(64,"0"));
    public static final class Row {
        public final String issue,inputHash;
        public final int actual; // 1..10, never visible to graph evaluation
        private final double[] values;private final boolean[] present;
        public Row(String issue,String inputHash,double[] values,boolean[] present,int actual) {
            need(TargetIssueResolver.isValid(issue)&&hash(inputHash)&&actual>=1&&actual<=10,"TRAIN_ROW_ID");
            need(values!=null&&present!=null&&values.length==present.length&&values.length<=80,"TRAIN_ROW_SHAPE");
            this.issue=issue;this.inputHash=inputHash;this.actual=actual;this.values=values.clone();this.present=present.clone();
            for(int i=0;i<values.length;i++)need(Double.isFinite(values[i])&&(present[i]||values[i]==0),"TRAIN_ROW_VALUE");
        }
        public double[] valuesCopy(){return values.clone();}
        public boolean[] presentCopy(){return present.clone();}
    }
    public static final class Dataset {
        public final Modern shape;public final String protocolHash,inputHash,rowsHash;public final List<String> featureIds,missingPolicies;
        public final int trainN;private final Row[] rows;private final String shapeHash;
        public Dataset(Modern shape,String protocol,String input,List<String> ids,List<String> missing,List<Row> rows,int trainN) {
            need(shape!=null&&hash(protocol)&&hash(input)&&ids!=null&&missing!=null&&rows!=null,"TRAIN_DATASET");
            need(trainN>=300&&trainN<=1200&&rows.size()==trainN+120&&shape.uniqueTrainN==trainN,"TRAIN_SPLIT");
            need(ids.size()==shape.featureN&&new HashSet<>(ids).size()==ids.size(),"TRAIN_FEATURE_IDS");
            this.shape=shape;protocolHash=protocol;inputHash=input;this.trainN=trainN;
            featureIds=Collections.unmodifiableList(new ArrayList<>(ids));missingPolicies=Collections.unmodifiableList(new ArrayList<>(missing));need(missing.size()==ids.size(),"TRAIN_MISSING_POLICY_SHAPE");for(String policy:missing)need(policy.equals("MASK")||policy.equals("REJECT"),"TRAIN_MISSING_POLICY");this.rows=rows.toArray(new Row[0]);
            MessageDigest digest=sha();
            for(int i=0;i<this.rows.length;i++) {
                Row r=this.rows[i];need(r!=null&&r.values.length==(shape.featureN==0?shape.inputDimension:shape.featureN),"TRAIN_ROW_DIMENSION");
                if(i>0)need(r.issue.compareTo(this.rows[i-1].issue)>0,"TRAIN_ISSUE_ORDER");
                if(i>0&&i!=trainN)need(TargetIssueResolver.next(this.rows[i-1].issue).targetIssue.equals(r.issue),"TRAIN_HISTORY_GAP");
                if(i==trainN)need(TargetIssueResolver.next(TargetIssueResolver.next(this.rows[i-1].issue).targetIssue).targetIssue.equals(r.issue),"TRAIN_EMBARGO");
                if(shape.featureN>0){int valid=0;for(int f=0;f<shape.featureN;f++){if(r.present[f])valid++;else need(missing.get(f).equals("MASK"),"TRAIN_REQUIRED_FEATURE_MISSING");}need(valid>0,"TRAIN_ALL_FEATURES_MISSING");}
                if(shape.featureN==0)for(boolean present:r.present)need(present,"TRAIN_BOUND_INPUT_MISSING");
                if(shape.kind==ModelKind.EXPERT_HEDGE)for(int e=0;e<shape.expertN;e++)probabilities(Arrays.copyOfRange(r.values,10*e,10*e+10));
                if(shape.kind==ModelKind.FIXED_GRAPH) { double sum=0;for(double x:r.values){need(x>=0,"TRAIN_FIXED_SCORE");sum+=x+1e-9;}need(Double.isFinite(sum)&&sum>0,"TRAIN_FIXED_SCORE"); }
                try {ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(b);
                    o.writeUTF(r.issue);o.writeUTF(r.inputHash);o.writeByte(r.actual);for(int f=0;f<r.values.length;f++){o.writeDouble(r.values[f]);o.writeBoolean(r.present[f]);}digest.update(b.toByteArray());
                }catch(IOException impossible){throw new AssertionError(impossible);}
            }
            need(shape.trainedUntil.equals(this.rows[this.rows.length-1].issue),"TRAIN_KNOWLEDGE_CUTOFF");rowsHash=hex(digest.digest());shapeHash=shapeIdentity(this);
        }
        public long estimatedRetainedBytes(){return estimatedDatasetBytesCP12(trainN,shape.featureN==0?shape.inputDimension:shape.featureN);}
    }
    public static final class State {
        private Phase phase=Phase.NORMALIZER;private int row,epoch,selectedEpoch,patience,batchN,validationN,hitN,bestHitN;
        private long steps,prng;private boolean hasBest;
        private double ce,ceComp,brier,brierComp,bestCE,bestBrier;
        private double[] mean,m2,std,weights,grad,m,v,best;
        private long[] counts;
        private int treeFeatureCursor,treeNode,treeFeature,treeCutIndex,treeRow,treeSize=1;
        private int[] assignment,nodeFeature,nodeLeft,nodeRight,nodeDepth,leftCounts,rightCounts;
        private double[] nodeCut,nodeProb,cutValues,bestGain,bestCut;
        private int[] cutN,bestFeature;
        private byte[] tape=new byte[0],bestTape=new byte[0];
        private String protocol,input,rowsHash,shapeHash;
        private State(Dataset d) {
            protocol=d.protocolHash;input=d.inputHash;rowsHash=d.rowsHash;shapeHash=d.shapeHash;
            int f=d.shape.featureN,n=d.shape.weights().length;
            mean=new double[f];m2=new double[f];std=new double[f];counts=new long[f];
            weights=new double[n];grad=new double[n];m=new double[n];v=new double[n];best=new double[n];prng=d.shape.seed;
            int dimensions=d.shape.kind==ModelKind.HISTOGRAM_TREE?d.shape.inputDimension:0;
            assignment=new int[dimensions==0?0:d.trainN];nodeFeature=new int[dimensions==0?0:31];nodeLeft=new int[nodeFeature.length];nodeRight=new int[nodeFeature.length];nodeDepth=new int[nodeFeature.length];
            Arrays.fill(nodeFeature,-1);Arrays.fill(nodeLeft,-1);Arrays.fill(nodeRight,-1);
            nodeCut=new double[nodeFeature.length];nodeProb=new double[nodeFeature.length*10];bestGain=new double[nodeFeature.length];bestCut=new double[nodeFeature.length];bestFeature=new int[nodeFeature.length];Arrays.fill(bestFeature,-1);
            cutValues=new double[dimensions*15];cutN=new int[dimensions];leftCounts=new int[10];rightCounts=new int[10];
        }
        public Phase phase(){return phase;} public int rowCursor(){return row;}public int epoch(){return epoch;}
        public long optimizerSteps(){return steps;}public boolean complete(){return phase==Phase.DONE;}
        public int selectedEpoch(){return selectedEpoch;}public int executedEpochs(){return epoch+1;}
        public double validationCE(){need(complete(),"TRAIN_NOT_DONE");return bestCE;}
        public double validationBrier(){need(complete(),"TRAIN_NOT_DONE");return bestBrier;}
        public int validationHits(){need(complete(),"TRAIN_NOT_DONE");return bestHitN;}
        public String orderedPredictionHash(){need(complete(),"TRAIN_NOT_DONE");return hex(sha().digest(bestTape));}
        public long normalizerCount(int f){return counts[f];}
        public double normalizerMean(int f){return mean[f];}
        public int pendingBatchRows(){return batchN;}
    }
    public static long estimatedDatasetBytesCP12(int trainN,int width){
        need(trainN>=300&&trainN<=1200&&width>=1&&width<=80,"TRAIN_DATA_MEMORY_SHAPE");
        return (128L<<10)+(trainN+120L)*(320L+16L*width);
    }
    public static long estimatedNumericsBytesCP12(Dataset d){
        need(d!=null,"TRAIN_NUMERICAL_INPUT");
        int weights=Modern.expectedWeights(d.shape.kind,d.shape.inputDimension,d.shape.featureN,d.shape.expertN);
        return 4L*MAX_CHECKPOINT+(256L<<10)+64L*weights+128L*d.shape.inputDimension+16L*d.trainN;
    }
    public static State start(Dataset d){return new State(d);}
    /** One input row, one tree histogram row, one bounded <=1200-element quantile sort,
     * or one bounded model/batch transition. Caller checks its deadline before every step. */
    public static boolean step(Dataset d,State s) {
        requireIdentity(d,s);
        switch(s.phase) {
            case NORMALIZER:
                if(s.row<d.trainN){Row r=d.rows[s.row++];for(int f=0;f<s.mean.length;f++)if(r.present[f]){long n=++s.counts[f];double delta=r.values[f]-s.mean[f];s.mean[f]+=delta/n;s.m2[f]+=delta*(r.values[f]-s.mean[f]);}}
                else {for(int f=0;f<s.std.length;f++){need(s.counts[f]>0,"TRAIN_FEATURE_ALL_MISSING");s.std[f]=Math.sqrt(Math.max(0,s.m2[f]/s.counts[f]));if(s.std[f]<1e-6)s.std[f]=1;}s.phase=Phase.INITIALIZE;s.row=0;}
                break;
            case INITIALIZE:
                if(d.shape.kind==ModelKind.MLP_D32_16_10){int dimension=d.shape.inputDimension;initializeLayer(s,0,dimension,32);initializeLayer(s,32*dimension+32,32,16);initializeLayer(s,32*dimension+32+512+16,16,10);}
                s.phase=d.shape.kind==ModelKind.HISTOGRAM_TREE?Phase.TREE_QUANTILES:Phase.TRAIN;break;
            case TRAIN:
                if(s.row==d.trainN){beginValidation(s);break;}
                double[] x=vector(d,s,s.row);int actual=d.rows[s.row].actual;
                if(d.shape.kind==ModelKind.EXPERT_HEDGE){double max=-Double.MAX_VALUE;for(int e=0;e<s.weights.length;e++){s.weights[e]+=.05*Math.log(Math.max(1e-15,x[e*10+actual-1]));max=Math.max(max,s.weights[e]);}for(int e=0;e<s.weights.length;e++)s.weights[e]-=max;s.steps++;}
                else if(d.shape.kind!=ModelKind.FIXED_GRAPH){double[] g=model(d,s,s.weights,ZERO).lossGradient(x,actual);for(int i=0;i<g.length;i++)s.grad[i]+=g[i];s.batchN++;}
                s.row++;
                if(d.shape.kind!=ModelKind.FIXED_GRAPH&&d.shape.kind!=ModelKind.EXPERT_HEDGE&&(s.batchN==32||s.row==d.trainN))update(d,s);
                break;
            case TREE_QUANTILES:
                if(s.treeFeatureCursor==d.shape.inputDimension){s.phase=Phase.TREE_SCAN;break;}
                int f=s.treeFeatureCursor++;double[] sorted=new double[d.trainN];for(int i=0;i<d.trainN;i++)sorted[i]=vector(d,s,i)[f];Arrays.sort(sorted);
                for(int q=1;q<=15;q++){double cut=sorted[q*(d.trainN-1)/16];int n=s.cutN[f];if(n==0||Double.compare(cut,s.cutValues[f*15+n-1])!=0){s.cutValues[f*15+n]=cut;s.cutN[f]++;}}
                break;
            case TREE_SCAN:treeStep(d,s);break;
            case VALIDATE:
                if(s.validationN<120){validateRow(d,s);break;}
                double ce=s.ce/120,brier=s.brier/120;
                if(!s.hasBest||ce<s.bestCE-1e-6){s.hasBest=true;s.bestCE=ce;s.bestBrier=brier;s.bestHitN=s.hitN;s.best=s.weights.clone();s.bestTape=s.tape.clone();s.selectedEpoch=s.epoch;s.patience=0;}
                else s.patience++;
                if(d.shape.kind==ModelKind.FIXED_GRAPH||d.shape.kind==ModelKind.EXPERT_HEDGE||d.shape.kind==ModelKind.HISTOGRAM_TREE||s.epoch==19||s.patience>=3){s.phase=Phase.DONE;s.weights=s.best.clone();}
                else {s.epoch++;s.row=0;s.phase=Phase.TRAIN;}
                break;
            case DONE:return false;
            default:throw new IllegalStateException("TRAIN_PHASE");
        }
        return true;
    }
    private static void beginValidation(State s){s.phase=Phase.VALIDATE;s.validationN=s.hitN=0;s.ce=s.ceComp=s.brier=s.brierComp=0;s.tape=new byte[0];}
    private static void validateRow(Dataset d,State s){
        Row r=d.rows[d.trainN+s.validationN];double[] p=model(d,s,s.weights,ZERO).probabilities(vector(d,s,d.trainN+s.validationN));probabilities(p);
        double ce=-Math.log(Math.max(1e-15,p[r.actual-1])),br=0;for(int k=0;k<10;k++){double error=p[k]-(k==r.actual-1?1:0);br+=error*error/10;}
        double a=ce-s.ceComp,t=s.ce+a;s.ceComp=(t-s.ce)-a;s.ce=t;a=br-s.brierComp;t=s.brier+a;s.brierComp=(t-s.brier)-a;s.brier=t;
        int above=0;for(int k=0;k<10;k++)if(p[k]>p[r.actual-1]||p[k]==p[r.actual-1]&&k<r.actual-1)above++;if(above<3)s.hitN++;
        try {ByteArrayOutputStream b=new ByteArrayOutputStream(s.tape.length+160);b.write(s.tape);DataOutputStream o=new DataOutputStream(b);
            o.writeUTF(r.issue);o.writeUTF(r.inputHash);for(double q:p)o.writeDouble(q);o.writeByte(r.actual-1);s.tape=b.toByteArray();need(s.tape.length<=MAX_TAPE,"TRAIN_VALIDATION_TAPE");
        }catch(IOException impossible){throw new AssertionError(impossible);}s.validationN++;
    }
    private static double[] vector(Dataset d,State s,int at){
        Row r=d.rows[at];if(d.shape.featureN==0)return r.values.clone();int f=d.shape.featureN;double[] x=new double[2*f];
        for(int i=0;i<f;i++)if(r.present[i]){x[i]=(r.values[i]-s.mean[i])/s.std[i];x[f+i]=1;need(Double.isFinite(x[i]),"TRAIN_STANDARDIZED_NONFINITE");}return x;
    }
    private static void initializeLayer(State s,int offset,int in,int out){double scale=Math.sqrt(6.0/(in+out));for(int i=0;i<in*out;i++)s.weights[offset+i]=(2*uniform(s)-1)*scale;}
    private static double uniform(State s){long z=(s.prng+=0x9e3779b97f4a7c15L);z=(z^(z>>>30))*0xbf58476d1ce4e5b9L;z=(z^(z>>>27))*0x94d049bb133111ebL;z^=z>>>31;return(z>>>11)*0x1.0p-53;}
    private static boolean isWeight(ModelKind k,int d,int i){if(k!=ModelKind.MLP_D32_16_10)return i<10*d;int b1=32*d,w2=b1+32,b2=w2+512,w3=b2+16,b3=w3+160;return i<b1||i>=w2&&i<b2||i>=w3&&i<b3;}
    private static void update(Dataset d,State s){
        need(s.batchN>0,"TRAIN_EMPTY_BATCH");double norm=0;for(int i=0;i<s.grad.length;i++){s.grad[i]/=s.batchN;if(isWeight(d.shape.kind,d.shape.inputDimension,i))s.grad[i]+=1e-4*s.weights[i];need(Double.isFinite(s.grad[i]),"TRAIN_GRADIENT_NONFINITE");norm=Math.hypot(norm,s.grad[i]);}
        double clip=norm>1?1/norm:1;s.steps++;
        for(int i=0;i<s.weights.length;i++){double g=s.grad[i]*clip;if(d.shape.kind==ModelKind.MLP_D32_16_10){s.m[i]=.9*s.m[i]+.1*g;s.v[i]=.999*s.v[i]+.001*g*g;s.weights[i]-=.001*(s.m[i]/(1-Math.pow(.9,s.steps)))/(Math.sqrt(s.v[i]/(1-Math.pow(.999,s.steps)))+1e-8);}else s.weights[i]-=.01*g;need(Double.isFinite(s.weights[i]),"TRAIN_WEIGHT_NONFINITE");s.grad[i]=0;}s.batchN=0;
    }
    private static void treeStep(Dataset d,State s){
        if(s.treeNode==s.treeSize){beginValidation(s);return;}
        int node=s.treeNode;
        // Each node starts with exactly one class-count scan; treeFeature=-1 is that subphase.
        if(s.treeRow==0&&s.treeFeature==0&&s.treeCutIndex==0&&sum(s.leftCounts)+sum(s.rightCounts)==0){s.treeFeature=-1;}
        if(s.treeFeature==-1){
            if(s.treeRow<d.trainN){int r=s.treeRow++;if(s.assignment[r]==node)s.rightCounts[d.rows[r].actual-1]++;return;}
            int n=sum(s.rightCounts);need(n>0,"TRAIN_TREE_EMPTY_NODE");for(int k=0;k<10;k++)s.nodeProb[10*node+k]=(s.rightCounts[k]+.5)/(n+5.0);
            if(s.nodeDepth[node]==4||n<40){finishTreeNode(s);return;}
            s.treeFeature=0;s.treeCutIndex=0;s.treeRow=0;Arrays.fill(s.rightCounts,0); // next call scans first cut; distinct marker below
            s.treeRow=-1;return;
        }
        if(s.treeFeature>=d.shape.inputDimension){
            if(s.bestFeature[node]>=0&&s.bestGain[node]>1e-12){int left=s.treeSize++,right=s.treeSize++;need(s.treeSize<=31,"TRAIN_TREE_NODES");s.nodeFeature[node]=s.bestFeature[node];s.nodeCut[node]=s.bestCut[node];s.nodeLeft[node]=left;s.nodeRight[node]=right;s.nodeDepth[left]=s.nodeDepth[right]=s.nodeDepth[node]+1;Arrays.fill(s.nodeProb,10*node,10*node+10,0);
                for(int r=0;r<d.trainN;r++)if(s.assignment[r]==node)s.assignment[r]=vector(d,s,r)[s.nodeFeature[node]]<=s.nodeCut[node]?left:right;
            }
            finishTreeNode(s);return;
        }
        if(s.treeCutIndex>=s.cutN[s.treeFeature]){s.treeFeature++;s.treeCutIndex=0;s.treeRow=-1;Arrays.fill(s.leftCounts,0);Arrays.fill(s.rightCounts,0);return;}
        if(s.treeRow==-1)s.treeRow=0;
        if(s.treeRow<d.trainN){int r=s.treeRow++;if(s.assignment[r]==node){int y=d.rows[r].actual-1;if(vector(d,s,r)[s.treeFeature]<=s.cutValues[s.treeFeature*15+s.treeCutIndex])s.leftCounts[y]++;else s.rightCounts[y]++;}return;}
        int nl=sum(s.leftCounts),nr=sum(s.rightCounts);if(nl>=20&&nr>=20){int[] all=new int[10];for(int k=0;k<10;k++)all[k]=s.leftCounts[k]+s.rightCounts[k];double gain=gini(all)-(nl*gini(s.leftCounts)+nr*gini(s.rightCounts))/(nl+nr);
            if(gain>s.bestGain[node]+1e-12){s.bestGain[node]=gain;s.bestFeature[node]=s.treeFeature;s.bestCut[node]=s.cutValues[s.treeFeature*15+s.treeCutIndex];}}
        s.treeCutIndex++;s.treeRow=-1;Arrays.fill(s.leftCounts,0);Arrays.fill(s.rightCounts,0);
    }
    private static void finishTreeNode(State s){s.treeNode++;s.treeFeature=s.treeCutIndex=s.treeRow=0;Arrays.fill(s.leftCounts,0);Arrays.fill(s.rightCounts,0);}
    private static int sum(int[] a){int n=0;for(int x:a)n+=x;return n;}
    private static double gini(int[] a){int n=sum(a);double value=1;for(int x:a)value-=(x/(double)n)*(x/(double)n);return value;}
    private static int preorder(State s,int old,int[] feature,double[] cut,int[] left,int[] right,double[] probs,int at){
        int here=at++;feature[here]=s.nodeFeature[old];cut[here]=s.nodeCut[old];left[here]=right[here]=-1;System.arraycopy(s.nodeProb,old*10,probs,here*10,10);
        if(feature[here]>=0){left[here]=at;at=preorder(s,s.nodeLeft[old],feature,cut,left,right,probs,at);right[here]=at;at=preorder(s,s.nodeRight[old],feature,cut,left,right,probs,at);}return at;
    }
    private static Modern model(Dataset d,State s,double[] weights,String receipt){
        int n=d.shape.kind==ModelKind.HISTOGRAM_TREE?s.treeSize:0;int[] f=new int[n],l=new int[n],r=new int[n];double[] c=new double[n],p=new double[10*n];if(n>0)need(preorder(s,0,f,c,l,r,p,0)==n,"TRAIN_TREE_PREORDER");
        Modern m=new Modern(d.shape.kind,d.shape.family,d.shape.inputDimension,d.shape.featureN,d.shape.expertN,d.trainN,d.shape.seed,d.shape.trainedUntil,d.shape.featureManifestHash,Modern.normalizerHash(d.featureIds,s.mean,s.std),d.shape.graphHash,receipt,s.mean,s.std,weights,f,c,l,r,p,d.shape.experts);return m;
    }
    public static Model finish(Dataset d,State s,String receiptHash){requireIdentity(d,s);need(s.complete()&&s.hasBest&&s.validationN==120&&hash(receiptHash),"TRAIN_FINISH");Model m=Model.fromModern(model(d,s,s.best,receiptHash));need(Model.parse(m.payload()).hash().equals(m.hash()),"TRAIN_CODEC_REPLAY");return m;}
    public static byte[] checkpoint(Dataset d,State s){
        requireIdentity(d,s);
        try {ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(bytes);o.writeInt(0x5343434b);o.writeShort(2);o.writeUTF(s.protocol);o.writeUTF(s.input);o.writeUTF(s.rowsHash);o.writeUTF(s.shapeHash);o.writeByte(s.phase.ordinal());
            for(int x:new int[]{s.row,s.epoch,s.selectedEpoch,s.patience,s.batchN,s.validationN,s.hitN,s.bestHitN,s.treeFeatureCursor,s.treeNode,s.treeFeature,s.treeCutIndex,s.treeRow,s.treeSize})o.writeInt(x);
            o.writeLong(s.steps);o.writeLong(s.prng);o.writeBoolean(s.hasBest);for(double x:new double[]{s.ce,s.ceComp,s.brier,s.brierComp,s.bestCE,s.bestBrier})o.writeDouble(x);
            for(double[] a:new double[][]{s.mean,s.m2,s.std,s.weights,s.grad,s.m,s.v,s.best,s.nodeCut,s.nodeProb,s.cutValues,s.bestGain,s.bestCut})write(o,a);
            o.writeInt(s.counts.length);for(long n:s.counts)o.writeLong(n);
            for(int[] a:new int[][]{s.assignment,s.nodeFeature,s.nodeLeft,s.nodeRight,s.nodeDepth,s.leftCounts,s.rightCounts,s.cutN,s.bestFeature})write(o,a);
            write(o,s.tape);write(o,s.bestTape);o.flush();byte[] body=bytes.toByteArray();need(body.length+32<=MAX_CHECKPOINT,"TRAIN_CHECKPOINT_SIZE");bytes.write(sha().digest(body));return bytes.toByteArray();
        }catch(IOException impossible){throw new AssertionError(impossible);}
    }
    public static State restore(Dataset d,byte[] encoded){
        need(encoded!=null&&encoded.length>=64&&encoded.length<=MAX_CHECKPOINT,"TRAIN_CHECKPOINT_SIZE");int bodyN=encoded.length-32;MessageDigest digest=sha();digest.update(encoded,0,bodyN);need(MessageDigest.isEqual(digest.digest(),Arrays.copyOfRange(encoded,bodyN,encoded.length)),"TRAIN_CHECKPOINT_HASH");
        try {DataInputStream in=new DataInputStream(new ByteArrayInputStream(encoded,0,bodyN));need(in.readInt()==0x5343434b&&in.readUnsignedShort()==2,"TRAIN_CHECKPOINT_REVISION");State s=new State(d);
            s.protocol=readHash(in);s.input=readHash(in);s.rowsHash=readHash(in);s.shapeHash=readHash(in);int phase=in.readUnsignedByte();need(phase<Phase.values().length,"TRAIN_CHECKPOINT_PHASE");s.phase=Phase.values()[phase];
            s.row=in.readInt();s.epoch=in.readInt();s.selectedEpoch=in.readInt();s.patience=in.readInt();s.batchN=in.readInt();s.validationN=in.readInt();s.hitN=in.readInt();s.bestHitN=in.readInt();s.treeFeatureCursor=in.readInt();s.treeNode=in.readInt();s.treeFeature=in.readInt();s.treeCutIndex=in.readInt();s.treeRow=in.readInt();s.treeSize=in.readInt();
            s.steps=in.readLong();s.prng=in.readLong();s.hasBest=in.readBoolean();s.ce=finite(in.readDouble());s.ceComp=finite(in.readDouble());s.brier=finite(in.readDouble());s.brierComp=finite(in.readDouble());s.bestCE=finite(in.readDouble());s.bestBrier=finite(in.readDouble());
            for(double[] a:new double[][]{s.mean,s.m2,s.std,s.weights,s.grad,s.m,s.v,s.best,s.nodeCut,s.nodeProb,s.cutValues,s.bestGain,s.bestCut})read(in,a);
            need(in.readInt()==s.counts.length,"TRAIN_CHECKPOINT_ARRAY");for(int i=0;i<s.counts.length;i++){s.counts[i]=in.readLong();need(s.counts[i]>=0&&s.counts[i]<=d.trainN,"TRAIN_NORMALIZER_COUNT");}
            for(int[] a:new int[][]{s.assignment,s.nodeFeature,s.nodeLeft,s.nodeRight,s.nodeDepth,s.leftCounts,s.rightCounts,s.cutN,s.bestFeature})read(in,a);
            s.tape=readBytes(in);s.bestTape=readBytes(in);need(in.available()==0,"TRAIN_CHECKPOINT_TRAILING");requireIdentity(d,s);
            need(s.row>=0&&s.row<=d.trainN&&s.epoch>=0&&s.epoch<=19&&s.selectedEpoch>=0&&s.selectedEpoch<=s.epoch&&s.patience>=0&&s.patience<=3&&s.batchN>=0&&s.batchN<32&&s.validationN>=0&&s.validationN<=120&&s.hitN>=0&&s.hitN<=s.validationN&&s.bestHitN>=0&&s.bestHitN<=120&&s.steps>=0,"TRAIN_CHECKPOINT_CURSOR");
            need(s.treeSize>=1&&s.treeSize<=31&&s.treeNode>=0&&s.treeNode<=s.treeSize&&s.treeFeatureCursor>=0&&s.treeFeatureCursor<=d.shape.inputDimension&&s.treeFeature>=-1&&s.treeFeature<=d.shape.inputDimension&&s.treeCutIndex>=0&&s.treeCutIndex<=15&&s.treeRow>=-1&&s.treeRow<=d.trainN,"TRAIN_CHECKPOINT_TREE_CURSOR");
            if(s.phase.ordinal()>Phase.INITIALIZE.ordinal())for(int f=0;f<s.std.length;f++)need(s.std[f]>=1e-6&&s.counts[f]>0,"TRAIN_CHECKPOINT_NORMALIZER");
            need(Arrays.equals(encoded,checkpoint(d,s)),"TRAIN_CHECKPOINT_CANONICAL");return s;
        }catch(IOException e){throw new IllegalArgumentException("TRAIN_CHECKPOINT_TRUNCATED",e);}
    }
    private static String shapeIdentity(Dataset d){return V51ChecksumEngine.sha256("CP9_TRAIN_SHAPE|"+d.shape.kind+'|'+d.shape.family+'|'+d.shape.inputDimension+'|'+d.shape.featureN+'|'+d.shape.expertN+'|'+d.trainN+'|'+d.shape.seed+'|'+d.shape.trainedUntil+'|'+d.shape.featureManifestHash+'|'+d.shape.graphHash+'|'+d.featureIds+'|'+d.missingPolicies+'|'+expertIdentity(d.shape.experts));}
    private static String expertIdentity(List<ExpertRef> refs){StringBuilder b=new StringBuilder();for(ExpertRef e:refs)b.append(e.freezeHash).append('|').append(e.modelHash).append('|').append(e.featureManifestHash).append('|').append(e.position).append(';');return b.toString();}
    private static void requireIdentity(Dataset d,State s){need(d!=null&&s!=null&&s.protocol.equals(d.protocolHash)&&s.input.equals(d.inputHash)&&s.rowsHash.equals(d.rowsHash)&&s.shapeHash.equals(d.shapeHash),"TRAIN_CHECKPOINT_IDENTITY");}
    private static void write(DataOutputStream o,double[] a)throws IOException{o.writeInt(a.length);for(double x:a)o.writeDouble(finite(x));}
    private static void write(DataOutputStream o,int[] a)throws IOException{o.writeInt(a.length);for(int x:a)o.writeInt(x);}
    private static void write(DataOutputStream o,byte[] a)throws IOException{o.writeInt(a.length);o.write(a);}
    private static void read(DataInputStream in,double[] a)throws IOException{need(in.readInt()==a.length,"TRAIN_CHECKPOINT_ARRAY");for(int i=0;i<a.length;i++)a[i]=finite(in.readDouble());}
    private static void read(DataInputStream in,int[] a)throws IOException{need(in.readInt()==a.length,"TRAIN_CHECKPOINT_ARRAY");for(int i=0;i<a.length;i++)a[i]=in.readInt();}
    private static byte[] readBytes(DataInputStream in)throws IOException{int n=in.readInt();need(n>=0&&n<=MAX_TAPE&&n<=in.available(),"TRAIN_CHECKPOINT_TAPE");byte[] b=new byte[n];in.readFully(b);return b;}
    private static String readHash(DataInputStream in)throws IOException{need(in.readUnsignedShort()==64,"TRAIN_CHECKPOINT_HASH_LENGTH");byte[] b=new byte[64];in.readFully(b);String s=new String(b,java.nio.charset.StandardCharsets.US_ASCII);need(hash(s),"TRAIN_CHECKPOINT_HASH_FORMAT");return s;}
    private static void probabilities(double[] p){double sum=0;for(double x:p){need(Double.isFinite(x)&&x>0&&x<=1,"TRAIN_PROBABILITY");sum+=x;}need(Math.abs(sum-1)<=1e-9,"TRAIN_PROBABILITY_SUM");}
    private static double finite(double x){need(Double.isFinite(x),"TRAIN_CHECKPOINT_NONFINITE");return x;}
    private static boolean hash(String x){return x!=null&&x.matches("[0-9a-f]{64}");}
    private static MessageDigest sha(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    private static String hex(byte[] bytes){StringBuilder s=new StringBuilder(bytes.length*2);for(byte b:bytes){s.append(Character.forDigit((b&255)>>>4,16)).append(Character.forDigit(b&15,16));}return s.toString();}
    private static void need(boolean condition,String code){if(!condition)throw new IllegalArgumentException(code);}
}
