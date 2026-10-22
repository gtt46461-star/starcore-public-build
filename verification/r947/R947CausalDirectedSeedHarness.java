package com.ethk3.novaonepro;
public final class R947CausalDirectedSeedHarness {
    public static void main(String[] args) {
        long seed=0x1234ABCDL;
        AlgorithmGenomeR908 open=DirectedResearchCreationR932.directedSeed(seed,"OPEN_EXPLORATION","OPEN");
        AlgorithmGenomeR908 trend=DirectedResearchCreationR932.directedSeed(seed,"OPEN_EXPLORATION","TREND");
        AlgorithmGenomeR908 shape=DirectedResearchCreationR932.directedSeed(seed,"OPEN_EXPLORATION","SHAPE");
        AlgorithmGenomeR908 relation=DirectedResearchCreationR932.directedSeed(seed,"OPEN_EXPLORATION","RELATION");
        require(open.algorithmGrade(),"open grade");
        require(trend.algorithmGrade()&&shape.algorithmGrade()&&relation.algorithmGrade(),"directed grade");
        require(!open.structuralHash.equals(trend.structuralHash),"trend changes structure");
        require(!trend.structuralHash.equals(shape.structuralHash),"shape distinct");
        require(!shape.structuralHash.equals(relation.structuralHash),"relation distinct");
        require(trend.canonical().contains("LOAD_X,"+((trend.scoreOutputReg+1)%AlgorithmGenomeR908.REGS)+",2,"),"trend feature2");
        require(shape.canonical().contains("LOAD_X,"+((shape.scoreOutputReg+1)%AlgorithmGenomeR908.REGS)+",3,"),"shape feature3");
        require(relation.canonical().contains("LOAD_X,"+((relation.scoreOutputReg+1)%AlgorithmGenomeR908.REGS)+",1,"),"relation feature1");
        System.out.println("R947_CAUSAL_DIRECTED_SEED_PASS|open="+open.structuralHash.substring(0,12)
                +"|trend="+trend.structuralHash.substring(0,12)+"|shape="+shape.structuralHash.substring(0,12)
                +"|relation="+relation.structuralHash.substring(0,12));
    }
    private static void require(boolean ok,String m){if(!ok)throw new AssertionError(m);}
}
