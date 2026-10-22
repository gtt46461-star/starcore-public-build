package com.ethk3.novaonepro;

import android.content.Context;
import android.os.Build;

import java.lang.reflect.Field;
import java.util.Locale;

public final class UiFormatter {

    private UiFormatter() {}

    public static String safeText(String value) {
        if (value == null || value.length() == 0) {
            return "--";
        }
        return sanitizeForUi(value);
    }

    public static String safeEmpty(String value) {
        if (value == null) {
            return "";
        }
        return sanitizeForUi(value);
    }

    public static String dashIfEmpty(String value) {
        if (value == null || value.length() == 0) {
            return "--";
        }
        return sanitizeForUi(value);
    }

    public static String issue(String issue) {
        if (issue == null || issue.length() == 0) {
            return "--";
        }
        return sanitizeForUi(issue);
    }

    public static String issuePair(String currentIssue, String targetIssue) {
        return "当前期 " + issue(currentIssue) + "  目标期 " + issue(targetIssue);
    }

    public static String targetIssueLine(String targetIssue) {
        return "下一期目标：" + issue(targetIssue);
    }

    public static String number(int number) {
        if (number < AppConfig.MIN_NUMBER || number > AppConfig.MAX_NUMBER) {
            return "--";
        }

        if (number < 10) {
            return "0" + number;
        }

        return String.valueOf(number);
    }

    public static String numberPlain(int number) {
        if (number < AppConfig.MIN_NUMBER || number > AppConfig.MAX_NUMBER) {
            return "--";
        }
        return String.valueOf(number);
    }

    public static String numberGroup(int n1, int n2, int n3) {
        return number(n1) + " / " + number(n2) + " / " + number(n3);
    }

    public static String numberGroupCompact(int n1, int n2, int n3) {
        return number(n1) + "," + number(n2) + "," + number(n3);
    }

    public static String numberGroup(int[] numbers) {
        if (numbers == null || numbers.length < AppConfig.FIXED_K) {
            return "-- / -- / --";
        }

        return numberGroup(numbers[0], numbers[1], numbers[2]);
    }

    public static String maskText(int mask) {
        if (!Models.isValidK3Mask(mask)) {
            return "-- / -- / --";
        }
        return numberGroup(Models.maskToNumbers(mask));
    }

    public static String openCode(int[] numbers) {
        if (numbers == null || numbers.length == 0) {
            return "--";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < numbers.length; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(number(numbers[i]));
        }

        return sb.toString();
    }

    public static String openCodeComma(int[] numbers) {
        if (numbers == null || numbers.length == 0) {
            return "--";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < numbers.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(number(numbers[i]));
        }

        return sb.toString();
    }

    public static String position(int position) {
        if (!AppConfig.isValidPosition(position)) {
            return "第--位置";
        }

        return "第" + position + "位置";
    }

    public static String positionShort(int position) {
        if (!AppConfig.isValidPosition(position)) {
            return "P--";
        }
        return "P" + position;
    }

    public static String positionAndNumbers(int position, int n1, int n2, int n3) {
        if (!AppConfig.isValidPosition(position)) {
            return "第--位置  -- / -- / --";
        }

        return position(position) + "  " + numberGroup(n1, n2, n3);
    }

    public static String positionAndMask(int position, int mask) {
        if (!AppConfig.isValidPosition(position) || !Models.isValidK3Mask(mask)) {
            return "第--位置  -- / -- / --";
        }

        int[] nums = Models.maskToNumbers(mask);
        return positionAndNumbers(position, nums[0], nums[1], nums[2]);
    }

    public static String betContent(int position, int n1, int n2, int n3) {
        if (!AppConfig.isPlayRuleLegal(position, AppConfig.FIXED_K, n1, n2, n3)) {
            return "本期不出手";
        }

        return positionAndNumbers(position, n1, n2, n3);
    }

    public static String probability(double value) {
        double v = cleanDouble(value);

        if (v < 0.0d) {
            v = 0.0d;
        }

        if (v > 1.0d) {
            v = 1.0d;
        }

        return String.format(Locale.US, "%.1f%%", Double.valueOf(v * 100.0d));
    }

    public static String probability2(double value) {
        double v = cleanDouble(value);

        if (v < 0.0d) {
            v = 0.0d;
        }

        if (v > 1.0d) {
            v = 1.0d;
        }

        return String.format(Locale.US, "%.2f%%", Double.valueOf(v * 100.0d));
    }

    /** Formats an evidence-backed probability without turning missing evidence into zero. */
    public static String formatOptionalProbability(
            boolean available, double value, String unavailableReason) {
        if (!available || Double.isNaN(value) || Double.isInfinite(value)) {
            return "N/A（校准未训练/未证明）";
        }
        return probability2(value);
    }

    public static String percent(double value) {
        return probability(value);
    }

    public static String percent2(double value) {
        return probability2(value);
    }

    public static String probabilityRaw(double value) {
        double v = cleanDouble(value);
        return String.format(Locale.US, "%.4f", Double.valueOf(v));
    }

    public static String signedProbability(double value) {
        double v = cleanDouble(value);
        String sign = v >= 0.0d ? "+" : "";
        return sign + String.format(Locale.US, "%.1f%%", Double.valueOf(v * 100.0d));
    }

    public static String signedProbability2(double value) {
        double v = cleanDouble(value);
        String sign = v >= 0.0d ? "+" : "";
        return sign + String.format(Locale.US, "%.2f%%", Double.valueOf(v * 100.0d));
    }

    public static String score(double value) {
        double v = cleanDouble(value);

        if (v >= 0.0d && v <= 1.0d) {
            v = v * 100.0d;
        }

        if (v < 0.0d) {
            v = 0.0d;
        }

        if (v > 100.0d) {
            v = 100.0d;
        }

        return String.format(Locale.US, "%.1f", Double.valueOf(v));
    }

    public static String score2(double value) {
        double v = cleanDouble(value);

        if (v >= 0.0d && v <= 1.0d) {
            v = v * 100.0d;
        }

        if (v < 0.0d) {
            v = 0.0d;
        }

        if (v > 100.0d) {
            v = 100.0d;
        }

        return String.format(Locale.US, "%.2f", Double.valueOf(v));
    }

    public static String score01(double value) {
        double v = AppConfig.clampScore01(cleanDouble(value));
        return String.format(Locale.US, "%.1f", Double.valueOf(v * 100.0d));
    }

    public static String score100(double value) {
        double v = AppConfig.clampScore100(cleanDouble(value));
        return String.format(Locale.US, "%.1f", Double.valueOf(v));
    }

    public static String signedScore(double value) {
        double v = cleanDouble(value);
        String sign = v >= 0.0d ? "+" : "";
        return sign + String.format(Locale.US, "%.1f", Double.valueOf(v));
    }

    public static String decimal(double value) {
        double v = cleanDouble(value);
        return String.format(Locale.US, "%.3f", Double.valueOf(v));
    }

    public static String decimal2(double value) {
        double v = cleanDouble(value);
        return String.format(Locale.US, "%.2f", Double.valueOf(v));
    }

    public static String decimal4(double value) {
        double v = cleanDouble(value);
        return String.format(Locale.US, "%.4f", Double.valueOf(v));
    }

    public static String integer(int value) {
        return String.valueOf(value);
    }

    public static String money(double value) {
        double v = cleanDouble(value);
        String sign = v >= 0.0d ? "+" : "";
        return sign + String.format(Locale.US, "%.2f", Double.valueOf(v));
    }

    public static String moneyNoSign(double value) {
        double v = cleanDouble(value);
        return String.format(Locale.US, "%.2f", Double.valueOf(v));
    }

    public static String odds(double value) {
        double v = cleanDouble(value);
        return String.format(Locale.US, "%.2f", Double.valueOf(v));
    }

    public static String countdown(long millis) {
        if (millis < 0L) {
            millis = 0L;
        }

        long totalSeconds = millis / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (hours > 0L) {
            return String.format(
                    Locale.US,
                    "%02d:%02d:%02d",
                    Long.valueOf(hours),
                    Long.valueOf(minutes),
                    Long.valueOf(seconds));
        }

        return String.format(Locale.US, "%02d:%02d", Long.valueOf(minutes), Long.valueOf(seconds));
    }

    public static String durationMs(long millis) {
        if (millis < 0L) {
            millis = 0L;
        }

        if (millis < 1000L) {
            return millis + "ms";
        }

        long seconds = millis / 1000L;

        if (seconds < 60L) {
            return seconds + "秒";
        }

        long minutes = seconds / 60L;
        long rest = seconds % 60L;

        if (minutes < 60L) {
            return minutes + "分" + rest + "秒";
        }

        long hours = minutes / 60L;
        long restMin = minutes % 60L;

        return hours + "小时" + restMin + "分";
    }

    public static String decisionText(Context context, String decision) {
        return decisionText(decision);
    }

