package com.ethk3.novaonepro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

/** Immutable typed research vector; unavailable is a bit, never a guessed numeric zero. */
public final class ResearchFeatureVectorR815 {
    private static final int CODEC_VERSION = 1;

    public static final class Binding {
        public final String gameCode;
        public final DatasetIdentityGateR815.Scope scope;
        public final String targetIssue;
        public final String historyHash;
        public final String datasetSha256;
        public final String schemaSha256;

        public Binding(
                String gameCode,
                DatasetIdentityGateR815.Scope scope,
                String targetIssue,
                String historyHash,
                String datasetSha256,
                String schemaSha256) {
            this.gameCode = safe(gameCode);
            this.scope = scope;
            this.targetIssue = safe(targetIssue);
            this.historyHash = safe(historyHash);
            this.datasetSha256 = safe(datasetSha256);
            this.schemaSha256 = safe(schemaSha256);
            validate();
        }

        public void validate() {
            if (!ReleaseIdentityR815.GAME_CODE.equals(gameCode)
                    || scope == null
                    || !TargetIssueResolver.isValid(targetIssue)
                    || !sha256(historyHash)
                    || !sha256(datasetSha256)
                    || !ResearchFeatureSchemaR815.schemaSha256().equals(schemaSha256))
                throw new IllegalArgumentException("R815_VECTOR_BINDING_INVALID");
        }

        public String canonical() {
            return gameCode
                    + '|'
                    + scope.name()
                    + '|'
                    + targetIssue
                    + '|'
                    + historyHash
                    + '|'
                    + datasetSha256
                    + '|'
                    + schemaSha256;
        }
    }

    public static final class Builder {
        private final Binding binding;
        private final double[] values = new double[ResearchFeatureSchemaR815.SIZE];
        private final boolean[] availability = new boolean[ResearchFeatureSchemaR815.SIZE];
        private final boolean[] assigned = new boolean[ResearchFeatureSchemaR815.SIZE];

        private Builder(Binding binding) {
            if (binding == null) throw new IllegalArgumentException("R815_VECTOR_BINDING_NULL");
            this.binding = binding;
        }

        public Builder set(int index, double value, boolean available) {
            checkIndex(index);
            if (available && (Double.isNaN(value) || Double.isInfinite(value)))
                throw new IllegalArgumentException("R815_VECTOR_VALUE_NOT_FINITE");
            values[index] = available ? value : 0.0d;
            availability[index] = available;
            assigned[index] = true;
            return this;
        }

        public Builder unavailable(int index) {
            return set(index, 0.0d, false);
        }

        public ResearchFeatureVectorR815 build() {
            for (int i = 0; i < assigned.length; i++) {
                if (!assigned[i]) throw new IllegalStateException("R815_VECTOR_INDEX_UNASSIGNED_" + i);
            }
            return new ResearchFeatureVectorR815(binding, values, availability);
        }
    }

    public final Binding binding;
    private final double[] values;
    private final boolean[] availability;
    private final String hash;

    private ResearchFeatureVectorR815(Binding binding, double[] values, boolean[] availability) {
        this.binding = binding;
        this.values = values.clone();
        this.availability = availability.clone();
        for (int i = 0; i < this.values.length; i++) {
            if (this.availability[i]
                    && (Double.isNaN(this.values[i]) || Double.isInfinite(this.values[i])))
                throw new IllegalArgumentException("R815_VECTOR_NOT_FINITE");
            if (!this.availability[i]) this.values[i] = 0.0d;
        }
        hash = V51ChecksumEngine.sha256(binding.canonical() + '|' + valuesCanonical());
    }

    public static Builder builder(Binding binding) {
        return new Builder(binding);
    }

    public double value(int index) {
        checkIndex(index);
        return values[index];
    }

    public boolean available(int index) {
        checkIndex(index);
        return availability[index];
    }

    public String hash() {
        return hash;
    }

    public byte[] encode() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(2048);
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(CODEC_VERSION);
            out.writeUTF(binding.canonical());
            for (int i = 0; i < values.length; i++) {
                out.writeBoolean(availability[i]);
                out.writeLong(Double.doubleToLongBits(values[i]));
            }
            out.writeUTF(hash);
            out.flush();
            return bytes.toByteArray();
        } catch (IOException impossible) {
            throw new IllegalStateException("R815_VECTOR_ENCODE", impossible);
        }
    }

    public static ResearchFeatureVectorR815 decode(byte[] encoded) {
        if (encoded == null || encoded.length < 64) throw new IllegalArgumentException("R815_VECTOR_CODEC_EMPTY");
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(encoded));
            if (in.readInt() != CODEC_VERSION) throw new IllegalArgumentException("R815_VECTOR_CODEC_VERSION");
            Binding binding = parseBinding(in.readUTF());
            Builder builder = builder(binding);
            for (int i = 0; i < ResearchFeatureSchemaR815.SIZE; i++) {
                boolean available = in.readBoolean();
                double value = Double.longBitsToDouble(in.readLong());
                builder.set(i, value, available);
            }
            String expected = in.readUTF();
            if (in.available() != 0) throw new IllegalArgumentException("R815_VECTOR_CODEC_TRAILING");
            ResearchFeatureVectorR815 vector = builder.build();
            if (!vector.hash.equals(expected)) throw new IllegalArgumentException("R815_VECTOR_CODEC_HASH");
            return vector;
        } catch (IOException failure) {
            throw new IllegalArgumentException("R815_VECTOR_CODEC_CORRUPT", failure);
        }
    }

    private String valuesCanonical() {
        StringBuilder out = new StringBuilder(ResearchFeatureSchemaR815.SIZE * 24);
        for (int i = 0; i < values.length; i++) {
            out.append('|').append(availability[i] ? '1' : '0').append(':')
                    .append(Long.toHexString(Double.doubleToLongBits(values[i])));
        }
        return out.toString();
    }

    private static Binding parseBinding(String canonical) {
        String[] p = canonical == null ? new String[0] : canonical.split("\\|", -1);
        if (p.length != 6) throw new IllegalArgumentException("R815_VECTOR_CODEC_BINDING");
        DatasetIdentityGateR815.Scope scope;
        try {
            scope = DatasetIdentityGateR815.Scope.valueOf(p[1]);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("R815_VECTOR_CODEC_SCOPE", failure);
        }
        return new Binding(p[0], scope, p[2], p[3], p[4], p[5]);
    }

    private static void checkIndex(int index) {
        if (index < 0 || index >= ResearchFeatureSchemaR815.SIZE)
            throw new IllegalArgumentException("R815_VECTOR_INDEX_RANGE");
    }

    private static boolean sha256(String value) {
        if (value == null || value.length() != 64) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return false;
        }
        return true;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
