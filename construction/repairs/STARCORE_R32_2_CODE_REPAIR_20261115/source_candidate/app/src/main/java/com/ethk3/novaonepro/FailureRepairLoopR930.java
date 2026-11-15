package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * R930 C09/C12 failure -> real module ablation -> falsifiable minimal repair -> executable creative
 * program -> fresh Future evidence loop. Counterfactual executions are read-only by contract.
 */
public final class FailureRepairLoopR930 {
    public static final String REVISION="R930_FAILURE_REPAIR_V1";
    public static final int MIN_FAILURE_SUPPORT=12;
    private FailureRepairLoopR930(){}

    public enum Module { FTRL,TINY_DEEP,TEACHER,TREND,SHAPE,RELATION,INVENTED,NEGATIVE_CONTROL }
    public enum FailureKind { CAPTURE,EXECUTION,POSITION,THIRD_BOUNDARY,CALIBRATION,DOMAIN,CONFLICT,RANDOM_RESIDUAL,WAIT_FOR_FAILURE_EVIDENCE }
    public enum Novelty { PARAMETER_VARIANT,STRUCTURAL_CHANGE,NEW_FAMILY }
    public enum Op { ATOM,CONST,ADD,SUB,MUL,SAFE_DIV,MIN,MAX,TANH,ABS,CLIP01 }

    public static final class FailureCase {
        public final String targetIssue,inputHash,selectedActionHash,selectionPolicyHash,calibrationHash;
        public final String teacherHash,trendHash,shapeHash,relationHash,consumerHash,stateHash;
        public final double rawScore,rawActionP,calibratedP,thirdMargin,fourthBoundary;
        public final boolean hit; public final String failureCaseHash;
        public FailureCase(String issue,String inputHash,String selectedHash,String selectionPolicyHash,String calibrationHash,
                           String teacherHash,String trendHash,String shapeHash,String relationHash,String consumerHash,String stateHash,
                           double rawScore,double rawActionP,double calibratedP,double thirdMargin,double fourthBoundary,boolean hit){
            this.targetIssue=required(issue);this.inputHash=hash(inputHash);this.selectedActionHash=hash(selectedHash);
            this.selectionPolicyHash=hash(selectionPolicyHash);this.calibrationHash=hash(calibrationHash);this.teacherHash=hash(teacherHash);
            this.trendHash=hash(trendHash);this.shapeHash=hash(shapeHash);this.relationHash=hash(relationHash);this.consumerHash=hash(consumerHash);this.stateHash=hash(stateHash);
            this.rawScore=finiteOrNaN(rawScore);this.rawActionP=probOrNaN(rawActionP);this.calibratedP=probOrNaN(calibratedP);
            this.thirdMargin=finiteOrNaN(thirdMargin);this.fourthBoundary=finiteOrNaN(fourthBoundary);this.hit=hit;
            failureCaseHash=V51ChecksumEngine.sha256(REVISION+'|'+targetIssue+'|'+this.inputHash+'|'+this.selectedActionHash+'|'+this.selectionPolicyHash+'|'+this.calibrationHash+'|'+this.teacherHash+'|'+this.trendHash+'|'+this.shapeHash+'|'+this.relationHash+'|'+this.consumerHash+'|'+this.stateHash+'|'+fmt(rawScore)+'|'+fmt(rawActionP)+'|'+fmt(calibratedP)+'|'+fmt(thirdMargin)+'|'+fmt(fourthBoundary)+'|'+hit);
        }
    }