    public static String decisionText(String decision) {
        String d = normalizeDecisionText(decision);

        if (AppConfig.DECISION_BET.equals(d)) {
            return "出手";
        }

        if (AppConfig.DECISION_HOLD.equals(d)) {
            return "不出手";
        }

        if (AppConfig.DECISION_SHADOW.equals(d)) {
            return "影子观察";
        }

        return safeText(decision);
    }

    public static String actionText(String action) {
        return decisionText(action);
    }

    public static String decisionTextLong(String decision) {
        String d = normalizeDecisionText(decision);

        if (AppConfig.DECISION_BET.equals(d)) {
            return "真实BET出手";
        }

        if (AppConfig.DECISION_HOLD.equals(d)) {
            return "HOLD保护观望";
        }

        if (AppConfig.DECISION_SHADOW.equals(d)) {
            return "SHADOW影子学习";
        }

        return safeText(decision);
    }

    public static int decisionColor(String decision) {
        String d = normalizeDecisionText(decision);

        if (AppConfig.DECISION_BET.equals(d)) {
            return R.color.colorWin;
        }

        if (AppConfig.DECISION_HOLD.equals(d)) {
            return R.color.colorHold;
        }

        if (AppConfig.DECISION_SHADOW.equals(d)) {
            return R.color.colorInfo;
        }

        return R.color.colorTextSub;
    }

    public static int decisionColor(Context context, String decision) {
        return getColor(context, decisionColor(decision));
    }

    public static String resultText(Context context, String result) {
        return resultText(result);
    }

    public static String resultText(String result) {
        String r = normalizeResultText(result);

        if (AppConfig.RESULT_WIN.equals(r)) {
            return "命中";
        }

        if (AppConfig.RESULT_MISS.equals(r)) {
            return "未中";
        }

        if (AppConfig.RESULT_DATA_ERROR.equals(r)) {
            return "数据异常";
        }

        if (AppConfig.RESULT_WAIT.equals(r)) {
            return "等待结算";
        }

        if (AppConfig.RESULT_VOID.equals(r)) {
            return "作废";
        }

        return safeText(result);
    }

    public static int resultColor(String result) {
        String r = normalizeResultText(result);

        if (AppConfig.RESULT_WIN.equals(r)) {
            return R.color.colorWin;
        }

        if (AppConfig.RESULT_MISS.equals(r)) {
            return R.color.colorMiss;
        }

        if (AppConfig.RESULT_DATA_ERROR.equals(r)) {
            return R.color.colorWarn;
        }

        if (AppConfig.RESULT_WAIT.equals(r)) {
            return R.color.colorTextSub;
        }

        if (AppConfig.RESULT_VOID.equals(r)) {
            return R.color.colorWarn;
        }

        return R.color.colorTextSub;
    }

    public static int resultColor(Context context, String result) {
        return getColor(context, resultColor(result));
    }

    public static String serviceStatusText(Context context, String status) {
        return serviceStatusText(status);
    }

    public static String serviceStatusText(String status) {
        if (AppConfig.SERVICE_STATUS_RUNNING.equals(status)) {
            return "运行中";
        }

        if (AppConfig.SERVICE_STATUS_STOPPED.equals(status)) {
            return "已停止";
        }

        if (AppConfig.SERVICE_STATUS_FETCHING.equals(status)) {
            return "抓取中";
        }

        if (AppConfig.SERVICE_STATUS_ANALYZING.equals(status)) {
            return "分析中";
        }

        if (AppConfig.SERVICE_STATUS_SETTLING.equals(status)) {
            return "结算中";
        }

        if (AppConfig.SERVICE_STATUS_LEARNING.equals(status)) {
            return "学习中";
        }

        if (AppConfig.SERVICE_STATUS_BACKTESTING.equals(status)) {
            return "回测中";
        }

        if (AppConfig.SERVICE_STATUS_BACKTEST_FAST.equals(status)) {
            return "快线回测中";
        }

        if (AppConfig.SERVICE_STATUS_BACKTEST_NORMAL.equals(status)) {
            return "普通回测中";
        }

        if (AppConfig.SERVICE_STATUS_BACKTEST_FULL.equals(status)) {
            return "完整深度回测中";
        }

        if (AppConfig.SERVICE_STATUS_WAITING.equals(status)) {
            return "等待中";
        }

        if (AppConfig.SERVICE_STATUS_DELAY.equals(status)) {
            return "延迟等待";
        }

        if (AppConfig.SERVICE_STATUS_API_ERROR.equals(status)) {
            return "接口异常";
        }

        if (AppConfig.SERVICE_STATUS_DATA_ERROR.equals(status)) {
            return "数据异常";
        }

        if (AppConfig.SERVICE_STATUS_AI_JUDGING.equals(status)) {
            return "AI裁决中";
        }

        if (AppConfig.SERVICE_STATUS_DEEP_ANALYZING.equals(status)) {
            return "深度分析中";
        }

        if (AppConfig.SERVICE_STATUS_DIAGNOSING.equals(status)) {
            return "自检诊断中";
        }

        if ("RUNNING".equals(status)) {
            return "运行中";
        }

        if ("BUSY".equals(status)) {
            return "忙碌";
        }

        if ("IDLE".equals(status)) {
            return "空闲";
        }

        if ("SKIP".equals(status)) {
            return "已跳过";
        }

        if ("OK".equals(status)) {
            return "正常";
        }

        if ("ERROR".equals(status)) {
            return "异常";
        }

        if ("FINISHED".equals(status)) {
            return "已完成";
        }

        if ("SETTLING".equals(status)) {
            return "结算中";
        }

        return safeText(status);
    }

    public static int statusColor(String status) {
        if (AppConfig.SERVICE_STATUS_RUNNING.equals(status)
                || AppConfig.SERVICE_STATUS_WAITING.equals(status)
                || AppConfig.SERVICE_STATUS_FETCHING.equals(status)
                || AppConfig.SERVICE_STATUS_ANALYZING.equals(status)
                || AppConfig.SERVICE_STATUS_SETTLING.equals(status)
                || AppConfig.SERVICE_STATUS_LEARNING.equals(status)
                || AppConfig.SERVICE_STATUS_BACKTESTING.equals(status)
                || AppConfig.SERVICE_STATUS_BACKTEST_FAST.equals(status)
                || AppConfig.SERVICE_STATUS_BACKTEST_NORMAL.equals(status)
                || AppConfig.SERVICE_STATUS_BACKTEST_FULL.equals(status)
                || AppConfig.SERVICE_STATUS_AI_JUDGING.equals(status)
                || AppConfig.SERVICE_STATUS_DEEP_ANALYZING.equals(status)
                || AppConfig.SERVICE_STATUS_DIAGNOSING.equals(status)
                || "RUNNING".equals(status)
                || "OK".equals(status)
                || "FINISHED".equals(status)
                || "SETTLING".equals(status)) {
            return R.color.colorWin;
        }

        if (AppConfig.SERVICE_STATUS_STOPPED.equals(status)
                || "IDLE".equals(status)
                || "SKIP".equals(status)) {
            return R.color.colorWarn;
        }

        if (AppConfig.SERVICE_STATUS_API_ERROR.equals(status)
                || AppConfig.SERVICE_STATUS_DATA_ERROR.equals(status)
                || "ERROR".equals(status)) {
            return R.color.colorMiss;
        }

        if (AppConfig.SERVICE_STATUS_DELAY.equals(status) || "BUSY".equals(status)) {
            return R.color.colorWarn;
        }

        return R.color.colorInfo;
    }

    public static int serviceStatusColor(String status) {
        return statusColor(status);
    }

    public static int serviceStatusColor(Context context, String status) {
        return getColor(context, statusColor(status));
    }

    public static String modeText(Context context, String mode) {
        return modeText(mode);
    }

    public static String modeText(String mode) {
        if (AppConfig.MODE_FORMING.equals(mode)) {
            return "形态形成中";
        }

        if (AppConfig.MODE_CONFIRMED.equals(mode)) {
            return "形态已确认";
        }

        if (AppConfig.MODE_ACCELERATING.equals(mode)) {
            return "趋势加速";
        }

        if (AppConfig.MODE_PULLBACK.equals(mode)) {
            return "趋势回拉";
        }

        if (AppConfig.MODE_RECOVERING.equals(mode)) {
            return "恢复中";
        }

        if (AppConfig.MODE_WEAKENING.equals(mode)) {
            return "趋势转弱";
        }

        if (AppConfig.MODE_BROKEN.equals(mode)) {
            return "形态破位";
        }

        if (AppConfig.MODE_REVERSING.equals(mode) || AppConfig.MODE_REVERSAL.equals(mode)) {
            return "反转风险";
        }

        if (AppConfig.MODE_CHAOS.equals(mode)) {
            return "无有效形态";
        }

        if (AppConfig.MODE_STABLE.equals(mode)) {
            return "稳定形态";
        }

        if (AppConfig.MODE_TRANSITION.equals(mode)) {
            return "过渡形态";
        }

        if (AppConfig.MODE_UNKNOWN.equals(mode)) {
            return "未知形态";
        }

        return safeText(mode);
    }

