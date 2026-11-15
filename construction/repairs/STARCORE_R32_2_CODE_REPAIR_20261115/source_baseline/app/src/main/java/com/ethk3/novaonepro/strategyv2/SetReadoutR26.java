package com.ethk3.novaonepro.strategyv2;
/** Separates legal marginal mass from learned action utility. Positions/numbers are zero-based. */
public interface SetReadoutR26 {
    final class Action {
        public final int position,a,b,c;public final double probability,utility;
        public Action(int p,int a,int b,int c,double probability,double utility){if(p<0||p>9||a<0||a>=b||b>=c||c>9||!Double.isFinite(probability)||probability<0||probability>1.000001||!Double.isFinite(utility))throw new IllegalArgumentException("R26_ACTION_TYPE");position=p;this.a=a;this.b=b;this.c=c;this.probability=probability;this.utility=utility;}
    }
    Action choose(float[][] legalMarginals);
}
