package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Durable form of the continuing evolution (P23): population, Hall of Fame, tier champions, director memory. */
public final class ModelStateCodecSU2 {
    private ModelStateCodecSU2() {}
    static final int MAGIC = 0x53553213, MAX = 4 << 20;

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
            if (in.readInt() != MAGIC) throw new IOException("SU2_EVOLUTION_STATE_MAGIC");
            readList(in, s.population); readList(in, s.hallOfFame);
            int n = in.readInt(); if (n < 0 || n > 3) throw new IOException("SU2_TIERS");
            for (int i = 0; i < n; i++) s.tiers.put(in.readUTF(), readRanked(in));
            if (in.readBoolean()) s.champion = readRanked(in);
            readStats(in, s.kindStats); readStats(in, s.opStats);
            s.effectiveGenerations = in.readInt();
            int notes = in.readInt(); if (notes < 0 || notes > 200) throw new IOException("SU2_NOTES");
            for (int i = 0; i < notes; i++) s.repairNotes.add(in.readUTF());
        }
        return s;
    }

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
