# FR-B09-03 手机研究工作台与生命周期

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-18。

前驱：B08。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/ResearchWorkbenchActivityR30R1.java`

动作：**ADD**。施工范围：完整新增类。

问题：一页塞47图会超容量并阻塞加载。

修改方式：3个分组、每组六图、后台只读加载、5s取消、离页取消、明确数据/消费/效益状态。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B09-03。

### `app/src/main/java/com/ethk3/novaonepro/ChartsActivityR83.java`

动作：**MODIFY**。施工范围：onCreate。

问题：用户找不到新的真实研究分析入口。

修改方式：增加研究工作台按钮，原29图入口和工具保留。

原方法锚点：ChartsActivityR83.onCreate(Bundle state) L52–81

基线SHA256：`eccf359d8da7ca583fb1b1ff0b6df6c95d752ff86dbb3bb7bbd1169c5e12a080`。共享合并顺序：FR-B09-03。

### `app/src/main/AndroidManifest.xml`

动作：**MODIFY**。施工范围：ResearchWorkbenchActivity声明。

问题：新Activity必须显式注册且不外部导出。

修改方式：新增android:exported=false声明；不新增权限。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`f733310a2d0d5d238e2306b18377608ec73b314344b37bb4ba5df95d6c586767`。共享合并顺序：FR-B09-03。

## 实施步骤

1. 应用完整B09入口补丁；图表打开只读，不触发额外训练或REAL。
2. 点击对象查看对应处方和证据引用；如果未来添加研究动作按钮，只投递幂等研究请求，不能改生产参数。

## 测试与反证

- 三组切换/横竖屏/返回/快速重复刷新/取消/低内存；后台研究不受影响。

## 验收

手机可打开18图分组且逐图说明状态；没有分析产物就如实显示未产出。

## 恢复与回滚

隐藏新按钮/Activity，原页面不受影响。

## 关键代码

顺序补丁：`patches/B09_research_workbench_entry.patch`。必须按SERIES顺序应用，一份跨文件补丁只应用一次。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/ResearchWorkbenchActivityR30R1.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/ChartsActivityR83.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

已预写目标文件：`code/app/src/main/AndroidManifest.xml`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

### 可直接审阅的关键代码

`app/src/main/java/com/ethk3/novaonepro/ResearchWorkbenchActivityR30R1.java`（关键组合目标；集成限制见上）：

```java
package com.ethk3.novaonepro;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.*;
import android.widget.*;
import com.ethk3.novaonepro.r952.ResearchChartViewR82;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import static com.ethk3.novaonepro.ResearchVisualDataR82.*;

/** Three groups of six research views. Opening a chart never starts training or grants a certificate. */
@android.annotation.SuppressLint("SetTextI18n")
public final class ResearchWorkbenchActivityR30R1 extends Activity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Handler ui=new Handler(Looper.getMainLooper());
    private CancellationSignal cancellation;private Future<?> future;private Runnable timeout;
    private LinearLayout content;private TextView status;private long epoch;private int group;
    TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(225,237,246));t.setPadding(16,12,16,12);return t;}
    @Override protected void onCreate(Bundle state){super.onCreate(state);if(state!=null)group=state.getInt("group",0);group=Math.max(0,Math.min(2,group));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(11,23,39));root.addView(text("研究工作台 · 18个研究图",21));
        root.addView(text("分析产物→算法动作→独立确认→主链证据。未产出、未消费和未证明有效分别显示。",13));
        LinearLayout tabs=new LinearLayout(this);String[] names={"结构与归因","创造与融合","质量与应用"};for(int i=0;i<3;i++){final int id=i;Button b=new Button(this);b.setText(names[i]);b.setOnClickListener(v->{group=id;load();});tabs.addView(b,new LinearLayout.LayoutParams(0,-2,1));}root.addView(tabs);
        status=text("等待读取已提交分析……",13);root.addView(status);ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }
    @Override protected void onStart(){super.onStart();load();}
    @Override protected void onStop(){cancel();super.onStop();}
    @Override protected void onDestroy(){cancel();worker.shutdownNow();ui.removeCallbacksAndMessages(null);super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle out){out.putInt("group",group);super.onSaveInstanceState(out);}
    private void cancel(){epoch++;if(cancellation!=null)cancellation.cancel();if(future!=null)future.cancel(true);if(timeout!=null)ui.removeCallbacks(timeout);}
    private void load(){cancel();final long request=epoch;final int selectedGroup=group;final CancellationSignal signal=new CancellationSignal();cancellation=signal;timeout=signal::cancel;ui.postDelayed(timeout,5000);status.setText("读取已提交研究快照……");
        future=worker.submit(()->{List<Panel> panels=null;String failure=null;
            try{File file=getApplicationContext().getDatabasePath(AppConfig.DB_NAME);if(!file.isFile())throw new IllegalStateException("数据库尚未建立");
                try(SourceReadLeaseR82 lease=SourceReadLeaseR82.acquire(getApplicationContext(),file,signal);SQLiteDatabase db=SQLiteDatabase.openDatabase(file.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY)){panels=ResearchWorkbenchRepositoryR30R1.load(db,selectedGroup,signal);}
            }catch(OperationCanceledException cancelled){failure="读取已取消或超过5秒；研究任务继续按原调度运行。";}catch(RuntimeException|java.io.IOException error){failure="研究视图读取失败："+error.getClass().getSimpleName();}
            final List<Panel> ready=panels;final String message=failure;ui.post(()->{if(request!=epoch||isFinishing())return;if(timeout!=null)ui.removeCallbacks(timeout);content.removeAllViews();if(ready==null){status.setText(message);return;}status.setText("本组"+ready.size()+"图；页面只读，数据状态逐图显示。");for(Panel p:ready){content.addView(text(p.title,17));content.addView(text(p.status+" · "+p.window+"\n"+p.note,12));if(!"STATUS".equals(p.kind))content.addView(new ResearchChartViewR82(this,p));}});
        });
    }
}

