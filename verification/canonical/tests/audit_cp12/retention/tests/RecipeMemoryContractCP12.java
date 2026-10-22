package com.ethk3.novaonepro;
import com.ethk3.novaonepro.r952.*;
import org.json.*;import java.util.*;import java.nio.file.*;
public final class RecipeMemoryContractCP12 {
 static int checks;static void ok(boolean v){checks++;if(!v)throw new AssertionError("check "+checks);}
 static void bad(Runnable r){checks++;try{r.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}throw new AssertionError("bad accepted");}
 static JSONObject root(String wire,String base,String target,int n)throws Exception{
  JSONObject root=new JSONObject();root.put("revision",FrozenConsumerRecipeR932.REVISION);root.put("source_sha256",SourceManifestR815.SOURCE_TREE_SHA256);root.put("lane",CertifiedPredictionR974.LANE);root.put("id",FrozenCandidateAdapterR958.PREFIX+"a".repeat(24));root.put("spec","AST3:"+"s".repeat(32763));root.put("state","s".repeat(262144));root.put("structure","a".repeat(64));root.put("evidence","a".repeat(64));root.put("certificate","a".repeat(64));root.put("weight",1.0);root.put("base",base);root.put("target",target);root.put("history_hash","a".repeat(64));root.put("surface_hash","");root.put("history_rows",n);root.put("elapsed_ms",0);root.put("history",new JSONArray());root.put("history_wire",wire);String context="{\"payload\":\""+"x".repeat(262130)+"\"}";root.put("frozen_context",context);root.put("context_hash",V51ChecksumEngine.sha256(context));return root;
 }
 public static void main(String[] args)throws Exception{
  MemoryBudgetCP4 model=new MemoryBudgetCP4(8L<<20),atom=new MemoryBudgetCP4(8L<<20);
  bad(()->RecipeBudgetScopeCP12.requireActive());bad(()->RecipeBudgetScopeCP12.open(new MemoryBudgetCP4(5L<<20),new MemoryBudgetCP4((4L<<20)-1)));ok(model.used()==0&&atom.used()==0);
  StringBuilder wire=new StringBuilder();String issue="202609230001",base="";for(int i=0;i<5000;i++){Models.Draw d=new Models.Draw();d.issue=issue;d.openTime=Long.MAX_VALUE;d.fetchTime=Long.MAX_VALUE;for(int n=0;n<10;n++)d.numbers[n]=n+1;FrozenHistoryWireCP11.append(wire,d);base=issue;issue=TargetIssueResolver.next(issue).targetIssue;}ok(wire.length()==FrozenHistoryWireCP11.MAX_CHARS);
  String encoded;
  try(RecipeBudgetScopeCP12 scope=RecipeBudgetScopeCP12.open(model,atom)){
   ok(model.used()==RecipeBudgetScopeCP12.MODEL_BYTES&&atom.used()==RecipeBudgetScopeCP12.ATOM_BYTES);
   JSONObject root=root(wire.toString(),base,issue,5000);FrozenConsumerRecipeR932.Recipe recipe=new FrozenConsumerRecipeR932.Recipe(root);ok(recipe.frozenHistory.size()==5000);ok(recipe.encoded.length()<1048576);encoded=recipe.encoded;
   Models.Draw first=recipe.frozenHistory.get(0);first.numbers[0]=99;first.issue="MUTATED";ok(recipe.frozenHistory.get(0).numbers[0]==1&&!recipe.frozenHistory.get(0).issue.equals("MUTATED"));
   bad(()->RecipeBudgetScopeCP12.open(model,atom));bad(()->RecipeBudgetScopeCP12.beginRecipe());
  }ok(model.used()==0&&atom.used()==0);bad(()->RecipeBudgetScopeCP12.requireActive());
  try(RecipeBudgetScopeCP12 scope=RecipeBudgetScopeCP12.open(model,atom)){
   FrozenConsumerRecipeR932.Recipe decoded=FrozenConsumerRecipeR932.decode(encoded,issue,CertifiedPredictionR974.LANE,FrozenCandidateAdapterR958.PREFIX+"a".repeat(24),1.0);ok(decoded.frozenHistory.size()==5000);ok(decoded.encoded.equals(encoded));
   PITSnapshotContractR952 pit=PITSnapshotContractR952.fromFrozenHistory(issue,decoded.frozenHistory,pitHash(issue,decoded.frozenHistory));pit.requireConsecutive();ok(pit.history.size()==5000);Models.Draw borrowed=pit.history.get(0);borrowed.numbers[0]=99;ok(pit.history.get(0).numbers[0]==1);
  }ok(model.used()==0&&atom.used()==0);
  try(RecipeBudgetScopeCP12 scope=RecipeBudgetScopeCP12.open(model,atom)){
   JSONObject malformed=root(wire.toString(),base,issue,5000);malformed.put("id","non-frozen");malformed.put("lane","FORMULA_DSL_ACTIVE_SHADOW");malformed.put("weight",.1);
   try{new FrozenConsumerRecipeR932.Recipe(malformed);throw new AssertionError("nonfrozen5000 accepted");}catch(IllegalArgumentException expected){checks++;}
  }ok(model.used()==0&&atom.used()==0);
  Files.writeString(Path.of(args[0]),"{\"status\":\"PASS\",\"assertions\":"+checks+",\"rows\":5000,\"wire_chars\":"+wire.length()+",\"encoded_chars\":"+encoded.length()+",\"java_heap_cap_bytes\":33554432,\"reserved_whole_recipe_bytes\":"+(RecipeBudgetScopeCP12.MODEL_BYTES+RecipeBudgetScopeCP12.ATOM_BYTES)+",\"scope\":\"Exact maxboundRecipe construction/decode, compactPIT defensivecopies and wholelease rollback/release under32MiB hostheap; syntheticbounds payload doesnot claim inferenceAndroidRSS or deviceSLO.\"}\n");
 }
 static String pitHash(String target,List<Models.Draw> source){StringBuilder out=new StringBuilder("R958_PIT_V2|").append(target);for(int i=source.size()-1;i>=0;i--){Models.Draw d=source.get(i);out.append('|').append(d.issue).append(':').append(d.numbersText());}return V51ChecksumEngine.sha256(out.toString());}
}
