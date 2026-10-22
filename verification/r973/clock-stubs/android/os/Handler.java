package android.os;
import java.util.ArrayList;
public class Handler {
 public static Handler latest; public final Looper owner; private final ArrayList<Runnable> q=new ArrayList<Runnable>();
 public Handler(Looper l){owner=l;latest=this;}
 public boolean post(Runnable r){q.add(r);return true;}
 public boolean postDelayed(Runnable r,long delay){q.add(r);return true;}
 public void removeCallbacks(Runnable r){while(q.remove(r)){} }
 public int pending(){return q.size();}
 public void runNext(){if(q.isEmpty())throw new AssertionError("QUEUE_EMPTY");q.remove(0).run();}
}
