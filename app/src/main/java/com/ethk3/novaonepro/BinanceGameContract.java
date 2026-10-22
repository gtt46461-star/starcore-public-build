package com.ethk3.novaonepro;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Immutable Binance1-PK10 and Duoqu /data contract. It owns no strategy, probability or BET
 * decision.
 */
public final class BinanceGameContract {
    public static final String MATERIALIZATION_MARKER =
            "STARCORE_TIANSHU_B01_MATERIALIZED_20260805";
    public static final String GAME_CODE = "Binance1-PK10";
    public static final String DATA_PATH = "/data";
    public static final String PRIMARY_BASE = "https://api.duoqu.online";
    public static final String BACKUP_BASE = "https://api1.duoqu.online";
    public static final int NUMBER_MIN = 1;
    public static final int NUMBER_MAX = 10;
    public static final int NUMBER_COUNT = 10;
    public static final int FIXED_K = 3;
    public static final int MAX_LATEST_LIMIT = 100;
    public static final int DEFAULT_LATEST_LIMIT = 20;
    public static final long MAX_FUTURE_SKEW_MS = 120000L;
    public static final long MAX_LATEST_AGE_MS = 600000L;

    private BinanceGameContract() {}

    public enum RequestKind {
        LATEST,
        DATE_BACKFILL
    }

    public static final class EndpointContract {
        public final String id;
        public final String base;
        public final String path;
        public final boolean primary;

        EndpointContract(String id, String base, String path, boolean primary) {
            this.id = id;
            this.base = base;
            this.path = path;
            this.primary = primary;
        }

        public String endpoint() {
            return normalizeBase(base) + DATA_PATH;
        }

        public boolean valid() {
            return base.startsWith("https://") && DATA_PATH.equals(path);
        }
    }

    public static EndpointContract primary() {
        return new EndpointContract("PRIMARY", PRIMARY_BASE, DATA_PATH, true);
    }

    public static EndpointContract backup() {
        return new EndpointContract("BACKUP", BACKUP_BASE, DATA_PATH, false);
    }

    public static final class RequestSpec {
        public final RequestKind kind;
        public final int limit;
        public final String date;
        public final long createdAt;
        public final String requestId;
        public final String gameCode;

        private RequestSpec(
                RequestKind kind, int limit, String date, long createdAt, String requestId) {
            this.kind = kind;
            this.limit = limit;
            this.date = safe(date);
            this.createdAt = createdAt;
            this.requestId = safe(requestId);
            this.gameCode = GAME_CODE;
        }

        public static RequestSpec latest(int limit) {
            int normalized = limit < 1 ? DEFAULT_LATEST_LIMIT : Math.min(MAX_LATEST_LIMIT, limit);
            long now = System.currentTimeMillis();
            return new RequestSpec(
                    RequestKind.LATEST,
                    normalized,
                    "",
                    now,
                    hash("LATEST|" + normalized + '|' + now));
        }

        public static RequestSpec date(String date) {
            long now = System.currentTimeMillis();
            return new RequestSpec(
                    RequestKind.DATE_BACKFILL,
                    0,
                    normalizeDate(date),
                    now,
                    hash("DATE|" + normalizeDate(date) + '|' + now));
        }

        public boolean valid() {
            if (kind == null || !GAME_CODE.equals(gameCode)) return false;
            if (kind == RequestKind.LATEST)
                return limit >= 1 && limit <= MAX_LATEST_LIMIT && date.length() == 0;
            return kind == RequestKind.DATE_BACKFILL && limit == 0 && isIsoDate(date);
        }

        public String canonical() {
            return kind + "|" + gameCode + "|" + limit + "|" + date + "|" + requestId;
        }
    }

    public static String buildUrl(EndpointContract endpoint, RequestSpec request, String key) {
        if (endpoint == null || request == null || !endpoint.valid() || !request.valid()) return "";
        StringBuilder sb = new StringBuilder(endpoint.endpoint());
        sb.append("?code=").append(enc(GAME_CODE));
        if (request.kind == RequestKind.LATEST) sb.append("&limit=").append(request.limit);
        else sb.append("&date=").append(enc(request.date));
        sb.append("&key=").append(enc(key));
        return sb.toString();
    }

