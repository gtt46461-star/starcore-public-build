import java.util.*;
public class R18ProjectionContractHarness {
 static Map<String,Object> node(String id,Object value,String detail){return ResearchObservatoryR30R1.point(id,value,-1,detail);}
 @SuppressWarnings("unchecked") static void edge(Map<String,Object> f,String a,String b,String proof){Map<String,Object> e=new LinkedHashMap<>();e.put("from",a);e.put("to",b);e.put("label",proof);((List<Object>)f.get("edges")).add(e);}

 /** Display-only projection of the exact receipt already verified by
  * ResearchMainlineContributionAuditR922.exactRealAttributionR34.
  * This method does not create authorization, settle trades, or infer quality. */
 static void appendRealAttributionR343(Map<String,Object> frame,Map<String,Object> row,
         String key,String experimentNode){
  List<Object> out=ResearchObservatoryR30R1.points(frame);
  Object raw=row.get("attributedRealSettlement"),experiment=row.get("experiment");
  Map<?,?> receipt=raw instanceof Map?(Map<?,?>)raw:null;
  Object authorization=row.get("realAuthorization");
  boolean bound=receipt!=null&&experiment instanceof String&&!((String)experiment).isEmpty()
      &&row.get("childHash")!=null&&row.get("confirmationTask")!=null
      &&"MATCHED_ORIGINAL_POLICY_MODEL_AUTHORIZATION_AND_TRADE_KEYS".equals(row.get("attributionReason"))
      &&(MainlineFreezeProofR31.PREFIX+experiment).equals(receipt.get("policy"))
      &&authorization instanceof String&&!((String)authorization).isEmpty()
      &&authorization.equals(receipt.get("authorizationHash"))
      &&receipt.get("tradeId") instanceof Number&&((Number)receipt.get("tradeId")).longValue()>0;
  if(!bound){
   out.add(node(key+" REAL缺证",null,receipt==null?String.valueOf(row.get("attributionReason"))
       :"REAL_PROJECTION_BINDING_MISMATCH"));
   return;
  }
  String authorizationNode=key+" REAL原始授权";
  out.add(node(authorizationNode,1,"authorizationHash="+authorization+"; tradeId="
      +receipt.get("tradeId")+"; targetIssue="+receipt.get("targetIssue")
      +"; predrawClock="+receipt.get("predrawClock")));
  edge(frame,experimentNode,authorizationNode,"EXACT_EXPERIMENT_POLICY_AND_AUTHORIZATION_TRADE_KEYS");
  String state=String.valueOf(receipt.get("state"));
  Object hit=receipt.get("hit"),settlement=receipt.get("settlementHash");
  boolean settled="SETTLED_EXACT".equals(state)&&"VERIFIED".equals(receipt.get("predrawClock"))
      &&settlement instanceof String&&!((String)settlement).isEmpty()
      &&hit instanceof Number&&(((Number)hit).doubleValue()==0.0||((Number)hit).doubleValue()==1.0);
  if(settled){
   String settledNode=key+" REAL结算";
   out.add(node(settledNode,hit,"settlementHash="+settlement+"; tradeId="+receipt.get("tradeId")
       +"; targetIssue="+receipt.get("targetIssue")+"; pnl="+receipt.get("pnl")
       +"; state=SETTLED_EXACT; 单次真实结算不代表统计优势"));
   edge(frame,authorizationNode,settledNode,"EXACT_AUTHORITATIVE_SETTLEMENT_RECEIPT");
  }else{
   out.add(node(key+("BET_AWAITING_SETTLEMENT".equals(state)?" REAL待结算":" REAL结算未验证"),
       null,"state="+state+"; settlement requires verified original terminal receipt"));
  }
 }

