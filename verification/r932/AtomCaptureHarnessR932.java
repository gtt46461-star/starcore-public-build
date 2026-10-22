package com.ethk3.novaonepro;
import java.util.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;

public final class AtomCaptureHarnessR932 {
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);System.out.println(label+"=PASS");}
    static String scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    static GenesisAssetVmR915.PredrawContext auditContext(AtomCaptureR932.Values original,String base,String target,boolean corrupt) {
        AtomCaptureR932.Values v=new AtomCaptureR932.Values(240,base);
        System.arraycopy(original.values,0,v.values,0,240);System.arraycopy(original.present,0,v.present,0,240);System.arraycopy(original.costNs,0,v.costNs,0,240);System.arraycopy(original.missingReasons,0,v.missingReasons,0,240);
        if(corrupt)v.values[0]+=.001;
        String hash=V51ChecksumEngine.sha256("FROZEN_FIXTURE|"+base);
        return new GenesisAssetVmR915.PredrawContext(String.valueOf(Long.parseLong(target)-1),target,hash,hash,128,3,1,4,7,11,v.values,v);
    }
    static boolean audit(SQLiteDatabase db,GenesisAssetVmR915.PredrawContext a,GenesisAssetVmR915.PredrawContext b) {
        db.beginTransaction();try {boolean pass=AtomAdmissionAuditR932.record(db,a,b);db.setTransactionSuccessful();return pass;}finally{db.endTransaction();}
    }
    static void verifyProvenancePayload(String payload)throws Exception{
        JSONObject root=new JSONObject(payload);check("R932_PER_ATOM_CAPTURE_V2".equals(root.getString("revision"))&&AtomProvenanceR932.REVISION.equals(root.getString("provenance_revision")),"ATOM_PROVENANCE_REVISION_FROZEN");
        JSONArray atoms=root.getJSONArray("atoms");check(atoms.length()==240,"ATOM_PROVENANCE_COVERS_ALL_240");
        int predraw=0,postdraw=0;Map<String,Integer> sources=new TreeMap<String,Integer>();
        for(int i=0;i<240;i++){
            JSONObject atom=atoms.getJSONObject(i);JSONArray consumers=atom.getJSONArray("consumers");GenesisSchemaRegistryR915.AtomSpec spec=GenesisSchemaRegistryR915.atoms().get(i);
            if(!spec.id.equals(atom.getString("id"))||atom.getLong("cost_ns")<0||atom.getString("missing_reason")==null||consumers.length()==0)throw new AssertionError("ATOM_PROVENANCE_REQUIRED_FIELDS_"+i);
            if(!AtomProvenanceR932.sourceOwner(i).equals(atom.getString("source_owner"))||!AtomProvenanceR932.sourceFunction(i).equals(atom.getString("source_function"))||!AtomProvenanceR932.sourceContractHash(i).equals(atom.getString("source_contract_hash"))||!AtomProvenanceR932.maskContract(i).equals(atom.getString("mask_contract"))||!AtomProvenanceR932.consumersHash(i).equals(atom.getString("consumers_hash")))throw new AssertionError("ATOM_PROVENANCE_CONTRACT_"+i);
            List<String> expected=AtomProvenanceR932.consumers(i);if(consumers.length()!=expected.size())throw new AssertionError("ATOM_PROVENANCE_CONSUMER_COUNT_"+i);for(int j=0;j<expected.size();j++)if(!expected.get(j).equals(consumers.getString(j)))throw new AssertionError("ATOM_PROVENANCE_CONSUMER_"+i+"_"+j);
            String fn=atom.getString("source_function");sources.put(fn,Integer.valueOf((sources.containsKey(fn)?sources.get(fn).intValue():0)+1));
            if(spec.settlementEvidence()){postdraw++;if(AtomProvenanceR932.hasPredrawScoreConsumer(i))throw new AssertionError("POSTDRAW_PREDRAW_CONSUMER_"+i);}else if(spec.predrawComputable()){predraw++;if(!AtomProvenanceR932.hasPredrawScoreConsumer(i))throw new AssertionError("PREDRAW_DIRECT_CONSUMER_MISSING_"+i);}
        }
        check(predraw>0&&postdraw==20,"ATOM_PREDRAW_AND_POSTDRAW_CONSUMER_BOUNDARY_EXPLICIT");
        check(sources.size()==12,"ATOM_ALL_A0_A11_SOURCE_FUNCTIONS_EXPLICIT");
        for(Integer n:sources.values())if(n.intValue()!=20)throw new AssertionError("ATOM_SOURCE_GROUP_NOT_20");
        check(true,"EVERY_ATOM_HAS_SOURCE_MASK_COST_AND_CONSUMER_LIST");
    }
    public static void main(String[] args)throws Exception {
        List<Models.Draw> history=new ArrayList<Models.Draw>();String issue="202608310001";
        for(int i=0;i<128;i++){Models.Draw draw=new Models.Draw();draw.issue=issue;draw.openTime=1788100000000L+i*60000L;draw.fetchTime=draw.openTime+500;for(int p=0;p<10;p++)draw.numbers[p]=1+(p+i*3)%10;history.add(draw);issue=TargetIssueResolver.next(issue).targetIssue;}
        HistoricalStrategyInventionR908.Profile profile=HistoricalStrategyInventionR908.Profile.from(history);
        V51Models.PositionAction action=new V51Models.PositionAction(3,1,4,7,8,.31,.72,.052,.021,.61,.82,new double[96],"FIXTURE",12,"FIXTURE");String h=V51ChecksumEngine.sha256("SYNTHETIC_HISTORY");
        AtomCaptureR932.Values values=GenesisAtomicWorldR920.materializeObserved(profile,action,128,h,640);
        double[] legacy=GenesisAtomicWorldR920.materialize(profile,action,128,h,640);
        check(Arrays.equals(values.values,legacy),"ATOM_INSTRUMENTATION_PRESERVES_VALUES");
        for(int i=180;i<200;i++)if(values.present[i]||values.costNs[i]!=0||values.values[i]!=0)throw new AssertionError("POSTDRAW_ATOM_EXECUTED");
        check(true,"POSTDRAW_TWENTY_ATOMS_ZERO_PRODUCTION_CALLS");
        for(int i=0;i<240;i++)if(values.costNs[i]<0||values.missingReasons[i]==null)throw new AssertionError("ATOM_TELEMETRY_MISSING");
        check(true,"EVERY_ATOM_HAS_ACTUAL_COST_AND_AVAILABILITY_REASON");
        String base=history.get(127).issue;GenesisAssetVmR915.PredrawContext context=new GenesisAssetVmR915.PredrawContext(base,issue,h,h,128,3,1,4,7,11,values.values,values);
        String payload=values.payload(issue,context.digest());verifyProvenancePayload(payload);
        SQLiteDatabase db=new SQLiteDatabase(args[0]);try {
            DbStore store=new DbStore(db);db.execSQL("CREATE TABLE IF NOT EXISTS "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY,dirty INTEGER)");AtomCaptureR932.schema(db);
            db.beginTransaction();try{AtomCaptureR932.freeze(db,context);AtomCaptureR932.freeze(db,context);db.setTransactionSuccessful();}finally{db.endTransaction();}
            check(scalar(db,"SELECT COUNT(*) FROM "+AtomCaptureR932.TABLE).equals("1"),"ATOM_ONE_ISSUE_ONE_CONTEXT_SUPPORT");
            GenesisAssetVmR915.PredrawContext historical=new GenesisAssetVmR915.PredrawContext(base,issue,h,h,128,3,1,4,7,11,values.values);
            db.beginTransaction();try{AtomCaptureR932.freeze(db,historical);db.setTransactionSuccessful();}finally{db.endTransaction();}
            check(scalar(db,"SELECT COUNT(*) FROM "+AtomCaptureR932.TABLE).equals("1"),"LEGACY_CONTEXT_WITHOUT_OBSERVATION_REMAINS_UNKNOWN_NOT_FABRICATED");
            String original=scalar(db,"SELECT payload FROM "+AtomCaptureR932.TABLE);JSONObject tampered=new JSONObject(original);tampered.getJSONArray("atoms").getJSONObject(0).put("source_owner","FAKE_OWNER");String corrupt=tampered.toString();
            db.execSQL("UPDATE "+AtomCaptureR932.TABLE+" SET payload=?,payload_hash=?",new Object[]{corrupt,V51ChecksumEngine.sha256(corrupt)});
            boolean provenanceRejected=false;try{AtomCaptureR932.maintain(store);}catch(IllegalStateException expected){provenanceRejected=true;}
            check(provenanceRejected&&store.getState("r932_atom_quality_cursor","").equals("")&&scalar(db,"SELECT COUNT(*) FROM "+AtomCaptureR932.QUALITY).equals("0"),"TAMPERED_ATOM_SOURCE_PROVENANCE_HARD_FAILS_ATOMICALLY");
            db.execSQL("UPDATE "+AtomCaptureR932.TABLE+" SET payload=?,payload_hash=?",new Object[]{original,V51ChecksumEngine.sha256(original)});
            db.execSQL("CREATE TRIGGER fixture_atom_abort BEFORE INSERT ON "+AtomCaptureR932.QUALITY+" BEGIN SELECT RAISE(ABORT,'INJECTED'); END");
            boolean failed=false;try{AtomCaptureR932.maintain(store);}catch(IllegalStateException expected){failed=true;}
            check(failed&&store.getState("r932_atom_quality_cursor","").equals("")&&scalar(db,"SELECT COUNT(*) FROM "+AtomCaptureR932.QUALITY).equals("0"),"ATOM_QUALITY_AND_CURSOR_ATOMIC_ON_FAILURE");
            db.execSQL("DROP TRIGGER fixture_atom_abort");AtomCaptureR932.maintain(store);AtomCaptureR932.maintain(store);
            check(scalar(db,"SELECT COUNT(*) FROM "+AtomCaptureR932.QUALITY+" WHERE n=1").equals("240"),"ATOM_INCREMENTAL_RETRY_DOES_NOT_DOUBLE_SUPPORT");
            check(scalar(db,"SELECT COUNT(*) FROM "+AtomCaptureR932.QUALITY+" WHERE available_n=0").equals("60"),"ATOM_MISSING_PROXY_AND_POSTDRAW_NOT_COUNTED_PRESENT");
            String atom=GenesisSchemaRegistryR915.atoms().get(0).id;
            String graphBefore=AtomCaptureR932.graphHash(store);
            check(!audit(db,context,auditContext(values,base,issue,true)),"ATOM_NONDETERMINISM_HARD_FAIL");
            check(AtomCaptureR932.quarantined(store).contains(0),"ATOM_HARD_FAIL_PERSISTS_QUARANTINE");
            String graphBlocked=AtomCaptureR932.graphHash(store);
            check(!graphBefore.equals(graphBlocked),"ATOM_DEPENDENCY_GRAPH_REVOKES_ON_FAILURE");
            String next=TargetIssueResolver.next(issue).targetIssue;
            for(int i=0;i<20;i++){GenesisAssetVmR915.PredrawContext a=auditContext(values,base,next,false);audit(db,a,a);next=TargetIssueResolver.next(next).targetIssue;}
            check(AtomCaptureR932.quarantined(store).contains(0)&&scalar(db,"SELECT pass_streak FROM "+AtomAdmissionAuditR932.RECOVERY+" WHERE atom_id='"+atom+"'").equals("0"),"ATOM_STALE_RAW_HISTORY_CANNOT_RECOVER");
            for(int i=0;i<20;i++){String freshBase=next;next=TargetIssueResolver.next(next).targetIssue;GenesisAssetVmR915.PredrawContext a=auditContext(values,freshBase,next,false);audit(db,a,a);audit(db,a,a);}
            check(!AtomCaptureR932.quarantined(store).contains(0)&&scalar(db,"SELECT n FROM "+AtomCaptureR932.QUALITY+" WHERE atom_id='"+atom+"'").equals("0"),"ATOM_TWENTY_FRESH_AUDITS_RESTORE_WATCH_WITH_ZERO_SUPPORT");
            check(scalar(db,"SELECT COUNT(*) FROM "+AtomAdmissionAuditR932.ARCHIVE).equals("1")&&!graphBefore.equals(AtomCaptureR932.graphHash(store)),"ATOM_OLD_QUALITY_PRESERVED_AND_OLD_GRAPH_NEVER_RESURRECTED");
            System.out.println("R932_ATOM_CAPTURE_PASS|checks=PROVENANCE_240_PLUS_EXISTING|scope=PRODUCTION_MATERIALIZER_REAL_SQLITE_SYNTHETIC_HISTORY_NOT_DEVICE");
        }finally{db.shutdown();}
    }
}
