package com.ethk3.novaonepro.r952;

import java.security.MessageDigest;

public final class ModelBundleLoaderR952 {
    private ModelBundleLoaderR952(){}
    public static byte[] verify(BundleManifestR952 manifest,byte[] artifact,String expectedSchema,String expectedCode){if(manifest==null||artifact==null)throw new IllegalArgumentException("R952_MODEL_LOAD_INPUT");if(!manifest.artifactHash.equals(hash(artifact)))throw new IllegalStateException("R952_MODEL_ARTIFACT_HASH_DRIFT");if(!manifest.inputSchemaHash.equals(expectedSchema))throw new IllegalStateException("R952_MODEL_SCHEMA_DRIFT");if(!manifest.codeHash.equals(expectedCode))throw new IllegalStateException("R952_MODEL_CODE_DRIFT");return artifact.clone();}
    private static String hash(byte[] bytes){
        try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder out=new StringBuilder(64);for(byte value:digest)out.append(String.format("%02x",value&255));return out.toString();}
        catch(Exception e){throw new IllegalStateException("R952_SHA256_UNAVAILABLE",e);}
    }
}
