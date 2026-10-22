package com.ethk3.novaonepro.strategyv2;
import java.util.*;

/** Typed subtree replacement, branching, grafting and fusion change the executed program. */
public final class GrammarFamilySU1 {
    private GrammarFamilySU1() {}
    public static String atom(Random r){switch(r.nextInt(5)){case 0:return "EXPERT("+r.nextInt(8)+")";case 1:return "XT("+r.nextInt(10)+")";case 2:return "NUMIS("+r.nextInt(10)+")";case 3:return AtomCatalogSU1.scoreAtoms().get(r.nextInt(168));default:return "REL("+new String[]{"SAME","PLUS1","MINUS1","MIRROR","SUM"}[r.nextInt(5)]+","+(1+r.nextInt(3))+","+r.nextInt(10)+","+(1+r.nextInt(9))+")";}}
    public static String condition(Random r,String guarded){
        if(guarded!=null&&r.nextDouble()<.35)return "HOTT("+new int[]{30,60,120}[r.nextInt(3)]+","+guarded+")";
        switch(r.nextInt(5)){case 0:return "ODD("+r.nextInt(10)+","+(1+r.nextInt(2))+")";case 1:return "BIG("+r.nextInt(10)+","+(1+r.nextInt(2))+")";case 2:return "HOT("+r.nextInt(8)+")";case 3:return AtomCatalogSU1.conditions().get(r.nextInt(150));default:return "GT("+r.nextInt(10)+",1,"+r.nextInt(10)+",2)";}
    }
    public static String random(Random r,int depth){if(depth<=1||r.nextDouble()<.35)return atom(r);if(r.nextDouble()<.25){String a=random(r,depth-1);return "IF("+condition(r,null)+","+a+","+random(r,depth-1)+")";}if(r.nextBoolean())return new String[]{"NEG","TANH","RANK"}[r.nextInt(3)]+"("+random(r,depth-1)+")";return new String[]{"ADD","SUB","MUL","MAX"}[r.nextInt(4)]+"("+random(r,depth-1)+","+random(r,depth-1)+")";}
    private static String bounded(String proposal,String fallback){try{ProgramTextSU1.Term t=ProgramTextSU1.parse(proposal);if(t.depth()<=5&&t.size()<=63)return t.toString();}catch(IllegalArgumentException invalid){/* counted by the caller if every retry fails */}return fallback;}
    public static String mutate(String text,Random r){
        ProgramTextSU1.Term root=ProgramTextSU1.parse(text);List<ProgramTextSU1.Term> scores=ProgramTextSU1.subtrees(root,false);ProgramTextSU1.Term selected=scores.get(r.nextInt(scores.size()));double u=r.nextDouble();String replacement;
        if(u<.35)replacement=random(r,2);else if(u<.55)replacement="IF("+condition(r,selected.toString())+","+selected+","+atom(r)+")";
        else if(u<.75)replacement=atom(r);else if(u<.90&&root.op.equals("IF"))replacement="IF("+condition(r,null)+","+root.args.get(1)+","+root.args.get(2)+")";
        else replacement=new String[]{"NEG","TANH","RANK"}[r.nextInt(3)]+"("+selected+")";
        try{return bounded(ProgramTextSU1.replace(root,selected,ProgramTextSU1.parse(replacement)).toString(),text);}catch(IllegalArgumentException invalid){return text;}
    }
    public static String crossover(String a,String b,Random r){ProgramTextSU1.Term root=ProgramTextSU1.parse(a),other=ProgramTextSU1.parse(b);List<ProgramTextSU1.Term> as=ProgramTextSU1.subtrees(root,false),bs=ProgramTextSU1.subtrees(other,false);return bounded(ProgramTextSU1.replace(root,as.get(r.nextInt(as.size())),bs.get(r.nextInt(bs.size()))).toString(),a);}
    public static String fuse(String a,String b,Random r){String proposal="ADD("+a+","+b+")";String result=bounded(proposal,"");return result.isEmpty()?crossover(a,b,r):result;}
    public static String seed(String family,Random r){
        switch(family){
            case "EXPERT_MIX":return "ADD(EXPERT("+r.nextInt(8)+"),NEG(EXPERT("+r.nextInt(8)+")))";
            case "RELATION":return "REL("+new String[]{"SAME","PLUS1","MINUS1","MIRROR","SUM"}[r.nextInt(5)]+","+(1+r.nextInt(3))+","+r.nextInt(10)+","+(1+r.nextInt(9))+")";
            case "XI_SELECTOR":return "XI("+r.nextInt(8)+","+r.nextInt(8)+","+r.nextInt(10)+")";
            case "DEEP":return "DEEP("+(2+r.nextInt(3))+",8,"+r.nextInt(1000000)+")";
            case "MATRIX":return "ADD(XT("+r.nextInt(10)+"),EXPERT("+r.nextInt(8)+"))";
            case "HEDGE":return "HEDGE(60,"+(500+r.nextInt(7501))+")";
            case "REGIME":return "IF(HOT("+r.nextInt(8)+"),EXPERT("+r.nextInt(8)+"),XT("+r.nextInt(10)+"))";
            default:return random(r,3);
        }
    }
}
