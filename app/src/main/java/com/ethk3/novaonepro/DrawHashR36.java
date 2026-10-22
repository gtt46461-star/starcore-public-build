package com.ethk3.novaonepro;

/** Fixed canonical draw serialization used by durable settlement tasks. */
public final class DrawHashR36 {
    public static final int LEGACY_VERSION = 1;
    public static final int CURRENT_VERSION = 2;

    private DrawHashR36() {}

    /** Legacy V1 includes provider metadata and is retained only for already persisted rows. */
    public static String canonical(Models.Draw draw) {
        if (draw == null || !draw.isValid() || !TargetIssueResolver.isValid(draw.issue))
            throw new IllegalArgumentException("R36_DRAW_INVALID");
        StringBuilder b = new StringBuilder("R36_DRAW|");
        b.append(draw.issue).append('|').append(Math.max(0L, draw.openTime));
        for (int i = 0; i < 10; i++) b.append('|').append(draw.numbers[i]);
        return b.toString();
    }

    public static String sha256(Models.Draw draw) {
        return V51ChecksumEngine.sha256(canonical(draw));
    }

    /** V2 is the business identity: issue plus its ordered 10-number permutation only. */
    public static String canonicalV2(Models.Draw draw) {
        if (draw == null || !draw.isValid() || !TargetIssueResolver.isValid(draw.issue))
            throw new IllegalArgumentException("R815_DRAW_INVALID");
        StringBuilder builder = new StringBuilder("R815_DRAW_V2|");
        builder.append(draw.issue);
        for (int i = 0; i < AppConfig.NUMBER_COUNT; i++) builder.append('|').append(draw.numbers[i]);
        return builder.toString();
    }

    public static String sha256V2(Models.Draw draw) {
        return V51ChecksumEngine.sha256(canonicalV2(draw));
    }

    public static boolean matchesPersisted(String storedHash, int storedVersion, Models.Draw draw) {
        if (storedHash == null || storedHash.length() != 64 || draw == null || !draw.isValid())
            return false;
        if (storedVersion >= CURRENT_VERSION) return storedHash.equals(sha256V2(draw));
        return storedHash.equals(sha256(draw)) || storedHash.equals(sha256V2(draw));
    }
}