```

`app/src/main/java/com/ethk3/novaonepro/ChartsActivityR83.java`（关键组合目标；集成限制见上）：

```java
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
        Button workbench=new Button(this);workbench.setText("研究工作台：结构 / 创造 / 质量 18图");
        workbench.setOnClickListener(v->startActivity(new Intent(this,ResearchWorkbenchActivityR30R1.class)));root.addView(workbench);
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
            try { android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND); } catch (Throwable ignored) { }
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
            } catch (OutOfMemoryError memory) {
                panels = null; error = "内存不足，图表未加载（不影响后台运行）；请稍后点刷新。";
            } catch (Throwable failure) {
                error = "读取失败：" + DatabaseCoordinator.describeCauseChain(failure, 600);
            }
            final List<Panel> ready = panels;
            final String message = error;
            final long cost = System.currentTimeMillis() - started;
            if (ready != null) try { recordLoadR27(cost); } catch (RuntimeException ignored) { }
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
            // R27.2: one bad panel shows its error instead of closing the page (device video 10.29 09:57: the app
            // restarted while this page loaded).
            try { addPanel(panel); }
            catch (RuntimeException failure) { charts.addView(text(panel.title + "：本图显示失败（" + failure.getClass().getSimpleName() + "），其他图不受影响。", 13, muted)); }
        }
        status.setText("共" + panels.size() + "张图，" + ready + "张已有数据；读取耗时" + cost + "ms。数据随运行持续积累。");
    }

    private void addPanel(Panel panel) {
        {
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
    }

    private static final int IMPORT_CSV_R27 = 27018;
    private void toolsR27() {
        worker.submit(() -> {
            DbStore store = new DbStore(getApplicationContext());
            String legacy = store.getState(ResearchLearningContinuityR912.R27_LEGACY_SWITCH, "1");
            // R27.2: the device crashed here (BadTokenException) when the page closed before the read returned.
            ui.post(() -> { if (isFinishing() || isDestroyed()) return; new android.app.AlertDialog.Builder(this).setTitle("R27 工具")
                    .setItems(new String[] {"导入历史CSV（与库内同期逐期比对，冲突不导入）", "旧R9xx研究链并行：" + ("0".equals(legacy) ? "已关闭（点此重新开启）" : "开启中（并行观察≥1440期后再关闭）"), "重试全部已隔离（QUARANTINED）的研究任务"}, (d, which) -> {
                        if (which == 0) { android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT); i.addCategory(android.content.Intent.CATEGORY_OPENABLE); i.setType("*/*"); startActivityForResult(i, IMPORT_CSV_R27); }
                        else if (which == 1) worker.submit(() -> { store.setState(ResearchLearningContinuityR912.R27_LEGACY_SWITCH, "0".equals(legacy) ? "1" : "0"); ui.post(() -> status.setText("旧研究链开关已改为 " + ("0".equals(legacy) ? "开启" : "关闭") + "；旧表保留只读。")); });
                        else worker.submit(() -> { int n = com.ethk3.novaonepro.strategyv2.HistoryImportR27.retryQuarantined(store); ui.post(() -> status.setText("已重置隔离任务：" + n + "个")); });
                    }).show(); });
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

```

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
