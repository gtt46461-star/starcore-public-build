#!/usr/bin/env python3
from pathlib import Path
import hashlib,json,re,subprocess,tempfile
ROOT=Path(__file__).resolve().parents[3]
SRC=ROOT/'source-installed/app/src/main/java/com/ethk3/novaonepro'
HERE=Path(__file__).resolve().parent
classifier=HERE.parent/'after/app/src/main/java/com/ethk3/novaonepro/r952/CreationChangeKindR343.java'
controller=classifier.parent/'EvolutionControllerR952.java'
with tempfile.TemporaryDirectory(prefix='r343-creation-') as tmp:
 temp=Path(tmp);sources=[]
 def put(rel,text):
  p=temp/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);sources.append(p)
 put('com/ethk3/novaonepro/TypedAtomMoleculeGraphR930.java','''package com.ethk3.novaonepro;public final class TypedAtomMoleculeGraphR930{public enum NodeKind{ATOM,MOLECULE}public static final class NodeSpec{public final NodeKind kind;public final String source;public NodeSpec(boolean atom,String source){kind=atom?NodeKind.ATOM:NodeKind.MOLECULE;this.source=source;}}}''')
 put('com/ethk3/novaonepro/r952/CandidateAstR952.java','''package com.ethk3.novaonepro.r952;import java.util.*;import com.ethk3.novaonepro.*;public final class CandidateAstR952 {public enum Family{FREQUENCY}public final int formatVersion;public final String structuralHash;public final Family family=Family.FREQUENCY;public final GraphSpec graphSpec;public CandidateAstR952(){formatVersion=2;structuralHash="legacy";graphSpec=null;}public CandidateAstR952(Map<String,Object> root){formatVersion=3;graphSpec=new GraphSpec(root);structuralHash=V51ChecksumEngine.sha256(StrictCanonicalJson.writeContext(root));}public static final class GraphSpec{final Map<String,Object> root;public final List<TypedAtomMoleculeGraphR930.NodeSpec> nodes=new ArrayList<>();@SuppressWarnings("unchecked")GraphSpec(Map<String,Object> r){root=r;for(Object o:(List<Object>)r.get("nodes")){Map<String,Object> n=(Map<String,Object>)o;nodes.add(new TypedAtomMoleculeGraphR930.NodeSpec("ATOM".equals(n.get("operator")),String.valueOf(n.get("atomId"))));}}public Map<String,Object> canonicalObject(){return root;}}}''')
 put('com/ethk3/novaonepro/r952/AtomEvaluatorCP4.java','''package com.ethk3.novaonepro.r952;public final class AtomEvaluatorCP4{public static final class Spec{public Spec(Object... fields){}}}''')
 put('com/ethk3/novaonepro/r952/LongSummaryBindingsR80.java','''package com.ethk3.novaonepro.r952;public final class LongSummaryBindingsR80{public static boolean validWindow(int n){throw new AssertionError("Long window not part of fixture");}}''')
 graph=(SRC/'TypedAtomMoleculeGraphR930.java').read_text();expression=re.search(r'case SIGNED_PRODUCT: return ([^;]+);',graph).group(1)
 put('com/ethk3/novaonepro/r952/SignedProductActualKernel.java','package com.ethk3.novaonepro.r952;public final class SignedProductActualKernel{public static double evaluate(double a,double b){double[] values={a,b};return '+expression+';}}')
 sources += [classifier,HERE/'CreationClassificationHarness.java',SRC/'V51ChecksumEngine.java',SRC/'r952/AtomSpecCP4.java',SRC/'r952/StrictCanonicalJson.java']
 subprocess.run(['java','com.sun.tools.javac.Main','-d',str(temp),*[str(p) for p in sources]],check=True)
 result=json.loads(subprocess.check_output(['java','-cp',str(temp),'com.ethk3.novaonepro.r952.CreationClassificationHarness'],text=True))
 text=controller.read_text();classification=text.index('final String creationClassification=');write=text.index('runAtomicWrite("CP12_EVOLUTION_CHILD_TRAIN_COMMIT"',classification)
 assert text.index('check(deadlineNs);',classification)<write
 assert text.index('commitPreparedCandidateCP5(db,prepared,expectation)',write)<text.index('KnowledgeLedgerR952.put(db,action.issue,childId,"R343_CREATION_CLASSIFICATION"',write)<text.index('ResearchTaskQueueR967.complete(db,lease',write)
 result.update({'classification_inside_original_child_commit':True,'classification_after_deadline_check_before_writer':True,'generation_or_quality_authority_changed':False,'android_or_real_transaction':'NOT_RUN','classifier_sha256':hashlib.sha256(classifier.read_bytes()).hexdigest(),'controller_sha256':hashlib.sha256(controller.read_bytes()).hexdigest()})
 print(json.dumps(result,ensure_ascii=False,indent=2))
