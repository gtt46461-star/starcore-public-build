package com.ethk3.novaonepro.strategyv2;

import java.util.*;
import com.ethk3.novaonepro.strategyv2.TypedProgram.*;
import com.ethk3.novaonepro.strategyv2.TypedProgram.Module;

/** Dependency-free positive, negative, phase-boundary and mutation counterexamples. */
public final class TypedProgramTest {
    private static int checks;
    private static final int[] ASC={1,2,3,4,5,6,7,8,9,10};
    private static final int[] LAST={10,2,3,4,5,6,7,8,9,1};
    private static final Frame FRAME=new Frame(10,100,new HistoryRow(9,90,LAST),new HistoryRow(8,80,ASC));
    private static void ok(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
    private static void rejected(String code,Runnable action){checks++;try{action.run();throw new AssertionError("Expected rejection: "+code);}catch(IllegalArgumentException expected){if(!expected.getMessage().contains(code))throw new AssertionError(code+" got "+expected,expected);}}
    private static Module uniform(String name){double[] p=new double[10];Arrays.fill(p,.1);return new Module(name,Phase.PREDICT,new Type[]{Type.POSITION},Arrays.asList(Node.parameter("p",Type.POSITION,0),Node.constant("out",Value.probability(p))),"out");}
    private static TypedProgram program(Module main,Module... extras){List<Module> all=new ArrayList<>();all.add(main);all.addAll(Arrays.asList(extras));return new TypedProgram(all,main.name,0,Collections.emptyList(),Limits.research());}
    private static Module setOperation(String name,Op op){return new Module(name,Phase.PREDICT,new Type[0],Arrays.asList(Node.constant("a",Value.set(21)),Node.constant("b",Value.set(12)),Node.of("out",op,Type.SET10,"a","b")),"out");}
    private static TypedProgram historyScorer(){
        Module feature=new Module("feature",Phase.PREDICT,new Type[]{Type.NUMBER,Type.POSITION},Arrays.asList(
                Node.parameter("n",Type.NUMBER,0),Node.parameter("p",Type.POSITION,1),Node.history("last","p",1),
                Node.of("match",Op.EQ,Type.BOOL,"n","last"),Node.constant("yes",Value.scalar(2)),Node.constant("no",Value.scalar(0)),
                Node.of("out",Op.IF,Type.SCALAR,"match","yes","no")),"out");
        Module main=new Module("main",Phase.PREDICT,new Type[]{Type.POSITION},Arrays.asList(Node.parameter("p",Type.POSITION,0),
                Node.bind("scores",Op.SCORE_EACH_NUMBER,"feature","p"),Node.of("out",Op.SOFTMAX,Type.PROB10,"scores")),"out");
        Module picks=new Module("picks",Phase.PREDICT,new Type[]{Type.POSITION},Arrays.asList(Node.parameter("p",Type.POSITION,0),
                Node.call("distribution","main",Type.PROB10,"p"),Node.of("out",Op.TOP3,Type.SET10,"distribution")),"out");
        return program(main,feature,picks);
    }
    public static void main(String[] args){
        TypedProgram base=program(uniform("main"));State empty=base.initialState(9);
        Decision d=base.predict(FRAME,empty);
        ok(d.position==1&&d.mask==7,"deterministic position and K3 tie break");
        ok(Math.abs(d.modelHitProbability-.3)<1e-12,"exact three-way union sum");
        ok(base.predict(FRAME,empty).mask==d.mask&&empty.version==0,"pure repeatable prediction");

        TypedProgram sets=program(uniform("main"),setOperation("union",Op.SET_UNION),setOperation("intersection",Op.SET_INTERSECTION),setOperation("difference",Op.SET_DIFFERENCE));
        State setState=sets.initialState(9);
        ok(sets.invoke("union",FRAME,setState).integer()==29,"SET10 union");
        ok(sets.invoke("intersection",FRAME,setState).integer()==4,"SET10 intersection");
        ok(sets.invoke("difference",FRAME,setState).integer()==17,"SET10 difference");
        Module extras=new Module("extra",Phase.PREDICT,new Type[0],Arrays.asList(Node.constant("s",Value.set(21)),
                Node.of("complement",Op.SET_COMPLEMENT,Type.SET10,"s"),Node.of("out",Op.SET_COUNT,Type.SCALAR,"complement")),"out");
        TypedProgram counted=program(uniform("main"),extras);
        ok(counted.invoke("extra",FRAME,counted.initialState(9)).scalar()==7,"SET10 bounded complement count");

        Module predicate=new Module("small",Phase.PREDICT,new Type[]{Type.NUMBER},Arrays.asList(Node.parameter("n",Type.NUMBER,0),
                Node.constant("four",Value.number(4)),Node.of("out",Op.LESS,Type.BOOL,"n","four")),"out");
        Module members=new Module("members",Phase.PREDICT,new Type[0],Collections.singletonList(Node.bind("out",Op.SET_WHERE,"small")),"out");
        Module setMain=new Module("main",Phase.PREDICT,new Type[]{Type.POSITION},Arrays.asList(Node.parameter("p",Type.POSITION,0),
                Node.call("set","members",Type.SET10),Node.of("score",Op.SET_TO_SCORE,Type.SCORE10,"set"),Node.of("out",Op.SOFTMAX,Type.PROB10,"score")),"out");
        TypedProgram bound=program(setMain,predicate,members);
        ok(bound.invoke("members",FRAME,bound.initialState(9)).integer()==7,"lexically bound number iteration is 1..10");
        ok(bound.predict(FRAME,bound.initialState(9)).mask==7,"bound set changes score and decision");

        TypedProgram parent=historyScorer();State parentState=parent.initialState(9);
        ok(parent.invoke("feature",FRAME,parentState,Value.number(10),Value.position(1)).scalar()==2,"raw history value consumed");
        ok(parent.invoke("feature",FRAME,parentState,Value.number(1),Value.position(1)).scalar()==0,"same history different bound entity");
        ok(parent.invoke("picks",FRAME,parentState,Value.position(1)).integer()==515,"history-dependent three-number selection");
        TypedProgram child=parent.replaceOperation("feature","match",Op.NE);State childState=child.initialState(9);
        ok(child.invoke("picks",FRAME,childState,Value.position(1)).integer()==7,"typed structural mutation changes executable behavior");
        ok(!parent.codeHash.equals(child.codeHash)&&!parent.semanticHash.equals(child.semanticHash),"child has fresh identities");
        rejected("STATE_PROGRAM_IDENTITY",()->child.predict(FRAME,parentState));
        rejected("TYPE:ADD",()->parent.replaceOperation("feature","match",Op.ADD));
        rejected("MUTATION_NO_SEMANTIC_CHANGE",()->parent.replaceOperation("feature","match",Op.EQ));
        rejected("RUNTIME_ARGUMENT_TYPE",()->parent.invoke("feature",FRAME,parentState,Value.position(1),Value.position(1)));

        double[] leftP=new double[10],rightP=new double[10];leftP[0]=1;rightP[1]=1;
        Module left=new Module("left",Phase.PREDICT,new Type[]{Type.POSITION},Collections.singletonList(Node.constant("out",Value.probability(leftP))),"out");
        Module right=new Module("right",Phase.PREDICT,new Type[]{Type.POSITION},Collections.singletonList(Node.constant("out",Value.probability(rightP))),"out");
        TypedProgram parents=program(left,right),fusion=parents.withProbabilityFusion("fused","left","right",.25);
        double[] mixed=fusion.invoke("fused",FRAME,fusion.initialState(9),Value.position(1)).vector();
        ok(mixed[0]==.25&&mixed[1]==.75,"fusion is actual probability mixture");
        Module duplicate=new Module("alias",Phase.PREDICT,new Type[]{Type.POSITION},Collections.singletonList(Node.constant("another",Value.probability(leftP))),"another");
        TypedProgram duplicateProgram=program(left,duplicate);
        rejected("FUSION_DUPLICATE",()->duplicateProgram.withProbabilityFusion("fused","left","alias",.5));

        Module update=new Module("update",Phase.SETTLE,new Type[0],Arrays.asList(Node.state("old",0),Node.constant("p",Value.position(1)),
                Node.of("actual",Op.SETTLED_NUMBER,Type.NUMBER,"p"),Node.of("numeric",Op.TO_SCALAR,Type.SCALAR,"actual"),Node.of("out",Op.ADD,Type.SCALAR,"old","numeric")),"out");
        Module read=new Module("read",Phase.PREDICT,new Type[0],Collections.singletonList(Node.state("out",0)),"out");
        TypedProgram online=new TypedProgram(Arrays.asList(uniform("main"),update,read),"main",1,Collections.singletonList(new Update(0,"update")),Limits.research());
        State old=online.initialState(9),next=online.settle(FRAME,old,new Settlement(10,120,LAST));
        ok(old.value(0)==0&&next.value(0)==10&&next.version==1&&next.settledThrough==10,"atomic immutable online update");
        rejected("STATE_TARGET_SEQUENCE",()->online.settle(FRAME,next,new Settlement(10,120,LAST)));
        rejected("POST_INVOKE_FORBIDDEN",()->online.invoke("update",FRAME,old));
        rejected("SETTLEMENT_PHASE",()->online.settle(FRAME,old,new Settlement(10,100,LAST)));
        rejected("SETTLEMENT_PHASE",()->online.settle(FRAME,old,new Settlement(11,120,LAST)));

        Module overflow=new Module("overflow",Phase.SETTLE,new Type[0],Arrays.asList(Node.constant("a",Value.scalar(Double.MAX_VALUE)),
                Node.of("out",Op.MULTIPLY,Type.SCALAR,"a","a")),"out");
        TypedProgram atomic=new TypedProgram(Arrays.asList(uniform("main"),update,overflow),"main",2,Arrays.asList(new Update(0,"update"),new Update(1,"overflow")),Limits.research());
        State before=atomic.initialState(9);
        rejected("VALUE_SCALAR",()->atomic.settle(FRAME,before,new Settlement(10,120,LAST)));
        ok(before.value(0)==0&&before.value(1)==0&&before.version==0,"failed second RHS commits no state");

        rejected("PIT_HISTORY",()->new Frame(10,100,new HistoryRow(10,90,ASC)));
        rejected("PIT_HISTORY",()->new Frame(10,100,new HistoryRow(9,101,ASC)));
        rejected("PIT_HISTORY",()->new Frame(10,100,new HistoryRow(8,90,ASC)));
        rejected("PERMUTATION_VALUE",()->new HistoryRow(9,90,1,1,3,4,5,6,7,8,9,10));
        rejected("SET_RANGE",()->Value.set(1024));
        rejected("PROB_SUM",()->Value.probability(new double[10]));
        Module leak=new Module("leak",Phase.PREDICT,new Type[]{Type.POSITION},Arrays.asList(Node.parameter("p",Type.POSITION,0),Node.of("out",Op.SETTLED_NUMBER,Type.NUMBER,"p")),"out");
        rejected("POST_LABEL_IN_PREDICTION",()->program(uniform("main"),leak));
        Module callsPost=new Module("callsPost",Phase.PREDICT,new Type[0],Collections.singletonList(Node.call("out","update",Type.SCALAR)),"out");
        rejected("POST_LABEL_IN_PREDICTION",()->new TypedProgram(Arrays.asList(uniform("main"),update,callsPost),"main",1,Collections.emptyList(),Limits.research()));
        Module forward=new Module("forward",Phase.PREDICT,new Type[0],Arrays.asList(Node.of("out",Op.ADD,Type.SCALAR,"later","later"),Node.constant("later",Value.scalar(1))),"out");
        rejected("DAG_PARENT_ORDER",()->program(uniform("main"),forward));
        Module recurse=new Module("recurse",Phase.PREDICT,new Type[0],Collections.singletonList(Node.call("out","recurse",Type.SCALAR)),"out");
        rejected("MODULE_RECURSION",()->program(uniform("main"),recurse));
        Module free=new Module("free",Phase.PREDICT,new Type[0],Collections.singletonList(Node.parameter("out",Type.NUMBER,0)),"out");
        rejected("PARAM_TYPE",()->program(uniform("main"),free));
        Module positionPredicate=new Module("wrongPredicate",Phase.PREDICT,new Type[]{Type.POSITION},Collections.singletonList(Node.constant("out",Value.bool(true))),"out");
        Module wrongBind=new Module("wrongBind",Phase.PREDICT,new Type[0],Collections.singletonList(Node.bind("out",Op.SET_WHERE,"wrongPredicate")),"out");
        rejected("BIND_ENTITY_TYPE",()->program(uniform("main"),positionPredicate,wrongBind));
        rejected("PREDICTION_STEP_BUDGET",()->new TypedProgram(Collections.singletonList(uniform("main")),"main",0,Collections.emptyList(),new Limits(2,20,2,2,0,5)));
        Module distant=new Module("distant",Phase.PREDICT,new Type[]{Type.POSITION},Arrays.asList(Node.parameter("p",Type.POSITION,0),Node.history("out","p",2)),"out");
        TypedProgram needsTwo=program(uniform("main"),distant);
        Frame one=new Frame(10,100,new HistoryRow(9,90,LAST));
        rejected("HISTORY_UNAVAILABLE",()->needsTwo.invoke("distant",one,needsTwo.initialState(9),Value.position(1)));

        TypedProgram renamed=program(uniform("renamed"));
        ok(base.semanticHash.equals(renamed.semanticHash)&&!base.codeHash.equals(renamed.codeHash),"semantic signature ignores module/node naming only");
        System.out.println("TYPED_PROGRAM_REFERENCE_PASS checks="+checks+" scope=JDK_REFERENCE_ONLY_NOT_ANDROID_OR_PREDICTION_QUALITY");
    }
}
