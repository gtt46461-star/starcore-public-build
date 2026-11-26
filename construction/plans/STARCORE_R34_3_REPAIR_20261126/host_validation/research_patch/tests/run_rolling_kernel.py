#!/usr/bin/env python3
"""Run unmodified CP9 and exact model/codec source segments with fixture-only platform bindings."""
from pathlib import Path
import re,hashlib,json,subprocess,shutil,os
root=Path(__file__).resolve().parents[1];base=Path(os.environ.get('STARCORE_SOURCE_ROOT',str(root.parents[1]/'source-installed')));out=root/'verification/kernel';src=out/'src';cl=out/'classes';src.mkdir(parents=True,exist_ok=True)
java=base/'app/src/main/java/com/ethk3/novaonepro';pkg='com/ethk3/novaonepro/'
def put(path,text):
 p=src/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
def copy(relative):put(pkg+relative,(java/relative).read_text())
copy('r952/DevelopmentTrainerCP9.java');copy('V51ChecksumEngine.java');copy('TargetIssueResolver.java')
s=(java/'r952/CandidateCompilerR952.java').read_text();body=s[s.index('    public enum ModelKind'):s.index('    public static final class Program')];tail=s[s.index('    public static int top3'):]
put(pkg+'r952/CandidateCompilerR952.java',s[:s.index('/** Bounded frequency')]+ 'public final class CandidateCompilerR952 {\n'+body+tail)
ast=(java/'r952/CandidateAstR952.java').read_text();family=re.search(r'    public enum Family \{.*?\}',ast,re.S).group(0);accept=ast[ast.index('    public static boolean familyAccepts'):ast.index('    public static final class GraphSpec')]
put(pkg+'r952/CandidateAstR952.java','package com.ethk3.novaonepro.r952;\npublic final class CandidateAstR952 {\n'+family+'\n'+accept+'}\n')
put(pkg+'r952/FeatureManifestR952.java','package com.ethk3.novaonepro.r952; import java.util.*; public final class FeatureManifestR952 { public boolean modern;public String hash,graphHash;public int inputDimension;public List<String> orderedFeatureIds; }')
put(pkg+'AppConfig.java','package com.ethk3.novaonepro; public final class AppConfig { public static final String LOTTERY_CODE="Binance1-PK10"; public static int buildK3Mask(int a,int b,int c){return (1<<(a-1))|(1<<(b-1))|(1<<(c-1));}public static int countK3MaskNumbers(int m){return Integer.bitCount(m);} }')
put(pkg+'DatabaseCoordinator.java','package com.ethk3.novaonepro; public final class DatabaseCoordinator { public static final String GAME_CODE="Binance1-PK10"; }')
put(pkg+'Models.java','package com.ethk3.novaonepro; public final class Models {public static final class Draw { public String issue;public long openTime;public boolean isValid(){return true;} } }')
put(pkg+'V50Enums.java','package com.ethk3.novaonepro; public final class V50Enums {public enum FailureCode {UNKNOWN,NONE,TARGET_ISSUE_MISMATCH} }')
put('android/util/Base64.java',(base/'verification/canonical/adapters/android/util/Base64.java').read_text())
t=(base/'verification/canonical/tests/audit_cp12/training/tests/CP9KernelContractCP12.java').read_text();fixture=t[:t.index(' public static void main')].replace('public final class CP9KernelContractCP12','public final class RollingFinalModelReplayR343Test')
fixture+=''' public static void main(String[] args)throws Exception {
  java.lang.reflect.Field access=Dataset.class.getDeclaredField("rows");access.setAccessible(true);
  for(ModelKind kind:ModelKind.values()){
   Dataset d=fixture(kind,0,300);State done=run(d,false);Model finalModel=DevelopmentTrainerCP9.finish(d,done,H);byte[] cp=DevelopmentTrainerCP9.checkpoint(d,done);
   Model loaded=Model.parse(finalModel.payload());List<Row> raw=Arrays.asList(((Row[])access.get(d)).clone());
   Dataset publication=new Dataset(loaded.modern(),d.protocolHash,d.inputHash,d.featureIds,d.missingPolicies,raw,d.trainN);
   State restored=DevelopmentTrainerCP9.restore(publication,cp);ok(restored.complete());
   Model replay=DevelopmentTrainerCP9.finish(publication,restored,H);
   ok(finalModel.hash().equals(replay.hash()));ok(finalModel.payload().equals(replay.payload()));
   ok(finalModel.modern().normalizerHash.equals(replay.modern().normalizerHash));ok(Arrays.equals(finalModel.modern().means(),replay.modern().means()));
   ok(Arrays.equals(finalModel.modern().standardDeviations(),replay.modern().standardDeviations()));ok(restored.orderedPredictionHash().equals(done.orderedPredictionHash()));
   ok(restored.optimizerSteps()==done.optimizerSteps());ok(restored.selectedEpoch()==done.selectedEpoch());
   bad(()->DevelopmentTrainerCP9.restore(fixture(kind,1,300),cp));
   System.out.println(kind+" final_shape_restore=PASS steps="+done.optimizerSteps()+" checkpoint="+cp.length);
  }
  String result="{\\"status\\":\\"PASS\\",\\"model_kinds\\":6,\\"contract_assertions\\":60,\\"assertions_including_transition_bound\\":"+assertions+",\\"transitions\\":"+transitions+",\\"scope\\":\\"unmodified CP9 + exact extracted Modern/Model/codec production segments; test-only platform bindings; not Android/DB owner or quality\\"}";
  Files.writeString(Path.of(args[0]),result+"\\n");System.out.println(result);
 }
}
'''
put(pkg+'r952/RollingFinalModelReplayR343Test.java',fixture)
files=sorted(src.rglob('*.java'));cmd=['java','com.sun.tools.javac.Main','-source','17','-target','17','-d',str(cl),*map(str,files)]
r=subprocess.run(cmd,capture_output=True,text=True);(out/'compile.log').write_text(r.stdout+r.stderr)
if r.returncode:print((r.stdout+r.stderr)[-6000:]);raise SystemExit(r.returncode)
r=subprocess.run(['java','-Xmx128m','-cp',str(cl),'com.ethk3.novaonepro.r952.RollingFinalModelReplayR343Test',str(out/'result.json')],capture_output=True,text=True);(out/'run.log').write_text(r.stdout+r.stderr);print(r.stdout+r.stderr)
(out/'source_binding.json').write_text(json.dumps({'kernel_source_sha256':hashlib.sha256((java/'r952/DevelopmentTrainerCP9.java').read_bytes()).hexdigest(),'compiler_source_sha256':hashlib.sha256((java/'r952/CandidateCompilerR952.java').read_bytes()).hexdigest(),'extracted_model_body_sha256':hashlib.sha256(body.encode()).hexdigest(),'extracted_helpers_sha256':hashlib.sha256(tail.encode()).hexdigest(),'scope':'exact excerpts, fixtures separate; model/codec/trainer math not modified'},indent=2))
raise SystemExit(r.returncode)