    /** Must rerun the complete selection chain on the exact same frozen input with one module disabled. */
    public interface AblationRunner { AblationResult rerun(FailureCase failure,Module disabledModule); }
    public static final class AblationResult {
        public final Module disabledModule;public final String inputHash,selectedActionHash,runHash;
        public final double selectedScore,selectedProbability;public final boolean hit,canonicalWriteAttempted;
        public final long elapsedNs;public final String status;
        public AblationResult(Module disabledModule,String inputHash,String selectedActionHash,double selectedScore,double selectedProbability,
                              boolean hit,boolean canonicalWriteAttempted,long elapsedNs,String status){
            this.disabledModule=disabledModule;this.inputHash=hash(inputHash);this.selectedActionHash=hash(selectedActionHash);
            this.selectedScore=finiteOrNaN(selectedScore);this.selectedProbability=probOrNaN(selectedProbability);this.hit=hit;
            this.canonicalWriteAttempted=canonicalWriteAttempted;this.elapsedNs=Math.max(0,elapsedNs);this.status=required(status);
            if(canonicalWriteAttempted)throw new IllegalStateException("R930_COUNTERFACTUAL_CANONICAL_WRITE_FORBIDDEN");
            this.runHash=V51ChecksumEngine.sha256(REVISION+'|'+disabledModule+'|'+this.inputHash+'|'+this.selectedActionHash+'|'+fmt(selectedScore)+'|'+fmt(selectedProbability)+'|'+hit+'|'+elapsedNs+'|'+this.status);
        }
    }
    public static final class AblationReport {
        public final FailureCase failure;public final Map<Module,AblationResult> results;public final String reportHash;
        AblationReport(FailureCase failure,Map<Module,AblationResult> results){this.failure=failure;this.results=Collections.unmodifiableMap(new LinkedHashMap<Module,AblationResult>(results));StringBuilder b=new StringBuilder(failure.failureCaseHash);for(Module m:Module.values()){AblationResult r=this.results.get(m);if(r!=null)b.append('|').append(r.runHash);}reportHash=V51ChecksumEngine.sha256(b.toString());}
    }

    public static AblationReport runRealAblations(FailureCase failure,AblationRunner runner){
        if(failure==null||runner==null)throw new IllegalArgumentException("R930_ABLATION_INPUT");LinkedHashMap<Module,AblationResult> out=new LinkedHashMap<Module,AblationResult>();
        for(Module module:Module.values()){
            AblationResult r=runner.rerun(failure,module);if(r==null||r.disabledModule!=module)throw new IllegalStateException("R930_ABLATION_RESULT_MISMATCH:"+module);
            if(!failure.inputHash.equals(r.inputHash))throw new IllegalStateException("R930_ABLATION_INPUT_DRIFT:"+module);out.put(module,r);
        }
        AblationResult neg=out.get(Module.NEGATIVE_CONTROL);if(neg!=null&&!failure.selectedActionHash.equals(neg.selectedActionHash))throw new IllegalStateException("R930_NEGATIVE_CONTROL_CHANGED_SELECTION");
        return new AblationReport(failure,out);
    }

    public static FailureKind classify(FailureCase failure,AblationReport ablation,int failureSupport){
        if(failureSupport<MIN_FAILURE_SUPPORT)return FailureKind.WAIT_FOR_FAILURE_EVIDENCE;
        if(failure==null)return FailureKind.EXECUTION;
        if(failure.hit)return FailureKind.RANDOM_RESIDUAL;
        if(!finite(failure.calibratedP))return FailureKind.CALIBRATION;
        if(finite(failure.thirdMargin)&&failure.thirdMargin<0.01d)return FailureKind.THIRD_BOUNDARY;
        if(ablation!=null){
            AblationResult relation=ablation.results.get(Module.RELATION),teacher=ablation.results.get(Module.TEACHER);
            if(relation!=null&&relation.hit)return FailureKind.CONFLICT;
            if(teacher!=null&&teacher.hit)return FailureKind.CONFLICT;
        }
        return FailureKind.RANDOM_RESIDUAL;
    }

    public static final class RepairPlan {
        public final String repairId,parentId,repairTarget,changedModules,atomIds,expectedEffect,falsification;
        public final int inheritedN=0;public final String planHash;public final boolean ready;
        RepairPlan(String parentId,String target,String modules,String atomIds,String expected,String falsification,boolean ready){
            this.parentId=required(parentId);this.repairTarget=required(target);this.changedModules=required(modules);this.atomIds=required(atomIds);this.expectedEffect=required(expected);this.falsification=required(falsification);this.ready=ready;
            planHash=V51ChecksumEngine.sha256(REVISION+"|REPAIR|"+this.parentId+'|'+this.repairTarget+'|'+this.changedModules+'|'+this.atomIds+'|'+this.expectedEffect+'|'+this.falsification);repairId="REPAIR-"+planHash.substring(0,20);
        }
    }

