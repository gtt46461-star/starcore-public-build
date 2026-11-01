package com.ethk3.novaonepro;

import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.ethk3.novaonepro.r952.ResearchChartViewR82;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Test APK only: capture the real window after layout, checked scrolling and frame submission. */
final class R30GalleryCaptureR30 {
    private R30GalleryCaptureR30() {}

    static final class Frame {
        final Bitmap bitmap;
        final Map<String,Object> evidence;
        final String pixelSha256;
        Frame(Bitmap bitmap,Map<String,Object> evidence,String hash) {
            this.bitmap=bitmap;this.evidence=evidence;this.pixelSha256=hash;
        }
    }

    private static final class Target {
        final LinearLayout list;
        final View card,root;
        final ScrollView scroll;
        final TextView title;
        final ResearchChartViewR82 chart;
        final String expectedTitle;
        Target(LinearLayout list,int index,String tag,String expectedTitle) {
            this.list=list;
            assertTrue("gallery content must belong to its real ScrollView",list.getParent() instanceof ScrollView);
            scroll=(ScrollView)list.getParent();root=scroll.getRootView();card=list.getChildAt(index);
            assertTrue("gallery target must be its actual card: "+index,card instanceof LinearLayout);
            if(tag!=null)assertEquals("target card identity",tag,card.getTag());
            LinearLayout children=(LinearLayout)card;
            assertTrue("card starts with its real title",children.getChildAt(0) instanceof TextView);
            title=(TextView)children.getChildAt(0);chart=findChart(card);
            this.expectedTitle=expectedTitle==null?title.getText().toString():expectedTitle;
            assertEquals("target title",this.expectedTitle,title.getText().toString());
        }
        int desiredScrollY() {
            int viewport=scroll.getHeight()-scroll.getPaddingTop()-scroll.getPaddingBottom();
            return Math.max(0,Math.min(card.getTop(),Math.max(0,list.getHeight()-viewport)));
        }
    }

    static ResearchChartViewR82 findChart(View card) {
        if(card instanceof ResearchChartViewR82)return (ResearchChartViewR82)card;
        if(card instanceof ViewGroup)for(int n=0;n<((ViewGroup)card).getChildCount();n++) {
            ResearchChartViewR82 found=findChart(((ViewGroup)card).getChildAt(n));
            if(found!=null)return found;
        }
        return null;
    }

    static void onMain(Instrumentation instrumentation,Runnable action) {
        AtomicReference<Throwable> failure=new AtomicReference<>();
        instrumentation.runOnMainSync(()->{try{action.run();}catch(Throwable error){failure.set(error);}});
        Throwable error=failure.get();
        if(error instanceof Error)throw (Error)error;
        if(error instanceof RuntimeException)throw (RuntimeException)error;
        if(error!=null)throw new AssertionError(error);
    }

    static Frame capture(Instrumentation instrumentation,LinearLayout list,int index,String tag,String expectedTitle)throws Exception {
        AtomicReference<Target> selected=new AtomicReference<>();
        onMain(instrumentation,()->selected.set(new Target(list,index,tag,expectedTitle)));
        Target target=selected.get();
        // A focusable chart can make ScrollView reveal the first chart after our scroll.
        // Keep focus on the container for this screenshot test; drawing and data stay unchanged.
        onMain(instrumentation,()->{
            target.scroll.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            target.scroll.setFocusableInTouchMode(true);target.scroll.requestFocus();
            target.scroll.setSmoothScrollingEnabled(false);
        });
        long deadline=SystemClock.elapsedRealtime()+10000;
        AtomicBoolean measured=new AtomicBoolean();
        do {
            instrumentation.waitForIdleSync();
            onMain(instrumentation,()->measured.set(target.card.isAttachedToWindow()&&target.card.isLaidOut()
                &&target.card.getWidth()>0&&target.card.getHeight()>0&&target.scroll.getHeight()>0
                &&!target.list.isLayoutRequested()&&!target.card.isLayoutRequested()&&!target.scroll.isLayoutRequested()));
            if(measured.get())break;
            SystemClock.sleep(50);
        }while(SystemClock.elapsedRealtime()<deadline);
        assertTrue("target layout must finish before scrolling: "+target.expectedTitle,measured.get());
        onMain(instrumentation,()->target.scroll.scrollTo(0,target.desiredScrollY()));
        String frameSync=awaitFrame(instrumentation,target.root);
        SystemClock.sleep(300);
        frameSync=awaitFrame(instrumentation,target.root);
        Map<String,Object> before=geometry(instrumentation,target);
        Bitmap bitmap=instrumentation.getUiAutomation().takeScreenshot();
        assertNotNull("actual display screenshot: "+target.expectedTitle,bitmap);
        try {
            Map<String,Object> after=geometry(instrumentation,target);
            assertEquals("target geometry must remain stable across screen capture",before,after);
            String hash=pixelHash(bitmap);
            after.put("frameSync",frameSync);after.put("settleMs",300);
            after.put("pixelSha256",hash);after.put("screenshotWidth",bitmap.getWidth());after.put("screenshotHeight",bitmap.getHeight());
            return new Frame(bitmap,after,hash);
        }catch(Exception|Error failure){bitmap.recycle();throw failure;}
    }

