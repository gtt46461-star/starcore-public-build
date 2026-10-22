package com.ethk3.novaonepro;
public final class IdentityContractHarnessR967 {
 static int n;static String h(char c){char[] a=new char[64];java.util.Arrays.fill(a,c);return new String(a);}
 static void ok(boolean v,String s){if(!v)throw new AssertionError(s);n++;System.out.println("PASS "+s);}
 interface Op{void run();}static void bad(Op o,String s){try{o.run();throw new AssertionError(s);}catch(IllegalArgumentException e){ok(true,s);}}
 public static void main(String[] a){
  ok(new ResearchSemanticsR967.Builder("T").text("a","x").integer("b",2).digest().equals(new ResearchSemanticsR967.Builder("T").integer("b",2).text("a","x").digest()),"ORDER_INVARIANT");
  ok(!new ResearchSemanticsR967.Builder("T").text("a","a|b").text("b","c").digest().equals(new ResearchSemanticsR967.Builder("T").text("a","a").text("b","b|c").digest()),"BOUNDARIES_DISTINCT");
  ok(!new ResearchSemanticsR967.Builder("T").integer("n",1).digest().equals(new ResearchSemanticsR967.Builder("T").text("n","1").digest()),"TYPES_DISTINCT");
  ok(new ResearchSemanticsR967.Builder("T").number("n",-0d).digest().equals(new ResearchSemanticsR967.Builder("T").number("n",0d).digest()),"SIGNED_ZERO_POLICY");
  byte[] b={1};ResearchSemanticsR967.Builder v=new ResearchSemanticsR967.Builder("T").bytes("b",b);String before=v.digest();b[0]=2;ok(before.equals(v.digest()),"DEFENSIVE_BYTES");
  bad(()->new ResearchSemanticsR967.Builder("T").text("a","x").text("a","y"),"DUPLICATE_FIELD_REJECTED");
  bad(()->new ResearchSemanticsR967.Builder("T").number("n",Double.NaN),"NAN_REJECTED");
  bad(()->new ResearchSemanticsR967.Builder("T").hash("h",""),"MISSING_HASH_REJECTED");
  String rule=ResearchSemanticsR967.trainingSemantics(h('a'),h('b'),h('c'),h('d'),h('e'),h('f'));
  ok(rule.equals(ResearchSemanticsR967.trainingSemantics(h('a'),h('b'),h('c'),h('d'),h('e'),h('f'))),"RULE_STABLE_WITHOUT_BUILD_OR_WEIGHTS");
  ok(!rule.equals(ResearchSemanticsR967.trainingSemantics(h('b'),h('b'),h('c'),h('d'),h('e'),h('f'))),"PROGRAM_CHANGE_NEW_RULE");
  String eval=ResearchSemanticsR967.frozenEvaluationHash(h('a'),h('b'),h('c'),h('d'),h('e'),h('f'));
  ok(!eval.equals(ResearchSemanticsR967.frozenEvaluationHash(h('a'),h('c'),h('c'),h('d'),h('e'),h('f'))),"FROZEN_WEIGHTS_CHANGE_NEW_EVAL");
  ok(!eval.equals(ResearchSemanticsR967.frozenEvaluationHash(h('a'),h('b'),h('c'),h('d'),h('e'),h('a'))),"INTEGRITY_EPOCH_CHANGE_NEW_EVAL");
  bad(()->ResearchSemanticsR967.trainingRunHash(rule,h('a'),h('b'),"EVAL_SEALED"),"SEALED_CANNOT_CREATE_TRAIN_RUN");
  String definition=ResearchSemanticsR967.recipeDefinitionHash(h('a'),h('b'),h('c'),h('d'),h('e'));
  String cert=ResearchSemanticsR967.certificateSemantics(eval,h('a'),h('b'),definition,h('c'),h('d'));
  ok(ResearchSemanticsR967.recipeInstanceHash(definition,cert,h('a'),"202609200001").length()==64,"ACYCLIC_RECIPE_CERT_INSTANCE");
  System.out.println("IDENTITY_REFERENCE_PASS checks="+n+"; integrated_into_study=NO; dependency_closure_inference=NO");
 }
}