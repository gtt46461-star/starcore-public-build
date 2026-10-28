#!/usr/bin/env python3
"""R27 HOST test: additive migration from a 1713d11-shaped database, plus the SQL half of H15.

Usage: test_r27_migration.py <sql_dump.json>
  sql_dump.json is printed by tools/r27/R27SqlDump.java from the compiled app sources, so the statements below are
  the exact production statements (SchemaR27.DDL / COLUMNS, MainlineBridgeSU2.R27_INTENT_SQL).

Checks
  M1  every baseline R26 table (DDL taken from git 1713d11) keeps every row and every original column value;
  M2  the guarded r26_task columns are added with their defaults; registered_at / first_progress_at are backfilled;
  M3  running the migration a second time changes nothing (schema and content hash identical);
  M4  no DROP / DELETE statement exists in the R27 migration.
  M6  (R27-031) the R27 loop's attempts feed the baseline r26_evolution ledger: the report's own EVOLUTION query counts
      them; replaying the same statements (killed process) does not double-count.
  H15-SQL  the R27 request query returns a row only for a FROZEN, unsettled row of a shadow with an unexpired ACTIVE
      certificate and a QUALIFIED e-process; no certificate / expired / revoked / not-qualified / settled -> nothing;
      a second frozen row for the same (policy, period) is rejected; a frozen decision cannot be rewritten.
Exit code 0 only when every check passes.
"""
import hashlib, json, re, sqlite3, subprocess, sys

BASE = "1713d114ebbd07ebd6544a29acfbce1bd883215c"
R26_SCHEMA = "app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeSchemaR26.java"
FAIL = []


def check(name, ok, detail=""):
    print(("R27_MIGRATION|%s|%s|%s" % (name, "OK" if ok else "FAILED", detail)).rstrip("|"))
    if not ok:
        FAIL.append(name)


def baseline_ddl():
    src = subprocess.run(["git", "show", BASE + ":" + R26_SCHEMA], capture_output=True, text=True, check=True).stdout
    body = src[src.index("DDL = {"):]
    body = body[:body.index("};")]
    return [s.encode().decode("unicode_escape") for s in re.findall(r'"((?:CREATE|INSERT)[^"\\]*(?:\\.[^"\\]*)*)"', body)]


def content_hash(db, tables_cols):
    h = hashlib.sha256()
    for t, cols in sorted(tables_cols.items()):
        h.update(t.encode())
        for row in db.execute("SELECT %s FROM %s ORDER BY rowid" % (",".join(cols), t)):
            h.update(repr(row).encode())
    return h.hexdigest()


def schema(db):
    return sorted(db.execute("SELECT type,name,sql FROM sqlite_master").fetchall(), key=lambda r: (r[0], r[1]))


def migrate(db, dump, now):
    # Mirrors SchemaR27.ensure (inside one writer transaction).
    with db:
        for s in dump["ddl"]:
            db.execute(s)
        for table, name, typ in dump["columns"]:
            if db.execute("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", (table,)).fetchone():
                if name not in [r[1] for r in db.execute("PRAGMA table_info(%s)" % table)]:
                    db.execute("ALTER TABLE %s ADD COLUMN %s %s" % (table, name, typ))
        db.execute("UPDATE r26_task SET registered_at=? WHERE registered_at=0", (now,))
        db.execute("UPDATE r26_task SET first_progress_at=last_progress WHERE first_progress_at=0 AND last_progress>0")
        db.execute("INSERT OR IGNORE INTO r27_migration VALUES(?,?,?)", (dump["revision"], now, BASE))