    public static int modeColor(String mode) {
        if (AppConfig.MODE_CONFIRMED.equals(mode)
                || AppConfig.MODE_ACCELERATING.equals(mode)
                || AppConfig.MODE_RECOVERING.equals(mode)
                || AppConfig.MODE_STABLE.equals(mode)) {
            return R.color.colorWin;
        }

        if (AppConfig.MODE_FORMING.equals(mode)
                || AppConfig.MODE_PULLBACK.equals(mode)
                || AppConfig.MODE_WEAKENING.equals(mode)
                || AppConfig.MODE_TRANSITION.equals(mode)) {
            return R.color.colorHold;
        }

        if (AppConfig.MODE_BROKEN.equals(mode)
                || AppConfig.MODE_REVERSING.equals(mode)
                || AppConfig.MODE_REVERSAL.equals(mode)
                || AppConfig.MODE_CHAOS.equals(mode)) {
            return R.color.colorMiss;
        }

        return R.color.colorTextSub;
    }

    public static int modeColor(Context context, String mode) {
        return getColor(context, modeColor(mode));
    }

    public static String reasonLevelText(Context context, String level) {
        return reasonLevelText(level);
    }

    public static String reasonLevelText(String level) {
        if (AppConfig.REASON_PASS.equals(level)) {
            return "通过";
        }

        if (AppConfig.REASON_BLOCK.equals(level)) {
            return "拦截";
        }

        if (AppConfig.REASON_WARN.equals(level)) {
            return "警告";
        }

        if (AppConfig.REASON_INFO.equals(level)) {
            return "提示";
        }

        return safeText(level);
    }

    public static int reasonColor(String level) {
        if (AppConfig.REASON_PASS.equals(level)) {
            return R.color.colorWin;
        }

        if (AppConfig.REASON_BLOCK.equals(level)) {
            return R.color.colorMiss;
        }

        if (AppConfig.REASON_WARN.equals(level)) {
            return R.color.colorWarn;
        }

        return R.color.colorInfo;
    }

    public static int reasonLevelColor(String level) {
        return reasonColor(level);
    }

    public static int reasonLevelColor(Context context, String level) {
        return getColor(context, reasonColor(level));
    }

    public static String blockReasonText(Context context, String blockCode) {
        return blockReasonText(blockCode);
    }

    public static String blockReasonText(String blockCode) {
        if (blockCode == null || blockCode.length() == 0) {
            return "--";
        }

        if (AppConfig.BLOCK_LOW_PROBABILITY.equals(blockCode)) {
            return "概率没有超过动态出手门槛";
        }

        if (AppConfig.BLOCK_SAMPLE_WEAK.equals(blockCode)) {
            return "样本不足或相似样本偏弱";
        }

        if (AppConfig.BLOCK_TREND_WEAK.equals(blockCode)) {
            return "趋势形态不够强";
        }

        if (AppConfig.BLOCK_DRIFT_HIGH.equals(blockCode)) {
            return "漂移风险过高";
        }

        if (AppConfig.BLOCK_BREAK_HIGH.equals(blockCode)) {
            return "破位深度过高";
        }

        if (AppConfig.BLOCK_RECOVERY_WEAK.equals(blockCode)) {
            return "恢复证据不足";
        }

        if (AppConfig.BLOCK_POSITION_WEAK.equals(blockCode)) {
            return "位置优势不足";
        }

        if (AppConfig.BLOCK_COMBO_WEAK.equals(blockCode)) {
            return "K3组合优势不足";
        }

        if (AppConfig.BLOCK_FREQ_HIGH.equals(blockCode)) {
            return "近期出手频率过高";
        }

        if (AppConfig.BLOCK_DATA_ERROR.equals(blockCode)) {
            return "数据异常";
        }

        if (AppConfig.BLOCK_HISTORY_WEAK.equals(blockCode)) {
            return "历史数据不足";
        }

        if (AppConfig.BLOCK_OPEN_TRADE_EXISTS.equals(blockCode)) {
            return "目标期已有未结算真实BET";
        }

        if (AppConfig.BLOCK_BACKTEST_WEAK.equals(blockCode)) {
            return "轻量验证或后台回测表现偏弱";
        }

        if (AppConfig.BLOCK_FULL_MODE_BUSY.equals(blockCode)) {
            return "完整深度回测正在运行，但不应阻塞实时出号";
        }

        if (AppConfig.BLOCK_PROFILE_STALE.equals(blockCode)) {
            return "深度画像陈旧";
        }

        if (AppConfig.BLOCK_AUDIT_OVER.equals(blockCode)) {
            return "概率审计显示过度自信";
        }

        if (AppConfig.BLOCK_AI_CONFLICT.equals(blockCode)) {
            return "AI裁决冲突过高";
        }

        if (AppConfig.BLOCK_A_PLUS_NOT_READY.equals(blockCode)) {
            return "A+证据尚未准备好";
        }

        if (AppConfig.BLOCK_BIAS_WEAK.equals(blockCode)) {
            return "偏差证据不足";
        }

        if (AppConfig.BLOCK_SCHEDULER_BUSY.equals(blockCode)) {
            return "调度器忙碌，实时优先";
        }

        if ("REALTIME_TIMEOUT_FAST_SAVE".equals(blockCode)) {
            return "实时分析超时，已保存快车道结果";
        }

        if ("PLACEHOLDER_NOT_REAL".equals(blockCode)) {
            return "临时占位，不计真实胜率，不保存真实交易";
        }

        if ("TARGET_ISSUE_MISMATCH".equals(blockCode)) {
            return "目标期错位，拒绝显示旧预测";
        }

        if ("FUTURE_LEAK_BLOCK".equals(blockCode)
                || "FUTURE_LEAK".equals(blockCode)
                || "BLOCK_FUTURE_LEAK".equals(blockCode)) {
            return "时间点快照发现未来数据泄露风险";
        }

        if ("AUDIT_FAILED".equals(blockCode) || "PROBABILITY_AUDIT_FAILED".equals(blockCode)) {
            return "概率审计未通过";
        }

        if ("PROMOTION_FAILED".equals(blockCode) || "PROMOTION_GATE_FAILED".equals(blockCode)) {
            return "晋级门未通过";
        }

        if ("A_PLUS_FAILED".equals(blockCode) || "A_PLUS_SHAPE_FAILED".equals(blockCode)) {
            return "A+形态未通过";
        }

        if ("STRICT_SETTLEMENT_FAILED".equals(blockCode)) {
            return "严格结算写入失败，真实盈亏未落库";
        }

        return translateLooseText(blockCode);
    }

    public static String missReasonText(Context context, String missCode) {
        return missReasonText(missCode);
    }

    public static String missReasonText(String missCode) {
        if (missCode == null || missCode.length() == 0) {
            return "--";
        }

        if (AppConfig.MISS_POSITION_LOST.equals(missCode)) {
            return "位置判断失败";
        }

        if (AppConfig.MISS_K3_RANK_4_6.equals(missCode)) {
            return "实际号码落在K3外第4-6层";
        }

        if (AppConfig.MISS_TREND_BROKEN.equals(missCode)) {
            return "趋势破位";
        }

        if (AppConfig.MISS_FALSE_RECOVERY.equals(missCode)) {
            return "假恢复";
        }

        if (AppConfig.MISS_DRIFT_SPIKE.equals(missCode)) {
            return "漂移突增";
        }

        if (AppConfig.MISS_HOT_REVERSE.equals(missCode)) {
            return "热号反杀";
        }

        if (AppConfig.MISS_MARKOV_FAILED.equals(missCode)) {
            return "马尔可夫预测失效";
        }

        if (AppConfig.MISS_THIRD_WEAK.equals(missCode)) {
            return "第三号码层偏弱";
        }

        if (AppConfig.MISS_SAMPLE_WEAK.equals(missCode)) {
            return "样本证据偏弱";
        }

        if (AppConfig.MISS_DATA_OFFSET.equals(missCode)) {
            return "数据错位";
        }

        if (AppConfig.MISS_GATE_TOO_LOW.equals(missCode)) {
            return "门槛过低导致误出手";
        }

        if (AppConfig.MISS_CALIBRATION_FAILED.equals(missCode)) {
            return "校准失效";
        }

        if (AppConfig.MISS_UNKNOWN.equals(missCode)) {
            return "未知错因";
        }

        if (AppConfig.MISS_POSITION_DRIFT.equals(missCode)) {
            return "位置漂移导致未中";
        }

        if (AppConfig.MISS_TREND_BREAK.equals(missCode)) {
            return "趋势破坏导致未中";
        }

        if (AppConfig.MISS_FALSE_RECOVERY_DEEP.equals(missCode)) {
            return "深度假恢复导致未中";
        }

        if (AppConfig.MISS_CYCLE_OFFSET.equals(missCode)) {
            return "周期偏移导致未中";
        }

        if (AppConfig.MISS_TRANSITION_FAIL.equals(missCode)) {
            return "转移关系失效";
        }

        if (AppConfig.MISS_ACTUAL_IN_LAYER_4_6.equals(missCode)) {
            return "实际号码落在第4-6层";
        }

        if (AppConfig.MISS_CALIBRATION_OVER.equals(missCode)) {
            return "校准过度自信";
        }

        if (AppConfig.MISS_STATE_SWITCH.equals(missCode)) {
            return "状态切换";
        }

        if (AppConfig.MISS_PROFILE_STALE.equals(missCode)) {
            return "画像陈旧";
        }

        if (AppConfig.MISS_HIGH_FREQ_OVERHEAT.equals(missCode)) {
            return "高频过热";
        }

        if (AppConfig.MISS_CHAOS_NOISE.equals(missCode)) {
            return "混沌噪声";
        }

        return translateLooseText(missCode);
    }

