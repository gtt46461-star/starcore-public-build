import java.lang.reflect.Method;
import com.ethk3.novaonepro.r952.EvolutionControllerR952;
public final class EvolutionStoragePolicyCP12Test {
    private static int checks;
    private static void check(Method method,String reason,boolean expected)throws Exception {
        if(((Boolean)method.invoke(null,reason)).booleanValue()!=expected)throw new AssertionError(reason);
        checks++;
    }
    public static void main(String[] args)throws Exception {
        Method retry=EvolutionControllerR952.class.getDeclaredMethod("isTransientStorageCP12",String.class);
        Method terminal=EvolutionControllerR952.class.getDeclaredMethod("isDeterministicStorageCP12",String.class);
        retry.setAccessible(true);terminal.setAccessible(true);
        for(String code:new String[]{"MODEL_STORAGE_CAPACITY","MODEL_STORAGE_LEGACY_OVER_BUDGET","TRAINING_WORKING_STORAGE_CAPACITY","TRAINING_WORKING_STORAGE_LEGACY_OVER_BUDGET","TRAINING_INPUT_STORAGE_CAPACITY","CAS_QUOTA"}){check(retry,code,true);check(terminal,code,false);}
        for(String code:new String[]{"MODEL_STORAGE_HASH_SIZE_CONFLICT","MODEL_STORAGE_SINGLE_PAYLOAD_BOUND","TRAINING_WORKING_SINGLE_PAYLOAD_BOUND"}){check(retry,code,false);check(terminal,code,true);}
        for(String code:new String[]{"TRAINING_WORKING_UNKNOWN","NOT_CAS_QUOTA","WAIT_HISTORY","MEMORY_PRESSURE",""}){check(retry,code,false);check(terminal,code,false);}
        System.out.println("{\"status\":\"PASS\",\"assertions\":"+checks+",\"scope\":\"Actual EvolutionController private production storage classifiers; deterministic identity/payload bound is terminal and cannot enter capacity WAIT_INPUT.\"}");
    }
}
