package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.util.*;
import java.util.zip.*;
/** Distinct executable, paired prequential research protocols. Exposed history never grants live authority. */
public final class ExpertExperimentR26 {
    private ExpertExperimentR26() {}
    public static final class Spec {
        public final String id,cluster,hypothesis,falsifier,ablation,left,right,operation;
        Spec(String id,String cluster,String h,String left,String right,String op,String f,String a){this.id=id;this.cluster=cluster;hypothesis=h;this.left=left;this.right=right;operation=op;falsifier=f;ablation=a;}
        public Map<String,Object> typedDiff(){Map<String,Object> d=new TreeMap<>();d.put("type","REPLACE_EXECUTABLE_RESEARCH_POLICY");d.put("left",genome(left).text());d.put("right",genome(right).text());d.put("operator",operation);d.put("inheritEvidence",false);return d;}
    }
    public static final List<Spec> SPECS=Collections.unmodifiableList(Arrays.asList(
        new Spec("E03","STATE_DURATION","Explicit duration can explain errors of a geometric state model","F08","F09","GEOMETRIC_TO_EXPLICIT_DURATION","No posterior segmentation or future state labels","Compare same-period duration filter and geometric HMM"),
        new Spec("E04","SCALE_DISAGREEMENT","A masked six-scale count encoder can outperform only the short scale","SHORT_SCALE","SIX_SCALE","ADD_PAST_ONLY_SCALES","Unavailable scales remain masked","Compare 8 versus 8/32/128/512/2048/5000; show each support count"),
        new Spec("E05","POSITION_INTERACTION","A trained triple hyperedge can explain sparse-edge position errors","F01","F03","SPARSE_EDGE_TO_HYPEREDGE","Expose same-period failures as well as gains","Sparse cross-position baseline versus hyperedge encoder"),
        new Spec("E06","SET_PATH_LOSS","Soft branches can preserve useful triples lost by a single set head","F15","F16","ADD_SOFT_SET_BRANCH","Utility is never treated as a probability","Compare final K3 and marginal top7/top5 retention"),
        new Spec("E07","REPRESENTATION_SATURATION","A selective recurrent state can explain causal convolution failures","F04","F06","TCN_TO_SELECTIVE_STATE","Training loss alone cannot promote a policy","Compare held-later prequential loss, action errors and bytes"),
        new Spec("E08","FORMULA_STAGNATION","A reusable causal macro can improve a stateless relation AST","F13","F14","EXTRACT_STATEFUL_TYPED_MACRO","Lag zero and untyped expressions cannot execute","Compare executable FEATURE AST with and without stateful macro"),
        new Spec("E09","CONFIDENT_RESIDUAL_FAILURE","A bounded residual after a frozen bootstrap can repair parent failures","F01","F22","ADD_FROZEN_PARENT_RESIDUAL","Evaluate failure cluster and its complement; never encode issue ID","Compare same-period parent and independent residual child"),
        new Spec("E10","CORRELATED_FAILURE","A contextual router can exploit genuine conditional complementarity","F01","F24","ADD_CONTEXTUAL_PORTFOLIO_ROUTER","Low correlation alone does not imply quality","Record joint failures and both exclusive successes"),
        new Spec("E11","SLOW_COLD_CHILD","Original immutable teacher preferences can accelerate a new student","F17","F23","BOUND_ORIGINAL_STATE_DISTILLATION","Missing teacher is explicit; no inherited hits or qualification","Compare real-label-only matching student and bound distillation"),
        new Spec("E12","CALIBRATION_COVERAGE","Selection-aware calibration and coverage expose overconfidence","COVERAGE15","COVERAGE30","CHANGE_SELECTIVE_GATE_AND_FIT_HELD_LATER_CALIBRATOR","Threshold increases do not prove quality","Report raw and held-later Platt ECE/Brier plus coverage frontier"),
        new Spec("E13","BLOCK_DRIFT","Change-point inference can explain explicit-duration block deterioration","F09","F10","DURATION_TO_CHANGEPOINT","Research streak diagnostics never reset the global REAL streak","Compare fixed blocks, worst block, action streaks and drift"),
        new Spec("E14","RESOURCE_CAPACITY","A smaller exact matching learner may meet a missed encoder budget","F05","F17","DISTILLATION_CAPACITY_PROPOSAL","Capacity failure must not be called statistical no advantage","Measure serialized bytes and paired wall cost; no automatic replacement")
    ));
    public static Spec spec(String id){for(Spec s:SPECS)if(s.id.equals(id))return s;throw new IllegalArgumentException("R26_EXPERT_PROTOCOL:"+id);}
    public static ModelGenomeSU2 genome(String code){
        if(code.equals("SHORT_SCALE")||code.equals("SIX_SCALE")){Map<String,String> p=new TreeMap<>();p.put("abi","1");p.put("mode",code.equals("SIX_SCALE")?"SIX":"SHORT");return new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member("R26SCALE",p,1)),"STATIC",true,"MASS",.15,.2,false);}
        if(code.startsWith("COVERAGE")){ModelGenomeSU2 g=FamilyRegistryR26.genome("F01");return new ModelGenomeSU2(g.members,g.pool,g.sink,g.gate,code.equals("COVERAGE15")?.15:.30,g.metaC,g.calibrated);}
        return FamilyRegistryR26.genome(code);
    }
    /** Full private pair plus metrics, serialized after completed samples only. */
    public static final class Pair {
        public final Spec spec;public final LiveStateR26 left,right;
        public int cursor,gaps,novel,jointFailure,leftOnly,rightOnly;
        public final int[] actions=new int[2],hits=new int[2],streak=new int[2],maxStreak=new int[2];
        public final double[] loss=new double[2],brier=new double[2];
        public final int[][] topRetention=new int[2][3],blockActions=new int[2][40],blockHits=new int[2][40];
        public final double[][] probability=new double[2][5000];public final boolean[][] outcome=new boolean[2][5000];
        public final int[] probabilityN=new int[2];
        public Pair(Spec s,String snapshot,long through){spec=s;left=new LiveStateR26(genome(s.left).bindBirth(snapshot,through));right=new LiveStateR26(genome(s.right).bindBirth(snapshot,through));}
        private Pair(Spec s,LiveStateR26 a,LiveStateR26 b){spec=s;left=a;right=b;}
        public void advance(int[] draw,boolean gap)throws IOException{
            OnlineModelsSU2.requireDraw(draw);if(cursor>=5000)throw new IOException("R26_EXPERT_CAPACITY");
            if(gap){left.onGap();right.onGap();gaps++;}
            LiveStateR26[] models={left,right};ModelLiveSU2.Decision[] decision={left.forecast(),right.forecast()};boolean[] good=new boolean[2];
            for(int side=0;side<2;side++){
                ModelLiveSU2.Decision d=decision[side];float[][] p=models[side].table();good[side]=(d.mask&(1<<(draw[d.position-1]-1)))!=0;
                if(cursor>=32){for(int pos=0;pos<10;pos++)loss[side]-=Math.log(Math.max(1e-12,p[pos][draw[pos]-1]))/10;
                    Integer[] rank=new Integer[10];for(int n=0;n<10;n++)rank[n]=n;final float[] row=p[d.position-1];Arrays.sort(rank,(a,b)->Float.compare(row[b],row[a]));for(int k=0;k<3;k++){int size=new int[]{7,5,3}[k];for(int j=0;j<size;j++)if(rank[j]+1==draw[d.position-1]){topRetention[side][k]++;break;}}
                    if(d.act){int block=Math.min(39,cursor/128);actions[side]++;blockActions[side][block]++;if(good[side]){hits[side]++;blockHits[side][block]++;streak[side]=0;}else{streak[side]++;maxStreak[side]=Math.max(maxStreak[side],streak[side]);}double q=Double.isFinite(d.phat)?d.phat:0.3;int at=probabilityN[side]++;probability[side][at]=q;outcome[side][at]=good[side];double error=q-(good[side]?1:0);brier[side]+=error*error;}
                }
            }
            if(cursor>=32){if(decision[0].position!=decision[1].position||decision[0].mask!=decision[1].mask||decision[0].act!=decision[1].act)novel++;if(!good[0]&&!good[1])jointFailure++;if(good[0]&&!good[1])leftOnly++;if(!good[0]&&good[1])rightOnly++;}
            left.learn(draw);right.learn(draw);cursor++;
        }
        public byte[] encode()throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(new GZIPOutputStream(bytes))){out.writeInt(0x26e10001);out.writeUTF(spec.id);for(LiveStateR26 model:new LiveStateR26[]{left,right}){out.writeUTF(model.genome.text());byte[] state=model.encode();out.writeInt(state.length);out.write(state);}for(int x:new int[]{cursor,gaps,novel,jointFailure,leftOnly,rightOnly})out.writeInt(x);for(int[] a:new int[][]{actions,hits,streak,maxStreak,probabilityN})for(int x:a)out.writeInt(x);for(double[] a:new double[][]{loss,brier})for(double x:a)out.writeDouble(x);for(int[][] a:new int[][][]{topRetention,blockActions,blockHits})for(int[] row:a)for(int x:row)out.writeInt(x);for(int side=0;side<2;side++)for(int i=0;i<probabilityN[side];i++){out.writeDouble(probability[side][i]);out.writeBoolean(outcome[side][i]);}}if(bytes.size()>8<<20)throw new IOException("R26_EXPERT_STATE_CAPACITY");return bytes.toByteArray();}
        public static Pair decode(byte[] bytes)throws IOException{if(bytes==null||bytes.length>8<<20)throw new IOException("R26_EXPERT_STATE_CAPACITY");try(DataInputStream in=new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes)))){if(in.readInt()!=0x26e10001)throw new IOException("R26_EXPERT_ABI");Spec s=spec(in.readUTF());LiveStateR26[] states=new LiveStateR26[2];for(int side=0;side<2;side++){String text=in.readUTF();int n=in.readInt();if(n<1||n>4<<20)throw new IOException("R26_EXPERT_MODEL_BOUND");byte[] b=new byte[n];in.readFully(b);states[side]=LiveStateR26.decode(text,b,LiveStateR26.hash(b));}Pair p=new Pair(s,states[0],states[1]);p.cursor=in.readInt();p.gaps=in.readInt();p.novel=in.readInt();p.jointFailure=in.readInt();p.leftOnly=in.readInt();p.rightOnly=in.readInt();if(p.cursor<0||p.cursor>5000||p.left.samples()!=p.cursor||p.right.samples()!=p.cursor)throw new IOException("R26_EXPERT_CURSOR");for(int[] a:new int[][]{p.actions,p.hits,p.streak,p.maxStreak,p.probabilityN})for(int i=0;i<a.length;i++){a[i]=in.readInt();if(a[i]<0||a[i]>5000)throw new IOException("R26_EXPERT_COUNTS");}for(double[] a:new double[][]{p.loss,p.brier})for(int i=0;i<a.length;i++){a[i]=in.readDouble();if(!Double.isFinite(a[i]))throw new IOException("R26_EXPERT_NUMERICS");}for(int[][] a:new int[][][]{p.topRetention,p.blockActions,p.blockHits})for(int[] row:a)for(int i=0;i<row.length;i++)row[i]=in.readInt();for(int side=0;side<2;side++)for(int i=0;i<p.probabilityN[side];i++){p.probability[side][i]=in.readDouble();p.outcome[side][i]=in.readBoolean();if(!Double.isFinite(p.probability[side][i])||p.probability[side][i]<0||p.probability[side][i]>1)throw new IOException("R26_EXPERT_PROBABILITY");}if(in.read()!=-1)throw new IOException("R26_EXPERT_TRAILING");return p;}}
    }
}
