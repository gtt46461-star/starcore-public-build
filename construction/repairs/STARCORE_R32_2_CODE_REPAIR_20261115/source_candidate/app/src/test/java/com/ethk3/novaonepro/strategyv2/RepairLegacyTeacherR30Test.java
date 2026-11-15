package com.ethk3.novaonepro.strategyv2;
import org.junit.Test;import static org.junit.Assert.*;import java.util.*;
public class RepairLegacyTeacherR30Test {
 @Test public void invalidRebindCannotChangeOriginalBinding(){Map<String,String> before=FamilyRegistryR26.legacyBinding();for(String fault:new String[]{"abi","asset","teacher","state","scope"}){Map<String,String> b=new HashMap<>();b.put("abi","1");b.put("scope","LEGACY_REPORTED_PREFERENCE_PROJECTION_V1");for(String key:new String[]{"asset","teacher","state"})b.put(key,StatsR27.sha256(key));b.put(fault,"wrong");try{FamilyRegistryR26.bindLegacyTeacher(b,p->null);fail(fault);}catch(IllegalArgumentException expected){}assertSame(before,FamilyRegistryR26.legacyBinding());}}
}
