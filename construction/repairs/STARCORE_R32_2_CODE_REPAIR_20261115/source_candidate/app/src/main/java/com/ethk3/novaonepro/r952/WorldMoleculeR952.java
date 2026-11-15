package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.V51ChecksumEngine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WorldMoleculeR952 {
    public final String moleculeId,operator,structuralHash,semanticHash;
    public final List<String> inputIds; public final int depth,cost;
    private final double[] parameters;
    public WorldMoleculeR952(String id,String op,List<String> inputs,int d,int c,String semantic){
        this(id,op,inputs,d,c,semantic,new double[0]);
    }
    public WorldMoleculeR952(String id,String op,List<String> inputs,int d,int c,String semantic,double[] values){
        moleculeId=req(id);operator=req(op);if(inputs==null||inputs.isEmpty())throw new IllegalArgumentException("R952_MOLECULE_INPUTS");
        ArrayList<String> copy=new ArrayList<String>();for(String x:inputs)copy.add(req(x));inputIds=Collections.unmodifiableList(copy);depth=d;cost=c;semanticHash=req(semantic);
        if(values==null)throw new IllegalArgumentException("R958_MOLECULE_PARAMETERS");parameters=values.clone();
        for(double value:parameters)if(!Double.isFinite(value))throw new IllegalArgumentException("R958_MOLECULE_PARAMETER_NONFINITE");
        structuralHash=V51ChecksumEngine.sha256(canonicalStructure());
    }
    public double[] parameters(){return parameters.clone();}
    public String parameterEncoding(){StringBuilder b=new StringBuilder();for(double v:parameters){if(b.length()>0)b.append(',');b.append(Double.toHexString(v));}return b.toString();}
    public String canonicalStructure(){return "MOLECULE2|"+operator+'|'+join(inputIds)+'|'+parameterEncoding();}
    static String join(List<String> v){StringBuilder b=new StringBuilder();for(String x:v){if(b.length()>0)b.append(',');b.append(x);}return b.toString();}
    static String req(String v){String s=v==null?"":v.trim();if(s.length()==0||s.indexOf('|')>=0||s.indexOf(',')>=0||s.indexOf('\n')>=0||s.indexOf('\r')>=0)throw new IllegalArgumentException("R952_MOLECULE_FIELD");return s;}
}
