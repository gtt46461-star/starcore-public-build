package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import java.nio.file.*;
import java.io.*;
import java.util.*;

/** Executes the actual Android numerical engine on a supplied historical dataset; it grants no authority. */
public final class RunPhoneCampaignsSU1 {
    private static void durable(Path path,String content)throws IOException{
        Path temp=path.resolveSibling(path.getFileName()+".partial");try(FileOutputStream stream=new FileOutputStream(temp.toFile())){stream.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));stream.flush();stream.getFD().sync();}Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=2)throw new IllegalArgumentException("CSV_PATH OUTPUT_DIRECTORY");Path csv=Paths.get(args[0]),directory=Paths.get(args[1]);Files.createDirectories(directory);
        List<int[]> rows=new ArrayList<>();for(String line:Files.readAllLines(csv)){
            if(line.startsWith("period,"))continue;int first=line.indexOf('"'),last=line.indexOf('"',first+1);if(first<0||last<0)throw new IllegalArgumentException("CSV_DRAW_FIELD");String[] fields=line.substring(first+1,last).split(",");int[] row=new int[10];for(int p=0;p<10;p++)row[p]=Integer.parseInt(fields[p]);RelationalStateInference.validateDraw(row);rows.add(row);if(rows.size()==5000)break;
        }
        if(rows.size()!=5000)throw new IllegalArgumentException("NEED_5000_ROWS");int[][] raw=rows.toArray(new int[0][]);String source=HashingSU1.bytes(Files.readAllBytes(csv));List<Object> reports=new ArrayList<>();long begin=System.nanoTime();
        for(CampaignControlsSU1.Variant variant:CampaignControlsSU1.Variant.values()){
            Path state=directory.resolve(variant+".checkpoint.json");Set<String> seen=new TreeSet<>();CampaignAuditSessionSU1 session;
            if(Files.isRegularFile(state)){Map<String,Object> saved=StrictCanonicalJson.readContextObject(Files.readString(state));for(Object key:(List<?>)saved.get("seen"))seen.add((String)key);session=CampaignAuditSessionSU1.restore(raw,source,(String)saved.get("session"));session.foundry.restoreSeen(seen);}else session=new CampaignAuditSessionSU1(raw,source,variant,11);
            try(CampaignAuditSessionSU1 active=session){
                while(!session.phase.equals("DONE")){
                    try{session.advance(new BudgetTokenSU1(2000,30000000,null));}catch(BudgetTokenSU1.Yield yielded){/* same numerical slice cap as the Android job */}
                    if(session.attempt!=null)seen.add(session.attempt.genome.key());
                    Map<String,Object> saved=new TreeMap<>();saved.put("session",session.checkpoint());saved.put("seen",new ArrayList<>(seen));durable(state,StrictCanonicalJson.writeContext(saved));
                    if(session.attempt!=null)System.out.println(variant+" attempt="+session.attempts+" evaluated="+session.foundry.evaluated+" result="+session.attempt.result+" phase="+session.phase);
                }
                Map<String,Object> report=StrictCanonicalJson.readContextObject(session.result());reports.add(report);durable(directory.resolve(variant+".report.json"),session.result()+"\n");System.out.println(variant+" COMPLETE "+session.result());
            }
        }
        Map<String,Object> result=new TreeMap<>();result.put("scope","ACTUAL_DEPLOYED_JAVA_ENGINE_HISTORICAL_RESEARCH_ONLY");result.put("inputCsvSha256",source);result.put("rows",5000);result.put("seed",11);result.put("seconds",(System.nanoTime()-begin)/1000000000);result.put("reports",reports);durable(directory.resolve("combined.json"),StrictCanonicalJson.writeContext(result)+"\n");
    }
}
