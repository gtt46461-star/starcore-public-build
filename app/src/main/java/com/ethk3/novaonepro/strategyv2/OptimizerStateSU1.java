package com.ethk3.novaonepro.strategyv2;

/** Immutable optimizer state. All moments and parameters are published together after a complete update. */
public final class OptimizerStateSU1 {
    public enum Kind { SGD_NO_MOMENTUM_V1, ADAM_BETA_09_0999_EPS_1E8_V1 }
    public final Kind kind;
    public final long steps;
    public final RelationalStateInference.Weights first,second;
    private OptimizerStateSU1(Kind kind,long steps,RelationalStateInference.Weights first,RelationalStateInference.Weights second){
        if(steps<0||steps>=100000000)throw new IllegalArgumentException("SU1_OPTIMIZER_STEP");
        if(kind==Kind.ADAM_BETA_09_0999_EPS_1E8_V1){
            if(first==null||second==null||first.d!=second.d||first.d>16)throw new IllegalArgumentException("SU1_ADAM_MOMENT_SHAPES");
            for(double[][] a:matrices(second))for(double[] row:a)for(double v:row)if(v<0)throw new IllegalArgumentException("SU1_ADAM_VARIANCE");
            for(double[] a:new double[][]{second.b,second.c})for(double v:a)if(v<0)throw new IllegalArgumentException("SU1_ADAM_VARIANCE");
        }else if(first!=null||second!=null)throw new IllegalArgumentException("SU1_SGD_HAS_NO_MOMENTS");
        this.kind=kind;this.steps=steps;this.first=first;this.second=second;
    }
    public static OptimizerStateSU1 sgd(long steps){return new OptimizerStateSU1(Kind.SGD_NO_MOMENTUM_V1,steps,null,null);}
    public static OptimizerStateSU1 adam(long steps,RelationalStateInference.Weights first,RelationalStateInference.Weights second){return new OptimizerStateSU1(Kind.ADAM_BETA_09_0999_EPS_1E8_V1,steps,first,second);}
    public static OptimizerStateSU1 adamInitial(int d){
        RelationalStateInference.Weights zero=new RelationalStateInference.Weights(new double[10][d],new double[10][d],new double[10][10],new double[d][d],new double[d][d],new double[d][d],new double[d],new double[d][10],new double[10]);
        return adam(0,zero,zero);
    }
    static double[][][] matrices(RelationalStateInference.Weights w){return new double[][][]{w.e,w.u,w.r,w.wx,w.wm,w.wh,w.wo};}
    private static RelationalStateInference.Weights assemble(double[][][] m,double[] b,double[] c){return new RelationalStateInference.Weights(m[0],m[1],m[2],m[3],m[4],m[5],b,m[6],c);}
    public static final class Update {
        public final RelationalStateInference.Weights weights;public final OptimizerStateSU1 state;
        Update(RelationalStateInference.Weights weights,OptimizerStateSU1 state){this.weights=weights;this.state=state;}
    }
    public Update apply(RelationalStateInference.Weights weights,RelationalTrainerSU1.Gradient gradient,double rate,double clip){
        if(!Double.isFinite(rate)||rate<=0||!Double.isFinite(clip)||clip<=0||steps>=99999999)throw new IllegalArgumentException("SU1_OPTIMIZER_UPDATE");
        if(kind==Kind.SGD_NO_MOMENTUM_V1)return new Update(RelationalTrainerSU1.update(weights,gradient,rate,clip),sgd(steps+1));
        if(weights.d!=first.d)throw new IllegalArgumentException("SU1_ADAM_PARAMETER_SHAPE");
        double scale=Math.min(1,clip/Math.max(1e-300,Math.sqrt(gradient.squaredNorm()))),b1=1-Math.pow(.9,steps+1),b2=1-Math.pow(.999,steps+1);
        double[][][] original=matrices(weights),m=matrices(first),v=matrices(second),g={gradient.e,gradient.u,gradient.r,gradient.wx,gradient.wm,gradient.wh,gradient.wo};
        double[][][] next=new double[7][][],nm=new double[7][][],nv=new double[7][][];
        for(int a=0;a<7;a++){
            next[a]=new double[original[a].length][];nm[a]=new double[original[a].length][];nv[a]=new double[original[a].length][];
            for(int row=0;row<original[a].length;row++){
                int n=original[a][row].length;next[a][row]=new double[n];nm[a][row]=new double[n];nv[a][row]=new double[n];
                for(int k=0;k<n;k++){double grad=g[a][row][k]*scale;nm[a][row][k]=.9*m[a][row][k]+.1*grad;nv[a][row][k]=.999*v[a][row][k]+.001*grad*grad;next[a][row][k]=original[a][row][k]-rate*(nm[a][row][k]/b1)/(Math.sqrt(nv[a][row][k]/b2)+1e-8);}
            }
        }
        double[][] ob={weights.b,weights.c},mb={first.b,first.c},vb={second.b,second.c},gb={gradient.b,gradient.c};
        double[][] nb=new double[2][],nmb=new double[2][],nvb=new double[2][];
        for(int a=0;a<2;a++){nb[a]=new double[ob[a].length];nmb[a]=new double[ob[a].length];nvb[a]=new double[ob[a].length];for(int k=0;k<ob[a].length;k++){double grad=gb[a][k]*scale;nmb[a][k]=.9*mb[a][k]+.1*grad;nvb[a][k]=.999*vb[a][k]+.001*grad*grad;nb[a][k]=ob[a][k]-rate*(nmb[a][k]/b1)/(Math.sqrt(nvb[a][k]/b2)+1e-8);}}
        return new Update(assemble(next,nb[0],nb[1]),adam(steps+1,assemble(nm,nmb[0],nmb[1]),assemble(nv,nvb[0],nvb[1])));
    }
}
