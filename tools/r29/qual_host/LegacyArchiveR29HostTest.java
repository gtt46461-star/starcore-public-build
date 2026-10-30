package com.ethk3.novaonepro;

import com.ethk3.novaonepro.strategyv2.JsonR27;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Runs every original archived program/state through the original feature extractor and VM.
 * Fixed legal synthetic history, full 1200-action scans, exact bit comparison and restart after every two
 * periods. This is serialization continuity under current original kernels, NOT a captured old-device
 * golden trace, prospective quality, an Android measurement, or an authorization to transfer certificates. */
public final class LegacyArchiveR29HostTest {
    static final int HISTORY=96,PERIODS=8,ACTIONS=1200;
    static final class Step { int ordinal,position,n1,n2,n3,hit;double score,margin;double[] update;String surface; }
    static void require(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    static String digest(byte[] bytes)throws Exception{byte[] hash=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder b=new StringBuilder();for(byte x:hash)b.append(String.format(Locale.ROOT,"%02x",x&255));return b.toString();}
    static String digest(String s)throws Exception{return digest(s.getBytes(StandardCharsets.UTF_8));}
    static List<Models.Draw> history(){
        List<Models.Draw> out=new ArrayList<>();Random rng=new Random(29007L);long first=TargetIssueResolver.sequenceOrdinalSU1("202610300001");
        for(int t=0;t<HISTORY+PERIODS;t++){
            Models.Draw d=new Models.Draw();d.issue=TargetIssueResolver.fromSequenceOrdinalSU1(first+t);d.numbers=new int[]{1,2,3,4,5,6,7,8,9,10};
            for(int k=9;k>0;k--){int j=rng.nextInt(k+1),x=d.numbers[k];d.numbers[k]=d.numbers[j];d.numbers[j]=x;}
            d.openTime=1793318400000L+t*60000L;d.fetchTime=d.openTime+1000;require(d.isValid(),"fixture illegal permutation");out.add(d);
        }return out;
    }
    static double[][][][] features(List<Models.Draw> history){
        double[][][][] all=new double[PERIODS][10][10][];
        for(int t=0;t<PERIODS;t++){
            // The current label and all future labels are excluded from the original feature extractor.
            HistoricalStrategyInventionR908.Profile profile=HistoricalStrategyInventionR908.Profile.from(history.subList(0,HISTORY+t));
            require(profile.ready,"original profile unavailable");
            for(int p=1;p<=10;p++)for(int n=1;n<=10;n++)all[t][p-1][n-1]=FrozenConsumerRecipeR932.inputs(profile,p,n);
        }return all;
    }
    static Step scan(AlgorithmGenomeR908 original,AlgorithmGenomeR908 restoredProgram,
            AlgorithmGenomeVmR908.State reference,AlgorithmGenomeVmR908.State restored,
            double[][][] features,Models.Draw label)throws Exception{
        String before=reference.encode(),restoredBefore=restored.encode();require(before.equals(restoredBefore),"pre-scan state mismatch");
        AlgorithmGenomeVmR908.Scorer scorer=new AlgorithmGenomeVmR908.Scorer(original,reference),replay=new AlgorithmGenomeVmR908.Scorer(restoredProgram,restored);
        Step best=null;StringBuilder surface=new StringBuilder();int ordinal=0;
        for(int p=1;p<=10;p++)for(int a=1;a<=8;a++)for(int b=a+1;b<=9;b++)for(int c=b+1;c<=10;c++,ordinal++){
            double[] x=features[p-1][a-1],y=features[p-1][b-1],z=features[p-1][c-1],input=new double[AlgorithmGenomeR908.INPUTS];
            for(int k=0;k<input.length;k++)input[k]=(x[k]+y[k]+z[k])/3;
            double actual=scorer.score(p,input),replayed=replay.score(p,input);
            require(Double.isFinite(actual)&&Double.doubleToLongBits(actual)==Double.doubleToLongBits(replayed),"action-score bit mismatch ordinal="+ordinal);
            // Also compare the allocation-free production scorer against the original direct VM entry.
            if((ordinal&31)==0)require(Double.doubleToLongBits(actual)==Double.doubleToLongBits(AlgorithmGenomeVmR908.score(original,reference,p,input)),"direct/scorer mismatch ordinal="+ordinal);
            surface.append('|').append(Long.toHexString(Double.doubleToLongBits(actual)));
            double spread=Math.max(Math.max(x[2],y[2]),z[2])-Math.min(Math.min(x[2],y[2]),z[2]);
            double margin=actual-.1*spread;
            if(best==null||margin>best.margin||(margin==best.margin&&ordinal<best.ordinal)){
                best=new Step();best.ordinal=ordinal;best.position=p;best.n1=a;best.n2=b;best.n3=c;best.score=actual;best.margin=margin;best.update=input;
                best.update[10]=margin;best.update[11]=actual;
            }
        }
        require(ordinal==ACTIONS,"incomplete full action scan");require(before.equals(reference.encode())&&restoredBefore.equals(restored.encode()),"predraw scorer mutated original state");
        int actual=label.getNumberAtPosition(best.position);best.hit=(actual==best.n1||actual==best.n2||actual==best.n3)?1:0;
        best.update[9]=best.hit;best.update[12]=(actual-1)/9.0;best.update[13]=best.hit-best.score;
        long updates=reference.updates;
        AlgorithmGenomeVmR908.update(original,reference,best.position,best.update.clone());
        AlgorithmGenomeVmR908.update(restoredProgram,restored,best.position,best.update.clone());
        require(reference.updates==updates+1&&reference.encode().equals(restored.encode()),"post-settlement full-state mismatch");
        best.surface=digest(surface.toString());return best;
    }
    public static void main(String[]args)throws Exception{
        Path archive=Paths.get(args[0]),output=Paths.get(args[1]);byte[] bytes=Files.readAllBytes(archive);String[] lines=new String(bytes,StandardCharsets.UTF_8).split("\n");
        String[] columns=lines[0].replace("\r","").split("\t",-1);Map<String,Integer> index=new HashMap<>();for(int i=0;i<columns.length;i++)index.put(columns[i],i);
        for(String field:new String[]{"genome_id","genome_spec","structural_hash","state_blob"})require(index.containsKey(field),"archive column missing "+field);
        List<Models.Draw> history=history();double[][][][] features=features(history);StringBuilder historyInput=new StringBuilder();for(Models.Draw d:history){historyInput.append(d.issue);for(int n:d.numbers)historyInput.append(':').append(n);}
        List<Object> records=new ArrayList<>();int passed=0,missing=0,failed=0;Set<String> ids=new HashSet<>();
        for(int line=1;line<lines.length;line++){
            if(lines[line].isEmpty())continue;Map<String,Object> receipt=new LinkedHashMap<>();String[] row=lines[line].replace("\r","").split("\t",-1);String id=row.length>index.get("genome_id")?row[index.get("genome_id")]:"archive-row-"+line;receipt.put("assetId",id);
            try{
                require(row.length==columns.length,"archive row column mismatch");require(ids.add(id),"duplicate archive identity");
                if(row[index.get("genome_spec")].isEmpty()||row[index.get("state_blob")].isEmpty()){receipt.put("status","MISSING");receipt.put("missing","original program or state_blob");missing++;records.add(receipt);continue;}
                AlgorithmGenomeR908 original=AlgorithmGenomeR908.parse(row[index.get("genome_spec")]);
                require(original.genomeId.equals(id)&&original.structuralHash.equals(row[index.get("structural_hash")]),"original executable identity mismatch");
                AlgorithmGenomeR908 restoredProgram=AlgorithmGenomeR908.parse(original.canonical());
                AlgorithmGenomeVmR908.State reference=AlgorithmGenomeVmR908.State.decodeStrict(row[index.get("state_blob")]),restored=AlgorithmGenomeVmR908.State.decodeStrict(reference.encode());
                require(reference!=restored&&reference.global!=restored.global&&reference.position[0]!=restored.position[0],"replay state aliases original");
                receipt.put("initialStateSha256",digest(reference.encode()));receipt.put("initialUpdates",reference.updates);List<Object> periods=new ArrayList<>();
                for(int t=0;t<PERIODS;t++){
                    Step step=scan(original,restoredProgram,reference,restored,features[t],history.get(HISTORY+t));Map<String,Object> period=new LinkedHashMap<>();period.put("period",t);period.put("fullActionCount",ACTIONS);period.put("surfaceSha256",step.surface);period.put("selectedOrdinal",step.ordinal);period.put("stateSha256",digest(reference.encode()));period.put("updates",reference.updates);period.put("status","BITWISE_OUTPUT_AND_STATE_EQUAL");periods.add(period);
                    if((t+1)%2==0){String durable=restored.encode();restoredProgram=AlgorithmGenomeR908.parse(restoredProgram.canonical());restored=AlgorithmGenomeVmR908.State.decodeStrict(durable);require(durable.equals(restored.encode()),"restart lost original VM state");}
                }
                receipt.put("periods",periods);receipt.put("status","PASS_HOST_ORIGINAL_VM_RESTART_CONTINUITY");passed++;
            }catch(IllegalArgumentException absent){receipt.put("status","MISSING_OR_INVALID_ORIGINAL_MATERIAL");receipt.put("error",absent.toString());missing++;}
            catch(Throwable failure){receipt.put("status","FAIL");receipt.put("error",failure.toString());failed++;}
            records.add(receipt);
        }
        Map<String,Object> result=new LinkedHashMap<>();result.put("schema","R29_B01_ALL_125_ORIGINAL_VM_HOST_CONTINUITY_1");result.put("archiveSha256",digest(bytes));result.put("fixtureHistorySha256",digest(historyInput.toString()));result.put("fixtureSeed",29007);result.put("historyRows",HISTORY);result.put("periodsPerAsset",PERIODS);result.put("restartsPerAsset",PERIODS/2);result.put("actionsPerPeriod",ACTIONS);result.put("passed",passed);result.put("missing",missing);result.put("failed",failed);result.put("assets",records);result.put("scope","SYNTHETIC_HOST_ORIGINAL_VM_SERIALIZATION_REPLAY; original feature extractor + direct/scorer VM; current kernels on both paths; no captured historical-device trace or cross-version numerical ABI equivalence claimed");result.put("productionEntryNotRun","AlgorithmInventionLabR908.trainDevelopmentStep database/runtime scheduling; selection/update fixture follows its source contract");result.put("naturalEvidenceTransferred",false);result.put("AndroidSchedulingAcceptance","NOT_RUN_BY_THIS_TEST");result.put("result",passed==125&&missing==0&&failed==0?"PASS_WITH_STATED_HOST_SCOPE":"FAIL_OR_MISSING_ORIGINAL_MATERIAL");
        Files.createDirectories(output.toAbsolutePath().getParent());Files.write(output,(JsonR27.write(result)+"\n").getBytes(StandardCharsets.UTF_8));
        System.out.println("R29_LEGACY_ARCHIVE|assets="+records.size()+"|passed="+passed+"|missing="+missing+"|failed="+failed+"|periods="+PERIODS+"|actionsPerPeriod="+ACTIONS+"|SYNTHETIC_HOST_ONLY");
        require(passed==125&&missing==0&&failed==0,"all-archive original VM continuity incomplete; inspect JSON receipt");
    }
}
