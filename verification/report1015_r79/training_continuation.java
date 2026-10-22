package com.ethk3.novaonepro;
import java.nio.file.*;
/** Actual runtime allowlist: DEV compatibility never grants evidence authority. */
public final class training_continuation {
 static int checks;
 static void ok(boolean b,String why){if(!b)throw new AssertionError(why);checks++;System.out.println("PASS "+why);}
 public static void main(String[] args)throws Exception {
  if(!args[0].equals("normal")){
   try{TrainingContinuationR973.compatible("unknown");throw new AssertionError("invalid generated rules accepted");}
   catch(ExceptionInInitializerError e){ok(e.getCause() instanceof IllegalStateException&&e.getCause().getMessage().equals(args[0]),"invalid exact predecessor list rejected");}
  }else{
   String[] rules=ResearchSemanticsGeneratedR967.COMPATIBLE_PREDECESSOR_RULES_SHA256.split(",");
   ok(rules.length==14,"exact fourteen R65-R78 predecessors generated");
   for(String rule:rules)ok(TrainingContinuationR973.compatible(rule),"exact reviewed DEV predecessor accepted "+rule);
   ok(TrainingContinuationR973.compatible(TrainingCodeIdentityR964.SHA256),"current DEV rule accepted");
   ok(!TrainingContinuationR973.compatible(null)&&!TrainingContinuationR973.compatible("f".repeat(64)),"null and unknown rules rejected");
   String predicate=TrainingContinuationR973.predicate("t.training_rule_hash");
   for(String rule:rules)ok(predicate.contains("t.training_rule_hash='"+rule+"'"),"SQL admission includes same exact predecessor");
   try{TrainingContinuationR973.predicate("x OR 1=1");throw new AssertionError("unsafe identifier");}catch(IllegalArgumentException expected){checks++;}
   ok(TrainingContinuationR973.scope().contains("certificateMigration=0")&&TrainingContinuationR973.scope().contains("futureSamplesInherited=0"),"no certificate or Future inheritance");
  }
  Files.writeString(Path.of(args[1]),"{\"status\":\"PASS\",\"checks\":"+checks+"}\n");
 }
}
