package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** P46 weights and selected-number contributions come from the trained production network, not labels or templates. */
public final class MatrixSnapshotSU2Test {
    static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        int[][] draws=Arrays.copyOf(ModelEvolutionR14Test.load(Paths.get(args[0],"REGIME_seed1.txt")),2200);
        Map<String,String> p=new TreeMap<>();p.put("atoms","0-1-2");p.put("pairs","0x1");p.put("mols","0@0");p.put("lags","none");p.put("group","0");p.put("lr","0.02");p.put("l2","0.0001");p.put("gd","0.999");
        ModelGenomeSU2 g=new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member("ATOMNET",p,1)),"STATIC",false,"MASS",.3,.2,true);
        CommitteeSU2 committee=new CommitteeSU2(Collections.singletonList(g),.3);
        MatrixSnapshotSU2.Data snapshot=MatrixSnapshotSU2.capture(committee,new ModelReplaySU2.Library(draws,64L<<20),draws.length-1);
        AtomNetSU2 net=AtomNetSU2.of(p);float[][] q=new float[10][10];float[] st=new float[3];for(int[] d:draws){net.predict(q,st);net.update(d);}
        List<MatrixSnapshotSU2.Factor> factors=MatrixSnapshotSU2.decode(snapshot.factors);require(factors.size()==5,"factor descriptors lost");
        for(int f=0;f<factors.size();f++)for(int position=0;position<10;position++)require(factors.get(f).weight[position]==net.u[f]+net.v[position*net.Fd+f],"snapshot weight is not trained coefficient");
        AtomStreamSU2 restored=new AtomStreamSU2();restored.read(new DataInputStream(new ByteArrayInputStream(snapshot.stream)));require(restored.time()==draws.length,"source state checkpoint lost");
        net.predict(q,st);int position=3,mask=(1<<0)|(1<<3)|(1<<7);double actual=0,expected=0;
        for(double v:MatrixSnapshotSU2.support(factors,restored,position,mask).values())actual+=v;
        for(int n=0;n<10;n++)if((mask&(1<<n))!=0)for(int f=0;f<net.Fd;f++)expected+=net.X[((position-1)*10+n)*net.Fd+f]*(net.u[f]+net.v[(position-1)*net.Fd+f])/3;
        require(Math.abs(actual-expected)<1e-9,"reason support differs from actual feature dot product");
        require(MatrixSnapshotSU2.teacherStates().length==5,"teacher states incomplete");
        require(MatrixSnapshotSU2.grade(0,0,0,0).equals("C_EXPLAIN_ONLY"),"missing forward evidence granted a grade");
        require(MatrixSnapshotSU2.grade(200,90,200,45).equals("A_VALID"),"supported forward evidence lost");
        require(snapshot.json.contains("atomic_explanation")&&snapshot.json.contains("teacher_matrix"),"report contract incomplete");
        Path out=Paths.get("build/plan1025/matrix-smoke.json");Files.createDirectories(out.getParent());Files.write(out,snapshot.json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        System.out.println("P46_MATRIX_SNAPSHOT_PASS: trained coefficients, all feature types, source-state resume, actual dot product and evidence grades");
    }
}
