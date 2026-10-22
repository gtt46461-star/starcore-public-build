package com.ethk3.novaonepro.r952;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class MoleculeGraphR952 {
    private final Map<String,WorldMoleculeR952> nodes=new HashMap<String,WorldMoleculeR952>();
    public MoleculeCompilerR952.Compiled compile(String id,Map<String,WorldAtomR952> atoms){return MoleculeCompilerR952.compile(nodes.get(id),atoms,nodes);}
    public void add(WorldMoleculeR952 m){if(m==null)throw new IllegalArgumentException("R952_GRAPH_NODE");if(nodes.containsKey(m.moleculeId))throw new IllegalArgumentException("R958_GRAPH_ID_IMMUTABLE");nodes.put(m.moleculeId,m);if(hasCycle(m.moleculeId,new HashSet<String>(),new HashSet<String>())){nodes.remove(m.moleculeId);throw new IllegalArgumentException("R952_MOLECULE_CYCLE");}}
    private boolean hasCycle(String id,Set<String> visiting,Set<String> done){if(done.contains(id))return false;if(!visiting.add(id))return true;WorldMoleculeR952 m=nodes.get(id);if(m!=null)for(String in:m.inputIds)if(nodes.containsKey(in)&&hasCycle(in,visiting,done))return true;visiting.remove(id);done.add(id);return false;}
}
