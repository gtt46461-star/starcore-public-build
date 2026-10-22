package com.ethk3.novaonepro.strategyv2;

import java.util.*;
import java.io.*;

/** RELGRAPH10 V1: typed event features and directed, role-labelled hyperedges enter the full BPTT path. */
public final class RelGraphModelSU1 {
    public static final String PROTOCOL="SU1_RELGRAPH10_V1|FLOAT64|WINDOW_RESET|ISSUE_MEAN_CE|IMMUTABLE_FACTS|CANONICAL_EDGES";
    public static final String[] GROUPS={"E","U","R","Wx","Wm","Wh","b","Wo","c","Wf","vo","Wrole","wv","btype","utype","vtype","Wmsg","vout"};
    private RelGraphModelSU1() {}
    public static final class Edge {
        public final int receiver,type,support;public final double value;public final long sourceIssue;
        public final String sourceRoot;
        private final int[] sources,roles;
        public Edge(int receiver,int type,int[] sources,int[] roles,double value,int support,long issue,String root){
            if(receiver<0||receiver>=100||type<0||type>=16||sources==null||roles==null||sources.length<1||sources.length>4||sources.length!=roles.length||!Double.isFinite(value)||Math.abs(value)>100||support<0||issue<1)throw new IllegalArgumentException("SU1_GRAPH_EDGE");
            this.receiver=receiver;this.type=type;this.sources=sources.clone();this.roles=roles.clone();this.value=value;this.support=support;sourceIssue=issue;sourceRoot=HashingSU1.sha(root);
            for(int i=0;i<sources.length;i++){if(sources[i]<0||sources[i]>=100||roles[i]<0||roles[i]>=16)throw new IllegalArgumentException("SU1_GRAPH_EDGE_ROLE");for(int j=0;j<i;j++)if(sources[j]==sources[i]&&roles[j]==roles[i])throw new IllegalArgumentException("SU1_GRAPH_DUPLICATE_SOURCE");}
        }
        String key(int[] positions,int[] numbers){List<String> input=new ArrayList<>();for(int i=0;i<sources.length;i++)input.add(roles[i]+":"+positions[sources[i]]+":"+numbers[sources[i]]);Collections.sort(input);return positions[receiver]+":"+numbers[receiver]+"|"+type+"|"+input+"|"+sourceIssue+"|"+sourceRoot;}
    }
    public static final class Step {
        public final long issue,availableAt;
        public final String receiptRoot;
        public final int channels;
        private final int[] draw,positions,numbers,actualNodes;
        private final double[] features;
        private final boolean[] mask;
        private final Edge[] edges;
        public Step(long issue,long availableAt,String root,int[] draw,int[] positions,int[] numbers,double[] features,boolean[] mask,int channels,Edge[] edges){
            if(issue<1||availableAt<1||positions==null||numbers==null||features==null||mask==null||edges==null||positions.length!=100||numbers.length!=100||channels<1||channels>16||features.length!=100*channels||mask.length!=features.length||edges.length>2048)throw new IllegalArgumentException("SU1_GRAPH_STEP_SHAPE");
            RelationalStateInference.validateDraw(draw);this.issue=issue;this.availableAt=availableAt;receiptRoot=HashingSU1.sha(root);this.channels=channels;
            this.draw=draw.clone();this.positions=positions.clone();this.numbers=numbers.clone();this.features=features.clone();this.mask=mask.clone();actualNodes=new int[10];Set<Integer> nodes=new HashSet<>();
            for(int node=0;node<100;node++){
                int p=positions[node],n=numbers[node];if(p<0||p>=10||n<0||n>=10||!nodes.add(p*10+n))throw new IllegalArgumentException("SU1_GRAPH_EVENT_AXES");
                if(draw[p]==n+1)actualNodes[p]=node;
            }
            for(double v:features)if(!Double.isFinite(v)||Math.abs(v)>100)throw new IllegalArgumentException("SU1_GRAPH_FEATURE_VALUE");
            this.edges=edges.clone();Set<String> keys=new HashSet<>();
            for(Edge e:this.edges)if(e==null||e.sourceIssue>issue||!receiptRoot.equals(e.sourceRoot)||!keys.add(e.key(positions,numbers)))throw new IllegalArgumentException("SU1_GRAPH_FUTURE_OR_DUPLICATE_EDGE");
            Arrays.sort(this.edges,Comparator.comparing(e->e.key(positions,numbers)));
        }
        public String inputHash(){StringBuilder b=new StringBuilder(issue+"|"+availableAt+"|"+receiptRoot);for(int node=0;node<100;node++){b.append('|').append(positions[node]).append(':').append(numbers[node]);for(int c=0;c<channels;c++)b.append(':').append(mask[node*channels+c]).append(':').append(Double.toHexString(features[node*channels+c]));}for(int n:draw)b.append('|').append(n);for(Edge e:edges)b.append('|').append(e.key(positions,numbers)).append(':').append(Double.toHexString(e.value)).append(':').append(e.support);return HashingSU1.text(b.toString());}
    }
    public static final class Input {
        public final String featureSchemaHash,hash;
        public final long targetIssue,cutoff;
        public final String provenance;
        private final Step[] steps;
        public Input(String schema,Step... steps){
            this(schema,steps.length==0?0:steps[steps.length-1].issue+1,latestAvailability(steps),"HISTORICAL_RECONSTRUCTED",steps);
        }
        private static long latestAvailability(Step[] steps){long result=0;for(Step step:steps)if(step!=null)result=Math.max(result,step.availableAt);return result;}
        public Input(String schema,long target,long cutoff,String provenance,Step... steps){
            featureSchemaHash=HashingSU1.sha(schema);if(steps.length<1||steps.length>256||target<1||cutoff<1||!Arrays.asList("HISTORICAL_RECONSTRUCTED","ONLINE_FROZEN").contains(provenance))throw new IllegalArgumentException("SU1_GRAPH_WINDOW");this.steps=steps.clone();targetIssue=target;this.cutoff=cutoff;this.provenance=provenance;StringBuilder b=new StringBuilder(PROTOCOL).append('|').append(schema).append('|').append(target).append('|').append(cutoff).append('|').append(provenance);long prior=0;
            for(Step s:this.steps){if(s==null||s.issue<=prior||s.issue>=target||s.availableAt>cutoff||s.channels!=steps[0].channels)throw new IllegalArgumentException("SU1_GRAPH_TIME_ORDER_OR_AVAILABILITY");prior=s.issue;b.append('|').append(s.inputHash());}hash=HashingSU1.text(b.toString());
        }
        public int length(){return steps.length;}
    }
    public static final class Weights {
        public final int d,channels,roleCount,typeCount;
        public final String featureSchemaHash;
        final Map<String,double[]> tensors;
        public Weights(int d,int channels,int roles,int types,String schema,Map<String,double[]> tensors){
            if(d<2||d>16||channels<1||channels>16||roles<1||roles>16||types<1||types>16)throw new IllegalArgumentException("SU1_GRAPH_WEIGHT_SHAPE");
            this.d=d;this.channels=channels;roleCount=roles;typeCount=types;featureSchemaHash=HashingSU1.sha(schema);Map<String,double[]> copy=new LinkedHashMap<>();Map<String,Integer> sizes=sizes(d,channels,roles,types);
            if(!tensors.keySet().equals(sizes.keySet()))throw new IllegalArgumentException("SU1_GRAPH_PARAMETER_KEYS");
            for(String key:GROUPS){double[] values=tensors.get(key);if(values==null||values.length!=sizes.get(key))throw new IllegalArgumentException("SU1_GRAPH_TENSOR_SHAPE:"+key);values=values.clone();for(double v:values)if(!Double.isFinite(v))throw new IllegalArgumentException("SU1_GRAPH_NONFINITE_WEIGHT");copy.put(key,values);}this.tensors=Collections.unmodifiableMap(copy);
        }
        double[] get(String key){return tensors.get(key);}
        public Map<String,double[]> parameters(){Map<String,double[]> out=new LinkedHashMap<>();for(String key:GROUPS)out.put(key,get(key).clone());return out;}
    }
    static Map<String,Integer> sizes(int d,int channels,int roles,int types){
        Map<String,Integer> m=new LinkedHashMap<>();int[] sizes={10*d,10*d,100,d*d,d*d,d*d,d,d*10,10,2*channels*d,d,roles*d*d,types*d,types*d,types*d,types*d,d*d,d};for(int i=0;i<GROUPS.length;i++)m.put(GROUPS[i],sizes[i]);return m;
    }
    public static Weights initial(int d,int channels,int roles,int types,String schema,long seed){Map<String,double[]> values=new LinkedHashMap<>();Random random=new Random(seed);for(Map.Entry<String,Integer> e:sizes(d,channels,roles,types).entrySet()){double[] v=new double[e.getValue()];for(int i=0;i<v.length;i++)v[i]=random.nextGaussian()*(e.getKey().equals("b")||e.getKey().equals("c")||e.getKey().equals("btype")?.01:.15);values.put(e.getKey(),v);}return new Weights(d,channels,roles,types,schema,values);}
    private static final class Tape {
        final double[] x,q,a,aggregate,v,z,m,h;
        Tape(int d,int edges){x=new double[100*d];q=new double[edges*d];a=new double[edges];aggregate=new double[100*d];v=new double[100*d];z=new double[10*d];m=new double[10*d];h=new double[10*d];}
    }
    public interface Trace {void layer(String name,int step,double[] values);}
    private static final class Forward {
        final Tape[] tape;final double[] relation;final double[][] probabilities;
        Forward(Tape[] tape,double[] relation,double[][] probabilities){this.tape=tape;this.relation=relation;this.probabilities=probabilities;}
    }
    public static long memoryBytes(Weights w,Input input){long bytes=0;for(double[] values:w.tensors.values())bytes+=values.length*8L*4;for(Step step:input.steps)bytes+=8L*(step.edges.length*(w.d+1L)+100L*w.d*4+30L*w.d+100L*w.channels*2)+step.edges.length*256L;return bytes;}
    private static Forward forward(Weights w,Input input,BudgetTokenSU1 budget,Trace trace){
        if(!w.featureSchemaHash.equals(input.featureSchemaHash)||memoryBytes(w,input)>32L<<20)throw new IllegalArgumentException("SU1_GRAPH_SCHEMA_OR_MEMORY");
        int d=w.d,c=w.channels;Tape[] tape=new Tape[input.steps.length];double[] relation=new double[100];
        for(int p=0;p<10;p++){double[] row=new double[10];System.arraycopy(w.get("R"),p*10,row,0,10);row=RelationalTrainerSU1.softmax(row);System.arraycopy(row,0,relation,p*10,10);}
        double[] previous=new double[10*d];
        for(int t=0;t<tape.length;t++){
            budget.checkpointOrYield();Step step=input.steps[t];if(step.channels!=c)throw new IllegalArgumentException("SU1_GRAPH_FEATURE_CHANNELS");Tape a=new Tape(d,step.edges.length);tape[t]=a;
            for(Edge edge:step.edges){if(edge.type>=w.typeCount)throw new IllegalArgumentException("SU1_GRAPH_EDGE_TYPE");for(int role:edge.roles)if(role>=w.roleCount)throw new IllegalArgumentException("SU1_GRAPH_ROLE_COUNT");}
            for(int node=0;node<100;node++)for(int j=0;j<d;j++){
                double value=w.get("E")[step.numbers[node]*d+j]+w.get("U")[step.positions[node]*d+j];
                for(int f=0;f<c;f++)if(step.mask[node*c+f])value+=step.features[node*c+f]*w.get("Wf")[f*d+j]+w.get("Wf")[(c+f)*d+j];
                if(step.actualNodes[step.positions[node]]==node)value+=w.get("vo")[j];if(!Double.isFinite(value))throw new IllegalArgumentException("SU1_GRAPH_NONFINITE_EVENT");a.x[node*d+j]=value;
            }
            double[] edgeScores=new double[step.edges.length],max=new double[100],sum=new double[100];Arrays.fill(max,-Double.MAX_VALUE);
            for(int e=0;e<step.edges.length;e++){
                if((e&63)==0)budget.checkpointOrYield();Edge edge=step.edges[e];if(edge.type>=w.typeCount)throw new IllegalArgumentException("SU1_GRAPH_EDGE_TYPE");if(edge.support==0)continue;
                for(int j=0;j<d;j++){
                    double value=edge.value*w.get("wv")[edge.type*d+j]+w.get("btype")[edge.type*d+j];
                    for(int source=0;source<edge.sources.length;source++){int role=edge.roles[source];if(role>=w.roleCount)throw new IllegalArgumentException("SU1_GRAPH_ROLE_COUNT");for(int k=0;k<d;k++)value+=a.x[edge.sources[source]*d+k]*w.get("Wrole")[(role*d+k)*d+j]/edge.sources.length;}
                    if(!Double.isFinite(value))throw new IllegalArgumentException("SU1_GRAPH_NONFINITE_EDGE");a.q[e*d+j]=Math.tanh(value);edgeScores[e]+=a.x[edge.receiver*d+j]*w.get("utype")[edge.type*d+j]+a.q[e*d+j]*w.get("vtype")[edge.type*d+j];
                }if(!Double.isFinite(edgeScores[e]))throw new IllegalArgumentException("SU1_GRAPH_NONFINITE_ATTENTION");max[edge.receiver]=Math.max(max[edge.receiver],edgeScores[e]);
            }
            for(int e=0;e<step.edges.length;e++){Edge edge=step.edges[e];if(edge.support>0){a.a[e]=Math.exp(edgeScores[e]-max[edge.receiver]);sum[edge.receiver]+=a.a[e];}}
            for(int e=0;e<step.edges.length;e++){Edge edge=step.edges[e];if(edge.support>0){a.a[e]/=sum[edge.receiver];for(int j=0;j<d;j++)a.aggregate[edge.receiver*d+j]+=a.a[e]*a.q[e*d+j];}}
            for(int node=0;node<100;node++)for(int j=0;j<d;j++){double value=a.x[node*d+j];for(int k=0;k<d;k++)value+=a.aggregate[node*d+k]*w.get("Wmsg")[k*d+j];if(!Double.isFinite(value))throw new IllegalArgumentException("SU1_GRAPH_NONFINITE_VALUE");a.v[node*d+j]=value;}
            for(int p=0;p<10;p++)System.arraycopy(a.v,step.actualNodes[p]*d,a.z,p*d,d);
            for(int p=0;p<10;p++)for(int q=0;q<10;q++)for(int j=0;j<d;j++)a.m[p*d+j]+=relation[p*10+q]*a.z[q*d+j];
            for(int p=0;p<10;p++)for(int j=0;j<d;j++){double value=w.get("b")[j];for(int k=0;k<d;k++)value+=a.z[p*d+k]*w.get("Wx")[k*d+j]+a.m[p*d+k]*w.get("Wm")[k*d+j]+previous[p*d+k]*w.get("Wh")[k*d+j];if(!Double.isFinite(value))throw new IllegalArgumentException("SU1_GRAPH_NONFINITE_STATE");a.h[p*d+j]=Math.tanh(value);}previous=a.h;
            if(trace!=null){trace.layer("event",t,a.x.clone());trace.layer("edge",t,a.q.clone());trace.layer("attention",t,a.a.clone());trace.layer("value",t,a.v.clone());trace.layer("state",t,a.h.clone());}
        }
        double[][] probabilities=new double[10][10];Tape last=tape[tape.length-1];Step step=input.steps[input.steps.length-1];
        for(int p=0;p<10;p++){
            double[] logits=w.get("c").clone();for(int n=0;n<10;n++)for(int j=0;j<d;j++)logits[n]+=last.h[p*d+j]*w.get("Wo")[j*10+n];
            for(int node=0;node<100;node++)if(step.positions[node]==p)for(int j=0;j<d;j++)logits[step.numbers[node]]+=last.v[node*d+j]*w.get("vout")[j];for(double value:logits)if(!Double.isFinite(value))throw new IllegalArgumentException("SU1_GRAPH_NONFINITE_LOGIT");probabilities[p]=RelationalTrainerSU1.softmax(logits);
        }
        return new Forward(tape,relation,probabilities);
    }
    public static double[][] predict(Weights weights,Input input,BudgetTokenSU1 budget){return forward(weights,input,budget,null).probabilities;}
    public static double[][] predict(Weights weights,Input input,BudgetTokenSU1 budget,Trace trace){return forward(weights,input,budget,trace).probabilities;}
    /** Controlled historical manipulations are explicitly reconstructed research inputs, never online facts. */
    public static Input controlInput(Input input,String mode,long seed){
        if(!Arrays.asList("NO_RELATION","SHUFFLED_RELATION","REPLACED_RELATION").contains(mode))throw new IllegalArgumentException("SU1_GRAPH_ABLATION_MODE");
        Step[] steps=new Step[input.steps.length];
        for(int t=0;t<steps.length;t++){
            Step old=input.steps[t];String root=HashingSU1.fields("SU1_GRAPH_CONTROL_V1",old.receiptRoot,mode,String.valueOf(seed));double[] features=old.features.clone();boolean[] mask=old.mask.clone();List<Edge> edges=new ArrayList<>();
            int[] permutation=new int[10];for(int p=0;p<10;p++)permutation[p]=p;
            if(mode.equals("SHUFFLED_RELATION")){Random random=new Random(seed^old.issue);for(int p=9;p>0;p--){int q=random.nextInt(p+1),v=permutation[p];permutation[p]=permutation[q];permutation[q]=v;}}
            int[] remap=new int[100];for(int node=0;node<100;node++){int wantedPosition=permutation[old.positions[node]],wantedNumber=old.numbers[node];for(int other=0;other<100;other++)if(old.positions[other]==wantedPosition&&old.numbers[other]==wantedNumber){remap[node]=other;break;}}
            for(int node=0;node<100;node++)for(int c=1;c<old.channels;c++){
                if(mode.equals("NO_RELATION")){features[node*old.channels+c]=0;mask[node*old.channels+c]=false;}
                else if(mode.equals("REPLACED_RELATION")){features[node*old.channels+c]=.1;}
                else{features[remap[node]*old.channels+c]=old.features[node*old.channels+c];mask[remap[node]*old.channels+c]=old.mask[node*old.channels+c];}
            }
            if(!mode.equals("NO_RELATION"))for(Edge edge:old.edges){int[] sources=edge.sources.clone();for(int i=0;i<sources.length;i++)sources[i]=remap[sources[i]];edges.add(new Edge(remap[edge.receiver],edge.type,sources,edge.roles,mode.equals("REPLACED_RELATION")?0:edge.value,edge.support,edge.sourceIssue,root));}
            steps[t]=new Step(old.issue,old.availableAt,root,old.draw,old.positions,old.numbers,features,mask,old.channels,edges.toArray(new Edge[0]));
        }
        return new Input(input.featureSchemaHash,input.targetIssue,input.cutoff,"HISTORICAL_RECONSTRUCTED",steps);
    }
    public static double[][] frequencyBaseline(Input input){
        Step step=input.steps[input.steps.length-1];double[][] probabilities=new double[10][10];for(int node=0;node<100;node++)probabilities[step.positions[node]][step.numbers[node]]=.01+(step.mask[node*step.channels]?Math.max(0,step.features[node*step.channels]):0);
        for(double[] row:probabilities){double total=0;for(double v:row)total+=v;for(int n=0;n<10;n++)row[n]/=total;}return probabilities;
    }
    public static final class Gradient {
        public final double loss;public double crossEntropyLoss,brierLoss,smoothnessLoss,l2Loss,resourceLoss;public final double[][] probabilities;final Map<String,double[]> tensors;
        Gradient(double loss,double[][] probabilities,Map<String,double[]> tensors){this.loss=loss;this.probabilities=probabilities;this.tensors=tensors;}
        public Map<String,double[]> parameters(){Map<String,double[]> out=new LinkedHashMap<>();for(String key:GROUPS)out.put(key,tensors.get(key).clone());return out;}
        public double norm(String key){double sum=0;for(double v:tensors.get(key))sum+=v*v;return Math.sqrt(sum);}
    }
    public static Gradient gradient(Weights w,Input input,int[] actual,BudgetTokenSU1 budget){return gradient(w,input,actual,budget,TrainingObjectiveSU1.crossEntropy());}
    public static Gradient gradient(Weights w,Input input,int[] actual,BudgetTokenSU1 budget,TrainingObjectiveSU1 objective){
        if(objective==null)throw new IllegalArgumentException("SU1_GRAPH_OBJECTIVE_REQUIRED");
        RelationalStateInference.validateDraw(actual);Forward f=forward(w,input,budget,null);int d=w.d,c=w.channels,length=f.tape.length;double ce=0,brier=0,smooth=0,l2=0;long parameters=0;
        for(String key:GROUPS){parameters+=w.get(key).length;if(objective.l2>0)for(double value:w.get(key))l2+=value*value/2;}
        l2/=parameters;
        for(int t=0;t<length;t++){double[] previous=t==0?new double[10*d]:f.tape[t-1].h;for(int i=0;i<10*d;i++){double change=f.tape[t].h[i]-previous[i];smooth+=change*change/(length*10*d);}}
        for(int p=0;p<10;p++)for(int n=0;n<10;n++){double error=f.probabilities[p][n]-(actual[p]==n+1?1:0);brier+=error*error/10;if(actual[p]==n+1)ce-=Math.log(Math.max(1e-300,f.probabilities[p][n]))/10;}
        double loss=ce+objective.brier*brier+objective.smoothness*smooth+objective.l2*l2+objective.resource*parameters;
        Map<String,double[]> g=new LinkedHashMap<>();for(String key:GROUPS)g.put(key,new double[w.get(key).length]);double[][] delta=new double[10][10];double[] dh=new double[10*d],gr=new double[100],lastDv=new double[100*d];Tape last=f.tape[length-1];Step lastStep=input.steps[length-1];
        for(int p=0;p<10;p++){
            double weightedError=0;for(int n=0;n<10;n++)weightedError+=f.probabilities[p][n]*(f.probabilities[p][n]-(actual[p]==n+1?1:0));
            for(int n=0;n<10;n++){
                double error=f.probabilities[p][n]-(actual[p]==n+1?1:0),value=error/10+objective.brier*2*f.probabilities[p][n]*(error-weightedError)/10;delta[p][n]=value;g.get("c")[n]+=value;
                for(int j=0;j<d;j++){g.get("Wo")[j*10+n]+=last.h[p*d+j]*value;dh[p*d+j]+=value*w.get("Wo")[j*10+n];}
            }
        }
        for(int node=0;node<100;node++)for(int j=0;j<d;j++){double value=delta[lastStep.positions[node]][lastStep.numbers[node]];lastDv[node*d+j]+=value*w.get("vout")[j];g.get("vout")[j]+=value*last.v[node*d+j];}
        for(int t=length-1;t>=0;t--){
            budget.checkpointOrYield();Step step=input.steps[t];Tape a=f.tape[t];double[] previous=t==0?new double[10*d]:f.tape[t-1].h,dprevious=new double[10*d],dz=new double[10*d],dm=new double[10*d],dv=t==length-1?lastDv:new double[100*d];
            for(int i=0;i<10*d;i++){double change=2*objective.smoothness*(a.h[i]-previous[i])/(length*10*d);dh[i]+=change;dprevious[i]-=change;}
            for(int p=0;p<10;p++)for(int j=0;j<d;j++){
                double da=dh[p*d+j]*(1-a.h[p*d+j]*a.h[p*d+j]);g.get("b")[j]+=da;
                for(int k=0;k<d;k++){g.get("Wx")[k*d+j]+=a.z[p*d+k]*da;g.get("Wm")[k*d+j]+=a.m[p*d+k]*da;g.get("Wh")[k*d+j]+=previous[p*d+k]*da;dz[p*d+k]+=w.get("Wx")[k*d+j]*da;dm[p*d+k]+=w.get("Wm")[k*d+j]*da;dprevious[p*d+k]+=w.get("Wh")[k*d+j]*da;}
            }
            for(int p=0;p<10;p++)for(int q=0;q<10;q++)for(int j=0;j<d;j++){gr[p*10+q]+=dm[p*d+j]*a.z[q*d+j];dz[q*d+j]+=f.relation[p*10+q]*dm[p*d+j];}
            for(int p=0;p<10;p++)for(int j=0;j<d;j++)dv[step.actualNodes[p]*d+j]+=dz[p*d+j];
            double[] dx=dv.clone(),daggregate=new double[100*d],dq=new double[step.edges.length*d],da=new double[step.edges.length],weighted=new double[100];
            for(int node=0;node<100;node++)for(int j=0;j<d;j++)for(int k=0;k<d;k++){g.get("Wmsg")[k*d+j]+=a.aggregate[node*d+k]*dv[node*d+j];daggregate[node*d+k]+=dv[node*d+j]*w.get("Wmsg")[k*d+j];}
            for(int e=0;e<step.edges.length;e++){Edge edge=step.edges[e];if(edge.support==0)continue;for(int j=0;j<d;j++){da[e]+=daggregate[edge.receiver*d+j]*a.q[e*d+j];dq[e*d+j]+=a.a[e]*daggregate[edge.receiver*d+j];}weighted[edge.receiver]+=a.a[e]*da[e];}
            for(int e=0;e<step.edges.length;e++){
                if((e&63)==0)budget.checkpointOrYield();Edge edge=step.edges[e];if(edge.support==0)continue;double ds=a.a[e]*(da[e]-weighted[edge.receiver]);
                for(int j=0;j<d;j++){
                    dx[edge.receiver*d+j]+=ds*w.get("utype")[edge.type*d+j];g.get("utype")[edge.type*d+j]+=ds*a.x[edge.receiver*d+j];g.get("vtype")[edge.type*d+j]+=ds*a.q[e*d+j];dq[e*d+j]+=ds*w.get("vtype")[edge.type*d+j];
                    double pre=dq[e*d+j]*(1-a.q[e*d+j]*a.q[e*d+j]);g.get("wv")[edge.type*d+j]+=edge.value*pre;g.get("btype")[edge.type*d+j]+=pre;
                    for(int source=0;source<edge.sources.length;source++)for(int k=0;k<d;k++){int role=edge.roles[source],node=edge.sources[source],at=(role*d+k)*d+j;g.get("Wrole")[at]+=a.x[node*d+k]*pre/edge.sources.length;dx[node*d+k]+=pre*w.get("Wrole")[at]/edge.sources.length;}
                }
            }
            for(int node=0;node<100;node++)for(int j=0;j<d;j++){
                double value=dx[node*d+j];g.get("E")[step.numbers[node]*d+j]+=value;g.get("U")[step.positions[node]*d+j]+=value;
                if(step.actualNodes[step.positions[node]]==node)g.get("vo")[j]+=value;
                for(int channel=0;channel<c;channel++)if(step.mask[node*c+channel]){g.get("Wf")[channel*d+j]+=step.features[node*c+channel]*value;g.get("Wf")[(c+channel)*d+j]+=value;}
            }dh=dprevious;
        }
        for(int p=0;p<10;p++){double mean=0;for(int q=0;q<10;q++)mean+=gr[p*10+q]*f.relation[p*10+q];for(int q=0;q<10;q++)g.get("R")[p*10+q]=f.relation[p*10+q]*(gr[p*10+q]-mean);}
        if(objective.l2>0)for(String key:GROUPS)for(int i=0;i<w.get(key).length;i++)g.get(key)[i]+=objective.l2*w.get(key)[i]/parameters;
        if(!Double.isFinite(loss))throw new IllegalArgumentException("SU1_GRAPH_NONFINITE_LOSS");for(double[] group:g.values())for(double v:group)if(!Double.isFinite(v))throw new IllegalArgumentException("SU1_GRAPH_NONFINITE_GRADIENT");
        Gradient result=new Gradient(loss,f.probabilities,g);result.crossEntropyLoss=ce;result.brierLoss=objective.brier*brier;result.smoothnessLoss=objective.smoothness*smooth;result.l2Loss=objective.l2*l2;result.resourceLoss=objective.resource*parameters;return result;
    }
    public static Weights update(Weights w,Gradient g,double rate,double clip){
        if(!Double.isFinite(rate)||rate<=0||!Double.isFinite(clip)||clip<=0)throw new IllegalArgumentException("SU1_GRAPH_OPTIMIZER");double sum=0;for(String key:GROUPS)for(double value:g.tensors.get(key))sum+=value*value;double scale=rate*Math.min(1,clip/Math.max(1e-300,Math.sqrt(sum)));Map<String,double[]> values=new LinkedHashMap<>();
        for(String key:GROUPS){double[] old=w.get(key),gradient=g.tensors.get(key),next=new double[old.length];for(int i=0;i<next.length;i++)next[i]=old[i]-scale*gradient[i];values.put(key,next);}return new Weights(w.d,w.channels,w.roleCount,w.typeCount,w.featureSchemaHash,values);
    }
    /** Closed replay transport; it carries input facts, not predictions, labels, alpha or certificates. */
    public static String encodeInput(Input input){
        try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){
            out.writeInt(0x53553149);out.writeInt(1);out.writeUTF(input.featureSchemaHash);out.writeLong(input.targetIssue);out.writeLong(input.cutoff);out.writeUTF(input.provenance);out.writeInt(input.steps.length);
            for(Step step:input.steps){out.writeLong(step.issue);out.writeLong(step.availableAt);out.writeUTF(step.receiptRoot);out.writeInt(step.channels);for(int n:step.draw)out.writeByte(n);for(int p:step.positions)out.writeByte(p);for(int n:step.numbers)out.writeByte(n);for(double v:step.features)out.writeDouble(v);for(boolean mask:step.mask)out.writeBoolean(mask);out.writeInt(step.edges.length);
                for(Edge edge:step.edges){out.writeByte(edge.receiver);out.writeByte(edge.type);out.writeInt(edge.support);out.writeDouble(edge.value);out.writeLong(edge.sourceIssue);out.writeByte(edge.sources.length);for(int i=0;i<edge.sources.length;i++){out.writeByte(edge.sources[i]);out.writeByte(edge.roles[i]);}}
            }
        }if(bytes.size()>2097152)throw new IllegalArgumentException("SU1_RELGRAPH_INPUT_BYTES");return Base64.getEncoder().encodeToString(bytes.toByteArray());}catch(IOException impossible){throw new IllegalStateException(impossible);}
    }
    public static Input decodeInput(String wire,String expectedHash){
        if(wire==null||wire.length()>2796204||!HashingSU1.text(wire).equals(HashingSU1.sha(expectedHash)))throw new IllegalArgumentException("SU1_RELGRAPH_INPUT_HASH_OR_SIZE");
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(wire)))){
            if(in.readInt()!=0x53553149||in.readInt()!=1)throw new IOException("SU1_RELGRAPH_INPUT_PROTOCOL");String schema=HashingSU1.sha(in.readUTF());long target=in.readLong(),cutoff=in.readLong();String provenance=in.readUTF();int n=in.readInt();if(n<1||n>256)throw new IOException("SU1_RELGRAPH_INPUT_STEPS");Step[] steps=new Step[n];
            for(int t=0;t<n;t++){
                long issue=in.readLong(),available=in.readLong();String root=HashingSU1.sha(in.readUTF());int channels=in.readInt();if(channels<1||channels>16)throw new IOException("SU1_RELGRAPH_INPUT_CHANNELS");int[] draw=new int[10],positions=new int[100],numbers=new int[100];double[] features=new double[100*channels];boolean[] mask=new boolean[features.length];for(int i=0;i<10;i++)draw[i]=in.readUnsignedByte();for(int i=0;i<100;i++)positions[i]=in.readUnsignedByte();for(int i=0;i<100;i++)numbers[i]=in.readUnsignedByte();for(int i=0;i<features.length;i++)features[i]=in.readDouble();for(int i=0;i<mask.length;i++)mask[i]=in.readBoolean();int count=in.readInt();if(count<0||count>2048)throw new IOException("SU1_RELGRAPH_INPUT_EDGES");Edge[] edges=new Edge[count];
                for(int i=0;i<count;i++){int receiver=in.readUnsignedByte(),type=in.readUnsignedByte(),support=in.readInt();double value=in.readDouble();long source=in.readLong();int arity=in.readUnsignedByte();if(arity<1||arity>4)throw new IOException("SU1_RELGRAPH_INPUT_ARITY");int[] sources=new int[arity],roles=new int[arity];for(int k=0;k<arity;k++){sources[k]=in.readUnsignedByte();roles[k]=in.readUnsignedByte();}edges[i]=new Edge(receiver,type,sources,roles,value,support,source,root);}
                steps[t]=new Step(issue,available,root,draw,positions,numbers,features,mask,channels,edges);
            }
            if(in.read()!=-1)throw new IOException("SU1_RELGRAPH_INPUT_TAIL");return new Input(schema,target,cutoff,provenance,steps);
        }catch(IOException|IllegalArgumentException invalid){throw new IllegalArgumentException("SU1_RELGRAPH_INPUT_CORRUPT",invalid);}
    }
}
