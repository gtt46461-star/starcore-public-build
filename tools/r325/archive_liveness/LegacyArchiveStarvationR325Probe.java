package com.ethk3.novaonepro.strategyv2;
import java.nio.file.*;
/** Executes the unmodified R32.4 production AtomNetRuntimeR30.step entry. */
public final class LegacyArchiveStarvationR325Probe {
    public static void main(String[] args)throws Exception {
        Class.forName("org.sqlite.JDBC");int failures=0;
        try(ArchiveLivenessFixtureR325 f=new ArchiveLivenessFixtureR325(Paths.get(args[0]))){
            f.insert(ArchiveLivenessFixtureR325.BAD,9,1);f.insert(ArchiveLivenessFixtureR325.GOOD,10,2);f.current();
            RuntimeR27 runtime=f.runtime();ResearchPoolR27.Worker worker=f.worker();
            for(int i=0;i<3;i++)try{AtomNetRuntimeR30.step(runtime,worker);throw new AssertionError("legacy unexpectedly restored current model");}
            catch(java.io.IOException expected){if(!"R324_SAMPLE_ARCHIVE_SOURCE_INCOMPLETE".equals(expected.getMessage()))throw expected;failures++;}
            if(runtime.atomSessionR30!=null||f.scalar("SELECT count(*) FROM r324_atomnet_sample_archive")!=0||f.scalar("SELECT count(*) FROM r31_atomnet_sample_stage")!=19)throw new AssertionError("legacy fixture changed unexpectedly");
        }
        System.out.println("LEGACY_REPRODUCED_BLOCKED_COLD_RESTORE_ATTEMPTS="+failures);
    }
}