    public static String hitReasonText(String hitCode) {
        if (hitCode == null || hitCode.length() == 0) {
            return "--";
        }

        if (AppConfig.HIT_TREND_CONFIRMED.equals(hitCode)) {
            return "趋势确认命中";
        }

        if (AppConfig.HIT_POSITION_STRENGTH.equals(hitCode)) {
            return "位置强度命中";
        }

        if (AppConfig.HIT_TOP3_MASS.equals(hitCode)) {
            return "Top3集中度命中";
        }

        if (AppConfig.HIT_CYCLE_POINT.equals(hitCode)) {
            return "周期点命中";
        }

        if (AppConfig.HIT_TRANSITION_CHAIN.equals(hitCode)) {
            return "转移链命中";
        }

        if (AppConfig.HIT_RECOVERY.equals(hitCode)) {
            return "恢复形态命中";
        }

        if (AppConfig.HIT_COMBO_MEMORY.equals(hitCode)) {
            return "组合记忆命中";
        }

        if (AppConfig.HIT_DEEP_PROFILE.equals(hitCode)) {
            return "深度画像命中";
        }

        if (AppConfig.HIT_LOW_DRIFT.equals(hitCode)) {
            return "低漂移命中";
        }

        if (AppConfig.HIT_A_PLUS_SHAPE.equals(hitCode)) {
            return "A+形态命中";
        }

        return translateLooseText(hitCode);
    }

    public static String apiLineText(Context context, String apiLine) {
        return apiLineText(apiLine);
    }

    public static String apiLineText(String apiLine) {
        if (AppConfig.API_LINE_PRIMARY.equals(apiLine)) {
            return "主接口";
        }

        if (AppConfig.API_LINE_BACKUP.equals(apiLine)) {
            return "备用接口";
        }

        return safeText(apiLine);
    }

    public static String backtestModeText(String mode) {
        if (AppConfig.BACKTEST_MODE_FAST.equals(mode)) {
            return "快线回测";
        }

        if (AppConfig.BACKTEST_MODE_NORMAL.equals(mode)) {
            return "普通回测";
        }

        if (AppConfig.BACKTEST_MODE_FULL.equals(mode)) {
            return "完整深度回测";
        }

        return safeText(mode);
    }

    public static String backtestStatusText(String status) {
        if (AppConfig.BACKTEST_STATUS_IDLE.equals(status) || "IDLE".equals(status)) {
            return "空闲";
        }

        if (AppConfig.BACKTEST_STATUS_WAITING.equals(status)) {
            return "等待中";
        }

        if (AppConfig.BACKTEST_STATUS_RUNNING.equals(status) || "RUNNING".equals(status)) {
            return "运行中";
        }

        if (AppConfig.BACKTEST_STATUS_YIELDING.equals(status)) {
            return "让出实时任务";
        }

        if (AppConfig.BACKTEST_STATUS_APPLYING.equals(status)) {
            return "应用学习结果";
        }

        if (AppConfig.BACKTEST_STATUS_OK.equals(status) || "OK".equals(status)) {
            return "正常完成";
        }

        if (AppConfig.BACKTEST_STATUS_ERROR.equals(status) || "ERROR".equals(status)) {
            return "异常";
        }

        if (AppConfig.BACKTEST_STATUS_CANCELLED.equals(status)) {
            return "已取消";
        }

        if ("SKIP".equals(status)) {
            return "跳过";
        }

        if ("BUSY".equals(status)) {
            return "已有任务运行";
        }

        if ("FINISHED".equals(status)) {
            return "已完成";
        }

        return safeText(status);
    }

    public static String biasStatusText(String status) {
        if (AppConfig.BIAS_STATUS_STRONG.equals(status)) {
            return "强偏差证据";
        }

        if (AppConfig.BIAS_STATUS_WEAK.equals(status)) {
            return "偏差证据偏弱";
        }

        if (AppConfig.BIAS_STATUS_NOISY.equals(status)) {
            return "噪声偏高";
        }

        if (AppConfig.BIAS_STATUS_NONE.equals(status)) {
            return "无明显偏差";
        }

        return safeText(status);
    }

    public static String aPlusStatusText(String status) {
        if (AppConfig.A_PLUS_CANDIDATE.equals(status)) {
            return "A+候选";
        }

        if (AppConfig.A_PLUS_WATCH.equals(status)) {
            return "A+观察";
        }

        if (AppConfig.A_PLUS_READY.equals(status)) {
            return "A+准备";
        }

        if (AppConfig.A_PLUS_ACTIVE.equals(status)) {
            return "A+强证据";
        }

        if (AppConfig.A_PLUS_STALE.equals(status)) {
            return "A+陈旧";
        }

        if (AppConfig.A_PLUS_RECOVERING.equals(status)) {
            return "A+恢复中";
        }

        if (AppConfig.A_PLUS_NONE.equals(status)) {
            return "非A+";
        }

        return safeText(status);
    }

    public static String promotionStatusText(String status) {
        if (AppConfig.PROMOTION_CANDIDATE.equals(status)) {
            return "晋级候选";
        }

        if (AppConfig.PROMOTION_CHALLENGER.equals(status)) {
            return "挑战者";
        }

        if (AppConfig.PROMOTION_SHADOW_OBSERVING.equals(status)) {
            return "影子观察";
        }

        if (AppConfig.PROMOTION_PROMOTED.equals(status)) {
            return "晋级通过";
        }

        if (AppConfig.PROMOTION_LIMITED_ACTIVE.equals(status)) {
            return "有限激活";
        }

        if (AppConfig.PROMOTION_ACTIVE.equals(status)) {
            return "激活";
        }

        if (AppConfig.PROMOTION_STALE.equals(status)) {
            return "晋级陈旧";
        }

        if (AppConfig.PROMOTION_REJECTED.equals(status)) {
            return "晋级拒绝";
        }

        if ("A_PLUS".equals(status)) {
            return "A+强证据";
        }

        if ("WAIT".equals(status)) {
            return "等待更多证据";
        }

        return safeText(status);
    }

    public static String aiJudgeText(String advice) {
        if (AppConfig.AI_JUDGE_BET.equals(advice)) {
            return "AI建议出手";
        }

        if (AppConfig.AI_JUDGE_HOLD.equals(advice)) {
            return "AI建议HOLD";
        }

        if (AppConfig.AI_JUDGE_SHADOW.equals(advice)) {
            return "AI建议影子观察";
        }

        if (AppConfig.AI_JUDGE_CONFLICT.equals(advice)) {
            return "AI冲突";
        }

        if (AppConfig.AI_JUDGE_RISK.equals(advice)) {
            return "AI风险";
        }

        return safeText(advice);
    }

    public static String diagnosisLevelText(String level) {
        if (AppConfig.DIAG_LEVEL_OK.equals(level)) {
            return "正常";
        }

        if (AppConfig.DIAG_LEVEL_WARN.equals(level)) {
            return "警告";
        }

        if (AppConfig.DIAG_LEVEL_DANGER.equals(level)) {
            return "危险";
        }

        if (AppConfig.DIAG_LEVEL_CRITICAL.equals(level)) {
            return "严重";
        }

        if ("BAD".equals(level)) {
            return "异常";
        }

        if ("FATAL".equals(level)) {
            return "致命";
        }

        return safeText(level);
    }

    public static int diagnosisLevelColor(String level) {
        if (AppConfig.DIAG_LEVEL_OK.equals(level)) {
            return R.color.colorWin;
        }

        if (AppConfig.DIAG_LEVEL_WARN.equals(level)) {
            return R.color.colorWarn;
        }

        if (AppConfig.DIAG_LEVEL_DANGER.equals(level)
                || AppConfig.DIAG_LEVEL_CRITICAL.equals(level)
                || "BAD".equals(level)
                || "FATAL".equals(level)) {
            return R.color.colorMiss;
        }

        return R.color.colorTextSub;
    }

