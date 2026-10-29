package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Explicit kernel prior for a new private policy. Old raw context, fitted gate,
 * calibration samples and qualification never become the new policy's evidence.
 * The next source starts after all labels exposed by the parent's birth and training. */
public final class WarmModelSeedR26 {
    private static final int MAGIC=0x26aa0001,MAX_RAW=32<<20,MAX_PACKED=4<<20;
    private static final String[] FIELDS={"_warm_parent","_warm_checkpoint","_warm_parent_state","_warm_kernel","_warm_through","_warm_member","_warm_exposed"};
    private WarmModelSeedR26(){}
    public static boolean has(Map<String,String> parameters){for(String field:FIELDS)if(parameters.containsKey(field))return true;return false;}
    public static boolean has(ModelGenomeSU2 genome){for(ModelGenomeSU2.Member m:genome.members)if(has(m.params))return true;return false;}
    private static void hex(String value){if(value==null||!value.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R26_WARM_HASH");}
    public static Map<String,String> structuralParameters(Map<String,String> parameters){
        Map<String,String> raw=new TreeMap<>(parameters);
        if(has(raw)){
            for(int i=0;i<4;i++)hex(raw.get(FIELDS[i]));
            String through=raw.get(FIELDS[4]),member=raw.get(FIELDS[5]);
            if(through==null||!through.matches("[0-9]+")||member==null||!member.matches("[0-3]"))throw new IllegalArgumentException("R26_WARM_BINDING");
            try{if(Long.parseLong(through)<0)throw new IllegalArgumentException("R26_WARM_THROUGH");}catch(NumberFormatException invalid){throw new IllegalArgumentException("R26_WARM_THROUGH",invalid);}
            String exposed=raw.get("_warm_exposed");if(exposed==null||!exposed.matches("[0-9]+")||Long.parseLong(exposed)<Long.parseLong(through))throw new IllegalArgumentException("R26_WARM_EXPOSURE");
            for(String field:FIELDS)raw.remove(field);
        }
        return raw;
    }
    private static Map<String,String> numericalParameters(ModelGenomeSU2.Member member){
        Map<String,String> raw=structuralParameters(member.params);raw.remove("_birth_snapshot");raw.remove("_birth_through");raw.remove("_legal_head");return raw;
    }
    private static String specification(ModelGenomeSU2.Member member){return PolicyIdentityR26.sha256(PolicyIdentityR26.canonical(Arrays.asList(member.kind,numericalParameters(member))));}
    private static void validate(ModelGenomeSU2 genome){
        if(!has(genome))throw new IllegalArgumentException("R26_WARM_INITIALIZER_REQUIRED");
        Map<String,String> first=genome.members.get(0).params;
        for(int i=0;i<genome.members.size();i++){
            Map<String,String> p=genome.members.get(i).params;structuralParameters(p);
            for(int field=0;field<5;field++)if(!Objects.equals(first.get(FIELDS[field]),p.get(FIELDS[field])))throw new IllegalArgumentException("R26_WARM_PARENT_BINDING");
            if(!Objects.equals(first.get("_warm_exposed"),p.get("_warm_exposed")))throw new IllegalArgumentException("R26_WARM_EXPOSURE_BINDING");
            if(!Integer.toString(i).equals(p.get("_warm_member")))throw new IllegalArgumentException("R26_WARM_MEMBER_BINDING");
            hex(p.get("_birth_snapshot"));String through=p.get("_birth_through");
            if(through==null||!through.matches("[0-9]+")||Long.parseLong(through)<=Long.parseLong(p.get("_warm_exposed")))throw new IllegalArgumentException("R26_WARM_SOURCE_ORDER");
        }
    }
    public static Object identity(ModelGenomeSU2 genome){
        validate(genome);Map<String,String> p=genome.members.get(0).params;
        return Arrays.asList("FROZEN_PARENT_KERNELS_CONTEXT_RESET_THEN_NEW_CAUSAL_BOOTSTRAP_NO_META_OR_EVIDENCE_INHERITANCE",p.get("_warm_parent"),p.get("_warm_checkpoint"),p.get("_warm_parent_state"),p.get("_warm_kernel"),p.get("_warm_through"),p.get("_warm_exposed"),p.get("_birth_snapshot"),p.get("_birth_through"));
    }
    private static final class BoundedBytes extends ByteArrayOutputStream {
        final int maximum;BoundedBytes(int maximum){this.maximum=maximum;}
        private void reserve(int n){if(n<0||count>maximum-n)throw new IllegalArgumentException("R26_WARM_STATE_CAPACITY");}
        @Override public synchronized void write(int value){reserve(1);super.write(value);}
        @Override public synchronized void write(byte[] bytes,int offset,int length){reserve(length);super.write(bytes,offset,length);}
    }
    public static final class Seed {
        public final ModelGenomeSU2 genome;public final String initializerHash;
        public final long parentSamples,parentExposedThrough;
        private final byte[] initializer,state;
        private Seed(ModelGenomeSU2 genome,byte[] initializer,byte[] state,long parentSamples,long parentExposedThrough){this.genome=genome;this.initializer=initializer.clone();this.state=state.clone();this.parentSamples=parentSamples;this.parentExposedThrough=parentExposedThrough;initializerHash=LiveStateR26.hash(initializer);}
        public byte[] initializer(){return initializer.clone();}
        public byte[] state(){return state.clone();}
    }
    public static Seed prepare(String parentCanonical,byte[] parentBytes,String parentStateHash,String parentCheckpoint,
                               long parentThrough,String snapshot,long sourceFirst,long sourceThrough)throws IOException{
        hex(parentCheckpoint);hex(parentStateHash);hex(snapshot);
        if(parentThrough<0||sourceFirst<=parentThrough||sourceThrough<sourceFirst)throw new IllegalArgumentException("R26_WARM_SOURCE_NOT_STRICTLY_AFTER_PARENT");
        LiveStateR26 parent=LiveStateR26.decode(parentCanonical,parentBytes,parentStateHash);
        if(parent.samples()<1&&!has(parent.genome))throw new IOException("R26_WARM_PARENT_UNTRAINED");
        long exposed=parentThrough;for(ModelGenomeSU2.Member member:parent.genome.members){if(member.params.containsKey("_birth_through"))exposed=Math.max(exposed,Long.parseLong(member.params.get("_birth_through")));if(member.params.containsKey("_warm_exposed"))exposed=Math.max(exposed,Long.parseLong(member.params.get("_warm_exposed")));}
        if(sourceFirst<=exposed)throw new IllegalArgumentException("R26_WARM_SOURCE_NOT_AFTER_PARENT_EXPOSURE");
        parent.onGap(); // Never retain an expired raw ring as a hidden second source calendar.
        BoundedBytes packed=new BoundedBytes(MAX_PACKED);
        try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new CooperativeOutputStreamR26(new GZIPOutputStream(packed)),32768))){
            out.writeInt(MAGIC);out.writeInt(parent.genome.members.size());
            for(ModelGenomeSU2.Member member:parent.genome.members){
                ExecutionBudgetR26.poll();BoundedBytes raw=new BoundedBytes(MAX_RAW);
                try(DataOutputStream kernel=new DataOutputStream(new BufferedOutputStream(new CooperativeOutputStreamR26(raw),32768))){parent.hub.models.get(member.modelKey()).write(kernel);}
                byte[] body=raw.toByteArray();out.writeUTF(member.kind);out.writeUTF(specification(member));out.writeInt(body.length);out.write(body);
            }
        }
        byte[] initializer=packed.toByteArray();String initializerHash=LiveStateR26.hash(initializer);
        List<ModelGenomeSU2.Member> members=new ArrayList<>();int index=0;
        for(ModelGenomeSU2.Member member:parent.genome.members){
            Map<String,String> p=numericalParameters(member);p.put("_warm_parent",parent.genome.key());p.put("_warm_checkpoint",parentCheckpoint);p.put("_warm_parent_state",parentStateHash);p.put("_warm_kernel",initializerHash);p.put("_warm_through",Long.toString(parentThrough));p.put("_warm_exposed",Long.toString(exposed));p.put("_warm_member",Integer.toString(index++));members.add(new ModelGenomeSU2.Member(member.kind,p,member.weight));
        }
        ModelGenomeSU2 g=parent.genome;ModelGenomeSU2 child=new ModelGenomeSU2(members,g.pool,g.sink,g.gate,g.coverage,g.metaC,g.calibrated).bindBirth(snapshot,sourceThrough);
        if(child.text().length()>8192)throw new IOException("R26_WARM_POLICY_CAPACITY");
        LiveStateR26 seeded=restore(child,initializer);return new Seed(child,initializer,seeded.encode(),parent.samples(),exposed);
    }
    public static LiveStateR26 restore(ModelGenomeSU2 genome,byte[] initializer)throws IOException{
        validate(genome);if(initializer==null||initializer.length>MAX_PACKED||!LiveStateR26.hash(initializer).equals(genome.members.get(0).params.get("_warm_kernel")))throw new IOException("R26_WARM_INITIALIZER_HASH");
        LiveStateR26 child=LiveStateR26.blankForVerifiedRestore(genome);
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new CooperativeInputStreamR26(new GZIPInputStream(new ByteArrayInputStream(initializer))),32768))){
            if(in.readInt()!=MAGIC||in.readInt()!=genome.members.size())throw new IOException("R26_WARM_INITIALIZER_SHAPE");
            for(ModelGenomeSU2.Member member:genome.members){
                if(!member.kind.equals(in.readUTF())||!specification(member).equals(in.readUTF()))throw new IOException("R26_WARM_INITIALIZER_MEMBER");
                int n=in.readInt();if(n<1||n>MAX_RAW)throw new IOException("R26_WARM_KERNEL_CAPACITY");byte[] raw=new byte[n];in.readFully(raw);
                try(DataInputStream kernel=new DataInputStream(new CooperativeInputStreamR26(new ByteArrayInputStream(raw)))){child.hub.models.get(member.modelKey()).read(kernel);if(kernel.read()!=-1)throw new IOException("R26_WARM_KERNEL_TRAILING");}
            }
            if(in.read()!=-1)throw new IOException("R26_WARM_INITIALIZER_TRAILING");
        }
        if(child.samples()!=0||child.live.t!=0)throw new IOException("R26_WARM_INHERITED_META_CLOCK");return child;
    }
}
