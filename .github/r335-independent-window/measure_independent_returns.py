"""Read observable signed row and return times even where another part of the full trace is missing."""
def partial_clock_rows(db,cal_path):
    import re
    import sqlite3
    from periods import load_collector
    import check_window
    original=load_collector()
    trace=original.trace
    calendar=trace.get_json(cal_path)
    assert not trace.calendar.validate_calendar(calendar)
    rows=[]
    for entry in calendar['entries']:
        target=entry['targetIssue']
        assert re.fullmatch('[0-9]{6,24}',target)
        row={'target':target,'timesMs':{}}
        rows.append(row)
        try:
            previous,_=trace.draw(db,trace.calendar.issue_offset(target,-1))
            opened,_=trace.draw(db,target)
            assert previous['open_time']+60000==entry['deadlineAt']
        except (KeyError,IndexError,ValueError,TypeError,sqlite3.Error,AssertionError):
            row['drawIdentity']='MISSING_OR_INCONSISTENT'
            continue
        row['drawIdentity']='VERIFIED'
        row['timesMs']['previousIngest']=int(previous['first_seen_at']-previous['open_time'])
        row['timesMs']['targetIngest']=int(opened['first_seen_at']-opened['open_time'])
        try:
            state,_=original.freeze_fact(db,entry)
            if state=='FROZEN_ON_TIME':
                freeze=trace.single(db,trace.FREEZE,'target_issue',target)
                row['timesMs']['freezeRecord']=int(freeze['commit_at']-previous['open_time'])
                params=dict(frozenAt=freeze['commit_at'],scheduledDeadline=entry['deadlineAt'])
                returned=check_window.durable_freeze_time(db,trace,target,params,opened)
                row['timesMs']['durableFreeze']=int(returned-previous['open_time'])
        except (KeyError,IndexError,ValueError,TypeError,sqlite3.Error):pass
        try:
            task=trace.single(db,trace.TASK,'target_issue',target)
            if task['status']=='COMPLETED' and task['draw_issue']==target and task['draw_hash']==opened['draw_hash_v2'] and re.fullmatch('[0-9a-f]{64}',task['settlement_hash'] or '') and task['completed_at']>=opened['first_seen_at']:
                row['timesMs']['settlementRecord']=int(task['completed_at']-opened['open_time'])
                wall,mono=check_window.durable_settlement_time(db,trace,target,task)
                row['timesMs']['durableSettlement']=int(wall-opened['open_time'])
                row['timesMs']['monotonicSettlement']=int(mono-opened['open_time'])
        except (KeyError,IndexError,ValueError,TypeError,sqlite3.Error):pass
    assert len(rows)==len({row['target'] for row in rows})==100
    summary={}
    for name,limit in [('freezeRecord',5000),('durableFreeze',5000),('settlementRecord',1000),('durableSettlement',1000),('monotonicSettlement',1000)]:
        observed=[(k['target'],k['timesMs'][name]) for k in rows if name in k['timesMs']]
        summary[name]=dict(measured=len(observed),unobserved=len(rows)-len(observed),limitMs=limit,violations=[period for period,ms in observed if ms<0 or ms>limit])
    return dict(scope='INDEPENDENT_OBSERVABLE_ROW_CLOCKS_NO_COMPLETE_CHAIN_OR_NATURAL_CLAIM',rows=rows,summary=summary)
