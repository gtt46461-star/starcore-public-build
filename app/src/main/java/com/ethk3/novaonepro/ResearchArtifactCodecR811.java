package com.ethk3.novaonepro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.security.MessageDigest;

/** Canonical, versioned Research artifact codec. Java object serialization is forbidden. */
public final class ResearchArtifactCodecR811 {
    public static final int SCHEMA_VERSION = 1;
    private static final long MAGIC = 0x5238313141525431L; // R811ART1

    private ResearchArtifactCodecR811() {}

    public static byte[] encode(Artifact artifact) {
        if (artifact == null) throw new IllegalArgumentException("R811_ARTIFACT_NULL");
        artifact.validate();
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeLong(MAGIC);
            out.writeInt(SCHEMA_VERSION);
            writeUtf(out, artifact.type);
            writeUtf(out, artifact.trainedUntilIssue);
            List<String> keys = new ArrayList<String>(artifact.fields.keySet());
            Collections.sort(keys);
            out.writeInt(keys.size());
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                writeUtf(out, key);
                double[] values = artifact.fields.get(key);
                out.writeInt(values.length);
                for (int j = 0; j < values.length; j++)
                    out.writeLong(Double.doubleToLongBits(values[j]));
            }
            out.flush();
            return bytes.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("R811_ARTIFACT_ENCODE", e);
        }
    }

    public static Artifact decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0)
            throw new IllegalArgumentException("R811_ARTIFACT_BYTES_EMPTY");
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
            if (in.readLong() != MAGIC) throw new IllegalStateException("R811_ARTIFACT_MAGIC");
            if (in.readInt() != SCHEMA_VERSION)
                throw new IllegalStateException("R811_ARTIFACT_SCHEMA");
            String type = readUtf(in), trained = readUtf(in);
            int count = in.readInt();
            if (count < 0 || count > 128) throw new IllegalStateException("R811_ARTIFACT_FIELDS");
            LinkedHashMap<String, double[]> fields = new LinkedHashMap<String, double[]>();
            String previous = "";
            for (int i = 0; i < count; i++) {
                String key = readUtf(in);
                if (i > 0 && key.compareTo(previous) <= 0)
                    throw new IllegalStateException("R811_ARTIFACT_FIELD_ORDER");
                previous = key;
                int length = in.readInt();
                if (length < 0 || length > 100000)
                    throw new IllegalStateException("R811_ARTIFACT_FIELD_SIZE");
                double[] values = new double[length];
                for (int j = 0; j < length; j++) values[j] = Double.longBitsToDouble(in.readLong());
                fields.put(key, values);
            }
            if (in.available() != 0) throw new IllegalStateException("R811_ARTIFACT_TRAILING_BYTES");
            Artifact artifact = new Artifact(type, trained, fields);
            artifact.validate();
            return artifact;
        } catch (Exception e) {
            throw new IllegalStateException("R811_ARTIFACT_DECODE", e);
        }
    }

    public static String hash(Artifact artifact) {
        return hashBytes(encode(artifact));
    }

    public static boolean verify(byte[] bytes, String expectedHash) {
        try {
            Artifact artifact = decode(bytes);
            return expectedHash != null
                    && expectedHash.equals(hash(artifact))
                    && expectedHash.equals(hashBytes(bytes));
        } catch (Throwable e) {
            return false;
        }
    }

    public static final class Artifact {
        public final String type, trainedUntilIssue;
        public final Map<String, double[]> fields;

        public Artifact(String type, String trained, Map<String, double[]> fields) {
            this.type = safe(type);
            trainedUntilIssue = safe(trained);
            LinkedHashMap<String, double[]> copy = new LinkedHashMap<String, double[]>();
            if (fields != null)
                for (Map.Entry<String, double[]> entry : fields.entrySet())
                    copy.put(safe(entry.getKey()), entry.getValue() == null ? new double[0] : entry.getValue().clone());
            this.fields = Collections.unmodifiableMap(copy);
        }

        void validate() {
            if (type.length() == 0 || type.length() > 64)
                throw new IllegalStateException("R811_ARTIFACT_TYPE");
            for (Map.Entry<String, double[]> entry : fields.entrySet()) {
                if (!entry.getKey().matches("[A-Za-z0-9_.-]{1,64}"))
                    throw new IllegalStateException("R811_ARTIFACT_KEY");
                double[] values = entry.getValue();
                for (int i = 0; i < values.length; i++)
                    if (Double.isNaN(values[i]) || Double.isInfinite(values[i]))
                        throw new IllegalStateException("R811_ARTIFACT_NONFINITE");
            }
        }
    }

    private static void writeUtf(DataOutputStream out, String value) throws Exception {
        byte[] bytes = safe(value).getBytes("UTF-8");
        if (bytes.length > 65535) throw new IllegalArgumentException("R811_ARTIFACT_TEXT_SIZE");
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readUtf(DataInputStream in) throws Exception {
        int length = in.readInt();
        if (length < 0 || length > 65535) throw new IllegalStateException("R811_ARTIFACT_TEXT_SIZE");
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, "UTF-8");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String hashBytes(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder out = new StringBuilder(64);
            for (int i = 0; i < digest.length; i++) {
                int value = digest[i] & 0xff;
                if (value < 16) out.append('0');
                out.append(Integer.toHexString(value));
            }
            return out.toString();
        } catch (Exception e) {
            throw new IllegalStateException("R811_ARTIFACT_HASH", e);
        }
    }
}
