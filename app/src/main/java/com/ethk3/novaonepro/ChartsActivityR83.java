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
                    panels = ChartsRepositoryR83.load(db, cancel);
                }
            } catch (OperationCanceledException canceled) {
                error = "读取超时或已取消，请点刷新重试。";
            } catch (Throwable failure) {
                error = "读取失败：" + DatabaseCoordinator.describeCauseChain(failure, 600);
            }
            final List<Panel> ready = panels;
            final String message = error;
            final long cost = System.currentTimeMillis() - started;
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

    private static String statusText(String status) {
        if (status.startsWith("EMPTY_NEEDS_20")) return "需要至少20次已结算样本";
        if (status.startsWith("EMPTY_NEEDS_SETTLED_ACTIONS")) return "等待出手结算";
        if (status.startsWith("EMPTY_NEEDS_SETTLED_P")) return "等待带概率的已结算预测";
        if (status.startsWith("EMPTY")) return "暂无数据，随运行自动积累";
        if (status.equals("READ_ERROR")) return "读取失败";
        return status;
    }
}
