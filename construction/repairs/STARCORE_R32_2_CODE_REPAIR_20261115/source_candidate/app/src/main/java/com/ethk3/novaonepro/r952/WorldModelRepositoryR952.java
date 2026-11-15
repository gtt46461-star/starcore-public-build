package com.ethk3.novaonepro.r952;

import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.V51ChecksumEngine;
import com.ethk3.novaonepro.TypedAtomMoleculeGraphR930;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.*;

public final class WorldModelRepositoryR952 {
    private WorldModelRepositoryR952(){}
    public static void putAtom(final DbStore store,final WorldAtomR952 atom){
        AtomOntologyR952.validate(atom);
        if(store==null||!store.coordinatorForResearchR952().runAtomicWrite("R952_WORLD_ATOM",new DatabaseCoordinator.WriteRunnable(){
            public void run(SQLiteDatabase db){ResearchMigrationRegistryR952.ensure(db);putAtom(db,atom);}
        }))throw new IllegalStateException("R958_WORLD_ATOM_COMMIT_FAILED");
    }
    public static void putMolecule(final DbStore store,final WorldMoleculeR952 m){
        if(store==null||!store.coordinatorForResearchR952().runAtomicWrite("R952_WORLD_MOLECULE",new DatabaseCoordinator.WriteRunnable(){
            public void run(SQLiteDatabase db){ResearchMigrationRegistryR952.ensure(db);putMolecule(db,m);}
        }))throw new IllegalStateException("R958_WORLD_MOLECULE_COMMIT_FAILED");
    }
    /** Called inside the existing research transaction; never opens a second writer. */
    public static void putAtom(SQLiteDatabase db,WorldAtomR952 atom){
        transaction(db);AtomOntologyR952.validate(atom);WorldAtomR952 previous=atom(db,atom.atomId);
        if(previous!=null){if(!previous.atomHash.equals(atom.atomHash))throw new IllegalStateException("R958_WORLD_ATOM_ID_REUSED");return;}
        db.execSQL("INSERT INTO r952_world_atom(atom_id,type,domain,producer,value_schema,pit_contract,lineage_hash,cost_class,revision,atom_hash) VALUES(?,?,?,?,?,?,?,?,?,?)",
                new Object[]{atom.atomId,atom.type,atom.domain,atom.producer,atom.valueSchema,atom.pitContract,atom.lineageHash,atom.costClass,atom.revision,atom.atomHash});
    }
    public static MoleculeCompilerR952.Compiled putMolecule(SQLiteDatabase db,WorldMoleculeR952 m){
        transaction(db);Map<String,WorldAtomR952> atoms=new HashMap<String,WorldAtomR952>();
        Map<String,WorldMoleculeR952> molecules=new HashMap<String,WorldMoleculeR952>();molecules.put(m.moleculeId,m);
        Set<String> seen=new HashSet<String>();seen.add(m.moleculeId);
        for(String parent:m.inputIds)load(db,parent,atoms,molecules,seen,1);
        MoleculeCompilerR952.Compiled compiled=MoleculeCompilerR952.compile(m,atoms,molecules);
        try(Cursor c=db.rawQuery("SELECT compiled_hash,resolved_semantic_hash FROM r952_world_molecule WHERE molecule_id=?",new String[]{m.moleculeId})){
            if(c.moveToFirst()){
                if(!compiled.structuralHash.equals(c.getString(0))||!compiled.semanticHash.equals(c.getString(1)))
                    throw new IllegalStateException("R958_WORLD_MOLECULE_ID_REUSED");
                require(db,m.moleculeId);return compiled;
            }
        }
        db.execSQL("INSERT INTO r952_world_molecule(molecule_id,operator,input_ids,structural_hash,semantic_hash,depth,cost,parameters,compiled_hash,resolved_semantic_hash,canonical_version) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                new Object[]{m.moleculeId,m.operator,WorldMoleculeR952.join(m.inputIds),m.structuralHash,m.semanticHash,m.depth,m.cost,m.parameterEncoding(),compiled.structuralHash,compiled.semanticHash,"WORLD_DAG_V2"});
        return require(db,m.moleculeId);
    }
    /** Rebuilds the executable from immutable persisted descriptors and verifies every hash. */
    public static MoleculeCompilerR952.Compiled require(SQLiteDatabase db,String id){
        Map<String,WorldAtomR952> atoms=new HashMap<String,WorldAtomR952>();Map<String,WorldMoleculeR952> molecules=new HashMap<String,WorldMoleculeR952>();
        load(db,id,atoms,molecules,new HashSet<String>(),0);
        MoleculeCompilerR952.Compiled result=MoleculeCompilerR952.compile(molecules.get(id),atoms,molecules);
        try(Cursor c=db.rawQuery("SELECT compiled_hash,resolved_semantic_hash FROM r952_world_molecule WHERE molecule_id=?",new String[]{id})){
            if(!c.moveToFirst()||!result.structuralHash.equals(c.getString(0))||!result.semanticHash.equals(c.getString(1)))
                throw new IllegalStateException("R958_WORLD_COMPILED_HASH_DRIFT");
        }
        return result;
    }
    private static void load(SQLiteDatabase db,String id,Map<String,WorldAtomR952> atoms,Map<String,WorldMoleculeR952> molecules,Set<String> seen,int depth){
        if(seen.contains(id))return;
        if(depth>MoleculeCompilerR952.MAX_DEPTH||seen.size()>=MoleculeCompilerR952.MAX_COST)
            throw new IllegalArgumentException("R958_WORLD_RELOAD_BOUNDS");
        seen.add(id);WorldAtomR952 a=atom(db,id);if(a!=null){atoms.put(id,a);return;}
        WorldMoleculeR952 m;
        try(Cursor c=db.rawQuery("SELECT operator,input_ids,structural_hash,semantic_hash,depth,cost,parameters,canonical_version FROM r952_world_molecule WHERE molecule_id=?",new String[]{id})){
            if(!c.moveToFirst())throw new IllegalStateException("R958_WORLD_PARENT_NOT_REGISTERED:"+id);
            if(!"WORLD_DAG_V2".equals(c.getString(7)))throw new IllegalStateException("R958_WORLD_LEGACY_DESCRIPTOR_NOT_EXECUTABLE");
            String encoded=c.getString(6);String[] parts=encoded.length()==0?new String[0]:encoded.split(",",-1);double[] parameters=new double[parts.length];
            for(int i=0;i<parts.length;i++)parameters[i]=Double.parseDouble(parts[i]);
            m=new WorldMoleculeR952(id,c.getString(0),Arrays.asList(c.getString(1).split(",",-1)),c.getInt(4),c.getInt(5),c.getString(3),parameters);
            if(!m.structuralHash.equals(c.getString(2))||!encoded.equals(m.parameterEncoding()))throw new IllegalStateException("R958_WORLD_STRUCTURE_HASH_DRIFT");
        }
        molecules.put(id,m);for(String parent:m.inputIds)load(db,parent,atoms,molecules,seen,depth+1);
    }
    private static WorldAtomR952 atom(SQLiteDatabase db,String id){
        try(Cursor c=db.rawQuery("SELECT type,domain,producer,value_schema,pit_contract,lineage_hash,cost_class,revision,atom_hash FROM r952_world_atom WHERE atom_id=?",new String[]{id})){
            if(!c.moveToFirst())return null;
            WorldAtomR952 a=new WorldAtomR952(id,c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7));
            AtomOntologyR952.validate(a);if(!a.atomHash.equals(c.getString(8)))throw new IllegalStateException("R958_WORLD_ATOM_HASH_DRIFT");return a;
        }
    }
    public static void recordAdmission(SQLiteDatabase db,WorldAtomR952 atom,String intentId,TypedAtomMoleculeGraphR930.Admission admission,String issue){
        transaction(db);
        TypedAtomMoleculeGraphR930.Admission verified=TypedAtomMoleculeGraphR930.decodeAdmission(admission.encode());
        if(!AtomDiscoveryEngineR952.propose(atom.atomId,verified).atomHash.equals(atom.atomHash))
            throw new IllegalArgumentException("R958_ADMISSION_ATOM_MISMATCH");
        try(Cursor c=db.rawQuery("SELECT intent_hash FROM r952_intent WHERE intent_id=?",new String[]{intentId})){
            if(!c.moveToFirst())throw new IllegalStateException("R958_ATOM_DISCOVERY_INTENT_REQUIRED");
            String payload=new JSONObject().put("version","R958_WORLD_ADMISSION_V2").put("atom_hash",atom.atomHash)
                .put("intent_id",intentId).put("intent_hash",c.getString(0)).put("admission",verified.encode()).toString();
            String hash=KnowledgeLedgerR952.put(db,issue,atom.atomId,"ATOM_ADMISSION",payload);
            if(!payload.equals(KnowledgeLedgerR952.require(db,hash)))throw new IllegalStateException("R958_ADMISSION_RELOAD_DRIFT");
        }catch(JSONException e){throw new IllegalArgumentException("R958_WORLD_ADMISSION_ENCODING",e);}
    }
    private static void transaction(SQLiteDatabase db){if(db==null||!db.inTransaction())throw new IllegalStateException("R958_WORLD_TRANSACTION_REQUIRED");}
}
