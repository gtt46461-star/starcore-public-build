package com.ethk3.novaonepro;
import java.io.*;import java.nio.channels.*;import org.json.*;
/** Cross-process FIFO.32 pending requests are durable; overflow is rejected explicitly, never overwrites an older request. */
public final class ReportRequestQueueCP12 {
 private ReportRequestQueueCP12(){}
 private static final Object JVM_LOCK=new Object();
 private interface Work<T>{T run(JSONObject queue)throws Exception;}
 public static void enqueue(File root,String id,String mode)throws Exception{hash(id);if(!mode.equals("FULL_REPORT")&&!mode.equals("RETENTION_PAGE")&&!mode.equals("SAF_TRANSFER"))throw new IOException("REPORT_QUEUE_MODE");locked(root,queue->{JSONArray rows=queue.getJSONArray("requests");for(int i=0;i<rows.length();i++)if(rows.getJSONObject(i).getString("requestId").equals(id))return null;if(rows.length()>=32)throw new IOException("REPORT_QUEUE_FULL");JSONObject item=new JSONObject();item.put("requestId",id);item.put("mode",mode);item.put("createdWallMs",System.currentTimeMillis());rows.put(item);return null;});}
 public static String head(File root)throws Exception{return locked(root,queue->{JSONArray rows=queue.getJSONArray("requests");return rows.length()==0?null:rows.getJSONObject(0).getString("requestId");});}
 public static JSONArray pending(File root)throws Exception{return locked(root,q->new JSONArray(q.getJSONArray("requests").toString()));}
 public static boolean hasInteractive(File root){return new File(root,"interactive-pending-r69.json").isFile();}
 public static String interactiveHead(File root)throws Exception{return locked(root,q->{JSONArray rows=q.getJSONArray("requests");for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);if(!"RETENTION_PAGE".equals(r.getString("mode")))return r.getString("requestId");}return null;});}
 /** Remove the exact claimed/cancelled request, even if a newer interactive request took priority. */
 public static void remove(File root,String id)throws Exception{hash(id);locked(root,q->{JSONArray rows=q.getJSONArray("requests"),keep=new JSONArray();for(int i=0;i<rows.length();i++)if(!id.equals(rows.getJSONObject(i).getString("requestId")))keep.put(rows.getJSONObject(i));q.put("requests",keep);return null;});}
 /** Bounded snapshot used by the single report owner so maintenance never expires queued work. */
 public static java.util.Set<String> pendingIds(File root)throws Exception{return locked(root,queue->{java.util.Set<String> ids=new java.util.HashSet<>();JSONArray rows=queue.getJSONArray("requests");for(int i=0;i<rows.length();i++)ids.add(rows.getJSONObject(i).getString("requestId"));return ids;});}
 public static void removeHead(File root,String id)throws Exception{hash(id);locked(root,queue->{JSONArray rows=queue.getJSONArray("requests");if(rows.length()==0||!rows.getJSONObject(0).getString("requestId").equals(id))return null;JSONArray remaining=new JSONArray();for(int i=1;i<rows.length();i++)remaining.put(rows.getJSONObject(i));queue.put("requests",remaining);return null;});}
 private static <T>T locked(File root,Work<T> work)throws Exception{synchronized(JVM_LOCK){if(!root.isDirectory()&&!root.mkdirs())throw new IOException("REPORT_QUEUE_ROOT");File journal=ReportArchiveVerifierCP12.inside(root,"request-queue.json");try(RandomAccessFile io=new RandomAccessFile(ReportArchiveVerifierCP12.inside(root,"request-queue.lock"),"rw");FileLock lock=io.getChannel().lock()){
  JSONObject queue=journal.exists()?new JSONObject(ReportJobJournalCP12.readAtomic(journal,16384)):new JSONObject();if(!journal.exists()){queue.put("format","REPORT_FIFO_CP12_1");queue.put("generation",0);queue.put("requests",new JSONArray());}
  if(!queue.getString("format").equals("REPORT_FIFO_CP12_1"))throw new IOException("REPORT_QUEUE_FORMAT");JSONArray rows=queue.getJSONArray("requests");if(rows.length()>32)throw new IOException("REPORT_QUEUE_BOUND");for(int i=0;i<rows.length();i++)hash(rows.getJSONObject(i).getString("requestId"));T result=work.run(queue);queue.put("generation",Math.addExact(queue.getLong("generation"),1));ReportJobJournalCP12.writeAtomic(journal,queue.toString());
  boolean interactive=false;JSONArray pending=queue.getJSONArray("requests");for(int i=0;i<pending.length();i++)if(!"RETENTION_PAGE".equals(pending.getJSONObject(i).getString("mode")))interactive=true;
  File signal=ReportArchiveVerifierCP12.inside(root,"interactive-pending-r69.json");if(interactive&&!signal.exists())ReportJobJournalCP12.writeAtomic(signal,"{}");else if(!interactive&&signal.exists()&&!signal.delete())throw new IOException("REPORT_QUEUE_SIGNAL_CLEANUP");return result;
 }}}
 private static void hash(String id)throws IOException{if(id==null||!id.matches("[0-9a-f]{64}"))throw new IOException("REPORT_QUEUE_ID");}
}
