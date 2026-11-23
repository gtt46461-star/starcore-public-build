"""Public issue clock and time offsets only. Every draw in the observed interval is kept."""
import time
COLUMNS = ('request','headers','firstByte','body','parse','validated','finished','firstSeen','lastPriorResponse','requestToFirstByteMonotonic','firstByteToBodyMonotonic','bodyToValidationMonotonic')
QUERY = """
WITH eligible AS (SELECT issue, open_time, first_seen_at FROM draws WHERE dirty=0 AND open_time>=:start),
first AS (
 SELECT t.*, row_number() OVER(PARTITION BY t.issue ORDER BY t.first_byte_wall_ms, t.request_id) AS ordinal
 FROM r31_fetch_timing t JOIN eligible d ON d.issue=t.issue
 WHERE t.accepted=1 AND t.body_truncated=0 AND t.first_byte_wall_ms>0
)
SELECT d.issue, d.open_time, t.request_wall_ms-d.open_time,t.headers_wall_ms-d.open_time,
 t.first_byte_wall_ms-d.open_time,t.body_complete_wall_ms-d.open_time,t.parse_wall_ms-d.open_time,
 t.validated_wall_ms-d.open_time,t.finished_wall_ms-d.open_time,d.first_seen_at-d.open_time,
 (SELECT max(old.body_complete_wall_ms)-d.open_time FROM r31_fetch_timing old
 JOIN draws prior ON prior.issue=old.issue AND prior.open_time=d.open_time-60000 AND prior.dirty=0
 WHERE old.accepted=1 AND old.body_truncated=0 AND old.body_complete_wall_ms<=t.first_byte_wall_ms),
 t.first_byte_mono_ms-t.request_mono_ms,t.body_complete_mono_ms-t.first_byte_mono_ms,t.validated_mono_ms-t.body_complete_mono_ms
FROM eligible d LEFT JOIN first t ON t.issue=d.issue AND t.ordinal=1 ORDER BY d.open_time
"""
def observe_transport(query,record,save):
    start = int(query("SELECT CAST(strftime('%s','now') AS INTEGER)*1000"))
    started = time.monotonic()
    record['scope'] = 'READ_ONLY_PRODUCTION_TRANSPORT_LEDGER_ORDINARY_SERVICE_LIVE_NO_HISTORY_IMPORT'
    record['guestStartWallMs'] = start
    for minute in range(1,15):
        time.sleep(max(0,started+minute*60-time.monotonic()))
        try:
            lines = query(QUERY.replace(':start',str(start))).splitlines()
            rows=[]
            for line in lines:
                issue, opened, *clocks = line.split('|')
                assert issue.isdigit() and len(issue)<=20 and len(clocks)==len(COLUMNS)
                rows.append(dict(issue=issue,openWallMs=int(opened),offsetMs={name:int(value) if value else None for name,value in zip(COLUMNS,clocks)}))
            record['periodsObserved']=rows
            record['observations'].append(dict(minute=minute,persistedDraws=len(rows),linkedTransports=sum(r['offsetMs']['firstByte'] is not None for r in rows)))
        except (ValueError,TimeoutError) as err:
            record['observations'].append(dict(minute=minute,readError=type(err).__name__))
        save()
