import com.ethk3.novaonepro.*;
public final class SemanticConsumerProbe {
 public static void main(String[] args) {
  String expected=args[0];
  if(!TrainingCodeIdentityR964.SHA256.equals(expected))throw new AssertionError("COMPILED_RULE_DRIFT");
  if(!TrainingContinuationR973.compatible(expected))throw new AssertionError("CURRENT_RULE_REJECTED");
  if(TrainingContinuationR973.compatible(null))throw new AssertionError("NULL_RULE_ACCEPTED");
  if(TrainingContinuationR973.compatible("2e127e7dd7be3235e9adccd85e3d43d543423e9fca30aebf57a80263bd09f228"))throw new AssertionError("UNREVIEWED_PREDECESSOR_INHERITED");
  String predicate=TrainingContinuationR973.predicate("run.training_rule_sha256");
  if(!predicate.equals("(run.training_rule_sha256='"+expected+"')"))throw new AssertionError("PREDICATE_IDENTITY_DRIFT");
  try{TrainingContinuationR973.predicate("run.rule' OR 1=1");throw new AssertionError("SQL_IDENTIFIER_ACCEPTED");}catch(IllegalArgumentException expectedError){}
  if(!TrainingContinuationR973.scope().contains("certificateMigration=0|futureSamplesInherited=0|proof="))throw new AssertionError("AUTHORITY_SCOPE_DRIFT");
  System.out.println("ACTUAL_COMPILED_CONSUMERS_PASS|rule="+expected+"|predecessor_rejected|no_certificate_or_future_inheritance|no_DB_side_effect");
 }
}