    public static String sanitizedUrl(EndpointContract endpoint, RequestSpec request) {
        return SafeLog.sanitizeUrl(buildUrl(endpoint, request, "[SECRET]"));
    }

    public static final class EnvelopeValidation {
        public final boolean valid;
        public final String code;
        public final String detail;
        public final int dataCount;

        EnvelopeValidation(boolean valid, String code, String detail, int dataCount) {
            this.valid = valid;
            this.code = code;
            this.detail = detail;
            this.dataCount = dataCount;
        }
    }

    /** Current provider namespace is immutable. Every explicit alias must agree;
     * an absent legacy declaration is not upgraded into proof of provider identity. */
    public static String declaredGameIdentityError(JSONObject object) {
        if(object==null)return "";
        for(String field:new String[]{"lotteryCode","lottery_code","gameCode","game_code"}){
            if(!object.has(field)||object.isNull(field))continue;
            String declared=object.optString(field,"").trim();
            if(!declared.isEmpty()&&!GAME_CODE.equals(declared))return "GAME_MISMATCH:"+field;
        }
        return "";
    }

    /** Validate parsed provider containers before any generic envelope fallback.
     * Bounds match a data response, not exported reports or arbitrary nested text. */
    public static String providerGameIdentityError(Object root) {
        return providerGameIdentityError(root,0,new int[]{0});
    }
    private static String providerGameIdentityError(Object value,int depth,int[] containers) {
        if(!(value instanceof JSONObject)&&!(value instanceof JSONArray))return "";
        if(depth>16||++containers[0]>100000)return "GAME_IDENTITY_SCOPE_BOUND";
        if(value instanceof JSONObject){
            JSONObject object=(JSONObject)value;
            String own=declaredGameIdentityError(object);if(!own.isEmpty())return own;
            java.util.Iterator<String> keys=object.keys();
            while(keys.hasNext()){
                String error=providerGameIdentityError(object.opt(keys.next()),depth+1,containers);
                if(!error.isEmpty())return error;
            }
        }else{
            JSONArray array=(JSONArray)value;
            for(int i=0;i<array.length();i++){
                String error=providerGameIdentityError(array.opt(i),depth+1,containers);
                if(!error.isEmpty())return error;
            }
        }
        return "";
    }

    public static EnvelopeValidation validateEnvelope(JSONObject root) {
        if (root == null) return new EnvelopeValidation(false, "ROOT_NULL", "root=null", 0);
        String info = root.optString("info", "");
        if (info.length() > 0 && !"success".equalsIgnoreCase(info))
            return new EnvelopeValidation(false, "INFO_NOT_SUCCESS", SafeLog.sanitize(info), 0);
        String gameError = providerGameIdentityError(root);
        if (!gameError.isEmpty())
            return new EnvelopeValidation(false, "GAME_MISMATCH", gameError, 0);
        Object data = root.opt("data");
        int count =
                data instanceof JSONArray
                        ? ((JSONArray) data).length()
                        : (data instanceof JSONObject ? 1 : 0);
        if (count <= 0) return new EnvelopeValidation(false, "DATA_EMPTY", "dataCount=0", 0);
        return new EnvelopeValidation(true, "OK", GAME_CODE, count);
    }

    public static final class ProviderDraw {
        public final String rawBetNo;
        public final String gmtBetNo;
        public final String issue;
        public final int[] numbers;
        public final String block;
        public final String hashValue;
        public final long openTimeMillis;
        public final boolean valid;
        public final String error;

        ProviderDraw(
                String rawBetNo,
                String gmtBetNo,
                String issue,
                int[] numbers,
                String block,
                String hashValue,
                long openTimeMillis,
                boolean valid,
                String error) {
            this.rawBetNo = safe(rawBetNo);
            this.gmtBetNo = safe(gmtBetNo);
            this.issue = safe(issue);
            this.numbers = numbers == null ? new int[0] : copy(numbers);
            this.block = safe(block);
            this.hashValue = safe(hashValue);
            this.openTimeMillis = openTimeMillis;
            this.valid = valid;
            this.error = safe(error);
        }

