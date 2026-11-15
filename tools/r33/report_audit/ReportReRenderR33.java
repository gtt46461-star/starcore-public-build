package com.ethk3.novaonepro.strategyv2;
import java.util.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.io.*;
import com.ethk3.novaonepro.ReportJsonWriterR31;
public final class ReportReRenderR33 {
 @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception {
  Map<String,Object> raw=(Map<String,Object>)JsonR27.parse(Files.readString(Path.of(args[0])));
  Map<String,Object> facts=(Map<String,Object>)raw.get("facts"),watermark=(Map<String,Object>)raw.get("watermark"),data=(Map<String,Object>)raw.get("sources");
  data.put("objectIndex",ReportObjectIndexR30.buildProductionR31(facts,watermark,data));
  ReportR27.R30Input input=new ReportR27.R30Input(facts,watermark,data,(List<String>)raw.get("errors"));
  List<Map<String,Object>> chapters=ReportR29.fromSnapshotR30(input);Map<String,Object> doc=ReportR29.document(chapters);
  if(doc.get("metricResults")!=input.metricResults)throw new AssertionError("ROOT_METRICS_NOT_ORIGINAL");
  ReportEvidenceProjectionR33.validateDocument(doc);
  com.ethk3.novaonepro.ReportJobJournalCP12.Job job=new com.ethk3.novaonepro.ReportJobJournalCP12.Job(String.valueOf(watermark.get("reportJobId")),String.valueOf(watermark.get("reportJobId")),StatsR27.sha256("offline-rerender"),"FULL_REPORT",1L);job.snapshotSha=String.valueOf(((Map<?,?>)watermark.get("sourceManifest")).get("snapshotSha256"));com.ethk3.novaonepro.ReportSemanticVerifierR31.validateDocument(doc,job);
  try(Writer out=Files.newBufferedWriter(Path.of(args[1]+".json"),StandardCharsets.UTF_8)){ReportJsonWriterR31.write(doc,out);}
  try(Writer out=Files.newBufferedWriter(Path.of(args[1]+".md"),StandardCharsets.UTF_8)){ReportJsonWriterR31.markdownR30((List<Map<String,Object>>)doc.get("chapters"),out);}
  Map<String,Object> receipt=new LinkedHashMap<>();receipt.put("scope","OFFLINE_RERENDER_OF_CAPTURED_REPORT_FACTS_AND_BOUNDED_SOURCES; NO_NEW_DATABASE_OR_DEVICE_RUN; NO_RECOVERED_OR_INVENTED_PER_OBJECT_COUNTERS");receipt.put("input",args[0]);receipt.put("jsonBytes",Files.size(Path.of(args[1]+".json")));receipt.put("markdownBytes",Files.size(Path.of(args[1]+".md")));receipt.put("factsUnchanged",facts.equals(input.facts));receipt.put("watermarkUnchanged",watermark.equals(input.watermark));receipt.put("metricCount",input.metricResults.size());receipt.put("assessment",doc.get("evidenceAssessmentR33"));Files.writeString(Path.of(args[1]+"_receipt.json"),JsonR27.write(receipt));System.out.println(JsonR27.write(receipt));
 }
}
