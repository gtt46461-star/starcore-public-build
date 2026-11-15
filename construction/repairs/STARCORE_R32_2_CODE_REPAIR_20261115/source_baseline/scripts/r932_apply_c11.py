#!/usr/bin/env python3
from pathlib import Path


def replace_once(path, old, new, label):
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    if new in text and old not in text:
        print(label + "=ALREADY_APPLIED")
        return
    count = text.count(old)
    if count != 1:
        raise SystemExit("%s expected one old block, found %d" % (label, count))
    p.write_text(text.replace(old, new, 1), encoding="utf-8")
    print(label + "=APPLIED")


vm = "app/src/main/java/com/ethk3/novaonepro/GenesisAssetVmR915.java"
asset = "app/src/main/java/com/ethk3/novaonepro/AssetGenesisEngineR915.java"

replace_once(
    vm,
    '''        if(context.observation!=null)for(int index:genome.atomIndices)\n            if(!context.observation.present[index])throw new IllegalStateException("R932_ATOM_INPUT_MISSING:"+GenesisSchemaRegistryR915.atoms().get(index).id+":"+context.observation.missingReasons[index]);\n''',
    '''        GenesisAtomDependencyR932.require(genome, context, materializeShape);\n''',
    "C11_VM_COMPLETE_DEPENDENCY_REQUIRE")

replace_once(
    vm,
    '''        double[] shaped = materializeShape && genome.outputType.shaped\n                ? shaped(genome.outputType, primary, context) : new double[0];\n''',
    '''        double[] shaped = materializeShape && genome.outputType.shaped\n                ? shaped(genome, primary, context) : new double[0];\n''',
    "C11_VM_SHAPE_CALL")

replace_once(
    vm,
    '''            case RESIDUAL_FIELD: return clamp01(0.5d + (score - context.atoms[174]) * 0.5d);\n            case RELATION_FIELD: return clamp01(0.6d * score + 0.4d * context.atoms[75]);\n            case TRANSITION_FIELD: return clamp01(0.6d * score + 0.4d * context.atoms[61]);\n            case MOTIF_FIELD: return clamp01(0.6d * score + 0.4d * context.atoms[33]);\n            case REGIME_FIELD: return clamp01(0.65d * score + 0.35d * context.atoms[132]);\n            case CALIBRATION_RESIDUAL: return clamp01(0.5d + 0.5d * (score - context.atoms[174]));\n''',
    '''            case RESIDUAL_FIELD: return clamp01(0.5d + (score - context.atoms[GenesisAtomDependencyR932.residualSemantic()]) * 0.5d);\n            case RELATION_FIELD: return clamp01(0.6d * score + 0.4d * context.atoms[GenesisAtomDependencyR932.relationSemantic()]);\n            case TRANSITION_FIELD: return clamp01(0.6d * score + 0.4d * context.atoms[GenesisAtomDependencyR932.transitionSemantic()]);\n            case MOTIF_FIELD: return clamp01(0.6d * score + 0.4d * context.atoms[GenesisAtomDependencyR932.motifSemantic()]);\n            case REGIME_FIELD: return clamp01(0.65d * score + 0.35d * context.atoms[GenesisAtomDependencyR932.regimeSemantic()]);\n            case CALIBRATION_RESIDUAL: return clamp01(0.5d + 0.5d * (score - context.atoms[GenesisAtomDependencyR932.residualSemantic()]));\n''',
    "C11_VM_SEMANTIC_FIELDS_USE_PRODUCTION_ATOMS")

p = Path(vm)
text = p.read_text(encoding="utf-8")
old_start = "    private static double[] shaped(\n            GenesisAssetGenomeR915.OutputType type,"
new_start = "    private static double[] shaped(\n            GenesisAssetGenomeR915 genome,"
if old_start in text:
    start = text.index("    private static double[] shaped(\n")
    end = text.index("    private static boolean number", start)
    new_method = '''    private static double[] shaped(\n            GenesisAssetGenomeR915 genome,\n            double primary,\n            PredrawContext context) {\n        GenesisAssetGenomeR915.OutputType type = genome.outputType;\n        double[] out = new double[Math.max(0, type.flatSize)];\n        for (int i = 0; i < out.length; i++) {\n            int number = 1 + i % 10;\n            double distance = Math.abs(number - context.n1) + Math.abs(number - context.n2)\n                    + Math.abs(number - context.n3);\n            double semantic;\n            switch (type) {\n                case MATRIX_FIELD: {\n                    double atom = context.atoms[GenesisAtomDependencyR932.shapeSignal(genome, i)];\n                    double topology = context.atoms[GenesisAtomDependencyR932.shapeTopology(i)];\n                    semantic = 0.45d * atom + 0.35d * topology + 0.20d * primary;\n                    break;\n                }\n                case GRAPH_FIELD: {\n                    double atom = context.atoms[GenesisAtomDependencyR932.shapeSignal(genome, i)];\n                    double topology = context.atoms[GenesisAtomDependencyR932.shapeTopology(i)];\n                    double time = context.atoms[GenesisAtomDependencyR932.shapeTime(i)];\n                    semantic = 0.40d * atom + 0.40d * Math.abs(topology - time)\n                            + 0.20d * primary;\n                    break;\n                }\n                case TENSOR_FIELD: {\n                    double atom = context.atoms[GenesisAtomDependencyR932.shapeSignal(genome, i)];\n                    double topology = context.atoms[GenesisAtomDependencyR932.shapeTopology(i)];\n                    double time = context.atoms[GenesisAtomDependencyR932.shapeTime(i)];\n                    semantic = 0.30d * atom + 0.30d * topology + 0.25d * time\n                            + 0.15d * primary;\n                    break;\n                }\n                case SEQUENCE_FIELD: {\n                    double atom = context.atoms[GenesisAtomDependencyR932.shapeSignal(genome, i)];\n                    double time = context.atoms[GenesisAtomDependencyR932.shapeTime(i)];\n                    semantic = 0.45d * time + 0.30d * atom + 0.25d * primary;\n                    break;\n                }\n                case STATE_MACHINE_FIELD:\n                    semantic = 0.50d * context.atoms[GenesisAtomDependencyR932.shapeMemory(i)]\n                            + 0.50d * primary;\n                    break;\n                case PROTOTYPE_LIBRARY: {\n                    double atom = context.atoms[GenesisAtomDependencyR932.shapeSignal(genome, i)];\n                    double time = context.atoms[GenesisAtomDependencyR932.shapeTime(i)];\n                    semantic = 0.35d * atom + 0.35d * time + 0.30d * primary;\n                    break;\n                }\n                default:\n                    semantic = primary * (0.70d + 0.30d * (1.0d - distance / 27.0d));\n                    break;\n            }\n            out[i] = clamp01(semantic);\n        }\n        return out;\n    }\n\n'''
    p.write_text(text[:start] + new_method + text[end:], encoding="utf-8")
    print("C11_VM_SHAPE_SAFE_PRODUCTION_ATOMS=APPLIED")