    public static String statScopeText(String scope) {
        if (AppConfig.STAT_SCOPE_REAL_TRADE.equals(scope)
                || AppConfig.STAT_SCOPE_REAL.equals(scope)) {
            return "真实BET";
        }

        if (AppConfig.STAT_SCOPE_SHADOW.equals(scope)) {
            return "影子观察";
        }

        if (AppConfig.STAT_SCOPE_HOLD.equals(scope)) {
            return "HOLD观望";
        }

        if (AppConfig.STAT_SCOPE_BACKTEST.equals(scope)) {
            return "后台回测";
        }

        if (AppConfig.STAT_SCOPE_MANUAL_BACKTEST.equals(scope)) {
            return "手动回测";
        }

        return safeText(scope);
    }

    public static String auditStatusText(String status) {
        if ("PASS".equals(status)) {
            return "审计通过";
        }

        if ("WARN".equals(status)) {
            return "审计警告";
        }

        if ("BLOCK".equals(status)) {
            return "审计拦截";
        }

        if ("ERROR".equals(status)) {
            return "审计异常";
        }

        if ("NO_FUTURE_LEAK".equals(status)) {
            return "无未来数据泄露";
        }

        if ("FUTURE_LEAK_BLOCK".equals(status)) {
            return "未来数据阻断";
        }

        if (AppConfig.PROB_BUCKET_OK.equals(status)) {
            return "概率桶正常";
        }

        if (AppConfig.PROB_BUCKET_UNDER.equals(status)) {
            return "预测偏保守";
        }

        if (AppConfig.PROB_BUCKET_OVER.equals(status)) {
            return "预测偏高";
        }

        if (AppConfig.PROB_BUCKET_DANGEROUS_OVER.equals(status)) {
            return "危险过度自信";
        }

        if (AppConfig.PROB_BUCKET_SAMPLE_WEAK.equals(status)) {
            return "审计样本不足";
        }

        return safeText(status);
    }

    public static String schedulerDecisionText(String code) {
        if ("ALLOW".equals(code) || "OK".equals(code)) {
            return "允许执行";
        }

        if ("BLOCK".equals(code)) {
            return "禁止执行";
        }

        if ("YIELD_REALTIME".equals(code) || "REALTIME_PRIORITY".equals(code)) {
            return "实时抓取优先";
        }

        if ("BUSY".equals(code) || "WORKER_BUSY".equals(code)) {
            return "已有任务运行";
        }

        if ("COOLDOWN".equals(code)) {
            return "冷却中";
        }

        if ("NEAR_OPEN_WINDOW".equals(code)) {
            return "接近开奖窗口";
        }

        if ("AFTER_OPEN_WINDOW".equals(code)) {
            return "开奖后实时窗口";
        }

        return safeText(code);
    }

    public static String statsLine1(int betCount, int hitCount) {
        int miss = betCount - hitCount;
        if (miss < 0) {
            miss = 0;
        }
        return "真实出手 " + betCount + "  命中 " + hitCount + "  未中 " + miss;
    }

    public static String statsLine1(Models.RuntimeStats stats) {
        if (stats == null) {
            return "真实出手 0  命中 0  未中 0";
        }

        int bet = readInt(stats, "realTradeCount", -1);
        int win = readInt(stats, "realWinCount", -1);
        int miss = readInt(stats, "realMissCount", -1);

        if (bet < 0) {
            bet = stats.betCount;
        }

        if (win < 0) {
            win = stats.hitCount;
        }

        if (miss < 0) {
            miss = bet - win;
        }

        if (miss < 0) {
            miss = 0;
        }

        return "真实出手 " + bet + "  命中 " + win + "  未中 " + miss;
    }

    public static String statsLine2(double hitRate, double freq100, double freq500) {
        return "胜率 "
                + probability(hitRate)
                + "  近100期频率 "
                + probability(freq100)
                + "  近500期频率 "
                + probability(freq500);
    }

    public static String statsLine2(Models.RuntimeStats stats) {
        if (stats == null) {
            return "胜率 --  频率 --  盈亏 --";
        }

        double hitRate = readDouble(stats, "realWinRate", -1.0d);
        double freq = readDouble(stats, "realFrequency", -1.0d);
        double pnl = readDouble(stats, "realPnl", Double.NaN);

        if (hitRate < 0.0d) {
            hitRate = stats.hitRate;
        }

        if (freq < 0.0d) {
            double f100 = readDouble(stats, "freq100", -1.0d);
            double f500 = readDouble(stats, "freq500", -1.0d);
            if (f100 >= 0.0d) {
                freq = f100;
            } else if (f500 >= 0.0d) {
                freq = f500;
            } else {
                freq = 0.0d;
            }
        }

        if (Double.isNaN(pnl) || Double.isInfinite(pnl)) {
            pnl = stats.pnl;
        }

        return "真实胜率 "
                + probability(hitRate)
                + "  真实频率 "
                + probability(freq)
                + "  真实盈亏 "
                + money(pnl);
    }

    public static String statsLine3(double pnl, double maxDrawdown, int loseStreak) {
        return "盈亏 " + money(pnl) + "  最大回撤 " + money(maxDrawdown) + "  连续未中 " + loseStreak;
    }

    public static String statsLine3(Models.RuntimeStats stats) {
        if (stats == null) {
            return "最大回撤 --  HOLD/SHADOW不计真实胜率";
        }

        double pnl = readDouble(stats, "realPnl", stats.pnl);
        double maxDrawdown = readDouble(stats, "realMaxDrawdown", stats.maxDrawdown);
        int loseStreak = readInt(stats, "realLoseStreak", stats.loseStreak);

        return "真实盈亏 "
                + money(pnl)
                + "  最大回撤 "
                + money(maxDrawdown)
                + "  连续未中 "
                + loseStreak
                + "  HOLD/SHADOW不计真实胜率";
    }

    public static String probabilityLine(double finalP, double gate, double edge) {
        return "最终概率 "
                + probability(finalP)
                + "  出手门槛 "
                + probability(gate)
                + "  概率优势 "
                + signedProbability(edge);
    }

    public static String probabilityLine(Models.Decision d) {
        if (d == null) {
            return "最终概率 --  出手门槛 --  概率优势 --";
        }

        double edge = d.edge;
        if (edge == 0.0d && d.finalP > 0.0d && d.dynamicGate > 0.0d) {
            edge = d.finalP - d.dynamicGate;
        }

        return probabilityLine(d.finalP, d.dynamicGate, edge);
    }

    public static String shapeLine(
            double trendLife, double driftRisk, double breakDepth, double recoveryScore) {
        return "趋势寿命 "
                + score(trendLife)
                + "  漂移风险 "
                + score(driftRisk)
                + "  破位深度 "
                + score(breakDepth)
                + "  恢复分 "
                + score(recoveryScore);
    }

    public static String shapeLine(Models.Decision d) {
        if (d == null) {
            return "趋势寿命 --  漂移风险 --  破位深度 --  恢复分 --";
        }

        return shapeLine(d.trendLife, d.driftRisk, d.breakDepth, d.recoveryScore);
    }

    public static String sampleLine(
            int effectiveSample, double similarHitRate, double comboHitRate) {
        return "有效样本 "
                + effectiveSample
                + "  相似命中率 "
                + probability(similarHitRate)
                + "  组合命中率 "
                + probability(comboHitRate);
    }

    public static String sampleLine(Models.Decision d) {
        if (d == null) {
            return "有效样本 --  相似命中率 --  组合命中率 --";
        }

        return sampleLine(d.effectiveSample, d.similarHitRate, d.comboHitRate);
    }

    public static String deepLine(Models.Decision d) {
        if (d == null) {
            return "深度分 --  证据分 --  风险 --";
        }

        return "深度分 "
                + score(d.deepScore)
                + "  证据分 "
                + score(d.evidenceScore)
                + "  证据风险 "
                + score(d.evidenceRisk)
                + "  AI "
                + score(d.aiJudgeScore)
                + "  置信 "
                + score(d.aiJudgeConfidence);
    }

    public static String backtestSummary(Models.BacktestReport report) {
        if (report == null) {
            return "暂无回测结果";
        }

        report.recalc();

        String mode =
                report.mode == null || report.mode.length() == 0
                        ? ""
                        : backtestModeText(report.mode) + " ";

        int total = report.totalCount > 0 ? report.totalCount : report.testCount;
        double winRate = report.winRate > 0.0d ? report.winRate : report.hitRate;

        return mode
                + "窗口="
                + report.window
                + "  出手="
                + report.betCount
                + "/"
                + total
                + "  频率="
                + probability(report.frequency)
                + "  胜率="
                + probability(winRate)
                + "  盈亏="
                + money(report.pnl)
                + "  回撤="
                + money(report.maxDrawdown);
    }

