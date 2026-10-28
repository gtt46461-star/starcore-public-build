package com.ethk3.novaonepro;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.OperationCanceledException;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.ethk3.novaonepro.r952.ResearchChartViewR82;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static com.ethk3.novaonepro.ResearchVisualDataR82.*;

/** 走势图表: K-line, trend, calibration, algorithm, iteration and learning charts on one page. */
// Native programmatic Chinese diagnostic UI: status lines join fixed labels with exact values.
@android.annotation.SuppressLint("SetTextI18n")
public final class ChartsActivityR83 extends Activity {
    private static final long READ_TIMEOUT_MS = 15000L;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private LinearLayout charts;
    private TextView status;
    private Future<?> future;
    private CancellationSignal pending;
    private long generation;
    private final int ink = Color.rgb(229, 239, 250), muted = Color.rgb(165, 184, 205);

    private int dp(float n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private TextView text(String s, int size, int color) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setPadding(dp(10), dp(6), dp(10), dp(6));
        return v;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(11, 23, 39));
        root.setPadding(dp(10), dp(8), dp(10), dp(4));
        TextView title = text("走势图表", 22, ink);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);
        root.addView(text("全部来自已提交的真实开奖、赛前冻结预测与结算记录；无数据的图显示原因，不补零。点图可看具体数值。", 13, muted));
        Button refresh = new Button(this);
        refresh.setText("刷新图表");
        refresh.setAllCaps(false);
        refresh.setOnClickListener(v -> load());
        root.addView(refresh);
        Button tools = new Button(this);
        tools.setText("R27 工具：导入历史CSV / 旧研究链开关 / 重试隔离任务");
        tools.setAllCaps(false);
        tools.setOnClickListener(v -> toolsR27());
        root.addView(tools);
        status = text("正在读取……", 13, muted);
        root.addView(status);
        ScrollView scroll = new ScrollView(this);
        charts = new LinearLayout(this);
        charts.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(charts);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    @Override
    protected void onStart() {
        super.onStart();
        load();
    }

    @Override
    protected void onStop() {
        generation++;
        if (pending != null) pending.cancel();
        if (future != null) future.cancel(true);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        ui.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void load() {
        if (pending != null) pending.cancel();
        if (future != null) future.cancel(true);
        final CancellationSignal cancel = new CancellationSignal();
        pending = cancel;
        final long epoch = ++generation;
        status.setText("正在后台读取图表数据……");
        final File file = getApplicationContext().getDatabasePath(AppConfig.DB_NAME);
        ui.postDelayed(cancel::cancel, READ_TIMEOUT_MS);
        future = worker.submit(() -> {
            List<Panel> panels = null;
            String error = "";
            long started = System.currentTimeMillis();
            try {
                if (!file.isFile()) throw new IllegalStateException("数据库尚未建立");
                try (SourceReadLeaseR82 lease = SourceReadLeaseR82.acquire(getApplicationContext(), file, cancel);
                     SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY)) {
                    panels = ChartsRepositoryR27.load(db, cancel);
                }
            } catch (OperationCanceledException canceled) {
                error = "读取超时或已取消，请点刷新重试。";
            } catch (Throwable failure) {
                error = "读取失败：" + DatabaseCoordinator.describeCauseChain(failure, 600);
            }
            final List<Panel> ready = panels;
            final String message = error;
            final long cost = System.currentTimeMillis() - started;
            if (ready != null) recordLoadR27(cost);
            ui.post(() -> {
                if (epoch != generation || isFinishing()) return;
                if (ready == null) {
                    status.setText(message);
                    return;
                }
                render(ready, cost);
            });
        });
    }

    private void render(List<Panel> panels, long cost) {
        charts.removeAllViews();
        int ready = 0;
        for (Panel panel : panels) {
            if ("READY".equals(panel.status)) ready++;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.rgb(19, 34, 53));
            bg.setCornerRadius(dp(12));
            card.setBackground(bg);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.bottomMargin = dp(10);
            TextView head = text(panel.title, 16, ink);
            head.setTypeface(null, Typeface.BOLD);
            card.addView(head);
            card.addView(text(panel.window + (panel.status.equals("READY") ? "" : "｜" + statusText(panel.status)), 12, muted));
            if (!"STATUS".equals(panel.kind)) card.addView(new ResearchChartViewR82(this, panel));
            card.addView(text("怎么看：" + panel.note, 12, muted));
            charts.addView(card, params);
        }
        status.setText("共" + panels.size() + "张图，" + ready + "张已有数据；读取耗时" + cost + "ms。数据随运行持续积累。");
    }

    private static final int IMPORT_CSV_R27 = 27018;
    private void toolsR27() {
        worker.submit(() -> {
            DbStore store = new DbStore(getApplicationContext());
            String legacy = store.getState(ResearchLearningContinuityR912.R27_LEGACY_SWITCH, "1");
            ui.post(() -> new android.app.AlertDialog.Builder(this).setTitle("R27 工具")
                    .setItems(new String[] {"导入历史CSV（与库内同期逐期比对，冲突不导入）", "旧R9xx研究链并行：" + ("0".equals(legacy) ? "已关闭（点此重新开启）" : "开启中（并行观察≥1440期后再关闭）"), "重试全部已隔离（QUARANTINED）的研究任务"}, (d, which) -> {
                        if (which == 0) { android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT); i.addCategory(android.content.Intent.CATEGORY_OPENABLE); i.setType("*/*"); startActivityForResult(i, IMPORT_CSV_R27); }
                        else if (which == 1) worker.submit(() -> { store.setState(ResearchLearningContinuityR912.R27_LEGACY_SWITCH, "0".equals(legacy) ? "1" : "0"); ui.post(() -> status.setText("旧研究链开关已改为 " + ("0".equals(legacy) ? "开启" : "关闭") + "；旧表保留只读。")); });
                        else worker.submit(() -> { int n = com.ethk3.novaonepro.strategyv2.HistoryImportR27.retryQuarantined(store); ui.post(() -> status.setText("已重置隔离任务：" + n + "个")); });
                    }).show());
        });
    }

    @Override
    protected void onActivityResult(int request, int result, android.content.Intent data) {
        super.onActivityResult(request, result, data);
        if (request != IMPORT_CSV_R27 || result != RESULT_OK || data == null || data.getData() == null) return;
        final android.net.Uri uri = data.getData();
        status.setText("正在比对并导入历史CSV……");
        worker.submit(() -> {
            String message;
            try (java.io.InputStream in = getContentResolver().openInputStream(uri)) {
                message = com.ethk3.novaonepro.strategyv2.HistoryImportR27.importCsv(new DbStore(getApplicationContext()), in);
            } catch (Exception e) { message = "导入失败：" + e.getClass().getSimpleName() + ":" + e.getMessage(); }
            final String m = message; ui.post(() -> status.setText(m));
        });
    }

    // R27-053 UI chapter: chart first-screen load times (P95 target ≤ 300 ms when reading the R27 cache).
    private static final java.util.ArrayDeque<Long> LOAD_MS_R27 = new java.util.ArrayDeque<>();
    private void recordLoadR27(long cost) {
        String summary;
        synchronized (LOAD_MS_R27) {
            LOAD_MS_R27.addLast(cost); while (LOAD_MS_R27.size() > 50) LOAD_MS_R27.removeFirst();
            List<Long> sorted = new java.util.ArrayList<>(LOAD_MS_R27); java.util.Collections.sort(sorted);
            summary = "n=" + sorted.size() + ",p95Ms=" + sorted.get(Math.max(0, (int) Math.ceil(sorted.size() * 0.95) - 1)) + ",lastMs=" + cost + ",at=" + System.currentTimeMillis();
        }
        try { new DbStore(getApplicationContext()).setState("r27_ui_chart_load", summary); } catch (RuntimeException ignored) { /* telemetry only */ }
    }

    private static String statusText(String status) {
        if (status.startsWith("EMPTY_NEEDS_20")) return "需要至少20次已结算样本";
        if (status.startsWith("EMPTY_NEEDS_SETTLED_ACTIONS")) return "等待出手结算";
        if (status.startsWith("EMPTY_NEEDS_SETTLED_P")) return "等待带概率的已结算预测";
        if (status.startsWith("EMPTY")) return "暂无数据，随运行自动积累";
        if (status.equals("READ_ERROR")) return "读取失败";
        if (status.equals("INSUFFICIENT")) return "样本不足（INSUFFICIENT），不画假线";
        if (status.equals("EMPTY_ALLOWED_ZERO")) return "暂无主链参与（允许为0）";
        return status;
    }
}
