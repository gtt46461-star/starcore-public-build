import com.ethk3.novaonepro.CountdownProjectionR971;import java.util.Random;
public class ClockAnchorContractCP12 {
 static int n;static void yes(boolean b){n++;if(!b)throw new AssertionError("anchor"+n);}static void bad(Runnable r){n++;try{r.run();throw new AssertionError("reject"+n);}catch(IllegalArgumentException expected){}}
 public static void main(String[] args){
  String t="202609230001";var a=CountdownProjectionR971.Anchor.capture(t,7,"sessionA",1000000L,940000L,200000L);
  yes(a.remaining(t,7,"sessionA",200000L)==60000L);yes(a.remaining(t,7,"sessionA",230000L)==30000L);yes(a.remaining(t,7,"sessionA",260000L)==0L);yes(a.signedRemaining(t,7,"sessionA",261000L)==-1000L);
  yes(a.remaining(t,7,"otherboot",210000L)==Long.MIN_VALUE);yes(a.remaining(t,8,"sessionA",210000L)==Long.MIN_VALUE);yes(a.remaining("202609230002",7,"sessionA",210000L)==Long.MIN_VALUE);yes(a.remaining(t,7,"sessionA",199999L)==Long.MIN_VALUE);yes(a.remaining(t,7,"sessionA",380001L)==Long.MIN_VALUE);
  bad(()->CountdownProjectionR971.Anchor.capture(t,7,"s",1000000L,1L,1L));bad(()->CountdownProjectionR971.Anchor.capture(t,-1,"s",1000000L,940000L,1L));bad(()->CountdownProjectionR971.Anchor.capture(t,0,"",1000000L,940000L,1L));bad(()->CountdownProjectionR971.Anchor.received(t,0,"s",1L,Long.MIN_VALUE,Long.MAX_VALUE));
  var r=CountdownProjectionR971.Anchor.received(t,7,"sessionA",a.deadlineWallMs,a.deadlineMonoMs,a.sampledMonoMs);yes(r.remaining(t,7,"sessionA",230000L)==30000L);
  Random random=new Random(5908);for(int i=0;i<10000;i++){long mono=1000000L+random.nextInt(1000000),duration=random.nextInt(120001);var x=CountdownProjectionR971.Anchor.capture(t,1,"s",9000000L+duration,9000000L,mono);long later=mono+random.nextInt(180001);yes(x.remaining(t,1,"s",later)==Math.max(0,mono+duration-later));}
  System.out.println("{\"status\":\"PASS\",\"assertions\":"+n+",\"scope\":\"Pure clockanchor arithmetic, session/target/revision/overflow guards; noAndroidHandler/lifecycle claim\"}");
 }
}
