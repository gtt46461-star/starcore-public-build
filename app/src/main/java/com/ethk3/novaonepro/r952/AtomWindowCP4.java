package com.ethk3.novaonepro.r952;
import java.util.*;
/** Proposed pure Java production kernel. One instance per position, research/realtime ownership isolated. */
public final class AtomWindowCP4 implements AutoCloseable {
 public static final int CAPACITY=5000, MAX_STATES=256;
 private final byte[] digits=new byte[CAPACITY]; private long end=-1; private int size;
 private final long[] lastSeen={-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};
 private final LinkedHashMap<String,Window> windows=new LinkedHashMap<>();
 private final LinkedHashMap<String,Markov2> markov2States=new LinkedHashMap<>();
 private final MemoryBudgetCP4 budget;private boolean closed;
 public AtomWindowCP4(MemoryBudgetCP4 budget){this.budget=Objects.requireNonNull(budget);if(!budget.reserve(8192))throw new Missing("ATOM_MEMORY_BUDGET");}
 public void close(){if(closed)return;for(Window w:windows.values())budget.release(w.bytes);windows.clear();for(Markov2 m:markov2States.values())budget.release(m.bytes);markov2States.clear();budget.release(8192);closed=true;}
 public static final class Missing extends RuntimeException { public Missing(String reason){super(reason);} }
 public void append(long ordinal,int digit){
  if(closed)throw new IllegalStateException("WINDOW_CLOSED");
  if(ordinal<0||digit<0||digit>9)throw new IllegalArgumentException("DIGIT_BIN");
  if(end>=0&&ordinal!=end+1)throw new Missing("HISTORY_SEQUENCE_GAP");
  // Update sufficient statistics before overwriting the oldest ring slot.
  for(Window w:windows.values())w.advance(ordinal,digit);
  for(Markov2 m:markov2States.values())m.advance(ordinal,digit);
  digits[(int)(ordinal%CAPACITY)]=(byte)digit;end=ordinal;size=Math.min(CAPACITY,size+1);lastSeen[digit]=ordinal;
 }
 public long end(){return end;} public int size(){return size;}
 public double omission(int digit){long at=lastSeen[digit];if(at<end-size+1||at<0)throw new Missing("LEFT_CENSORED_OMISSION");return end-at;}
 private int at(long ordinal){if(ordinal<end-size+1||ordinal>end)throw new Missing("INSUFFICIENT_HISTORY");return digits[(int)(ordinal%CAPACITY)]&255;}
 public int lag(int lag){if(lag<1)throw new IllegalArgumentException("LAG");return at(end-lag+1);}
 public Window window(int count,int lag){
  if(count<1||lag<0||count+lag>CAPACITY)throw new IllegalArgumentException("WINDOW_BOUND");
  String key=count+":"+lag;Window found=windows.get(key);if(found!=null)return found;
  if(windows.size()+markov2States.size()>=MAX_STATES)throw new Missing("ATOM_STATE_BUDGET");
  long bytes=4096L+64L*count;if(!budget.reserve(bytes))throw new Missing("ATOM_MEMORY_BUDGET");
  try{found=new Window(count,lag,bytes);found.rebuild();windows.put(key,found);return found;}catch(RuntimeException|Error e){budget.release(bytes);throw e;}
 }
 public void releaseUnused(Set<String> keep){Iterator<Map.Entry<String,Window>>it=windows.entrySet().iterator();while(it.hasNext()){Map.Entry<String,Window>e=it.next();if(!keep.contains(e.getKey())){budget.release(e.getValue().bytes);it.remove();}}Iterator<Map.Entry<String,Markov2>>mi=markov2States.entrySet().iterator();while(mi.hasNext()){Map.Entry<String,Markov2>e=mi.next();if(!keep.contains(e.getKey())){budget.release(e.getValue().bytes);mi.remove();}}}
 /** One cached100x10 sufficient-statistic state per transition-window; digits/alpha share it. */
 public Markov2 markov2(int count){
  if(closed)throw new IllegalStateException("WINDOW_CLOSED");
  if(count<1||count>CAPACITY-2)throw new IllegalArgumentException("MARKOV_WINDOW_BOUND");
  String key="M2:"+count;Markov2 found=markov2States.get(key);if(found!=null)return found;
  if(windows.size()+markov2States.size()>=MAX_STATES)throw new Missing("ATOM_STATE_BUDGET");
  long bytes=8192L;if(!budget.reserve(bytes))throw new Missing("ATOM_MEMORY_BUDGET");
  try{found=new Markov2(count,bytes);found.rebuild();markov2States.put(key,found);return found;}catch(RuntimeException|Error failure){budget.release(bytes);throw failure;}
 }
 public final class Markov2 {
  public final int count;private final long bytes;private final int[] triples=new int[1000];private boolean ready;
  private Markov2(int count,long bytes){this.count=count;this.bytes=bytes;}
  private int index(int a,int b,int c){return (a*10+b)*10+c;}
  private void rebuild(){Arrays.fill(triples,0);ready=false;if(size<count+2)return;long first=end-count+1;for(int i=0;i<count;i++){long target=first+i;triples[index(at(target-2),at(target-1),at(target))]++;}ready=true;}
  private void advance(long ordinal,int digit){if(!ready)return;long outgoing=ordinal-count;int remove=index(at(outgoing-2),at(outgoing-1),at(outgoing));if(triples[remove]<=0)throw new IllegalStateException("MARKOV_COUNT_UNDERFLOW");triples[remove]--;triples[index(at(ordinal-2),at(ordinal-1),digit)]++;}
  private void require(){if(closed)throw new IllegalStateException("WINDOW_CLOSED");if(!ready)rebuild();if(!ready)throw new Missing("INSUFFICIENT_HISTORY");}
  public int[] counts(){require();return triples.clone();}
  public double probability(int a,int b,int digit,double alpha,int minContext){
   require();if(a<0||a>9||b<0||b>9||digit<0||digit>9||!Double.isFinite(alpha)||alpha<=0||minContext!=8)throw new IllegalArgumentException("MARKOV_CONTEXT_PARAMETERS");
   int first=index(a,b,0),support=0;for(int c=0;c<10;c++)support+=triples[first+c];
   if(support<minContext)throw new Missing("MARKOV_CONTEXT_SUPPORT");
   return (triples[first+digit]+alpha)/(support+10*alpha);
  }
 }
 public static double entropy(double[] c){double n=0,h=0;for(double x:c)n+=x;if(n==0)throw new Missing("EMPTY");for(double x:c)if(x>0){double p=x/n;h-=p*Math.log(p);}return h;}
 public static double[] probabilities(double[] c,double alpha){double n=alpha*c.length;for(double x:c)n+=x;if(n<=0)throw new Missing("EMPTY");double[]p=new double[c.length];for(int i=0;i<c.length;i++)p[i]=(c[i]+alpha)/n;return p;}
 public static double js(double[]p,double[]q){return MatrixMetricsCP11.js(p,q);}
 public final class Window {
  public final int count,lag; private final long bytes;private boolean ready;
  private final double[] bins=new double[10],weighted=new double[10],joint=new double[100];
  private final ArrayDeque<Long>[] seen;
  private final double[] gapSum=new double[10],gapSq=new double[10];
  @SuppressWarnings("unchecked") private Window(int count,int lag,long bytes){this.count=count;this.lag=lag;this.bytes=bytes;seen=(ArrayDeque<Long>[])new ArrayDeque[10];for(int d=0;d<10;d++)seen[d]=new ArrayDeque<>();}
  private void clear(){Arrays.fill(bins,0);Arrays.fill(weighted,0);Arrays.fill(joint,0);Arrays.fill(gapSum,0);Arrays.fill(gapSq,0);for(ArrayDeque<Long>q:seen)q.clear();ready=false;}
  private void occurrence(long ordinal,int d){ArrayDeque<Long>q=seen[d];if(!q.isEmpty()){double g=ordinal-q.peekLast();gapSum[d]+=g;gapSq[d]+=g*g;}q.addLast(ordinal);}
  private void removeOccurrence(int d){ArrayDeque<Long>q=seen[d];long old=q.removeFirst();if(!q.isEmpty()){double g=q.peekFirst()-old;gapSum[d]-=g;gapSq[d]-=g*g;}}
  private void rebuild(){clear();if(size<count+lag)return;long first=end-count+1;for(int i=0;i<count;i++){long p=first+i;int d=at(p);bins[d]++;weighted[d]+=i;occurrence(p,d);if(lag>0)joint[at(p-lag)*10+d]++;}ready=true;}
  private void advance(long ordinal,int newDigit){
   if(!ready){ // Capacity warm-up: wait until the new sample is installed, then lazy rebuild.
    return;
   }
   long outgoing=ordinal-count;int old=at(outgoing);
   for(int d=0;d<10;d++)weighted[d]-=bins[d]-(d==old?1:0);
   bins[old]--;bins[newDigit]++;weighted[newDigit]+=count-1;removeOccurrence(old);occurrence(ordinal,newDigit);
   if(lag>0){joint[at(outgoing-lag)*10+old]--;joint[at(ordinal-lag)*10+newDigit]++;}
  }
  private void require(){if(!ready)rebuild();if(!ready)throw new Missing("INSUFFICIENT_HISTORY");}
  public double[] counts(){require();return bins.clone();}
  public double[] jointCounts(){require();if(lag<1)throw new IllegalStateException("LAG_REQUIRED");return joint.clone();}
  public double frequency(int d){require();return bins[d]/count;}
  public double numericMean(){require();double s=0;for(int d=0;d<10;d++)s+=d*bins[d];return s/count;}
  public double numericVariance(){require();double s=0,m=numericMean();for(int d=0;d<10;d++)s+=(d-m)*(d-m)*bins[d];return s/count;}
  public double slope(int d){require();if(count<2)throw new Missing("SLOPE_SUPPORT");return (weighted[d]-(count-1)*bins[d]/2)/(count*(count*count-1.0)/12);}
  public double omission(int d){require();if(seen[d].isEmpty())throw new Missing("LEFT_CENSORED_OMISSION");return end-seen[d].peekLast();}
  public double gap(int d,String mode){require();int n=seen[d].size()-1;if(n<1)throw new Missing("INSUFFICIENT_OCCURRENCES");double mean=gapSum[d]/n,var=Math.max(0,gapSq[d]/n-mean*mean);Iterator<Long>it=seen[d].descendingIterator();long last=it.next(),prev=it.next();double recent=last-prev;
   switch(mode){case "LAST":return recent;case "MEAN":return mean;case "VAR":return var;case "Z":if(n<2||var<1e-12)throw new Missing("GAP_VARIANCE_ZERO");return (recent-mean)/Math.sqrt(var);default:throw new IllegalArgumentException("GAP_MODE");}}
  public double transition(int from,int to,double alpha){require();if(lag<1)throw new IllegalArgumentException("LAG_REQUIRED");double n=10*alpha;for(int d=0;d<10;d++)n+=joint[from*10+d];if(n<=0)throw new Missing("EMPTY_TRANSITION");return (joint[from*10+to]+alpha)/n;}
  public double conditionalEntropy(){require();if(lag<1)throw new IllegalArgumentException("LAG_REQUIRED");double h=0;for(int a=0;a<10;a++){double n=0;for(int b=0;b<10;b++)n+=joint[a*10+b];for(int b=0;b<10;b++){double c=joint[a*10+b];if(c>0)h-=c/count*Math.log(c/n);}}return h;}
  public double indicatorMI(int digit){require();if(lag<1)throw new IllegalArgumentException("LAG_REQUIRED");double yes=bins[digit],no=count-yes,out=0;for(int a=0;a<10;a++){double n=0;for(int b=0;b<10;b++)n+=joint[a*10+b];double c=joint[a*10+digit];if(c>0)out+=c/count*Math.log(c*count/(n*yes));if(n-c>0)out+=(n-c)/count*Math.log((n-c)*count/(n*no));}return Math.max(0,out);}
  public double periodicity(int digit){require();if(lag<1)throw new IllegalArgumentException("LAG_REQUIRED");double a=0,b=bins[digit],ab=joint[digit*10+digit];for(int d=0;d<10;d++)a+=joint[digit*10+d];double denom=Math.sqrt((a-a*a/count)*(b-b*b/count));if(denom<=1e-12)throw new Missing("ZERO_VARIANCE");return (ab-a*b/count)/denom;}
  public double digitCorrelation(){require();if(lag<1)throw new IllegalArgumentException("LAG_REQUIRED");double a=0,b=0,aa=0,bb=0,ab=0;for(int i=0;i<10;i++)for(int j=0;j<10;j++){double n=joint[i*10+j];a+=n*i;b+=n*j;aa+=n*i*i;bb+=n*j*j;ab+=n*i*j;}double den=Math.sqrt(Math.max(0,(aa-a*a/count)*(bb-b*b/count)));if(den<=1e-12)throw new Missing("ZERO_VARIANCE");return (ab-a*b/count)/den;}
 }
}
