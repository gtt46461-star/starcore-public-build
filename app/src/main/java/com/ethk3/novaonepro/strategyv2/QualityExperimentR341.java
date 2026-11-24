package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.BuildConfig;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Offline transport for the production factory runner and its registered six arms. */
public final class QualityExperimentR341 {
    private static final String SCOPE="ENGINEERING_ONLY_SYNTHETIC_FULL_FACTORY";
    private QualityExperimentR341() {}

    public static void main(String[] args)throws Exception{
        if(args.length!=3)throw new IllegalArgumentException("expected source commit, output directory and step budget");
        if(!args[0].matches("[0-9a-f]{40}")||!BuildConfig.SOURCE_COMMIT_R26.equals(args[0]))
            throw new IllegalArgumentException("R341_FACTORY_SOURCE_COMMIT_MISMATCH");
        int budget=Integer.parseInt(args[2]);
        if(budget<1)throw new IllegalArgumentException("R341_FACTORY_STEP_BUDGET");
        Path out=Paths.get(args[1]).toAbsolutePath().normalize();
        Files.createDirectories(out);
        boolean complete;
        try(FileChannel channel=FileChannel.open(out.resolve("factory.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
                FileLock lock=channel.tryLock()){
            if(lock==null)throw new IllegalStateException("R341_FACTORY_OUTPUT_BUSY");
            complete=run(args[0],out,budget);
        }
        if(!complete)System.exit(2);
    }

    static boolean run(String source,Path out,int budget)throws Exception{
        Path manifest=out.resolve("registration.json"),checkpoint=out.resolve("checkpoint.json");
        Map<String,Object> registration=new LinkedHashMap<>();registration.put("sourceCommit",source);
        registration.put("scope",SCOPE);registration.put("protocolHash",FactoryCapabilityExperimentR31.protocolHash());
        registration.put("protocol",FactoryCapabilityExperimentR31.protocol());
        registration.put("productionWritable",false);registration.put("naturalQualityEligible",false);
        String registered=JsonR27.write(registration);
        if(Files.exists(manifest)){
            if(!registered.equals(read(manifest)))throw new IllegalStateException("R341_FACTORY_REGISTRATION_DRIFT");
        }else write(manifest,registered);
        try(FactoryCapabilityExperimentR31 experiment=Files.exists(checkpoint)
                ?FactoryCapabilityExperimentR31.restore(read(checkpoint)):new FactoryCapabilityExperimentR31(source)){
            if(!source.equals(experiment.source()))throw new IllegalStateException("R341_FACTORY_RESTART_SOURCE_MISMATCH");
            Path trials=out.resolve("trials");Files.createDirectories(trials);
            for(int step=0;step<budget&&!experiment.complete();step++){
                experiment.step();
                Map<String,Object> trial=experiment.pendingArtifact();
                if(trial!=null)preserveTrial(trials,trial);
                write(checkpoint,experiment.snapshot());
            }
            Map<String,Object> summary=experiment.summary();summary.put("scope",SCOPE);
            summary.put("originalTrialDirectory","trials");
            write(out.resolve("summary.json"),JsonR27.write(summary));
            System.out.println(JsonR27.write(summary));
            return experiment.complete()&&Boolean.TRUE.equals(summary.get("meetsRegisteredExperiment"));
        }
    }

    private static void preserveTrial(Path dir,Map<String,Object> current)throws Exception{
        String arm=String.valueOf(current.get("arm"));long seed=((Number)current.get("seed")).longValue();
        if(!arm.matches("[A-Z_]+")||seed<710000||seed>=710000+FactoryCapabilityExperimentR31.REPLICATES)
            throw new IllegalStateException("R341_FACTORY_TRIAL_IDENTITY");
        Path trial=dir.resolve(arm+"-"+seed+".json");
        if(Files.exists(trial)){
            Object previous=JsonR27.parse(read(trial));
            if(!(previous instanceof Map))throw new IllegalStateException("R341_FACTORY_ORIGINAL_TRIAL_TYPE");
            for(String field:new String[]{"arm","seed","sourceCommit","protocolHash","status","candidateLedgerSha256","certificateFailures"})
                if(!Objects.equals(String.valueOf(((Map<?,?>)previous).get(field)),String.valueOf(current.get(field))))
                    throw new IllegalStateException("R341_FACTORY_ORIGINAL_TRIAL_CONFLICT:"+trial+":"+field);
        }else write(trial,JsonR27.write(current));
    }

    private static String read(Path path)throws Exception{
        if(Files.size(path)>64L<<20)throw new IllegalStateException("R341_FACTORY_ORIGINAL_SIZE");
        return new String(Files.readAllBytes(path),StandardCharsets.UTF_8);
    }

    private static void write(Path path,String text)throws Exception{
        Path pending=path.resolveSibling(path.getFileName()+".pending");
        byte[] bytes=text.getBytes(StandardCharsets.UTF_8);
        if(bytes.length>64L<<20)throw new IllegalStateException("R341_FACTORY_OUTPUT_SIZE");
        try(FileChannel channel=FileChannel.open(pending,StandardOpenOption.CREATE,StandardOpenOption.WRITE,StandardOpenOption.TRUNCATE_EXISTING)){
            java.nio.ByteBuffer buffer=java.nio.ByteBuffer.wrap(bytes);
            while(buffer.hasRemaining())channel.write(buffer);
            channel.force(true);
        }
        Files.move(pending,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }
}
