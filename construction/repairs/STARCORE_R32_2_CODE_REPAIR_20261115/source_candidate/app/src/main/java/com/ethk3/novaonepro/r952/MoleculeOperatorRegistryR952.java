package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.Operator;

public final class MoleculeOperatorRegistryR952 {
    private MoleculeOperatorRegistryR952(){}
    public static boolean allowed(String op){try{resolve(op);return true;}catch(IllegalArgumentException bad){return false;}}
    public static Operator resolve(String op){
        if(op==null)throw new IllegalArgumentException("R952_MOLECULE_OPERATOR");
        if("DELTA".equals(op))return Operator.SUBTRACT;
        if("RATIO_SAFE".equals(op))return Operator.SAFE_DIVIDE;
        if("CLAMP".equals(op))return Operator.CLIP;
        return Operator.valueOf(op);
    }
}