elif new_start in text:
    print("C11_VM_SHAPE_SAFE_PRODUCTION_ATOMS=ALREADY_APPLIED")
else:
    raise SystemExit("C11 shaped method boundary not found")

old = '''        final ArrayList<Freeze> freezes=new ArrayList<Freeze>();\n        java.util.Set<Integer> quarantined=AtomCaptureR932.quarantined(store);\n        for (AssetRow row:assets) {\n            String missing="";\n            for(int atom:row.genome.atomIndices) {\n                if(quarantined.contains(atom)){missing="ATOM_QUARANTINED:"+atom;break;}\n                if(contexts[0].observation!=null&&!contexts[0].observation.present[atom]){missing="ATOM_INPUT_MISSING:"+atom+":"+contexts[0].observation.missingReasons[atom];break;}\n            }\n            if(missing.length()>0){ResearchCaptureLedgerR932.record(store,target,"genesis:"+row.genome.assetId,"SYSTEM_INPUT_UNAVAILABLE",missing,0);continue;}\n            ResearchWorkBudgetR927.checkpoint();\n            if (System.currentTimeMillis()>=deadline || RealtimePressureGateR811.hasActiveCriticalWork()) break;\n            boolean proxy=GlobalResearchScheduleR932.proxy(store,target,"genesis:"+row.genome.assetId);\n'''
new = '''        final ArrayList<Freeze> freezes=new ArrayList<Freeze>();\n        java.util.Set<Integer> quarantined=AtomCaptureR932.quarantined(store);\n        for (AssetRow row:assets) {\n            boolean proxy=GlobalResearchScheduleR932.proxy(store,target,"genesis:"+row.genome.assetId);\n            String missing=GenesisAtomDependencyR932.unavailable(row.genome,contexts[0],!proxy,quarantined);\n            if(missing.length()>0){ResearchCaptureLedgerR932.record(store,target,"genesis:"+row.genome.assetId,"SYSTEM_INPUT_UNAVAILABLE",missing,0);continue;}\n            ResearchWorkBudgetR927.checkpoint();\n            if (System.currentTimeMillis()>=deadline || RealtimePressureGateR811.hasActiveCriticalWork()) break;\n'''
replace_once(asset, old, new, "C11_CAPTURE_COMPLETE_QUARANTINE_CLOSURE")

# Final structural assertions: no old narrow check, no unsafe motif identity, no modulo-240 reads.
vm_text = Path(vm).read_text(encoding="utf-8")
asset_text = Path(asset).read_text(encoding="utf-8")
checks = {
    "VM_USES_COMPLETE_DEPENDENCY_REQUIRE": "GenesisAtomDependencyR932.require(genome, context, materializeShape);" in vm_text,
    "VM_SEMANTIC_FIELDS_USE_PRODUCTION_ATOMS": "GenesisAtomDependencyR932.motifSemantic()" in vm_text and "context.atoms[33]" not in vm_text,
    "SHAPE_NO_RAW_MODULO_240": "(i * 31 + 75) % context.atoms.length" not in vm_text and "(i * 11 + 43) % context.atoms.length" not in vm_text,
    "CAPTURE_USES_COMPLETE_QUARANTINE_CLOSURE": "GenesisAtomDependencyR932.unavailable(row.genome,contexts[0],!proxy,quarantined)" in asset_text,
    "OLD_ATOMINDICES_ONLY_GATE_REMOVED": "for(int atom:row.genome.atomIndices)" not in asset_text,
}
for name, ok in checks.items():
    print(name + "=" + ("PASS" if ok else "FAIL"))
    if not ok:
        raise SystemExit(name)