    public static String decisionShortLine(Models.Decision d) {
        if (d == null) {
            return "暂无决策";
        }

        StringBuilder sb = new StringBuilder();

        sb.append(decisionText(d.decision));

        if (d.position > 0 && Models.isValidK3Mask(d.k3Mask)) {
            sb.append(" ");
            sb.append(positionAndNumbers(d.position, d.n1, d.n2, d.n3));
        }

        sb.append(" ");
        sb.append(probabilityLine(d));

        if (d.blockCode != null && d.blockCode.length() > 0) {
            sb.append(" 拦截=");
            sb.append(blockReasonText(d.blockCode));
        }

        return sanitizeForUi(sb.toString());
    }

    public static String decisionTargetCheckLine(Models.Decision d, String runtimeTargetIssue) {
        if (d == null) {
            return "当前目标期 " + issue(runtimeTargetIssue) + "｜暂无正式预测";
        }

        String dTarget = d.targetIssue == null ? "" : d.targetIssue;
        String rTarget = runtimeTargetIssue == null ? "" : runtimeTargetIssue;

        if (rTarget.length() > 0 && dTarget.length() > 0 && !rTarget.equals(dTarget)) {
            return "目标期错位：运行目标期 " + rTarget + "，决策目标期 " + dTarget + "｜拒绝用旧预测冒充当前预测";
        }

        return "目标期 " + issue(dTarget.length() > 0 ? dTarget : rTarget) + "｜预测已对齐";
    }

    public static String tradeShortLine(Models.Trade t) {
        if (t == null) {
            return "暂无交易";
        }

        StringBuilder sb = new StringBuilder();

        sb.append(resultText(t.result));
        sb.append(" ");
        sb.append(positionAndNumbers(t.position, t.n1, t.n2, t.n3));

        if (t.actualNumber > 0) {
            sb.append(" 实际=");
            sb.append(number(t.actualNumber));
        }

        sb.append(" 盈亏=");
        sb.append(money(t.pnl));

        if (AppConfig.RESULT_MISS.equals(normalizeResultText(t.result))
                && t.missReason != null
                && t.missReason.length() > 0) {
            sb.append(" 错因=");
            sb.append(missReasonText(t.missReason));
        }

        return sanitizeForUi(sb.toString());
    }

