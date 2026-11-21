package com.ethk3.novaonepro.strategyv2;
import java.util.*;

/** Pure decisions used by the existing research consumers. Never authorizes REAL.
 * These decisions are hypotheses or resource allocations, not proof of quality.
 * The caller binds one authenticated original machine frame, not a chart cache.
 */
public final class ResearchActionPolicyR33 {
 private ResearchActionPolicyR33() {}
 public static final Set<String> READ_ONLY=Collections.unmodifiableSet(new HashSet<>(Arrays.asList("R11","R14","R18")));
 public static final class Source {
  public final String view,run,dataset,abi,commit,role,inputHash,productHash;
  public final long revision,throughOrdinal,availableAt;
  public Source(String view,String run,String dataset,String abi,String commit,String role,String input,String product,long revision,long through,long available) {
   if(view==null||(!view.matches("R(0[1-9]|1[0-8])")&&!view.matches("MATRIX:(EVIDENCE_TEACHER|STUDENT_ERROR|FORWARD_REASONING|REVERSE_REASONING|COUNTERFACTUAL|FEEDBACK|CONTRADICTION|TEACHER_DIVERSITY|INVARIANCE|CAUSAL_SUPPORT|COUNTER_EVIDENCE|RESIDUAL_MECHANISM|NUMBER_POSITION_COMPATIBILITY|CROSS_POSITION_FLOW|ACTION_UTILITY|ALGORITHM_COMPLEMENTARITY|EVOLUTION_CREDIT|RESOURCE_EFFICIENCY)")))throw new IllegalArgumentException("UNKNOWN_RESEARCH_VIEW");
   this.view=view;this.run=hash(run);this.dataset=hash(dataset);this.abi=abi;this.commit=commit;this.role=role;inputHash=hash(input);productHash=hash(product);this.revision=revision;throughOrdinal=through;availableAt=available;
   if(revision<0||through<0||available<0)throw new IllegalArgumentException("SOURCE_RANGE");
  }
  public void requireAction(String dataset,long revision,String abi,String commit,long decisionOrdinal,long preparedAt) {
   if(READ_ONLY.contains(view))throw new IllegalArgumentException("FORMAL_VIEW_READ_ONLY:"+view);
   String requiredRole = "R08".equals(view) ? "TRAIN" : "SELECT";
   if(!requiredRole.equals(role))throw new IllegalArgumentException("VIEW_SOURCE_ROLE_MISMATCH:"+view);
   if(!this.dataset.equals(dataset)||this.revision!=revision||!Objects.equals(this.abi,abi)||!Objects.equals(this.commit,commit))throw new IllegalArgumentException("SOURCE_IDENTITY_CHANGED");
   if(throughOrdinal>=decisionOrdinal||availableAt>preparedAt)throw new IllegalArgumentException("SOURCE_NOT_AVAILABLE_BEFORE_ACTION");
  }
 }
 public static String hash(String x){if(x==null||!x.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("SHA256_REQUIRED");return x;}
 public static final class Window {
  public final int position,size;public final long support;public final double value;
  public Window(int position,int size,long support,double value){this.position=position;this.size=size;this.support=support;this.value=value;}
 }
 /** R01: a non-flat structural state may diversify a hypothesis's window. It cannot claim predictive gain.
  * R02's existing positive paired-lower-bound rule retains priority in ResearchContext.window.
  * The returned window must be in the executor's actual allowed set. */
 public static Window structuralWindow(List<Window> rows,Set<Integer> allowed,long minimumSupport,double random) {
  if(!Double.isFinite(random)||random<0||random>=1||minimumSupport<1)throw new IllegalArgumentException("WINDOW_SELECTION_INPUT");
  List<Window> eligible=new ArrayList<>();
  for(Window w:rows)if(w.position>=1&&w.position<=10&&allowed.contains(w.size)&&w.support>=minimumSupport&&Double.isFinite(w.value)&&Math.abs(w.value)>1e-12)eligible.add(w);
  eligible.sort(Comparator.comparingInt((Window w)->w.position).thenComparingInt(w->w.size));
  return eligible.isEmpty()?null:eligible.get(Math.min(eligible.size()-1,(int)(random*eligible.size())));
 }
 public static final class Credit {
  public final String object,experiment,target;public final long support;public final Double lower,upper;
  public Credit(String object,String experiment,String target,long support,Double lower,Double upper){this.object=object;this.experiment=experiment;this.target=target;this.support=support;this.lower=lower;this.upper=upper;}
 }
 /** R09/R10/R16: actual paired outcomes modulate a research allocation once per experiment.
  * A minimum exploration mass always remains; birth counts or missing bounds confer no credit.
  * Reapplying the same evidence computes the same weights from fixed priors (no compounding). */
 public static Map<String,Double> weights(Map<String,Double> priors,List<Credit> credits,String target,long minimumSupport,double explorationMass) {
  if(priors.isEmpty()||minimumSupport<1||!Double.isFinite(explorationMass)||explorationMass<=0||explorationMass>1)throw new IllegalArgumentException("WEIGHT_INPUT");
  Map<String,Double> raw=new TreeMap<>();for(Map.Entry<String,Double> e:priors.entrySet()){double p=e.getValue();if(!Double.isFinite(p)||p<=0)throw new IllegalArgumentException("PRIOR_WEIGHT");raw.put(e.getKey(),p);}
  Map<String,Credit> seen=new HashMap<>();Map<String,Long> score=new HashMap<>();
  for(Credit c:credits){if(!raw.containsKey(c.object)||!Objects.equals(target,c.target)||c.support<minimumSupport||c.lower==null||c.upper==null||!Double.isFinite(c.lower)||!Double.isFinite(c.upper)||c.lower>c.upper)continue;
   if(c.experiment==null||c.experiment.isEmpty())throw new IllegalArgumentException("CREDIT_EXPERIMENT_ID_REQUIRED");
   String identity=c.object+"|"+c.experiment;Credit prior=seen.putIfAbsent(identity,c);
   if(prior!=null){if(prior.support!=c.support||!Objects.equals(prior.lower,c.lower)||!Objects.equals(prior.upper,c.upper))throw new IllegalArgumentException("CONFLICTING_EXPERIMENT_CREDIT");continue;}
   int sign=c.lower>0?1:c.upper<0?-1:0;score.put(c.object,Math.addExact(score.getOrDefault(c.object,0L),(long)sign));
  }
  double maximum=0;for(double p:raw.values())maximum=Math.max(maximum,p);
  double total=0;for(String key:raw.keySet()){long bounded=Math.max(-4L,Math.min(4L,score.getOrDefault(key,0L)));double factor=Math.pow(1.25,bounded);raw.put(key,(raw.get(key)/maximum)*factor);total+=raw.get(key);}
  if(!Double.isFinite(total)||total<=0)throw new IllegalArgumentException("WEIGHT_NORMALIZATION");
  Map<String,Double> out=new LinkedHashMap<>();for(String key:raw.keySet())out.put(key,explorationMass/raw.size()+(1-explorationMass)*raw.get(key)/total);return Collections.unmodifiableMap(out);
 }
 public static final class ParentPair {
  public final String a,b;public final int targetA,targetB;public final long support;public final double residualCorrelation;
  public ParentPair(String a,String b,int targetA,int targetB,long support,double correlation){this.a=hash(a);this.b=hash(b);this.targetA=targetA;this.targetB=targetB;this.support=support;residualCorrelation=correlation;}
 }
 /** R12 only proposes a complementary parent set. FusionOpsR27.fuse still fits TRAIN and requires
  * its original SELECT improvement check before a candidate reaches the unchanged birth gate. */
 public static List<ParentPair> fusionPairs(List<ParentPair> pairs,long minimumSupport,double maxCorrelation) {
  if(minimumSupport<1||!Double.isFinite(maxCorrelation)||maxCorrelation<=-1||maxCorrelation>=1)throw new IllegalArgumentException("FUSION_INPUT");
  List<ParentPair> out=new ArrayList<>();Set<String> seen=new HashSet<>();
  for(ParentPair p:pairs){if(p.a.equals(p.b)||p.targetA!=p.targetB||p.targetA<1||p.targetA>10||p.support<minimumSupport||!Double.isFinite(p.residualCorrelation)||p.residualCorrelation< -1||p.residualCorrelation>1||p.residualCorrelation>maxCorrelation)continue;
   String key=p.a.compareTo(p.b)<0?p.a+"|"+p.b:p.b+"|"+p.a;if(seen.add(key))out.add(p);
  }
  out.sort(Comparator.comparing(p->p.a+"|"+p.b));return Collections.unmodifiableList(out);
 }
 /** R17: a previously refuted exact mechanism may be skipped on the same input identity only.
  * New data/revision, a protected asset or unresolved bounds always permits reevaluation. */
 public static boolean skipRefuted(String candidateMechanism,String negativeMechanism,String dataset,String negativeDataset,long revision,long negativeRevision,boolean protectedAsset,long support,long minimumSupport,Double upper95) {
  return minimumSupport>=1&&support>=minimumSupport&&revision>=0&&negativeRevision>=0&&!protectedAsset&&candidateMechanism!=null&&!candidateMechanism.isEmpty()&&negativeMechanism!=null&&dataset!=null&&dataset.matches("[a-f0-9]{64}")&&Objects.equals(candidateMechanism,negativeMechanism)&&Objects.equals(dataset,negativeDataset)&&revision==negativeRevision&&upper95!=null&&Double.isFinite(upper95)&&upper95<0;
 }
}
