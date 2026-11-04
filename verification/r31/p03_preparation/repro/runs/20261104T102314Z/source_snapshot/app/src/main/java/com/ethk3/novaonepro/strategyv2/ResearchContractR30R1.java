package com.ethk3.novaonepro.strategyv2;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

/** Immutable research identity. This object can never grant REAL authority. */
public final class ResearchContractR30R1 {
    public enum Role { TRAIN, SELECT, ONLINE_RESEARCH, CONFIRM, FUTURE, SYNTHETIC }
    public final String datasetHash, policyHash, abi, sourceCommit, inputHash;
    public final Role role;
    public final long fromOrdinal, throughOrdinal, availableAt, sourceRevision;

    public ResearchContractR30R1(String dataset, String policy, String abi, String source,
            String input, Role role, long from, long through, long available, long revision) {
        this.datasetHash=sha(dataset); this.policyHash=sha(policy); this.inputHash=sha(input);
        if(abi==null||abi.isEmpty()||source==null||!source.matches("[0-9a-f]{40}"))
            throw new IllegalArgumentException("RESEARCH_IDENTITY");
        if(from<0||through<from||available<0||revision<0)throw new IllegalArgumentException("RESEARCH_BOUNDARY");
        this.abi=abi;sourceCommit=source;this.role=Objects.requireNonNull(role);
        fromOrdinal=from;throughOrdinal=through;availableAt=available;sourceRevision=revision;
    }
    public void requireAdvice(long targetOrdinal,long preparedAt) {
        if(role!=Role.TRAIN&&role!=Role.SELECT&&role!=Role.ONLINE_RESEARCH)
            throw new IllegalStateException("FORMAL_OR_SYNTHETIC_LABELS_CANNOT_DRIVE_RESEARCH_ADVICE");
        if(throughOrdinal>=targetOrdinal||availableAt>preparedAt)
            throw new IllegalStateException("RESEARCH_ADVICE_PIT");
    }
    public String key(String consumerVersion) {
        if(consumerVersion==null||consumerVersion.isEmpty())throw new IllegalArgumentException("CONSUMER_VERSION");
        return hash(datasetHash+'|'+policyHash+'|'+abi+'|'+sourceCommit+'|'+inputHash+'|'+role.name()
                +'|'+fromOrdinal+'|'+throughOrdinal+'|'+availableAt+'|'+sourceRevision+'|'+consumerVersion);
    }
    public static String sha(String s){if(s==null||!s.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("SHA256_REQUIRED");return s;}
    public static String hash(String s){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte v:b)x.append(String.format(java.util.Locale.ROOT,"%02x",v&255));return x.toString();}catch(java.security.NoSuchAlgorithmException e){throw new AssertionError(e);}}
}
