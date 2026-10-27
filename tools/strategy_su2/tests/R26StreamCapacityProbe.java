package com.ethk3.novaonepro.strategyv2;
import java.io.*;import java.util.*;
/** Synthetic capacity measurement only, not a quality or physical-device test. */
public final class R26StreamCapacityProbe {
 public static void main(String[] args)throws Exception {
  int members=Integer.parseInt(args[0]);if(members<4||members>48||members%4!=0)throw new IllegalArgumentException("members must be 4..48 in groups of four");List<String> roster=new ArrayList<>();
  for(int n=0;n<members/4;n++){List<ModelGenomeSU2.Member> group=new ArrayList<>();for(int j=0;j<4;j++){Map<String,String> p=new TreeMap<>(ModelEvolutionSU2.centralParams("MSDIR"));p.put("alpha",Double.toString(.1+(n*4+j)*.03));group.add(new ModelGenomeSU2.Member("MSDIR",p,1));}roster.add(new ModelGenomeSU2(group,"STATIC",false,"MASS",.3,.2,true).text());}
  LegacyReplayStateR26 state=new LegacyReplayStateR26(roster,1000);R26ChunkedCheckpointTest.Memory cas=new R26ChunkedCheckpointTest.Memory();Random random=new Random(26);long started=System.nanoTime();
  for(int t=0;t<2501;t++){int[] d={1,2,3,4,5,6,7,8,9,10};for(int p=9;p>0;p--){int j=random.nextInt(p+1),v=d[p];d[p]=d[j];d[j]=v;}state.advance(d);if(t==2499||t==2500){long before=System.nanoTime();byte[] encoded=ChunkedCheckpointR26.write(state::writeInflated,cas);long millis=(System.nanoTime()-before)/1000000;System.out.println("STREAM_CAPACITY|members="+members+"|samples="+(t+1)+"|rootBytes="+encoded.length+"|referencedParts="+ChunkedCheckpointR26.children(encoded).size()+"|storedPhysicalBytes="+cas.files.values().stream().mapToLong(x->x.length).sum()+"|encodeMs="+millis+"|totalMs="+(System.nanoTime()-started)/1000000);if(t==2500){LegacyReplayStateR26 restored=LegacyReplayStateR26.readInflated(ChunkedCheckpointR26.open(encoded,cas));state.advance(R26RecoveryTest.draw(7));restored.advance(R26RecoveryTest.draw(7));if(!Arrays.equals(ChunkedCheckpointR26.write(state::writeInflated,cas),ChunkedCheckpointR26.write(restored::writeInflated,cas)))throw new AssertionError("48-member next update changed after streamed restore");System.out.println("STREAM_CAPACITY_NEXT_UPDATE_PASS|members="+members);}}}
 }
}
