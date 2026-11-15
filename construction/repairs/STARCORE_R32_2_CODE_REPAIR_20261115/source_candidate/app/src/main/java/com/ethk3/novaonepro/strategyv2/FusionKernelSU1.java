package com.ethk3.novaonepro.strategyv2;

/** Frozen parent score graphs; a predictable gate and two temperatures are jointly fitted on past labels. */
public final class FusionKernelSU1 {
    public static final String PROTOCOL="SU1_FUSION_GATE_TEMPERATURE_V1|CAUSAL_BLOCK50|32_SAMPLES_2_EPOCHS|SGD_003|L2_1E4|FROZEN_PARENTS";
    private FusionKernelSU1() {}
    public static final class Model {final double[] parameters;Model(double[] parameters){this.parameters=parameters.clone();}}
    private static double sigmoid(double x){return 1/(1+Math.exp(-Math.max(-30,Math.min(30,x))));}
    private static double entropy(double[] p){double e=0;for(double v:p)if(v>0)e-=v*Math.log(v);return e/Math.log(10);}
    private static double[] probability(double[] score,double temperature){double[] x=new double[10];for(int n=0;n<10;n++)x[n]=score[n]*temperature;return RelationalTrainerSU1.softmax(x);}
    private static double mass(double[] p){double a=0,b=0,c=0;for(double v:p){if(v>a){c=b;b=a;a=v;}else if(v>b){c=b;b=v;}else if(v>c)c=v;}return a+b+c;}
    private static double[] features(double[] a,double[] b){return new double[]{1,mass(a)-mass(b),entropy(a)-entropy(b)};}
    private static double[] featureTemperatureDerivative(double[] probability,double[] scores,double temperature,double mean){
        Integer[] order={0,1,2,3,4,5,6,7,8,9};java.util.Arrays.sort(order,(a,b)->{int compare=Double.compare(probability[b],probability[a]);return compare==0?Integer.compare(a,b):compare;});
        double mass=0,entropy=0;
        for(int n=0;n<10;n++){double dp=probability[n]*temperature*(scores[n]-mean);if(n==order[0]||n==order[1]||n==order[2])mass+=dp;entropy-=dp*(Math.log(Math.max(1e-300,probability[n]))+1)/Math.log(10);}
        return new double[]{mass,entropy};
    }
    public static Model train(ProgramTreeSU1.Node left,ProgramTreeSU1.Node right,ProgramTreeSU1.Context c,int window){
        int boundary=c.t/50*50,first=Math.max(1,boundary-Math.min(c.fidelity.fusionExamples,window));double[] theta=new double[5];
        for(int epoch=0;epoch<c.fidelity.fusionEpochs;epoch++)for(int u=first;u<boundary;u++){
            c.budget.checkpointOrYield();ProgramTreeSU1.Context prior=c.atTarget(u);
            double[][] a=left.score(prior),b=right.score(prior);double[] gradient=new double[5];
            double ta=Math.exp(theta[3]),tb=Math.exp(theta[4]);
            for(int p=0;p<10;p++){
                double[] pa=probability(a[p],ta),pb=probability(b[p],tb),x=features(pa,pb);
                double gate=sigmoid(theta[0]+theta[1]*x[1]+theta[2]*x[2]);int label=c.draws[u][p]-1;
                double q=Math.max(1e-12,gate*pa[label]+(1-gate)*pb[label]);
                double dg=-(pa[label]-pb[label])*gate*(1-gate)/q/10;
                for(int k=0;k<3;k++)gradient[k]+=dg*x[k];
                double meanA=0,meanB=0;for(int n=0;n<10;n++){meanA+=pa[n]*a[p][n];meanB+=pb[n]*b[p][n];}
                gradient[3]-=gate*pa[label]*ta*(a[p][label]-meanA)/q/10;
                gradient[4]-=(1-gate)*pb[label]*tb*(b[p][label]-meanB)/q/10;
                double[] derivativeA=featureTemperatureDerivative(pa,a[p],ta,meanA),derivativeB=featureTemperatureDerivative(pb,b[p],tb,meanB);
                gradient[3]+=dg*(theta[1]*derivativeA[0]+theta[2]*derivativeA[1]);
                gradient[4]-=dg*(theta[1]*derivativeB[0]+theta[2]*derivativeB[1]);
            }
            double norm=0;for(int k=0;k<5;k++){gradient[k]+=.0001*theta[k];norm+=gradient[k]*gradient[k];}
            double scale=.03*Math.min(1,1/Math.max(1e-300,Math.sqrt(norm)));
            for(int k=0;k<5;k++)theta[k]=Math.max(-5,Math.min(5,theta[k]-scale*gradient[k]));
        }
        return new Model(theta);
    }
    public static double[][] score(Model model,double[][] left,double[][] right){
        double[] t=model.parameters;double[][] out=new double[10][10];
        for(int p=0;p<10;p++){
            double[] a=probability(left[p],Math.exp(t[3])),b=probability(right[p],Math.exp(t[4])),x=features(a,b);
            double gate=sigmoid(t[0]+t[1]*x[1]+t[2]*x[2]);
            for(int n=0;n<10;n++)out[p][n]=ProgramTreeSU1.clean(Math.log(Math.max(1e-12,gate*a[n]+(1-gate)*b[n])));
        }
        return out;
    }
}
