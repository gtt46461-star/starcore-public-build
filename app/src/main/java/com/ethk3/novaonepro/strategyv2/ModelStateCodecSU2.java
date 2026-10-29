package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Durable form of the continuing evolution (P23): population, Hall of Fame, tier champions, director memory. */
public final class ModelStateCodecSU2 {
    private ModelStateCodecSU2() {}
    /** R16 (P48): same layout as R15; the new magic keeps an R15 build from loading genomes with REGMEM / RELMEM members. */
    static final int MAGIC_R13 = 0x53553213, MAGIC_R14 = 0x53553214, MAGIC_R15 = 0x53553215, MAGIC = 0x53553216, MAX = 4 << 20;

    public static byte[] encode(ModelEvolutionSU2.State s) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
                out.writeInt(MAGIC);
                writeList(out, s.population); writeList(out, s.hallOfFame);
                out.writeInt(s.tiers.size()); for (Map.Entry<String, ModelEvolutionSU2.Ranked> e : s.tiers.entrySet()) { out.writeUTF(e.getKey()); writeRanked(out, e.getValue()); }
                out.writeBoolean(s.champion != null); if (s.champion != null) writeRanked(out, s.champion);
                writeStats(out, s.kindStats); writeStats(out, s.opStats);
                out.writeInt(s.effectiveGenerations);
                int notes = Math.min(200, s.repairNotes.size()); out.writeInt(notes);
                for (int i = s.repairNotes.size() - notes; i < s.repairNotes.size(); i++) out.writeUTF(s.repairNotes.get(i));
                out.writeInt(s.committees.size());                            // R14: committee champions per tier
                for (Map.Entry<String, CommitteeSU2> e : s.committees.entrySet()) { out.writeUTF(e.getKey()); writeLong(out, e.getValue().text()); }
                out.writeDouble(s.committeeJudge);
                out.writeInt(s.atomPrior.size());                              // R15: research memory (atom prior)
                for (Map.Entry<Integer, double[]> e : s.atomPrior.entrySet()) { out.writeInt(e.getKey()); out.writeDouble(e.getValue()[0]); out.writeDouble(e.getValue()[1]); }
                out.writeInt(s.priorSeen.size()); for (String k : s.priorSeen) writeLong(out, k);
            }
            byte[] b = bytes.toByteArray();
            if (b.length > MAX) throw new IllegalStateException("SU2_EVOLUTION_STATE_TOO_LARGE");
            return b;
        } catch (IOException impossible) { throw new IllegalStateException(impossible); }
    }

    public static ModelEvolutionSU2.State decode(byte[] blob) throws IOException {
        if (blob == null || blob.length > MAX) throw new IOException("SU2_EVOLUTION_STATE_SIZE");
        ModelEvolutionSU2.State s = new ModelEvolutionSU2.State();
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(blob)))) {
            int magic = in.readInt();
            if (magic != MAGIC && magic != MAGIC_R15 && magic != MAGIC_R14 && magic != MAGIC_R13) throw new IOException("SU2_EVOLUTION_STATE_MAGIC");
            readList(in, s.population); readList(in, s.hallOfFame);
            int n = in.readInt(); if (n < 0 || n > 3) throw new IOException("SU2_TIERS");
            for (int i = 0; i < n; i++) s.tiers.put(in.readUTF(), readRanked(in));
            if (in.readBoolean()) s.champion = readRanked(in);
            readStats(in, s.kindStats); readStats(in, s.opStats);
            s.effectiveGenerations = in.readInt();
            int notes = in.readInt(); if (notes < 0 || notes > 200) throw new IOException("SU2_NOTES");
            for (int i = 0; i < notes; i++) s.repairNotes.add(in.readUTF());
            if (magic == MAGIC || magic == MAGIC_R15 || magic == MAGIC_R14) {
                int nc = in.readInt(); if (nc < 0 || nc > 3) throw new IOException("SU2_COMMITTEES");
                for (int i = 0; i < nc; i++) s.committees.put(in.readUTF(), CommitteeSU2.parse(readLong(in)));
                s.committeeJudge = in.readDouble();
            }
            if (magic == MAGIC || magic == MAGIC_R15) {
                int np = in.readInt(); if (np < 0 || np > 10000) throw new IOException("SU2_PRIOR");
                for (int i = 0; i < np; i++) { int k = in.readInt(); s.atomPrior.put(k, new double[]{in.readDouble(), in.readDouble()}); }
                int ns = in.readInt(); if (ns < 0 || ns > 100000) throw new IOException("SU2_PRIOR_SEEN");
                for (int i = 0; i < ns; i++) s.priorSeen.add(readLong(in));
            }
        }
        return s;
    }

    /** Committee texts can exceed writeUTF's 64 KB-safe range on future sizes; length-prefixed UTF-8. */
    static void writeLong(DataOutput o, String s) throws IOException { byte[] b = s.getBytes(java.nio.charset.StandardCharsets.UTF_8); o.writeInt(b.length); o.write(b); }
    static String readLong(DataInput in) throws IOException { int n = in.readInt(); if (n < 0 || n > 65536) throw new IOException("SU2_TEXT"); byte[] b = new byte[n]; in.readFully(b); return new String(b, java.nio.charset.StandardCharsets.UTF_8); }

    static void writeRanked(DataOutput o, ModelEvolutionSU2.Ranked r) throws IOException { o.writeUTF(r.g.text()); o.writeUTF(r.origin); o.writeUTF(r.parent); o.writeUTF(r.note); }
    static ModelEvolutionSU2.Ranked readRanked(DataInput in) throws IOException {
        ModelGenomeSU2 g = ModelGenomeSU2.parse(in.readUTF());
        return new ModelEvolutionSU2.Ranked(g, in.readUTF(), in.readUTF(), in.readUTF());
    }
    static void writeList(DataOutput o, List<ModelEvolutionSU2.Ranked> l) throws IOException { o.writeInt(l.size()); for (ModelEvolutionSU2.Ranked r : l) writeRanked(o, r); }
    static void readList(DataInput in, List<ModelEvolutionSU2.Ranked> l) throws IOException { int n = in.readInt(); if (n < 0 || n > 256) throw new IOException("SU2_LIST"); for (int i = 0; i < n; i++) l.add(readRanked(in)); }
    static void writeStats(DataOutput o, Map<String, double[]> m) throws IOException { o.writeInt(m.size()); for (Map.Entry<String, double[]> e : m.entrySet()) { o.writeUTF(e.getKey()); o.writeDouble(e.getValue()[0]); o.writeDouble(e.getValue()[1]); } }
    static void readStats(DataInput in, Map<String, double[]> m) throws IOException { int n = in.readInt(); if (n < 0 || n > 64) throw new IOException("SU2_STATS"); for (int i = 0; i < n; i++) m.put(in.readUTF(), new double[]{in.readDouble(), in.readDouble()}); }
}
