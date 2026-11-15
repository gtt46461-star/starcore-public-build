package com.ethk3.novaonepro.strategyv2;
/** Reads the existing protected golden asset; a fingerprint match is numerical ABI evidence only. */
public final class FeatureGoldenR31Host {
    public static void main(String[] args)throws Exception{
        ReplayDatasetR27 data;
        try(java.io.InputStream input=new java.io.FileInputStream(args[0])){data=SemanticAbiR27.readGolden(input,true);}
        FeatureCacheR27 cache=new FeatureCacheR27(data);
        System.out.println(JsonR27.write(OperatorGoldenR27.fingerprints(cache)));
    }
}
