package com.ethk3.novaonepro.strategyv2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** R27-013 replay dataset: ordered legal permutations, gap-aware segments and a content hash.
 * Missing issues are never invented: a gap only starts a new segment (docs/03 3.1). Numbers are 1..10. */
public final class ReplayDatasetR27 {
    public final int[][] draws;
    public final String[] issues;
    public final long[] ordinals;
    /** Segment index for every row; rows t-1,t are causally consecutive iff segment[t-1]==segment[t]. */
    public final int[] segment;
    /** First row of every segment. */
    public final int[] segmentStart;
    public final String datasetHash;
    public final int rejectedRows;
    public static final String READER_ABI_R31="R31_REPLAY_KNOWLEDGE_READER_1", CALENDAR_ABI_R31="SU1_2020_UTC_1440_1";
    public static final int ORIGINAL_MAGIC_R31=0x52333144,MAX_ORIGINAL_BYTES_R31=1048576;
    public final long[] openTimeR31,knownAtR31;
    public final String[] readerAbiR31,knowledgeScopeR31;
    public final boolean[] provenanceRecordedR31;
    public final boolean legacyIdentityR31;
    public final String provenanceHashR31;

    public static final class Row {
        public final String issue; public final long ordinal; public final int[] numbers;
        public final long openTime,knownAt;public final String readerAbi,knowledgeScope;public final boolean provenanceRecorded;
        /** Old original input remains readable with explicitly unknown knowledge time. */
        public Row(String issue,long ordinal,int[] numbers){this(issue,ordinal,numbers,-1,-1,"LEGACY_UNRECORDED","LEGACY_KNOWN_AT_UNKNOWN",false);}
        public Row(String issue,long ordinal,int[] numbers,long openTime,long knownAt,String readerAbi,String knowledgeScope){this(issue,ordinal,numbers,openTime,knownAt,readerAbi,knowledgeScope,true);}
        private Row(String issue,long ordinal,int[] numbers,long openTime,long knownAt,String readerAbi,String knowledgeScope,boolean recorded){
            if(readerAbi==null||knowledgeScope==null||!readerAbi.matches("[A-Za-z0-9_.-]{1,128}")||!knowledgeScope.matches("[A-Za-z0-9_.-]{1,128}"))throw new IllegalArgumentException("R31_REPLAY_READER_IDENTITY");
            if(recorded&&(issue==null||!issue.matches("\\d{12}")||ordinal!=ordinalOf(issue)-java.time.LocalDate.of(2020,1,1).toEpochDay()*1440L))throw new IllegalArgumentException("R31_REPLAY_CANONICAL_CALENDAR");
            if(!recorded&&(openTime>0||knownAt>0||!"LEGACY_UNRECORDED".equals(readerAbi)||!"LEGACY_KNOWN_AT_UNKNOWN".equals(knowledgeScope)))throw new IllegalArgumentException("R31_REPLAY_LEGACY_PROVENANCE_CONFLICT");
            this.issue=issue;this.ordinal=ordinal;this.numbers=numbers;this.openTime=openTime>0?openTime:-1;this.knownAt=knownAt>0?knownAt:-1;this.readerAbi=readerAbi;this.knowledgeScope=knowledgeScope;provenanceRecorded=recorded;
        }
    }

    private ReplayDatasetR27(int[][] draws, String[] issues, long[] ordinals, int rejected,long[] opened,long[] known,String[] reader,String[] scope,boolean[] recorded) {
        this.draws = draws; this.issues = issues; this.ordinals = ordinals; this.rejectedRows = rejected;
        openTimeR31=opened;knownAtR31=known;readerAbiR31=reader;knowledgeScopeR31=scope;provenanceRecordedR31=recorded;
        int t = draws.length; segment = new int[t];
        List<Integer> starts = new ArrayList<>();
        for (int i = 0; i < t; i++) {
            if (i == 0 || ordinals[i] != ordinals[i - 1] + 1) starts.add(i);
            segment[i] = starts.size() - 1;
        }
        segmentStart = new int[starts.size()];
        for (int i = 0; i < segmentStart.length; i++) segmentStart[i] = starts.get(i);
        boolean legacy=true;StringBuilder identity=new StringBuilder("R31_REPLAY_KNOWLEDGE_1|").append(CALENDAR_ABI_R31);
        for(int i=0;i<t;i++){legacy&=!recorded[i];identity.append('|').append(issues[i]).append(':').append(ordinals[i]).append(':').append(opened[i]).append(':').append(known[i]).append(':').append(reader[i]).append(':').append(scope[i]).append(':').append(recorded[i]);}
        legacyIdentityR31=legacy;provenanceHashR31=StatsR27.sha256(identity.toString());
        String original=hash(issues,draws,segment);datasetHash=legacy?original:StatsR27.sha256("R31_REPLAY_DATASET_V3|"+original+"|"+provenanceHashR31);
    }

