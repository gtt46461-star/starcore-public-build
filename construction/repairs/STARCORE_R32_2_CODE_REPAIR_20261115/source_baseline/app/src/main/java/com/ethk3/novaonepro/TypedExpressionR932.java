package com.ethk3.novaonepro;

import java.util.*;

/** Finite, dimensionless, predraw expression bytecode with interval checks and common subexpressions. */
public final class TypedExpressionR932 {
    public static final String REVISION="R932_TYPED_PIT_EXPRESSION_CSE_V1";
    public static final int MAX_NODES=48,MAX_DEPTH=10;
    public static final String[] INPUTS=ResearchCompiledGraphRuntimeR906.MATRIX_SCHEMA.split("\\|");
    private TypedExpressionR932(){}
    public static final class Program {
        final List<Instruction> instructions;final int output;
        public final String hash,canonical;public final List<String> dependencies;public final double minimum,maximum;
        Program(List<Instruction> p,int o,String canonical){instructions=Collections.unmodifiableList(new ArrayList<Instruction>(p));output=o;this.canonical=canonical;hash=V51ChecksumEngine.sha256(REVISION+'|'+canonical);TreeSet<String> refs=new TreeSet<String>();for(Instruction i:p)if(i.feature>=0)refs.add(INPUTS[i.feature]);dependencies=Collections.unmodifiableList(new ArrayList<String>(refs));minimum=p.get(o).lo;maximum=p.get(o).hi;}
        public double evaluate(double[] input){
            if(input==null||input.length!=INPUTS.length)throw new IllegalArgumentException("R932_TYPED_INPUT_SHAPE");
            for(double v:input)if(!Double.isFinite(v))throw new IllegalArgumentException("R932_TYPED_INPUT_NONFINITE");
            double[] values=new double[instructions.size()];
            for(int j=0;j<values.length;j++){
                Instruction i=instructions.get(j);double a=i.a<0?0:values[i.a],b=i.b<0?0:values[i.b],c=i.c<0?0:values[i.c],v;
                switch(i.op){
                    case "CONST":v=i.constant;break;
                    case "FEATURE":v=clip(input[i.feature],i.lo,i.hi);break;
                    case "ADD":v=a+b;break;case "SUB":v=a-b;break;
                    case "MUL":case "INTERACTION":v=a*b;break;
                    case "MIN":v=Math.min(a,b);break;case "MAX":v=Math.max(a,b);break;
                    case "ABS":v=Math.abs(a);break;case "ONE_MINUS":v=1-a;break;
                    case "SIGMOID":v=1/(1+Math.exp(-clip(a,-35,35)));break;
                    case "CLIP":v=clip(a,b,c);break;
                    case "SAFE_DIV":v=a/(Math.abs(b)<c?(b<0?-c:c):b);break;
                    default:throw new IllegalStateException("R932_TYPED_OPCODE");
                }
                if(!Double.isFinite(v))throw new IllegalStateException("R932_TYPED_OUTPUT_NONFINITE");values[j]=v;
            }
            return values[output];
        }
    }
    static final class Instruction {
        final String op,key;final int a,b,c,feature;final double constant,lo,hi;
        Instruction(String op,String key,int a,int b,int c,int f,double k,double lo,double hi){this.op=op;this.key=key;this.a=a;this.b=b;this.c=c;feature=f;constant=k;this.lo=lo;this.hi=hi;}
    }
    public static Program compile(String expression){
        ResearchStrategyDslEngine.Node root=ResearchStrategyDslEngine.parse(expression);
        ArrayList<Instruction> program=new ArrayList<Instruction>();HashMap<String,Integer> common=new HashMap<String,Integer>();
        int output=compile(root,program,common,0);
        Instruction result=program.get(output);if(result.lo<0||result.hi>1)throw new IllegalArgumentException("R932_TYPED_SCORE_REQUIRES_BOUNDED_ROOT");
        return new Program(program,output,result.key);
    }
    private static int compile(ResearchStrategyDslEngine.Node n,List<Instruction> p,Map<String,Integer> common,int depth){
        if(depth>MAX_DEPTH||p.size()>=MAX_NODES)throw new IllegalArgumentException("R932_TYPED_RESOURCE_BOUND");
        String op=n.op;int a=-1,b=-1,c=-1,feature=-1;double k=0,lo=0,hi=0;String key;
        if(op.equals("FEATURE")){
            for(int i=0;i<INPUTS.length;i++)if(INPUTS[i].equals(n.value))feature=i;
            if(feature<0)throw new IllegalArgumentException("R932_TYPED_UNKNOWN_OR_NONPIT_INPUT:"+n.value);
            lo=feature==1||feature==2?-16:0;hi=feature==1||feature==2?16:1;key="FEATURE("+n.value+")";
        }else if(op.equals("CONST")){
            k=Double.parseDouble(n.value);if(!Double.isFinite(k)||Math.abs(k)>16)throw new IllegalArgumentException("R932_TYPED_CONSTANT_RANGE");lo=hi=k;key="CONST("+Double.toString(k)+")";
        }else{
            if(!Arrays.asList("ADD","SUB","MUL","INTERACTION","MIN","MAX","ABS","ONE_MINUS","SIGMOID","CLIP","SAFE_DIV").contains(op))throw new IllegalArgumentException("R932_TYPED_UNSUPPORTED_OR_STATEFUL_OPERATOR:"+op);
            a=compile(n.children.get(0),p,common,depth+1);if(n.children.size()>1)b=compile(n.children.get(1),p,common,depth+1);if(n.children.size()>2)c=compile(n.children.get(2),p,common,depth+1);
            if(b>=0&&Arrays.asList("ADD","MUL","INTERACTION","MIN","MAX").contains(op)&&p.get(a).key.compareTo(p.get(b).key)>0){int t=a;a=b;b=t;}
            Instruction x=p.get(a),y=b<0?null:p.get(b),z=c<0?null:p.get(c);
            switch(op){
                case "ADD":lo=x.lo+y.lo;hi=x.hi+y.hi;break;
                case "SUB":lo=x.lo-y.hi;hi=x.hi-y.lo;break;
                case "MUL":case "INTERACTION":double[] endpoints={x.lo*y.lo,x.lo*y.hi,x.hi*y.lo,x.hi*y.hi};lo=hi=endpoints[0];for(double v:endpoints){lo=Math.min(lo,v);hi=Math.max(hi,v);}break;
                case "MIN":lo=Math.min(x.lo,y.lo);hi=Math.min(x.hi,y.hi);break;
                case "MAX":lo=Math.max(x.lo,y.lo);hi=Math.max(x.hi,y.hi);break;
                case "ABS":lo=x.lo<=0&&x.hi>=0?0:Math.min(Math.abs(x.lo),Math.abs(x.hi));hi=Math.max(Math.abs(x.lo),Math.abs(x.hi));break;
                case "ONE_MINUS":lo=1-x.hi;hi=1-x.lo;break;
                case "SIGMOID":lo=0;hi=1;break;
                case "CLIP":if(!y.op.equals("CONST")||!z.op.equals("CONST")||y.constant>z.constant)throw new IllegalArgumentException("R932_TYPED_CLIP_BOUNDS");lo=y.constant;hi=z.constant;break;
                case "SAFE_DIV":if(!z.op.equals("CONST")||z.constant<1e-6)throw new IllegalArgumentException("R932_TYPED_DIVISION_BOUND");hi=Math.max(Math.abs(x.lo),Math.abs(x.hi))/z.constant;lo=-hi;break;
                default:throw new IllegalArgumentException("R932_TYPED_OPERATION");
            }
            key=op+'('+x.key+(y==null?"":","+y.key)+(z==null?"":","+z.key)+')';
        }
        if(!Double.isFinite(lo)||!Double.isFinite(hi)||Math.max(Math.abs(lo),Math.abs(hi))>1e8)throw new IllegalArgumentException("R932_TYPED_UNBOUNDED_INTERMEDIATE");
        Integer prior=common.get(key);if(prior!=null)return prior;
        int index=p.size();p.add(new Instruction(op,key,a,b,c,feature,k,lo,hi));common.put(key,index);return index;
    }
    private static double clip(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
}
