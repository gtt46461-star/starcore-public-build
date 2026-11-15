#!/usr/bin/env python3
from pathlib import Path

# E04A: wire reversible parent trace into the real Genesis registration boundary.
path=Path('app/src/main/java/com/ethk3/novaonepro/AssetGenesisEngineR915.java')
text=path.read_text(encoding='utf-8')
old='''    private static boolean insertGenome(SQLiteDatabase db,GenesisAssetGenomeR915 g,String trained,\n            String earliest,long now){\n        ContentValues v=new ContentValues();v.put("asset_id",g.assetId);v.put("namespace",g.namespace());\n'''
new='''    private static boolean insertGenome(SQLiteDatabase db,GenesisAssetGenomeR915 g,String trained,\n            String earliest,long now){\n        // E04A production boundary: every new molecule must reversibly resolve every explicit,\n        // fixed-semantic and materialized-shape parent to the immutable L0 registry before it can\n        // enter the persistent candidate population.\n        GenesisMoleculeTraceR932.Trace moleculeTrace=GenesisMoleculeTraceR932.trace(g,true);\n        if(!moleculeTrace.completeFor(g))\n            throw new IllegalStateException("R932_GENESIS_MOLECULE_PARENT_TRACE_INCOMPLETE");\n        ContentValues v=new ContentValues();v.put("asset_id",g.assetId);v.put("namespace",g.namespace());\n'''
if new in text and old not in text:
    print('E04A_GENESIS_REGISTRATION_TRACE=ALREADY_APPLIED')
elif text.count(old)==1:
    path.write_text(text.replace(old,new,1),encoding='utf-8')
    print('E04A_GENESIS_REGISTRATION_TRACE=APPLIED')
else:
    raise SystemExit('E04A_INSERT_GENOME_ANCHOR_COUNT='+str(text.count(old)))

check=path.read_text(encoding='utf-8')
if 'GenesisMoleculeTraceR932.trace(g,true)' not in check or 'R932_GENESIS_MOLECULE_PARENT_TRACE_INCOMPLETE' not in check:
    raise SystemExit('E04A_PRODUCTION_WIRING_MISSING')
print('E04A_PRODUCTION_WIRING=PASS')

# E03E: replace coarse risk-only mode naming with the additive, label-blind multiscale state
# decomposition. Numeric risk/stability formulas remain unchanged; this only makes the state truth
# explicit and keeps runtime/network outage a distinct namespace.
drift=Path('app/src/main/java/com/ethk3/novaonepro/DistributionDriftEngine.java')
dtext=drift.read_text(encoding='utf-8')
old_mode='''            String mode =\n                    risk >= 0.78d\n                            ? "HARD_DRIFT"\n                            : (risk >= 0.55d ? "WATCH" : (risk >= 0.30d ? "SOFT_DRIFT" : "STABLE"));\n'''
new_mode='''            DriftUncertaintyStateR932.Result driftState =\n                    DriftUncertaintyStateR932.classify(\n                            shortMedium, mediumLong, shortLong, true, windows.historyRows);\n            String mode = driftState.state.name();\n'''
if new_mode in dtext and old_mode not in dtext:
    print('E03E_DRIFT_STATE_PRODUCTION_WIRING=ALREADY_APPLIED')
elif dtext.count(old_mode)==1:
    drift.write_text(dtext.replace(old_mode,new_mode,1),encoding='utf-8')
    print('E03E_DRIFT_STATE_PRODUCTION_WIRING=APPLIED')
else:
    raise SystemExit('E03E_DRIFT_MODE_ANCHOR_COUNT='+str(dtext.count(old_mode)))

dcheck=drift.read_text(encoding='utf-8')
if 'DriftUncertaintyStateR932.classify(' not in dcheck:
    raise SystemExit('E03E_PRODUCTION_WIRING_MISSING')
print('E03E_PRODUCTION_WIRING=PASS')

# E04G: even if a certificate was once valid, an atom quarantine must make the certified
# descendant contribute exactly zero on the next issue. This guard is intentionally before the
# recipe executes and returns the original 1200-action surface unchanged via fallback().
mixer=Path('app/src/main/java/com/ethk3/novaonepro/CertifiedResearchMixerR915.java')
mtext=mixer.read_text(encoding='utf-8')
old_guard='''            GenesisAssetGenomeR915 identity=GenesisAssetGenomeR915.parse(candidate.genomeSpec);\n            if(!identity.assetGrade()||!identity.assetId.equals(candidate.assetId)||!identity.structuralHash.equals(candidate.structuralHash)||!identity.behaviorHash.equals(candidate.behaviorHash)||!identity.mechanismNameZh.equals(candidate.nameZh))return fallback(store,productionActions,"ASSET_IDENTITY_MISMATCH",candidate);\n            double weight=window.weight;\n'''
new_guard='''            GenesisAssetGenomeR915 identity=GenesisAssetGenomeR915.parse(candidate.genomeSpec);\n            if(!identity.assetGrade()||!identity.assetId.equals(candidate.assetId)||!identity.structuralHash.equals(candidate.structuralHash)||!identity.behaviorHash.equals(candidate.behaviorHash)||!identity.mechanismNameZh.equals(candidate.nameZh))return fallback(store,productionActions,"ASSET_IDENTITY_MISMATCH",candidate);\n            if(GenesisMoleculeTraceR932.blockedByQuarantine(identity,true,AtomCaptureR932.quarantined(store)))\n                return fallback(store,productionActions,"ASSET_PARENT_ATOM_QUARANTINED_ZERO_CONTRIBUTION",candidate);\n            double weight=window.weight;\n'''
if new_guard in mtext and old_guard not in mtext:
    print('E04G_CERTIFIED_DESCENDANT_QUARANTINE_GUARD=ALREADY_APPLIED')
elif mtext.count(old_guard)==1:
    mixer.write_text(mtext.replace(old_guard,new_guard,1),encoding='utf-8')
    print('E04G_CERTIFIED_DESCENDANT_QUARANTINE_GUARD=APPLIED')
else:
    raise SystemExit('E04G_MIXER_GUARD_ANCHOR_COUNT='+str(mtext.count(old_guard)))

mcheck=mixer.read_text(encoding='utf-8')
if 'ASSET_PARENT_ATOM_QUARANTINED_ZERO_CONTRIBUTION' not in mcheck or 'blockedByQuarantine(identity,true' not in mcheck:
    raise SystemExit('E04G_PRODUCTION_WIRING_MISSING')
print('E04G_PRODUCTION_WIRING=PASS')
