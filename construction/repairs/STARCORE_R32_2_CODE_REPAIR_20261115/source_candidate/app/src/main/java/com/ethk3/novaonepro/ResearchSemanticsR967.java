package com.ethk3.novaonepro;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/** Complete pure identity primitive. NOT an automatic dependency/integrity verifier. */
public final class ResearchSemanticsR967 {
    private ResearchSemanticsR967(){}
    private static final int MAX_FIELDS=128,MAX_TEXT_BYTES=1048576;
    public static final class Builder {
        private final String purpose;
        private final TreeMap<String,Field> fields=new TreeMap<String,Field>();
        public Builder(String purpose){if(purpose==null||!purpose.matches("[A-Z0-9_]{1,64}"))throw new IllegalArgumentException("PURPOSE");this.purpose=purpose;}
        public Builder text(String key,String value){if(value==null)throw new IllegalArgumentException("NULL:"+key);return put(key,1,value.getBytes(StandardCharsets.UTF_8));}
        public Builder hash(String key,String value){requireHash(value);return put(key,2,value.getBytes(StandardCharsets.US_ASCII));}
        public Builder integer(String key,long value){return put(key,3,longBytes(value));}
        public Builder bool(String key,boolean value){return put(key,4,new byte[]{(byte)(value?1:0)});}
        public Builder number(String key,double value){if(Double.isNaN(value)||Double.isInfinite(value))throw new IllegalArgumentException("NONFINITE:"+key);return put(key,5,longBytes(Double.doubleToLongBits(value==0d?0d:value)));}
        public Builder bytes(String key,byte[] value){if(value==null)throw new IllegalArgumentException("NULL:"+key);return put(key,6,value.clone());}
        private Builder put(String key,int type,byte[] value){
            if(key==null||!key.matches("[a-z][a-z0-9_]{0,63}")||fields.containsKey(key))throw new IllegalArgumentException("DUPLICATE_OR_INVALID_FIELD:"+key);
            if(fields.size()>=MAX_FIELDS||value.length>MAX_TEXT_BYTES)throw new IllegalArgumentException("CONTRACT_TOO_LARGE");
            fields.put(key,new Field(type,value));return this;
        }
        public byte[] canonicalBytes(){
            try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
                write(out,"R967_TYPED_IDENTITY_V1".getBytes(StandardCharsets.US_ASCII));write(out,purpose.getBytes(StandardCharsets.US_ASCII));out.writeInt(fields.size());
                for(Map.Entry<String,Field> e:fields.entrySet()){write(out,e.getKey().getBytes(StandardCharsets.US_ASCII));out.writeByte(e.getValue().type);write(out,e.getValue().bytes);}
                out.flush();return bytes.toByteArray();
            }catch(IOException impossible){throw new IllegalStateException(impossible);}
        }
        public String digest(){return sha(canonicalBytes());}
    }
    private static final class Field{final int type;final byte[] bytes;Field(int type,byte[] value){this.type=type;bytes=value;}}
    public static String trainingSemantics(String program,String features,String pit,String trainer,String stateSchema,String dependencies){
        return new Builder("TRAIN_RULE_V1").hash("program",program).hash("features",features).hash("pit",pit)
                .hash("trainer",trainer).hash("state_schema",stateSchema).hash("dependencies",dependencies).digest();
    }
    public static String trainingRunHash(String rule,String initialState,String split,String role){
        if(!"DEV".equals(role)&&!"TRAIN_STREAM".equals(role))throw new IllegalArgumentException("ROLE_NOT_TRAINABLE");
        return new Builder("TRAIN_RUN_V1").hash("rule",rule).hash("initial_state",initialState).hash("split",split).text("role",role).digest();
    }
    public static String frozenEvaluationHash(String program,String numericState,String features,String dependencies,String selector,String integrityEpoch){
        return new Builder("FROZEN_EVAL_V1").hash("program",program).hash("numeric_state",numericState).hash("features",features)
                .hash("dependencies",dependencies).hash("selector",selector).hash("integrity_epoch",integrityEpoch).digest();
    }
    public static String calibrationSemantics(String hypothesis,String evaluation,String bundle,String selector,String dependencies,String pit,String calibrator,String integrityEpoch){
        return new Builder("CALIBRATION_V1").hash("hypothesis",hypothesis).hash("evaluation",evaluation).hash("bundle",bundle)
                .hash("selector",selector).hash("dependencies",dependencies).hash("pit",pit).hash("calibrator",calibrator).hash("integrity_epoch",integrityEpoch).digest();
    }
    /** Definition excludes certificate id, target inputs and fitted calibration artifact: no hash cycle. */
    public static String recipeDefinitionHash(String evaluator,String selector,String weights,String probabilityMethod,String dependencies){
        return new Builder("RECIPE_DEFINITION_V1").hash("evaluator",evaluator).hash("selector",selector)
                .hash("weights",weights).hash("probability_method",probabilityMethod).hash("dependencies",dependencies).digest();
    }
    public static String certificateSemantics(String evaluation,String calibrationModel,String plan,String recipeDefinition,String dependencies,String integrityEpoch){
        return new Builder("CERTIFICATE_SEMANTICS_V1").hash("evaluation",evaluation).hash("calibration_model",calibrationModel)
                .hash("plan",plan).hash("recipe_definition",recipeDefinition).hash("dependencies",dependencies).hash("integrity_epoch",integrityEpoch).digest();
    }
    public static String recipeInstanceHash(String definition,String certificate,String input,String target){
        if(target==null||!target.matches("[0-9]{12}"))throw new IllegalArgumentException("TARGET_FORMAT");
        // Caller MUST additionally validate exact base/target through TargetIssueResolver.
        return new Builder("RECIPE_INSTANCE_V1").hash("definition",definition).hash("certificate",certificate).hash("input",input).text("target",target).digest();
    }
    private static void write(DataOutputStream out,byte[] b)throws IOException{out.writeInt(b.length);out.write(b);}
    private static byte[] longBytes(long x){byte[] b=new byte[8];for(int i=7;i>=0;i--){b[i]=(byte)x;x>>>=8;}return b;}
    private static void requireHash(String h){if(h==null||!h.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("HASH_REQUIRED");}
    private static String sha(byte[] b){try{byte[] d=MessageDigest.getInstance("SHA-256").digest(b);StringBuilder s=new StringBuilder(64);for(byte x:d){int v=x&255;s.append("0123456789abcdef".charAt(v>>>4));s.append("0123456789abcdef".charAt(v&15));}return s.toString();}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
