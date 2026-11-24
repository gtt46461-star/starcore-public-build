package com.ethk3.novaonepro.strategyv2;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/** Pure contract. Rebirth is a fresh research identity, never a restored old learner. */
public final class ColdRebirthContractR34 {
    private ColdRebirthContractR34() {}
    public static final String PROTOCOL = "R34_COLD_RETRAIN_ONLY_1";
    public static final int MAX_PENDING = 16, MAX_SNAPSHOT_BYTES = 176 * 1024;
    public static final String INSERT_SOURCE = "INSERT INTO r26_foundry(campaign_id,snapshot_hash,source_through,snapshot,status,created_at,numerical_abi,source_first) VALUES(?,?,?,?,?,?,?,?)";
    public static final String INSERT_CANDIDATE = "INSERT INTO r26_candidate(campaign_id,policy_hash,canonical_text,parent,operation) VALUES(?,?,?,?,?)";
    public static final String INSERT_EXPERT = "INSERT INTO r26_expert_experiment(experiment_id,expert_id,campaign_id,state,state_hash,checkpoint_hash,numerical_abi,proposal_json,created_at) VALUES(?,?,?,?,?,'',?,?,?)";
    public static final String INSERT_RECEIPT = "INSERT INTO r34_cold_rebirth(source_kind,source_campaign,source_id,source_key,source_state_hash,source_checkpoint,source_cursor,source_abi,target_abi,child_campaign,child_id,snapshot_hash,source_first,source_through,birth_kind,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    public static String hash(String... fields) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String field : fields) {
                if (field == null) throw new IllegalArgumentException("R34_NULL_IDENTITY_FIELD");
                byte[] bytes = field.getBytes(StandardCharsets.UTF_8);
                digest.update(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
                digest.update((byte) ':'); digest.update(bytes);
            }
            StringBuilder out = new StringBuilder();
            for (byte value : digest.digest()) out.append(String.format(java.util.Locale.ROOT,"%02x",value & 255));
            return out.toString();
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    public static String sourceKey(String kind,String campaign,String id) {
        if (!"CANDIDATE".equals(kind) && !"EXPERT".equals(kind))
            throw new IllegalArgumentException("R34_SOURCE_KIND");
        return hash(PROTOCOL,kind,campaign,id);
    }

    public static String campaign(String sourceKey,String targetAbi,String snapshotHash) {
        hex(sourceKey); hex(targetAbi); hex(snapshotHash);
        return hash(PROTOCOL,"CAMPAIGN",sourceKey,targetAbi,snapshotHash);
    }

