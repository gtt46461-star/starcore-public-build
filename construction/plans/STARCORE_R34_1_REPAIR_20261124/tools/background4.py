"""Additional unattended field-campaign checks. Call after the original campaign verifier."""
from datetime import datetime, timedelta, timezone


def require(ok, reason):
    if not ok:
        raise ValueError("BACKGROUND_CONTINUITY: " + reason)


def instant(value):
    t = datetime.fromisoformat(value.replace("Z", "+00:00"))
    require(t.tzinfo is not None, "timestamp needs offset")
    return t.astimezone(timezone.utc)


def ordinal(key):
    require(isinstance(key, str) and len(key) == 12 and key.isascii() and key.isdigit(), "invalid issue")
    sequence = int(key[8:])
    require(1 <= sequence <= 1440, "invalid issue sequence")
    return datetime.strptime(key[:8], "%Y%m%d").toordinal() * 1440 + sequence


def check_background4(receipt, originals):
    run = originals.json(receipt["background_campaign_artifact"])
    start, end = instant(receipt["window_start"]), instant(receipt["window_end"])
    names = [run[k] for k in ("preregistered_calendar_artifact", "opportunity_ledger_artifact",
             "boot_segments_artifact", "interventions_artifact", "raw_events_artifact",
             "coverage_evaluator_artifact", "observer_artifact")]
    require(len(names) == len(set(names)), "observer and source documents must be distinct")
    calendar, ledger, boots, actions, raw, outcome, observer = [originals.json(n) for n in names]
    scope = {k: receipt[k] for k in ("device_identity", "apk_sha256", "installed_version_code",
                                     "window_start", "window_end")}
    scope["source_commit"] = receipt["source_after"]
    for doc in (calendar, ledger, boots, actions, raw, outcome, observer):
        require(all(doc.get(k) == v for k, v in scope.items()), "foreign or missing device campaign scope")
    require(instant(calendar["registered_at"]) <= start, "calendar registered after observation began")
    targets = calendar["targets"]
    items = [calendar["before_window"], *targets, calendar["after_window"]]
    times = [instant(it["opportunity_at"]) for it in items]
    ordinals = [ordinal(it["target_key"]) for it in items]
    require(targets and times[0] < start <= times[1] and times[-2] < end <= times[-1] and
            all(b-a == timedelta(minutes=1) for a, b in zip(times, times[1:])) and
            all(a+1 == b for a, b in zip(ordinals, ordinals[1:])), "calendar boundary, gap or order")

    # One independent device observation stream supports both interval and intervention projections.
    event_by_id = {}
    segment_edges = {"SEGMENT_START", "SEGMENT_END"}
    observed_actions = {"HOME", "SCREEN_OFF", "SCREEN_ON", "LOCK", "UNLOCK", "DOZE", "POWER_MODE",
                        "NETWORK_DOWN", "NETWORK_UP", "TASK_REMOVED", "OS_PROCESS_EXIT", "OS_RESTART",
                        "REBOOT", "PACKAGE_REPLACED", "READ_ONLY_OBSERVATION"}
    intervention = {"ACTIVITY_OPEN", "MANUAL_START_SERVICE", "USER_FORCE_STOP", "USER_REENABLE",
                    "DEBUG_WAKE", "ACTIVE_RECOVERY", "OBSERVER_GAP", "UNKNOWN"}
    in_window_actions = []
    require(observer.get("complete") is True and observer["records"], "incomplete device observation")
    for e in observer["records"]:
        key, kind = e["event_id"], e["kind"]
        require(isinstance(key,str) and key and key not in event_by_id and
                isinstance(e["boot_id"],str) and e["boot_id"] and type(e["elapsed_ms"]) is int
                and e["elapsed_ms"] >= 0, "duplicate or invalid observer event")
        require(kind in segment_edges | observed_actions | intervention, "unknown observer event")
        t = instant(e["observed_at"])
        event_by_id[key] = (e, t)
        if start <= t < end and kind not in segment_edges:
            in_window_actions.append(key)
            require(kind in observed_actions, "assisted or unobserved interval: " + kind)
    action_ids = actions["observer_event_ids"]
    require(isinstance(action_ids,list) and len(set(action_ids)) == len(action_ids) and
            set(action_ids) == set(in_window_actions), "intervention projection differs from observer")
    for doc in (boots, actions, outcome):
        require(doc.get("observer_sha256") == originals.hashes[names[6]], "observer digest differs")
    edge_ids = set()
    observed_intervals = []
    cursor = start
    for segment in boots["segments"]:
        a_id, b_id = segment["start_event_id"], segment["end_event_id"]
        a, ta = event_by_id[a_id]
        b, tb = event_by_id[b_id]
        require(a_id not in edge_ids and b_id not in edge_ids and
                a["kind"] == "SEGMENT_START" and b["kind"] == "SEGMENT_END" and
                a["boot_id"] == b["boot_id"] == segment["boot_id"] and
                type(segment["clock_error_ms"]) is int and segment["clock_error_ms"] >= 0 and
                b["elapsed_ms"] > a["elapsed_ms"] and ta == cursor and ta < tb <= end,
                "unobserved interval, overlapping range or invalid boot segment")
        # Bound clock agreement by the entire campaign's preregistered calibration error.
        tolerance = calendar["clock_error_ms"]
        duration_ms = round((tb-ta).total_seconds()*1000)
        require(type(tolerance) is int and tolerance >= 0 and segment["clock_error_ms"] <= tolerance
                and abs(duration_ms-(b["elapsed_ms"]-a["elapsed_ms"])) <= segment["clock_error_ms"],
                "clock anchors cannot support this interval")
        edge_ids.update((a_id,b_id))
        observed_intervals.append((ta,tb,a["boot_id"],a["elapsed_ms"],b["elapsed_ms"],segment["clock_error_ms"]))
        cursor = tb
    require(cursor == end and edge_ids == {k for k,(e,t) in event_by_id.items()
            if start <= t <= end and e["kind"] in segment_edges}, "incomplete observer intervals")
    def on_observed_clock(event, when):
        mono = event["elapsed_ms"]
        if type(mono) is not int:
            return False
        for lo, hi, boot, first, last, error_ms in observed_intervals:
            if lo <= when <= hi and event["boot_id"] == boot and first <= mono <= last:
                wall_delta = round((when-lo).total_seconds()*1000)
                if abs(wall_delta-(mono-first)) <= error_ms:
                    return True
        return False

    for key in in_window_actions:
        e,t = event_by_id[key]
        require(on_observed_clock(e,t), "observer action outside its boot")

    target_keys = {it["target_key"] for it in targets}
    target_time = {it["target_key"]:instant(it["opportunity_at"]) for it in targets}
    latest, seen = {}, set()
    for e in raw["opportunity_events"]:
        key, seq, t = e["target_key"], e["sequence"], instant(e["observed_at"])
        require(key in target_keys and type(seq) is int and seq >= 0 and (key,t,seq) not in seen,
                "unknown or ambiguous opportunity event")
        seen.add((key,t,seq))
        if start <= t < end:
            require(on_observed_clock(e,t),
                    "business event outside its observed boot")
            if key not in latest or (t,seq) > latest[key][0]:
                latest[key] = ((t,seq),e)
    rows = ledger["opportunities"]
    require(len(rows) == len(target_keys) and {r["target_key"] for r in rows} == target_keys,
            "opportunity denominator differs")
    def original(row, field):
        ref = row[field + "_artifact"]
        require(ref not in names and ref in originals.files, "missing independent " + field + " original")
        doc = originals.json(ref)
        if field + "_record_index" in row:
            ix = row[field + "_record_index"]
            require(type(ix) is int and 0 <= ix < len(doc["records"]), "invalid " + field + " record index")
            return doc["records"][ix]
        return doc

    progress = 0
    for r in rows:
        key = r["target_key"]
        if key in latest:
            e = latest[key][1]
            require(all(r[k] == e[k] for k in ("observed_at","terminal_state","reason","role",
                         "service_commit","producer","boot_id","elapsed_ms")), "72h result differs from last event")
            require(type(e["service_commit"]) is bool, "untyped service commit indicator")
            if e["service_commit"]:
                require(e["producer"] == "SERVICE", "UI or recovery represented as autonomous service")
                c = original(e,"commit")
                require(c.get("committed") is True and
                        all(c.get(k) == v for k,v in scope.items() if k not in ("window_start","window_end")) and
                        all(c.get(k) == e[k] for k in ("target_key","role","observed_at","boot_id","elapsed_ms",
                                                       "terminal_state","producer")),
                        "unbound service commit original")
                progress += 1
        else:
            require(r["terminal_state"] == "NOT_OBSERVED_AS_OF_CUTOFF" and
                    instant(r["observed_at"]) == end and r.get("service_commit") is False,
                    "target without event must remain an explicit end-of-window gap")
    # Each registered opportunity has a distinct outcome for the primary prediction shadow.
    # Completed, eligible business uses its own immutable evidence; auxiliary roles stay separate.
    slo = outcome["per_target_shadow"]
    require(len(slo) == len(target_keys) and {r["target_key"] for r in slo} == target_keys,
            "missing primary-shadow target")
    verified_freezes = 0
    for item in slo:
        k = item["target_key"]
        inp = original(item,"input")
        require(inp.get("target_key") == k and inp.get("origin") == "INDEPENDENT_INPUT_OBSERVER" and
                all(inp.get(f) == scope[f] for f in ("source_commit","device_identity","apk_sha256")),
                "unbound primary-shadow opportunity conditions")
        state = inp["input_state"]
        if state in {"NETWORK_UNAVAILABLE","SOURCE_UNAVAILABLE","OS_RESTRICTION"}:
            require(item["outcome"] == "OBSERVED_INPUT_CONSTRAINT" and bool(inp["reason"]) and
                    instant(inp["from_at"]) <= instant(inp["opportunity_at"]) < instant(inp["until_at"])
                    and instant(inp["opportunity_at"]) == target_time[k], "unsupported input exception")
            continue
        require(state == "HEALTHY" and instant(inp["opportunity_at"]) == target_time[k],
                "unknown health or wrong opportunity used as coverage")
        require(k in latest and latest[k][1]["service_commit"] is True and
                target_time[k] <= instant(inp["deadline_at"]) <= instant(inp["draw_due_at"]),
                "healthy target lacks service-ledger progress or comparable authority deadline")
        freeze = original(item,"freeze")
        require(freeze.get("target_key") == k and freeze.get("committed") is True and
                freeze.get("role") == "R35_PREDICTION_SHADOW" and freeze.get("producer") == "SERVICE"
                and all(freeze.get(f) == scope[f] for f in ("source_commit","device_identity","apk_sha256"))
                and bool(freeze.get("freeze_key")) and
                target_time[k] <= instant(freeze["returned_at"]) < min(instant(inp["deadline_at"]),end),
                "healthy target lacks valid primary predraw service freeze")
        verified_freezes += 1
        authority = original(item,"draw")
        require(authority.get("origin") == "AUTHORITATIVE_DRAW_INGRESS" and authority.get("target_key") == k
                and all(authority.get(f) == scope[f] for f in ("source_commit","device_identity","apk_sha256"))
                and bool(authority.get("provider_id")), "missing target-scoped draw source")
        if instant(inp["draw_due_at"]) >= end:
            require(item["outcome"] == "FROZEN_PENDING_DRAW" and authority.get("state") == "NOT_YET_OBSERVED"
                    and instant(authority["as_of"]) == end, "unmatured sample misreported")
            continue
        settle = original(item,"settlement")
        require(originals.hashes[item["draw_artifact"]] != originals.hashes[item["settlement_artifact"]]
                and authority.get("state") == "DRAW_RECEIVED" and bool(authority.get("draw_id")) and
                settle.get("draw_id") == authority["draw_id"]
                and instant(inp["draw_due_at"]) <= instant(authority["draw_at"])
                <= instant(authority["received_at"]) <= instant(settle["committed_at"]) < end
                and instant(settle["draw_at"]) == instant(authority["draw_at"]),
                "wrong authoritative draw, early draw or settlement before draw receipt")
        require(item["outcome"] == "FROZEN_AND_SETTLED" and settle.get("target_key") == k and
                settle.get("freeze_key") == freeze["freeze_key"] and settle.get("role") == freeze["role"] and
                settle.get("committed") is True and settle.get("producer") == "SERVICE" and
                all(settle.get(f) == scope[f] for f in ("source_commit","device_identity","apk_sha256"))
                and target_time[k] <= instant(settle["draw_at"]) <= instant(settle["committed_at"]) < end
                and instant(freeze["returned_at"]) < instant(settle["draw_at"]),
                "healthy target lacks its own mature settlement")
    require(verified_freezes > 0, "no eligible primary shadow demonstrated")
    require(progress > 0 and outcome.get("service_commit_targets") == progress and
            outcome.get("raw_events_sha256") == originals.hashes[names[4]], "no demonstrated service progress")
    return progress
