package com.ethk3.novaonepro;
import java.nio.file.*;import java.util.*;import org.json.*;
/** Fixed exploratory protocol over report data. No production authority or old-35% replication claim. */
public final class CrossPositionReplayR78 {
 static int[] top(double[] q){Integer[] ids={0,1,2,3,4,5,6,7,8,9};Arrays.sort(ids,(a,b)->{int z=Double.compare(q[b],q[a]);return z==0?Integer.compare(a,b):z;});return new int[]{ids[0]+1,ids[1]+1,ids[2]+1};}
 static boolean hit(int[] selected,int actual){for(int x:selected)if(x==actual)return true;return false;}
 public static void main(String[] a)throws Exception{
  JSONArray input=new JSONArray(Files.readString(Path.of(a[0])));int n=input.length();int[][] draws=new int[n][10];String[] issues=new String[n];
  for(int i=0;i<n;i++){JSONObject r=input.getJSONObject(i);issues[i]=r.getString("issue");for(int p=0;p<10;p++)draws[i][p]=r.getInt("n"+(p+1));}
  int[] windows={20,120,500},hits=new int[3],streak=new int[3],longest=new int[3];int[][][] paired=new int[3][3][2];List<String> forecasts=new ArrayList<>();long start=System.nanoTime();
  for(int t=n-1200;t<n;t++){
   double[][][] experts=new double[3][10][10];double[][] mean=new double[10][10];
   for(int e=0;e<3;e++)for(int p=0;p<10;p++){for(int d=0;d<10;d++)experts[e][p][d]=1;for(int i=t-windows[e];i<t;i++)experts[e][p][draws[i][p]-1]++;for(int d=0;d<10;d++){experts[e][p][d]/=windows[e]+10.0;mean[p][d]+=experts[e][p][d]/3;}}
   double[] votes=new double[10];for(int d=0;d<10;d++)votes[d]=CrossPositionAssignmentResearchEngine.locationVote(experts,null,1,d+1,3).score+mean[0][d]*0.001;
   double[][] support=CrossPositionAssignmentResearchEngine.kBestAssignmentSupport(mean,8,1).support;
   int[][] predictions={top(mean[0]),top(votes),top(support[0])};
   // Store each complete selection at source t-1 before reading the target label.
   JSONObject record=new JSONObject();record.put("source",issues[t-1]);record.put("target",issues[t]);record.put("k3",new JSONArray(Arrays.asList(predictions[0],predictions[1],predictions[2])));forecasts.add(record.toString());
   boolean[] labels=new boolean[3];for(int j=0;j<3;j++){labels[j]=hit(predictions[j],draws[t][0]);if(labels[j]){hits[j]++;streak[j]=0;}else longest[j]=Math.max(longest[j],++streak[j]);}
   for(int j=0;j<3;j++)for(int k=0;k<j;k++)if(labels[j]!=labels[k])paired[j][k][labels[j]?0:1]++;
  }
  JSONObject out=new JSONObject();out.put("scope","ACTUAL_COMPONENT_EXPLORATORY_HISTORICAL_REPLAY_NOT_OLD_35_PERCENT_REPLICATION_OR_FUTURE");out.put("n",1200);out.put("position",1);out.put("k",3);out.put("windows",new JSONArray(windows));out.put("assignment_paths",8);out.put("temperature",1);out.put("dirichlet_alpha",1);out.put("first",issues[n-1200]);out.put("last",issues[n-1]);
  JSONArray strategies=new JSONArray();String[] names={"equal_frequency","top3_intersection_votes","legal_assignment_8"};for(int j=0;j<3;j++){JSONObject s=new JSONObject();s.put("name",names[j]);s.put("hits",hits[j]);s.put("hit_rate",hits[j]/1200.0);s.put("max_miss",longest[j]);strategies.put(s);}out.put("strategies",strategies);out.put("paired_win_loss_against_frequency",new JSONArray(Arrays.asList(paired[1][0],paired[2][0])));out.put("elapsed_ms",(System.nanoTime()-start)/1000000);out.put("promotion","NOT_GRANTED_EXPLORATORY_COMPONENT_ONLY");Files.writeString(Path.of(a[1]),out.toString(2));Files.write(Path.of(a[2]),forecasts);System.out.println(out.toString(2));
 }
}
