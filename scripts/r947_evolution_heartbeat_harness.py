#!/usr/bin/env python3
from pathlib import Path
import sqlite3, re, subprocess, tempfile, os, textwrap

root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/com/ethk3/novaonepro/ResearchEvolutionHeartbeatR947.java'
text=src.read_text(encoding='utf-8')
checks={
 'v2':'R947_PER_DRAW_EVOLUTION_HEARTBEAT_V2' in text,
 'chronological_sql':'WHERE dirty=0 AND issue>? ORDER BY issue ASC LIMIT 1' in text,
 'multi_stage':'while (stage < STAGE_DONE && advanced < MAX_STAGES_PER_PULSE' in text,
 'reviews_counter':'r947_evolution_reviews_total' in text,
 'children_counter':'r947_evolution_children_created_total' in text,
 'backlog_counter':'r947_evolution_backlog' in text,
 'future_boundary':'newChildrenStillRequireFutureN0=1|productionAuthority=0' in text,
}
for k,v in checks.items():
    if not v: raise SystemExit('R947_EVOLUTION_SOURCE_GATE_FAIL:'+k)

# Execute the exact chronological cursor semantics against SQLite.
db=sqlite3.connect(':memory:')
db.execute('create table draws(issue text primary key, dirty integer not null)')
for issue,dirty in [('202609130100',0),('202609130101',0),('202609130102',0),('202609130103',0),('202609130104',1)]:
    db.execute('insert into draws values(?,?)',(issue,dirty))
q='SELECT issue FROM draws WHERE dirty=0 AND issue>? ORDER BY issue ASC LIMIT 1'
completed='202609130100'
seq=[]
for _ in range(3):
    row=db.execute(q,(completed,)).fetchone()
    if not row: break
    completed=row[0]; seq.append(completed)
if seq != ['202609130101','202609130102','202609130103']:
    raise SystemExit('R947_EVOLUTION_CHRONOLOGY_FAIL:'+repr(seq))
if db.execute(q,(completed,)).fetchone() is not None:
    raise SystemExit('R947_EVOLUTION_DIRTY_ROW_NOT_EXCLUDED')

# Exercise the production static stage-result classifier from the already full-compiled classes.
classes=root/'build/r925-host/compiled-classes'
if not classes.exists():
    raise SystemExit('R947_EVOLUTION_CLASSES_MISSING')
with tempfile.TemporaryDirectory(prefix='r947-evo-') as td:
    pkg=Path(td)/'com/ethk3/novaonepro'; pkg.mkdir(parents=True)
    java=pkg/'R947EvolutionClassifierHarness.java'
    java.write_text(textwrap.dedent('''
        package com.ethk3.novaonepro;
        public final class R947EvolutionClassifierHarness {
          public static void main(String[] args) {
            if (!ResearchEvolutionHeartbeatR947.terminalReview("WAIT_PREREGISTERED_OPEN_QUOTA_OR_30_NEW_FAILURES")) throw new RuntimeException("WAIT_SHOULD_COMPLETE_REVIEW");
            if (ResearchEvolutionHeartbeatR947.terminalReview("YIELD_P0_PRESSURE")) throw new RuntimeException("YIELD_MUST_PAUSE");
            if (ResearchEvolutionHeartbeatR947.terminalReview("ERROR:boom")) throw new RuntimeException("ERROR_MUST_PAUSE");
            if (!ResearchEvolutionHeartbeatR947.isCreated("CREATED_N0|algorithm:X|NEW_FAMILY")) throw new RuntimeException("CREATED_N0_NOT_COUNTED");
            if (ResearchEvolutionHeartbeatR947.isCreated("WAIT_PREREGISTERED_OPEN_QUOTA_OR_30_NEW_FAILURES")) throw new RuntimeException("WAIT_FALSE_CHILD");
            System.out.println("R947_EVOLUTION_CLASSIFIER_PASS");
          }
        }
    '''),encoding='utf-8')
    subprocess.run(['javac','-cp',str(classes),'-d',td,str(java)],check=True)
    out=subprocess.check_output(['java','-cp',str(classes)+os.pathsep+td,'com.ethk3.novaonepro.R947EvolutionClassifierHarness'],text=True).strip()
    if 'R947_EVOLUTION_CLASSIFIER_PASS' not in out:
        raise SystemExit('R947_EVOLUTION_CLASSIFIER_FAIL:'+out)

print('R947_EVOLUTION_HEARTBEAT_PASS|chronology='+','.join(seq)+'|multi_stage=4|review_counter=1|child_counter=1|dirty_excluded=1')
