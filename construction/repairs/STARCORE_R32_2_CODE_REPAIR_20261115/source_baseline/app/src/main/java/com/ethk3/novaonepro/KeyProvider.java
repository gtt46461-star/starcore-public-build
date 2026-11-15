package com.ethk3.novaonepro;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Secret provider with no plaintext key literal in source, logs, database or reports. */
public final class KeyProvider {
    private static final String MATERIALIZATION_MARKER =
            "STARCORE_TIANSHU_B01_MATERIALIZED_20260805";
    private static final String INJECTION_SOURCE =
            "DEVICE_OVERRIDE_OR_AUTHORIZED_LEGACY_RELEASE_DEFAULT_R995";
    private static volatile PrivateApiCredentialCP12 credential;
    private static final java.util.concurrent.atomic.AtomicBoolean RELOAD_PENDING=new java.util.concurrent.atomic.AtomicBoolean();
    private static final java.util.concurrent.ThreadPoolExecutor CREDENTIAL_IO =
            new java.util.concurrent.ThreadPoolExecutor(1,1,0L,java.util.concurrent.TimeUnit.MILLISECONDS,
                    new java.util.concurrent.ArrayBlockingQueue<Runnable>(4),r->{Thread t=new Thread(r,"api-credential-cp12");t.setDaemon(true);return t;});

    private KeyProvider() {}

    public static String getApiKey() {
        PrivateApiCredentialCP12 owner=credential;
        String configured=owner==null?"":owner.get();
        return configured.isEmpty()?releaseDefaultApiKey():configured;
    }

    private static String releaseDefaultApiKey() {
        return (BuildConfig.DUOQU_API_KEY_A+BuildConfig.DUOQU_API_KEY_B).trim();
    }

    public static boolean usesReleaseDefault() {
        PrivateApiCredentialCP12 owner=credential;
        return (owner==null||owner.get().isEmpty())&&!releaseDefaultApiKey().isEmpty();
    }

    public static String credentialSource() {
        return usesReleaseDefault()?"AUTHORIZED_LEGACY_RELEASE_DEFAULT":"DEVICE_ANDROID_KEYSTORE_PRIVATE_CREDENTIAL_CP12";
    }

    /** Startup schedules private file/keystore IO; all getters remain memory-only on UI. */
    public static synchronized void initialize(android.content.Context context) {
        if(credential!=null)return;PrivateApiCredentialCP12 owner=new PrivateApiCredentialCP12(new PrivateApiCredentialCP12.DeviceStorage(context));
        credential=owner;reloadAsync(context);
    }
    public static void reloadAsync(android.content.Context context){
        initialize(context);if(!RELOAD_PENDING.compareAndSet(false,true))return;PrivateApiCredentialCP12 owner=credential;
        try{CREDENTIAL_IO.execute(()->{try{owner.load();}finally{RELOAD_PENDING.set(false);}});}catch(java.util.concurrent.RejectedExecutionException busy){RELOAD_PENDING.set(false);}
    }
    public interface ConfigurationResult{void complete(boolean saved,String reason);}
    public static void configureAsync(android.content.Context context,char[] input,ConfigurationResult result) {
        initialize(context);final char[] copy=input==null?new char[0]:input.clone();final PrivateApiCredentialCP12 owner=credential;
        try{CREDENTIAL_IO.execute(()->{boolean saved=false;String reason="KEY_STORE_WRITE_FAILED";
            try{owner.configure(copy);saved=true;reason="READY";}catch(Exception failure){reason=failure.getMessage()!=null&&failure.getMessage().equals("KEY_INPUT_INVALID")?"KEY_INPUT_INVALID":"KEY_STORE_WRITE_FAILED";}
            finally{zeroize(copy);}result.complete(saved,reason);
        });}catch(java.util.concurrent.RejectedExecutionException busy){zeroize(copy);result.complete(false,"KEY_CONFIGURATION_BUSY");}
    }
    public static String unavailableReasonCP12(){PrivateApiCredentialCP12 owner=credential;return (owner==null?"KEY_OWNER_NOT_INITIALIZED":owner.state())+"|original_reason="+(owner==null?"APPLICATION_CREDENTIAL_OWNER_NOT_RUN":owner.failureReason())+"|missing=device_api_credential|recovery=HOME_API_LINE_CONFIGURE";}

