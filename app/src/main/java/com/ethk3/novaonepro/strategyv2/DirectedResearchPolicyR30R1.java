package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Exact counterfactual candidate. It must be measured before any prescription may materialize it. */
public final class DirectedResearchPolicyR30R1 {
    private DirectedResearchPolicyR30R1(){}
    /** Exact registered component intervention; the old subtree contract is unchanged. */
    public static PolicySpecR27 counterfactualR31(PolicySpecR27 original,String path,int generation){
        if(path!=null&&path.startsWith("root/"))return withoutChild(original,path,generation);
        PolicySpecR27 p=PolicySpecR27.parse(original.fullJson());
        PolicySpecR27.Meta meta=new PolicySpecR27.Meta(p.meta==null?"I09":p.meta.idea,"MEASURED_PRUNE",Collections.singletonList(p.policyHash()),generation,0,"COUNTERFACTUAL_PREPARED:"+path);
        PolicySpecR27 result;
        if("selector".equals(path)){
            if(p.rule==PolicySpecR27.Rule.ALWAYS)throw new IllegalArgumentException("SELECTOR_ALREADY_ALWAYS");
            if(SetIntersectionPolicyR30.exact(p))throw new IllegalArgumentException("HARD_INTERSECTION_SELECTOR_CONTRACT");
            result=p.withSelector(p.position,PolicySpecR27.Rule.ALWAYS,0,1,meta);
        }else if("readout".equals(path)){
            if(!p.fitTemperature&&p.temperature==1)throw new IllegalArgumentException("CALIBRATOR_ALREADY_IDENTITY");
            if(SetIntersectionPolicyR30.exact(p))throw new IllegalArgumentException("HARD_INTERSECTION_READOUT_CONTRACT");
            result=new PolicySpecR27(p.ast,p.position,p.rule,p.threshold,p.rate,1,false,p.lr,p.forget,meta);
        }else if(path!=null&&path.matches("family:[0-9]{1,2}")){
            int family=Integer.parseInt(path.substring(7));boolean found=false;
            for(PolicyAstR27.Node n:PolicyAstR27.nodes(p.ast))if(n.op.equals("family")&&n.ints[0]==family)found=true;
            if(!found)throw new IllegalArgumentException("FAMILY_COMPONENT_MISSING");
            result=p.withAst(maskFamilyR31(p.ast,family),meta);
        }else throw new IllegalArgumentException("COMPONENT_PATH");
        PolicyAstR27.type(result.ast);String bad=PolicyAstR27.rangeAndCausality(result.ast);if(bad!=null)throw new IllegalArgumentException(bad);
        if(result.policyHash().equals(p.policyHash()))throw new IllegalArgumentException("COMPONENT_NO_CHANGE");return result;
    }
    private static PolicyAstR27.Node maskFamilyR31(PolicyAstR27.Node node,int family){
        if(node.op.equals("family")&&node.ints[0]==family)return PolicyAstR27.constant(0);
        PolicyAstR27.Node[] kids=new PolicyAstR27.Node[node.kids.length];for(int i=0;i<kids.length;i++)kids[i]=maskFamilyR31(node.kids[i],family);
        return new PolicyAstR27.Node(node.op,node.ints,node.param,kids);
    }
    public static PolicySpecR27 withoutChild(PolicySpecR27 original,String path,int generation){
        if(path==null||!path.matches("root(?:/[0-9]+)+"))throw new IllegalArgumentException("PRUNING_PATH_REQUIRES_CHILD");
        PolicySpecR27 p=PolicySpecR27.parse(original.fullJson());String[] parts=path.split("/");
        PolicyAstR27.Node parent=p.ast;for(int i=1;i<parts.length-1;i++){int slot=Integer.parseInt(parts[i]);if(slot>=parent.kids.length)throw new IllegalArgumentException("PRUNING_PATH");parent=parent.kids[slot];}
        int slot=Integer.parseInt(parts[parts.length-1]);
        if(parent.kids.length!=2||slot>1||!(parent.op.equals("add")||parent.op.equals("mix")||parent.op.equals("sub")))
            throw new IllegalArgumentException("PRUNING_REQUIRES_DECLARED_ADDITIVE_COUNTERFACTUAL");
        PolicyAstR27.Node replacement=parent.kids[1-slot];
        if(parent.op.equals("sub")&&slot==0)replacement=PolicyAstR27.op("neg",replacement);
        List<PolicyAstR27.Node> nodes=PolicyAstR27.nodes(p.ast);int parentIndex=-1;
        for(int i=0;i<nodes.size();i++)if(nodes.get(i)==parent){parentIndex=i;break;}
        if(parentIndex<0)throw new IllegalStateException("PRUNING_PARENT_NOT_FOUND");
        PolicyAstR27.Node ast=PolicyAstR27.replace(p.ast,parentIndex,replacement);
        PolicyAstR27.type(ast);String bad=PolicyAstR27.rangeAndCausality(ast);if(bad!=null)throw new IllegalArgumentException(bad);
        PolicySpecR27 result=p.withAst(ast,new PolicySpecR27.Meta(p.meta==null?"I09":p.meta.idea,"MEASURED_PRUNE",Collections.singletonList(p.policyHash()),generation,0,"COUNTERFACTUAL_PREPARED:"+path));
        if(result.policyHash().equals(p.policyHash()))throw new IllegalArgumentException("PRUNING_NO_CHANGE");return result;
    }
    public static PolicySpecR27 materialize(ResearchAdviceR30R1 advice,ResearchContractR30R1 source,
            PolicySpecR27 original,String measuredCounterfactualJson,String measuredCounterfactualHash,int generation,long target,long preparedAt){
        source.requireAdvice(target,preparedAt);
        if(advice.action!=ResearchAdviceR30R1.Action.PRUNE_COMPONENT||!advice.parentPolicy.equals(original.policyHash())
                ||!advice.evidenceKey.equals(source.key("PAIRED_COMPONENT_ADVICE_V1|"+advice.analysisContractHash)))throw new IllegalStateException("PRUNING_EVIDENCE_IDENTITY");
        PolicySpecR27 child=PolicySpecR27.parse(measuredCounterfactualJson);
        PolicySpecR27 expected=counterfactualR31(original,advice.componentPath,generation);
        if(!child.policyHash().equals(ResearchContractR30R1.sha(measuredCounterfactualHash))||!child.policyHash().equals(expected.policyHash()))
            throw new IllegalStateException("UNMEASURED_COUNTERFACTUAL_SUBSTITUTION");
        return child.withMeta(new PolicySpecR27.Meta(child.meta==null?"I09":child.meta.idea,"MEASURED_PRUNE",Collections.singletonList(original.policyHash()),generation,0,"PROPOSAL:"+advice.proposalId+";"+advice.reason));
    }
}
