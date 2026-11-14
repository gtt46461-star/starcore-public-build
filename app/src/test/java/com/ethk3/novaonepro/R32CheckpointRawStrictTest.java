package com.ethk3.novaonepro;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class R32CheckpointRawStrictTest {
    @Test public void missingOriginalStillBlocksTheActualRawAndGcReachabilityPath()throws Exception{
        File root=Files.createTempDirectory("r32-strict-original-").toFile();
        com.ethk3.novaonepro.strategyv2.CheckpointArtifactsR26 store=new com.ethk3.novaonepro.strategyv2.CheckpointArtifactsR26(root);
        try{store.reachableHashes(Collections.singleton(String.format(Locale.ROOT,"%064x",32)));fail("Missing original accepted by raw/GC closure");}catch(FileNotFoundException expected){assertTrue(expected.getMessage().contains(".bin"));}
    }
}
