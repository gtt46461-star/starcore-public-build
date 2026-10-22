package com.ethk3.novaonepro.r952;

public final class FullbloodContractHarness {
    private static void require(boolean value,String code){if(!value)throw new AssertionError(code);}
    public static void main(String[] args){
        CandidateAstR952 ast=new CandidateAstR952(CandidateAstR952.Operator.RECENCY_BLEND,1,120);
        require(ast.canonical.indexOf("K=3")>=0,"FIXED_K3");
        require(CandidateAstR952.parse(ast.canonical).structuralHash.equals(ast.structuralHash),"AST_PARITY");
        double[] scores=new double[1200];for(int i=0;i<scores.length;i++)scores[i]=i%17;
        int[] ranked=ActionRankContractR952.rank(scores);require(ranked.length==1200,"ACTION_1200");
        require(!PredictabilityContractR952.evaluate(119,.4,.1,.2,2).pass,"FUTURE_GATE");
        CausalValidationEngineR952.Result causal=CausalValidationEngineR952.evaluate("H",4,.1,"none",0);
        require(causal.status==CausalEdgeR952.Status.CORRELATIONAL,"NO_FAKE_CAUSAL");
        boolean blocked=false;try{CandidateSandboxR952.rejectReal();}catch(SecurityException expected){blocked=true;}
        require(blocked,"NO_DIRECT_REAL");
        System.out.println("R952_FULLBLOOD_CONTRACT=PASS|tests=7");
    }
}
