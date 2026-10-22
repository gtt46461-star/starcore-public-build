package com.ethk3.novaonepro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/** Exact text copy with an explicit full-file/chunk fallback for Android Binder size limits. */
public final class DiagnosticTextExportR923 {
    public static final int SAFE_CLIPBOARD_CHARS = 16_000;
    private DiagnosticTextExportR923() {}

    public static void copyOrExport(final Activity activity, final String label, final String body) {
        if (activity == null || activity.isFinishing()) return;
        final String exact = body == null ? "" : body;
        if (exact.length() > SAFE_CLIPBOARD_CHARS) { requireSnapshot(activity); return; }
        if (exact.length() <= SAFE_CLIPBOARD_CHARS) {
            try {
                copy(activity, label, exact);
                Toast.makeText(activity, "完整内容已复制｜字符数=" + exact.length(), Toast.LENGTH_LONG).show();
                return;
            } catch (Throwable failure) {
                // Full text is retained. No silent truncation or fake "copied" message.
            }
        }
        new AlertDialog.Builder(activity)
                .setTitle("完整报告已生成：" + exact.length() + "字符")
                .setMessage("全文较长或系统剪贴板暂不可用。可以一次分享完整TXT文件，或分段复制；两种方式均不删减内容。")
                .setPositiveButton("分享完整TXT", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        shareFile(activity, label, exact);
                    }
                })
                .setNeutralButton("分段复制", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        chooseChunk(activity, label, exact);
                    }
                })
                .setNegativeButton("取消", null).show();
    }

    static String[] chunks(String text) {
        String exact = text == null ? "" : text;
        if (exact.length() > SAFE_CLIPBOARD_CHARS) throw new IllegalArgumentException("DETAIL_REQUIRES_FULL_SNAPSHOT");
        ArrayList<String> out = new ArrayList<String>();
        int start = 0;
        while (start < exact.length()) {
            int end = Math.min(exact.length(), start + SAFE_CLIPBOARD_CHARS);
            if (end < exact.length() && Character.isHighSurrogate(exact.charAt(end - 1))) end--;
            out.add(exact.substring(start, end));
            start = end;
        }
        if (out.isEmpty()) out.add("");
        return out.toArray(new String[out.size()]);
    }

    private static void chooseChunk(final Activity activity, final String label, final String exact) {
        final String[] parts = chunks(exact);
        final String[] choices = new String[parts.length];
        for (int i = 0; i < parts.length; i++) choices[i] = "第" + (i + 1) + "/" + parts.length
                + "段（" + parts[i].length() + "字符）";
        new AlertDialog.Builder(activity).setTitle("按顺序复制全部分段，内容没有添加标记")
                .setItems(choices, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        try {
                            copy(activity, label, parts[which]);
                            Toast.makeText(activity, choices[which] + "已复制", Toast.LENGTH_LONG).show();
                            if (!activity.isFinishing()) chooseChunk(activity, label, exact);
                        } catch (Throwable failure) {
                            Toast.makeText(activity, "复制失败：" + failure.getClass().getSimpleName(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }).setNegativeButton("关闭", null).show();
    }

    private static void copy(Context context, String label, String exact) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) throw new IllegalStateException("CLIPBOARD_UNAVAILABLE");
        clipboard.setPrimaryClip(ClipData.newPlainText(label == null ? "完整检测报告" : label, exact));
    }

    public static void shareFile(final Activity activity, final String label, final String body) {
        share(activity,label,body,false);
    }
    public static void shareCsv(final Activity activity, final String label, final String body) {
        share(activity,label,body,true);
    }
    private static void share(final Activity activity, final String label, final String body, final boolean csv) {
        if(activity==null||activity.isFinishing()||activity.isDestroyed())return;
        final String exact=body==null?"":body;
        if(exact.length()>SAFE_CLIPBOARD_CHARS){requireSnapshot(activity);return;}
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    File dir = new File(activity.getCacheDir(), "r923_reports");
                    if (!dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("REPORT_DIR");
                    String digest = V51ChecksumEngine.sha256(exact);
                    File file = new File(dir, "R923_" + digest + (csv ? ".csv" : ".txt"));
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        java.io.Writer writer=new java.io.OutputStreamWriter(out,StandardCharsets.UTF_8);
                        for(int offset=0;offset<exact.length();offset+=8192)writer.write(exact,offset,Math.min(8192,exact.length()-offset));
                        writer.flush();out.getFD().sync();
                    }
                    final Uri uri = Uri.parse("content://" + activity.getPackageName()
                            + ".diagnostic.reports/" + file.getName());
                    activity.runOnUiThread(new Runnable() {
                        @Override public void run() {
                            if (activity.isFinishing()) return;
                            try {
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType(csv ? "text/csv" : "text/plain");
                                intent.putExtra(Intent.EXTRA_STREAM, uri);
                                intent.putExtra(Intent.EXTRA_SUBJECT, label == null ? "完整检测报告" : label);
                                intent.setClipData(ClipData.newRawUri("完整检测报告", uri));
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                activity.startActivity(Intent.createChooser(intent, csv ? "导出逐期流水CSV" : "分享完整检测报告TXT"));
                            } catch (Throwable failure) {
                                Toast.makeText(activity, "分享失败：" + failure.getClass().getSimpleName(),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                } catch (final Throwable failure) {
                    activity.runOnUiThread(new Runnable() {
                        @Override public void run() {
                            if (!activity.isFinishing()) Toast.makeText(activity,
                                    "完整TXT生成失败：" + failure.getClass().getSimpleName(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }, "R923-Exact-Report-Export").start();
    }
    private static void requireSnapshot(Activity activity){
        Toast.makeText(activity,"详情超过界面安全上限；完整原始数据将从冻结检测报告交付，不声明已复制本段正文。",Toast.LENGTH_LONG).show();
        CompleteReportFileR927.export(activity);
    }
    public static void sharePrepared(Activity activity,String label,File file,String mime){
        if(activity==null||activity.isFinishing()||activity.isDestroyed())return;
        if(file==null||!file.isFile()||file.length()>131072L){requireSnapshot(activity);return;}
        Uri uri=Uri.parse("content://"+activity.getPackageName()+".diagnostic.reports/"+file.getName());
        Intent intent=new Intent(Intent.ACTION_SEND);intent.setType(mime);
        intent.putExtra(Intent.EXTRA_STREAM,uri);intent.putExtra(Intent.EXTRA_SUBJECT,label);
        intent.setClipData(ClipData.newRawUri(label,uri));intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(Intent.createChooser(intent,label));
    }

}
