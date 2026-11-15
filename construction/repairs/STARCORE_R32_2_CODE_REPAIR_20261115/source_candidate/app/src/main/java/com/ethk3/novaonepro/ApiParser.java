package com.ethk3.novaonepro;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public final class ApiParser {

    private static final String TAG = "ApiParser";

    private ApiParser() {}

    public static Models.ApiResult parseLatest(String json, String sourceLine) {
        if (json == null || json.trim().length() == 0) {
            return Models.ApiResult.fail("EMPTY_JSON", "接口返回为空");
        }

        try {
            Object root = new JSONTokener(json).nextValue();
            String responseGameError=BinanceGameContract.providerGameIdentityError(root);
            if(!responseGameError.isEmpty())return Models.ApiResult.fail(
                    responseGameError.startsWith("GAME_MISMATCH:")?"GAME_MISMATCH":"GAME_IDENTITY_UNVERIFIED",responseGameError);

            Models.ApiResult duoqu = parseDuoquLatest(root, json, sourceLine);
            if (duoqu != null) {
                return duoqu;
            }

            JSONObject drawObj = findFirstDrawObject(root, 0);

            if (drawObj == null) {
                return Models.ApiResult.fail("DRAW_NOT_FOUND", "未找到开奖对象");
            }

            String gameError=BinanceGameContract.declaredGameIdentityError(drawObj);
            if(!gameError.isEmpty())return Models.ApiResult.fail("GAME_MISMATCH",gameError);
            Models.Draw draw = parseDrawObject(drawObj, sourceLine, null);

            if (draw == null || !draw.isValid()) {
                return Models.ApiResult.fail("DRAW_INVALID", "开奖数据不完整或号码非法");
            }

            Models.ApiResult result = Models.ApiResult.ok(draw, sourceLine);
            result.ok = true;
            result.success = true;
            result.rawJson = json;
            result.apiLine = sourceLine == null ? AppConfig.API_LINE_PRIMARY : sourceLine;
            result.sourceLine = result.apiLine;
            result.fromBackup = AppConfig.API_LINE_BACKUP.equals(result.sourceLine);

            ArrayList<Models.Draw> list = new ArrayList<Models.Draw>();
            list.add(draw);
            result.history = list;
            result.draws = list;

            return result;
        } catch (Exception e) {
            SafeLog.e(TAG, "parse latest error", e);
            return Models.ApiResult.fail("PARSE_LATEST_ERROR", SafeLog.sanitize(e.getMessage()));
        }
    }

    public static Models.ApiResult parseHistory(String json, String sourceLine) {
        if (json == null || json.trim().length() == 0) {
            return Models.ApiResult.fail("EMPTY_JSON", "接口返回为空");
        }

        try {
            Object root = new JSONTokener(json).nextValue();
            String responseGameError=BinanceGameContract.providerGameIdentityError(root);
            if(!responseGameError.isEmpty())return Models.ApiResult.fail(
                    responseGameError.startsWith("GAME_MISMATCH:")?"GAME_MISMATCH":"GAME_IDENTITY_UNVERIFIED",responseGameError);

            Models.ApiResult duoqu = parseDuoquHistory(root, json, sourceLine);
            if (duoqu != null) {
                return duoqu;
            }

            JSONArray array = findDrawArray(root, 0);
            ArrayList<Models.Draw> list = new ArrayList<Models.Draw>();

            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    Object item = array.opt(i);
                    JSONObject obj = null;

                    if (item instanceof JSONObject) {
                        obj = (JSONObject) item;
                    } else if (item instanceof String) {
                        obj = tryStringAsJsonObject((String) item);
                    }

                    if (obj == null) {
                        continue;
                    }

                    String gameError=BinanceGameContract.declaredGameIdentityError(obj);
                    if(!gameError.isEmpty())return Models.ApiResult.fail("GAME_MISMATCH",gameError);
                    Models.Draw draw = parseDrawObject(obj, sourceLine, null);

                    if (draw != null && draw.isValid()) {
                        list.add(draw);
                    }
                }
            }

            if (list.size() == 0) {
                JSONObject single = findFirstDrawObject(root, 0);

                if (single != null) {
                    String gameError=BinanceGameContract.declaredGameIdentityError(single);
                    if(!gameError.isEmpty())return Models.ApiResult.fail("GAME_MISMATCH",gameError);
                    Models.Draw draw = parseDrawObject(single, sourceLine, null);

                    if (draw != null && draw.isValid()) {
                        list.add(draw);
                    }
                }
            }

            if (list.size() == 0) {
                return Models.ApiResult.fail("HISTORY_EMPTY", "历史开奖解析为空");
            }

            sortDrawsByTimeAndIssue(list);

            Models.ApiResult result = new Models.ApiResult();
            result.success = true;
            result.ok = true;
            result.sourceLine = sourceLine == null ? AppConfig.API_LINE_PRIMARY : sourceLine;
            result.apiLine = result.sourceLine;
            result.fromBackup = AppConfig.API_LINE_BACKUP.equals(result.sourceLine);
            result.history = list;
            result.draws = list;
            result.latestDraw = list.get(list.size() - 1);
            result.rawJson = json;

            return result;
        } catch (Exception e) {
            SafeLog.e(TAG, "parse history error", e);
            return Models.ApiResult.fail("PARSE_HISTORY_ERROR", SafeLog.sanitize(e.getMessage()));
        }
    }

    private static Models.ApiResult parseDuoquLatest(
            Object root, String rawJson, String sourceLine) {
        ArrayList<Models.Draw> list = parseDuoquDrawList(root, rawJson, sourceLine);

        if (list == null) {
            return null;
        }

        if (list.size() == 0) {
            return Models.ApiResult.fail("DUOQU_DATA_EMPTY", "多趣data为空");
        }

        sortDrawsByTimeAndIssue(list);

        Models.Draw latest = list.get(list.size() - 1);

        Models.ApiResult result = new Models.ApiResult();
        result.success = true;
        result.ok = true;
        result.sourceLine = sourceLine == null ? AppConfig.API_LINE_PRIMARY : sourceLine;
        result.apiLine = result.sourceLine;
        result.fromBackup = AppConfig.API_LINE_BACKUP.equals(result.sourceLine);
        result.latestDraw = latest;
        result.history = list;
        result.draws = list;
        result.rawJson = rawJson == null ? "" : rawJson;

        return result;
    }

    private static Models.ApiResult parseDuoquHistory(
            Object root, String rawJson, String sourceLine) {
        ArrayList<Models.Draw> list = parseDuoquDrawList(root, rawJson, sourceLine);

        if (list == null) {
            return null;
        }

        if (list.size() == 0) {
            return Models.ApiResult.fail("DUOQU_DATA_EMPTY", "多趣data为空");
        }

        sortDrawsByTimeAndIssue(list);

        Models.ApiResult result = new Models.ApiResult();
        result.success = true;
        result.ok = true;
        result.sourceLine = sourceLine == null ? AppConfig.API_LINE_PRIMARY : sourceLine;
        result.apiLine = result.sourceLine;
        result.fromBackup = AppConfig.API_LINE_BACKUP.equals(result.sourceLine);
        result.history = list;
        result.draws = list;
        result.latestDraw = list.get(list.size() - 1);
        result.rawJson = rawJson == null ? "" : rawJson;

        return result;
    }

    private static ArrayList<Models.Draw> parseDuoquDrawList(
            Object root, String rawJson, String sourceLine) {
        if (!(root instanceof JSONObject)) {
            return null;
        }

        JSONObject jo = (JSONObject) root;

        if (!jo.has("data")) {
            return null;
        }

        BinanceGameContract.EnvelopeValidation envelope = BinanceGameContract.validateEnvelope(jo);
        if (!envelope.valid) {
            ArrayList<Models.Draw> rejected = new ArrayList<Models.Draw>();
            SafeLog.w(TAG, "duoqu envelope rejected: " + envelope.code + "|" + envelope.detail);
            return rejected;
        }

        String info = jo.optString("info", "");

        if (info != null && info.length() > 0 && !"success".equalsIgnoreCase(info)) {
            ArrayList<Models.Draw> empty = new ArrayList<Models.Draw>();
            SafeLog.w(TAG, "duoqu info not success: " + SafeLog.sanitize(info));
            return empty;
        }

        Object dataObj = jo.opt("data");

        JSONArray arr = null;

        if (dataObj instanceof JSONArray) {
            arr = (JSONArray) dataObj;
        } else if (dataObj instanceof JSONObject) {
            arr = new JSONArray();
            arr.put(dataObj);
        } else if (dataObj instanceof String) {
            String s = ((String) dataObj).trim();

            try {
                if (s.startsWith("[")) {
                    arr = new JSONArray(s);
                } else if (s.startsWith("{")) {
                    arr = new JSONArray();
                    arr.put(new JSONObject(s));
                }
            } catch (Exception e) {
                arr = null;
            }
        }

        if (arr == null) {
            return null;
        }

        ArrayList<Models.Draw> list = new ArrayList<Models.Draw>();

        for (int i = 0; i < arr.length(); i++) {
            Object item = arr.opt(i);
            JSONObject obj = null;

            if (item instanceof JSONObject) {
                obj = (JSONObject) item;
            } else if (item instanceof String) {
                obj = tryStringAsJsonObject((String) item);
            }

            if (obj == null) {
                continue;
            }

            // An explicit row conflict rejects this response as a unit. It cannot
            // be converted to a partial successful history from the remaining rows.
            if(!BinanceGameContract.declaredGameIdentityError(obj).isEmpty())return new ArrayList<Models.Draw>();
            Models.Draw draw = parseStrictBinanceDuoquDrawObject(obj, null, sourceLine);

            if (draw != null && draw.isValid()) {
                list.add(draw);
            }
        }

        return validateStrictDuoquList(list);
    }

    private static Models.Draw parseDuoquDrawObject(
            JSONObject obj, String rawJson, String sourceLine) {
        if (obj == null) {
            return null;
        }

        Models.Draw draw = new Models.Draw();

        draw.issue =
                pickString(
                        obj,
                        "betNo",
                        "issue",
                        "expect",
                        "period",
                        "periods",
                        "issueNo",
                        "issue_no",
                        "drawNo",
                        "draw_no",
                        "no");

        int[] numbers = null;

        if (obj.has("openNumber")) {
            numbers = parseNumbersValue(obj.opt("openNumber"));
        }

        if (!isValidNumbersArray(numbers)) {
            numbers = pickNumbers(obj);
        }

        if (!isValidNumbersArray(numbers)) {
            return null;
        }

        draw.numbers = numbers;
        draw.openTime = parseTimeValue(obj.opt("openTime"));

        if (draw.openTime <= 0L) {
            draw.openTime = pickTime(obj);
        }

        draw.fetchTime = System.currentTimeMillis();
        draw.sourceLine = sourceLine == null ? AppConfig.API_LINE_PRIMARY : sourceLine;
        draw.rawJson = obj.toString();

        if (draw.issue == null || draw.issue.length() == 0) {
            draw.issue = pickString(obj, "gmtBetNo");
        }

        if (draw.issue == null || draw.issue.length() == 0) {
            draw.issue = buildIssueFromTime(draw.openTime);
        }

        if (draw.openTime <= 0L) {
            draw.dirty = 1;draw.dirtyReason = "PROVIDER_OPEN_TIME_MISSING";return null;
        }

        if (!draw.isValid()) {
            draw.dirty = 1;
            draw.dirtyReason = "invalid duoqu draw";
            return null;
        }

        draw.dirty = 0;
        draw.dirtyReason = "";

        return draw;
    }

    public static Models.Draw parseDrawObject(JSONObject obj, String sourceLine, String rawJson) {
        if (obj == null) {
            return null;
        }

        if (obj.has("betNo") || obj.has("openNumber")) {
            return parseDuoquDrawObject(obj, rawJson, sourceLine);
        }

        Models.Draw draw = new Models.Draw();

        draw.issue =
                pickString(
                        obj,
                        "betNo",
                        "issue",
                        "expect",
                        "period",
                        "periods",
                        "issueNo",
                        "issue_no",
                        "turnNum",
                        "turn_num",
                        "round",
                        "drawNo",
                        "draw_no",
                        "no");

        draw.openTime = pickTime(obj);
        draw.fetchTime = System.currentTimeMillis();
        draw.sourceLine = sourceLine == null ? AppConfig.API_LINE_PRIMARY : sourceLine;
        draw.rawJson = obj.toString();

        int[] numbers = pickNumbers(obj);

        if (numbers != null && numbers.length == AppConfig.NUMBER_COUNT) {
            draw.numbers = numbers;
        }

        if (draw.issue == null || draw.issue.length() == 0) {
            draw.issue = buildIssueFromTime(draw.openTime);
        }

        if (draw.openTime <= 0L) {
            draw.dirty = 1;draw.dirtyReason = "PROVIDER_OPEN_TIME_MISSING";return null;
        }

        if (!draw.isValid()) {
            draw.dirty = 1;
            draw.dirtyReason = "invalid draw";
            return null;
        }

        draw.dirty = 0;
        draw.dirtyReason = "";

        return draw;
    }

    private static JSONObject findFirstDrawObject(Object obj, int depth) {
        if (obj == null || depth > 8) {
            return null;
        }

        if (obj instanceof JSONObject) {
            JSONObject jo = (JSONObject) obj;

            if (looksLikeDrawObject(jo)) {
                return jo;
            }

            String[] priorityKeys =
                    new String[] {
                        "data",
                        "result",
                        "draw",
                        "open",
                        "latest",
                        "last",
                        "current",
                        "currentIssue",
                        "current_issue",
                        "item"
                    };

            for (int i = 0; i < priorityKeys.length; i++) {
                if (jo.has(priorityKeys[i])) {
                    JSONObject found = findFirstDrawObject(jo.opt(priorityKeys[i]), depth + 1);

                    if (found != null) {
                        return found;
                    }
                }
            }

            Iterator<String> keys = jo.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                Object child = jo.opt(key);
                JSONObject found = findFirstDrawObject(child, depth + 1);

                if (found != null) {
                    return found;
                }
            }
        } else if (obj instanceof JSONArray) {
            JSONArray arr = (JSONArray) obj;

            for (int i = 0; i < arr.length(); i++) {
                JSONObject found = findFirstDrawObject(arr.opt(i), depth + 1);

                if (found != null) {
                    return found;
                }
            }
        } else if (obj instanceof String) {
            JSONObject jo = tryStringAsJsonObject((String) obj);

            if (jo != null) {
                return findFirstDrawObject(jo, depth + 1);
            }
        }

        return null;
    }

    private static JSONArray findDrawArray(Object obj, int depth) {
        if (obj == null || depth > 8) {
            return null;
        }

        if (obj instanceof JSONArray) {
            JSONArray arr = (JSONArray) obj;

            if (arrayLooksLikeDrawList(arr)) {
                return arr;
            }

            for (int i = 0; i < arr.length(); i++) {
                JSONArray found = findDrawArray(arr.opt(i), depth + 1);

                if (found != null) {
                    return found;
                }
            }
        }

        if (obj instanceof JSONObject) {
            JSONObject jo = (JSONObject) obj;

            String[] listKeys =
                    new String[] {
                        "data",
                        "list",
                        "rows",
                        "history",
                        "records",
                        "record",
                        "items",
                        "result",
                        "openData",
                        "open_data",
                        "draws",
                        "lotteryList",
                        "lottery_list"
                    };

            for (int i = 0; i < listKeys.length; i++) {
                if (jo.has(listKeys[i])) {
                    Object child = jo.opt(listKeys[i]);

                    if (child instanceof JSONArray) {
                        JSONArray arr = (JSONArray) child;

                        if (arrayLooksLikeDrawList(arr)) {
                            return arr;
                        }
                    }

                    JSONArray found = findDrawArray(child, depth + 1);

                    if (found != null) {
                        return found;
                    }
                }
            }

            Iterator<String> keys = jo.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray found = findDrawArray(jo.opt(key), depth + 1);

                if (found != null) {
                    return found;
                }
            }
        } else if (obj instanceof String) {
            JSONObject jo = tryStringAsJsonObject((String) obj);

            if (jo != null) {
                return findDrawArray(jo, depth + 1);
            }
        }

        return null;
    }

    private static boolean looksLikeDrawObject(JSONObject obj) {
        if (obj == null) {
            return false;
        }

        boolean hasIssue =
                hasAny(
                        obj,
                        "betNo",
                        "gmtBetNo",
                        "issue",
                        "expect",
                        "period",
                        "periods",
                        "issueNo",
                        "issue_no",
                        "turnNum",
                        "turn_num",
                        "round",
                        "drawNo",
                        "draw_no",
                        "no");

        boolean hasNumbers =
                hasAny(
                        obj,
                        "openNumber",
                        "openCode",
                        "open_code",
                        "code",
                        "numbers",
                        "nums",
                        "result",
                        "openResult",
                        "open_result",
                        "awardNumbers",
                        "award_numbers",
                        "number",
                        "num");

        boolean hasSeparateNumbers = true;

        for (int i = 1; i <= 10; i++) {
            if (!obj.has("n" + i) && !obj.has("num" + i) && !obj.has("ball" + i)) {
                hasSeparateNumbers = false;
                break;
            }
        }

        return hasIssue && (hasNumbers || hasSeparateNumbers);
    }

    private static boolean arrayLooksLikeDrawList(JSONArray arr) {
        if (arr == null || arr.length() == 0) {
            return false;
        }

        int check = Math.min(arr.length(), 5);
        int hit = 0;

        for (int i = 0; i < check; i++) {
            Object item = arr.opt(i);

            if (item instanceof JSONObject) {
                if (looksLikeDrawObject((JSONObject) item)) {
                    hit++;
                }
            } else if (item instanceof String) {
                JSONObject jo = tryStringAsJsonObject((String) item);

                if (jo != null && looksLikeDrawObject(jo)) {
                    hit++;
                }
            }
        }

        return hit > 0;
    }

    private static int[] pickNumbers(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        String[] arrayOrStringKeys =
                new String[] {
                    "openNumber",
                    "openCode",
                    "open_code",
                    "code",
                    "numbers",
                    "nums",
                    "result",
                    "openResult",
                    "open_result",
                    "awardNumbers",
                    "award_numbers",
                    "number",
                    "num"
                };

        for (int i = 0; i < arrayOrStringKeys.length; i++) {
            String key = arrayOrStringKeys[i];

            if (!obj.has(key)) {
                continue;
            }

            Object value = obj.opt(key);
            int[] parsed = parseNumbersValue(value);

            if (isValidNumbersArray(parsed)) {
                return parsed;
            }
        }

        int[] fromSeparate = parseSeparateNumberFields(obj);

        if (isValidNumbersArray(fromSeparate)) {
            return fromSeparate;
        }

        return null;
    }

    private static int[] parseNumbersValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof JSONArray) {
            JSONArray arr = (JSONArray) value;

            if (arr.length() < AppConfig.NUMBER_COUNT) {
                return null;
            }

            int[] out = new int[AppConfig.NUMBER_COUNT];

            for (int i = 0; i < AppConfig.NUMBER_COUNT; i++) {
                out[i] = parseNumberToken(String.valueOf(arr.opt(i)));
            }

            return out;
        }

        if (value instanceof String) {
            return parseNumbersString((String) value);
        }

        return parseNumbersString(String.valueOf(value));
    }

    private static int[] parseNumbersString(String text) {
        if (text == null) {
            return null;
        }

        String s = text.trim();

        if (s.length() == 0) {
            return null;
        }

        s = s.replace("[", "");
        s = s.replace("]", "");
        s = s.replace("\"", "");
        s = s.replace("'", "");
        s = s.replace("，", ",");
        s = s.replace("、", ",");
        s = s.replace("|", ",");
        s = s.replace("/", ",");
        s = s.replace(";", ",");
        s = s.replace(" ", ",");

        while (s.indexOf(",,") >= 0) {
            s = s.replace(",,", ",");
        }

        String[] parts = s.split(",");
        ArrayList<Integer> nums = new ArrayList<Integer>();

        for (int i = 0; i < parts.length; i++) {
            int n = parseNumberToken(parts[i]);

            if (AppConfig.isValidNumber(n)) {
                nums.add(Integer.valueOf(n));
            }
        }

        if (nums.size() < AppConfig.NUMBER_COUNT) {
            nums.clear();

            String digits = onlyDigits(text);

            if (digits.length() == 20) {
                for (int i = 0; i < 10; i++) {
                    String piece = digits.substring(i * 2, i * 2 + 2);
                    int n = parseNumberToken(piece);

                    if (AppConfig.isValidNumber(n)) {
                        nums.add(Integer.valueOf(n));
                    }
                }
            }
        }

        if (nums.size() != AppConfig.NUMBER_COUNT) {
            return null;
        }

        int[] out = new int[AppConfig.NUMBER_COUNT];

        for (int i = 0; i < out.length; i++) {
            out[i] = nums.get(i).intValue();
        }

        return out;
    }

    private static int[] parseSeparateNumberFields(JSONObject obj) {
        int[] out = new int[AppConfig.NUMBER_COUNT];

        for (int i = 1; i <= AppConfig.NUMBER_COUNT; i++) {
            String[] keys = new String[] {"n" + i, "num" + i, "ball" + i, "position" + i, "p" + i};

            int value = 0;

            for (int k = 0; k < keys.length; k++) {
                if (obj.has(keys[k])) {
                    value = parseNumberToken(String.valueOf(obj.opt(keys[k])));
                    break;
                }
            }

            out[i - 1] = value;
        }

        return out;
    }

    private static boolean isValidNumbersArray(int[] numbers) {
        if (numbers == null || numbers.length != AppConfig.NUMBER_COUNT) {
            return false;
        }

        boolean[] seen = new boolean[AppConfig.NUMBER_COUNT + 1];

        for (int i = 0; i < numbers.length; i++) {
            int n = numbers[i];

            if (!AppConfig.isValidNumber(n)) {
                return false;
            }

            if (seen[n]) {
                return false;
            }

            seen[n] = true;
        }

        return true;
    }

    private static int parseNumberToken(String value) {
        if (value == null) {
            return 0;
        }

        String s = value.trim();

        if (s.length() == 0) {
            return 0;
        }

        s = s.replace("号", "");
        s = s.replace("+", "");
        s = s.replace("\"", "");
        s = s.replace("'", "");

        StringBuilder digits = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);

            if (ch >= '0' && ch <= '9') {
                digits.append(ch);
            }
        }

        if (digits.length() == 0) {
            return 0;
        }

        try {
            return Integer.parseInt(digits.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private static String onlyDigits(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);

            if (ch >= '0' && ch <= '9') {
                sb.append(ch);
            }
        }

        return sb.toString();
    }

    private static String pickString(JSONObject obj, String... keys) {
        if (obj == null || keys == null) {
            return "";
        }

        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];

            if (obj.has(key)) {
                String value = obj.optString(key, "");

                if (value != null && value.trim().length() > 0) {
                    return value.trim();
                }
            }
        }

        return "";
    }

    private static long pickTime(JSONObject obj) {
        if (obj == null) {
            return 0L;
        }

        String[] keys =
                new String[] {
                    "openTime",
                    "open_time",
                    "time",
                    "timestamp",
                    "createTime",
                    "create_time",
                    "drawTime",
                    "draw_time",
                    "date",
                    "datetime",
                    "opentime"
                };

        for (int i = 0; i < keys.length; i++) {
            if (!obj.has(keys[i])) {
                continue;
            }

            Object value = obj.opt(keys[i]);
            long t = parseTimeValue(value);

            if (t > 0L) {
                return t;
            }
        }

        return 0L;
    }

    private static long parseTimeValue(Object value) {
        return ProviderTimeCodecR816.parse(value);
    }

    private static String buildIssueFromTime(long time) {
        long t = time > 0L ? time : System.currentTimeMillis();
        return "T" + String.valueOf(t);
    }

    private static JSONObject tryStringAsJsonObject(String text) {
        if (text == null) {
            return null;
        }

        String s = text.trim();

        if (!s.startsWith("{")) {
            return null;
        }

        try {
            return new JSONObject(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean hasAny(JSONObject obj, String... keys) {
        if (obj == null || keys == null) {
            return false;
        }

        for (int i = 0; i < keys.length; i++) {
            if (obj.has(keys[i])) {
                return true;
            }
        }

        return false;
    }

    private static void sortDrawsByTimeAndIssue(List<Models.Draw> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<Models.Draw>() {
                    @Override
                    public int compare(Models.Draw a, Models.Draw b) {
                        if (a == null && b == null) {
                            return 0;
                        }

                        if (a == null) {
                            return -1;
                        }

                        if (b == null) {
                            return 1;
                        }

                        if (a.openTime < b.openTime) {
                            return -1;
                        }

                        if (a.openTime > b.openTime) {
                            return 1;
                        }

                        String ia = a.issue == null ? "" : a.issue;
                        String ib = b.issue == null ? "" : b.issue;

                        return ia.compareTo(ib);
                    }
                });
    }

    /* STARCORE_TIANSHU_B01_MATERIALIZED_20260805 */
    private static Models.Draw parseStrictBinanceDuoquDrawObject(
            JSONObject obj, String rawJson, String sourceLine) {
        BinanceGameContract.ProviderDraw provider = BinanceGameContract.parseProviderDraw(obj);
        if (provider == null || !provider.valid) {
            SafeLog.w(
                    TAG, "provider draw rejected: " + (provider == null ? "NULL" : provider.error));
            return null;
        }
        Models.Draw draw = new Models.Draw();
        draw.issue = provider.issue;
        draw.numbers = provider.numbers;
        draw.openTime = provider.openTimeMillis;
        draw.fetchTime = System.currentTimeMillis();
        draw.sourceLine = sourceLine == null ? AppConfig.API_LINE_PRIMARY : sourceLine;
        draw.rawJson = obj.toString();
        if (!draw.isValid()) {
            draw.dirty = 1;
            draw.dirtyReason = "strict binance draw invalid";
            return null;
        }
        draw.dirty = 0;
        draw.dirtyReason = "";
        return draw;
    }

    private static ArrayList<Models.Draw> validateStrictDuoquList(ArrayList<Models.Draw> input) {
        ArrayList<Models.Draw> clean = new ArrayList<Models.Draw>();
        if (input == null) return clean;
        sortDrawsByTimeAndIssue(input);
        String previousIssue = "";
        long previousTime = 0L;
        for (int i = 0; i < input.size(); i++) {
            Models.Draw draw = input.get(i);
            if (draw == null || !draw.isValid() || !BinanceGameContract.isPermutation(draw.numbers))
                continue;
            if (previousIssue.length() > 0
                    && BinanceGameContract.compareIssue(previousIssue, draw.issue) >= 0) continue;
            if (previousTime > 0L && draw.openTime < previousTime) continue;
            clean.add(draw);
            previousIssue = draw.issue;
            previousTime = draw.openTime;
        }
        return clean;
    }

    public static boolean tianshuParserSelfTest() {
        int[] numbers = new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        return BinanceGameContract.GAME_CODE.equals(AppConfig.LOTTERY_CODE)
                && BinanceGameContract.isPermutation(numbers)
                && BinanceGameContract.selfTest();
    }
}
