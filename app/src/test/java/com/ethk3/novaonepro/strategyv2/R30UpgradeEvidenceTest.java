package com.ethk3.novaonepro.strategyv2;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public final class R30UpgradeEvidenceTest {
    @Test public void emptySnapshotHasExplicitReasonsInsteadOfEmptyStructureTables(){
        UpgradeDiagnosisSU2.Diagnosis d=UpgradeDiagnosisSU2.diagnose(new UpgradeDiagnosisSU2.Snapshot());UpgradeDiagnosisSU2.Ledger l=new UpgradeDiagnosisSU2.Ledger();
        String md=UpgradeDiagnosisSU2.renderR30(d,l);assertTrue(md.contains("INSUFFICIENT"));assertTrue(md.contains("UNKNOWN"));assertFalse(md.contains(UpgradeDiagnosisSU2.T.get("S3.h")+"\n"+UpgradeDiagnosisSU2.T.get("S3.th")));assertFalse(md.contains(UpgradeDiagnosisSU2.T.get("S1.fam.h")+"\n"+UpgradeDiagnosisSU2.T.get("S1.fam.th")));
        Map<?,?> report=(Map<?,?>)JsonR27.parse(UpgradeDiagnosisSU2.toJsonR30(d,l));for(Object item:(List<?>)report.get("r30Sections")){Map<?,?> row=(Map<?,?>)item;assertTrue(Arrays.asList("UNKNOWN","INSUFFICIENT").contains(row.get("status")));assertNotNull(row.get("reason"));assertNotNull(row.get("collectionAction"));assertNull(row.get("qualityEffective"));}
    }
    @Test public void missingZeroAndStoredAblationAreDifferentAndReferencesSurvive(){
        Map<String,Object> missing=UpgradeDiagnosisSU2.evidenceR30("su1_model_ablation",null,null,"empty","ModelAblationStoreSU1.advanceOne");assertEquals("UNKNOWN",missing.get("status"));assertNull(missing.get("records"));
        Map<String,Object> empty=UpgradeDiagnosisSU2.evidenceR30("su1_model_ablation",0L,Collections.emptyList(),"empty","ModelAblationStoreSU1.advanceOne");assertEquals("INSUFFICIENT",empty.get("status"));assertEquals(0L,empty.get("records"));
        List<?> rows=Collections.singletonList(Collections.singletonMap("report_hash","original-report-hash"));Map<String,Object> stored=UpgradeDiagnosisSU2.evidenceR30("su1_model_ablation",2L,rows,"empty","ModelAblationStoreSU1.advanceOne");assertEquals("RECORDED_UNVALIDATED",stored.get("status"));assertSame(rows,stored.get("rows"));assertEquals(1L,stored.get("omittedRows"));assertNull(stored.get("pairedImprovementLower95"));
        assertEquals("PARTIAL",UpgradeDiagnosisSU2.evidenceR30("source",1L,null,"empty","collect").get("status"));
    }
    @Test public void currentRenderingAddsSourceStatesWithoutChangingLegacyDiagnosisMath(){
        UpgradeDiagnosisSU2.Snapshot s=UpgradeDiagnosisSU2.parse("SU2DIAG\t1\ndataset\tfixture\nseq\t2500\ngen\tc1\tF15\t0.30\t300\t0.35\t300\t1\nop\tMUTATE\t0.25\n");UpgradeDiagnosisSU2.Diagnosis d=UpgradeDiagnosisSU2.diagnose(s);UpgradeDiagnosisSU2.Ledger l=new UpgradeDiagnosisSU2.Ledger();l.update(d.findings,s.seq);
        Map<?,?> legacy=(Map<?,?>)JsonR27.parse(UpgradeDiagnosisSU2.toJson(d,l));Map<String,Object> current=new LinkedHashMap<>((Map<String,Object>)JsonR27.parse(UpgradeDiagnosisSU2.toJsonR30(d,l)));assertNotNull(current.remove("r30Sections"));assertEquals(legacy,current);
        assertEquals("INSUFFICIENT",((Map<?,?>)JsonR27.parse(UpgradeDiagnosisSU2.unavailableR30("INSUFFICIENT","no snapshot")[1])).get("status"));assertEquals(0L,((Map<?,?>)JsonR27.parse(UpgradeDiagnosisSU2.unavailableR30("INSUFFICIENT","no snapshot")[1])).get("records"));assertNull(((Map<?,?>)JsonR27.parse(UpgradeDiagnosisSU2.unavailableR30("UNKNOWN","SQLiteException")[1])).get("records"));
    }
}
