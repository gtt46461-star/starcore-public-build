import com.ethk3.novaonepro.strategyv2.ResearchActionPolicyR33;
import java.util.*;
public final class ResearchActionPolicyR33HostTest {
 static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);}
 static void rejected(Runnable r){boolean rejected=false;try{r.run();}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"must reject");}
 public static void main(String[] args){
  String h="0".repeat(64),j="1".repeat(64);
  for(String view:ResearchActionPolicyR33.READ_ONLY)rejected(()->new ResearchActionPolicyR33.Source(view,h,h,"abi","commit","FUTURE",h,h,1,100,1000).requireAction(h,1,"abi","commit",101,1001));
  ResearchActionPolicyR33.Source s=new ResearchActionPolicyR33.Source("R01",h,h,"abi","commit","SELECT",h,h,1,100,1000);
  s.requireAction(h,1,"abi","commit",101,1001);rejected(()->s.requireAction(h,1,"abi","commit",100,1001));rejected(()->s.requireAction(h,1,"abi","commit",101,999));rejected(()->s.requireAction(j,1,"abi","commit",101,1001));
  for (int i=1;i<=18;i++) {
   String view=String.format(Locale.ROOT,"R%02d",i);
   for(String role:Arrays.asList("TRAIN","SELECT","CONFIRM","FUTURE","SELECT_RESEARCH")) {
    Runnable action=()->new ResearchActionPolicyR33.Source(view,h,h,"abi","commit",role,h,h,1,100,1000).requireAction(h,1,"abi","commit",101,1001);
    if(!ResearchActionPolicyR33.READ_ONLY.contains(view) && role.equals(view.equals("R08")?"TRAIN":"SELECT"))action.run();else rejected(action);
   }
  }
  Set<Integer> legal=new HashSet<>(Arrays.asList(8,32,128,512));List<ResearchActionPolicyR33.Window> windows=Arrays.asList(new ResearchActionPolicyR33.Window(1,32,200,.2),new ResearchActionPolicyR33.Window(1,999,500,.2),new ResearchActionPolicyR33.Window(1,128,3,.2));
  check(ResearchActionPolicyR33.structuralWindow(windows,legal,200,.5).size==32,"only legal supported window");check(ResearchActionPolicyR33.structuralWindow(Collections.emptyList(),legal,200,.5)==null,"empty remains empty");
  Map<String,Double> priors=new LinkedHashMap<>();priors.put("A",1.);priors.put("B",1.);
  ResearchActionPolicyR33.Credit c=new ResearchActionPolicyR33.Credit("A","trial1","P1",256,.1,.2);
  Map<String,Double> one=ResearchActionPolicyR33.weights(priors,Arrays.asList(c),"P1",200,.25),duplicate=ResearchActionPolicyR33.weights(priors,Arrays.asList(c,c),"P1",200,.25);
  check(one.equals(duplicate),"duplicate trial cannot compound credit");check(one.get("A")>one.get("B"),"supported source changes real allocation");check(Math.abs(one.get("A")+one.get("B")-1)<1e-12,"budget mass preserved");check(one.get("B")>=.125,"exploration protected");
  check(ResearchActionPolicyR33.weights(priors,Arrays.asList(c),"P2",200,.25).get("A")==.5,"wrong target no credit");
  ResearchActionPolicyR33.ParentPair good=new ResearchActionPolicyR33.ParentPair(h,j,1,1,256,.2),wrong=new ResearchActionPolicyR33.ParentPair(h,j,1,2,256,.2);
  check(ResearchActionPolicyR33.fusionPairs(Arrays.asList(good,wrong),200,.6).size()==1,"same target pair only");
  check(ResearchActionPolicyR33.skipRefuted("m","m",h,h,1,1,false,256,200,-.01),"same-input refutation skips waste");check(!ResearchActionPolicyR33.skipRefuted("m","m",h,h,2,1,false,256,200,-.01),"new revision retests");check(!ResearchActionPolicyR33.skipRefuted("m","m",h,h,1,1,true,256,200,-.01),"protected asset never retired by failure memory");
  System.out.println("PASS ResearchActionPolicyR33HostTest: formal-role, PIT, wrong-source, output influence, duplicate credit, fixed mass, same-target fusion, refutation recovery");
 }
}