    public static RepairPlan proposeMinimalRepair(String parentId,FailureKind kind,AblationReport report,int failureSupport,String atomIds){
        if(failureSupport<MIN_FAILURE_SUPPORT||kind==FailureKind.WAIT_FOR_FAILURE_EVIDENCE)return new RepairPlan(parentId,"WAIT_FOR_FAILURE_EVIDENCE","NONE",atomIds,"NO_CHANGE_UNTIL_SUPPORT","FAIL_IF_ANY_PRODUCTION_MUTATION_BEFORE_SUPPORT",false);
        String module="NONE",expected="reduce same failure category on new paired Future",falsify="no paired improvement on preregistered Future window";
        if(report!=null){double best=Double.NEGATIVE_INFINITY;for(Map.Entry<Module,AblationResult> e:report.results.entrySet()){if(e.getKey()==Module.NEGATIVE_CONTROL)continue;AblationResult r=e.getValue();double gain=(r.hit?1.0d:0.0d)-(report.failure.hit?1.0d:0.0d);if(gain>best){best=gain;module=e.getKey().name();}}}
        return new RepairPlan(parentId,kind.name(),module,atomIds,expected,falsify,true);
    }

    public static final class Instruction {
        public final Op op; public final String atomId; public final double constant; public final int left,right;
        private Instruction(Op op,String atomId,double constant,int left,int right){this.op=op;this.atomId=atomId==null?"":atomId;this.constant=constant;this.left=left;this.right=right;}
        public static Instruction atom(String id){return new Instruction(Op.ATOM,required(id),0,-1,-1);}public static Instruction constant(double v){if(!finite(v))throw new IllegalArgumentException("R930_CONST");return new Instruction(Op.CONST,"",v,-1,-1);}public static Instruction unary(Op op,int left){return new Instruction(op,"",0,left,-1);}public static Instruction binary(Op op,int left,int right){return new Instruction(op,"",0,left,right);}
        String canonical(){return op.name()+"|"+atomId+'|'+fmt(constant)+'|'+left+'|'+right;}
    }
    public static final class CreativeProgram {
        public final String id,nameZh,familyId,parentId;public final Novelty novelty;public final List<Instruction> code;public final String[] atomIds;
        public final String purpose,failureCondition,programHash;public final int futureN=0;
        public CreativeProgram(String nameZh,String familyId,String parentId,Novelty novelty,List<Instruction> code,String purpose,String failureCondition){
            this.nameZh=required(nameZh);this.familyId=required(familyId);this.parentId=parentId==null?"":parentId.trim();this.novelty=novelty==null?Novelty.STRUCTURAL_CHANGE:novelty;this.code=Collections.unmodifiableList(new ArrayList<Instruction>(code));this.purpose=required(purpose);this.failureCondition=required(failureCondition);
            if(this.code.isEmpty()||this.code.size()>128)throw new IllegalArgumentException("R930_PROGRAM_SIZE");ArrayList<String> atoms=new ArrayList<String>();StringBuilder b=new StringBuilder(REVISION).append('|').append(this.nameZh).append('|').append(this.familyId).append('|').append(this.parentId).append('|').append(this.novelty).append('|').append(this.purpose).append('|').append(this.failureCondition);
            for(int i=0;i<this.code.size();i++){Instruction x=this.code.get(i);validateInstruction(x,i);if(x.op==Op.ATOM&&!atoms.contains(x.atomId))atoms.add(x.atomId);b.append('|').append(x.canonical());}
            this.atomIds=atoms.toArray(new String[atoms.size()]);programHash=V51ChecksumEngine.sha256(b.toString());id="R930-"+programHash.substring(0,24);
        }
    }