        public String canonical() {
            StringBuilder sb = new StringBuilder();
            sb.append(rawBetNo).append('|').append(gmtBetNo).append('|').append(issue).append('|');
            for (int i = 0; i < numbers.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(numbers[i]);
            }
            return sb.append('|')
                    .append(block)
                    .append('|')
                    .append(hashValue)
                    .append('|')
                    .append(openTimeMillis)
                    .toString();
        }

        public String fingerprint() {
            return hash(canonical());
        }
    }

    public static ProviderDraw parseProviderDraw(JSONObject obj) {
        if (obj == null) return invalidDraw("OBJECT_NULL");
        String gameError=providerGameIdentityError(obj);
        if(!gameError.isEmpty())return invalidDraw(gameError);
        String rawBetNo = first(obj, "betNo", "issue", "expect");
        String gmtBetNo = first(obj, "gmtBetNo", "gmt_issue");
        String issue = rawBetNo.length() > 0 ? rawBetNo : gmtBetNo;
        int[] numbers = parseOpenNumber(obj.opt("openNumber"));
        String block = first(obj, "block", "blockNo", "height");
        String hashValue = first(obj, "hash", "blockHash", "block_hash");
        long openTime = parseOpenTime(obj.opt("openTime"));
        if (issue.length() == 0) return invalidDraw("ISSUE_EMPTY");
        if (!isPermutation(numbers)) return invalidDraw("NUMBER_PERMUTATION_INVALID");
        if (openTime <= 0L) return invalidDraw("OPEN_TIME_INVALID");
        if (openTime > System.currentTimeMillis() + MAX_FUTURE_SKEW_MS)
            return invalidDraw("OPEN_TIME_FUTURE");
        return new ProviderDraw(
                rawBetNo, gmtBetNo, issue, numbers, block, hashValue, openTime, true, "");
    }

    private static ProviderDraw invalidDraw(String error) {
        return new ProviderDraw("", "", "", new int[0], "", "", 0L, false, error);
    }

