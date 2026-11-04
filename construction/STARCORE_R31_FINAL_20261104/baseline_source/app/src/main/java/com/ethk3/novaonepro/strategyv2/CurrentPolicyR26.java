package com.ethk3.novaonepro.strategyv2;
/** Verify the complete current numerical identity, including composite/fitted policies. */
public final class CurrentPolicyR26 {
    private CurrentPolicyR26() {}
    public static boolean matches(String text,String hash){
        try{
            if(StageStrategySU2.isStage(text))return StageStrategySU2.currentPolicy(text)&&StageStrategySU2.keyOf(text).equals(hash);
            if(CommitteeSU2.isCommittee(text)){CommitteeSU2 c=CommitteeSU2.parse(text);return c.legalReadoutR26()&&c.key().equals(hash);}
            ModelGenomeSU2 g=ModelGenomeSU2.parse(text);return text.startsWith("SU2|ID=R26|")&&g.legalReadoutR26()&&g.key().equals(hash);
        }catch(IllegalArgumentException invalid){return false;}
    }
}