    public int size() { return draws.length; }
    public int segments() { return segmentStart.length; }
    /** Number of gaps (missing-issue holes) — equals segments-1 by construction. */
    public int gaps() { return Math.max(0, segmentStart.length - 1); }
    /** True when rows t-lag..t are all in one causally consecutive segment. */
    public boolean consecutive(int t, int lag) { return t - lag >= 0 && segment[t - lag] == segment[t]; }
    public int number(int t, int position) { return draws[t][position - 1]; }

    public static boolean legal(int[] p) {
        if (p == null || p.length != 10) return false;
        int seen = 0;
        for (int v : p) { if (v < 1 || v > 10 || (seen & (1 << v)) != 0) return false; seen |= 1 << v; }
        return true;
    }

    /** Builds from arbitrary rows: invalid permutations and duplicate ordinals are rejected (counted), order by ordinal. */
    public static ReplayDatasetR27 of(List<Row> rows) {
        Row[] sorted = rows.toArray(new Row[0]);
        Arrays.sort(sorted, (a, b) -> Long.compare(a.ordinal, b.ordinal));
        List<Row> kept = new ArrayList<>(sorted.length); int rejected = 0;
        for (Row r : sorted) {
            if (!legal(r.numbers) || (!kept.isEmpty() && kept.get(kept.size() - 1).ordinal == r.ordinal)) { rejected++; continue; }
            kept.add(r);
        }
        int[][] d = new int[kept.size()][]; String[] is = new String[kept.size()]; long[] o = new long[kept.size()];
        long[] opened=new long[d.length],known=new long[d.length];String[] reader=new String[d.length],scope=new String[d.length];boolean[] recorded=new boolean[d.length];
        for (int i = 0; i < d.length; i++) {Row row=kept.get(i);d[i]=row.numbers.clone();is[i]=row.issue;o[i]=row.ordinal;opened[i]=row.openTime;known[i]=row.knownAt;reader[i]=row.readerAbi;scope[i]=row.knowledgeScope;recorded[i]=row.provenanceRecorded;}
        return new ReplayDatasetR27(d,is,o,rejected,opened,known,reader,scope,recorded);
    }

    /** Incremental append of one newer draw (docs/03 3.2): old rows and their segments are unchanged. */
    public ReplayDatasetR27 append(Row r) {
        if (!legal(r.numbers)) throw new IllegalArgumentException("R27_REPLAY_ILLEGAL_PERMUTATION");
        if (size() > 0 && r.ordinal <= ordinals[size() - 1]) throw new IllegalArgumentException("R27_REPLAY_APPEND_NOT_NEWER");
        int n = size();
        int[][] d = Arrays.copyOf(draws, n + 1); d[n] = r.numbers.clone();
        String[] is = Arrays.copyOf(issues, n + 1); is[n] = r.issue;
        long[] o = Arrays.copyOf(ordinals, n + 1); o[n] = r.ordinal;
        long[] opened=Arrays.copyOf(openTimeR31,n+1),known=Arrays.copyOf(knownAtR31,n+1);opened[n]=r.openTime;known[n]=r.knownAt;
        String[] reader=Arrays.copyOf(readerAbiR31,n+1),scope=Arrays.copyOf(knowledgeScopeR31,n+1);reader[n]=r.readerAbi;scope[n]=r.knowledgeScope;
        boolean[] recorded=Arrays.copyOf(provenanceRecordedR31,n+1);recorded[n]=r.provenanceRecorded;
        return new ReplayDatasetR27(d,is,o,rejectedRows,opened,known,reader,scope,recorded);
    }

