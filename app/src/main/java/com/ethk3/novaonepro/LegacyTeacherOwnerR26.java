package com.ethk3.novaonepro;
import android.content.Context;
import com.ethk3.novaonepro.strategyv2.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Loads the preserved public R908 program/state, executes the original feature/VM path.
 * Its projected preference distribution is a teacher target, never recovered natural qualification. */
public final class LegacyTeacherOwnerR26 {
    private static final String ASSET="research/r967_history/R908_125_algorithms.tsv";
    private static volatile LegacyTeacherOwnerR26 instance;private static final ThreadLocal<Frame> FRAME=new ThreadLocal<>();
    private final AlgorithmGenomeR908 genome;private final String stateText,trained,identity;private final Map<String,String> binding;
    private static final class Frame {final long[] issues,open,available;final int[][] draws;final int cursor;double[][] projection;Frame(long[] i,long[] o,long[] a,int[][] d,int c){issues=i;open=o;available=a;draws=d;cursor=c;}}
    private LegacyTeacherOwnerR26(Context context)throws IOException{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=context.getAssets().open(ASSET)){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){bytes.write(buffer,0,n);if(bytes.size()>4<<20)throw new IOException("F23_ASSET_BOUND");}}
        String text=new String(bytes.toByteArray(),StandardCharsets.UTF_8);String[] lines=text.split("\n");String[] header=lines[0].trim().split("\t",-1);Map<String,Integer> index=new HashMap<>();for(int i=0;i<header.length;i++)index.put(header[i],i);String[] selected=null;double best=-1;int count=0;
        for(int line=1;line<lines.length;line++){if(lines[line].isEmpty())continue;String[] row=lines[line].split("\t",-1);if(row.length!=header.length)throw new IOException("F23_ARCHIVE_COLUMNS");count++;double rate=Double.parseDouble(row[index.get("dev_hit_rate")]);if(selected==null||rate>best||(rate==best&&row[0].compareTo(selected[0])<0)){selected=row;best=rate;}}
        if(count!=125||selected==null)throw new IOException("F23_ARCHIVE_DENOMINATOR");genome=AlgorithmGenomeR908.parse(selected[index.get("genome_spec")]);stateText=selected[index.get("state_blob")];AlgorithmGenomeVmR908.State state=AlgorithmGenomeVmR908.State.decodeStrict(stateText);if(!state.encode().equals(stateText)||!genome.structuralHash.equals(selected[index.get("structural_hash")]))throw new IOException("F23_ORIGINAL_IDENTITY");trained=selected[index.get("trained_until_issue")];if(!TargetIssueResolver.isValid(trained))throw new IOException("F23_TRAINING_CUTOFF");
        Map<String,String> b=new TreeMap<>();b.put("abi","1");b.put("asset",PolicyIdentityR26.sha256(text));b.put("teacher",genome.structuralHash);b.put("state",PolicyIdentityR26.sha256(stateText));b.put("scope","LEGACY_REPORTED_PREFERENCE_PROJECTION_V1");binding=Collections.unmodifiableMap(b);identity=PolicyIdentityR26.sha256(PolicyIdentityR26.canonical(binding));
    }
    public static synchronized void register(Context context){try{if(instance==null)instance=new LegacyTeacherOwnerR26(context);LegacyTeacherOwnerR26 owner=instance;FamilyRegistryR26.bindLegacyTeacher(owner.binding,p->{if(!p.equals(owner.binding))throw new IllegalArgumentException("F23_SOURCE_BINDING");return new DistillationFamilyR26(new DistillationFamilyR26.Teacher(){public double[][] marginal(){return owner.projection();}public String identity(){return owner.identity;}});});}catch(IOException e){throw new IllegalStateException("F23_ORIGINAL_ARCHIVE_BLOCKED",e);}}
    public static void enter(long[] issues,long[] open,long[] available,int[][] draws,int cursor){if(cursor<0||cursor>=draws.length||issues.length!=draws.length||open.length!=draws.length||available.length!=draws.length)throw new IllegalArgumentException("F23_FRAME");FRAME.set(new Frame(issues,open,available,draws,cursor));}
    public static void leave(){FRAME.remove();}
    private double[][] projection(){Frame f=FRAME.get();if(f==null)throw new IllegalStateException("F23_FROZEN_INPUT_REQUIRED");if(f.projection!=null)return f.projection;if(f.cursor<24)return null;
        if(f.issues[f.cursor]<=0||trained.compareTo(String.valueOf(f.issues[f.cursor]))>=0)return null;
        ArrayList<Models.Draw> history=new ArrayList<>();long previous=0;for(int i=Math.max(0,f.cursor-480);i<f.cursor;i++){if(f.issues[i]<=previous||f.issues[i]>=f.issues[f.cursor]||f.available[i]<=0||(previous>0&&TargetIssueResolver.sequenceOrdinalSU1(String.valueOf(f.issues[i]))!=TargetIssueResolver.sequenceOrdinalSU1(String.valueOf(previous))+1))return null;Models.Draw d=new Models.Draw();d.issue=String.valueOf(f.issues[i]);d.openTime=f.open[i];d.fetchTime=f.available[i];d.numbers=f.draws[i].clone();if(!d.isValid()||!TargetIssueResolver.isValid(d.issue))throw new IllegalArgumentException("F23_INVALID_SOURCE");history.add(d);previous=f.issues[i];}
        HistoricalStrategyInventionR908.Profile profile=HistoricalStrategyInventionR908.Profile.from(history);if(!profile.ready)return null;AlgorithmGenomeVmR908.State state=AlgorithmGenomeVmR908.State.decodeStrict(stateText);String before=state.encode();AlgorithmGenomeVmR908.Scorer scorer=new AlgorithmGenomeVmR908.Scorer(genome,state);double[][] score=new double[10][10];
        for(int pos=0;pos<10;pos++){ExecutionBudgetR26.poll();double[][] features=new double[10][];for(int n=0;n<10;n++)features[n]=FrozenConsumerRecipeR932.inputs(profile,pos+1,n+1);double z=0;for(int a=0;a<8;a++)for(int b=a+1;b<9;b++)for(int c=b+1;c<10;c++){double[] x=new double[AlgorithmGenomeR908.INPUTS];for(int j=0;j<x.length;j++)x[j]=(features[a][j]+features[b][j]+features[c][j])/3;double preference=Math.exp(scorer.score(pos+1,x)/.2);z+=preference;score[pos][a]+=preference/3;score[pos][b]+=preference/3;score[pos][c]+=preference/3;}for(int n=0;n<10;n++)score[pos][n]=Math.log(Math.max(1e-12,score[pos][n]/z));}
        if(!before.equals(state.encode()))throw new IllegalStateException("F23_ORIGINAL_STATE_MUTATED");f.projection=ExactPermutationR26.infer(score,ExecutionBudgetR26.current()).marginal;return f.projection;
    }
}
