package com.ethk3.novaonepro.r952;
import com.ethk3.novaonepro.*;
import java.io.*;import java.nio.file.*;import java.util.*;
/** Same fixture runs against unmodified R71 and current actual product bytecode. */
public final class DevelopmentIoR72 {
 static int checks;static void ok(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
 static final class Sink extends ByteArrayOutputStream {int calls;public synchronized void write(int b){calls++;super.write(b);}public synchronized void write(byte[]b,int o,int n){calls++;super.write(b,o,n);}}
 static final class Source extends ByteArrayInputStream {int calls;Source(byte[]b){super(b);}public synchronized int read(){calls++;return super.read();}public synchronized int read(byte[]b,int o,int n){calls++;return super.read(b,o,n);}}
 public static void main(String[]a)throws Exception {
  Map<String,Object> h=new TreeMap<>();h.put("revision","SCDEV1");h.put("game","PK10");h.put("build","R72_FIXTURE");h.put("sourceManifestHash",V51ChecksumEngine.sha256("isolated synthetic data"));h.put("scheduleRevision","R967");h.put("bindings",Collections.emptyList());
  DevelopmentInputCP9.Header header=new DevelopmentInputCP9.Header(StrictCanonicalJson.writeContext(h));
  List<Models.Draw> history=new ArrayList<>();String issue="202610010001";
  for(int i=0;i<5000;i++){Models.Draw d=new Models.Draw();d.issue=issue;d.openTime=1_700_000_000_000L+60000L*i;for(int j=0;j<10;j++)d.numbers[j]=(i+j)%10+1;history.add(d);issue=TargetIssueResolver.next(issue).targetIssue;}
  Sink sink=new Sink();DevelopmentInputCP9.Receipt receipt=DevelopmentInputCP9.write(sink,header,history);byte[]bytes=sink.toByteArray();Files.write(Path.of(a[0]),bytes);
  Source source=new Source(bytes);MemoryBudgetCP4 budget=new MemoryBudgetCP4(8L<<20);
  try(DevelopmentInputCP9.Input input=DevelopmentInputCP9.read(source,receipt.byteCount,receipt.inputHash,header,budget)){
   ok(input.rowCount()==5000,"row count");for(int i=0;i<5000;i++){ok(input.issue(i).equals(history.get(i).issue),"issue");for(int p=1;p<=10;p++)ok(input.actual(i,p)==history.get(i).numbers[p-1],"number");}
  }ok(budget.used()==0,"input lease released");
  for(int mode=0;mode<3;mode++) {byte[] bad=mode==0?Arrays.copyOf(bytes,bytes.length+1):bytes.clone();if(mode==1)bad[bad.length-50]^=1;if(mode==2)bad=Arrays.copyOf(bytes,bytes.length-1);boolean rejected=false;
   try(DevelopmentInputCP9.Input ignored=DevelopmentInputCP9.read(new Source(bad),receipt.byteCount,receipt.inputHash,header,budget)){}catch(IOException|IllegalArgumentException expected){rejected=true;}ok(rejected,"trailing/corrupt/truncated rejection "+mode);ok(budget.used()==0,"failure lease release");}
  for(boolean bound:new boolean[]{false,true})for(int required:new int[]{0,20,50,500,2000,4579}){
   List<Models.Draw> full=DevelopmentProductionCP12.selectHistoryCP12(history,required,bound);
   int limit=Math.min(5000,Math.max(20,required)+(bound?421:1621));
   List<Models.Draw> capped=DevelopmentProductionCP12.selectHistoryCP12(history.subList(history.size()-limit,history.size()),required,bound);
   ok(full.size()==capped.size(),"history window size");for(int i=0;i<full.size();i++)ok(full.get(i)==capped.get(i),"exact history row");
  }
  String json="{\"checks\":"+checks+",\"input_sha256\":\""+receipt.inputHash+"\",\"bytes\":"+bytes.length+",\"source_read_calls\":"+source.calls+",\"destination_write_calls\":"+sink.calls+"}";
  Files.writeString(Path.of(a[0]+".json"),json+"\n");System.out.println(json);
 }
}
