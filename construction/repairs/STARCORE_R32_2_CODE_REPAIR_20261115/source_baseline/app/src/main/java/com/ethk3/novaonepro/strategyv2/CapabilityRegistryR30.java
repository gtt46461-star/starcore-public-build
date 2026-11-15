package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Actual handler registry, separate from runtime observations and statistical evidence. */
public final class CapabilityRegistryR30 {
    private CapabilityRegistryR30(){}
    private static final String PREFIX="com.ethk3.novaonepro.strategyv2.";
    public static final class Definition {
        public final String id,kind,label,implementationClass,entryClass,entryMethod,outputMode,blockedReason,matrixId;
        public final boolean implemented,executable;
        Definition(String id,String kind,String label,String impl,String entry,String method,String mode,boolean ready,String blocked,String matrix){
            this.id=id;this.kind=kind;this.label=label;implementationClass=impl;entryClass=entry;entryMethod=method;outputMode=mode;matrixId=matrix;
            implemented=type(impl)&&MatrixRegistryR30.declared(entry,method);executable=implemented&&ready;blockedReason=!implemented?"IMPLEMENTATION_OR_ENTRY_UNAVAILABLE":!ready?blocked:null;
        }
        public Map<String,Object> json(){Map<String,Object> r=new LinkedHashMap<>();r.put("id",id);r.put("kind",kind);r.put("label",label);r.put("implementationClass",implementationClass);r.put("entryClass",entryClass);r.put("entryMethod",entryMethod);r.put("outputMode",outputMode);if(matrixId!=null)r.put("matrixRegistryId",matrixId);r.put("implemented",implemented);r.put("executable",executable);r.put("executionBlockedReason",blockedReason);return r;}
    }
    static boolean type(String name){try{Class.forName(name,false,CapabilityRegistryR30.class.getClassLoader());return true;}catch(ClassNotFoundException|LinkageError absent){return false;}}
    public static List<Definition> all(){
        List<Definition> out=new ArrayList<>();
        for(int i=1;i<=24;i++){
            String id=String.format(java.util.Locale.ROOT,"F%02d",i),binding=FamilyRegistryR26.implementation(id),simple=binding.isEmpty()?"DistillationFamilyR26":binding.split(":",2)[0];
            if(simple.equals("OnlineModelsSU2.Bocpd"))simple="OnlineModelsSU2$Bocpd";
            boolean ready=FamilyRegistryR26.EXECUTABLE.contains(id)&&(!id.equals("F23")||!binding.isEmpty()&&ModelReplaySU2.ADAPTERS.containsKey("R26F23"));
            out.add(new Definition(id,"FAMILY",id,PREFIX+simple,PREFIX+"FamilyRegistryR26","build","LEARNING_MODEL",ready,id.equals("F23")?"F23_ORIGINAL_TEACHER_NOT_BOUND":"FAMILY_NOT_REGISTERED",null));
        }
        for(String[] e:ExpertProposalsR27.EXPERTS){boolean audit=Arrays.asList("E01","E14","E15","E16").contains(e[0]);out.add(new Definition(e[0],"EXPERT",e[1],PREFIX+"ExpertProposalsR27",PREFIX+"ExpertProposalsR27","propose",audit?"AUDIT_RECEIPT":"PROPOSAL_OR_DEPENDENCY_AUDIT",true,null,null));}
        for(IdeaFamilyR27 f:EvolutionLoopR27.FAMILIES)out.add(new Definition(f.id(),"IDEA",f.nameZh(),f.getClass().getName(),PREFIX+"IdeaFamilyR27","propose",f.id().equals("I00")?"CONTROL_ONLY_POLICY":"FULL_POLICY_PROPOSAL",true,null,null));
        out.add(new Definition("ATOMNET","MODEL","AtomNet十位置",PREFIX+"AtomNetR27",PREFIX+"AtomNetRuntimeR30","step","TEN_POSITION_LEARNING_AND_NUMBER_SURFACE",true,null,null));
        for(MatrixRegistryR30.Definition d:MatrixRegistryR30.all())out.add(new Definition("MATRIX:"+d.id,"MATRIX",d.id,d.producerClass,d.producerClass,d.producerMethod,d.outputScope,true,null,d.id));
        if(out.size()!=82)throw new IllegalStateException("R30_CAPABILITY_HANDLER_COUNT:"+out.size());
        Set<String> ids=new HashSet<>();for(Definition d:out)if(!ids.add(d.id))throw new IllegalStateException("R30_DUPLICATE_CAPABILITY:"+d.id);
        return Collections.unmodifiableList(out);
    }
}
