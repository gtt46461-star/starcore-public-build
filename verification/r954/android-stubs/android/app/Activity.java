package android.app;

import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

public class Activity extends Context implements ComponentCallbacks2 {
    protected void onCreate(Bundle b) {}
    protected void onStart() {}
    protected void onResume() {}
    protected void onPause() {}
    protected void onStop() {}
    protected void onDestroy() {}
    protected void onNewIntent(Intent i) {}
    protected void onActivityResult(int r, int c, Intent i) {}
    protected void onSaveInstanceState(Bundle b) {}
    public void onTrimMemory(int level) {}
    public static final int RESULT_OK = -1;
    private Intent intent = new Intent();
    public void setContentView(int id) {}
    public void setContentView(View v) {}
    public void addContentView(View v, ViewGroup.LayoutParams params) {}
    public Window getWindow() { return new Window(); }
    public Intent getIntent() { return intent; }
    public void setIntent(Intent i) { intent = i; }
    public void startActivity(Intent i) {}
    public void startActivityForResult(Intent i, int r) {}
    public int checkSelfPermission(String p) { return 0; }
    public void requestPermissions(String[] p, int r) {}
    public android.content.res.Resources getResources() { return new android.content.res.Resources(); }
    public void recreate() {}
    public <T extends View> T findViewById(int id) { return null; }
    public void runOnUiThread(Runnable r) { if (r != null) r.run(); }
    public boolean isFinishing() { return false; }
    public boolean isDestroyed() { return false; }
    public void finish() {}
}
