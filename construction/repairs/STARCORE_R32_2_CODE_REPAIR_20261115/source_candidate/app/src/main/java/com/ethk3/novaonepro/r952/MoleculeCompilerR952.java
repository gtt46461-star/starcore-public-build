package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.TypedAtomMoleculeGraphR930;
import com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.*;
import com.ethk3.novaonepro.V51ChecksumEngine;
import java.util.*;

/** Resolves the whole bounded DAG and delegates every operation to the existing typed VM. */
public final class MoleculeCompilerR952 {
    public static final int MAX_DEPTH=8,MAX_COST=100;
    private static final String NS="R958_WORLD",REV="R958_WORLD_DAG_V2";
    private MoleculeCompilerR952(){}

    /** Metadata alone is not an executable program. */
    public static Compiled compile(WorldMoleculeR952 m){
        return compile(m,Collections.<String,WorldAtomR952>emptyMap(),Collections.<String,WorldMoleculeR952>emptyMap());
    }

    public static Compiled compile(WorldMoleculeR952 root,Map<String,WorldAtomR952> atoms,
            Map<String,WorldMoleculeR952> molecules){
        if(root==null||atoms==null||molecules==null)throw new IllegalArgumentException("R952_MOLECULE_NULL");
        Map<String,WorldMoleculeR952> all=new HashMap<String,WorldMoleculeR952>(molecules);
        WorldMoleculeR952 previous=all.put(root.moleculeId,root);
        if(previous!=null&&!previous.canonicalStructure().equals(root.canonicalStructure()))
            throw new IllegalArgumentException("R958_MOLECULE_ROOT_DRIFT");
        List<NodeSpec> nodes=new ArrayList<NodeSpec>();
        Set<String> visiting=new HashSet<String>(),done=new HashSet<String>();
        Map<String,Integer> depths=new HashMap<String,Integer>();
        Map<String,Set<String>> closure=new HashMap<String,Set<String>>();
        resolve(root.moleculeId,atoms,all,nodes,visiting,done,depths,closure);
        Graph graph=new Graph("BINANCE1_PK10","FROZEN_PREDRAW",REV,nodes);
        Compiled compiled=compile(graph,NS+':'+root.moleculeId);
        int cost=cost(closure.get(root.moleculeId),atoms);
        return new Compiled(graph,compiled.outputId,compiled.depth,cost,compiled.semanticHash);
    }

    private static void resolve(String id,Map<String,WorldAtomR952> atoms,Map<String,WorldMoleculeR952> molecules,
            List<NodeSpec> nodes,Set<String> visiting,Set<String> done,Map<String,Integer> depths,
            Map<String,Set<String>> closure){
        if(done.contains(id))return;
        if(!visiting.add(id))throw new IllegalArgumentException("R952_MOLECULE_CYCLE");
        if(visiting.size()>MAX_DEPTH+1||done.size()+visiting.size()>MAX_COST)
            throw new IllegalArgumentException("R952_MOLECULE_COST_OR_DEPTH");
        WorldAtomR952 atom=atoms.get(id);WorldMoleculeR952 m=molecules.get(id);
        if(atom!=null&&m!=null)throw new IllegalArgumentException("R958_WORLD_ID_COLLISION");
        Set<String> lineage=new HashSet<String>();lineage.add(id);int depth=0;
        if(atom!=null){
            AtomOntologyR952.validate(atom);
            if(!id.equals(atom.atomId))throw new IllegalArgumentException("R958_ATOM_ID_DRIFT");
            ValueType type="FLOAT64".equals(atom.valueSchema)?ValueType.DOUBLE:ValueType.valueOf(atom.valueSchema);
            String identity=atom.type+'|'+atom.domain+'|'+atom.producer+'|'+atom.valueSchema+'|'+atom.pitContract+'|'+atom.lineageHash+'|'+atom.revision;
            nodes.add(new NodeSpec(NS,id,V51ChecksumEngine.sha256(identity),NodeKind.ATOM,type,new Shape(1,1),
                    Stage.FROZEN_PREDRAW,"1",0,0,MissingPolicy.REJECT,atom.producer,atom.pitContract,
                    0L,Collections.<String>emptyList(),null,new double[0],false,atom.revision));
        }else{
            if(m==null)throw new IllegalArgumentException("R958_MOLECULE_UNKNOWN_PARENT:"+id);
            if(!id.equals(m.moleculeId)||!m.structuralHash.equals(V51ChecksumEngine.sha256(m.canonicalStructure())))
                throw new IllegalArgumentException("R958_MOLECULE_HASH_DRIFT");
            List<String> parents=new ArrayList<String>();
            for(String parent:m.inputIds){
                resolve(parent,atoms,molecules,nodes,visiting,done,depths,closure);
                depth=Math.max(depth,depths.get(parent).intValue()+1);lineage.addAll(closure.get(parent));
                parents.add(NS+':'+parent);
            }
            if(m.depth!=depth||depth<1||depth>MAX_DEPTH)throw new IllegalArgumentException("R952_MOLECULE_DEPTH");
            if(m.cost<cost(lineage,atoms)||m.cost>MAX_COST)throw new IllegalArgumentException("R952_MOLECULE_COST");
            nodes.add(new NodeSpec(NS,id,m.semanticHash,NodeKind.MOLECULE,ValueType.DOUBLE,new Shape(1,1),
                    Stage.FROZEN_PREDRAW,"1",0,0,MissingPolicy.REJECT,"MoleculeCompilerR952","source_issue<target_issue",
                    0L,parents,MoleculeOperatorRegistryR952.resolve(m.operator),m.parameters(),false,REV));
        }
        visiting.remove(id);done.add(id);depths.put(id,Integer.valueOf(depth));closure.put(id,lineage);
    }

