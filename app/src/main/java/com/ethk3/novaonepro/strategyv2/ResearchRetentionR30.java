package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.*;

/** Total physical regular-file budget. Evidence has no automatic deletion policy; only explicit duplicates/caches do. */
public final class ResearchRetentionR30 {
    private ResearchRetentionR30() {}
    public static final long DEFAULT_BUDGET_BYTES=1_500_000_000L; // decimal 1.5 GB, reported in bytes
    public static final int MAX_FILES_PER_STEP=64, MAX_SCAN_ENTRIES=200_000, MAX_DEPTH=16;
    public static final String RETIRED="databases/starcore-r29-research.db.r30-retired";
    public static final List<String> PROTECTED_TABLES=Collections.unmodifiableList(Arrays.asList(
        "r26_incumbent_frozen","r26_frozen","r26_feedback","r26_consumption","r26_policy_identity","r26_checkpoint_receipt","r26_gap_receipt","r26_expert_audit","r26_evolution","r26_foundry","r26_legacy_campaign","r26_expert_experiment","r26_live_model","r26_candidate",
        "r27_policy","r27_lineage","r27_generation","r27_live_frozen","r27_live_shadow","r27_eprocess","r27_certificate","r27_holdout_exposure","r27_atomnet_state","r29_qual_receipt","r29_model_checkpoint","r29_legacy_model_checkpoint",
        "r30_confirm_read","r30_frozen_confirm","r30_model_artifact","r30_model_surface","r30_model_call","r30_decision_explain","r30_decision_mismatch","r30_certificate_revocation_event","r30_revoked_policy","r30_revoked_certificate",
        "su2_certificate","su2_mainline_choice","su2_mainline_binding","su2_live_state","su2_matrix_snapshot","su2_diag_snapshot","su2_live_decision","su2_model_genome","su2_lineage",
        "su1_model_ablation","su1_campaign_audit","su1_live_prediction","su1_live_result","su1_sealed_plan","su1_sealed_result","su1_certificate","su1_alpha_ledger"));
    public static final class Candidate {
        public final File file;public final String relative,kind;public final long bytes,modified;
        Candidate(File file,String relative,String kind){this.file=file;this.relative=relative;this.kind=kind;this.bytes=file.length();this.modified=file.lastModified();}
    }
    public static final class Census {
        public long bytes;public int entries,unreadable,excludedInstallLinks;public boolean complete=true;
        public final List<Candidate> candidates=new ArrayList<>();
        public Map<String,Object> json(){Map<String,Object> m=new LinkedHashMap<>();m.put("measuredRegularFileBytes",bytes);m.put("complete",complete);m.put("entries",entries);m.put("unreadableOrUnresolved",unreadable);m.put("excludedInstallationLinks",excludedInstallLinks);m.put("scope","Regular files inside app data root; installation lib symlink excluded; no symlink followed");return m;}
    }
    static int rank(Candidate c){return c.kind.equals("VERIFIED_RETIRED_DUPLICATE")?0:1;}
    static final Comparator<Candidate> ORDER=Comparator.comparingInt(ResearchRetentionR30::rank).thenComparing((Candidate c)->-c.bytes).thenComparing(c->c.relative);
    public static String kind(String relative){
        if(relative.equals(RETIRED)||relative.equals(RETIRED+"-wal")||relative.equals(RETIRED+"-shm")||relative.equals(RETIRED+"-journal"))return "VERIFIED_RETIRED_DUPLICATE";
        if(relative.startsWith("code_cache/")||relative.startsWith("cache/r30-recomputable/"))return "RECOMPUTABLE_CACHE";
        return "PROTECTED_OR_UNCLASSIFIED";
    }
    public static Census measure(File dataRoot)throws IOException{
        File root=dataRoot.getCanonicalFile();Census out=new Census();Deque<Object[]> stack=new ArrayDeque<>();stack.push(new Object[]{root,"",0});
        PriorityQueue<Candidate> bounded=new PriorityQueue<>(MAX_FILES_PER_STEP,ORDER.reversed());
        while(!stack.isEmpty()){
            if(Thread.currentThread().isInterrupted()){out.complete=false;out.unreadable++;break;}
            Object[] entry=stack.pop();File folder=(File)entry[0];String prefix=(String)entry[1];int depth=(Integer)entry[2];
            File[] children=folder.listFiles();if(children==null){out.complete=false;out.unreadable++;continue;}
            for(File child:children){
                if(++out.entries>MAX_SCAN_ENTRIES){out.complete=false;stack.clear();break;}
                String relative=prefix.isEmpty()?child.getName():prefix+"/"+child.getName();
                if(!child.getAbsoluteFile().equals(child.getCanonicalFile())){if(relative.equals("lib"))out.excludedInstallLinks++;else{out.complete=false;out.unreadable++;}continue;}
                if(child.isDirectory()){if(depth>=MAX_DEPTH){out.complete=false;out.unreadable++;}else stack.push(new Object[]{child,relative,depth+1});continue;}
                if(!child.isFile()){out.complete=false;out.unreadable++;continue;}
                out.bytes=Math.addExact(out.bytes,child.length());String type=kind(relative);
                if(!type.equals("PROTECTED_OR_UNCLASSIFIED")){bounded.add(new Candidate(child,relative,type));if(bounded.size()>MAX_FILES_PER_STEP)bounded.poll();}
            }
        }
        out.candidates.addAll(bounded);out.candidates.sort(ORDER);return out;
    }
    public interface Audit {
        /** For a retired duplicate, verify original foldback state and complete copy receipts. Null means preserve. */
        String retiredCopyReference(Candidate candidate)throws Exception;
        /** Must commit the durable reference before the file is unlinked. No database write lock spans file IO. */
        void beforeDelete(Candidate candidate,String reference)throws Exception;
        void afterDelete(Candidate candidate,String reference,boolean deleted)throws Exception;
    }
    public static final class Result {
        public final Census before;public Census after;public final long budget;public long deletedBytes;public int filesDeleted,protectedRetired;public String state;
        Result(Census before,long budget){this.before=before;this.after=before;this.budget=budget;}
        public Map<String,Object> json(){Map<String,Object> m=new LinkedHashMap<>();m.put("budgetBytes",budget);m.put("before",before.json());m.put("after",after.json());m.put("deletedBytes",deletedBytes);m.put("filesDeleted",filesDeleted);m.put("retiredWithoutCopyProofPreserved",protectedRetired);m.put("status",state);m.put("targetMet",after.complete&&budget>0?after.bytes<=budget:null);m.put("policy","Only verified retired duplicates and explicitly recomputable cache files; all other files protected");return m;}
    }
    public static Result clean(File dataRoot,long budget,Audit audit)throws Exception{
        Census before=measure(dataRoot);Result result=new Result(before,budget);
        if(budget<=0){result.state="UNKNOWN_INVALID_BUDGET";return result;}
        if(!before.complete){result.state="UNKNOWN_INCOMPLETE_CENSUS";return result;}
        if(before.bytes<=budget){result.state="WITHIN_BUDGET";return result;}
        long remaining=before.bytes;File root=dataRoot.getCanonicalFile();
        for(Candidate candidate:before.candidates){
            if(remaining<=budget)break;
            File file=candidate.file;if(!file.getCanonicalFile().toPath().startsWith(root.toPath())||!file.getAbsoluteFile().equals(file.getCanonicalFile())||!file.isFile()||file.length()!=candidate.bytes||file.lastModified()!=candidate.modified)continue;
            String reference=candidate.kind.equals("VERIFIED_RETIRED_DUPLICATE")?audit.retiredCopyReference(candidate):"EXPLICIT_RECOMPUTABLE_CACHE";
            if(reference==null){result.protectedRetired++;continue;}
            audit.beforeDelete(candidate,reference);
            boolean deleted=false;if(file.isFile()&&file.length()==candidate.bytes&&file.lastModified()==candidate.modified)deleted=file.delete();
            audit.afterDelete(candidate,reference,deleted);
            if(deleted){result.filesDeleted++;result.deletedBytes+=candidate.bytes;remaining-=candidate.bytes;}
        }
        result.after=measure(root);boolean eligibleRemaining=false;
        for(Candidate candidate:result.after.candidates)if(candidate.kind.equals("RECOMPUTABLE_CACHE")||audit.retiredCopyReference(candidate)!=null){eligibleRemaining=true;break;}
        result.state=!result.after.complete?"UNKNOWN_INCOMPLETE_CENSUS":result.after.bytes<=budget?"WITHIN_BUDGET":result.filesDeleted>0&&eligibleRemaining?"MORE_RECOMPUTABLE_WORK":"BLOCKED_PROTECTED_DATA_BUDGET";
        return result;
    }
}