    public static void hex(String value) {
        if (value == null || !value.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("R34_IDENTITY_HASH");
    }

    /** Delete all state-transfer and previous birth metadata; retain numerical structure only. */
    public static SortedMap<String,String> coldParameters(Map<String,String> original,String sourceKey,String abi) {
        hex(sourceKey); hex(abi); SortedMap<String,String> out = new TreeMap<>();
        for (Map.Entry<String,String> item : original.entrySet()) {
            String key = item.getKey();
            if (key.startsWith("_warm_") || key.startsWith("_birth_") || key.startsWith("_cold_") || key.equals("_legal_head")) continue;
            out.put(key,item.getValue());
        }
        out.put("_cold_parent",sourceKey); out.put("_cold_abi",abi); out.put("_cold_protocol",PROTOCOL);
        return out;
    }

    /** Lineage belongs to the policy identity, not the strict numerical parameter map. */
    public static boolean hasColdParameters(Map<String,String> parameters) {
        for(String key:parameters.keySet())if(key.startsWith("_cold_"))return true;
        return false;
    }
    public static SortedMap<String,String> stripColdParameters(Map<String,String> parameters) {
        SortedMap<String,String> out=new TreeMap<>(parameters);
        if(!hasColdParameters(out))return out;
        hex(out.remove("_cold_parent"));hex(out.remove("_cold_abi"));
        if(!PROTOCOL.equals(out.remove("_cold_protocol")))throw new IllegalArgumentException("R34_COLD_PROTOCOL");
        if(hasColdParameters(out))throw new IllegalArgumentException("R34_UNKNOWN_COLD_FIELD");
        return out;
    }

    /** Keep only zero-trained metadata until a successor receipt exists; raw snapshots still expire. */
    public static String protectZeroCandidateSql(String alias) {
        alias(alias);
        return "("+alias+".cursor=0 AND "+alias+".checkpoint_hash='' AND "+alias+".dev_n=0 AND "+alias+".dev_k=0 AND "+alias+".val_n=0 AND "+alias+".val_k=0 AND "+alias+".loss_sum=0 AND "+alias+".state_hash='' AND coalesce(length("+alias+".state),0)=0 AND "+birthWithoutSuccessor("CANDIDATE",alias,"policy_hash")+")";
    }
    public static String protectZeroExpertSql(String alias) {
        alias(alias);
        return "("+alias+".cursor=0 AND "+alias+".checkpoint_hash='' AND "+alias+".work_nanos=0 AND "+alias+".result_json='{}' AND "+birthWithoutSuccessor("EXPERT",alias,"experiment_id")+")";
    }
    private static void alias(String value) {
        if(value==null||!value.matches("[A-Za-z_][A-Za-z0-9_]*"))throw new IllegalArgumentException("R34_SQL_INTERNAL_ALIAS");
    }
    private static String birthWithoutSuccessor(String kind,String alias,String id) {
        return "EXISTS(SELECT 1 FROM r34_cold_rebirth born WHERE born.source_kind='"+kind+"' AND born.child_campaign="+alias+".campaign_id AND born.child_id="+alias+"."+id+") AND NOT EXISTS(SELECT 1 FROM r34_cold_rebirth successor WHERE successor.source_kind='"+kind+"' AND successor.source_campaign="+alias+".campaign_id AND successor.source_id="+alias+"."+id+")";
    }
    public static String pruneSourceSql() {
        return "DELETE FROM r26_foundry WHERE campaign_id IN (SELECT f.campaign_id FROM r26_foundry f WHERE f.status<>'RUNNING' AND NOT EXISTS(SELECT 1 FROM r26_candidate c WHERE c.campaign_id=f.campaign_id) AND NOT EXISTS(SELECT 1 FROM r26_expert_experiment e WHERE e.campaign_id=f.campaign_id AND (e.status='RUNNING' OR "+protectZeroExpertSql("e")+")) AND f.campaign_id NOT IN (SELECT campaign_id FROM r26_foundry ORDER BY created_at DESC LIMIT 2) ORDER BY f.created_at,f.campaign_id LIMIT 2)";
    }
    public static String pruneExpiredExpertsSql(int limit) {
        if(limit<1||limit>512)throw new IllegalArgumentException("R34_RETENTION_SLICE_BOUND");
        return "DELETE FROM r26_expert_experiment WHERE rowid IN (SELECT e.rowid FROM r26_expert_experiment e WHERE e.campaign_id NOT IN (SELECT campaign_id FROM r26_foundry) AND (length(e.state)=0 OR e.cursor=0 AND e.checkpoint_hash='') AND NOT "+protectZeroExpertSql("e")+" LIMIT "+limit+")";
    }

    public static void requireSnapshot(int samples,int bytes,long first,long through,long parentExposure) {
        if (samples < 64 || samples > 4096 || bytes <= 0 || bytes > MAX_SNAPSHOT_BYTES
                || first < 0 || through < first || through - first >= 5000
                || parentExposure < 0 || through < parentExposure)
            throw new IllegalArgumentException("R34_COLD_SOURCE_BOUND_OR_ORDER");
    }

    static void requireParentIdentity(String abi, long exposure, long cursor, String stateHash, String checkpointHash) {
        if (abi == null || abi.isEmpty()) throw new IllegalArgumentException("R34_COLD_PARENT_ABI_OWNER_MISSING");
        hex(abi);
        if (exposure < 0L) throw new IllegalArgumentException("R34_COLD_PARENT_SOURCE_EXPOSURE_MISSING");
        if (cursor > 0L) {
            hex(stateHash);
            hex(checkpointHash);
        }
    }

    public static void requireFreshCounts(long cursor,long train,long confirm,long future,long certificates) {
        if (cursor != 0 || train != 0 || confirm != 0 || future != 0 || certificates != 0)
            throw new IllegalArgumentException("R34_COLD_EVIDENCE_INHERITANCE");
    }

    public static long retryDelay(int attempts) {
        if (attempts < 1) throw new IllegalArgumentException("R34_RETRY_ATTEMPTS");
        return Math.min(1800000L,30000L << Math.min(6,attempts - 1));
    }
}