    private static String awaitFrame(Instrumentation instrumentation,View root)throws InterruptedException {
        CountDownLatch committed=new CountDownLatch(1);AtomicBoolean hardware=new AtomicBoolean();
        onMain(instrumentation,()->{
            hardware.set(Build.VERSION.SDK_INT>=29&&root.isHardwareAccelerated());
            if(hardware.get())root.getViewTreeObserver().registerFrameCommitCallback(committed::countDown);
            else Choreographer.getInstance().postFrameCallback(first->{
                root.invalidate();Choreographer.getInstance().postFrameCallback(second->committed.countDown());
            });
            root.invalidate();
        });
        assertTrue("real window frame must be submitted before screenshot",committed.await(5,TimeUnit.SECONDS));
        return hardware.get()?"API29_FRAME_COMMIT":"CHOREOGRAPHER_TWO_FRAMES";
    }

    private static Map<String,Object> geometry(Instrumentation instrumentation,Target target) {
        AtomicReference<Map<String,Object>> snapshot=new AtomicReference<>();
        onMain(instrumentation,()->{
            assertTrue("real gallery window must have focus",target.root.hasWindowFocus());
            assertTrue("target card must remain in the actual content",target.card.getParent()==target.list);
            assertTrue("capture cannot precede layout",!target.list.isLayoutRequested()&&!target.card.isLayoutRequested());
            assertEquals("scroll must still reveal the requested card",target.desiredScrollY(),target.scroll.getScrollY());
            assertEquals("visible title must name the requested card",target.expectedTitle,target.title.getText().toString());
            Rect viewport=new Rect(),card=new Rect(),title=new Rect(),chart=new Rect();
            assertTrue("real ScrollView must be visible",target.scroll.getGlobalVisibleRect(viewport));
            assertTrue("requested card must be visibly clipped into the viewport",target.card.getGlobalVisibleRect(card)&&Rect.intersects(viewport,card));
            assertTrue("requested Chinese/Rxx title must be visible",target.title.getGlobalVisibleRect(title)&&viewport.contains(title));
            assertEquals("title cannot be cropped out of its screenshot",target.title.getHeight(),title.height());
            if(target.chart!=null) {
                assertTrue("requested chart must be visible",target.chart.getGlobalVisibleRect(chart)&&Rect.intersects(viewport,chart));
                int minimum=Math.min(target.chart.getHeight(),Math.max(1,viewport.height()/5));
                assertTrue("visible chart area must be meaningful",chart.height()>=minimum);
            }
            Map<String,Object> values=new LinkedHashMap<>();
            values.put("title",target.expectedTitle);values.put("scrollY",target.scroll.getScrollY());
            values.put("cardTop",target.card.getTop());values.put("cardHeight",target.card.getHeight());
            values.put("viewport",rect(viewport));values.put("visibleCard",rect(card));values.put("visibleTitle",rect(title));
            values.put("visibleChart",target.chart==null?null:rect(chart));snapshot.set(values);
        });
        return snapshot.get();
    }

    private static java.util.List<Integer> rect(Rect value) {
        return Arrays.asList(value.left,value.top,value.right,value.bottom);
    }

    private static String pixelHash(Bitmap bitmap)throws Exception {
        ByteBuffer pixels=ByteBuffer.allocate(bitmap.getByteCount());bitmap.copyPixelsToBuffer(pixels);
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(pixels.array());StringBuilder hash=new StringBuilder();
        for(byte value:digest)hash.append(String.format(java.util.Locale.ROOT,"%02x",value&255));
        return hash.toString();
    }
}