 static Map<String,Object> frame(){Map<String,Object> f=new LinkedHashMap<>();f.put("points",new ArrayList<Object>());f.put("edges",new ArrayList<Object>());return f;}
 static Map<String,Object> row(){Map<String,Object> r=new LinkedHashMap<>();r.put("experiment","experiment-a");r.put("childHash","child-a");r.put("confirmationTask","confirm-a");r.put("realAuthorization","authorization-a");r.put("attributionReason","MATCHED_ORIGINAL_POLICY_MODEL_AUTHORIZATION_AND_TRADE_KEYS");Map<String,Object> receipt=new LinkedHashMap<>();receipt.put("policy","R31-R27-experiment-a");receipt.put("authorizationHash","authorization-a");receipt.put("tradeId",42L);receipt.put("targetIssue","209901010001");receipt.put("predrawClock","VERIFIED");receipt.put("state","SETTLED_EXACT");receipt.put("hit",1);receipt.put("settlementHash","settlement-a");receipt.put("pnl",1.0);r.put("attributedRealSettlement",receipt);return r;}
 @SuppressWarnings("unchecked") static Map<String,Object> receipt(Map<String,Object> r){return (Map<String,Object>)r.get("attributedRealSettlement");}
 static boolean has(Map<String,Object> f,String label){for(Object x:ResearchObservatoryR30R1.points(f))if(label.equals(((Map<?,?>)x).get("label")))return true;return false;}
 static int edges(Map<String,Object> f){return ((List<?>)f.get("edges")).size();}
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 static int count;
 static void verify(String name,Map<String,Object> r,String expected,int edges){Map<String,Object> f=frame();appendRealAttributionR343(f,r,"1","1 实验/资格");check(has(f,"1 "+expected),name+": missing expected node");check(edges(f)==edges,name+": wrong edges");if(!expected.equals("REAL结算"))check(!has(f,"1 REAL结算"),name+": false settlement");if(expected.equals("REAL结算"))check(!has(f,"1 REAL缺证"),name+": false missing evidence");count++;System.out.println("PASS "+name);}
 public static void main(String[] args){
  verify("exact_settled_win",row(),"REAL结算",2);
  Map<String,Object> r=row();receipt(r).put("hit",0);verify("exact_settled_loss_still_real",r,"REAL结算",2);
  r=row();r.remove("attributedRealSettlement");r.put("attributionReason","NO_EXACT_ORIGINAL_POLICY_AUTHORIZATION_AND_SETTLEMENT_KEY");verify("no_original_receipt",r,"REAL缺证",0);
  r=row();receipt(r).put("authorizationHash","wrong-authorization");verify("wrong_authorization",r,"REAL缺证",0);
  r=row();receipt(r).put("policy","R31-R27-wrong-experiment");verify("wrong_experiment",r,"REAL缺证",0);
  r=row();receipt(r).put("state","BET_AWAITING_SETTLEMENT");receipt(r).remove("settlementHash");receipt(r).remove("hit");verify("awaiting_settlement",r,"REAL待结算",1);
  r=row();receipt(r).put("state","SETTLEMENT_IDENTITY_UNKNOWN");verify("unknown_terminal_identity",r,"REAL结算未验证",1);
  r=row();receipt(r).put("predrawClock","UNVERIFIED_ORDER");verify("unverified_predraw_clock",r,"REAL结算未验证",1);
  r=row();receipt(r).remove("settlementHash");verify("missing_terminal_hash",r,"REAL结算未验证",1);
  r=row();r.remove("confirmationTask");verify("missing_confirmation_link",r,"REAL缺证",0);
  r=row();receipt(r).put("hit",.5);verify("invalid_hit",r,"REAL结算未验证",1);
  r=row();receipt(r).put("tradeId",0);verify("invalid_trade_id",r,"REAL缺证",0);
  System.out.println("PASS_TOTAL="+count);
 }
}
final class ResearchObservatoryR30R1 {
 @SuppressWarnings("unchecked") static List<Object> points(Map<String,Object> f){return (List<Object>)f.get("points");}
 static Map<String,Object> point(String id,Object value,long n,String detail){Map<String,Object> x=new LinkedHashMap<>();x.put("label",id);x.put("value",value);x.put("n",n);x.put("details",detail);return x;}
}
final class MainlineFreezeProofR31 {static final String PREFIX="R31-R27-";}
