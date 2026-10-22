package com.ethk3.novaonepro.r952;
import com.ethk3.novaonepro.*;import com.ethk3.novaonepro.r952.CandidateCompilerR952.*;import com.ethk3.novaonepro.r952.DevelopmentTrainerCP9.*;
import java.util.*;import java.io.*;import java.nio.file.*;import java.lang.reflect.*;
/** Real numerical adapter under the exact model quota allocated by a128MiB max heap.
 * No Android scheduling, native execution or live heap telemetry claim. */
public final class LowHeapTrainingContractCP12 {
 static int checks;static long peak;static int checkpointMax;
 static void ok(boolean b){checks++;if(!b)throw new AssertionError("lowheap "+checks);}
 static void bad(Runnable r){checks++;try{r.run();}catch(IllegalArgumentException|IllegalStateException e){return;}throw new AssertionError("accepted invalid");}
 public static void main(String[] args)throws Exception {
  long total=MemoryBudgetCP4.algorithmTotal(128L<<20);ok(total==(8L<<20));MemoryBudgetCP4 budget=new MemoryBudgetCP4(total/2);ok(budget.limit==(4L<<20));
  List<Models.Draw> history=new ArrayList<>();String issue="202609230001";for(int i=0;i<441;i++){Models.Draw d=new Models.Draw();d.issue=issue;d.openTime=1000+60000L*i;for(int p=0;p<10;p++)d.numbers[p]=(i+p)%10+1;history.add(d);issue=TargetIssueResolver.next(issue).targetIssue;}
  Map<String,Object> h=ModernModelContractTest.map("revision","SCDEV1","game","PK10","build","R980","sourceManifestHash",ModernModelContractTest.H,"scheduleRevision","TARGET_ISSUE_RESOLVER_R980","bindings",Collections.emptyList());DevelopmentInputCP9.Header header=new DevelopmentInputCP9.Header(StrictCanonicalJson.writeContext(h));
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();DevelopmentInputCP9.Receipt receipt=DevelopmentInputCP9.write(bytes,header,history);DevelopmentInputCP9.Input input=DevelopmentInputCP9.read(new ByteArrayInputStream(bytes.toByteArray()),receipt.byteCount,receipt.inputHash,header,budget);ok(input.split(0).trainN==300);ok(budget.used()==(2L<<20));input.close();input.close();ok(budget.used()==0);
  for(String field:new String[]{"issues","openTimes","numbers"}){Field f=DevelopmentInputCP9.Input.class.getDeclaredField(field);f.setAccessible(true);ok(f.get(input)==null);}bad(()->input.rowCount());
  for(ModelKind kind:ModelKind.values()){
   Dataset d=DevelopmentTrainerContractCP9.fixture(kind,0,300);int width=d.shape.featureN==0?d.shape.inputDimension:d.shape.featureN;
   try(MemoryBudgetCP4.Lease rows=budget.acquire(DevelopmentProductionCP12.datasetBytesCP12(d.trainN,width))){
    byte[] checkpoint;try(ResearchTrainingHookRegistryR952.PreparedNumericsCP9 initial=ResearchTrainingHookRegistryR952.prepareNumericsCP9(d,null,0,budget)){
     ok(!initial.complete());ok(budget.used()<=budget.limit);peak=Math.max(peak,budget.used());checkpoint=initial.checkpoint();checkpointMax=Math.max(checkpointMax,checkpoint.length);ok(checkpoint.length<=262144);
    }
    try(ResearchTrainingHookRegistryR952.PreparedNumericsCP9 resumed=ResearchTrainingHookRegistryR952.prepareNumericsCP9(d,checkpoint,System.nanoTime()+30_000_000_000L,budget)){
     ok(resumed.complete());ok(resumed.model(ModernModelContractTest.H).updates==300);ok(Double.isFinite(resumed.validationCE()));ok(Double.isFinite(resumed.validationBrier()));ok(resumed.checkpoint().length<=262144);peak=Math.max(peak,budget.used());checkpointMax=Math.max(checkpointMax,resumed.checkpoint().length);
    }
    ok(budget.used()==rows.bytes);byte[] broken=checkpoint.clone();broken[20]^=1;bad(()->ResearchTrainingHookRegistryR952.prepareNumericsCP9(d,broken,0,budget));ok(budget.used()==rows.bytes);
   }ok(budget.used()==0);
  }
  Dataset largest=DevelopmentTrainerContractCP9.fixture(ModelKind.MLP_D32_16_10,0,1200);try(MemoryBudgetCP4.Lease rows=budget.acquire(DevelopmentProductionCP12.datasetBytesCP12(1200,2));ResearchTrainingHookRegistryR952.PreparedNumericsCP9 n=ResearchTrainingHookRegistryR952.prepareNumericsCP9(largest,null,System.nanoTime()+30_000_000_000L,budget)){ok(n.complete());ok(n.model(ModernModelContractTest.H).updates==1200);ok(budget.used()<=budget.limit);peak=Math.max(peak,budget.used());}
  ok(budget.used()==0);ok(peak<=(4L<<20));Files.writeString(Path.of(args[0]),"{\"status\":\"PASS\",\"assertions\":"+checks+",\"model_budget_bytes\":"+budget.limit+",\"peak_reserved_bytes\":"+peak+",\"maximum_checkpoint_bytes\":"+checkpointMax+",\"model_kinds\":6,\"heap_process_limit\":\"128MiB\",\"scope\":\"Actual SCDEV parse+phase close releases arrays, six300-row numerical model adapters+SCCK2 restore;1200-row MLP completed; fixed4MiB model quota derived from128MiB heap. No Android/device memory or scheduler claim.\"}\n");
 }
}
