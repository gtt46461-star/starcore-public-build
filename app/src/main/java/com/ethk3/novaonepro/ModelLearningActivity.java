package com.ethk3.novaonepro;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Learning/invention evidence and owner-signed model package management.
 *
 * <p>The page deliberately owns no SQL and no independent counters. Home, this page and the
 * complete diagnostic report use {@link RuntimeTruthSnapshotR906} and the same owner receipts.
 * Live observations are time-bound; only the export reads an immutable database copy.
 */
public final class ModelLearningActivity extends Activity {
    private static final String PAGE_TITLE = "深度学习・动态迭代・算法创造状态";
    private static final long FOREGROUND_REFRESH_MS = 15_000L;

    private DbStore store;
    private TextView text;
    private HandlerThread truthThread;
    private Handler truthHandler;
    private final Handler foregroundHandler = new Handler();
    private volatile boolean destroyed;
    private volatile boolean foreground;
    private volatile boolean readRunning;
    private volatile String copyPayload = "";
    private final Runnable foregroundRefresh = new Runnable() {
        @Override public void run() {
            if (!foreground || destroyed) return;
            requestTruth();
            foregroundHandler.postDelayed(this, FOREGROUND_REFRESH_MS);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_diagnosis);
        ((TextView) findViewById(R.id.txt_diagnosis_title)).setText(PAGE_TITLE);
        text = (TextView) findViewById(R.id.txt_diagnosis_full);
        text.setText("正在读取候选、训练和结算记录；查询未完成的项目显示未知……");

        truthThread = new HandlerThread("r919_model_learning_truth", Process.THREAD_PRIORITY_BACKGROUND);
        truthThread.start();
        truthHandler = new Handler(truthThread.getLooper());

        Button copy = (Button) findViewById(R.id.btn_diagnosis_copy);
        copy.setText("复制算法创造与结算证据");
        copy.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                String payload = copyPayload.length() == 0 ? String.valueOf(text.getText()) : copyPayload;
                ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (manager != null) manager.setPrimaryClip(ClipData.newPlainText(PAGE_TITLE, payload));
                Toast.makeText(ModelLearningActivity.this, "当前读取的证据已复制", Toast.LENGTH_SHORT).show();
            }
        });
        Button models=(Button)findViewById(R.id.btn_diagnosis_copy_all);
        models.setText("管理模型包");
        models.setOnClickListener(new View.OnClickListener(){public void onClick(View view){
            new android.app.AlertDialog.Builder(ModelLearningActivity.this).setTitle("模型包")
                .setItems(new String[]{"导入模型签名公钥", "导入已签名模型包", "撤回到上一模型包", "导出本机模型合同"},
                new android.content.DialogInterface.OnClickListener(){public void onClick(android.content.DialogInterface dialog,int which){
                    if(which==2){requestModelRollback();return;}
                    if(which==3){android.content.Intent save=new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT);save.addCategory(android.content.Intent.CATEGORY_OPENABLE);save.setType("application/json");save.putExtra(android.content.Intent.EXTRA_TITLE,"starcore-runtime-model-contract.json");startActivityForResult(save,9323);return;}
                    android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);intent.setType("*/*");
                    startActivityForResult(intent,which==0?9321:9322);
                }}).show();
        }});
        requestTruth();
    }

    @Override
    protected void onActivityResult(final int request,int result,android.content.Intent data){
        super.onActivityResult(request,result,data);
        if(request==9323&&result==RESULT_OK&&data!=null&&data.getData()!=null){
            final android.net.Uri output=data.getData();truthHandler.post(new Runnable(){public void run(){try(java.io.OutputStream out=getContentResolver().openOutputStream(output)){if(out==null)throw new java.io.IOException("无法写入文件");out.write(RuntimeModelContractR932.json().getBytes("UTF-8"));modelMessage("本机模型合同已导出。");}catch(Exception e){modelMessage("导出失败："+e.getMessage());}}});return;
        }
        if((request!=9321&&request!=9322)||result!=RESULT_OK||data==null||data.getData()==null)return;
        final android.net.Uri uri=data.getData();
        truthHandler.post(new Runnable(){public void run(){
            try(java.io.InputStream in=getContentResolver().openInputStream(uri)){
                if(in==null)throw new java.io.IOException("无法读取所选文件");
                if(request==9321){
                    final byte[] key=ModelBundleManagerR929.readBounded(in,8192);
                    ModelBundleRuntimeR932.decodeKey(key);final String fingerprint=ModelBundleRuntimeR932.digest(key);
                    runOnUiThread(new Runnable(){public void run(){if(destroyed)return;
                        new android.app.AlertDialog.Builder(ModelLearningActivity.this).setTitle("确认模型签名公钥")
                            .setMessage("请与模型提供方核对公钥指纹：\n"+fingerprint+"\n确认后仅接受该公钥签名的模型包。")
                            .setNegativeButton("取消",null).setPositiveButton("信任此公钥",new android.content.DialogInterface.OnClickListener(){
                                public void onClick(android.content.DialogInterface d,int w){truthHandler.post(new Runnable(){public void run(){
                                    try{ModelBundleRuntimeR932.trustKey(key);modelMessage("模型签名公钥已保存");}
                                    catch(Exception e){modelMessage("公钥导入失败："+e.getMessage());}
                                }});}}).show();
                    }});
                }else{
                    String hash=ModelBundleRuntimeR932.importArchive(requireStore(),in);
                    modelMessage("模型包校验通过，将在下一期分析开始时整体激活。\n"+hash);
                }
            }catch(Exception e){modelMessage("模型导入失败："+e.getMessage());}
        }});
    }
    private void requestModelRollback(){
        truthHandler.post(new Runnable(){public void run(){try{
            ModelBundleRuntimeR932.requestRollback(requireStore(),"OWNER_REQUEST");modelMessage("已撤回模型包；正在运行的一期保留其固定版本。");
        }catch(Exception e){modelMessage("撤回失败："+e.getMessage());}}});
    }
    private void modelMessage(final String message){runOnUiThread(new Runnable(){public void run(){
        if(!destroyed)new android.app.AlertDialog.Builder(ModelLearningActivity.this).setMessage(message).setPositiveButton("知道了",null).show();
    }});}

    @Override
    protected void onResume() {
        super.onResume();
        if (copyPayload.length() > 0) requestTruth();
    }

    @Override
    protected void onStart() {
        super.onStart();
        foreground = true;
        foregroundHandler.removeCallbacks(foregroundRefresh);
        foregroundHandler.post(foregroundRefresh);
    }

    @Override
    protected void onStop() {
        foreground = false;
        foregroundHandler.removeCallbacks(foregroundRefresh);
        super.onStop();
    }

    private void requestTruth() {
        if (destroyed || readRunning || truthHandler == null) return;
        readRunning = true;
        truthHandler.post(new Runnable() {
            @Override public void run() {
                String body;
                try {
                    DbStore current = requireStore();
                    body = render(RuntimeTruthSnapshotR906.capture(current))
                            + "\n\n【模型包】\n" + ModelBundleRuntimeR932.status(current);
                } catch (Throwable error) {
                    CrashRecorder.recordCaught(getApplicationContext(), error, "R68_MODEL_PAGE_READ");
                    body = "读取暂未完成，页面会自动重试；已有训练数据未删除。\n"
                            + DatabaseCoordinator.describeCauseChain(error, 1200);
                } finally {
                    readRunning = false;
                }
                final String rendered = body;
                copyPayload = rendered;
                if (destroyed) {
                    closeStoreOnce();
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        if (!destroyed && text != null) text.setText(rendered);
                    }
                });
            }
        });
    }

    /** Only the page's serial background handler opens or reads SQLite. */
    private DbStore requireStore() {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper())
            throw new IllegalStateException("R68_MODEL_PAGE_DB_ON_MAIN");
        if (destroyed) throw new IllegalStateException("R68_MODEL_PAGE_CLOSED");
        if (store == null) store = new DbStore(getApplicationContext());
        return store;
    }

    private static String render(RuntimeTruthSnapshotR906 truth) {
        StringBuilder out = new StringBuilder(32768);
        out.append("【R923唯一快照身份】\n")
                .append("快照=").append(truth.snapshotId)
                .append("｜状态=").append(truth.status)
                .append("｜目标期=").append(explicit(truth.targetIssue, "尚无赛前目标期"))
                .append("\n数据库data_version=").append(truth.dbDataVersion)
                .append("｜生成时间=").append(truth.generatedAt)
                .append("\nSHA-256=").append(truth.hash)
                .append("\n读取错误=").append(explicit(truth.readError, "无"))
                .append("\n\n【唯一主链与后台连续运行】\n")
                .append(truth.pipelineDetail)
                .append("\n\n【逐期开奖任务、逐期结算与两个分母】\n")
                .append(truth.shadowDetail)
                .append("\n\n【深度学习・动态迭代・算法/策略创造】\n")
                .append(truth.researchDetail)
                .append("\n\n【真实出手唯一硬门】\n")
                .append(truth.realDetail)
                .append("\n\n【为什么中 / 为什么不中 / 为什么不出手】\n");

        if (truth.whyCards.length == 0) {
            out.append("无卡片｜原因=同一快照没有可展示证据");
        } else {
            for (int i = 0; i < truth.whyCards.length; i++) {
                RuntimeTruthSnapshotR906.TruthCard card = truth.whyCards[i];
                out.append(i + 1).append('.').append(card.title)
                        .append("｜状态=").append(card.status)
                        .append("｜来源=").append(card.source)
                        .append("\n").append(card.detail).append("\n");
            }
        }

        out.append("\n【十六项运行真值】\n");
        if (truth.stateBoxes.length == 0) {
            out.append("无状态框｜原因=同一快照没有可展示证据\n");
        } else {
            for (int i = 0; i < truth.stateBoxes.length; i++) {
                RuntimeTruthSnapshotR906.StateBox box = truth.stateBoxes[i];
                out.append(box.ordinal).append('.').append(box.title)
                        .append("｜").append(box.value)
                        .append("｜状态=").append(box.status)
                        .append("｜来源=").append(box.source).append('\n');
            }
        }

        out.append("\n【不得伪造的证明边界】\n")
                .append("中文名必须由候选的真实状态记忆、条件分流、变量耦合、非线性和更新机制生成；")
                .append("改参数不计新算法。候选必须赛前冻结，开奖后只结算已冻结版本，学习最早影响t+1。\n")
                .append("创造能力负责扩大搜索空间、未来OOS验证和自动淘汰，不能承诺随机开奖必然达到70%或80%；")
                .append("没有Future-OOS证书就显示未证明，Research没有直接REAL授权权。\n")
                .append("首页、本页、完整报告Owner=RuntimeTruthSnapshotR906｜版本=")
                .append(RuntimeTruthSnapshotR906.VERSION).append('\n');
        return out.toString();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        foreground = false;
        foregroundHandler.removeCallbacks(foregroundRefresh);
        try { if (truthThread != null) truthThread.quitSafely(); }
        catch (Throwable ignored) {}
        if (!readRunning) closeStoreOnce();
        super.onDestroy();
    }

    private synchronized void closeStoreOnce() {
        DbStore current = store;
        store = null;
        if (current != null) try { current.close(); } catch (Throwable ignored) {}
    }

    private static String explicit(String value, String fallback) {
        return safe(value).length() == 0 ? fallback : safe(value);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
