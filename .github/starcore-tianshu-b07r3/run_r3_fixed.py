#!/usr/bin/env python3
"""R3 deterministic driver with bounded compatibility repairs.

The frozen apply_r3.py payload is kept intact on disk. Only patch_diag is
replaced in memory so it matches the actual R2 diagnostic class. After source
generation, the audit manifest must contain exactly three newline append calls.
Both valid Java escaped-newline literals and the historical malformed physical
newline form are accepted and normalized to `(char)10`; any other count fails.
Python bytecode caches are removed so source-only scans remain deterministic.
"""
from pathlib import Path
import re
import shutil

SCRIPT = Path(__file__).with_name("apply_r3.py")
if not SCRIPT.is_file():
    raise SystemExit("R3_PATCHER_MISSING:" + str(SCRIPT))

source = SCRIPT.read_text(encoding="utf-8")
start = source.index("def patch_diag(s):")
end_marker = "patch(Path('app/src/main/java/com/ethk3/novaonepro/B07R2FullDiagnosticEngine.java'),patch_diag,'R3 kernel/background/real-action/root-cause/50-60 diagnostic extension')\n"
end = source.index(end_marker, start) + len(end_marker)

fixed = r"""def patch_diag(s):
    anchor='''        return clip(s.toString(),AppConfig.B07R2_REPORT_MAX_CHARS);\n    }'''
    extra='''        s.append("\\n【B07R3 1-7批次加强复核】\\n");\n        s.append("R3版本=").append(AppConfig.B07R3_VERSION).append("\\n");\n        s.append("后台能力=").append(AppConfig.B07R3_BACKGROUND_SCOPE).append("\\n");\n        s.append("退出/划掉任务：前台服务即时重挂 + 同服务foreground re-delivery + Alarm + Job；息屏靠PARTIAL_WAKE_LOCK。用户手动强行停止/厂商禁后台无法由普通应用绕过，必须在报告中如实显示。\\n");\n        s.append("真实出手修复：全局WalkForward CHAMPION只做证据诊断，不再一票锁死活动V51；候选仍必须通过PredictionContract、RuntimeFreshness、硬风险、EventP、ActionLCB、Coverage、Third、Counter、Uncertainty、Authorization。\\n");\n        s.append("频率红线：活动V51不允许频率债务制造机会；频率只能通过扩大已证明状态域、改善候选排序/校准/证据覆盖来增加。\\n");\n        s.append("Kernel最后状态=").append(state(store,StarcoreEvidenceKernel.STATE_LAST)).append("\\n");\n        s.append("Kernel回放状态=").append(state(store,StarcoreEvidenceKernel.STATE_REPLAY)).append("\\n");\n        s.append("Kernel插件健康：\\n").append(StarcoreEvidenceKernel.renderHealth(store));\n        String champion=state(store,WalkForwardChampionEngine.STATE_STATUS);\n        if(champion.length()==0) champion="MODEL_NOT_PROVEN";\n        s.append("全局模型证明=").append(champion).append("；注意：它仍决定能否声称模型已证明，但不再等价于活动V51每个候选永远禁止真实BET。\\n");\n        s.append("50%/60%目标分析：只有严格时间顺序OOS/真实影子样本的命中率、Wilson下界、ECE、四块稳定性和覆盖同时支持时才可称达标。若未达标，应优先做：层级校准(HierarchicalCalibration)、状态域漂移证书(RegimeDriftCertificate)、第三号条件排序(ThirdNumberConditionalRank)、反事实残差挖掘(RankerResidualMining)、专家相关性去重(ExpertCorrelationAudit)、消融账本(AblationLedger)、已证明域覆盖扩张(CertifiedCoverageExpansion)；禁止简单降低门槛凑50/60或凑频率。\\n");\n        s.append("Kernel职责边界：只聚合证据/健康/回放；不写FinalP、不选Lane、不创建BET，DecisionGate仍是唯一真实BET出口。\\n");\n        return clip(s.toString(),AppConfig.B07R3_REPORT_MAX_CHARS);\n    }'''
    return once(s,anchor,extra,'diag_tail_r3_exact')
patch(Path('app/src/main/java/com/ethk3/novaonepro/B07R2FullDiagnosticEngine.java'),patch_diag,'R3 kernel/background/real-action/root-cause/50-60 diagnostic extension')
"""

patched = source[:start] + fixed + source[end:]
exec(compile(patched, str(SCRIPT), "exec"), {"__name__": "__main__", "__file__": str(SCRIPT)})

manifest_path = Path("app/src/main/java/com/ethk3/novaonepro/B07R2BuildAuditManifest.java")
manifest_text = manifest_path.read_text(encoding="utf-8")
# Current generator form is exact Java source append('\\n'). Use literal
# matching instead of regex escaping: this Python value contains one backslash+n.
valid_literal = "append('\\n')"
valid_count = manifest_text.count(valid_literal)
malformed_count = 0
if valid_count == 3:
    manifest_text = manifest_text.replace(valid_literal, "append((char)10)")
elif valid_count == 0:
    # Compatibility only for an older generator/run that emitted physical
    # whitespace/newlines inside char literals. It too must occur exactly 3x.
    manifest_text, malformed_count = re.subn(r"append\('\s+'\)", "append((char)10)", manifest_text)
else:
    raise SystemExit("R3_MANIFEST_VALID_NEWLINE_COUNT=%d_EXPECTED_0_OR_3" % valid_count)
count = valid_count + malformed_count
if count != 3:
    compact = re.sub(r"\s+", " ", manifest_text[-1200:])
    print("R3_MANIFEST_TAIL_DIAG=" + compact[:1000])
    raise SystemExit("R3_MANIFEST_NEWLINE_PATTERN_COUNT=%d_EXPECTED=3" % count)
manifest_path.write_text(manifest_text, encoding="utf-8")
print("R3_MANIFEST_NEWLINE_NORMALIZE_OK valid=%d malformed=%d total=3" % (valid_count, malformed_count))

ROOT = Path(__file__).resolve().parent
removed = 0
for cache_dir in list(ROOT.rglob("__pycache__")):
    if cache_dir.is_dir():
        shutil.rmtree(str(cache_dir), ignore_errors=True)
        removed += 1
print("R3_PYCACHE_CLEANUP_OK removed=%d" % removed)
