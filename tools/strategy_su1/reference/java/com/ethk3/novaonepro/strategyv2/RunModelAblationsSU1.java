package com.ethk3.novaonepro.strategyv2;
import java.nio.file.*;
import java.io.*;
import java.util.*;
/** Executes the production phone model-control engine on the supplied history, with durable freeze/restart. */
public final class RunModelAblationsSU1 {
    private static void durable(Path path,String content)throws Exception{Path temp=path.resolveSibling(path.getFileName()+".partial");try(FileOutputStream out=new FileOutputStream(temp.toFile())){out.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));out.flush();out.getFD().sync();}Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
    public static void main(String[] args)throws Exception{
        if(args.length!=3)throw new IllegalArgumentException("CSV OUTPUT_DIRECTORY EXECUTED_CLASS_ARCHIVE_SHA");Path csv=Paths.get(args[0]),out=Paths.get(args[1]);Files.createDirectories(out);String source=HashingSU1.bytes(Files.readAllBytes(csv)),kernel=HashingSU1.sha(args[2]);List<int[]> rows=new ArrayList<>();
        for(String line:Files.readAllLines(csv)){if(line.startsWith("period,"))continue;int first=line.indexOf('"'),last=line.indexOf('"',first+1);if(first<0||last<0)throw new IllegalArgumentException("CSV_DRAW_FIELD");String[] fields=line.substring(first+1,last).split(",");int[] row=new int[10];for(int p=0;p<10;p++)row[p]=Integer.parseInt(fields[p]);RelationalStateInference.validateDraw(row);rows.add(row);if(rows.size()==5000)break;}
        if(rows.size()!=5000)throw new IllegalArgumentException("MODEL_AUDIT_REQUIRES_5000_ROWS");int[][] raw=rows.toArray(new int[0][]);List<Object> reports=new ArrayList<>();
        for(long seed:new long[]{11,23,41}){
            Path checkpoint=out.resolve("seed-"+seed+".checkpoint.json");ModelAblationSessionSU1 session=Files.isRegularFile(checkpoint)?ModelAblationSessionSU1.restore(raw,source,kernel,Files.readString(checkpoint)):new ModelAblationSessionSU1(raw,source,kernel,seed);
            while(!session.phase.equals("DONE")){
                try{session.advance(new BudgetTokenSU1(2000,30000000,null));}catch(BudgetTokenSU1.Yield exhausted){/* Identical phone slice limits. */}
                durable(checkpoint,session.checkpoint());System.out.println("seed="+seed+" phase="+session.phase+" checkpointBytes="+Files.size(checkpoint));
                session=ModelAblationSessionSU1.restore(raw,source,kernel,Files.readString(checkpoint));
            }
            String report=session.report();durable(out.resolve("seed-"+seed+".report.json"),report+"\n");reports.add(com.ethk3.novaonepro.r952.StrictCanonicalJson.readContextObject(report));System.out.println("seed="+seed+" DONE "+report);
        }
        Map<String,Object> combined=new TreeMap<>();combined.put("inputCsvSha256",source);combined.put("executedClassArchiveSha256",kernel);combined.put("scope","ACTUAL_JVM_PRODUCTION_MODEL_ABLATIONS_HISTORICAL_ONLY_NO_NATURAL_AUTHORITY");combined.put("reports",reports);durable(out.resolve("combined.json"),com.ethk3.novaonepro.r952.StrictCanonicalJson.writeContext(combined)+"\n");
    }
}
