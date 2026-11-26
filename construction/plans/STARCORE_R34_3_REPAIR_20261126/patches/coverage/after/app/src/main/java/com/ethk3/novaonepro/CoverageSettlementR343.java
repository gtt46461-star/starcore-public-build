package com.ethk3.novaonepro;

/** Read-only domain-correct closure count. A label hash is not an aggregate task hash. */
final class CoverageSettlementR343 {
    private CoverageSettlementR343() {}

    static boolean closed(int shadowSettled, String taskStatus, String labelHash,
            String aggregateTaskHash, int exactLabelReceipt) {
        return shadowSettled==1 && "COMPLETED".equals(taskStatus) && exactLabelReceipt==1
                && hash(labelHash) && hash(aggregateTaskHash);
    }
    private static boolean hash(String value) { return value!=null && value.matches("[a-f0-9]{64}"); }

    /** Aliases d/o/t are the bounded 30-draw snapshot, canonical shadow and settlement task. */
    static String labelReceiptExists(String table) {
        if(table==null || !table.matches("[A-Za-z_][A-Za-z0-9_]*"))
            throw new IllegalArgumentException("R343_LABEL_TABLE_IDENTIFIER");
        return "EXISTS(SELECT 1 FROM "+table+" l WHERE l.target_issue=d.issue"
                +" AND l.game_code=o.game_code AND l.base_issue=o.base_issue"
                +" AND l.position=o.position AND l.n1=o.n1 AND l.n2=o.n2 AND l.n3=o.n3"
                +" AND l.record_hash=o.settlement_hash AND l.actual_number=o.actual_number"
                +" AND l.hit_label=o.hit_label AND l.strict_oos=1 AND l.pre_draw_frozen=1"
                +" AND l.prediction_created_at>0 AND l.prediction_created_at<d.open_time"
                +" AND l.draw_open_time=d.open_time AND l.settled_at=o.settled_at"
                +" AND l.settled_at>=d.open_time AND t.completed_at>=l.settled_at)";
    }
}