    /** Keeps only the newest {@code limit} rows (the database retains ≤5000 draws). */
    public ReplayDatasetR27 tail(int limit) {
        if (size() <= limit) return this;
        int from = size() - limit;
        return new ReplayDatasetR27(Arrays.copyOfRange(draws, from, size()), Arrays.copyOfRange(issues, from, size()), Arrays.copyOfRange(ordinals, from, size()), rejectedRows,Arrays.copyOfRange(openTimeR31,from,size()),Arrays.copyOfRange(knownAtR31,from,size()),Arrays.copyOfRange(readerAbiR31,from,size()),Arrays.copyOfRange(knowledgeScopeR31,from,size()),Arrays.copyOfRange(provenanceRecordedR31,from,size()));
    }

    /** Same rows with a different label sequence (used by tests and the shuffled-label control). */
    public ReplayDatasetR27 withDraws(int[][] replacement) {
        if (replacement.length != size()) throw new IllegalArgumentException("R27_REPLAY_SHAPE");
        for (int[] p : replacement) if (!legal(p)) throw new IllegalArgumentException("R27_REPLAY_ILLEGAL_PERMUTATION");
        int[][] owned=new int[replacement.length][];for(int i=0;i<owned.length;i++)owned[i]=replacement[i].clone();
        return new ReplayDatasetR27(owned,issues.clone(),ordinals.clone(),rejectedRows,openTimeR31.clone(),knownAtR31.clone(),readerAbiR31.clone(),knowledgeScopeR31.clone(),provenanceRecordedR31.clone());
    }

