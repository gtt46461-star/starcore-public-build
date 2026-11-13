package com.ethk3.novaonepro;

import com.ethk3.novaonepro.strategyv2.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import org.json.*;

/** The generated document and its actual delivered ZIP share a hash-bound
 * semantic receipt. ZIP readback streams every member/CRC, including split ZIPs;
 * it does not expand another complete report object in memory. */
public final class ReportSemanticVerifierR31 {
    private ReportSemanticVerifierR31() {}
    public static final String RECEIPT="R31_SEMANTIC_RECEIPT.json";
    private static Map<?,?> map(Object value)throws IOException {
        if(!(value instanceof Map))throw new IOException("REPORT_SEMANTIC_OBJECT_MISSING");return (Map<?,?>)value;
    }
    private static List<?> list(Object value)throws IOException {
        if(!(value instanceof List))throw new IOException("REPORT_SEMANTIC_ARRAY_MISSING");return (List<?>)value;
    }
    private static void require(boolean condition,String reason)throws IOException {if(!condition)throw new IOException("REPORT_SEMANTIC_"+reason);}
    private static void exactIds(Object values,String field,Set<String> expected,String reason)throws IOException {
        Set<String> actual=new HashSet<>();
        for(Object value:list(values)) {
            Object id=map(value).get(field);
            require(id instanceof String&&actual.add((String)id),reason+"_DUPLICATE_OR_MISSING_ID");
        }
        require(actual.equals(expected),reason+"_ID_SET");
    }
    private static void checkpoint(ReportNativeCP4.Control control)throws IOException {
        if(Thread.currentThread().isInterrupted()||!control.shouldContinue())throw new InterruptedIOException("REPORT_SEMANTIC_CANCELLED");
    }
    public static void validateDocument(Map<String,Object> document,ReportJobJournalCP12.Job job)throws IOException {
        require(list(document.get("chapters")).size()==12,"CHAPTER_COUNT");
        require(MetricDefinitionsR29.R31_VERSION.equals(document.get("metricRegistryVersion")),"METRIC_VERSION");
        Map<?,?> watermark=map(document.get("snapshotWatermark"));
        require(job.jobId.equals(watermark.get("reportJobId")),"JOB_ID");
        require(job.snapshotSha.equals(map(watermark.get("sourceManifest")).get("snapshotSha256")),"SNAPSHOT_ID");
        Set<String> chapters=new HashSet<>();for(int i=1;i<=12;i++)chapters.add(String.format(Locale.ROOT,"R30_%02d",i));
        exactIds(document.get("chapters"),"sectionId",chapters,"CHAPTER");
        Set<String> metrics=new HashSet<>();for(Object raw:list(document.get("metricResults"))) {
            Map<?,?> metric=map(raw);String id=String.valueOf(metric.get("metricId"));
            require(metrics.add(id),"DUPLICATE_METRIC");require(metric.get("r31Definition") instanceof Map&&metric.get("r31Evaluation") instanceof Map,"UNWIRED_METRIC:"+id);
        }
        require(metrics.size()==44,"METRIC_COUNT");for(CheckMetricCrosswalkR31.Metric definition:CheckMetricCrosswalkR31.definitions())require(metrics.contains(definition.id),"MISSING_METRIC:"+definition.id);
        Map<?,?> objects=map(document.get("objectIndex"));
        for(Object[] expected:new Object[][]{{"legacyCharts",29},{"researchCharts",18},{"matrices",20},{"logicalTables",16},{"capabilities",82}})
            require(list(map(objects.get(expected[0])).get("objects")).size()==(Integer)expected[1],"OBJECT_COUNT:"+expected[0]);
        Set<String> legacy=new HashSet<>(ChartRegistryR31.visibleLegacyIds()),research=new HashSet<>(),matrices=new HashSet<>(),capabilities=new HashSet<>();
        for(ChartRegistryR31.Entry entry:ChartRegistryR31.all())if(entry.group==ChartRegistryR31.Group.RESEARCH)research.add(entry.id);
        for(MatrixRegistryR30.Definition entry:MatrixRegistryR30.all())matrices.add(entry.id);
        for(CapabilityRegistryR30.Definition entry:CapabilityRegistryR30.all())capabilities.add(entry.id);
        for(Object[] expected:new Object[][]{{"legacyCharts",legacy},{"researchCharts",research},{"matrices",matrices},{"capabilities",capabilities}}) {
            @SuppressWarnings("unchecked") Set<String> ids=(Set<String>)expected[1];
            exactIds(map(objects.get(expected[0])).get("objects"),"objectId",ids,"OBJECT:"+expected[0]);
        }
        require(list(document.get("researchTables")).size()==16,"TABLE_READ_RECEIPTS");
        Set<String> tables=new HashSet<>(ResearchTableAdaptersR30R1.ids());
        exactIds(document.get("researchTables"),"id",tables,"TABLE_READER");
        exactIds(map(objects.get("logicalTables")).get("objects"),"id",tables,"TABLE_OBJECT");
        for(Object raw:list(document.get("chapters"))) {
            Map<?,?> chapter=map(raw);require(Objects.equals(watermark,chapter.get("snapshotWatermark")),"CHAPTER_WATERMARK");
            if("R30_12".equals(chapter.get("sectionId"))) {
                Map<?,?> complete=map(chapter.get("completeSnapshotEvidenceR31"));
                require("R31_COMPLETE_CAPTURED_EVIDENCE_V1".equals(complete.get("format")),"COMPLETE_EVIDENCE_VERSION");
                require(Objects.equals(watermark,complete.get("snapshotWatermark")),"COMPLETE_EVIDENCE_WATERMARK");
                map(complete.get("facts"));map(complete.get("sources"));list(complete.get("sourceErrors"));
            }
        }
    }
    private static void combine(byte[] total,String name,String hash)throws Exception {
        byte[] value=MessageDigest.getInstance("SHA-256").digest((name+"\u0000"+hash).getBytes(StandardCharsets.UTF_8));
        for(int i=0;i<total.length;i++)total[i]^=value[i];
    }
    public static File writeReceipt(File directory,Map<String,Object> document,ReportCatalogCP4.Result catalog,
            ReportJobJournalCP12.Job job,boolean raw,ReportNativeCP4.Control control)throws Exception {
        validateDocument(document,job);Set<String> expected=new TreeSet<>();for(CheckMetricCrosswalkR31.Check check:CheckMetricCrosswalkR31.checks())expected.add(check.id);
        Map<String,Object> checks=new TreeMap<>();byte[] catalogDigest=new byte[32];int pages=0;
        for(File page:catalog.indexFiles) {
            checkpoint(control);String hash=ReportArchiveCP4.hash(page,control);combine(catalogDigest,page.getName(),hash);pages++;
            try(ReportArchiveVerifierCP12.LineReader lines=new ReportArchiveVerifierCP12.LineReader(new FileInputStream(page),131072)) {
                byte[] line;while((line=lines.next())!=null) {
                    checkpoint(control);JSONObject row=new JSONObject(new String(line,StandardCharsets.UTF_8));
                    if(!"check_result".equals(row.optString("kind")))continue;
                    String id=row.getString("id");require(expected.contains(id)&&!checks.containsKey(id),"CHECK_ID:"+id);
                    Map<String,Object> result=new LinkedHashMap<>();for(String field:new String[]{"execution_state","domain_outcome","health","completes_contract"})result.put(field,row.opt(field));checks.put(id,result);
                }
            }
        }
        require(checks.keySet().equals(expected)&&checks.size()==96,"CHECK_COUNT_OR_SET");
        Map<String,Object> receipt=new LinkedHashMap<>();receipt.put("format","R31_REPORT_SEMANTICS_V1");receipt.put("jobId",job.jobId);receipt.put("snapshotSha256",job.snapshotSha);
        receipt.put("sourceCommit",map(document.get("snapshotWatermark")).get("sourceCommit"));receipt.put("reportSha256",ReportArchiveCP4.hash(new File(directory,"R30_REPORT.json"),control));
        receipt.put("chapters",12);receipt.put("metrics",44);receipt.put("checks",checks);receipt.put("catalogPages",pages);receipt.put("catalogHashXor",ReportArchiveCP4.hex(catalogDigest));receipt.put("rawEvidence",raw);
        receipt.put("scope","DOCUMENT_IDENTITY_OBJECT_AND_CHECK_COVERAGE_NOT_BUSINESS_HEALTH_OR_NATURAL_QUALITY");
        File file=new File(directory,RECEIPT);ReportJobJournalCP12.writeAtomic(file,JsonR27.write(receipt));return file;
    }
    private static final class Parts extends InputStream {
        final List<File> files;int at;InputStream current;
        Parts(List<File> files){this.files=files;}
        @Override public int read()throws IOException{byte[] one=new byte[1];int n=read(one,0,1);return n<0?-1:one[0]&255;}
        @Override public int read(byte[] bytes,int offset,int length)throws IOException {
            if(length==0)return 0;for(;;){if(current==null){if(at>=files.size())return -1;current=new FileInputStream(files.get(at++));}
                int n=current.read(bytes,offset,length);if(n>=0)return n;current.close();current=null;}
        }
        @Override public void close()throws IOException{if(current!=null){current.close();current=null;}}
    }
    /** Reads exactly the files the Provider/SAF consumers receive, before READY. */
    public static void verifyDelivery(File directory,ReportJobJournalCP12.Job job,ReportNativeCP4.Control control)throws Exception {
        File receiptFile=new File(directory,RECEIPT);JSONObject receipt=new JSONObject(ReportJobJournalCP12.readAtomic(receiptFile,65536));
        require(job.jobId.equals(receipt.getString("jobId"))&&job.snapshotSha.equals(receipt.getString("snapshotSha256")),"RECEIPT_IDENTITY");
        JSONObject index=new JSONObject(ReportJobJournalCP12.readAtomic(new File(directory,"delivery/index.json"),65536));require(job.jobId.equals(index.getString("jobId")),"DELIVERY_JOB_ID");
        require(Arrays.asList("SCREPORT_SINGLE_ZIP_R992",ReportPublicationCP12.ZIP_PART_FORMAT_R30).contains(index.getString("format")),"DELIVERY_FORMAT");
        if(ReportPublicationCP12.ZIP_PART_FORMAT_R30.equals(index.getString("format")))ReportPublicationCP12.verifyPartsR30(directory,index,control);
        JSONArray rows=index.getJSONArray("files");require(rows.length()>0&&rows.length()<=128,"DELIVERY_COUNT");List<File> files=new ArrayList<>();
        for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);File file=ReportArchiveVerifierCP12.inside(directory,row.getString("path"));require(file.length()==row.getLong("bytes")&&ReportArchiveCP4.hash(file,control).equals(row.getString("sha256")),"DELIVERY_FILE_HASH");files.add(file);}
        Set<String> required=new HashSet<>(Arrays.asList("R30_REPORT.json","R30_REPORT.md",RECEIPT,"REPORT_DETAILS.txt","R26_REPORT.json","R27_REPORT.md","R26_POLICY_IDENTITIES.jsonl","R26_FROZEN_SURFACE.json","STORAGE_CENSUS.txt",receipt.getBoolean("rawEvidence")?"snapshot.db":"DIAGNOSTIC_SCOPE.json"));
        byte[] catalogDigest=new byte[32];int pages=0;String reportHash=null,receiptHash=null;Set<String> names=new HashSet<>();
        try(ZipInputStream zip=new ZipInputStream(new BufferedInputStream(new Parts(files),32768))) {
            ZipEntry entry;byte[] buffer=new byte[32768];while((entry=zip.getNextEntry())!=null) {
                checkpoint(control);String name=entry.getName();require(!name.startsWith("/")&&!name.contains("..")&&!name.contains("\\"),"ZIP_PATH");
                require(names.size()<200000&&names.add(name),"ZIP_DUPLICATE_OR_EXCESS_MEMBERS");
                MessageDigest digest=MessageDigest.getInstance("SHA-256");int n;long count=0;while((n=zip.read(buffer))!=-1){checkpoint(control);digest.update(buffer,0,n);count=Math.addExact(count,n);}zip.closeEntry(); // CRC is checked by ZipInputStream.
                String hash=ReportArchiveCP4.hex(digest.digest());required.remove(name);
                if(name.equals("snapshot.db"))require(job.snapshotSha.equals(hash)&&job.snapshotBytes==count,"ZIP_SNAPSHOT_HASH");
                if(name.equals("R30_REPORT.json"))reportHash=hash;if(name.equals(RECEIPT))receiptHash=hash;
                // The existing archive protocol names catalog pages at its root;
                // their private source paths are catalog/catalog-NNNN.jsonl.
                if(name.matches("catalog-[0-9]{4,}\\.jsonl")){combine(catalogDigest,name,hash);pages++;}
            }
        }
        require(required.isEmpty(),"ZIP_MISSING_MEMBERS:"+required);
        require(receipt.getString("reportSha256").equals(reportHash)&&ReportArchiveCP4.hash(receiptFile,control).equals(receiptHash),"ZIP_DOCUMENT_OR_RECEIPT_HASH");
        require(pages==receipt.getInt("catalogPages")&&ReportArchiveCP4.hex(catalogDigest).equals(receipt.getString("catalogHashXor")),"ZIP_CHECK_EVIDENCE_CLOSURE");
    }
}