    public static char[] acquireKeyChars() {
        return getApiKey().toCharArray();
    }

    public static void releaseKeyChars(char[] chars) {
        zeroize(chars);
    }

    private static char[] decodeChars() {
        return getApiKey().toCharArray();
    }

    private static void zeroize(char[] chars) {
        if (chars == null) return;
        for (int i = 0; i < chars.length; i++) chars[i] = '\0';
    }

    public static String getKeyParamName() {
        return "key";
    }

    public static String getLotteryParamName() {
        return "code";
    }

    public static String maskKey() {
        char[] chars = decodeChars();
        try {
            if (chars.length <= 4) return "****";
            return new String(new char[] {chars[0], chars[1]})
                    + "******"
                    + new String(new char[] {chars[chars.length - 2], chars[chars.length - 1]});
        } finally {
            zeroize(chars);
        }
    }

    public static String maskedStatus() {
        return isKeyReady()
                ? "READY:" + maskKey() + "|fp=" + shortFingerprint() + "|source=" + credentialSource()
                        + "|privateOverrideState=" + (credential==null?"NOT_LOADED":credential.state())
                : "NOT_READY|source=" + INJECTION_SOURCE;
    }

    public static boolean isKeyReady() {
        char[] chars = decodeChars();
        try {
            if (chars.length < 8 || chars.length > 128) return false;
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9'))
                    return false;
            }
            return true;
        } finally {
            zeroize(chars);
        }
    }

    public static String fingerprint() {
        char[] chars=decodeChars();byte[] bytes=null;
        try {bytes=new String(chars).getBytes("UTF-8");return hex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(Exception failure){return "FINGERPRINT_ERROR";}
        finally{zeroize(chars);if(bytes!=null)java.util.Arrays.fill(bytes,(byte)0);}
    }

    public static String shortFingerprint() {
        String fp = fingerprint();
        return fp.length() <= 12 ? fp : fp.substring(0, 12);
    }

    private static String hex(byte[] data) {
        if (data == null) return "";
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (int i = 0; i < data.length; i++) {
            int value = data[i] & 0xFF;
            if (value < 16) sb.append('0');
            sb.append(Integer.toHexString(value));
        }
        return sb.toString();
    }

    public static boolean looksLikeSecret(String text) {
        if (text == null || text.length() == 0) return false;
        char[] chars = decodeChars();
        try {
            return indexOf(text, chars) >= 0 || containsSensitiveParameter(text);
        } finally {
            zeroize(chars);
        }
    }

    private static int indexOf(String text, char[] pattern) {
        if (text == null
                || pattern == null
                || pattern.length == 0
                || text.length() < pattern.length) return -1;
        outer:
        for (int i = 0; i <= text.length() - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++)
                if (text.charAt(i + j) != pattern[j]) continue outer;
            return i;
        }
        return -1;
    }

    private static boolean containsSensitiveParameter(String text) {
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        return lower.indexOf("key=") >= 0
                || lower.indexOf("token=") >= 0
                || lower.indexOf("secret=") >= 0
                || lower.indexOf("authorization:") >= 0
                || lower.indexOf("bearer ") >= 0
                || lower.indexOf("basic ") >= 0;
    }

    public static String redact(String text) {
        return SafeLog.sanitize(text);
    }

    /** The inactive release default remains sensitive after a device override is loaded. */
    static String redactReleaseDefault(String text) {
        String key=releaseDefaultApiKey();
        return text==null?"":key.isEmpty()?text:text.replace(key,"[SECRET]");
    }

    public static final class PolicyRule {
        public final String id;
        public final String field;
        public final String group;
        public final boolean mandatory;

        PolicyRule(String id, String field, String group, boolean mandatory) {
            this.id = id;
            this.field = field;
            this.group = group;
            this.mandatory = mandatory;
        }
    }

    public static List<PolicyRule> policyRules() {
        ArrayList<PolicyRule> rules = new ArrayList<PolicyRule>();
        rules.add(new PolicyRule("TIANSHU-SECRET-001", "NO_PLAINTEXT_LOG", "GROUP_01", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-002", "NO_DATABASE_STORAGE", "GROUP_01", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-003", "NO_REPORT_EXPORT", "GROUP_01", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-004", "NO_URL_LOG", "GROUP_01", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-005", "NO_STACKTRACE_SECRET", "GROUP_01", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-006", "NO_RAW_ARCHIVE_SECRET", "GROUP_01", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-007", "FINGERPRINT_ONLY", "GROUP_01", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-008", "MASKED_STATUS", "GROUP_01", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-009", "ZEROIZE_CHAR_BUFFER", "GROUP_01", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-010", "DEVICE_PRIVATE_CREDENTIAL_STORAGE", "GROUP_01", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-011", "QUERY_PARAM_REDACTION", "GROUP_01", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-012", "HEADER_REDACTION", "GROUP_01", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-013", "JSON_REDACTION", "GROUP_01", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-014", "FORM_REDACTION", "GROUP_01", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-015", "BASIC_AUTH_REDACTION", "GROUP_01", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-016", "BEARER_REDACTION", "GROUP_01", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-017", "CRASH_REPORT_REDACTION", "GROUP_01", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-018", "CLIPBOARD_REDACTION", "GROUP_01", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-019", "BACKUP_REDACTION", "GROUP_01", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-020", "ARTIFACT_SCAN", "GROUP_01", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-021", "NO_PLAINTEXT_LOG", "GROUP_02", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-022", "NO_DATABASE_STORAGE", "GROUP_02", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-023", "NO_REPORT_EXPORT", "GROUP_02", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-024", "NO_URL_LOG", "GROUP_02", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-025", "NO_STACKTRACE_SECRET", "GROUP_02", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-026", "NO_RAW_ARCHIVE_SECRET", "GROUP_02", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-027", "FINGERPRINT_ONLY", "GROUP_02", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-028", "MASKED_STATUS", "GROUP_02", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-029", "ZEROIZE_CHAR_BUFFER", "GROUP_02", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-030", "DEVICE_PRIVATE_CREDENTIAL_STORAGE", "GROUP_02", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-031", "QUERY_PARAM_REDACTION", "GROUP_02", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-032", "HEADER_REDACTION", "GROUP_02", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-033", "JSON_REDACTION", "GROUP_02", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-034", "FORM_REDACTION", "GROUP_02", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-035", "BASIC_AUTH_REDACTION", "GROUP_02", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-036", "BEARER_REDACTION", "GROUP_02", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-037", "CRASH_REPORT_REDACTION", "GROUP_02", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-038", "CLIPBOARD_REDACTION", "GROUP_02", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-039", "BACKUP_REDACTION", "GROUP_02", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-040", "ARTIFACT_SCAN", "GROUP_02", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-041", "NO_PLAINTEXT_LOG", "GROUP_03", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-042", "NO_DATABASE_STORAGE", "GROUP_03", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-043", "NO_REPORT_EXPORT", "GROUP_03", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-044", "NO_URL_LOG", "GROUP_03", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-045", "NO_STACKTRACE_SECRET", "GROUP_03", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-046", "NO_RAW_ARCHIVE_SECRET", "GROUP_03", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-047", "FINGERPRINT_ONLY", "GROUP_03", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-048", "MASKED_STATUS", "GROUP_03", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-049", "ZEROIZE_CHAR_BUFFER", "GROUP_03", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-050", "DEVICE_PRIVATE_CREDENTIAL_STORAGE", "GROUP_03", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-051", "QUERY_PARAM_REDACTION", "GROUP_03", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-052", "HEADER_REDACTION", "GROUP_03", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-053", "JSON_REDACTION", "GROUP_03", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-054", "FORM_REDACTION", "GROUP_03", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-055", "BASIC_AUTH_REDACTION", "GROUP_03", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-056", "BEARER_REDACTION", "GROUP_03", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-057", "CRASH_REPORT_REDACTION", "GROUP_03", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-058", "CLIPBOARD_REDACTION", "GROUP_03", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-059", "BACKUP_REDACTION", "GROUP_03", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-060", "ARTIFACT_SCAN", "GROUP_03", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-061", "NO_PLAINTEXT_LOG", "GROUP_04", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-062", "NO_DATABASE_STORAGE", "GROUP_04", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-063", "NO_REPORT_EXPORT", "GROUP_04", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-064", "NO_URL_LOG", "GROUP_04", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-065", "NO_STACKTRACE_SECRET", "GROUP_04", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-066", "NO_RAW_ARCHIVE_SECRET", "GROUP_04", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-067", "FINGERPRINT_ONLY", "GROUP_04", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-068", "MASKED_STATUS", "GROUP_04", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-069", "ZEROIZE_CHAR_BUFFER", "GROUP_04", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-070", "DEVICE_PRIVATE_CREDENTIAL_STORAGE", "GROUP_04", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-071", "QUERY_PARAM_REDACTION", "GROUP_04", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-072", "HEADER_REDACTION", "GROUP_04", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-073", "JSON_REDACTION", "GROUP_04", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-074", "FORM_REDACTION", "GROUP_04", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-075", "BASIC_AUTH_REDACTION", "GROUP_04", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-076", "BEARER_REDACTION", "GROUP_04", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-077", "CRASH_REPORT_REDACTION", "GROUP_04", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-078", "CLIPBOARD_REDACTION", "GROUP_04", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-079", "BACKUP_REDACTION", "GROUP_04", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-080", "ARTIFACT_SCAN", "GROUP_04", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-081", "NO_PLAINTEXT_LOG", "GROUP_05", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-082", "NO_DATABASE_STORAGE", "GROUP_05", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-083", "NO_REPORT_EXPORT", "GROUP_05", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-084", "NO_URL_LOG", "GROUP_05", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-085", "NO_STACKTRACE_SECRET", "GROUP_05", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-086", "NO_RAW_ARCHIVE_SECRET", "GROUP_05", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-087", "FINGERPRINT_ONLY", "GROUP_05", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-088", "MASKED_STATUS", "GROUP_05", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-089", "ZEROIZE_CHAR_BUFFER", "GROUP_05", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-090", "DEVICE_PRIVATE_CREDENTIAL_STORAGE", "GROUP_05", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-091", "QUERY_PARAM_REDACTION", "GROUP_05", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-092", "HEADER_REDACTION", "GROUP_05", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-093", "JSON_REDACTION", "GROUP_05", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-094", "FORM_REDACTION", "GROUP_05", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-095", "BASIC_AUTH_REDACTION", "GROUP_05", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-096", "BEARER_REDACTION", "GROUP_05", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-097", "CRASH_REPORT_REDACTION", "GROUP_05", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-098", "CLIPBOARD_REDACTION", "GROUP_05", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-099", "BACKUP_REDACTION", "GROUP_05", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-100", "ARTIFACT_SCAN", "GROUP_05", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-101", "NO_PLAINTEXT_LOG", "GROUP_06", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-102", "NO_DATABASE_STORAGE", "GROUP_06", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-103", "NO_REPORT_EXPORT", "GROUP_06", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-104", "NO_URL_LOG", "GROUP_06", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-105", "NO_STACKTRACE_SECRET", "GROUP_06", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-106", "NO_RAW_ARCHIVE_SECRET", "GROUP_06", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-107", "FINGERPRINT_ONLY", "GROUP_06", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-108", "MASKED_STATUS", "GROUP_06", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-109", "ZEROIZE_CHAR_BUFFER", "GROUP_06", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-110", "DEVICE_PRIVATE_CREDENTIAL_STORAGE", "GROUP_06", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-111", "QUERY_PARAM_REDACTION", "GROUP_06", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-112", "HEADER_REDACTION", "GROUP_06", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-113", "JSON_REDACTION", "GROUP_06", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-114", "FORM_REDACTION", "GROUP_06", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-115", "BASIC_AUTH_REDACTION", "GROUP_06", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-116", "BEARER_REDACTION", "GROUP_06", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-117", "CRASH_REPORT_REDACTION", "GROUP_06", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-118", "CLIPBOARD_REDACTION", "GROUP_06", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-119", "BACKUP_REDACTION", "GROUP_06", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-120", "ARTIFACT_SCAN", "GROUP_06", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-121", "NO_PLAINTEXT_LOG", "GROUP_07", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-122", "NO_DATABASE_STORAGE", "GROUP_07", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-123", "NO_REPORT_EXPORT", "GROUP_07", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-124", "NO_URL_LOG", "GROUP_07", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-125", "NO_STACKTRACE_SECRET", "GROUP_07", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-126", "NO_RAW_ARCHIVE_SECRET", "GROUP_07", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-127", "FINGERPRINT_ONLY", "GROUP_07", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-128", "MASKED_STATUS", "GROUP_07", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-129", "ZEROIZE_CHAR_BUFFER", "GROUP_07", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-130", "DEVICE_PRIVATE_CREDENTIAL_STORAGE", "GROUP_07", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-131", "QUERY_PARAM_REDACTION", "GROUP_07", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-132", "HEADER_REDACTION", "GROUP_07", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-133", "JSON_REDACTION", "GROUP_07", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-134", "FORM_REDACTION", "GROUP_07", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-135", "BASIC_AUTH_REDACTION", "GROUP_07", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-136", "BEARER_REDACTION", "GROUP_07", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-137", "CRASH_REPORT_REDACTION", "GROUP_07", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-138", "CLIPBOARD_REDACTION", "GROUP_07", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-139", "BACKUP_REDACTION", "GROUP_07", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-140", "ARTIFACT_SCAN", "GROUP_07", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-141", "NO_PLAINTEXT_LOG", "GROUP_08", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-142", "NO_DATABASE_STORAGE", "GROUP_08", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-143", "NO_REPORT_EXPORT", "GROUP_08", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-144", "NO_URL_LOG", "GROUP_08", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-145", "NO_STACKTRACE_SECRET", "GROUP_08", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-146", "NO_RAW_ARCHIVE_SECRET", "GROUP_08", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-147", "FINGERPRINT_ONLY", "GROUP_08", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-148", "MASKED_STATUS", "GROUP_08", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-149", "ZEROIZE_CHAR_BUFFER", "GROUP_08", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-150", "DEVICE_PRIVATE_CREDENTIAL_STORAGE", "GROUP_08", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-151", "QUERY_PARAM_REDACTION", "GROUP_08", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-152", "HEADER_REDACTION", "GROUP_08", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-153", "JSON_REDACTION", "GROUP_08", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-154", "FORM_REDACTION", "GROUP_08", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-155", "BASIC_AUTH_REDACTION", "GROUP_08", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-156", "BEARER_REDACTION", "GROUP_08", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-157", "CRASH_REPORT_REDACTION", "GROUP_08", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-158", "CLIPBOARD_REDACTION", "GROUP_08", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-159", "BACKUP_REDACTION", "GROUP_08", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-160", "ARTIFACT_SCAN", "GROUP_08", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-161", "NO_PLAINTEXT_LOG", "GROUP_09", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-162", "NO_DATABASE_STORAGE", "GROUP_09", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-163", "NO_REPORT_EXPORT", "GROUP_09", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-164", "NO_URL_LOG", "GROUP_09", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-165", "NO_STACKTRACE_SECRET", "GROUP_09", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-166", "NO_RAW_ARCHIVE_SECRET", "GROUP_09", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-167", "FINGERPRINT_ONLY", "GROUP_09", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-168", "MASKED_STATUS", "GROUP_09", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-169", "ZEROIZE_CHAR_BUFFER", "GROUP_09", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-170", "DEVICE_PRIVATE_CREDENTIAL_STORAGE", "GROUP_09", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-171", "QUERY_PARAM_REDACTION", "GROUP_09", true));
        rules.add(new PolicyRule("TIANSHU-SECRET-172", "HEADER_REDACTION", "GROUP_09", true));
        return Collections.unmodifiableList(rules);
    }

    public static boolean policyReady() {
        return isKeyReady() && fingerprint().length() == 64 && policyRules().size() >= 100;
    }
}
