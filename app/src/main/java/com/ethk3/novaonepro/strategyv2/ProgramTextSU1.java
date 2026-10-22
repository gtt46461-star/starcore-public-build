package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Bounded, typed syntax tree shared by compilation, evolution, deduplication and macro mining. */
public final class ProgramTextSU1 {
    private ProgramTextSU1() {}
    public static final class Term {
        public final String op;
        public final List<Object> args;
        Term(String op,List<Object> args){this.op=op;this.args=Collections.unmodifiableList(new ArrayList<>(args));}
        public String toString(){StringBuilder s=new StringBuilder(op).append('(');for(int i=0;i<args.size();i++){if(i>0)s.append(',');s.append(args.get(i));}return s.append(')').toString();}
        public int size(){int n=1;for(Object a:args)if(a instanceof Term)n+=((Term)a).size();return n;}
        public int depth(){int d=0;for(Object a:args)if(a instanceof Term)d=Math.max(d,((Term)a).depth());return d+1;}
        public boolean condition(){return Arrays.asList("GT","ODD","BIG","HOT","HOTT","ATOMC").contains(op);}
    }
    public static Term parse(String text){
        if(text==null||text.length()<3||text.length()>4096)throw new IllegalArgumentException("SU1_PROGRAM_SIZE");
        Reader r=new Reader(text.trim());Object value=r.item(0);
        if(r.i!=r.s.length()||!(value instanceof Term))throw new IllegalArgumentException("SU1_PROGRAM_SYNTAX");
        Term t=(Term)value;validate(t,false);if(cost(t)>1000000)throw new IllegalArgumentException("SU1_PROGRAM_COST");return t;
    }
    public static String canonical(String text){return parse(text).toString();}
    private static final class Reader {
        final String s;int i,count;
        Reader(String s){this.s=s;}
        Object item(int depth){
            if(depth>12||++count>255)throw new IllegalArgumentException("SU1_PROGRAM_COMPLEXITY");
            int from=i;while(i<s.length()&&s.charAt(i)!='('&&s.charAt(i)!=','&&s.charAt(i)!=')')i++;
            String token=s.substring(from,i);
            if(!token.matches("[A-Za-z0-9_=;.+-]+"))throw new IllegalArgumentException("SU1_PROGRAM_TOKEN");
            if(i==s.length()||s.charAt(i)!='(')return token;
            i++;List<Object> args=new ArrayList<>();
            if(i<s.length()&&s.charAt(i)!=')')while(true){args.add(item(depth+1));if(i<s.length()&&s.charAt(i)==','){i++;continue;}break;}
            if(i>=s.length()||s.charAt(i++)!=')')throw new IllegalArgumentException("SU1_PROGRAM_CLOSE");return new Term(token,args);
        }
    }
    private static void arity(Term t,int n){if(t.args.size()!=n)throw new IllegalArgumentException("SU1_ARITY:"+t.op);}
    private static int number(Term t,int at,int low,int high){Object a=t.args.get(at);if(!(a instanceof String))throw new IllegalArgumentException("SU1_LITERAL");int v=Integer.parseInt((String)a);if(v<low||v>high)throw new IllegalArgumentException("SU1_RANGE:"+t.op);return v;}
    private static void child(Term t,int at,boolean condition){Object a=t.args.get(at);if(!(a instanceof Term))throw new IllegalArgumentException("SU1_TYPE");validate((Term)a,condition);}
    private static void validate(Term t,boolean condition){
        if(t.condition()!=condition)throw new IllegalArgumentException("SU1_SCORE_CONDITION_TYPE");
        switch(t.op){
            case "EXPERT":case "HOT":arity(t,1);number(t,0,0,7);break;
            case "XT":case "NUMIS":arity(t,1);number(t,0,0,9);break;
            case "XI":arity(t,3);number(t,0,0,7);number(t,1,0,7);number(t,2,0,9);break;
            case "HEDGE":arity(t,2);number(t,0,30,360);number(t,1,1,8000);break;
            case "DEEP":arity(t,3);number(t,0,2,8);number(t,1,8,32);number(t,2,0,2147483647);break;
            case "REL":arity(t,4);if(!Arrays.asList("SAME","PLUS1","MINUS1","MIRROR","SUM").contains(t.args.get(0)))throw new IllegalArgumentException("SU1_REL_KIND");number(t,1,1,5000);number(t,2,0,9);number(t,3,0,9);break;
            case "ODD":case "BIG":arity(t,2);number(t,0,0,9);number(t,1,1,5000);break;
            case "GT":arity(t,4);number(t,0,0,9);number(t,1,1,5000);number(t,2,0,9);number(t,3,1,5000);break;
            case "HOTT":arity(t,2);number(t,0,1,120);child(t,1,false);break;
            case "ATOM":arity(t,2);AtomCatalogSU1.requireScore(t.args.get(0).toString(),t.args.get(1).toString());break;
            case "ATOMC":arity(t,3);number(t,1,0,9);AtomCatalogSU1.requireCondition(t.args.get(0).toString(),t.args.get(2).toString());break;
            case "ADD":case "SUB":case "MUL":case "MAX":arity(t,2);child(t,0,false);child(t,1,false);break;
            case "NEG":case "TANH":case "RANK":arity(t,1);child(t,0,false);break;
            case "IF":arity(t,3);child(t,0,true);child(t,1,false);child(t,2,false);break;
            default:throw new IllegalArgumentException("SU1_UNKNOWN_OPERATOR:"+t.op);
        }
    }
    private static long cost(Term t){long c=t.op.equals("DEEP")?300000:1;for(Object a:t.args)if(a instanceof Term)c+=cost((Term)a);return t.op.equals("HOTT")?c*Integer.parseInt(t.args.get(0).toString())*10:c;}
    public static boolean constant(Term t){
        if(t.op.equals("NUMIS"))return true;
        if(t.op.equals("SUB")&&t.args.get(0).toString().equals(t.args.get(1).toString()))return true;
        if(Arrays.asList("NEG","TANH","RANK","ADD","SUB","MUL","MAX").contains(t.op)){for(Object a:t.args)if(a instanceof Term&&!constant((Term)a))return false;return true;}return false;
    }
    public static List<Term> subtrees(Term root,boolean conditions){List<Term> out=new ArrayList<>();walk(root,conditions,out);return out;}
    private static void walk(Term t,boolean cond,List<Term> out){if(t.condition()==cond)out.add(t);for(Object a:t.args)if(a instanceof Term)walk((Term)a,cond,out);}
    public static Term replace(Term root,Term selected,Term replacement){if(root==selected)return replacement;List<Object> out=new ArrayList<>();for(Object a:root.args)out.add(a instanceof Term?replace((Term)a,selected,replacement):a);return new Term(root.op,out);}
}
