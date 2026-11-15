package com.ethk3.novaonepro;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;

/** Bounded file preview, without a large Binder payload or a database text round trip. */
public final class DiagnosisActivity extends Activity {
    private volatile boolean closed;
    private TextView text;
    private String smallBody = "";
    private File fullFile;
    private static boolean alive(Context context) {
        return !(context instanceof Activity) || (!((Activity) context).isFinishing() && !((Activity) context).isDestroyed());
    }
    private static Intent intent(Context context, String title) {
        Intent intent = new Intent(context, DiagnosisActivity.class);
        intent.putExtra("title", title == null ? "完整内容" : title.substring(0, Math.min(160, title.length())));
        if (!(context instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
    public static void openText(final Context context, final String title, final String body) {
        if (context == null || !alive(context)) return;
        final String exact = body == null ? "" : body;
        if (exact.length() <= DiagnosticFileStoreR941.PREVIEW_CHARS) {
            context.startActivity(intent(context, title).putExtra("body", exact)); return;
        }
        // Large derived detail is never sent through Binder or retained in a UI export worker.
        context.startActivity(intent(context, title).putExtra("requires_full_snapshot", true));
    }
    public static void openState(Context context, String title) {
        if (context != null && alive(context)) context.startActivity(intent(context, title));
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_diagnosis);
        text = (TextView) findViewById(R.id.txt_diagnosis_full);
        TextView title = (TextView) findViewById(R.id.txt_diagnosis_title);
        String requested = getIntent().getStringExtra("title"); title.setText(requested == null ? "完整诊断" : requested);
        Button copy = (Button) findViewById(R.id.btn_diagnosis_copy), all = (Button) findViewById(R.id.btn_diagnosis_copy_all);
        copy.setOnClickListener(v -> {
            if (fullFile != null) DiagnosticTextExportR923.sharePrepared(this, "导出完整详情", fullFile, fullFile.getName().endsWith(".zip")?"application/zip":"text/plain");
            else DiagnosticTextExportR923.copyOrExport(this, "完整详情", smallBody);
        });
        all.setOnClickListener(v -> CompleteReportFileR927.export(this, all, text));
        String body = getIntent().getStringExtra("body"), name = getIntent().getStringExtra("report_file");
        if (state != null && state.getString("r941_report_file") != null)
            name = state.getString("r941_report_file");
        // Keep bounded input available when Android recreates the Activity. Large legacy
        // input is removed immediately and replaced by a file reference after publication.
        if (body != null && body.length() > DiagnosticFileStoreR941.PREVIEW_CHARS)
            getIntent().removeExtra("body");
        if (body != null && body.length() <= DiagnosticFileStoreR941.PREVIEW_CHARS) { smallBody = body; text.setText(body); }
        else if (name != null) loadFile(name, null, copy);
        else { smallBody = "详情未通过大正文传递；完整原始数据由冻结检测报告交付。";
            text.setText(smallBody); CompleteReportFileR927.export(this, all, text); }
    }
    private void loadFile(final String name, final String legacyBody, final Button copy) {
        copy.setEnabled(false); text.setText("正在后台准备完整详情……");
        final Context app = getApplicationContext();
        new Thread(() -> {

            try {
                File directory = new File(app.getCacheDir(), "r923_reports");
                if (!directory.isDirectory() && !directory.mkdirs()) throw new java.io.IOException("REPORT_DIRECTORY");
                final File ready;
                if (name != null) ready = DiagnosticFileStoreR941.resolve(directory, name);
                else throw new java.io.IOException("DETAIL_NAME_REQUIRED");
                final String preview = DiagnosticFileStoreR941.preview(ready);
                runOnUiThread(() -> {
                    if (closed || isFinishing() || isDestroyed()) return;
                    fullFile = ready;
                    getIntent().putExtra("report_file", ready.getName()).removeExtra("body");
                    text.setText(preview); copy.setText(ready.getName().endsWith(".zip")?"导出完整无损报告ZIP":"导出完整详情TXT"); copy.setEnabled(true);
                });
            } catch (final Throwable error) {
                runOnUiThread(() -> {
                    if (!closed && !isFinishing()) text.setText("详情读取失败：" + DatabaseCoordinator.describeCauseChain(error, 1200));
                });
            } finally {
                // No database connection or complete-report writer exists in this Activity.
            }
        }, "R941-Detail-Preview").start();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        if (fullFile != null) state.putString("r941_report_file", fullFile.getName());
        super.onSaveInstanceState(state);
    }
    @Override protected void onDestroy() { closed = true; fullFile = null; smallBody = ""; super.onDestroy(); }
}
