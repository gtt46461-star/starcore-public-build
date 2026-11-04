package com.ethk3.novaonepro.strategyv2;
import java.util.*;
public final class SchemaContractHostR31 {
    public static void main(String[] args)throws Exception{
        Map<String,Object> out=new LinkedHashMap<>();out.put("revision",ModelPublicationR31.REVISION);out.put("contractHash",ModelPublicationR31.contractHashR31());List<Object> objects=new ArrayList<>();for(String[] object:ModelPublicationR31.schemaObjectsR31())objects.add(Arrays.asList(object));out.put("objects",objects);java.lang.reflect.Method ddl=ModelPublicationR31.class.getDeclaredMethod("ddl");ddl.setAccessible(true);out.put("exactDdl",Arrays.asList((String[])ddl.invoke(null)));System.out.println(JsonR27.write(out));
    }
}
