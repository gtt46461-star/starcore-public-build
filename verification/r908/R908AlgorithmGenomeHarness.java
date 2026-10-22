package com.ethk3.novaonepro;

public final class R908AlgorithmGenomeHarness {
    public static void main(String[] args) {
        AlgorithmGenomeR908 g = AlgorithmGenomeR908.seed(202608272058L, 1, "PRIMITIVE_SEARCH");
        require(g.algorithmGrade(), "ALGORITHM_GRADE");
        AlgorithmGenomeR908 p = AlgorithmGenomeR908.parse(g.canonical());
        require(g.structuralHash.equals(p.structuralHash), "ROUNDTRIP_HASH");
        AlgorithmGenomeR908 lineageOnly = new AlgorithmGenomeR908(99, "DIFFERENT_LINEAGE", g.scoreProgram, g.updateProgram, g.scoreOutputReg);
        require(g.structuralHash.equals(lineageOnly.structuralHash), "LINEAGE_NOT_STRUCTURAL_NOVELTY");
        require(g.canonical().equals(p.canonical()), "ROUNDTRIP_CANONICAL");
        AlgorithmGenomeVmR908.State s = new AlgorithmGenomeVmR908.State();
        double[] x = new double[AlgorithmGenomeR908.INPUTS];
        for (int i=0;i<9;i++) x[i] = 0.05d + 0.09d*i;
        String before = s.encode();
        double a = AlgorithmGenomeVmR908.score(g,s,3,x);
        double b = AlgorithmGenomeVmR908.score(g,s,3,x);
        require(Double.doubleToLongBits(a)==Double.doubleToLongBits(b), "PREDRAW_DETERMINISTIC");
        require(before.equals(s.encode()), "PREDRAW_STATE_READ_ONLY");
        x[9]=1.0d; x[10]=0.22d; x[11]=a; x[12]=0.66d; x[13]=1.0d-a;
        AlgorithmGenomeVmR908.update(g,s,3,x);
        require(!before.equals(s.encode()), "SETTLEMENT_STATE_CHANGED");
        require(s.updates==1L, "STATE_UPDATE_COUNT");
        AlgorithmGenomeR908 m = g.mutate(99887766L,2);
        require(m.algorithmGrade(), "MUTANT_ALGORITHM_GRADE");
        require(!g.structuralHash.equals(m.structuralHash), "STRUCTURAL_MUTATION");
        AlgorithmGenomeR908 mate = AlgorithmGenomeR908.seed(202608272059L, 1, "PRIMITIVE_SEARCH_B");
        AlgorithmGenomeR908 child = g.crossover(mate, 44332211L, 2);
        require(child.algorithmGrade(), "CROSSOVER_ALGORITHM_GRADE");
        require(!child.structuralHash.equals(g.structuralHash), "CROSSOVER_NEW_STRUCTURE");
        require(child.parentIds.contains(g.genomeId) && child.parentIds.contains(mate.genomeId), "CROSSOVER_PARENT_TRACE");
        String bh1=AlgorithmGenomeVmR908.behaviorHash(g), bh2=AlgorithmGenomeVmR908.behaviorHash(g);
        require(bh1.equals(bh2), "BEHAVIOR_HASH_DETERMINISTIC");
        require(g.mechanismCell()!=null && g.mechanismCell().length()>0, "MECHANISM_CELL");
        require(g.chineseName()!=null && g.chineseName().contains("算法") && g.chineseName().contains("代"), "CHINESE_MECHANISM_NAME");
        System.out.println("R908_ALGORITHM_GENOME_CONTRACT=PASS");
        System.out.println("GENOME_ID="+g.genomeId);
        System.out.println("NAME_ZH="+g.chineseName());
        System.out.println("STRUCTURAL_HASH="+g.structuralHash);
        System.out.println("BEHAVIOR_HASH="+bh1);
        System.out.println("MECHANISM_CELL="+g.mechanismCell());
        System.out.println("LINEAGE_NOT_STRUCTURAL_NOVELTY=PASS");
        System.out.println("SCORE_BEFORE="+a);
        System.out.println("STATE_UPDATES="+s.updates);
        System.out.println("MUTANT_ID="+m.genomeId);
        System.out.println("MUTANT_NAME_ZH="+m.chineseName());
        System.out.println("CROSSOVER_ID="+child.genomeId);
        System.out.println("CROSSOVER_NAME_ZH="+child.chineseName());
        System.out.println("CROSSOVER_PARENTS="+child.parentIds);
        System.out.println("INPUT_CONTRACT="+AlgorithmGenomeVmR908.explainInputs());
    }
    private static void require(boolean ok,String code){if(!ok)throw new IllegalStateException("FAIL:"+code);}
}