def main():
    dump = json.load(open(sys.argv[1]))
    db = sqlite3.connect(":memory:")
    ddl = baseline_ddl()
    applied, skipped = 0, []
    # Tables first; indexes/triggers on tables owned by other baseline schemas (not part of this file) are skipped
    # and listed — they are untouched by R27 either way.
    for s in sorted(ddl, key=lambda x: 0 if x.startswith("CREATE TABLE") else 1):
        try:
            db.execute(s)
            applied += 1
        except sqlite3.OperationalError as e:
            skipped.append(str(e))
    db.execute("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER DEFAULT 0)")
    check("BASELINE_DDL", applied > 20, "applied=%d statements from %s@%s skippedForeign=%s" % (applied, R26_SCHEMA, BASE[:7], skipped))
    for i in range(300):
        perm = list(range(1, 11))
        perm = perm[i % 10:] + perm[:i % 10]
        db.execute("INSERT INTO draws VALUES(?,?,?,?,?,?,?,?,?,?,?,0)", ["20261028%04d" % i] + perm)
    for i in range(6):
        db.execute("INSERT INTO r26_task(task_id,lane,mechanism,cursor,status,last_progress,steps) VALUES(?,?,?,?,?,?,?)",
                   ("task%d" % i, "RESEARCH", "M%d" % i, i * 10, "READY", (i * 1000) if i % 2 else 0, i))
    tables = {r[0]: [c[1] for c in db.execute("PRAGMA table_info(%s)" % r[0])]
              for r in db.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall()}
    before = content_hash(db, tables)
    counts_before = {t: db.execute("SELECT count(*) FROM %s" % t).fetchone()[0] for t in tables}

    migrate(db, dump, 1_800_000_000_000)
    after = content_hash(db, tables)
    check("M1_NO_DATA_LOSS", before == after and counts_before == {t: db.execute("SELECT count(*) FROM %s" % t).fetchone()[0] for t in tables},
          "tables=%d rows=%d" % (len(tables), sum(counts_before.values())))
    cols = [r[1] for r in db.execute("PRAGMA table_info(r26_task)")]
    missing = [c[1] for c in dump["columns"] if c[1] not in cols]
    rows = db.execute("SELECT task_id,last_progress,registered_at,first_progress_at,wait_kind,error_count FROM r26_task ORDER BY task_id").fetchall()
    ok = not missing and all(r[2] == 1_800_000_000_000 and r[3] == r[1] and r[4] == "" and r[5] == 0 for r in rows)
    check("M2_GUARDED_COLUMNS", ok, "missing=%s rows=%s" % (missing, rows))
    s1, h1 = schema(db), content_hash(db, {r[0]: [c[1] for c in db.execute("PRAGMA table_info(%s)" % r[0])] for r in db.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall()})
    migrate(db, dump, 1_900_000_000_000)
    s2, h2 = schema(db), content_hash(db, {r[0]: [c[1] for c in db.execute("PRAGMA table_info(%s)" % r[0])] for r in db.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall()})
    check("M3_IDEMPOTENT", s1 == s2 and h1 == h2, "objects=%d" % len(s2))
    bad = [s for s in dump["ddl"] if re.search(r"\b(DROP|DELETE)\b", s, re.I)]
    check("M4_NO_DROP_OR_DELETE", not bad, "statements=%d" % len(dump["ddl"]))
    r27 = [r[0] for r in db.execute("SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'r27_%'")]
    check("M5_R27_TABLES", len(r27) >= 30, "r27_tables=%d" % len(r27))

    # ---------------------------------------------------------------- M6 / R27-031
    evo = dump["r26Evolution"]
    def attempt(i, state):
        h = "%064x" % i
        a = "R27:MAIN:" + h
        if state in ("BORN", "REJECTED"):
            db.execute(evo["insert"], (a, "R27_MAIN_G0", h, None, "M_SUBTREE" if i % 2 else "M_CONST", "STRUCTURAL_CANDIDATE" if i % 2 else "PARAMETER_CANDIDATE", "s%d" % i, 1 if state == "BORN" else 0))
        elif state == "ACTIVE":
            db.execute(evo["accepted"], (a,))
        elif state == "VERIFIED":
            db.execute(evo["verified"], (a,))
        elif state == "QUALIFIED":
            db.execute(evo["qualified"], (h,))
    def replay():
        with db:
            for i in range(10):
                attempt(i, "BORN" if i < 8 else "REJECTED")
            for i in range(4):
                attempt(i, "ACTIVE")
            attempt(1, "VERIFIED")
            attempt(1, "QUALIFIED")
    before = dict(zip([d[0] for d in db.execute(evo["reportQuery"]).description], db.execute(evo["reportQuery"]).fetchone()))
    replay()
    once = dict(zip([d[0] for d in db.execute(evo["reportQuery"]).description], db.execute(evo["reportQuery"]).fetchone()))
    replay()
    twice = dict(zip([d[0] for d in db.execute(evo["reportQuery"]).description], db.execute(evo["reportQuery"]).fetchone()))
    expect = {"attempts": 10, "uniquePolicies": 10, "structuralCandidates": 5, "behaviorNovel": 8, "researchAccepted": 4, "prospectiveQualified": 1, "verifiedImprovements": 1}
    check("M6_R26_EVOLUTION_SEVEN_COUNTERS", before["attempts"] == 0 and once == expect and twice == expect, "before=%s after=%s replayed=%s" % (before, once, twice))

    # ---------------------------------------------------------------- H15 (SQL half)
    now = 1_800_000_000_000
    q = dump["qualified"]
    blob = b"\x00" * 80
    cases = {  # policy: (certificate status/expiry or None, e-process state, shadow status)
        "P_OK": (("ACTIVE", now + 3600_000), q, "ACTIVE"),
        "P_NO_CERT": (None, q, "ACTIVE"),
        "P_EXPIRED": (("ACTIVE", now - 1), q, "ACTIVE"),
        "P_REVOKED": (("REVOKED", now + 3600_000), q, "ACTIVE"),
        "P_NOT_QUALIFIED": (("ACTIVE", now + 3600_000), "ACCUMULATING", "ACTIVE"),
        "P_RETIRED_SHADOW": (("ACTIVE", now + 3600_000), q, "RETIRED_TO_ARCHIVE"),
    }
    with db:
        for i, (p, (cert, state, shadow)) in enumerate(cases.items()):
            db.execute("INSERT INTO r27_live_shadow(policy_hash,spec_json,temperature,bias,global_index,status,reason,promoted_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?)",
                       (p, "{}", 1.0, blob, i + 1, shadow, "test", now, now))
            db.execute("INSERT INTO r27_eprocess(policy_hash,global_index,alpha,log_e_up,log_e_down,n,sum_u,sum_u2,last_ordinal,state,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                       (p, i + 1, 0.01, 5.0, 0.0, 900, 0.0, 0.0, 99, state, now))
            if cert:
                db.execute("INSERT INTO r27_certificate VALUES(?,?,?,?,?,?)", (p, cert[0], "{}", "d", now - 10, cert[1]))
            db.execute("INSERT INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                       (p, 100, "202610280100", 3, blob, 1, 2, 3, 1, "FROZEN", now - 5, now + 60_000))
    got = {p: db.execute(dump["intentSql"], (p, "100", str(now), q)).fetchall() for p in cases}
    check("H15_SQL_CERTIFIED_ROW", len(got["P_OK"]) == 1 and got["P_OK"][0][4] == 1, "rows=%d" % len(got["P_OK"]))
    for p in ["P_NO_CERT", "P_EXPIRED", "P_REVOKED", "P_NOT_QUALIFIED", "P_RETIRED_SHADOW"]:
        check("H15_SQL_REJECT_" + p[2:], got[p] == [], "rows=%d" % len(got[p]))
    try:
        with db:
            db.execute("INSERT INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                       ("P_OK", 100, "202610280100", 5, blob, 4, 5, 6, 1, "FROZEN", now, now + 60_000))
        second = "ACCEPTED"
    except sqlite3.IntegrityError as e:
        second = "REJECTED:" + str(e)
    check("H15_SQL_SECOND_DECISION_SAME_PERIOD", second.startswith("REJECTED"), second)
    try:
        with db:
            db.execute("UPDATE r27_live_frozen SET n1=7,n2=8,n3=9 WHERE policy_hash='P_OK' AND target_ordinal=100")
        rewrite = "ACCEPTED"
    except sqlite3.DatabaseError as e:
        rewrite = "REJECTED:" + str(e)
    check("H15_SQL_FROZEN_REWRITE", "R27_FROZEN_IMMUTABLE" in rewrite, rewrite)
    with db:
        db.execute("UPDATE r27_live_frozen SET hit=0,settled_at=? WHERE policy_hash='P_OK' AND target_ordinal=100", (now,))
    late = db.execute(dump["intentSql"], ("P_OK", "100", str(now), q)).fetchall()
    check("H15_SQL_NO_REQUEST_AFTER_SETTLEMENT", late == [], "rows=%d" % len(late))
    nxt = db.execute(dump["intentSql"], ("P_OK", "101", str(now), q)).fetchall()
    check("H15_SQL_NO_UNFROZEN_NEXT_PERIOD_REQUEST", nxt == [], "rows=%d (a loss never creates a request for the next period)" % len(nxt))

    print("R27_MIGRATION|RESULT|%s" % ("ALL_OK" if not FAIL else "FAILED:" + ",".join(FAIL)))
    sys.exit(1 if FAIL else 0)


if __name__ == "__main__":
    main()
