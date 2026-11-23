package com.ethk3.novaonepro.strategyv2;
import java.util.Map;
import java.util.TreeMap;

public final class ColdRebirthContractR34Test {
    private static int assertions;
    private static void need(boolean condition,String message){assertions++;if(!condition)throw new AssertionError(message);}
    private static void reject(Runnable action,String message){boolean failed=false;try{action.run();}catch(IllegalArgumentException expected){failed=true;}need(failed,message);}
    public static void main(String[] args){
        String a="a".repeat(64),b="b".repeat(64),c="c".repeat(64);
        String source=ColdRebirthContractR34.sourceKey("CANDIDATE","old-campaign","old-policy");
        need(source.equals(ColdRebirthContractR34.sourceKey("CANDIDATE","old-campaign","old-policy")),"stable replay identity");
        need(!source.equals(ColdRebirthContractR34.sourceKey("EXPERT","old-campaign","old-policy")),"owner domains separated");
        need(!ColdRebirthContractR34.hash("ab","c").equals(ColdRebirthContractR34.hash("a","bc")),"length encoding is unambiguous");
        String id=ColdRebirthContractR34.campaign(source,a,b);
        need(id.equals(ColdRebirthContractR34.campaign(source,a,b)),"same plan gives same child campaign");
        need(!id.equals(ColdRebirthContractR34.campaign(source,c,b)),"changed ABI gets new research identity");
        need(!id.equals(ColdRebirthContractR34.campaign(source,a,c)),"changed source gets distinct preparation identity");
        Map<String,String> old=new TreeMap<>();old.put("lr","0.02");old.put("seed","7");old.put("_warm_kernel",b);old.put("_warm_checkpoint",c);old.put("_birth_snapshot",a);old.put("_cold_parent",c);old.put("_legal_head","OLD");
        Map<String,String> fresh=ColdRebirthContractR34.coldParameters(old,source,a);
        need(fresh.get("lr").equals("0.02")&&fresh.get("seed").equals("7"),"numerical structure retained");
        need(fresh.keySet().stream().noneMatch(k->k.startsWith("_warm_")||k.startsWith("_birth_")),"all weight and birth transfers removed");
        need(old.get("_warm_checkpoint").equals(c)&&old.get("_cold_parent").equals(c),"old parent map unchanged");
        need(fresh.get("_cold_parent").equals(source)&&fresh.get("_cold_abi").equals(a),"new lineage bound");
        Map<String,String> numeric=ColdRebirthContractR34.stripColdParameters(fresh);
        need(numeric.size()==2&&numeric.get("lr").equals("0.02")&&numeric.get("seed").equals("7"),"strict numerical factory sees only original parameters");
        need(ColdRebirthContractR34.hasColdParameters(fresh),"identity lineage remains on caller map");
        need(ColdRebirthContractR34.stripColdParameters(numeric).equals(numeric),"non-cold factory parameters preserved");
        reject(()->ColdRebirthContractR34.stripColdParameters(Map.of("_cold_parent",source)),"partial lineage rejected");
        Map<String,String> unknown=new TreeMap<>(fresh);unknown.put("_cold_other","x");
        reject(()->ColdRebirthContractR34.stripColdParameters(unknown),"unknown lineage field rejected");
        Map<String,String> wrong=new TreeMap<>(fresh);wrong.put("_cold_protocol","not-current");
        reject(()->ColdRebirthContractR34.stripColdParameters(wrong),"wrong cold protocol rejected");
        ColdRebirthContractR34.requireSnapshot(4096,172040,1000,5095,4095);
        reject(()->ColdRebirthContractR34.requireSnapshot(63,100,1,63,63),"insufficient source rejected");
        reject(()->ColdRebirthContractR34.requireSnapshot(4097,100,1,4097,0),"work bound enforced");
        reject(()->ColdRebirthContractR34.requireSnapshot(4096,176*1024+1,1,4096,0),"writer byte bound enforced");
        reject(()->ColdRebirthContractR34.requireSnapshot(64,3000,1,5001,0),"history span bound enforced");
        reject(()->ColdRebirthContractR34.requireSnapshot(64,3000,100,164,200),"exposure not moved backward");
        ColdRebirthContractR34.requireFreshCounts(0,0,0,0,0);
        for(int i=0;i<5;i++){final int at=i;reject(()->{long[] n=new long[5];n[at]=1;ColdRebirthContractR34.requireFreshCounts(n[0],n[1],n[2],n[3],n[4]);},"no old count inheritance "+i);}
        need(ColdRebirthContractR34.retryDelay(1)==30000,"retry begins bounded");
        need(ColdRebirthContractR34.retryDelay(1000000)==1800000,"retry saturates without overflow");
        reject(()->ColdRebirthContractR34.sourceKey("LIVE","x","y"),"no real authority source");
        String successorSource=ColdRebirthContractR34.sourceKey("CANDIDATE",id,"fresh-policy");
        need(!successorSource.equals(source),"expired zero child becomes a distinct successor parent");
        need(!ColdRebirthContractR34.campaign(successorSource,a,b).equals(id),"same-ABI zero-trained restart has a fresh identity");
        reject(()->ColdRebirthContractR34.protectZeroCandidateSql("c;DROP TABLE x"),"internal SQL alias bound");
        reject(()->ColdRebirthContractR34.pruneExpiredExpertsSql(513),"retention slice bound");
        System.out.println("R34_COLD_PURE_CONTRACT_PASS assertions="+assertions+" sourceKey="+source+" campaign="+id);
        System.out.println("protectCandidateSQL="+ColdRebirthContractR34.protectZeroCandidateSql("r26_candidate"));
        System.out.println("protectExpertSQL="+ColdRebirthContractR34.protectZeroExpertSql("e"));
        System.out.println("pruneSourceSQL="+ColdRebirthContractR34.pruneSourceSql());
        System.out.println("pruneExpertSQL="+ColdRebirthContractR34.pruneExpiredExpertsSql(16));
    }
}