    private static int cost(Set<String> ids,Map<String,WorldAtomR952> atoms){
        int cost=0;for(String id:ids)cost+=atoms.containsKey(id)?AtomOntologyR952.cost(atoms.get(id)):1;
        if(cost>MAX_COST)throw new IllegalArgumentException("R952_MOLECULE_COST");return cost;
    }

    /** Used by the live Genesis molecular genome; retains its existing graph and serialization. */
    public static Compiled compile(Graph graph,String outputId){
        if(graph==null||graph.orderedNodes.size()>MAX_COST)throw new IllegalArgumentException("R952_MOLECULE_COST");
        NodeSpec root=graph.require(outputId);
        if(root.kind==NodeKind.ATOM)throw new IllegalArgumentException("R958_MOLECULE_OUTPUT_IS_ATOM");
        Set<String> used=new HashSet<String>();collect(graph,outputId,used);
        if(used.size()!=graph.orderedNodes.size())throw new IllegalArgumentException("R958_MOLECULE_UNUSED_NODE");
        Map<String,Integer> depths=new HashMap<String,Integer>();Map<String,String> semantics=new HashMap<String,String>();
        for(NodeSpec node:graph.orderedNodes){
            if(!node.stage.productionPredraw()||!node.availabilityContract.contains("source_issue<target_issue"))
                throw new IllegalArgumentException("R958_MOLECULE_PIT_CONTRACT");
            if(node.shape.elements()!=1||node.valueType==ValueType.CATEGORY)
                throw new IllegalArgumentException("R958_MOLECULE_SCALAR_TYPE");
            int depth=0;StringBuilder canonical=new StringBuilder(REV).append('|').append(node.kind).append('|')
                    .append(node.valueType).append('|').append(node.shape.canonical()).append('|').append(node.stage)
                    .append('|').append(node.unit).append('|').append(node.lag).append('|').append(node.window)
                    .append('|').append(node.missingPolicy).append('|').append(node.version);
            if(node.kind==NodeKind.ATOM)canonical.append('|').append(node.semantic).append('|').append(node.source)
                    .append('|').append(node.availabilityContract);
            else{
                canonical.append('|').append(node.operator);
                for(double parameter:node.parametersCopy())canonical.append('|').append(Double.toHexString(parameter));
                for(String parent:node.parents){depth=Math.max(depth,depths.get(parent).intValue()+1);canonical.append('|').append(semantics.get(parent));}
            }
            if(depth>MAX_DEPTH)throw new IllegalArgumentException("R952_MOLECULE_DEPTH");
            depths.put(node.qualifiedId(),Integer.valueOf(depth));semantics.put(node.qualifiedId(),V51ChecksumEngine.sha256(canonical.toString()));
        }
        return new Compiled(graph,outputId,depths.get(outputId).intValue(),used.size(),semantics.get(outputId));
    }

    private static void collect(Graph graph,String id,Set<String> used){
        if(used.add(id))for(String parent:graph.require(id).parents)collect(graph,parent,used);
    }

    public static final class Compiled {
        public final String structuralHash,semanticHash,outputId;
        public final int depth,cost;
        public final Graph graph;
        private Compiled(Graph g,String output,int d,int c,String semantic){graph=g;outputId=output;depth=d;cost=c;structuralHash=g.graphHash;semanticHash=semantic;}
        public Evaluation evaluate(FrozenInputs inputs){return TypedAtomMoleculeGraphR930.evaluate(graph,inputs);}
        public double scalar(double[] parents){
            NodeSpec root=graph.require(outputId);
            for(String parent:root.parents)if(graph.require(parent).kind!=NodeKind.ATOM)
                throw new IllegalArgumentException("R958_SCALAR_REQUIRES_DIRECT_ATOMS");
            return TypedAtomMoleculeGraphR930.scalarOperation(root,parents);
        }
    }
}
