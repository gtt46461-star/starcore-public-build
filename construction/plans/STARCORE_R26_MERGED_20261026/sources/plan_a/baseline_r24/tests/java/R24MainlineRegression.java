package com.ethk3.novaonepro.strategyv2;
import java.util.*;
public final class R24MainlineRegression {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static MainlinePolicySU2.Member member(String id,int n,int per20,boolean weak,boolean incumbent){
        boolean[] a=new boolean[n],h=new boolean[n];int x=0;
        for(int t=0;t<n;t++){a[t]=t%20<per20;if(a[t]){h[t]=weak?x%10==0:x%2==0;x++;}}
        return new MainlinePolicySU2.Member(id,id,incumbent,a,h,0);
    }
    public static void main(String[] args){
        MainlinePolicySU2.Member c=member("C",1500,3,false,false),xi=member("XI",1500,3,false,true);
        boolean original=args.length>0&&args[0].equals("original");
        if(original){check(MainlinePolicySU2.demotable(c,xi,1500),"original defect must reproduce");System.out.println("REPRODUCED: 15% healthy policy marked demotable with 225 actions");return;}
        check(!MainlinePolicySU2.demotable(c,xi,1500),"15% sufficient coverage but immature sample not degradation");
        check(MainlinePolicySU2.demotable(member("W",2500,5,true,false),member("XI",2500,5,false,true),2500),"weak mature policy still demotes");
        check(MainlinePolicySU2.demotable(member("L",2000,2,false,false),member("XI",2000,3,false,true),2000),"sustained inadequate coverage still reported");
        check(!MainlinePolicySU2.demotable(member("N",100,3,false,false),member("XI",100,3,false,true),100),"immature history not degraded");
        check(!MainlinePolicySU2.eligible(c,1500,1500),"no promotion without sample evidence");
        check(MainlinePolicySU2.DEMOTE_WINDOW*MainlinePolicySU2.MIN_COVERAGE>=MainlinePolicySU2.MIN_ACTS,"window permits requested frequency");
        System.out.println("R24_PATCH_HOST_CHECKS="+checks+" PASS; Android integration NOT RUN");
    }
}