    public static int[] parseOpenNumber(Object value) {
        if (value == null) return new int[0];
        ArrayList<Integer> values = new ArrayList<Integer>();
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) addNumber(values, array.opt(i));
        } else {
            String normalized =
                    String.valueOf(value)
                            .trim()
                            .replace('[', ' ')
                            .replace(']', ' ')
                            .replace('，', ',')
                            .replace('|', ',')
                            .replace(';', ',')
                            .replace('-', ',');
            String[] parts = normalized.split("[,\\s]+");
            for (int i = 0; i < parts.length; i++) addNumber(values, parts[i]);
        }
        int[] out = new int[values.size()];
        for (int i = 0; i < values.size(); i++) out[i] = values.get(i).intValue();
        return out;
    }

    private static void addNumber(ArrayList<Integer> values, Object value) {
        if (value == null) return;
        try {
            values.add(Integer.valueOf(String.valueOf(value).trim()));
        } catch (Exception ignored) {
        }
    }

    public static boolean isPermutation(int[] numbers) {
        if (numbers == null || numbers.length != NUMBER_COUNT) return false;
        boolean[] seen = new boolean[NUMBER_MAX + 1];
        for (int i = 0; i < numbers.length; i++) {
            int n = numbers[i];
            if (n < NUMBER_MIN || n > NUMBER_MAX || seen[n]) return false;
            seen[n] = true;
        }
        return true;
    }

    public static final class DrawListValidation {
        public final boolean valid;
        public final String error;
        public final int duplicateCount;
        public final int orderErrors;

        DrawListValidation(boolean valid, String error, int duplicateCount, int orderErrors) {
            this.valid = valid;
            this.error = error;
            this.duplicateCount = duplicateCount;
            this.orderErrors = orderErrors;
        }
    }

    public static DrawListValidation validateDrawList(List<ProviderDraw> draws) {
        if (draws == null || draws.size() == 0) return new DrawListValidation(false, "EMPTY", 0, 0);
        Set<String> issues = new HashSet<String>();
        int duplicates = 0;
        int orderErrors = 0;
        String previous = "";
        for (int i = 0; i < draws.size(); i++) {
            ProviderDraw draw = draws.get(i);
            if (draw == null || !draw.valid)
                return new DrawListValidation(false, "INVALID_ITEM_" + i, duplicates, orderErrors);
            if (!issues.add(draw.issue)) duplicates++;
            if (previous.length() > 0 && compareIssue(previous, draw.issue) >= 0) orderErrors++;
            previous = draw.issue;
        }
        return new DrawListValidation(
                duplicates == 0 && orderErrors == 0,
                duplicates > 0 ? "DUPLICATE_ISSUE" : (orderErrors > 0 ? "ISSUE_ORDER" : "OK"),
                duplicates,
                orderErrors);
    }

    public static int compareIssue(String left, String right) {
        String a = digits(left);
        String b = digits(right);
        if (a.length() != b.length()) return a.length() < b.length() ? -1 : 1;
        int cmp = a.compareTo(b);
        if (cmp != 0) return cmp;
        return safe(left).compareTo(safe(right));
    }

    public static long parseOpenTime(Object value) {
        return ProviderTimeCodecR816.parse(value);
    }

    public static String normalizeDate(String date) {
        String value = safe(date).trim();
        if (isIsoDate(value)) return value;
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static boolean isIsoDate(String date) {
        return safe(date).matches("\\d{4}-\\d{2}-\\d{2}");
    }

    public static boolean isValidDate(String date) {
        String value = safe(date).trim();
        if (!isIsoDate(value)) return false;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            format.setLenient(false);
            Date parsed = format.parse(value);
            return parsed != null && value.equals(format.format(parsed));
        } catch (ParseException ignored) {
            return false;
        }
    }

    private static String first(JSONObject obj, String a, String b) {
        String value = obj.optString(a, "");
        return value.length() > 0 ? value : obj.optString(b, "");
    }

    private static String first(JSONObject obj, String a, String b, String c) {
        String value = first(obj, a, b);
        return value.length() > 0 ? value : obj.optString(c, "");
    }

    private static String normalizeBase(String base) {
        String value = safe(base).trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    private static String enc(String value) {
        try {
            return URLEncoder.encode(safe(value), "UTF-8");
        } catch (Exception error) {
            return "";
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String digits(String value) {
        return safe(value).replaceAll("[^0-9]", "");
    }

    private static int[] copy(int[] values) {
        int[] out = new int[values.length];
        System.arraycopy(values, 0, out, 0, values.length);
        return out;
    }

    private static String hash(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(safe(value).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (int i = 0; i < bytes.length; i++) {
                int v = bytes[i] & 255;
                if (v < 16) sb.append('0');
                sb.append(Integer.toHexString(v));
            }
            return sb.toString();
        } catch (Exception error) {
            return "HASH_ERROR";
        }
    }

    public static final class RuleSpec {
        public final String id;
        public final String field;
        public final String group;
        public final boolean mandatory;

        RuleSpec(String id, String field, String group, boolean mandatory) {
            this.id = id;
            this.field = field;
            this.group = group;
            this.mandatory = mandatory;
        }
    }

    public static List<RuleSpec> rules() {
        ArrayList<RuleSpec> rules = new ArrayList<RuleSpec>();
        rules.add(new RuleSpec("TIANSHU-GAME-001", "GAME_CODE", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-002", "DATA_PATH", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-003", "PRIMARY_ENDPOINT", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-004", "BACKUP_ENDPOINT", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-005", "PARAM_CODE", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-006", "PARAM_LIMIT", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-007", "PARAM_DATE", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-008", "PARAM_KEY", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-009", "LOTTERY_CODE", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-010", "INFO", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-011", "DATA", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-012", "BET_NO", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-013", "GMT_BET_NO", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-014", "OPEN_NUMBER", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-015", "BLOCK", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-016", "HASH", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-017", "OPEN_TIME", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-018", "PERMUTATION", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-019", "NUMBER_RANGE", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-020", "NO_DUPLICATES", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-021", "ISSUE_ORDER", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-022", "TIME_ORDER", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-023", "FRESHNESS", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-024", "PROVIDER_TIME", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-025", "RAW_ARCHIVE", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-026", "REDACTION", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-027", "FINGERPRINT", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-028", "SINGLE_POSITION", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-029", "FIXED_K3", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-030", "NO_BET_LOGIC", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-031", "GAME_ISOLATION", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-032", "SHADOW_ONLY", "GROUP_01", true));
        rules.add(new RuleSpec("TIANSHU-GAME-033", "GAME_CODE", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-034", "DATA_PATH", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-035", "PRIMARY_ENDPOINT", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-036", "BACKUP_ENDPOINT", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-037", "PARAM_CODE", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-038", "PARAM_LIMIT", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-039", "PARAM_DATE", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-040", "PARAM_KEY", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-041", "LOTTERY_CODE", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-042", "INFO", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-043", "DATA", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-044", "BET_NO", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-045", "GMT_BET_NO", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-046", "OPEN_NUMBER", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-047", "BLOCK", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-048", "HASH", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-049", "OPEN_TIME", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-050", "PERMUTATION", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-051", "NUMBER_RANGE", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-052", "NO_DUPLICATES", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-053", "ISSUE_ORDER", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-054", "TIME_ORDER", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-055", "FRESHNESS", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-056", "PROVIDER_TIME", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-057", "RAW_ARCHIVE", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-058", "REDACTION", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-059", "FINGERPRINT", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-060", "SINGLE_POSITION", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-061", "FIXED_K3", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-062", "NO_BET_LOGIC", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-063", "GAME_ISOLATION", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-064", "SHADOW_ONLY", "GROUP_02", true));
        rules.add(new RuleSpec("TIANSHU-GAME-065", "GAME_CODE", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-066", "DATA_PATH", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-067", "PRIMARY_ENDPOINT", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-068", "BACKUP_ENDPOINT", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-069", "PARAM_CODE", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-070", "PARAM_LIMIT", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-071", "PARAM_DATE", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-072", "PARAM_KEY", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-073", "LOTTERY_CODE", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-074", "INFO", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-075", "DATA", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-076", "BET_NO", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-077", "GMT_BET_NO", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-078", "OPEN_NUMBER", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-079", "BLOCK", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-080", "HASH", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-081", "OPEN_TIME", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-082", "PERMUTATION", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-083", "NUMBER_RANGE", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-084", "NO_DUPLICATES", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-085", "ISSUE_ORDER", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-086", "TIME_ORDER", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-087", "FRESHNESS", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-088", "PROVIDER_TIME", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-089", "RAW_ARCHIVE", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-090", "REDACTION", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-091", "FINGERPRINT", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-092", "SINGLE_POSITION", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-093", "FIXED_K3", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-094", "NO_BET_LOGIC", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-095", "GAME_ISOLATION", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-096", "SHADOW_ONLY", "GROUP_03", true));
        rules.add(new RuleSpec("TIANSHU-GAME-097", "GAME_CODE", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-098", "DATA_PATH", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-099", "PRIMARY_ENDPOINT", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-100", "BACKUP_ENDPOINT", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-101", "PARAM_CODE", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-102", "PARAM_LIMIT", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-103", "PARAM_DATE", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-104", "PARAM_KEY", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-105", "LOTTERY_CODE", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-106", "INFO", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-107", "DATA", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-108", "BET_NO", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-109", "GMT_BET_NO", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-110", "OPEN_NUMBER", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-111", "BLOCK", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-112", "HASH", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-113", "OPEN_TIME", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-114", "PERMUTATION", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-115", "NUMBER_RANGE", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-116", "NO_DUPLICATES", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-117", "ISSUE_ORDER", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-118", "TIME_ORDER", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-119", "FRESHNESS", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-120", "PROVIDER_TIME", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-121", "RAW_ARCHIVE", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-122", "REDACTION", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-123", "FINGERPRINT", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-124", "SINGLE_POSITION", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-125", "FIXED_K3", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-126", "NO_BET_LOGIC", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-127", "GAME_ISOLATION", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-128", "SHADOW_ONLY", "GROUP_04", true));
        rules.add(new RuleSpec("TIANSHU-GAME-129", "GAME_CODE", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-130", "DATA_PATH", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-131", "PRIMARY_ENDPOINT", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-132", "BACKUP_ENDPOINT", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-133", "PARAM_CODE", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-134", "PARAM_LIMIT", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-135", "PARAM_DATE", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-136", "PARAM_KEY", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-137", "LOTTERY_CODE", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-138", "INFO", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-139", "DATA", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-140", "BET_NO", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-141", "GMT_BET_NO", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-142", "OPEN_NUMBER", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-143", "BLOCK", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-144", "HASH", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-145", "OPEN_TIME", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-146", "PERMUTATION", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-147", "NUMBER_RANGE", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-148", "NO_DUPLICATES", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-149", "ISSUE_ORDER", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-150", "TIME_ORDER", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-151", "FRESHNESS", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-152", "PROVIDER_TIME", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-153", "RAW_ARCHIVE", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-154", "REDACTION", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-155", "FINGERPRINT", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-156", "SINGLE_POSITION", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-157", "FIXED_K3", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-158", "NO_BET_LOGIC", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-159", "GAME_ISOLATION", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-160", "SHADOW_ONLY", "GROUP_05", true));
        rules.add(new RuleSpec("TIANSHU-GAME-161", "GAME_CODE", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-162", "DATA_PATH", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-163", "PRIMARY_ENDPOINT", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-164", "BACKUP_ENDPOINT", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-165", "PARAM_CODE", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-166", "PARAM_LIMIT", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-167", "PARAM_DATE", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-168", "PARAM_KEY", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-169", "LOTTERY_CODE", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-170", "INFO", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-171", "DATA", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-172", "BET_NO", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-173", "GMT_BET_NO", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-174", "OPEN_NUMBER", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-175", "BLOCK", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-176", "HASH", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-177", "OPEN_TIME", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-178", "PERMUTATION", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-179", "NUMBER_RANGE", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-180", "NO_DUPLICATES", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-181", "ISSUE_ORDER", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-182", "TIME_ORDER", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-183", "FRESHNESS", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-184", "PROVIDER_TIME", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-185", "RAW_ARCHIVE", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-186", "REDACTION", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-187", "FINGERPRINT", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-188", "SINGLE_POSITION", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-189", "FIXED_K3", "GROUP_06", true));
        rules.add(new RuleSpec("TIANSHU-GAME-190", "NO_BET_LOGIC", "GROUP_06", true));
        return Collections.unmodifiableList(rules);
    }

    public static String contractHash() {
        StringBuilder sb =
                new StringBuilder(GAME_CODE)
                        .append('|')
                        .append(primary().endpoint())
                        .append('|')
                        .append(backup().endpoint());
        List<RuleSpec> rules = rules();
        for (int i = 0; i < rules.size(); i++) {
            RuleSpec r = rules.get(i);
            sb.append('|').append(r.id).append(':').append(r.field).append(':').append(r.group);
        }
        return hash(sb.toString());
    }

    public static boolean selfTest() {
        int[] numbers = new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        RequestSpec latest = RequestSpec.latest(20);
        RequestSpec date = RequestSpec.date("2026-08-05");
        return GAME_CODE.equals(AppConfig.LOTTERY_CODE)
                && isPermutation(numbers)
                && latest.valid()
                && date.valid()
                && primary().valid()
                && backup().valid()
                && rules().size() >= 150;
    }
}
