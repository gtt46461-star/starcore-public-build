package com.ethk3.novaonepro;

import java.util.*;

/** Focused E03A contract fixture over the production PitFeatureDagR929 implementation. */
public final class PitDagAcceptanceHarnessR932 {
    private static int checks;
    private static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;System.out.println(name+"=PASS");}
    private static PitFeatureDagR929.FeatureSpec spec(String id,String semantic,List<String> parents,PitFeatureDagR929.TemporalKind temporal){
        return new PitFeatureDagR929.FeatureSpec(id,semantic,"unit",PitFeatureDagR929.ValueType.SCALAR,
                new PitFeatureDagR929.Shape(1,1),temporal,parents,1,4,"HISTORY","EXPLICIT_MASK",1_000_000L,"v1");
    }
    private static PitFeatureDagR929.Manifest manifest(PitFeatureDagR929.FeatureSpec... specs){
        return new PitFeatureDagR929.Manifest(PitFeatureDagR929.GAME,"ACTION_META_1200","E03A_TEST",Arrays.asList(specs));
    }
    private static void rejects(Runnable action,String marker,String name){
        boolean rejected=false;try{action.run();}catch(RuntimeException expected){rejected=expected.toString().contains(marker);}
        check(rejected,name);
    }
    public static void main(String[] args){
        final String source="202609100001",target="202609100002";final long frozen=2000L;

        PitFeatureDagR929.FeatureSpec zeroSpec=spec("zero","legal_zero",Collections.<String>emptyList(),PitFeatureDagR929.TemporalKind.PREDRAW);
        Map<String,PitFeatureDagR929.Node> zeroNodes=new HashMap<String,PitFeatureDagR929.Node>();
        zeroNodes.put("zero",new PitFeatureDagR929.Node(zeroSpec,ctx->PitFeatureDagR929.ValueResult.present(new double[]{0.0d},source,1500L,10L,"zero-source")));
        PitFeatureDagR929.FrozenGraph zero=PitFeatureDagR929.freeze(manifest(zeroSpec),target,frozen,zeroNodes);
        check(zero.require("zero").isPresent(0)&&zero.require("zero").value(0)==0.0d
                        && zero.require("zero").missingReason(0)==PitFeatureDagR929.MissingReason.NONE,
                "LEGAL_ZERO_REMAINS_PRESENT_NOT_MISSING");

        PitFeatureDagR929.FeatureSpec missingSpec=spec("missing","legacy_unknown",Collections.<String>emptyList(),PitFeatureDagR929.TemporalKind.PREDRAW);
        Map<String,PitFeatureDagR929.Node> missingNodes=new HashMap<String,PitFeatureDagR929.Node>();
        missingNodes.put("missing",new PitFeatureDagR929.Node(missingSpec,ctx->PitFeatureDagR929.ValueResult.missing(1,PitFeatureDagR929.MissingReason.LEGACY_UNKNOWN,source,1500L,10L,"legacy-source")));
        PitFeatureDagR929.FrozenGraph missing=PitFeatureDagR929.freeze(manifest(missingSpec),target,frozen,missingNodes);
        check(!missing.require("missing").isPresent(0)&&missing.require("missing").value(0)==0.0d
                        && missing.require("missing").missingReason(0)==PitFeatureDagR929.MissingReason.LEGACY_UNKNOWN,
                "LEGACY_UNKNOWN_ZERO_STORAGE_DOES_NOT_UPGRADE_TO_PRESENT_ZERO");

        final PitFeatureDagR929.FeatureSpec expected=spec("semantic","expected_semantic",Collections.<String>emptyList(),PitFeatureDagR929.TemporalKind.PREDRAW);
        final PitFeatureDagR929.FeatureSpec wrong=spec("semantic","wrong_same_shape_semantic",Collections.<String>emptyList(),PitFeatureDagR929.TemporalKind.PREDRAW);
        final Map<String,PitFeatureDagR929.Node> wrongNodes=new HashMap<String,PitFeatureDagR929.Node>();
        wrongNodes.put("semantic",new PitFeatureDagR929.Node(wrong,ctx->PitFeatureDagR929.ValueResult.present(new double[]{1.0d},source,1500L,10L,"semantic-source")));
        rejects(()->PitFeatureDagR929.freeze(manifest(expected),target,frozen,wrongNodes),"R929_NODE_SPEC_MISMATCH","SAME_SHAPE_WRONG_SEMANTIC_REJECTED");

        rejects(()->manifest(spec("label","actual_target",Collections.<String>emptyList(),PitFeatureDagR929.TemporalKind.POSTDRAW_LABEL)),
                "R929_LABEL_IN_PRODUCTION_DAG","POSTDRAW_LABEL_CANNOT_ENTER_PRODUCTION_DAG");

        final PitFeatureDagR929.FeatureSpec cycA=spec("a","a",Arrays.asList("b"),PitFeatureDagR929.TemporalKind.PREDRAW);
        final PitFeatureDagR929.FeatureSpec cycB=spec("b","b",Arrays.asList("a"),PitFeatureDagR929.TemporalKind.PREDRAW);
        rejects(()->manifest(cycA,cycB),"R929_FEATURE_CYCLE","FEATURE_GRAPH_CYCLE_REJECTED");

        final PitFeatureDagR929.FeatureSpec futureSpec=spec("future","future_source",Collections.<String>emptyList(),PitFeatureDagR929.TemporalKind.PREDRAW);
        final Map<String,PitFeatureDagR929.Node> futureNodes=new HashMap<String,PitFeatureDagR929.Node>();
        futureNodes.put("future",new PitFeatureDagR929.Node(futureSpec,ctx->PitFeatureDagR929.ValueResult.present(new double[]{1.0d},target,1500L,10L,"future-source")));
        rejects(()->PitFeatureDagR929.freeze(manifest(futureSpec),target,frozen,futureNodes),"R929_TIME_TRAVEL","TARGET_OR_FUTURE_SOURCE_REJECTED");

        final PitFeatureDagR929.FeatureSpec lateSpec=spec("late","late_available",Collections.<String>emptyList(),PitFeatureDagR929.TemporalKind.PREDRAW);
        final Map<String,PitFeatureDagR929.Node> lateNodes=new HashMap<String,PitFeatureDagR929.Node>();
        lateNodes.put("late",new PitFeatureDagR929.Node(lateSpec,ctx->PitFeatureDagR929.ValueResult.present(new double[]{1.0d},source,frozen+1,10L,"late-source")));
        rejects(()->PitFeatureDagR929.freeze(manifest(lateSpec),target,frozen,lateNodes),"R929_VALUE_AVAILABLE_AFTER_FREEZE","LATE_AVAILABILITY_REJECTED");

        final PitFeatureDagR929.FeatureSpec parent=spec("parent","parent",Collections.<String>emptyList(),PitFeatureDagR929.TemporalKind.PREDRAW);
        final PitFeatureDagR929.FeatureSpec child=spec("child","child",Arrays.asList("parent"),PitFeatureDagR929.TemporalKind.PREDRAW);
        final Map<String,PitFeatureDagR929.Node> parentLateNodes=new HashMap<String,PitFeatureDagR929.Node>();
        parentLateNodes.put("parent",new PitFeatureDagR929.Node(parent,ctx->PitFeatureDagR929.ValueResult.present(new double[]{1},source,1800L,10L,"p")));
        parentLateNodes.put("child",new PitFeatureDagR929.Node(child,ctx->PitFeatureDagR929.ValueResult.present(new double[]{2},source,1700L,10L,"c")));
        rejects(()->PitFeatureDagR929.freeze(manifest(parent,child),target,frozen,parentLateNodes),"R929_PARENT_AVAILABLE_AFTER_CHILD","CHILD_CANNOT_PRECEDE_PARENT_AVAILABILITY");

        int[] actual={1,2,3,4,5,6,7,8,9,10};
        PitFeatureDagR929.LabelJoin label=new PitFeatureDagR929.LabelJoin(PitFeatureDagR929.GAME,target,actual,2500L,2600L,"v1");
        check(label.actualAtPosition(1)==1&&label.actualAtPosition(10)==10,"LABEL_JOIN_IS_SEPARATE_POSTDRAW_OBJECT");

        System.out.println("R932_E03A_PIT_DAG_ACCEPTANCE_PASS|checks="+checks+"|scope=PRODUCTION_CLASS_HOST_ENGINEERING|device=false|future_effect=false");
    }
}
