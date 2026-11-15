package com.ethk3.novaonepro;

import com.ethk3.novaonepro.r952.ChartGeometryR82;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ChartGeometryR82Test {
    private static List<String> names(int count) {
        List<String> out=new ArrayList<>();
        for(int n=0;n<count;n++)out.add("node-"+n);
        return out;
    }

    @Test public void legacy64BudgetAndCardinalCoordinatesRemainUnchanged() {
        List<String> names=names(64);
        Map<String,float[]> original=ChartGeometryR82.graph(names,200,100);
        assertEquals(64,original.size());
        assertArrayEquals(new float[]{100,17},original.get("node-0"),.0001f);
        assertArrayEquals(new float[]{172,50},original.get("node-16"),.0001f);
        assertArrayEquals(new float[]{100,83},original.get("node-32"),.0001f);
        assertArrayEquals(new float[]{28,50},original.get("node-48"),.0001f);
        Map<String,float[]> research=ChartGeometryR82.researchGraph(names,200,100);
        for(String name:names)assertArrayEquals(original.get(name),research.get(name),0f);
        IllegalArgumentException failure=assertThrows(IllegalArgumentException.class,
            ()->ChartGeometryR82.graph(names(65),200,100));
        assertEquals("GRAPH_NODE_BUDGET",failure.getMessage());
    }

    @Test public void actualR03PositionPairsKeepAll100OriginalNodes() {
        List<String> names=new ArrayList<>();
        for(int position=1;position<=10;position++)for(int source=1;source<=10;source++)
            names.add("P"+source+"@1→P"+position);
        Map<String,float[]> graph=ChartGeometryR82.researchGraph(names,1080,1020);
        assertEquals(names,new ArrayList<>(graph.keySet()));
        assertEquals(100,graph.size());
        assertNotNull(graph.get("P10@1→P10"));
        for(float[] point:graph.values()) {
            assertTrue(Float.isFinite(point[0])&&point[0]>=0&&point[0]<=1080);
            assertTrue(Float.isFinite(point[1])&&point[1]>=0&&point[1]<=1020);
        }
    }

    @Test public void researchGraphAcceptsExactly120AndRejects121WithoutTruncation() {
        Map<String,float[]> graph=ChartGeometryR82.researchGraph(names(120),1080,1020);
        assertEquals(120,graph.size());assertNotNull(graph.get("node-119"));
        IllegalArgumentException failure=assertThrows(IllegalArgumentException.class,
            ()->ChartGeometryR82.researchGraph(names(121),1080,1020));
        assertEquals("GRAPH_NODE_BUDGET",failure.getMessage());
    }
}
