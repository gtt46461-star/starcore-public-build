#!/usr/bin/env python3
from pathlib import Path
import re

# 1) Normalize the three audit-manifest newline appends after Fullblood has
# regenerated the file. This must be exact: any other count aborts.
P = Path('app/src/main/java/com/ethk3/novaonepro/B07R2BuildAuditManifest.java')
if not P.is_file():
    raise SystemExit('POST_FULLBLOOD_MANIFEST_MISSING')
s = P.read_text(encoding='utf-8')
malformed_re = re.compile(r"append\('\s+'\)")
malformed = malformed_re.findall(s)
valid_literal = "append('\\n')"
valid = s.count(valid_literal)
if len(malformed) == 3 and valid == 0:
    s, changed = malformed_re.subn('append((char)10)', s)
elif len(malformed) == 0 and valid == 3:
    s = s.replace(valid_literal, 'append((char)10)')
    changed = 3
else:
    raise SystemExit('POST_FULLBLOOD_NEWLINE_COUNT malformed=%d valid=%d expected one form=3' % (len(malformed), valid))
if changed != 3:
    raise SystemExit('POST_FULLBLOOD_CHANGED=%d_EXPECTED=3' % changed)
if malformed_re.search(s) or valid_literal in s:
    raise SystemExit('POST_FULLBLOOD_NORMALIZE_INCOMPLETE')
if s.count('append((char)10)') != 3:
    raise SystemExit('POST_FULLBLOOD_CHAR10_COUNT=%d_EXPECTED=3' % s.count('append((char)10)'))
P.write_text(s, encoding='utf-8')
print('POST_FULLBLOOD_NORMALIZE_OK changed=3 char10=3')

# 2) Exact compile repair for the single R3 LanePolicy diagnostic annotation.
# LanePolicyCompiler has safe(String) but no append(String,String) helper. The
# annotation must never control the gate; it only augments reasonSummary.
L = Path('app/src/main/java/com/ethk3/novaonepro/LanePolicyCompiler.java')
if not L.is_file():
    raise SystemExit('POST_FULLBLOOD_LANE_MISSING')
ls = L.read_text(encoding='utf-8')
old = '            c.reasonSummary = append(c.reasonSummary, "全局Champion尚未证明：不伪造MODEL_PROVEN；V51仅允许候选级严格OOS合同继续接受后续全部质量/风险门审判");'
new = '            c.reasonSummary = safe(c.reasonSummary) + (safe(c.reasonSummary).length() > 0 ? "；" : "") + "全局Champion尚未证明：不伪造MODEL_PROVEN；V51仅允许候选级严格OOS合同继续接受后续全部质量/风险门审判";'
count = ls.count(old)
if count != 1:
    raise SystemExit('POST_FULLBLOOD_LANE_APPEND_COUNT=%d_EXPECTED=1' % count)
ls = ls.replace(old, new, 1)
if old in ls or ls.count(new) != 1:
    raise SystemExit('POST_FULLBLOOD_LANE_APPEND_REPAIR_INCOMPLETE')
L.write_text(ls, encoding='utf-8')
print('POST_FULLBLOOD_LANE_APPEND_REPAIR_OK count=1')
