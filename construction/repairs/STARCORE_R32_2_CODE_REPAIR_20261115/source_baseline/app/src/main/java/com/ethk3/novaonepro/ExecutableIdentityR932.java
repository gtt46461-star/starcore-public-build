package com.ethk3.novaonepro;

import java.util.*;

/** Scheduling identity only. Existing genome IDs, lineage and statistical hypotheses are retained. */
public final class ExecutableIdentityR932 {
    private ExecutableIdentityR932(){}
    public static String semantic(AlgorithmGenomeR908 g){return identity(g,false);}
    public static String topology(AlgorithmGenomeR908 g){return identity(g,true);}
    private static String identity(AlgorithmGenomeR908 g,boolean parametersAbstract){
        if(g==null)throw new IllegalArgumentException("R932_EXECUTABLE_REQUIRED");
        String[] local=slots("S",AlgorithmGenomeR908.STATE_SLOTS),global=slots("G",AlgorithmGenomeR908.GLOBAL_STATE_SLOTS),peer=slots("PEER",AlgorithmGenomeR908.STATE_SLOTS);
        String[] score=run(g.scoreProgram,local.clone(),global.clone(),peer,false,parametersAbstract);
        String result=score[g.scoreOutputReg];
        run(g.updateProgram,local,global,peer,true,parametersAbstract);
        return hash("R932_VM_SEMANTICS_CLAMP16_STATE8_V1|"+result+'|'+Arrays.toString(local)+'|'+Arrays.toString(global));
    }
    private static String[] run(List<AlgorithmGenomeR908.Insn> program,String[] local,String[] global,String[] peer,boolean update,boolean abstractK){
        String[] r=new String[AlgorithmGenomeR908.REGS];Arrays.fill(r,"ZERO");
        for(AlgorithmGenomeR908.Insn i:program){
            String a=read(r,i.a),b=read(r,i.b),value,k=abstractK?"PARAM":Double.toHexString(i.k);
            switch(i.op){
                case LOAD_X:value=i.a>=0&&i.a<AlgorithmGenomeR908.INPUTS?"X"+i.a:"ZERO";break;
                case LOAD_S:value=read(local,i.a);break;
                case LOAD_G:value=read(global,i.a);break;
                case LOAD_PEER:value=read(peer,i.a);break;
                case CONST:value="CONST:"+(abstractK?"PARAM":Double.toHexString(Math.max(-16,Math.min(16,i.k))));break;
                case WRITE_S:case WRITE_G:
                    value=r[i.dst];String[] dest=i.op==AlgorithmGenomeR908.Op.WRITE_S?local:global;
                    if(update&&i.a>=0&&i.a<dest.length)dest[i.a]=hash("STATE8|"+value);
                    break;
                case ADD:case MUL:case MIN:case MAX:
                    if(a.compareTo(b)>0){String t=a;a=b;b=t;}
                    value=i.op.name()+'|'+a+'|'+b;break;
                case SUB:case SAFE_DIV:value=i.op.name()+'|'+a+'|'+b;break;
                case GT_SELECT:case MIX:value=i.op.name()+'|'+a+'|'+b+'|'+k;break;
                default:value=i.op.name()+'|'+a;break;
            }
            r[i.dst]=hash("FINITE_CLAMP16|"+value);
        }
        return r;
    }
    private static String[] slots(String prefix,int n){String[] r=new String[n];for(int i=0;i<n;i++)r[i]=prefix+i;return r;}
    private static String read(String[] a,int i){return i>=0&&i<a.length?a[i]:"ZERO";}
    private static String hash(String s){return V51ChecksumEngine.sha256(s);}
}