    /** Preserve provenance when producers split/tail an immutable input. */
    public Row rowR31(int index){return new Row(issues[index],ordinals[index],draws[index].clone(),openTimeR31[index],knownAtR31[index],readerAbiR31[index],knowledgeScopeR31[index],provenanceRecordedR31[index]);}
    /** Past availability check; UNKNOWN never becomes a positive PIT proof. */
    public boolean knownBeforeR31(int index,long decisionTime){return provenanceRecordedR31[index]&&knownAtR31[index]>0&&knownAtR31[index]<decisionTime&&(!"DRAW_FIRST_SEEN".equals(knowledgeScopeR31[index])||(openTimeR31[index]>0&&knownAtR31[index]>=openTimeR31[index]));}
    public java.util.Map<String,Object> provenanceR31(){
        java.util.Map<String,Object> out=new java.util.LinkedHashMap<>();int known=0,invalidChronology=0;java.util.Set<String> readers=new java.util.TreeSet<>(),scopes=new java.util.TreeSet<>();
        for(int i=0;i<size();i++){if(provenanceRecordedR31[i]&&knownAtR31[i]>0){known++;if("DRAW_FIRST_SEEN".equals(knowledgeScopeR31[i])&&(openTimeR31[i]<=0||knownAtR31[i]<openTimeR31[i]))invalidChronology++;}readers.add(readerAbiR31[i]);scopes.add(knowledgeScopeR31[i]);}
        out.put("identityVersion",legacyIdentityR31?"LEGACY_V2_KNOWN_AT_UNKNOWN":"R31_DATASET_V3");out.put("calendarAbi",legacyIdentityR31?"LEGACY_ORDINAL_ORIGIN_UNKNOWN":CALENDAR_ABI_R31);out.put("provenanceHash",provenanceHashR31);out.put("readerAbis",new ArrayList<>(readers));out.put("knowledgeScopes",new ArrayList<>(scopes));out.put("rows",size());out.put("recordedKnownAtRows",known);out.put("invalidKnownAtChronologyRows",invalidChronology);out.put("unknownKnownAtRows",size()-known);out.put("naturalPastVisibilityProven",false);return out;
    }
    /** Compact dictionaries keep 10k real original rows within the unchanged
     * 1MiB recovery bound. No per-row timestamp is synthesized. */
    public byte[] originalBytesR31(){
        if(size()>10000)throw new IllegalArgumentException("R31_REPLAY_ORIGINAL_ROW_BOUND");
        try{java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();java.io.DataOutputStream out=new java.io.DataOutputStream(bytes);
            java.util.List<String> readers=new ArrayList<>(),scopes=new ArrayList<>();for(int i=0;i<size();i++){if(!readers.contains(readerAbiR31[i]))readers.add(readerAbiR31[i]);if(!scopes.contains(knowledgeScopeR31[i]))scopes.add(knowledgeScopeR31[i]);}
            if(readers.size()>128||scopes.size()>128)throw new IllegalArgumentException("R31_REPLAY_READER_DICTIONARY_BOUND");
            out.writeInt(ORIGINAL_MAGIC_R31);out.writeInt(1);out.writeInt(size());out.writeUTF(datasetHash);out.writeUTF(provenanceHashR31);
            for(java.util.List<String> dictionary:Arrays.asList(readers,scopes)){out.writeInt(dictionary.size());for(String value:dictionary)out.writeUTF(value);}
            for(int i=0;i<size();i++){out.writeUTF(issues[i]);out.writeLong(ordinals[i]);for(int n:draws[i])out.writeByte(n);out.writeLong(openTimeR31[i]);out.writeLong(knownAtR31[i]);out.writeByte(readers.indexOf(readerAbiR31[i]));out.writeByte(scopes.indexOf(knowledgeScopeR31[i]));out.writeBoolean(provenanceRecordedR31[i]);if(bytes.size()>MAX_ORIGINAL_BYTES_R31)throw new IllegalArgumentException("R31_REPLAY_ORIGINAL_BYTE_BOUND");}
            out.close();return bytes.toByteArray();
        }catch(java.io.IOException impossible){throw new IllegalStateException("R31_REPLAY_ORIGINAL_ENCODE",impossible);}
    }
    public static ReplayDatasetR27 originalBytesR31(byte[] bytes){
        if(bytes==null||bytes.length>MAX_ORIGINAL_BYTES_R31)throw new IllegalArgumentException("R31_REPLAY_ORIGINAL_BYTE_BOUND");
        try{java.io.DataInputStream in=new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes));if(in.readInt()!=ORIGINAL_MAGIC_R31||in.readInt()!=1)throw new java.io.IOException("READER_VERSION");int n=in.readInt();if(n<0||n>10000)throw new java.io.IOException("ROW_BOUND");String dataset=in.readUTF(),provenance=in.readUTF();
            java.util.List<java.util.List<String>> dictionaries=new ArrayList<>();for(int d=0;d<2;d++){int count=in.readInt();if(count<0||count>128)throw new java.io.IOException("DICTIONARY_BOUND");java.util.List<String> dictionary=new ArrayList<>();for(int j=0;j<count;j++){String value=in.readUTF();if(!value.matches("[A-Za-z0-9_.-]{1,128}"))throw new java.io.IOException("READER_IDENTITY");dictionary.add(value);}dictionaries.add(dictionary);}
            java.util.List<Row> rows=new ArrayList<>();for(int i=0;i<n;i++){String issue=in.readUTF();long ordinal=in.readLong();int[] numbers=new int[10];for(int p=0;p<10;p++)numbers[p]=in.readUnsignedByte();long opened=in.readLong(),known=in.readLong();int reader=in.readUnsignedByte(),scope=in.readUnsignedByte();boolean recorded=in.readBoolean();if(reader>=dictionaries.get(0).size()||scope>=dictionaries.get(1).size())throw new java.io.IOException("READER_DICTIONARY_INDEX");rows.add(new Row(issue,ordinal,numbers,opened,known,dictionaries.get(0).get(reader),dictionaries.get(1).get(scope),recorded));}
            ReplayDatasetR27 result=of(rows);if(in.read()!=-1||result.rejectedRows!=0||result.size()!=n||!dataset.equals(result.datasetHash)||!provenance.equals(result.provenanceHashR31))throw new java.io.IOException("ORIGINAL_IDENTITY");return result;
        }catch(java.io.IOException invalid){throw new IllegalArgumentException("R31_REPLAY_ORIGINAL_DECODE",invalid);}
    }

    /** R30-B03-02: sha256(first issue | last issue | row count | normalized rows). A new hash is a new dataset
     * generation: fingerprints, scores and qualifications keyed by an older hash are not compared across it. */
    static String hash(String[] issues, int[][] draws, int[] segment) {
        StringBuilder b = new StringBuilder("R30_REPLAY_DATASET_V2|").append(issues.length == 0 ? "" : issues[0]).append('|')
                .append(issues.length == 0 ? "" : issues[issues.length - 1]).append('|').append(draws.length);
        for (int i = 0; i < draws.length; i++) {
            b.append('|').append(issues[i]).append(':');
            for (int v : draws[i]) b.append((char) ('0' + v - 1));
            b.append(':').append(segment[i]);
        }
        return StatsR27.sha256(b.toString());
    }

    /** Ordinal of a yyyyMMddNNNN issue (1440 issues per UTC day); only differences are used. */
    public static long ordinalOf(String issue) {
        if (issue == null || !issue.matches("\\d{12}")) throw new IllegalArgumentException("R27_ISSUE_FORMAT:" + issue);
        java.time.LocalDate day = java.time.LocalDate.of(Integer.parseInt(issue.substring(0, 4)), Integer.parseInt(issue.substring(4, 6)), Integer.parseInt(issue.substring(6, 8)));
        int n = Integer.parseInt(issue.substring(8));
        if (n < 1 || n > 1440) throw new IllegalArgumentException("R27_ISSUE_RANGE:" + issue);
        return day.toEpochDay() * 1440L + n;
    }

    /** Reads the repository CSV format (period,open_time_utc_ms,open_time_local,open_number,block,hash,valid,fetch_time). */
    public static List<Row> readCsv(Reader in) throws IOException {
        List<Row> rows = new ArrayList<>();
        BufferedReader r = new BufferedReader(in);
        String header = r.readLine();
        if (header == null || !header.startsWith("period,")) throw new IOException("R27_CSV_HEADER");
        String line;
        while ((line = r.readLine()) != null) {
            if (line.isEmpty()) continue;
            int q1 = line.indexOf('"'), q2 = line.indexOf('"', q1 + 1);
            if (q1 < 0 || q2 < 0) throw new IOException("R27_CSV_NUMBERS");
            String period = line.substring(0, line.indexOf(','));
            String[] tail = line.substring(q2 + 2).split(",");
            if (tail.length < 3 || !"1".equals(tail[2].trim())) continue;
            String[] ns = line.substring(q1 + 1, q2).split(",");
            int[] p = new int[ns.length];
            for (int i = 0; i < ns.length; i++) p[i] = Integer.parseInt(ns[i].trim());
            rows.add(new Row(period, ordinalOf(period), p));
        }
        return rows;
    }

    public static ReplayDatasetR27 readCsv(java.io.InputStream in) throws IOException {
        return of(readCsv(new java.io.InputStreamReader(in, StandardCharsets.UTF_8)));
    }

    /** Synthetic dataset for tests/null controls: i.i.d. uniform permutations, optional gaps. */
    public static ReplayDatasetR27 synthetic(int n, long seed, int[] gapAfterRows) {
        StatsR27.SplitMix rng = new StatsR27.SplitMix(seed);
        List<Row> rows = new ArrayList<>(n);
        long ordinal = 1_000_000L; int g = 0;
        for (int i = 0; i < n; i++) {
            if (gapAfterRows != null && g < gapAfterRows.length && i == gapAfterRows[g]) { ordinal += 3; g++; }
            rows.add(new Row(String.format(java.util.Locale.ROOT, "S%011d", ordinal), ordinal, randomPermutation(rng)));
            ordinal++;
        }
        return of(rows);
    }

    public static int[] randomPermutation(StatsR27.SplitMix rng) {
        int[] p = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        for (int i = 9; i > 0; i--) { int j = rng.nextInt(i + 1); int x = p[i]; p[i] = p[j]; p[j] = x; }
        return p;
    }

    /** Planted weak signal (docs/01 F, docs/12 H06): with probability q, position 1 takes the previous row's
     * position-2 number by swapping two places, so every row stays a legal permutation. */
    public ReplayDatasetR27 planted(double q, long seed) {
        StatsR27.SplitMix rng = new StatsR27.SplitMix(seed);
        int[][] d = new int[size()][];
        for (int t = 0; t < size(); t++) {
            d[t] = draws[t].clone();
            if (t > 0 && consecutive(t, 1) && rng.chance(q)) {
                int want = d[t - 1][1];
                for (int j = 0; j < 10; j++) if (d[t][j] == want) { d[t][j] = d[t][0]; d[t][0] = want; break; }
            }
        }
        return withDraws(d);
    }
}