    /** Stack-index program: every operand must reference a prior instruction, preventing cycles. */
    private static void validateInstruction(Instruction x,int index){if(x==null)throw new IllegalArgumentException("R930_PROGRAM_NULL_INSTRUCTION");if(x.op==Op.ATOM){GenesisSchemaRegistryR915.AtomSpec found=findAtom(x.atomId);if(found==null||!found.productionScoreEligible())throw new IllegalArgumentException("R930_PROGRAM_ATOM_NOT_PREDRAW_PRODUCTION:"+x.atomId);return;}if(x.op==Op.CONST)return;if(x.left<0||x.left>=index)throw new IllegalArgumentException("R930_PROGRAM_LEFT_DAG");if(isBinary(x.op)&&(x.right<0||x.right>=index))throw new IllegalArgumentException("R930_PROGRAM_RIGHT_DAG");}
    private static boolean isBinary(Op op){return op==Op.ADD||op==Op.SUB||op==Op.MUL||op==Op.SAFE_DIV||op==Op.MIN||op==Op.MAX;}

    public static double execute(CreativeProgram program,Map<String,AtomQualityGraphR930.ValueResult> atoms){
        if(program==null||atoms==null)throw new IllegalArgumentException("R930_EXEC_INPUT");double[] v=new double[program.code.size()];
        for(int i=0;i<v.length;i++){Instruction x=program.code.get(i);switch(x.op){case ATOM:AtomQualityGraphR930.ValueResult a=atoms.get(x.atomId);if(a==null||!a.present)throw new IllegalStateException("R930_PROGRAM_ATOM_MISSING:"+x.atomId);v[i]=a.value;break;case CONST:v[i]=x.constant;break;case ADD:v[i]=v[x.left]+v[x.right];break;case SUB:v[i]=v[x.left]-v[x.right];break;case MUL:v[i]=v[x.left]*v[x.right];break;case SAFE_DIV:v[i]=Math.abs(v[x.right])<1e-9?0:v[x.left]/v[x.right];break;case MIN:v[i]=Math.min(v[x.left],v[x.right]);break;case MAX:v[i]=Math.max(v[x.left],v[x.right]);break;case TANH:v[i]=Math.tanh(v[x.left]);break;case ABS:v[i]=Math.abs(v[x.left]);break;case CLIP01:v[i]=Math.max(0,Math.min(1,v[x.left]));break;default:throw new IllegalStateException("R930_PROGRAM_OP");}if(!finite(v[i]))throw new IllegalStateException("R930_PROGRAM_NONFINITE:"+i);}
        return v[v.length-1];
    }

    public static CreativeProgram minimalChild(RepairPlan plan,String familyId,String atomA,String atomB){
        if(plan==null||!plan.ready)throw new IllegalStateException("R930_REPAIR_NOT_READY");ArrayList<Instruction> code=new ArrayList<Instruction>();code.add(Instruction.atom(atomA));code.add(Instruction.atom(atomB));code.add(Instruction.binary(Op.SUB,0,1));code.add(Instruction.unary(Op.TANH,2));String zh=chineseName(plan.repairTarget,plan.changedModules);return new CreativeProgram(zh,familyId,plan.parentId,Novelty.STRUCTURAL_CHANGE,code,"定向修复:"+plan.repairTarget,plan.falsification);
    }
    public static String chineseName(String target,String modules){return "定向"+cleanZh(target)+"·"+cleanZh(modules)+"残差结构";}
    private static String cleanZh(String s){String v=required(s).replace('_','·');return v.length()>18?v.substring(0,18):v;}
    private static GenesisSchemaRegistryR915.AtomSpec findAtom(String id){for(GenesisSchemaRegistryR915.AtomSpec a:GenesisSchemaRegistryR915.atoms())if(a.id.equals(id))return a;return null;}
    private static double probOrNaN(double x){return finite(x)&&x>=0&&x<=1?x:Double.NaN;}private static double finiteOrNaN(double x){return finite(x)?x:Double.NaN;}private static boolean finite(double x){return !Double.isNaN(x)&&!Double.isInfinite(x);}private static String required(String x){String v=x==null?"":x.trim();if(v.length()==0)throw new IllegalArgumentException("R930_REPAIR_REQUIRED");return v;}private static String hash(String x){String v=required(x).toLowerCase(Locale.US);if(!v.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R930_REPAIR_HASH");return v;}private static String fmt(double x){return finite(x)?String.format(Locale.US,"%.8f",x):"N/A";}
}
