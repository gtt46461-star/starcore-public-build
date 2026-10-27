package com.ethk3.novaonepro.strategyv2;
import java.io.*;import java.security.*;import java.util.*;
/** Independent byte store, shifted-history deduplication, corruption and full learner continuation. */
public final class R26ChunkedCheckpointTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static final class Memory implements ChunkedCheckpointR26.Store {
        final Map<String,byte[]> files=new HashMap<>();
        String hash(byte[] bytes){try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder s=new StringBuilder();for(byte b:digest)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}catch(Exception e){throw new AssertionError(e);}}
        public String put(byte[] bytes){String hash=hash(bytes);files.put(hash,bytes.clone());return hash;}
        public InputStream open(String hash)throws IOException{byte[] bytes=files.get(hash);if(bytes==null||!hash(bytes).equals(hash))throw new IOException("INDEPENDENT_STORE_DIGEST");return new ByteArrayInputStream(bytes);}
    }
    static byte[] read(byte[] manifest,Memory store)throws Exception{try(InputStream in=ChunkedCheckpointR26.open(manifest,store);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[7777];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return out.toByteArray();}}
    public static void main(String[] args)throws Exception {
        Memory store=new Memory();byte[] data=new byte[4<<20];new Random(26).nextBytes(data);byte[] first=ChunkedCheckpointR26.write(out->out.write(data),store);check(Arrays.equals(data,read(first,store)),"full stream did not round trip");
        byte[] partitioned=ChunkedCheckpointR26.write(out->{for(int i=0;i<data.length;i+=61)out.write(data,i,Math.min(61,data.length-i));},store);check(Arrays.equals(first,partitioned),"serialization call boundaries changed checkpoint");
        byte[] shifted=new byte[data.length];System.arraycopy(data,410,shifted,0,data.length-410);byte[] tail=new byte[410];new Random(27).nextBytes(tail);System.arraycopy(tail,0,shifted,data.length-410,410);
        byte[] second=ChunkedCheckpointR26.write(out->out.write(shifted),store);Set<String> old=ChunkedCheckpointR26.children(first),next=ChunkedCheckpointR26.children(second),shared=new HashSet<>(old);shared.retainAll(next);check(shared.size()>old.size()*.7,"shifted raw ring did not reuse most immutable chunks: "+shared.size()+"/"+old.size());check(Arrays.equals(shifted,read(second,store)),"shifted state changed");
        byte[] corrupt=second.clone();corrupt[12]^=1;try{read(corrupt,store);throw new AssertionError("wrong raw digest accepted");}catch(IOException expected){}
        corrupt=second.clone();corrupt[44]=127;try{ChunkedCheckpointR26.children(corrupt);throw new AssertionError("unbounded manifest accepted");}catch(IOException expected){}
        String victim=next.iterator().next();byte[] good=store.files.get(victim);store.files.put(victim,new byte[]{1});try{read(second,store);throw new AssertionError("missing/corrupt child accepted");}catch(IOException expected){}store.files.put(victim,good);
        ModelGenomeSU2 g=new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member("MSDIR",ModelEvolutionSU2.centralParams("MSDIR"),1)),"STATIC",false,"MASS",.3,.2,true);LegacyReplayStateR26 a=new LegacyReplayStateR26(Collections.singletonList(g.text()),1000);for(int t=0;t<60;t++)a.advance(R26RecoveryTest.draw(t));
        byte[] root=ChunkedCheckpointR26.write(a::writeInflated,store);LegacyReplayStateR26 b=LegacyReplayStateR26.readInflated(ChunkedCheckpointR26.open(root,store));check(Arrays.equals(a.encode(),b.encode()),"complete learner state not restored");a.advance(R26RecoveryTest.draw(61));b.advance(R26RecoveryTest.draw(61));check(Arrays.equals(a.encode(),b.encode()),"next learned update changed after chunk recovery");
        System.out.println("R26_CHUNKED_CHECKPOINT_PASS parts="+old.size()+" reused="+shared.size()+" storedBytes="+store.files.values().stream().mapToLong(x->x.length).sum());
    }
}
