import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.strategyv2.FailureDispositionR31;
import com.ethk3.novaonepro.strategyv2.WaitKindR27;
import java.util.concurrent.TimeoutException;

/** Controlled cause injection into actual production classifiers; no SQL/model execution claim. */
public final class ClassifierParityHost {
  static final class ProducerDependency extends IllegalStateException implements FailureDispositionR31.ClassifiedFailure {
    ProducerDependency(Throwable cause){super("unavailable input",cause);}
    public String failureCategory(){return "DEPENDENCY";}
    public String operation(){return "CONTROLLED_PRODUCER";}
    public String phase(){return "PREPARE";}
    public String commitOutcomeName(){return "NOT_SUBMITTED";}
  }
  static boolean compare(String label,Throwable root){
    FailureDispositionR31.Decision direct=FailureDispositionR31.classify(root);
    String category=DatabaseCoordinator.failureCategoryR31(root);
    DatabaseCoordinator.WriteRejectedR31 adapted=new DatabaseCoordinator.WriteRejectedR31(category,"CONTROLLED_WRITER","BODY",DatabaseCoordinator.CommitOutcome.ROLLED_BACK,root);
    FailureDispositionR31.Decision writer=FailureDispositionR31.classify(adapted);
    String directWait=WaitKindR27.classify(0,null,root,0,1,null,false).kind;
    String writerWait=WaitKindR27.classify(0,null,adapted,0,1,null,false).kind;
    boolean same=direct.disposition==writer.disposition&&direct.category.equals(writer.category);
    System.out.println(label+" direct="+direct.disposition+":"+direct.category+" writer="+writer.disposition+":"+writer.category+" directWait="+directWait+" writerWait="+writerWait+" same="+same);
    return same;
  }
  public static void main(String[] args){
    int mismatches=0;
    if(!compare("plain-java-timeout",new TimeoutException("bounded operation expired")))mismatches++;
    if(!compare("typed-dependency-around-storage-full",new ProducerDependency(new IllegalStateException("SQLITE_FULL: database or disk is full"))))mismatches++;
    if(!compare("typed-dependency-around-storage-corrupt",new ProducerDependency(new IllegalStateException("SQLITE_CORRUPT"))))mismatches++;
    if(!compare("typed-dependency-around-storage-constraint",new ProducerDependency(new IllegalStateException("constraint failed"))))mismatches++;
    if(!compare("control-busy",new IllegalStateException("CP3_DB_BUSY_DEADLINE")))throw new AssertionError("CONTROL_BUSY_MISMATCH");
    if(!compare("control-stale",new IllegalStateException("R26_STALE_FENCE_OR_LEASE_OR_SESSION")))throw new AssertionError("CONTROL_STALE_MISMATCH");
    if(!compare("control-cancel",new InterruptedException("user stop")))throw new AssertionError("CONTROL_CANCEL_MISMATCH");
    DatabaseCoordinator.WriteRejectedR31 committed=new DatabaseCoordinator.WriteRejectedR31("BUG","CONTROLLED","DIAGNOSTIC",DatabaseCoordinator.CommitOutcome.COMMITTED_DIAGNOSTIC_FAILED,new IllegalStateException("SQLITE_FULL"));
    if(!FailureDispositionR31.classify(committed).alreadyCommitted)throw new AssertionError("COMMITTED_OUTCOME_LOST");
    DatabaseCoordinator.WriteRejectedR31 unknown=new DatabaseCoordinator.WriteRejectedR31("LOCK_BUSY","CONTROLLED","END",DatabaseCoordinator.CommitOutcome.UNKNOWN,new IllegalStateException("SQLITE_FULL"));
    if(!FailureDispositionR31.classify(unknown).requiresCommitResolution)throw new AssertionError("UNKNOWN_OUTCOME_LOST");
    System.out.println("COMMITTED_AND_UNKNOWN_OUTCOME_PRIORITY_CONTROLS_PASS");
    System.out.println("CLASSIFIER_PARITY_MISMATCHES="+mismatches);
    if(mismatches!=0)throw new AssertionError("FOUR_ENTRY_CLASSIFIER_PARITY_NOT_MET");
  }
}