    public static String tradeSettlementLine(Models.Trade t) {
        if (t == null) {
            return "暂无真实交易";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("真实交易：");
        sb.append(resultText(t.result));
        sb.append("｜目标期=");
        sb.append(issue(t.targetIssue));
        sb.append("｜");
        sb.append(positionAndNumbers(t.position, t.n1, t.n2, t.n3));

        if (t.actualNumber > 0) {
            sb.append("｜实际=");
            sb.append(number(t.actualNumber));
        }

        sb.append("｜盈亏=");
        sb.append(money(t.pnl));

        if (t.statScope != null && t.statScope.length() > 0) {
            sb.append("｜口径=");
            sb.append(statScopeText(t.statScope));
        }

        if (t.settled <= 0) {
            sb.append("｜等待结算");
        }

        if (AppConfig.RESULT_MISS.equals(normalizeResultText(t.result))
                && t.missReason != null
                && t.missReason.length() > 0) {
            sb.append("｜错因=");
            sb.append(missReasonText(t.missReason));
        }

        return sanitizeForUi(sb.toString());
    }

    public static String decisionSettlementLine(Models.Decision d) {
        if (d == null) {
            return "暂无决策";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("决策：");
        sb.append(decisionText(d.decision));
        sb.append("｜目标期=");
        sb.append(issue(d.targetIssue));

        if (d.position > 0 && Models.isValidK3Mask(d.k3Mask)) {
            sb.append("｜");
            sb.append(positionAndNumbers(d.position, d.n1, d.n2, d.n3));
        }

        if (isTerminalResult(d.result)) {
            sb.append("｜");
            sb.append(resultText(d.result));
        } else if (AppConfig.DECISION_BET.equals(normalizeDecisionText(d.decision))) {
            sb.append("｜等待结算");
        } else {
            sb.append("｜不计真实统计");
        }

        int actual = readInt(d, "actualNumber", 0);
        if (actual > 0) {
            sb.append("｜实际=");
            sb.append(number(actual));
        }

        double pnl = readDouble(d, "pnl", 0.0d);
        if (pnl != 0.0d) {
            sb.append("｜盈亏=");
            sb.append(money(pnl));
        }

        return sanitizeForUi(sb.toString());
    }

    public static boolean isTerminalResult(String result) {
        String r = normalizeResultText(result);
        return AppConfig.RESULT_WIN.equals(r)
                || AppConfig.RESULT_MISS.equals(r)
                || AppConfig.RESULT_DATA_ERROR.equals(r)
                || AppConfig.RESULT_VOID.equals(r);
    }

    public static boolean isWinResult(String result) {
        return AppConfig.RESULT_WIN.equals(normalizeResultText(result));
    }

    public static boolean isMissResult(String result) {
        return AppConfig.RESULT_MISS.equals(normalizeResultText(result));
    }

    public static String candidateLine(Models.ComboCandidate c) {
        if (c == null) {
            return "暂无候选";
        }

        StringBuilder sb = new StringBuilder();

        if (c.rank > 0) {
            sb.append("#");
            sb.append(c.rank);
            sb.append(" ");
        }

        sb.append(positionAndNumbers(c.position, c.n1, c.n2, c.n3));
        sb.append(" FinalP=");
        sb.append(probability(c.finalP));
        sb.append(" Gate=");
        sb.append(probability(c.dynamicGate));
        sb.append(" Edge=");
        sb.append(signedProbability(c.edge));
        sb.append(" Evidence=");
        sb.append(score(c.evidenceScore));
        sb.append(" Risk=");
        sb.append(score(c.evidenceRisk));

        if (c.blockCode != null && c.blockCode.length() > 0) {
            sb.append(" Block=");
            sb.append(blockReasonText(c.blockCode));
        }

        return sanitizeForUi(sb.toString());
    }

    public static String runtimeDataLine(int drawCount, int remain240, int gapCount) {
        StringBuilder sb = new StringBuilder();

        sb.append("历史数据：");
        sb.append(drawCount);
        sb.append("期");

        if (remain240 > 0) {
            sb.append("｜距30期受控启动还差");
            sb.append(remain240);
            sb.append("期");
        } else {
            sb.append("｜已满足30期受控启动门槛");
        }

        if (gapCount > 0) {
            sb.append("｜疑似缺口");
            sb.append(gapCount);
            sb.append("期");
        } else {
            sb.append("｜近期无明显缺口");
        }

        return sb.toString();
    }

    public static String frequencyHealthText(double freq100, double freq500) {
        double f100 = cleanDouble(freq100);
        double f500 = cleanDouble(freq500);

        if (f100 > AppConfig.TARGET_FREQ_HARD_MAX || f500 > AppConfig.TARGET_FREQ_HARD_MAX) {
            return "频率硬超上限，必须强保护";
        }

        if (f100 > AppConfig.TARGET_FREQ_MAX || f500 > AppConfig.TARGET_FREQ_MAX) {
            return "频率偏高，应提高门槛";
        }

        if (f100 >= AppConfig.TARGET_FREQ_IDEAL_LOW && f100 <= AppConfig.TARGET_FREQ_MID) {
            return "频率处于理想区间";
        }

        if (f100 < AppConfig.TARGET_FREQ_MIN) {
            return "频率偏低，可等待强形态";
        }

        return "频率可接受";
    }

    public static String hitRateHealthText(double hitRate) {
        double hr = cleanDouble(hitRate);

        if (hr >= AppConfig.TARGET_HIT_RATE_IDEAL) {
            return "胜率达到理想线";
        }

        if (hr >= AppConfig.TARGET_HIT_RATE_GOOD) {
            return "胜率达到良好线";
        }

        if (hr >= AppConfig.TARGET_HIT_RATE_MIN) {
            return "胜率达到最低目标线";
        }

        if (hr > 0.0d) {
            return "胜率低于目标线";
        }

        return "暂无真实BET样本";
    }

    public static String gateHealthText(double finalP, double gate, double edge) {
        if (finalP >= gate && edge >= 0.0d) {
            return "概率通过门槛";
        }

        if (finalP < gate) {
            return "概率未过门槛";
        }

        return "概率优势不足";
    }

    public static String realtimeValidationText(Models.BacktestReport report) {
        if (report == null) {
            return "实时轻量验证：暂无结果，不阻塞实时出号";
        }

        report.recalc();

        String status = backtestStatusText(report.status);
        int total = report.totalCount > 0 ? report.totalCount : report.testCount;

        return "实时轻量验证："
                + status
                + " W"
                + report.window
                + " 出手="
                + report.betCount
                + "/"
                + total
                + " 频率="
                + probability(report.frequency)
                + " 胜率="
                + probability(report.hitRate)
                + " 盈亏="
                + money(report.pnl)
                + "｜只修正门槛，不阻塞实时出号";
    }

    public static String manualBacktestText(Models.ManualDeepBacktestReport report) {
        if (report == null) {
            return "手动深度回测：暂无报告";
        }

        report.recalc();

        return "手动深度回测："
                + backtestStatusText(report.status)
                + " W"
                + report.windowSize
                + " 出手="
                + report.betCount
                + "/"
                + report.testCount
                + " 频率="
                + probability(report.frequency)
                + " 胜率="
                + probability(report.hitRate)
                + " 盈亏="
                + money(report.pnl)
                + " 回撤="
                + money(report.maxDrawdown)
                + "｜完成后的学习快照可反哺实时预测";
    }

    public static String learningSnapshotText(Models.RuntimeLearningSnapshot snapshot) {
        if (snapshot == null) {
            return "学习快照：暂无";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("学习快照 ");
        sb.append(dashIfEmpty(snapshot.snapshotId));
        sb.append("｜样本=");
        sb.append(snapshot.totalSample);
        sb.append("｜总命中率=");
        sb.append(probability(snapshot.totalHitRate));
        sb.append("｜近样本=");
        sb.append(snapshot.recentSample);
        sb.append("｜近命中率=");
        sb.append(probability(snapshot.recentHitRate));

        if (snapshot.summaryText != null && snapshot.summaryText.length() > 0) {
            sb.append("｜");
            sb.append(snapshot.summaryText);
        }

        return sanitizeForUi(sb.toString());
    }

    public static String runtimeStateLine(Models.AppRuntimeState state) {
        if (state == null) {
            return "服务状态 --｜当前期 --｜目标期 --";
        }

        return "服务状态 "
                + serviceStatusText(state.serviceStatus)
                + "｜当前期 "
                + issue(state.currentIssue)
                + "｜目标期 "
                + issue(state.targetIssue)
                + "｜倒计时 "
                + countdown(state.countdownMs)
                + "｜接口 "
                + apiLineText(state.apiLine);
    }

    public static String blockCodeText(String code) {
        return blockReasonText(code);
    }

    public static String fieldFinalP() {
        return "最终概率";
    }

    public static String fieldGate() {
        return "出手门槛";
    }

    public static String fieldEdge() {
        return "概率优势";
    }

    public static String fieldTrendLife() {
        return "趋势寿命";
    }

    public static String fieldDriftRisk() {
        return "漂移风险";
    }

    public static String fieldBreakDepth() {
        return "破位深度";
    }

    public static String fieldRecovery() {
        return "恢复分";
    }

    public static String fieldSimilar() {
        return "相似命中率";
    }

    public static String fieldSample() {
        return "有效样本";
    }

    public static String fieldComboHr() {
        return "组合命中率";
    }

    public static String fieldChampionGap() {
        return "主优势差";
    }

    public static String fieldComboGap() {
        return "组合优势差";
    }

    public static String yesNo(boolean value) {
        return value ? "是" : "否";
    }

    public static String passFail(boolean value) {
        return value ? "通过" : "未通过";
    }

    public static String holdShadowNote() {
        return "HOLD/SHADOW只学习观察，不计真实胜率";
    }

    public static String realStatsNote() {
        return "真实胜率/真实盈亏只统计 trades 表中已结算的正式 BET，HOLD 和 SHADOW 不参与真实统计";
    }

    public static String fixedRuleNote() {
        return "硬规则：单位置 + 固定K3；禁止K4、动态K、多位置、加倍、马丁、追期";
    }

    public static String realtimePriorityNote() {
        return "实时出号优先；手动深测、后台健康检查、完整回测不能卡住实时预测";
    }

    public static String sanitizeForUi(String text) {
        if (text == null || text.length() == 0) {
            return "";
        }

        String s = text;
        s = maskQueryParam(s, "key");
        s = maskQueryParam(s, "token");
        s = maskQueryParam(s, "secret");
        s = maskQueryParam(s, "password");
        s = maskQueryParam(s, "sign");
        s = maskQueryParam(s, "auth");
        s = maskInlineSecret(s, "key");
        s = maskInlineSecret(s, "token");
        s = maskInlineSecret(s, "secret");
        s = maskInlineSecret(s, "password");
        s = maskInlineSecret(s, "sign");
        s = maskInlineSecret(s, "auth");
        return s;
    }

    public static String translateLooseText(String text) {
        if (text == null || text.length() == 0) {
            return "";
        }

        String s = sanitizeForUi(text);

        s = s.replace("REALTIME_TIMEOUT_FAST_SAVE", "实时超时已保存快车道结果");
        s = s.replace("TARGET_ISSUE_MISMATCH", "目标期错位");
        s = s.replace("PLACEHOLDER_NOT_REAL", "临时占位不计真实统计");
        s = s.replace("STRICT_SINGLE_POSITION_K3_FAST_CATCHUP", "严格单位置K3补扫");
        s = s.replace("STRICT_SINGLE_POSITION_K3", "严格单位置K3");
        s = s.replace("REAL_TRADE", "真实BET");
        s = s.replace("RUNNING", "运行中");
        s = s.replace("STOPPED", "已停止");
        s = s.replace("FETCHING", "抓取中");
        s = s.replace("ANALYZING", "分析中");
        s = s.replace("SETTLING", "结算中");
        s = s.replace("LEARNING", "学习中");
        s = s.replace("BACKTESTING", "回测中");
        s = s.replace("BACKTEST_FAST", "快速回测");
        s = s.replace("BACKTEST_NORMAL", "普通回测");
        s = s.replace("BACKTEST_FULL", "完整深测");
        s = s.replace("AI_JUDGING", "AI裁决中");
        s = s.replace("DEEP_ANALYZING", "深度分析中");
        s = s.replace("DIAGNOSING", "自检诊断中");
        s = s.replace("WAITING", "等待中");
        s = s.replace("DELAY", "延迟等待");
        s = s.replace("API_ERROR", "接口异常");
        s = s.replace("DATA_ERROR", "数据异常");

        s = s.replace("FAST", "快线");
        s = s.replace("NORMAL", "普通");
        s = s.replace("FULL", "完整深度");
        s = s.replace("IDLE", "空闲");
        s = s.replace("BUSY", "忙碌");
        s = s.replace("SKIP", "跳过");
        s = s.replace("FINISHED", "已完成");
        s = s.replace("ERROR", "异常");
        s = s.replace("OK", "正常");

        s = s.replace("FinalP", "最终概率");
        s = s.replace("RawP", "原始概率");
        s = s.replace("CalP", "校准概率");
        s = s.replace("Gate", "出手门槛");
        s = s.replace("FinalGate", "最终门槛");
        s = s.replace("Edge", "概率优势");
        s = s.replace("Block", "拦截原因");

        s = s.replace("TrendLife", "趋势寿命");
        s = s.replace("DriftRisk", "漂移风险");
        s = s.replace("BreakDepth", "破位深度");
        s = s.replace("RecoveryPower", "恢复力量");
        s = s.replace("Recovery", "恢复分");
        s = s.replace("BreakRisk", "断点风险");
        s = s.replace("FalseRecovery", "假恢复风险");

        s = s.replace("Similar", "相似形态");
        s = s.replace("Sample", "样本");
        s = s.replace("ComboHR", "组合命中率");
        s = s.replace("ChampionGap", "主优势差");
        s = s.replace("ComboGap", "组合优势差");

        s = s.replace("SHADOW", "影子观察");
        s = s.replace("HOLD", "不出手");
        s = s.replace("BET", "出手");
        s = s.replace("HIT", "命中");
        s = s.replace("WIN", "命中");
        s = s.replace("LOSE", "未中");
        s = s.replace("LOSS", "未中");
        s = s.replace("MISS", "未中");
        s = s.replace("WAIT", "等待");
        s = s.replace("VOID", "作废");

        s = s.replace("CHAOS", "无有效形态");
        s = s.replace("FORMING", "形态形成中");
        s = s.replace("CONFIRMED", "形态已确认");
        s = s.replace("ACCELERATING", "趋势加速");
        s = s.replace("PULLBACK", "趋势回拉");
        s = s.replace("RECOVERING", "恢复中");
        s = s.replace("WEAKENING", "趋势转弱");
        s = s.replace("BROKEN", "形态破位");
        s = s.replace("REVERSING", "反转风险");
        s = s.replace("REVERSAL", "反转风险");
        s = s.replace("STABLE", "稳定形态");
        s = s.replace("TRANSITION", "过渡形态");

        s = s.replace("HISTORY_WEAK", "历史数据不足");
        s = s.replace("LOW_PROBABILITY", "概率不足");
        s = s.replace("SAMPLE_WEAK", "样本不足");
        s = s.replace("TREND_WEAK", "趋势偏弱");
        s = s.replace("DRIFT_HIGH", "漂移过高");
        s = s.replace("BREAK_HIGH", "破位过深");
        s = s.replace("RECOVERY_WEAK", "恢复不足");
        s = s.replace("POSITION_WEAK", "位置优势不足");
        s = s.replace("COMBO_WEAK", "组合优势不足");
        s = s.replace("FREQ_HIGH", "频率过高");
        s = s.replace("DATA_OFFSET", "数据错位");
        s = s.replace("OPEN_TRADE_EXISTS", "已有未结算交易");

        s = s.replace("MARKOV_FAILED", "Markov失效");
        s = s.replace("TREND_BROKEN", "趋势破坏");
        s = s.replace("DRIFT_SPIKE", "漂移突增");
        s = s.replace("HOT_REVERSE", "热号反杀");
        s = s.replace("FALSE_RECOVERY", "假恢复");
        s = s.replace("THIRD_WEAK", "第三号偏弱");
        s = s.replace("K3_RANK_4_6", "落在第4-6名");
        s = s.replace("POSITION_LOST", "位置优势丢失");
        s = s.replace("UNKNOWN", "未知原因");

        s = s.replace("PASS", "通过");
        s = s.replace("BLOCK", "拦截");
        s = s.replace("WARN", "警告");
        s = s.replace("INFO", "提示");

        s = s.replace("A_PLUS", "A+形态");
        s = s.replace("PROMOTION", "晋级门");
        s = s.replace("AUDIT", "审计");
        s = s.replace("SNAPSHOT", "快照");
        s = s.replace("NO_FUTURE_LEAK", "无未来泄露");
        s = s.replace("FUTURE_LEAK_BLOCK", "未来数据阻断");

        return s;
    }

    public static String normalizeDecisionText(String decision) {
        if (decision == null) {
            return AppConfig.DECISION_HOLD;
        }

        String d = decision.trim();

        if (d.length() == 0) {
            return AppConfig.DECISION_HOLD;
        }

        if (AppConfig.DECISION_BET.equals(d)
                || AppConfig.ACTION_BET.equals(d)
                || "REAL_BET".equals(d)
                || "REAL_TRADE".equals(d)
                || "出手".equals(d)
                || "下注".equals(d)) {
            return AppConfig.DECISION_BET;
        }

        if (AppConfig.DECISION_SHADOW.equals(d)
                || AppConfig.ACTION_SHADOW.equals(d)
                || "SHADOW_BET".equals(d)
                || "影子".equals(d)
                || "影子观察".equals(d)) {
            return AppConfig.DECISION_SHADOW;
        }

        if (AppConfig.DECISION_HOLD.equals(d)
                || AppConfig.ACTION_HOLD.equals(d)
                || "NO_BET".equals(d)
                || "WAIT".equals(d)
                || "PASS".equals(d)
                || "不出手".equals(d)
                || "等待".equals(d)) {
            return AppConfig.DECISION_HOLD;
        }

        return d;
    }

    public static String normalizeResultText(String result) {
        if (result == null) {
            return AppConfig.RESULT_WAIT;
        }

        String r = result.trim();

        if (r.length() == 0) {
            return AppConfig.RESULT_WAIT;
        }

        if (AppConfig.RESULT_WIN.equals(r) || "HIT".equals(r) || "命中".equals(r) || "中".equals(r)) {
            return AppConfig.RESULT_WIN;
        }

        if (AppConfig.RESULT_MISS.equals(r)
                || "LOSE".equals(r)
                || "LOSS".equals(r)
                || "未中".equals(r)
                || "不中".equals(r)) {
            return AppConfig.RESULT_MISS;
        }

        if (AppConfig.RESULT_DATA_ERROR.equals(r) || "DATA_ERROR".equals(r) || "数据异常".equals(r)) {
            return AppConfig.RESULT_DATA_ERROR;
        }

        if (AppConfig.RESULT_VOID.equals(r) || "VOID".equals(r) || "作废".equals(r)) {
            return AppConfig.RESULT_VOID;
        }

        if (AppConfig.RESULT_WAIT.equals(r)
                || "WAIT".equals(r)
                || "PENDING".equals(r)
                || "等待结算".equals(r)) {
            return AppConfig.RESULT_WAIT;
        }

        return r;
    }

    private static String maskQueryParam(String input, String key) {
        if (input == null || input.length() == 0 || key == null || key.length() == 0) {
            return input;
        }

        String lower = input.toLowerCase(Locale.US);
        String find = key.toLowerCase(Locale.US) + "=";
        int index = lower.indexOf(find);

        if (index < 0) {
            return input;
        }

        StringBuilder sb = new StringBuilder();
        int start = 0;

        while (index >= 0) {
            int valueStart = index + find.length();
            int valueEnd = input.length();

            int amp = input.indexOf("&", valueStart);
            int space = input.indexOf(" ", valueStart);
            int newline = input.indexOf("\n", valueStart);
            int tab = input.indexOf("\t", valueStart);

            if (amp >= 0 && amp < valueEnd) {
                valueEnd = amp;
            }

            if (space >= 0 && space < valueEnd) {
                valueEnd = space;
            }

            if (newline >= 0 && newline < valueEnd) {
                valueEnd = newline;
            }

            if (tab >= 0 && tab < valueEnd) {
                valueEnd = tab;
            }

            sb.append(input.substring(start, valueStart));
            sb.append("***");

            start = valueEnd;

            lower = input.toLowerCase(Locale.US);
            index = lower.indexOf(find, start);
        }

        sb.append(input.substring(start));
        return sb.toString();
    }

    private static String maskInlineSecret(String input, String key) {
        if (input == null || input.length() == 0 || key == null || key.length() == 0) {
            return input;
        }

        String lower = input.toLowerCase(Locale.US);
        String find1 = key.toLowerCase(Locale.US) + ":";
        String find2 = key.toLowerCase(Locale.US) + "：";

        String out = maskInlineSecretByToken(input, lower, find1);
        out = maskInlineSecretByToken(out, out.toLowerCase(Locale.US), find2);
        return out;
    }

    private static String maskInlineSecretByToken(String input, String lower, String find) {
        int index = lower.indexOf(find);
        if (index < 0) {
            return input;
        }

        StringBuilder sb = new StringBuilder();
        int start = 0;

        while (index >= 0) {
            int valueStart = index + find.length();

            while (valueStart < input.length()) {
                char ch = input.charAt(valueStart);
                if (ch == ' ' || ch == '\t') {
                    valueStart++;
                } else {
                    break;
                }
            }

            int valueEnd = input.length();
            int amp = input.indexOf("&", valueStart);
            int space = input.indexOf(" ", valueStart);
            int newline = input.indexOf("\n", valueStart);
            int tab = input.indexOf("\t", valueStart);
            int comma = input.indexOf(",", valueStart);
            int semi = input.indexOf(";", valueStart);

            if (amp >= 0 && amp < valueEnd) valueEnd = amp;
            if (space >= 0 && space < valueEnd) valueEnd = space;
            if (newline >= 0 && newline < valueEnd) valueEnd = newline;
            if (tab >= 0 && tab < valueEnd) valueEnd = tab;
            if (comma >= 0 && comma < valueEnd) valueEnd = comma;
            if (semi >= 0 && semi < valueEnd) valueEnd = semi;

            sb.append(input.substring(start, valueStart));
            sb.append("***");

            start = valueEnd;
            lower = input.toLowerCase(Locale.US);
            index = lower.indexOf(find, start);
        }

        sb.append(input.substring(start));
        return sb.toString();
    }

    private static int getColor(Context context, int colorRes) {
        if (context == null) {
            return 0xff94a3b8;
        }

        try {
            if (Build.VERSION.SDK_INT >= 23) {
                return context.getResources().getColor(colorRes, context.getTheme());
            }
            return context.getResources().getColor(colorRes);
        } catch (Throwable e) {
            return 0xff94a3b8;
        }
    }

    private static double cleanDouble(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0d;
        }

        return value;
    }

    private static int readInt(Object obj, String fieldName, int fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v instanceof Number) {
                return ((Number) v).intValue();
            }

            if (v instanceof String) {
                return Integer.parseInt((String) v);
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private static double readDouble(Object obj, String fieldName, double fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v instanceof Number) {
                double d = ((Number) v).doubleValue();
                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    return fallback;
                }
                return d;
            }

            if (v instanceof String) {
                double d = Double.parseDouble((String) v);
                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    return fallback;
                }
                return d;
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    public static String v48ModelStatus(String status) {
        String s = status == null ? "" : status;
        if ("CHAMPION".equals(s)) return "模型已通过单动作走前验证";
        if (s.length() == 0) return "模型状态未知";
        return "模型未证明：" + s + "；继续诊断观察，不以降低门槛制造出手";
    }
}
