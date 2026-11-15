package com.ethk3.novaonepro.strategyv2;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public final class R31FactoryStateTransportTest {
    @Test public void multiMegabyteOriginalIsLosslessAndFitsAnAndroidCursorCell(){
        StringBuilder data=new StringBuilder("{\"events\":[");for(int i=0;i<6000;i++){if(i>0)data.append(',');data.append("{\"kind\":\"TRANSPORT_FIXTURE_NOT_SCIENCE\",\"row\":").append(i).append(",\"text\":\"");for(int j=0;j<18;j++)data.append("完整谱系原件，不是功效结果；");data.append("\"}");}String raw=data.append("]}").toString();assertTrue(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>2*1024*1024);
        String cell=FactoryStateCodecR31.encode(raw);assertTrue(cell.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=FactoryStateCodecR31.MAX_CELL_BYTES);assertEquals(raw,FactoryStateCodecR31.decode(cell));
    }
    @Test @SuppressWarnings("unchecked") public void corruptionAndLyingInflateLengthAreRefused(){
        Map<String,Object> cell=(Map<String,Object>)JsonR27.parse(FactoryStateCodecR31.encode("{\"x\":1}"));cell.put("sha256",StatsR27.sha256("different"));assertThrows(IllegalArgumentException.class,()->FactoryStateCodecR31.decode(JsonR27.write(cell)));
        cell.put("rawBytes",1);assertThrows(IllegalArgumentException.class,()->FactoryStateCodecR31.decode(JsonR27.write(cell)));assertEquals("{\"legacy\":true}",FactoryStateCodecR31.decode("{\"legacy\":true}"));
    }
    @Test public void incompressibleStateIsRefusedBeforeTheWriter(){
        byte[] bytes=new byte[400000];new Random(37).nextBytes(bytes);String raw="{\"bytes\":\""+Base64.getEncoder().encodeToString(bytes)+"\"}";assertThrows(MemoryBudgetR31.CapacityWait.class,()->FactoryStateCodecR31.encode(raw));
    }
}
